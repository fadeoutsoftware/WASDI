package wasdi.processors;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;

import wasdi.LauncherMain;
import wasdi.asynch.PushDockerImagesThread;
import wasdi.processors.dockerUtils.DockerUtils;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.payloads.DeleteProcessorPayload;
import wasdi.shared.payloads.DeployProcessorPayload;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

/**
 * Docker processor Engine
 * 
 * It implements method to deploy, redeploy and run applications in 
 * Docker images
 * 
 * @author p.campanella
 *
 */
public abstract class DockerProcessorEngine extends WasdiProcessorEngine {
	
	/**
	 * Name of the generated Docker Image
	 */
	protected String m_sDockerImageName = "";
	
	/**
	 * Address of the docker registry in use
	 */
	protected String m_sDockerRegistry = "";
	
	/**
	 * Create clean instance
	 */
	public DockerProcessorEngine() {
		super();
	}
	
	/**
	 * Get the name of the docker image
	 * @return
	 */
	public String getDockerImageName() {
		return m_sDockerImageName;
	}

	/**
	 * Set the name of the docker image
	 * @param sDockerImageName
	 */
	public void setDockerImageName(String sDockerImageName) {
		this.m_sDockerImageName = sDockerImageName;
	}
	
	/**
	 * Get the adress of the docker registry
	 * @return
	 */
	public String getDockerRegistry() {
		return m_sDockerRegistry;
	}

	/**
	 * Set the address of the docker registry
	 * @param sDockerRegistry
	 */
	public void setDockerRegistry(String sDockerRegistry) {
		this.m_sDockerRegistry = sDockerRegistry;
	}
	
    /**
     * Deploy a new Processor in WASDI
     *
     * @param oParameter Processor Parameter with the info of the app to deploy
     */
    @Override
    public boolean deploy(ProcessorParameter oParameter) {
        return deploy(oParameter, true);
    }
    
    /**
     * Deploy a new Processor in WASDI
     *
     * @param oParameter Processor Parameter with the info of the app to deploy
     * @param bFirstDeploy True if this is the first deploy. False if it is not
     */
    public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {

        WasdiLog.debugLog("DockerProcessorEngine.DeployProcessor: start");

        if (oParameter == null) {
        	return logDeployErrorAndClean("parameter is null, return false", bFirstDeploy);
        }

        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessorRepository oProcessorRepository = new ProcessorRepository();

        String sProcessorName = oParameter.getName();
        String sProcessorId = oParameter.getProcessorID();

        try {

            processWorkspaceLog("Start Deploy of " + sProcessorName + " Type " + oParameter.getProcessorType());
            
            if (bFirstDeploy) {
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.RUNNING, 0);
                processWorkspaceLog("This is a first deploy of this app");
            }

            // First Check if processor exists
            Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
            String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);

            // Create the file
            File oProcessorZipFile = new File(sProcessorFolder + sProcessorId + ".zip");

            WasdiLog.debugLog("DockerProcessorEngine.DeployProcessor: check processor exists in " + oProcessorZipFile.getAbsolutePath());

            // Check it
            if (oProcessorZipFile.exists() == false) {
            	return logDeployErrorAndClean("Cannot find the processor file... something went wrong", bFirstDeploy);
            }

