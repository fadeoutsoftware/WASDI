package wasdi.processors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.json.JSONObject;

import com.google.common.io.Files;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.config.EnvironmentVariableConfig;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.ProcessorTypeConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.packagemanagers.PipOneShotPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.docker.DockerUtils;
import wasdi.shared.utils.docker.containersViewModels.ContainerInfo;
import wasdi.shared.utils.docker.containersViewModels.constants.ContainerStates;
import wasdi.shared.utils.log.WasdiLog;

public class PipOneShotProcessorEngine extends DockerBuildOnceEngine {
	
	public PipOneShotProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.PIP_ONESHOT);
		
	}
	
	@Override
	public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {
		 
		// We do not need to start after the build
		m_bRunAfterDeploy = false;
		// And we work with our main register
		m_sDockerRegistry = getDockerRegisterAddress();
		
		if (Utils.isNullOrEmpty(m_sDockerRegistry)) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.deploy: register address not found, return false.");
			return false;			
		}
		
		WasdiLog.debugLog("PipOneShotProcessorEngine.deploy: call base class deploy");
		
		// Build the image of the docker
		boolean bResult = super.deploy(oParameter, bFirstDeploy);
		
		if (!bResult) {
			// This is not good
			WasdiLog.errorLog("PipOneShotProcessorEngine.deploy: super class deploy returned false. So we stop here.");
			return false;
		}
		
		// Get Processor Name and Id
		String sProcessorId = oParameter.getProcessorID();
		
		// Read the processor from the db
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		// Here we save the address of the image
		String sPushedImageAddress = pushImageInRegisters(oProcessor);
		
		if (Utils.isNullOrEmpty(sPushedImageAddress)) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.deploy: Impossible to push the image.");
			return false;
		}
		
        return true;		
	}
	
	protected void addEnvironmentVariablesToProcessorType(ProcessorTypeConfig oProcessorTypeConfig, String sEncodedJson, ProcessorParameter oParameter) {
		addEnvironmentVariablesToProcessorType(oProcessorTypeConfig, sEncodedJson, oParameter, false);
	}
	
	protected void addEnvironmentVariablesToProcessorType(ProcessorTypeConfig oProcessorTypeConfig, String sEncodedJson, ProcessorParameter oParameter, boolean bRefreshPackageList) {
		addEnvironmentVariablesToProcessorType(oProcessorTypeConfig, sEncodedJson, oParameter, bRefreshPackageList, "");
	}
	
	protected void addEnvironmentVariablesToProcessorType(ProcessorTypeConfig oProcessorTypeConfig, String sEncodedJson, ProcessorParameter oParameter, boolean bRefreshPackageList, String sPackageListFileName) {
        EnvironmentVariableConfig oEnvVariable = oProcessorTypeConfig.getEnvironmentVariableConfig("WASDI_ONESHOT_ENCODED_PARAMS");
        
        if (oEnvVariable == null) {
        	oEnvVariable = new EnvironmentVariableConfig();
        	oEnvVariable.key = "WASDI_ONESHOT_ENCODED_PARAMS";
        	oProcessorTypeConfig.environmentVariables.add(oEnvVariable);
        }
        
        if (sEncodedJson.startsWith("{")) {
        	WasdiLog.infoLog("PipOneShotProcessorEngine.addEnvironmentVariablesToProcessorType: param is not encoded, encoding it");
        	
        	sEncodedJson = StringUtils.encodeUrl(sEncodedJson).replace("+", "%20");
        	
//			if (sEncodedJson.contains("+")) {
//				// The + char is not encoded but then in the launcher, when is decode, become a space. Replace the char with the correct representation
//				sEncodedJson = sEncodedJson.replaceAll("\\+", "%2B");
//				WasdiLog.debugLog("PipOneShotProcessorEngine.addEnvironmentVariablesToProcessorType: replaced + with %2B in encoded JSON " + sEncodedJson);
//			}
        }
        
        oEnvVariable.value = sEncodedJson;
        
        oEnvVariable = oProcessorTypeConfig.getEnvironmentVariableConfig("WASDI_WEBSERVER_URL");
        
        if (oEnvVariable == null) {
        	oEnvVariable = new EnvironmentVariableConfig();
        	oEnvVariable.key = "WASDI_WEBSERVER_URL";
        	oProcessorTypeConfig.environmentVariables.add(oEnvVariable);
        }
        
        oEnvVariable.value = WasdiConfig.Current.baseUrl;
        
        oEnvVariable = oProcessorTypeConfig.getEnvironmentVariableConfig("WASDI_USER");
        
        if (oEnvVariable == null) {
        	oEnvVariable = new EnvironmentVariableConfig();
        	oEnvVariable.key = "WASDI_USER";
        	oProcessorTypeConfig.environmentVariables.add(oEnvVariable);
        }
        
        oEnvVariable.value = oParameter.getUserId();
        
        oEnvVariable = oProcessorTypeConfig.getEnvironmentVariableConfig("WASDI_SESSION_ID");
        
        if (oEnvVariable == null) {
        	oEnvVariable = new EnvironmentVariableConfig();
        	oEnvVariable.key = "WASDI_SESSION_ID";
        	oProcessorTypeConfig.environmentVariables.add(oEnvVariable);
        }
        
        oEnvVariable.value = oParameter.getSessionID();
        
        oEnvVariable = oProcessorTypeConfig.getEnvironmentVariableConfig("WASDI_PROCESS_WORKSPACE_ID");
        
        if (oEnvVariable == null) {
        	oEnvVariable = new EnvironmentVariableConfig();
        	oEnvVariable.key = "WASDI_PROCESS_WORKSPACE_ID";
        	oProcessorTypeConfig.environmentVariables.add(oEnvVariable);
        }
        
        oEnvVariable.value = oParameter.getProcessObjId();
        
        oEnvVariable = oProcessorTypeConfig.getEnvironmentVariableConfig("WASDI_WORKSPACE_ID");
        
        if (oEnvVariable == null) {
        	oEnvVariable = new EnvironmentVariableConfig();
        	oEnvVariable.key = "WASDI_WORKSPACE_ID";
        	oProcessorTypeConfig.environmentVariables.add(oEnvVariable);
        }
        
        oEnvVariable.value = oParameter.getWorkspace();
        
//        oEnvVariable = oProcessorTypeConfig.getEnvironmentVariableConfig("WASDI_ONESHOT_ON_SERVER");
//        
//        if (oEnvVariable == null) {
//        	oEnvVariable = new EnvironmentVariableConfig();
//        	oEnvVariable.key = "WASDI_ONESHOT_ON_SERVER";
//        	oProcessorTypeConfig.environmentVariables.add(oEnvVariable);
//        }
//        
//        oEnvVariable.value = "1";
        
        if (bRefreshPackageList) {
            oEnvVariable = oProcessorTypeConfig.getEnvironmentVariableConfig("WASDI_ONESHOT_REFRESH_PACKAGE_LIST");
            
            if (oEnvVariable == null) {
            	oEnvVariable = new EnvironmentVariableConfig();
            	oEnvVariable.key = "WASDI_ONESHOT_REFRESH_PACKAGE_LIST";
            	oProcessorTypeConfig.environmentVariables.add(oEnvVariable);
            }
            
            WasdiLog.debugLog("PipOneShotProcessorEngine.addEnvironmentVariablesToProcessorType: adding a temp file name to force the package refresh operation " + sPackageListFileName);
            oEnvVariable.value = sPackageListFileName;        	
        }
	}
	
	@Override
	public boolean run(ProcessorParameter oParameter) {
		
        if (oParameter == null) {
            WasdiLog.errorLog("PipOneShotProcessorEngine.run: parameter is null");
            return false;
        }
        
        WasdiLog.debugLog("PipOneShotProcessorEngine.run");

        // Get Repo and Process Workspace
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = m_oProcessWorkspace;

        try {

            // Check workspace folder
        	checkAndCreateWorkspaceFolder(oParameter);

            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

            // First Check if processor exists
            String sProcessorName = oParameter.getName();
            String sProcessorId = oParameter.getProcessorID();

            ProcessorRepository oProcessorRepository = new ProcessorRepository();
            Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

            // Check processor
            if (oProcessor == null) {
            	WasdiLog.errorLog("PipOneShotProcessorEngine.run: Impossible to find processor " + sProcessorId);
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
                return false;                    
            }
            
    		// And we work with our main register
    		m_sDockerRegistry = getDockerRegisterAddress();
    		
    		if (Utils.isNullOrEmpty(m_sDockerRegistry)) {
    			WasdiLog.errorLog("PipOneShotProcessorEngine.run: register address not found, return false.");
    			return false;			
    		}            
            
            //m_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.INFO.name(), m_oParameter.getExchange(), "INSTALLATION DONE<BR>STARTING APP", m_oParameter.getExchange());
    		
            // Decode JSON
            String sEncodedJson = oParameter.getJson();

            // Json sanity check
            if (Utils.isNullOrEmpty(sEncodedJson)) {
            	sEncodedJson = "{}";
            }
            
            ProcessorTypeConfig oProcessorTypeConfig = WasdiConfig.Current.dockers.getProcessorTypeConfig(oProcessor.getType());
            
            if (oProcessorTypeConfig == null) {
            	oProcessorTypeConfig = new ProcessorTypeConfig();
            	oProcessorTypeConfig.processorType = oProcessor.getType();
            	WasdiConfig.Current.dockers.processorTypes.add(oProcessorTypeConfig);
            }
            
            addEnvironmentVariablesToProcessorType(oProcessorTypeConfig,sEncodedJson,oParameter);
            
            // Create the Docker Utils Object
            DockerUtils oDockerUtils = new DockerUtils(oProcessor, m_oParameter, PathsConfig.getProcessorFolder(sProcessorName), m_sDockerRegistry, m_oProcessWorkspaceLogger);

            // Check if is started otherwise start it
            String sContainerName = startContainerAndGetName(oDockerUtils, oProcessor, oParameter, false, WasdiConfig.Current.dockers.removeDockersAfterShellExec, false);
            
            // If we do not have a container name here, we are not in the position to continue
            if (Utils.isNullOrEmpty(sContainerName)) {
            	WasdiLog.errorLog("PipOneShotProcessorEngine.run: Impossible to start the application docker");
            	return false;
            }
                                    
            // Read Again Process Workspace: the user may have changed it!
            oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());

            // Here we can wait for the process to finish with the status check
            // we can also handle a timeout, that is a property (with default) of the processor
            String sStatus = oProcessWorkspace.getStatus();

            WasdiLog.debugLog("PipOneShotProcessorEngine.run: process Status after start: " + sStatus);
            
            if (sStatus.equals(ProcessStatus.DONE.name())==false && sStatus.equals(ProcessStatus.ERROR.name())==false && sStatus.equals(ProcessStatus.STOPPED.name())==false) {
            	sStatus = waitForApplicationToFinish(oProcessor, oProcessWorkspace.getProcessObjId(), sStatus, oProcessWorkspace, sContainerName);
            }

            WasdiLog.debugLog("PipOneShotProcessorEngine.run: process finished with status " + sStatus);
            
            // Check and set the operation end-date
            if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
                oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("PipOneShotProcessorEngine.run Exception", oEx);
            try {
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
            } catch (Exception oInnerEx) {
                WasdiLog.errorLog("PipOneShotProcessorEngine.run Exception", oInnerEx);
            }

            return false;
        }
        finally {
        	if (oProcessWorkspace != null) {
        		m_oProcessWorkspace.setStatus(oProcessWorkspace.getStatus());
        	}
        }

        return true;
		
	}
	

	@Override
	protected IPackageManager getPackageManager(String sUrl) {
		
		PipOneShotPackageManager oPipOneShotPackageManager = new PipOneShotPackageManager(sUrl);
		
		return oPipOneShotPackageManager;
	}

	/**
	 * Override of the wait for application to start
	 */
	@Override
	public void waitForApplicationToStart(ProcessorParameter oParameter) {
		try {
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(oParameter.getProcessorID());
			
			DockerUtils oDockerUtils = new DockerUtils(oProcessor, m_oParameter, PathsConfig.getProcessorFolder(oProcessor.getName()), m_sDockerRegistry);
			
	        WasdiLog.debugLog("PipOneShotProcessorEngine.waitForApplicationToStart: wait to let docker start");

	        Integer iNumberOfAttemptsToPingTheServer = WasdiConfig.Current.dockers.numberOfAttemptsToPingTheServer;
	        Integer iMillisBetweenAttmpts = WasdiConfig.Current.dockers.millisBetweenAttmpts;

	        for (int i = 0; i < iNumberOfAttemptsToPingTheServer; i++) {
	        	
	        	ContainerInfo oContainer = oDockerUtils.getContainerInfoByImageName(oProcessor.getName(), oProcessor.getVersion());
	        	
	        	if (oContainer.State.equals(ContainerStates.RUNNING) || oContainer.State.equals(ContainerStates.EXITED) || oContainer.State.equals(ContainerStates.DEAD)) {
	        		return;
	        	}
	        	
	        	Thread.sleep(iMillisBetweenAttmpts);
	        }
	        
	        WasdiLog.debugLog("PipOneShotProcessorEngine.waitForApplicationToStart: attemps finished.. probably did not started!");
		}
    	catch (InterruptedException oEx) {
    		Thread.currentThread().interrupt();
    		WasdiLog.errorLog("PipOneShotProcessorEngine.waitForApplicationToStart: current thread was interrupted ", oEx);
    	}
		catch (Exception oEx) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.waitForApplicationToStart: exception ", oEx);
		}
	}
	
    /**
     * Updates the processor environment
     */
	@Override
	public boolean environmentUpdate(ProcessorParameter oParameter) {

		if (oParameter == null) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.environmentUpdate: oParameter is null");
			return false;
		}

		if (Utils.isNullOrEmpty(oParameter.getJson())) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.environmentUpdate: update command is null or empty");
			return false;
		}
		
		if (!WasdiConfig.Current.isMainNode()) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.environmentUpdate: this processor manipulate environment only on the main node");
			return true;			
		}

		ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
		ProcessWorkspace oProcessWorkspace = null;

		try {
			oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oProcessWorkspace = m_oProcessWorkspace;

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			// First Check if processor exists
			String sProcessorName = oParameter.getName();
			String sProcessorId = oParameter.getProcessorID();

			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

			// Check processor
			if (oProcessor == null) {
				WasdiLog.errorLog("PipOneShotProcessorEngine.environmentUpdate: oProcessor is null [" + sProcessorId + "]");
				return false;
			}

			WasdiLog.infoLog("PipOneShotProcessorEngine.environmentUpdate: update env for " + sProcessorName);

			String sJson = oParameter.getJson();
			WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: sJson: " + sJson);
			JSONObject oJsonItem = new JSONObject(sJson);

			Object oUpdateCommand = oJsonItem.get("updateCommand");

			if (oUpdateCommand == null || oUpdateCommand.equals(org.json.JSONObject.NULL)) {
				// This will be done in the Operation code
				WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: refresh of the list of libraries.");
			} 
			else {
				String sUpdateCommand = (String) oUpdateCommand;
				WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: sUpdateCommand: " + sUpdateCommand);
				
				String [] asParts = sUpdateCommand.split("/");
				
				String sPackage = "";
				boolean bAdd = true;
				
				if (asParts != null) {
					if (asParts.length>=2) {
						sPackage = asParts[1];
						
						if (asParts[0].equals("removePackage")) {
							bAdd = false;
						}
					}
				}
				
				if (Utils.isNullOrEmpty(sPackage)) {
					WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: cannot extract the name of the package, error");
					return false;
				}
				
				String sMessage = "PipOneShotProcessorEngine.environmentUpdate: ";
				if (bAdd) sMessage += "Adding Package ";
				else sMessage += "Removing Package ";
				sMessage += sPackage;
				
				WasdiLog.debugLog(sMessage);
				
				String sProcessorPath = PathsConfig.getProcessorFolder(sProcessorName);
				
				File oPipFile = new File(sProcessorPath+"pip.txt");
				
				// we re-read all the actions line per line
				ArrayList<String> asPipLines = new ArrayList<>();
				
				if (oPipFile.exists()) {
			        try (java.util.stream.Stream<String> oLinesStream = java.nio.file.Files.lines(oPipFile.toPath())) {
			        	oLinesStream.forEach(sLine -> {
			        		asPipLines.add(sLine);
			            });
			        }					
				}
		        
		        ArrayList<String> asCleanPipLines = Utils.removeDuplicates(asPipLines);
		        
		        if (bAdd) {
		        	if (asCleanPipLines.contains(sPackage)) {
		        		WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: the package " +  sPackage + " already exists, move last");
		        		asCleanPipLines.remove(sPackage);
		        	}
		        	else {
		        		WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: package " +  sPackage + " added");
		        	}
		        	asCleanPipLines.add(sPackage);
		        }
		        else {
		        	if (asCleanPipLines.contains(sPackage)) {
		        		WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: removing package " +  sPackage);
		        		asCleanPipLines.remove(sPackage);
		        	}
		        	else {
		        		WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: the package " +  sPackage + " is not in the list, nothing to remove");
		        	}
		        }
		        
		        // Re-write pip.txt
		        WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: writing new pip.txt");
				try (OutputStream oOutStream = new FileOutputStream(oPipFile, false)) {
					for (String sActualLine : asCleanPipLines) {
						String sWriteLine = sActualLine + "\n";
						byte[] ayBytes = sWriteLine.getBytes();
						oOutStream.write(ayBytes);
					}
				}		        
		        
		        WasdiLog.debugLog("PipOneShotProcessorEngine.environmentUpdate: starting re-deploy");
		        this.redeploy(oParameter);
			}

			return true;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.environmentUpdate Exception", oEx);
			try {

				if (oProcessWorkspace != null) {
					// Check and set the operation end-date
					if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
						oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
					}

					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				}
			} catch (Exception e) {
				WasdiLog.errorLog("PipOneShotProcessorEngine.environmentUpdate Exception", e);
			}

			return false;
		}
	}
	
	public boolean refreshPackagesInfo(ProcessorParameter oParameter) {
		if (oParameter == null) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.refreshPackagesInfo: oParameter is null");
			return false;
		}
		
		try {
			
			String sProcessorName = oParameter.getName();
			String sProcessorId = oParameter.getProcessorID();
	
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
	
			// Set the processor path
			String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);
			File oProcessorFolder = new File(sProcessorFolder);
	
			// Is the processor installed in this node?
			if (!oProcessorFolder.exists()) {
				WasdiLog.errorLog("PipOneShotProcessorEngine.refreshPackagesInfo: Processor [" + sProcessorName + "] environment not updated in this node, return");
				return false;
			}
	
	        // Create the Docker Utils Object
	        DockerUtils oDockerUtils = new DockerUtils(oProcessor, m_oParameter, PathsConfig.getProcessorFolder(sProcessorName), m_sDockerRegistry);
	        
	        ProcessorTypeConfig oProcessorTypeConfig = WasdiConfig.Current.dockers.getProcessorTypeConfig(oProcessor.getType());
	        
	        if (oProcessorTypeConfig == null) {
	        	oProcessorTypeConfig = new ProcessorTypeConfig();
	        	oProcessorTypeConfig.processorType = oProcessor.getType();
	        	WasdiConfig.Current.dockers.processorTypes.add(oProcessorTypeConfig);
	        }
	        
	        String sRandomName = Utils.getRandomName();
	        
	        addEnvironmentVariablesToProcessorType(oProcessorTypeConfig, "", oParameter, true, sRandomName);
	        
	        String sContainerName = oDockerUtils.start("", oProcessor.getPort(), true);
	        
	        // Try to start Again the docker
	        if (Utils.isNullOrEmpty(sContainerName)) {
	        	WasdiLog.errorLog("PipOneShotProcessorEngine.refreshPackagesInfo: Impossible to start the application docker");
	        	return false;
	        }
	        
	        waitForApplicationToFinish(oProcessor, oParameter.getProcessObjId(), "", m_oProcessWorkspace);
	        
	        String sWorkspacePath = PathsConfig.getWorkspacePath(oParameter);
	        String sOriginFile = sWorkspacePath + sRandomName;
	        
	        String sDestinationFile = sProcessorFolder + "packagesInfo.json";
	        
	        WasdiLog.debugLog("PipOneShotProcessorEngine.refreshPackagesInfo: moving packages file " + sOriginFile + " in the processor folder " + sDestinationFile);
	        
	        File oOriginalFile = new File(sOriginFile);
	        File oDestinationFile = new File(sDestinationFile);
	        
	        if (!oOriginalFile.exists()) {
	        	WasdiLog.debugLog("PipOneShotProcessorEngine.refreshPackagesInfo: looks that original file does not exist, strange, wait a little bit");
	        }

	        Files.copy(oOriginalFile, oDestinationFile);
	        
	        WasdiLog.debugLog("PipOneShotProcessorEngine.refreshPackagesInfo: cleaning temp file");
	        oOriginalFile.delete();
	        
	        WasdiLog.debugLog("PipOneShotProcessorEngine.refreshPackagesInfo: packages list updated");

			return true;
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.refreshPackagesInfo: exception ", oEx);
		}

		return false;
	}
	
	@Override
	protected void onAfterUnzipProcessor(String sProcessorFolder) {
		WasdiLog.infoLog("PipOneShotProcessorEngine.onAfterUnzipProcessor: calling base class method");
		
		super.onAfterUnzipProcessor(sProcessorFolder);
		
		try {

			// Check the pip file
			File oPipFile = new File(sProcessorFolder+"pip.txt");

			if (!oPipFile.exists()) {
				WasdiLog.infoLog("PipProcessorEngine.onAfterUnzipProcessor: pip file not present, done");
				return;
			}
			else {
				WasdiLog.infoLog("PipOneShotProcessorEngine.onAfterUnzipProcessor: Make a copy of orginal/adjusted pip file");
				File oDestinationPipFile = new File(sProcessorFolder + "pip_original.txt");
				Files.copy(oPipFile, oDestinationPipFile);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PipOneShotProcessorEngine.onAfterUnzipProcessor: exception ", oEx);
		}
	}
	
}
