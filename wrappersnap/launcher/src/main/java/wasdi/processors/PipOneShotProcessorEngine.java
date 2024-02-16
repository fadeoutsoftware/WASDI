package wasdi.processors;

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
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.docker.DockerUtils;
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
            
            EnvironmentVariableConfig oEnvVariable = oProcessorTypeConfig.getEnvironmentVariableConfig("WASDI_ONESHOT_ENCODED_PARAMS");
            
            if (oEnvVariable == null) {
            	oEnvVariable = new EnvironmentVariableConfig();
            	oEnvVariable.key = "WASDI_ONESHOT_ENCODED_PARAMS";
            	oProcessorTypeConfig.environmentVariables.add(oEnvVariable);
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
            
            // Create the Docker Utils Object
            DockerUtils oDockerUtils = new DockerUtils(oProcessor, m_oParameter, PathsConfig.getProcessorFolder(sProcessorName), m_sDockerRegistry);

            // Check if is started otherwise start it
            String sContainerName = startContainerAndGetName(oDockerUtils, oProcessor, oParameter);
            
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

            WasdiLog.debugLog("PipOneShotProcessorEngine.run: process Status: " + sStatus);

            sStatus = waitForApplicationToFinish(oProcessor, oProcessWorkspace.getProcessObjId(), sStatus, oProcessWorkspace);

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
		return null;
	}

}