            if (bFirstDeploy) {
            	LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.RUNNING, 2);
            }
                
            WasdiLog.errorLog("DockerProcessorEngine.DeployProcessor: unzip processor");

            // Unzip the processor (and check for entry point myProcessor.py)
            if (!unzipProcessor(sProcessorFolder, sProcessorId + ".zip", oParameter.getProcessObjId())) {
            	return logDeployErrorAndClean("error unzipping the Processor [" + sProcessorName + "]", bFirstDeploy);
            }

            onAfterUnzipProcessor(sProcessorFolder);

            if (bFirstDeploy) {
            	LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.RUNNING, 20);
            }
            
            WasdiLog.debugLog("DockerProcessorEngine.DeployProcessor: copy container image template");

            // Copy Docker template files in the processor folder
            File oDockerTemplateFolder = new File(m_sDockerTemplatePath);
            File oProcessorFolder = new File(sProcessorFolder);

            FileUtils.copyDirectory(oDockerTemplateFolder, oProcessorFolder);

            if (bFirstDeploy) {
            	LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.RUNNING, 25);
            }
            
            onAfterCopyTemplate(sProcessorFolder);

            // Generate the image
            WasdiLog.debugLog("DockerProcessorEngine.DeployProcessor: building image (Registry = " + m_sDockerRegistry + ")");
            processWorkspaceLog("Start building Image");

            // Create Docker Util and deploy the docker
            DockerUtils oDockerUtils = new DockerUtils(oProcessor, sProcessorFolder, m_sTomcatUser, m_sDockerRegistry);
            m_sDockerImageName = oDockerUtils.build();
            
            if (Utils.isNullOrEmpty(m_sDockerImageName)) {
            	return logDeployErrorAndClean("the deploy returned an empyt image name, something went wrong", bFirstDeploy);            	
            }

            onAfterDeploy(sProcessorFolder);
            processWorkspaceLog("Image done");

            if (bFirstDeploy) {
            	LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.RUNNING, 70);
            }

            // Find the processor port
            int iProcessorPort = oProcessorRepository.getNextProcessorPort();
            if (!bFirstDeploy) {
                iProcessorPort = oProcessor.getPort();
            }

            if (bFirstDeploy) {
            	// In case of a first deploy we update the processor with the assigned port
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.RUNNING, 90);
                oProcessor.setPort(iProcessorPort);
                oProcessorRepository.updateProcessor(oProcessor);
            }

            try {
                DeployProcessorPayload oDeployPayload = new DeployProcessorPayload();
                oDeployPayload.setProcessorName(sProcessorName);
                oDeployPayload.setType(oParameter.getProcessorType());
                m_oProcessWorkspace.setPayload(LauncherMain.s_oMapper.writeValueAsString(oDeployPayload));
            } catch (Exception oPayloadException) {
                WasdiLog.errorLog("DockerProcessorEngine.DeployProcessor Exception creating payload ", oPayloadException);
            }

            if (bFirstDeploy) {
            	LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.DONE, 100);
            }
            
            processWorkspaceLog(new EndMessageProvider().getGood());

        } catch (Exception oEx) {

            processWorkspaceLog("There was an error... sorry...");
            processWorkspaceLog(new EndMessageProvider().getBad());

            WasdiLog.errorLog("DockerProcessorEngine.DeployProcessor Exception", oEx);
            try {
                if (bFirstDeploy) {
                    try {
                        oProcessorRepository.deleteProcessor(sProcessorId);
                    } catch (Exception oInnerEx) {
                        WasdiLog.errorLog("DockerProcessorEngine.DeployProcessor Exception", oInnerEx);
                    }

                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
            } catch (Exception e) {
                WasdiLog.errorLog("DockerProcessorEngine.DeployProcessor Exception", e);
            }
            return false;
        }

        return true;
    }


    /**
     * Called after the unzip of the processor
     *
     * @param sProcessorFolder
     */
    protected void onAfterUnzipProcessor(String sProcessorFolder) {

    }

    /**
     * Called after the template is copied in the processor folder
     *
     * @param sProcessorFolder
     */
    protected void onAfterCopyTemplate(String sProcessorFolder) {

    }

    /**
     * Called after the deploy is done
     *
     * @param sProcessorFolder
     */
    protected void onAfterDeploy(String sProcessorFolder) {

    }
    
    /**
     * Run a Docker Processor
     */
    public boolean run(ProcessorParameter oParameter) {
    	return run(oParameter, true);
    }



    /**
     * Run a Docker Processor
     * 
     * @param oParameter Processo Parameter
     * @param bBuildLocally True to build locally, false otherwise
     * @return
     */
    @SuppressWarnings("unchecked")
    public boolean run(ProcessorParameter oParameter, boolean bBuildLocally) {

        WasdiLog.debugLog("DockerProcessorEngine.run: start");

        if (oParameter == null) {
            WasdiLog.errorLog("DockerProcessorEngine.run: parameter is null");
            return false;
        }

        // Get Repo and Process Workspace
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = m_oProcessWorkspace;

        try {

            // Check workspace folder
            String sWorkspacePath = PathsConfig.getWorkspacePath(oParameter);

            File oWorkspacePath = new File(sWorkspacePath);

            if (!oWorkspacePath.exists()) {
                try {
                    oWorkspacePath.mkdirs();
                } catch (Exception oWorkspaceFolderException) {
                    WasdiLog.errorLog("DockerProcessorEngine.run: exception creating ws: " + oWorkspaceFolderException);
                }
            }

            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

            // First Check if processor exists
            String sProcessorName = oParameter.getName();
            String sProcessorId = oParameter.getProcessorID();

            ProcessorRepository oProcessorRepository = new ProcessorRepository();
            Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

            // Check processor
            if (oProcessor == null) {
            	WasdiLog.errorLog("DockerProcessorEngine.run: Impossible to find processor " + sProcessorId);
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
                return false;                    
            }

            // Check if the processor is available on the node
            if (!isProcessorOnNode(oParameter)) {
                WasdiLog.infoLog("DockerProcessorEngine.run: processor not available on node download it");
                
                m_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.INFO.name(), m_oParameter.getExchange(), "APP NOT ON NODE<BR>INSTALLATION STARTED", m_oParameter.getExchange());

                String sProcessorZipFile = downloadProcessor(oProcessor, oParameter.getSessionID());

                WasdiLog.infoLog("DockerProcessorEngine.run: processor zip file downloaded: " + sProcessorZipFile);
                
                if (Utils.isNullOrEmpty(sProcessorZipFile)) {
                    WasdiLog.errorLog("DockerProcessorEngine.run: processor not available on node and not downloaded: exit.. ");
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
                    return false;                	
                }
                
                // Now we need or to redeploy or to unzip locally
            	boolean bResult = true;
            	
            	if (bBuildLocally) {
            		bResult = deploy(oParameter, false);
            	}
            	else {
            		File oZipFile = new File(sProcessorZipFile);
            		bResult = unzipProcessor(PathsConfig.getProcessorFolder(oProcessor), oZipFile.getName(), oParameter.getProcessObjId());
            	}
            	
            	if (!bResult) {
                    WasdiLog.errorLog("DockerProcessorEngine.run: impossible to deploy locally or unzip. We stop here");
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
                    return false;                		
            	}
                
                m_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.INFO.name(), m_oParameter.getExchange(), "INSTALLATION DONE<BR>STARTING APP", m_oParameter.getExchange());                    
            }

            // Create the Docker Utils Object
            DockerUtils oDockerUtils = new DockerUtils(oProcessor, PathsConfig.getProcessorFolder(sProcessorName), m_sTomcatUser, m_sDockerRegistry);
            
            // Check if the container is started
            boolean bIsContainerStarted = oDockerUtils.isContainerStarted(sProcessorName, oProcessor.getVersion());
            
            if (!bIsContainerStarted) {
            	WasdiLog.debugLog("DockerProcessorEngine.run: the container must be started");
            	
                // Try to start Again the docker
                oDockerUtils.start();
                
                // Wait for it
                waitForApplicationToStart(oParameter);                
                
            	WasdiLog.debugLog("DockerProcessorEngine.run: Processor just built and started, reconstruct the environment");
            	reconstructEnvironment(oParameter, oProcessor.getPort());
            }

            // Decode JSON
            String sEncodedJson = oParameter.getJson();
            String sJson = java.net.URLDecoder.decode(sEncodedJson, "UTF-8");

            WasdiLog.debugLog("DockerProcessorEngine.run: calling " + sProcessorName + " at port " + oProcessor.getPort());

            // Json sanity check
            if (Utils.isNullOrEmpty(sJson)) {
                sJson = "{}";
            }

            // Json sanity check
            if (sJson.equals("\"\"")) {
                sJson = "{}";
            }
            
            String sJsonCopy = sJson;
            
            if (sJsonCopy.length()>200) {
            	sJsonCopy = sJsonCopy.substring(0, 200);
            	sJsonCopy += "\n ... [TRUNCATED]";
            }

            WasdiLog.debugLog("DockerProcessorEngine.run: Decoded JSON Parameter " + sJsonCopy);

            // Call localhost:port
            String sUrl = "http://" + WasdiConfig.Current.dockers.internalDockersBaseAddress + ":" + oProcessor.getPort() + "/run/" + oParameter.getProcessObjId();

            sUrl += "?user=" + oParameter.getUserId();
            sUrl += "&sessionid=" + oParameter.getSessionID();
            sUrl += "&workspaceid=" + oParameter.getWorkspace();

            WasdiLog.debugLog("DockerProcessorEngine.run: calling URL = " + sUrl);

            // Create connection
            URL oProcessorUrl = new URL(sUrl);
            
			int iConnectionTimeOut = WasdiConfig.Current.connectionTimeout;
			int iReadTimeOut = WasdiConfig.Current.readTimeout;
			            

            WasdiLog.debugLog("DockerProcessorEngine.run: call open connection");
            HttpURLConnection oConnection = (HttpURLConnection) oProcessorUrl.openConnection();
            oConnection.setDoOutput(true);
            oConnection.setRequestMethod("POST");
            oConnection.setRequestProperty("Content-Type", "application/json");
            oConnection.setConnectTimeout(iConnectionTimeOut);
            oConnection.setReadTimeout(iReadTimeOut);

            // Try to fetch the result from docker
            OutputStream oOutputStream = oConnection.getOutputStream();
            oOutputStream.write(sJson.getBytes());
            oOutputStream.flush();
            if (!(oConnection.getResponseCode() == HttpURLConnection.HTTP_OK || oConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED)) {
                logErrorMessageFromConnection(oConnection);
                throw new Exception("DockerProcessorEngine.run: response code is: " + oConnection.getResponseCode());
            }

            // Get Result from server
            BufferedReader oBufferedReader = new BufferedReader(new InputStreamReader((oConnection.getInputStream())));

            String sJsonOutput = "";
            String sOutputResult;

            WasdiLog.debugLog("DockerProcessorEngine.run: Output from Server .... \n");

            while ((sOutputResult = oBufferedReader.readLine()) != null) {
                WasdiLog.debugLog("DockerProcessorEngine.run: " + sOutputResult);
                sJsonOutput += sOutputResult;
            }

            WasdiLog.debugLog("DockerProcessorEngine.run: out from the read Line loop");

            oConnection.disconnect();

            // Read Again Process Workspace: the user may have changed it!
            oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());

            // Here we can wait for the process to finish with the status check
            // we can also handle a timeout, that is property (with default) of the processor
            String sStatus = oProcessWorkspace.getStatus();

            WasdiLog.debugLog("DockerProcessorEngine.run: process Status: " + sStatus);

            WasdiLog.debugLog("DockerProcessorEngine.run: process output: " + sJsonOutput);

            Map<String, String> oOutputJsonMap = null;

            try {
                ObjectMapper oMapper = new ObjectMapper();
                oOutputJsonMap = oMapper.readValue(sJsonOutput, Map.class);
            } catch (Exception oEx) {
                WasdiLog.debugLog("DockerProcessorEngine.run: exception converting proc output in Json " + oEx);
            }

            // Check if it is a processor > 1.0:
            // first processors where blocking: docker server waited for the execution to end before going back to the launcher
            // New processors (>=2.0) are asynch: returns just Engine Version
            if (oOutputJsonMap != null) {

                // Check if we have the engine version
                if (oOutputJsonMap.containsKey("processorEngineVersion")) {

                    String sProcessorEngineVersion = oOutputJsonMap.get("processorEngineVersion");

                    // Try to convert the version
                    double dVersion = 1.0;

                    try {
                        dVersion = Double.parseDouble(sProcessorEngineVersion);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    WasdiLog.debugLog("DockerProcessorEngine.run: processor engine version " + dVersion);

                    // New, Asynch, Processor?
                    if (dVersion > 1.0) {
                        // Yes

                        // Check the processId
                        String sProcId = oOutputJsonMap.get("processId");
                        sStatus = waitForApplicationToFinish(oProcessor, sProcId, sStatus, oProcessWorkspace);

                    } else {
                        // Old processor engine: force safe status
                        WasdiLog.debugLog("DockerProcessorEngine.run: processor engine v1.0 - force process as done");
                        LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
                    }
                } else {
                    // Old processor engine: force safe status
                    WasdiLog.debugLog("DockerProcessorEngine.run: processor engine v1.0 - force process as done");
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
                }
            } else {
                // Old processor engine: force safe status
                WasdiLog.debugLog("DockerProcessorEngine.run: impossible to read processor output in a json. Force closed");
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
            }

            // Check and set the operation end-date
            if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
                oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
                // P.Campanella 20200115: I think this is to add, but I cannot test it now :( ...
                //LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.valueOf(oProcessWorkspace.getStatus()), oProcessWorkspace.getProgressPerc());
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("DockerProcessorEngine.run Exception", oEx);
            try {
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
            } catch (Exception oInnerEx) {
                WasdiLog.errorLog("DockerProcessorEngine.run Exception", oInnerEx);
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
     * Deletes a docker image
     */
    public boolean delete(ProcessorParameter oParameter) {
        // Get the docker Id or name from the param; we should save it in the build or run
        // call docker rmi -f <containerId>

        if (oParameter == null) {
            WasdiLog.errorLog("DockerProcessorEngine.delete: oParameter is null");
            return false;
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

            WasdiLog.infoLog("Delete Processor " + sProcessorName + " ID: " + sProcessorId);

            ProcessorRepository oProcessorRepository = new ProcessorRepository();
            Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

            // Check processor
            if (oProcessor == null) {
                processWorkspaceLog("Processor in the db is already null, try to delete docker and folder ");
                WasdiLog.errorLog("DockerProcessorEngine.delete: oProcessor in the db is already null [" + sProcessorId + "], try to delete docker and folder");
            } else {
                if (!oParameter.getUserId().equals(oProcessor.getUserId())) {
                    WasdiLog.errorLog("DockerProcessorEngine.delete: oProcessor is not of user [" + oParameter.getUserId() + "]. Exit");
                    return false;
                }
            }

            String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);

            File oProcessorFolder = new File(sProcessorFolder);

            processWorkspaceLog("Delete Processor Docker");

            DockerUtils oDockerUtils = new DockerUtils(oProcessor, sProcessorFolder, m_sTomcatUser);
            // Set also the docker registry
            oDockerUtils.setDockerRegistry(m_sDockerRegistry);
            // Give the name of the processor to delete to be sure that it works also if oProcessor is already null
            oDockerUtils.delete(sProcessorName, oParameter.getVersion());

            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 33);

            // delete the folder
            processWorkspaceLog("Delete Processor Folder");
            FileUtils.deleteDirectory(oProcessorFolder);
            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 66);

            if (oProcessor != null) {
                // delete the db entry
                oProcessorRepository.deleteProcessor(sProcessorId);
            }

            // Check and set the operation end-date
            if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
                oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
            }

            try {
                DeleteProcessorPayload oDeletePayload = new DeleteProcessorPayload();
                oDeletePayload.setProcessorName(sProcessorName);
                oDeletePayload.setProcessorId(sProcessorId);
                oProcessWorkspace.setPayload(LauncherMain.s_oMapper.writeValueAsString(oDeletePayload));
            } catch (Exception oPayloadException) {
                WasdiLog.errorLog("DockerProcessorEngine.delete Exception creating payload ", oPayloadException);
            }

            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

            processWorkspaceLog("Processor Deleted");
            processWorkspaceLog(new EndMessageProvider().getGood());

            return true;
        } catch (Exception oEx) {

            processWorkspaceLog("There was an error deleting the processor");
            processWorkspaceLog(new EndMessageProvider().getBad());

            WasdiLog.errorLog("DockerProcessorEngine.delete Exception", oEx);
            try {

                if (oProcessWorkspace != null) {
                    // Check and set the operation end-date
                    if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
                        oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
                    }

                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
            } catch (Exception e) {
                WasdiLog.errorLog("DockerProcessorEngine.delete Exception", e);
            }

            return false;
        }
    }
    
    
    /**
     * Re-Deploy an existing processor
     *
     * @param oParameter Processor Parameter
     * @return true if all ok
     */
    public boolean redeploy(ProcessorParameter oParameter) {
    	return redeploy(oParameter, true);
    }

    /**
     * Re-Deploy an existing processor
     *
     * @param oParameter Processor Parameter
     * @param bDeleteOldImage True to delete the old image, false otherwise
     * @return true if all ok
     */
    public boolean redeploy(ProcessorParameter oParameter, boolean bDeleteOldImage) {

        if (oParameter == null) {
            WasdiLog.errorLog("DockerProcessorEngine.redeploy: oParameter is null");
            return false;
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
                WasdiLog.errorLog("DockerProcessorEngine.redeploy: oProcessor is null [" + sProcessorId + "]");
                return false;
            }

            String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);

            WasdiLog.infoLog("DockerProcessorEngine.redeploy: update docker for " + sProcessorName);

            onAfterUnzipProcessor(sProcessorFolder);

            // Copy Docker template files in the processor folder
            File oDockerTemplateFolder = new File(m_sDockerTemplatePath);
            File oProcessorFolder = new File(sProcessorFolder);

            FileUtils.copyDirectory(oDockerTemplateFolder, oProcessorFolder);

            onAfterCopyTemplate(sProcessorFolder);
            
            // Delete the run script: it will be recreated at the right moment
            WasdiFileUtils.deleteFile(sProcessorFolder+"runwasdidocker.sh");

            // Create utils
            DockerUtils oDockerUtils = new DockerUtils(oProcessor, sProcessorFolder, m_sTomcatUser, m_sDockerRegistry);

            if (bDeleteOldImage) {
                // Delete the image
                WasdiLog.infoLog("DockerProcessorEngine.redeploy: delete the container");
                oDockerUtils.delete();            	
            }

            // Create again
            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 33);
            WasdiLog.infoLog("DockerProcessorEngine.redeploy: deploy the image");
            m_sDockerImageName = oDockerUtils.build();

            onAfterDeploy(sProcessorFolder);

            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

            WasdiLog.infoLog("DockerProcessorEngine.redeploy: docker " + sProcessorName + " updated");
            return true;
        } catch (Exception oEx) {
            WasdiLog.errorLog("DockerProcessorEngine.redeploy Exception", oEx);
            try {
                if (oProcessWorkspace != null) {
                    // Check and set the operation end-date
                    if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
                        oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
                    }

                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
            } catch (Exception e) {
                WasdiLog.errorLog("DockerProcessorEngine.redeploy Exception", e);
            }

            return false;
        }
    }

    @Override
    public boolean libraryUpdate(ProcessorParameter oParameter) {

        if (oParameter == null) {
            WasdiLog.errorLog("DockerProcessorEngine.libraryUpdate: oParameter is null");
            return false;
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
                WasdiLog.errorLog("DockerProcessorEngine.libraryUpdate: oProcessor is null [" + sProcessorId + "]");
                return false;
            }

            WasdiLog.infoLog("DockerProcessorEngine.libraryUpdate: update lib for " + sProcessorName);

            // Call localhost:port
            String sUrl = "http://" + WasdiConfig.Current.dockers.internalDockersBaseAddress + ":" + oProcessor.getPort() + "/run/--wasdiupdate";

            // Connect to the docker
            URL oProcessorUrl = new URL(sUrl);
            HttpURLConnection oConnection = (HttpURLConnection) oProcessorUrl.openConnection();
            oConnection.setDoOutput(true);
            oConnection.setRequestMethod("POST");
            oConnection.setRequestProperty("Content-Type", "application/json");
            OutputStream oOutputStream = oConnection.getOutputStream();
            oOutputStream.write("{}".getBytes());
            oOutputStream.flush();

            if (!(oConnection.getResponseCode() == HttpURLConnection.HTTP_OK || oConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED)) {
                return false;
            }
            
            BufferedReader oBufferedReader = new BufferedReader(new InputStreamReader((oConnection.getInputStream())));
            String sOutputResult;
            String sOutputCumulativeResult = "";
            WasdiLog.infoLog("DockerProcessorEngine.libraryUpdate: Output from Server .... \n");
            while ((sOutputResult = oBufferedReader.readLine()) != null) {
            	WasdiLog.infoLog("DockerProcessorEngine.libraryUpdate: " + sOutputResult);

                if (!Utils.isNullOrEmpty(sOutputResult)) sOutputCumulativeResult += sOutputResult;
            }
            oConnection.disconnect();

            waitForApplicationToStart(oParameter);

			if (WasdiConfig.Current.isMainNode()) {
				refreshPackagesInfo(oParameter);
			}

            WasdiLog.infoLog("DockerProcessorEngine.libraryUpdate: lib updated");

            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

            return true;
        } catch (Exception oEx) {
            WasdiLog.errorLog("DockerProcessorEngine.libraryUpdate Exception", oEx);

            return false;
        }
        finally {
            try {

                if (oProcessWorkspace != null) {
                    // Check and set the operation end-date
                    if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
                        oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
                    }

                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
            } catch (Exception e) {
                WasdiLog.errorLog("DockerProcessorEngine.libraryUpdate Exception", e);
            }
        	
        }
    }
    
    /**
     * Get the specific instance of the Package Manager compatible with this instance
     * @param sIp
     * @param iPort
     * @return
     */
    protected abstract IPackageManager getPackageManager(String sIp, int iPort);

    /**
     * Updates the processor environment
     */
	@Override
	public boolean environmentUpdate(ProcessorParameter oParameter) {

		if (oParameter == null) {
			WasdiLog.errorLog("DockerProcessorEngine.environmentUpdate: oParameter is null");
			return false;
		}

		if (Utils.isNullOrEmpty(oParameter.getJson())) {
			WasdiLog.errorLog("DockerProcessorEngine.environmentUpdate: update command is null or empty");
			return false;
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
				WasdiLog.errorLog("DockerProcessorEngine.environmentUpdate: oProcessor is null [" + sProcessorId + "]");
				return false;
			}

			WasdiLog.infoLog("DockerProcessorEngine.environmentUpdate: update env for " + sProcessorName);

			String sJson = oParameter.getJson();
			WasdiLog.debugLog("DockerProcessorEngine.environmentUpdate: sJson: " + sJson);
			JSONObject oJsonItem = new JSONObject(sJson);

			Object oUpdateCommand = oJsonItem.get("updateCommand");

			if (oUpdateCommand == null || oUpdateCommand.equals(org.json.JSONObject.NULL)) {
				WasdiLog.debugLog("DockerProcessorEngine.environmentUpdate: refresh of the list of libraries.");
			} else {
				String sUpdateCommand = (String) oUpdateCommand;
				WasdiLog.debugLog("DockerProcessorEngine.environmentUpdate: sUpdateCommand: " + sUpdateCommand);

				String sIp = WasdiConfig.Current.dockers.internalDockersBaseAddress;
				int iPort = oProcessor.getPort();

				IPackageManager oPackageManager = getPackageManager(sIp, iPort);
				oPackageManager.operatePackageChange(sUpdateCommand);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("DockerProcessorEngine.environmentUpdate Exception", oEx);
			try {

				if (oProcessWorkspace != null) {
					// Check and set the operation end-date
					if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
						oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
					}

					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				}
			} catch (Exception e) {
				WasdiLog.errorLog("DockerProcessorEngine.environmentUpdate Exception", e);
			}

			return false;
		}
	}


	/**
	 * Force the refresh of the packagesInfo.json file. Ideally, the file should be refreshed after every update operation.
	 * @param oParameter the processor parameter
	 * @param iPort port of the processor server
	 * @return
	 */	
	public boolean refreshPackagesInfo(ProcessorParameter oParameter) {
		if (oParameter == null) {
			WasdiLog.errorLog("DockerProcessorEngine.refreshPackagesInfo: oParameter is null");
			return false;
		}

		String sProcessorName = oParameter.getName();
		String sProcessorId = oParameter.getProcessorID();

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

		// Set the processor path
		String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);
		File oProcessorFolder = new File(sProcessorFolder);

		// Is the processor installed in this node?
		if (!oProcessorFolder.exists()) {
			WasdiLog.errorLog("DockerProcessorEngine.refreshPackagesInfo: Processor [" + sProcessorName + "] environment not updated in this node, return");
			return false;
		}
		

		String sIp = WasdiConfig.Current.dockers.internalDockersBaseAddress;
		int iPort = oProcessor.getPort();

        // Create the Docker Utils Object
        DockerUtils oDockerUtils = new DockerUtils(oProcessor, PathsConfig.getProcessorFolder(sProcessorName), m_sTomcatUser, m_sDockerRegistry);
        
        // Check if the container is started
        boolean bIsContainerStarted = oDockerUtils.isContainerStarted(sProcessorName, oProcessor.getVersion());
        
        if (!bIsContainerStarted) {
        	WasdiLog.debugLog("DockerProcessorEngine.refreshPackagesInfo: Container not started: starting it");
        	oDockerUtils.start();
        	waitForApplicationToStart(oParameter);
        	this.reconstructEnvironment(oParameter, oProcessor.getPort());
        }
        
		try {
			IPackageManager oPackageManager = getPackageManager(sIp, iPort);

			Map<String, Object> aoPackagesInfo = oPackageManager.getPackagesInfo();

			String sFileFullPath = sProcessorFolder + "packagesInfo.json";

			boolean bResult = WasdiFileUtils.writeMapAsJsonFile(aoPackagesInfo, sFileFullPath);

			if (!bResult) {
				WasdiLog.errorLog("DockerProcessorEngine.refreshPackagesInfo: the packagesInfo.json file was not created.");
			}

			return bResult;
		} catch (Exception oEx) {
			WasdiLog.errorLog("DockerProcessorEngine.refreshPackagesInfo: ", oEx);
		}

		return false;
	}


	/**
	 * Waits some time to let application start
	 * 
	 * @param oParameter Processor Paramter
	 */
	public void waitForApplicationToStart(ProcessorParameter oParameter) {
		try {
	        WasdiLog.debugLog("DockerProcessorEngine.waitForApplicationToStart: wait to let docker start");

	        Integer iNumberOfAttemptsToPingTheServer = WasdiConfig.Current.dockers.numberOfAttemptsToPingTheServer;
	        Integer iMillisBetweenAttmpts = WasdiConfig.Current.dockers.millisBetweenAttmpts;

	        for (int i = 0; i < iNumberOfAttemptsToPingTheServer; i++) {
	        	
	        	Thread.sleep(iMillisBetweenAttmpts);
	        	
	        	if (isDockerServerUp(oParameter)) {
	        		return;
	        	}
	        }
	        
	        WasdiLog.debugLog("DockerProcessorEngine.waitForApplicationToStart: attemps finished.. probably did not started!");
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("DockerProcessorEngine.waitForApplicationToStart: exception " + oEx.toString());
		}
	}
	
    /**
     * Wait for a processor to finish.
     *
     * The method polls the status of the process workspace to detect when the app is finished.
     * It implements also an internal timeout based on the max time allowed declared by the processor itself 
     * 
     * @param oProcessor Processor Entity
     * @param sProcId Process Workspace Id
     * @param sStatus Status
     * @param oProcessWorkspace Process Workspace Entity
     * @return New Status
     */
    protected String waitForApplicationToFinish(Processor oProcessor, String sProcId, String sStatus, ProcessWorkspace oProcessWorkspace) {
    	
    	WasdiLog.debugLog("DockerProcessorEngine.waitForApplicationToFinish: wait for the processor to finish");
    	
    	try {
        	ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        	
            if (sProcId.equals("ERROR")) {
                // Force cycle to exit, leave flag as it is to send a rabbit message
                sStatus = ProcessStatus.ERROR.name();
                oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
                return sStatus;
            }
            
            long lTimeSpentMs = 0;
            int iThreadSleepMs = 2000;
            boolean bForcedError = false;

            // Wait for the process to finish, while checking timeout
            while (!(sStatus.equals("DONE") || sStatus.equals("STOPPED") || sStatus.equals("ERROR"))) {
                oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());

                sStatus = oProcessWorkspace.getStatus();
                try {
                    Thread.sleep(iThreadSleepMs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }

                // Increase the time
                lTimeSpentMs += iThreadSleepMs;

                if (oProcessor.getTimeoutMs() > 0) {
                    if (lTimeSpentMs > oProcessor.getTimeoutMs()) {
                        // Timeout
                        WasdiLog.debugLog("DockerProcessorEngine.waitForApplicationToFinish: Timeout of Processor with ProcId " + oProcessWorkspace.getProcessObjId() + " Time spent [ms] " + lTimeSpentMs);

                        // Update process and rabbit users
                        LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                        bForcedError = true;
                        // Force cycle to exit
                        sStatus = ProcessStatus.ERROR.name();
                    }
                }
            }

            // The process finished: alone or forced?
            if (!bForcedError) {
                // Alone: write again the status to be sure to update rabbit users
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.valueOf(oProcessWorkspace.getStatus()), oProcessWorkspace.getProgressPerc());
            }
            
            WasdiLog.debugLog("DockerProcessorEngine.waitForApplicationToFinish: processor done");
            
            return sStatus;    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerProcessorEngine.waitForApplicationToFinish error ", oEx);
    		return ProcessStatus.ERROR.name();
		}
    }

    /**
     * Logs an error obtained by an Http Connection 
     * @param oConnection
     * @throws IOException
     * @throws Exception
     */
    protected void logErrorMessageFromConnection(HttpURLConnection oConnection) {

    	try {
            InputStream oErrorStream = oConnection.getErrorStream();
            try (Reader oReader = new InputStreamReader(oErrorStream)) {
                String sMessage = CharStreams.toString(oReader);
                WasdiLog.errorLog("DockerProcessorEngine.logErrorMessageFromConnection: connection failed with " + oConnection.getResponseCode() + ": " + sMessage);
            }    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerProcessorEngine.logErrorMessageFromConnection: error ", oEx);
		}
    }	
    
    /**
     * Log an error message both on node log files and on the online web interface.
     * If it is the first deploy try to clean the processor from the db and closes the process workspace
     * 
     * @param sLogMessage Message to log
     * @param bFirstDeploy True if this is a first deploy
     */
    protected boolean logDeployErrorAndClean(String sLogMessage, boolean bFirstDeploy) {
    	
    	try {
            WasdiLog.errorLog("DockerProcessorEngine.logDeployErrorAndClean: " + sLogMessage);

            processWorkspaceLog(sLogMessage);
            processWorkspaceLog(new EndMessageProvider().getBad());

            if (bFirstDeploy) {
            	
            	if (m_oParameter!=null) {
            		ProcessorRepository oProcessorRepository = new ProcessorRepository();
            		oProcessorRepository.deleteProcessor(m_oParameter.getProcessorID());
            	}
            	
            	ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.ERROR, 100);
            }    		
    	}
    	catch (Exception oEx) {
			WasdiLog.errorLog("DockerProcessorEngine.logDeployErrorAndClean: error " + oEx.toString());
		}
    
    	return false;
    }
    
    /**
     * Check if the internal server on the docker is up
     * @param oParameter
     * @return
     */
	public boolean isDockerServerUp(ProcessorParameter oParameter) {
		try {
			String sProcessorId = oParameter.getProcessorID();

			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

			String sIp = WasdiConfig.Current.dockers.internalDockersBaseAddress;
			int iPort = oProcessor.getPort();


			String sUrl = "http://" + sIp + ":" + iPort + "/hello";
			WasdiLog.debugLog("DockerProcessorEngine.isDockerServerUp: poll sUrl: " + sUrl);

			Map<String, String> asHeaders = Collections.emptyMap();

			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders);
			Integer iResult = oHttpCallResponse.getResponseCode();

			WasdiLog.debugLog("DockerProcessorEngine.isDockerServerUp: got iResult: " + iResult);

			return (iResult != null && iResult.intValue() == 200);
		} catch (Exception oEx) {
			WasdiLog.errorLog("DockerProcessorEngine.isDockerServerUp: exception " + oEx.toString());
		}

		return false;
	}

	@Override
	protected boolean reconstructEnvironment(ProcessorParameter oParameter, int iPort) {
		
		boolean bRet = true;
		
		try {
			
			// We need to reach the main server
			String sBaseUrl = WasdiConfig.Current.baseUrl;
			
			if (!sBaseUrl.endsWith("/")) sBaseUrl += "/";
			String sUrl = sBaseUrl + "packageManager/environmentActions?name=" + oParameter.getName();
			
			// Create the headers
			Map<String, String> asHeaders = HttpUtils.getStandardHeaders(oParameter.getSessionID());
			
			WasdiLog.debugLog("DockerProcessorEngine.reconstructEnvironment: calling url " + sUrl);
			
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders);
			// Call the API to get the lastest action list
			String sResult = oHttpCallResponse.getResponseBody();
			
			// Convert to an array of strings
			ArrayList<String> asActions = MongoRepository.s_oMapper.readValue(sResult, new TypeReference<ArrayList<String>>(){});
			
			// Do we have actions?
			if (asActions.size()>0) {
				
				WasdiLog.debugLog("DockerProcessorEngine.reconstructEnvironment: got " + asActions.size() + " actions");
				
				// Yes! Lets re-do all
				
				// Take the ip
				String sIp = WasdiConfig.Current.dockers.internalDockersBaseAddress;
				
				// Create package manager info
				IPackageManager oPackageManager = getPackageManager(sIp, iPort);
				
				// For each command
				for (String sUpdateCommand : asActions) {
					
					WasdiLog.debugLog("DockerProcessorEngine.reconstructEnvironment: executing " + sUpdateCommand);
					bRet &= oPackageManager.operatePackageChange(sUpdateCommand);
					
					if (!bRet) {
						WasdiLog.debugLog("DockerProcessorEngine.reconstructEnvironment: error executing " + sUpdateCommand);
						break;
					}
				}
				
			}
			else {
				WasdiLog.debugLog("DockerProcessorEngine.reconstructEnvironment: no actions to do");
			}
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("DockerProcessorEngine.reconstructEnvironment: exception " + oEx.toString());
		}
		
		// execute all the ops with the binded Package Manager for the app
		WasdiLog.debugLog("DockerProcessorEngine.reconstructEnvironment: done");
		
		return bRet;
	}
	
	/**
	 * Push one image in all the registers
	 * @param oProcessor Processor
	 * @return name of the image
	 */
	protected String pushImageInRegisters(Processor oProcessor) {
		try {
			List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
			
			// And get the processor folder
			String sProcessorFolder = PathsConfig.getProcessorFolder(oProcessor.getName());
			
			// Create the docker utils
			DockerUtils oDockerUtils = new DockerUtils(oProcessor, sProcessorFolder, m_sTomcatUser);
			
			// Here we keep track of how many registers we tried
			int iAvailableRegisters=0;
			// Here we save the address of the image
			String sPushedImageAddress = "";
			
			// For each register: ordered by priority
			for (; iAvailableRegisters<aoRegisters.size(); iAvailableRegisters++) {
				
				DockerRegistryConfig oDockerRegistryConfig = aoRegisters.get(iAvailableRegisters);
				
				WasdiLog.debugLog("DockerProcessorEngine.pushImageInRegisters: try to push to " + oDockerRegistryConfig.id);
				
				// Try to login and push
				sPushedImageAddress = loginAndPush(oDockerUtils, oDockerRegistryConfig, m_sDockerImageName, sProcessorFolder);
				
				if (!Utils.isNullOrEmpty(sPushedImageAddress)) {
					WasdiLog.debugLog("DockerProcessorEngine.pushImageInRegisters: image pushed");
					// Ok we got a valid address!
					break;
				}
			}
			
			// Did we used all the avaialbe options?
			if (iAvailableRegisters<aoRegisters.size()) {
				
				PushDockerImagesThread oPushDockerImagesThread = new PushDockerImagesThread();
				oPushDockerImagesThread.setProcessor(oProcessor);
				
				for (int iOtherRegisters = 0; iOtherRegisters<aoRegisters.size(); iOtherRegisters++) {
					oPushDockerImagesThread.getRegisters().add(aoRegisters.get(iOtherRegisters));
				}
				
				WasdiLog.debugLog("DockerProcessorEngine.pushImageInRegisters: staring thread to push on other regitries");
				oPushDockerImagesThread.start();
			}
			
			return sPushedImageAddress;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("DockerProcessorEngine.pushImageInRegisters: error " + oEx.toString());
		}
		
		return "";
	}
	
	
	/**
	 * Log in and Push an image to a Docker Registry
	 * @param oDockerUtils
	 * @param oDockerRegistryConfig
	 * @param sImageName
	 * @return
	 */
	protected String loginAndPush(DockerUtils oDockerUtils, DockerRegistryConfig oDockerRegistryConfig, String sImageName, String sFolder) {
		try {
			// Login in the docker
			boolean bLogged = oDockerUtils.loginInRegistry(oDockerRegistryConfig.address, oDockerRegistryConfig.user, oDockerRegistryConfig.password, sFolder);
			
			if (!bLogged) {
				WasdiLog.debugLog("DockerProcessorEngine.loginAndPush: error logging in, return false.");
				return "";
			}
			
			// Push the image
			boolean bPushed = oDockerUtils.push(sImageName);
			
			if (!bPushed) {
				WasdiLog.debugLog("DockerProcessorEngine.loginAndPush: error in push, return false.");
				return "";				
			}
			
			return sImageName;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("DockerProcessorEngine.loginAndPush: Exception " + oEx.toString());
		}
		
		return "";
	}
	

}
