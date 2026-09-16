package wasdi.processors;

import java.io.File;

import com.google.common.io.Files;

import wasdi.LauncherMain;
import wasdi.processors.drivers.ContainerRuntimeDriver;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.EnvironmentVariableConfig;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.ProcessorTypeConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Base engine for "one shot" processors: the container runs the app once, using
 * WASDI_* environment variables to pass run context, and exits instead of exposing
 * an internal HTTP server. Package-manager specific behavior stays in subclasses.
 */
public abstract class OneShotProcessorEngine extends DockerBuildOnceEngine {

	public OneShotProcessorEngine() {
		super();
	}

	/**
	 * One-shot containers are transient: there is no previous version to stop before redeploying.
	 * Any run currently in progress just finishes with the old image; future runs use the new one.
	 */
	@Override
	protected void stopRunningVersionBeforeRedeploy(Processor oProcessor) {
		WasdiLog.debugLog("OneShotProcessorEngine.stopRunningVersionBeforeRedeploy: nothing to stop for one-shot processors");
	}

	@Override
	public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {
		 
		// We do not need to start after the build
		m_bRunAfterDeploy = false;
		// And we work with our main register
		m_sDockerRegistry = getDockerRegisterAddress();
		
		if (Utils.isNullOrEmpty(m_sDockerRegistry)) {
			WasdiLog.errorLog("OneShotProcessorEngine.deploy: register address not found, return false.");
			return false;			
		}
		
		WasdiLog.debugLog("OneShotProcessorEngine.deploy: call base class deploy");
		
		// Build the image of the docker
		boolean bResult = super.deploy(oParameter, bFirstDeploy);
		
		if (!bResult) {
			// This is not good
			WasdiLog.errorLog("OneShotProcessorEngine.deploy: super class deploy returned false. So we stop here.");
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
			WasdiLog.errorLog("OneShotProcessorEngine.deploy: Impossible to push the image.");
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
        	WasdiLog.infoLog("OneShotProcessorEngine.addEnvironmentVariablesToProcessorType: param is not encoded, encoding it");
        	
        	sEncodedJson = StringUtils.encodeUrl(sEncodedJson).replace("+", "%20");
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
        
        if (bRefreshPackageList) {
            oEnvVariable = oProcessorTypeConfig.getEnvironmentVariableConfig("WASDI_ONESHOT_REFRESH_PACKAGE_LIST");
            
            if (oEnvVariable == null) {
            	oEnvVariable = new EnvironmentVariableConfig();
            	oEnvVariable.key = "WASDI_ONESHOT_REFRESH_PACKAGE_LIST";
            	oProcessorTypeConfig.environmentVariables.add(oEnvVariable);
            }
            
            WasdiLog.debugLog("OneShotProcessorEngine.addEnvironmentVariablesToProcessorType: adding a temp file name to force the package refresh operation " + sPackageListFileName);
            oEnvVariable.value = sPackageListFileName;        	
        }
	}
	
	@Override
	public boolean run(ProcessorParameter oParameter) {
		
        if (oParameter == null) {
            WasdiLog.errorLog("OneShotProcessorEngine.run: parameter is null");
            return false;
        }
        
        WasdiLog.debugLog("OneShotProcessorEngine.run");

        // Get Repo and Process Workspace
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = m_oProcessWorkspace;

        try {

            // Check workspace folder
        	checkAndCreateWorkspaceFolder(oParameter);

            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

            // First Check if processor exists
            String sProcessorId = oParameter.getProcessorID();

            ProcessorRepository oProcessorRepository = new ProcessorRepository();
            Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

            // Check processor
            if (oProcessor == null) {
            	WasdiLog.errorLog("OneShotProcessorEngine.run: Impossible to find processor " + sProcessorId);
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
                return false;                    
            }
            
    		// And we work with our main register
    		m_sDockerRegistry = getDockerRegisterAddress();
    		
    		if (Utils.isNullOrEmpty(m_sDockerRegistry)) {
    			WasdiLog.errorLog("OneShotProcessorEngine.run: register address not found, return false.");
    			return false;			
    		}            
            
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
            
            // The container runtime driver decides where/how the app actually executes (local Docker today)
            ContainerRuntimeDriver oContainerRuntimeDriver = getContainerRuntimeDriver();

            // Check if is started otherwise start it
            String sContainerName = oContainerRuntimeDriver.run(oParameter, m_sDockerImageName, sEncodedJson);
            
            // If we do not have a container name here, we are not in the position to continue
            if (Utils.isNullOrEmpty(sContainerName)) {
            	WasdiLog.errorLog("OneShotProcessorEngine.run: Impossible to start the application docker");
            	return false;
            }
                                    
            // Read Again Process Workspace: the user may have changed it!
            oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());

            // Here we can wait for the process to finish with the status check
            // we can also handle a timeout, that is a property (with default) of the processor
            String sStatus = oProcessWorkspace.getStatus();

            WasdiLog.debugLog("OneShotProcessorEngine.run: process Status after start: " + sStatus);
            
            if (sStatus.equals(ProcessStatus.DONE.name())==false && sStatus.equals(ProcessStatus.ERROR.name())==false && sStatus.equals(ProcessStatus.STOPPED.name())==false) {
            	sStatus = oContainerRuntimeDriver.waitForCompletion(oParameter);
            }

            WasdiLog.debugLog("OneShotProcessorEngine.run: process finished with status " + sStatus);
            
            // Check and set the operation end-date
            if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
                oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("OneShotProcessorEngine.run Exception", oEx);
            try {
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
            } catch (Exception oInnerEx) {
                WasdiLog.errorLog("OneShotProcessorEngine.run Exception", oInnerEx);
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

	/**
	 * Override of the wait for application to start
	 */
	@Override
	public boolean refreshPackagesInfo(ProcessorParameter oParameter) {
		if (oParameter == null) {
			WasdiLog.errorLog("OneShotProcessorEngine.refreshPackagesInfo: oParameter is null");
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
				WasdiLog.errorLog("OneShotProcessorEngine.refreshPackagesInfo: Processor [" + sProcessorName + "] environment not updated in this node, return");
				return false;
			}
	
	        ProcessorTypeConfig oProcessorTypeConfig = WasdiConfig.Current.dockers.getProcessorTypeConfig(oProcessor.getType());
	        
	        if (oProcessorTypeConfig == null) {
	        	oProcessorTypeConfig = new ProcessorTypeConfig();
	        	oProcessorTypeConfig.processorType = oProcessor.getType();
	        	WasdiConfig.Current.dockers.processorTypes.add(oProcessorTypeConfig);
	        }
	        
	        String sRandomName = Utils.getRandomName();
	        
	        addEnvironmentVariablesToProcessorType(oProcessorTypeConfig, "", oParameter, true, sRandomName);
	        
	        // This is a normal one-shot run: the env var above makes the executor dump the
	        // package list to sRandomName instead of running the processor
	        ContainerRuntimeDriver oContainerRuntimeDriver = getContainerRuntimeDriver();
	        
	        String sContainerName = oContainerRuntimeDriver.run(oParameter, m_sDockerImageName, "");
	        
	        // Try to start Again the docker
	        if (Utils.isNullOrEmpty(sContainerName)) {
	        	WasdiLog.errorLog("OneShotProcessorEngine.refreshPackagesInfo: Impossible to start the application docker");
	        	return false;
	        }
	        
	        oContainerRuntimeDriver.waitForCompletion(oParameter);
	        
	        String sWorkspacePath = PathsConfig.getWorkspacePath(oParameter);
	        String sOriginFile = sWorkspacePath + sRandomName;
	        
	        String sDestinationFile = sProcessorFolder + "packagesInfo.json";
	        
	        WasdiLog.debugLog("OneShotProcessorEngine.refreshPackagesInfo: moving packages file " + sOriginFile + " in the processor folder " + sDestinationFile);
	        
	        File oOriginalFile = new File(sOriginFile);
	        File oDestinationFile = new File(sDestinationFile);
	        
	        if (!oOriginalFile.exists()) {
	        	WasdiLog.debugLog("OneShotProcessorEngine.refreshPackagesInfo: looks that original file does not exist, strange, wait a little bit");
	        }

	        Files.copy(oOriginalFile, oDestinationFile);
	        
	        WasdiLog.debugLog("OneShotProcessorEngine.refreshPackagesInfo: cleaning temp file");
	        oOriginalFile.delete();
	        
	        WasdiLog.debugLog("OneShotProcessorEngine.refreshPackagesInfo: packages list updated");

			return true;
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("OneShotProcessorEngine.refreshPackagesInfo: exception ", oEx);
		}

		return false;
	}

    @Override
    public boolean stopApplication(ProcessorParameter oParameter) {
    	try {
    		return getContainerRuntimeDriver().stop(oParameter.getProcessObjId());
    	}
    	catch (Exception oEx) {
			WasdiLog.errorLog("OneShotProcessorEngine.stopApplication: error", oEx);
			return false;
		}
    }
}
