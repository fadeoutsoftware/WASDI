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
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;

import wasdi.LauncherMain;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.managers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.payloads.DeleteProcessorPayload;
import wasdi.shared.payloads.DeployProcessorPayload;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.viewmodels.HttpCallResponse;

public abstract class DockerProcessorEngine extends WasdiProcessorEngine {

	public DockerProcessorEngine() {
		super();
	}
	
    public DockerProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser) {
        super(sWorkingRootPath, sDockerTemplatePath, sTomcatUser);
    }

    /**
     * Deploy a new Processor in WASDI
     *
     * @param oParameter
     */
    @Override
    public boolean deploy(ProcessorParameter oParameter) {
        return deploy(oParameter, true);
    }

    /**
     * Deploy a new Processor in WASDI
     *
     * @param oParameter
     */
    public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {

        LauncherMain.s_oLogger.debug("DockerProcessorEngine.DeployProcessor: start");

        if (oParameter == null) return false;

        ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
        ProcessorRepository oProcessorRepository = new ProcessorRepository();
        ProcessWorkspace oProcessWorkspace = null;


        String sProcessorName = oParameter.getName();
        String sProcessorId = oParameter.getProcessorID();

        try {

            processWorkspaceLog("Start Deploy of " + sProcessorName + " Type " + oParameter.getProcessorType());

            oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
            oProcessWorkspace = m_oProcessWorkspace;

            if (bFirstDeploy) {
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
                processWorkspaceLog("This is a first deploy of this app");
            }

            // First Check if processor exists
            Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

            String sProcessorFolder = getProcessorFolder(sProcessorName);

            // Create the file
            File oProcessorZipFile = new File(sProcessorFolder + sProcessorId + ".zip");

            LauncherMain.s_oLogger.debug("DockerProcessorEngine.DeployProcessor: check processor exists");

            // Check it
            if (oProcessorZipFile.exists() == false) {
                LauncherMain.s_oLogger.debug("DockerProcessorEngine.DeployProcessor the Processor [" + sProcessorName + "] does not exists in path " + oProcessorZipFile.getPath());

                processWorkspaceLog("Cannot find the processor file... something went wrong");
                processWorkspaceLog(new EndMessageProvider().getBad());

                if (bFirstDeploy) {
                    oProcessorRepository.deleteProcessor(sProcessorId);
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
                return false;
            }

            if (bFirstDeploy)
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 2);
            LauncherMain.s_oLogger.error("DockerProcessorEngine.DeployProcessor: unzip processor");

            // Unzip the processor (and check for entry point myProcessor.py)
            if (!unzipProcessor(sProcessorFolder, sProcessorId + ".zip", oParameter.getProcessObjId())) {
                LauncherMain.s_oLogger.debug("DockerProcessorEngine.DeployProcessor error unzipping the Processor [" + sProcessorName + "]");

                processWorkspaceLog("Error unzipping the processor");
                processWorkspaceLog(new EndMessageProvider().getBad());

                if (bFirstDeploy) {
                    oProcessorRepository.deleteProcessor(sProcessorId);
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
                return false;
            }

            onAfterUnzipProcessor(sProcessorFolder);

            if (bFirstDeploy)
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 20);
            LauncherMain.s_oLogger.debug("DockerProcessorEngine.DeployProcessor: copy container image template");

            // Copy Docker template files in the processor folder
            File oDockerTemplateFolder = new File(m_sDockerTemplatePath);
            File oProcessorFolder = new File(sProcessorFolder);

            FileUtils.copyDirectory(oDockerTemplateFolder, oProcessorFolder);

            if (bFirstDeploy)
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 25);

            // Generate the image
            LauncherMain.s_oLogger.debug("DockerProcessorEngine.DeployProcessor: building image");
            onAfterCopyTemplate(sProcessorFolder);

            processWorkspaceLog("Start building Image");

            // Create Docker Util and deploy the docker
            DockerUtils oDockerUtils = new DockerUtils(oProcessor, sProcessorFolder, m_sWorkingRootPath, m_sTomcatUser);
            oDockerUtils.deploy();

            onAfterDeploy(sProcessorFolder);

            processWorkspaceLog("Image done, start the docker");

            if (bFirstDeploy)
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 70);

            // Run the container: find the port and reconstruct the environment
            int iProcessorPort = oProcessorRepository.getNextProcessorPort();
            if (!bFirstDeploy) {
                iProcessorPort = oProcessor.getPort();
            }

            oDockerUtils.run(iProcessorPort);

            processWorkspaceLog("Application started");

            if (bFirstDeploy) {
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 90);
                oProcessor.setPort(iProcessorPort);
                oProcessorRepository.updateProcessor(oProcessor);
            }
            else {
            	waitForApplicationToStart(oParameter);
            	reconstructEnvironment(oParameter, iProcessorPort);
            }

            try {
                DeployProcessorPayload oDeployPayload = new DeployProcessorPayload();
                oDeployPayload.setProcessorName(sProcessorName);
                oDeployPayload.setType(oParameter.getProcessorType());
                oProcessWorkspace.setPayload(LauncherMain.s_oMapper.writeValueAsString(oDeployPayload));
            } catch (Exception oPayloadException) {
                LauncherMain.s_oLogger.error("DockerProcessorEngine.DeployProcessor Exception creating payload ", oPayloadException);
            }

            if (bFirstDeploy)
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
            
            
            processWorkspaceLog(new EndMessageProvider().getGood());

        } catch (Exception oEx) {

            processWorkspaceLog("There was an error... sorry...");
            processWorkspaceLog(new EndMessageProvider().getBad());

            LauncherMain.s_oLogger.error("DockerProcessorEngine.DeployProcessor Exception", oEx);
            try {
                if (bFirstDeploy) {
                    try {
                        oProcessorRepository.deleteProcessor(sProcessorId);
                    } catch (Exception oInnerEx) {
                        LauncherMain.s_oLogger.error("DockerProcessorEngine.DeployProcessor Exception", oInnerEx);
                    }

                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
            } catch (Exception e) {
                LauncherMain.s_oLogger.error("DockerProcessorEngine.DeployProcessor Exception", e);
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
    @SuppressWarnings("unchecked")
    public boolean run(ProcessorParameter oParameter) {

        LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: start");

        if (oParameter == null) {
            LauncherMain.s_oLogger.error("DockerProcessorEngine.run: parameter is null");
            return false;
        }

        ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
        ProcessWorkspace oProcessWorkspace = null;

        try {

            // Get Repo and Process Workspace
            oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
            oProcessWorkspace = m_oProcessWorkspace;


            // Check workspace folder
            String sWorkspacePath = LauncherMain.getWorkspacePath(oParameter);

            File oWorkspacePath = new File(sWorkspacePath);

            if (!oWorkspacePath.exists()) {
                try {
                    LauncherMain.s_oLogger.info("DockerProcessorEngine.run: creating ws folder");
                    oWorkspacePath.mkdirs();
                } catch (Exception oWorkspaceFolderException) {
                    LauncherMain.s_oLogger.error("DockerProcessorEngine.run: exception creating ws: " + oWorkspaceFolderException);
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
                // Catch block will handle
                throw new Exception("DockerProcessorEngine.run: Impossible to find processor " + sProcessorId);
            }

            // Check if the processor is available on the node
            if (!isProcessorOnNode(oParameter)) {
                LauncherMain.s_oLogger.info("DockerProcessorEngine.run: processor not available on node download it");
                
                m_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.INFO.name(), m_oParameter.getExchange(), "APP NOT ON NODE<BR>INSTALLATION STARTED", m_oParameter.getExchange());

                String sProcessorZipFile = downloadProcessor(oProcessor, oParameter.getSessionID());

                LauncherMain.s_oLogger.info("DockerProcessorEngine.run: processor zip file downloaded: " + sProcessorZipFile);

                if (!Utils.isNullOrEmpty(sProcessorZipFile)) {
                    deploy(oParameter, false);
                    
                    m_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.INFO.name(), m_oParameter.getExchange(), "INSTALLATION DONE<BR>STARTING APP", m_oParameter.getExchange());
                    
                } else {
                    LauncherMain.s_oLogger.error("DockerProcessorEngine.run: processor not available on node and not downloaded: exit.. ");
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
                    return false;
                }
            }

            // Decode JSON
            String sEncodedJson = oParameter.getJson();
            String sJson = java.net.URLDecoder.decode(sEncodedJson, "UTF-8");

            LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: calling " + sProcessorName + " at port " + oProcessor.getPort());

            // Json sanity check
            if (Utils.isNullOrEmpty(sJson)) {
                sJson = "{}";
            }

            // Json sanity check
            if (sJson.equals("\"\"")) {
                sJson = "{}";
            }

            LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: Decoded JSON Parameter " + sJson);

            // Call localhost:port
            String sUrl = "http://" + WasdiConfig.Current.dockers.internalDockersBaseAddress + ":" + oProcessor.getPort() + "/run/" + oParameter.getProcessObjId();

            sUrl += "?user=" + oParameter.getUserId();
            sUrl += "&sessionid=" + oParameter.getSessionID();
            sUrl += "&workspaceid=" + oParameter.getWorkspace();

            LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: calling URL = " + sUrl);

            // Create connection
            URL oProcessorUrl = new URL(sUrl);
            
			int iConnectionTimeOut = WasdiConfig.Current.connectionTimeout;
			int iReadTimeOut = WasdiConfig.Current.readTimeout;
			            

            LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: call open connection");
            HttpURLConnection oConnection = (HttpURLConnection) oProcessorUrl.openConnection();
            oConnection.setDoOutput(true);
            oConnection.setRequestMethod("POST");
            oConnection.setRequestProperty("Content-Type", "application/json");
            oConnection.setConnectTimeout(iConnectionTimeOut);
            oConnection.setReadTimeout(iReadTimeOut);
            

            OutputStream oOutputStream = null;
            try {

                // Try to fetch the result from docker
                oOutputStream = oConnection.getOutputStream();
                oOutputStream.write(sJson.getBytes());
                oOutputStream.flush();
                if (!(oConnection.getResponseCode() == HttpURLConnection.HTTP_OK || oConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED)) {
                    printErrorMessageFromConnection(oConnection);
                    throw new Exception("DockerProcessorEngine.printErrorMessageFromConnection: response code is: " + oConnection.getResponseCode());
                }
            } catch (Exception oE) {
                LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: connection failed due to: " + oE + ", try to start container again");

                // Try to start Again the docker
                String sProcessorFolder = getProcessorFolder(sProcessorName);

                // Start it
                DockerUtils oDockerUtils = new DockerUtils(oProcessor, sProcessorFolder, m_sWorkingRootPath, m_sTomcatUser);
                oDockerUtils.run();
                
                // Wait a little bit
                waitForApplicationToStart(oParameter);

                // Try again the connection
                LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: connection failed: try to connect again");
                oProcessorUrl = new URL(sUrl);
                oConnection = (HttpURLConnection) oProcessorUrl.openConnection();
                oConnection.setDoOutput(true);
                oConnection.setRequestMethod("POST");
                oConnection.setRequestProperty("Content-Type", "application/json");

                oOutputStream = oConnection.getOutputStream();
                oOutputStream.write(sJson.getBytes());
                oOutputStream.flush();

                if (!(oConnection.getResponseCode() == HttpURLConnection.HTTP_OK || oConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED)) {
                    printErrorMessageFromConnection(oConnection);
                    // Nothing to do
                    throw new RuntimeException("Failed Again: HTTP error code : " + oConnection.getResponseCode());
                }

                LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: ok container recovered");
            }

            // Get Result from server
            BufferedReader oBufferedReader = new BufferedReader(new InputStreamReader((oConnection.getInputStream())));

            String sJsonOutput = "";
            String sOutputResult;

            LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: Output from Server .... \n");

            while ((sOutputResult = oBufferedReader.readLine()) != null) {
                LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: " + sOutputResult);
                sJsonOutput += sOutputResult;
            }

            LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: out from the read Line loop");

            oConnection.disconnect();

            // Read Again Process Workspace: the user may have changed it!
            oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());

            // Here we can wait for the process to finish with the status check
            // we can also handle a timeout, that is property (with default) of the processor
            long lTimeSpentMs = 0;
            int iThreadSleepMs = 2000;

            String sStatus = oProcessWorkspace.getStatus();

            LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: process Status: " + sStatus);

            LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: process output: " + sJsonOutput);

            Map<String, String> oOutputJsonMap = null;

            try {
                ObjectMapper oMapper = new ObjectMapper();
                oOutputJsonMap = oMapper.readValue(sJsonOutput, Map.class);
            } catch (Exception oEx) {
                LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: exception converting proc output in Json " + oEx);
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

                    LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: processor engine version " + dVersion);

                    // New, Asynch, Processor?
                    if (dVersion > 1.0) {

                        boolean bForcedError = false;

                        // Yes
                        LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: processor engine version > 1.0: wait for the processor to finish");

                        // Check the processId
                        String sProcId = oOutputJsonMap.get("processId");

                        if (sProcId.equals("ERROR")) {
                            // Force cycle to exit, leave flag as it is to send a rabbit message
                            sStatus = ProcessStatus.ERROR.name();
                            oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
                        }

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
                                    LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: Timeout of Processor with ProcId " + oProcessWorkspace.getProcessObjId() + " Time spent [ms] " + lTimeSpentMs);

                                    // Update process and rabbit users
                                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                                    bForcedError = true;
                                    // Force cycle to exit
                                    sStatus = ProcessStatus.ERROR.name();
                                }
                            }
                        }

                        // The process finished: alone of forced?
                        if (!bForcedError) {
                            // Alone: write again the status to be sure to update rabbit users
                            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.valueOf(oProcessWorkspace.getStatus()), oProcessWorkspace.getProgressPerc());
                        }

                        LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: processor done");

                    } else {
                        // Old processor engine: force safe status
                        LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: processor engine v1.0 - force process as done");
                        LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
                    }
                } else {
                    // Old processor engine: force safe status
                    LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: processor engine v1.0 - force process as done");
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
                }
            } else {
                // Old processor engine: force safe status
                LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: impossible to read processor output in a json. Force closed");
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
            }

            // Check and set the operation end-date
            if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
                oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
                // P.Campanella 20200115: I think this is to add, but I cannot test it now :( ...
                //LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.valueOf(oProcessWorkspace.getStatus()), oProcessWorkspace.getProgressPerc());
            }
        } catch (Exception oEx) {
            LauncherMain.s_oLogger.error("DockerProcessorEngine.run Exception", oEx);
            try {
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
            } catch (Exception oInnerEx) {
                LauncherMain.s_oLogger.error("DockerProcessorEngine.run Exception", oInnerEx);
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

    protected void printErrorMessageFromConnection(HttpURLConnection oConnection) throws IOException, Exception {

        InputStream oErrorStream = oConnection.getErrorStream();
        try (Reader reader = new InputStreamReader(oErrorStream)) {
            String sMessage = CharStreams.toString(reader);
            LauncherMain.s_oLogger.error("DockerProcessorEngine.printErrorMessageFromConnection: connection failed with " + oConnection.getResponseCode() + ": " + sMessage);
        }
    }

    public boolean delete(ProcessorParameter oParameter) {
        // Get the docker Id or name from the param; we should save it in the build or run
        // call docker rmi -f <containerId>

        if (oParameter == null) {
            LauncherMain.s_oLogger.error("DockerProcessorEngine.delete: oParameter is null");
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

            processWorkspaceLog("Delete Processor " + sProcessorName + " ID: " + sProcessorId);

            ProcessorRepository oProcessorRepository = new ProcessorRepository();
            Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

            // Check processor
            if (oProcessor == null) {
                processWorkspaceLog("Processor in the db is already null, try to delete docker and folder ");
                LauncherMain.s_oLogger.error("DockerProcessorEngine.delete: oProcessor in the db is already null [" + sProcessorId + "], try to delete docker and folder");
                //return false;
            } else {
                if (!oParameter.getUserId().equals(oProcessor.getUserId())) {
                    LauncherMain.s_oLogger.error("DockerProcessorEngine.delete: oProcessor is not of user [" + oParameter.getUserId() + "]. Exit");
                    return false;
                }
            }


            String sProcessorFolder = getProcessorFolder(sProcessorName);

            File oProcessorFolder = new File(sProcessorFolder);

            processWorkspaceLog("Delete Processor Docker");

            DockerUtils oDockerUtils = new DockerUtils(oProcessor, sProcessorFolder, m_sWorkingRootPath, m_sTomcatUser);
            // Give the name of the processor to delete to be sure that it works also if oProcessor is already null
            oDockerUtils.delete(sProcessorName);

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
                LauncherMain.s_oLogger.error("DockerProcessorEngine.delete Exception creating payload ", oPayloadException);
            }

            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

            processWorkspaceLog("Processor Deleted");
            processWorkspaceLog(new EndMessageProvider().getGood());

            return true;
        } catch (Exception oEx) {

            processWorkspaceLog("There was an error deleting the processor");
            processWorkspaceLog(new EndMessageProvider().getBad());

            LauncherMain.s_oLogger.error("DockerProcessorEngine.delete Exception", oEx);
            try {

                if (oProcessWorkspace != null) {
                    // Check and set the operation end-date
                    if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
                        oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
                    }

                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
            } catch (Exception e) {
                LauncherMain.s_oLogger.error("DockerProcessorEngine.delete Exception", e);
            }

            return false;
        }
    }

    /**
     * Deploy
     *
     * @param oParameter
     * @return
     */
    public boolean redeploy(ProcessorParameter oParameter) {

        if (oParameter == null) {
            LauncherMain.s_oLogger.error("DockerProcessorEngine.redeploy: oParameter is null");
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
                LauncherMain.s_oLogger.error("DockerProcessorEngine.redeploy: oProcessor is null [" + sProcessorId + "]");
                return false;
            }

            String sProcessorFolder = getProcessorFolder(sProcessorName);

            LauncherMain.s_oLogger.info("DockerProcessorEngine.redeploy: update docker for " + sProcessorName);

            onAfterUnzipProcessor(sProcessorFolder);

            // Copy Docker template files in the processor folder
            File oDockerTemplateFolder = new File(m_sDockerTemplatePath);
            File oProcessorFolder = new File(sProcessorFolder);

            FileUtils.copyDirectory(oDockerTemplateFolder, oProcessorFolder);

            onAfterCopyTemplate(sProcessorFolder);

            // Create utils
            DockerUtils oDockerUtils = new DockerUtils(oProcessor, sProcessorFolder, m_sWorkingRootPath, m_sTomcatUser);

            // Delete the image
            LauncherMain.s_oLogger.info("DockerProcessorEngine.redeploy: delete the container");
            oDockerUtils.delete();

            // Create again
            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 33);
            LauncherMain.s_oLogger.info("DockerProcessorEngine.redeploy: deploy the image");
            oDockerUtils.deploy();

            onAfterDeploy(sProcessorFolder);
            
            // Run
            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 66);
            LauncherMain.s_oLogger.info("DockerProcessorEngine.redeploy: run the container");
            oDockerUtils.run();
            
            
            // Recreate the user environment
            waitForApplicationToStart(oParameter);
            reconstructEnvironment(oParameter, oProcessor.getPort());
            
			if (WasdiConfig.Current.nodeCode.equals("wasdi")) {
				refreshPackagesInfo(oParameter);
			}            

            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

            LauncherMain.s_oLogger.info("DockerProcessorEngine.redeploy: docker " + sProcessorName + " updated");
            return true;
        } catch (Exception oEx) {
            LauncherMain.s_oLogger.error("DockerProcessorEngine.redeploy Exception", oEx);
            try {
                if (oProcessWorkspace != null) {
                    // Check and set the operation end-date
                    if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
                        oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
                    }

                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
            } catch (Exception e) {
                LauncherMain.s_oLogger.error("DockerProcessorEngine.redeploy Exception", e);
            }

            return false;
        }
    }

    @Override
    public boolean libraryUpdate(ProcessorParameter oParameter) {

        if (oParameter == null) {
            LauncherMain.s_oLogger.error("DockerProcessorEngine.libraryUpdate: oParameter is null");
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
                LauncherMain.s_oLogger.error("DockerProcessorEngine.libraryUpdate: oProcessor is null [" + sProcessorId + "]");
                return false;
            }

            LauncherMain.s_oLogger.info("DockerProcessorEngine.libraryUpdate: update lib for " + sProcessorName);

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
            LauncherMain.s_oLogger.info("DockerProcessorEngine.libraryUpdate: Output from Server .... \n");
            while ((sOutputResult = oBufferedReader.readLine()) != null) {
            	LauncherMain.s_oLogger.info("DockerProcessorEngine.libraryUpdate: " + sOutputResult);

                if (!Utils.isNullOrEmpty(sOutputResult)) sOutputCumulativeResult += sOutputResult;
            }
            oConnection.disconnect();


            waitForApplicationToStart(oParameter);

			if (WasdiConfig.Current.nodeCode.equals("wasdi")) {
				refreshPackagesInfo(oParameter);
			}

            LauncherMain.s_oLogger.info("DockerProcessorEngine.libraryUpdate: lib updated");

            LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

            return true;
        } catch (Exception oEx) {
            LauncherMain.s_oLogger.error("DockerProcessorEngine.libraryUpdate Exception", oEx);

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
                LauncherMain.s_oLogger.error("DockerProcessorEngine.libraryUpdate Exception", e);
            }
        	
        }
    }

    protected abstract IPackageManager getPackageManager(String sIp, int iPort);

	@Override
	public boolean environmentUpdate(ProcessorParameter oParameter) {

		if (oParameter == null) {
			LauncherMain.s_oLogger.error("DockerProcessorEngine.environmentUpdate: oParameter is null");
			return false;
		}

		if (Utils.isNullOrEmpty(oParameter.getJson())) {
			LauncherMain.s_oLogger.error("DockerProcessorEngine.environmentUpdate: update command is null or empty");
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
				LauncherMain.s_oLogger.error("DockerProcessorEngine.environmentUpdate: oProcessor is null [" + sProcessorId + "]");
				return false;
			}

			LauncherMain.s_oLogger.info("DockerProcessorEngine.environmentUpdate: update env for " + sProcessorName);

			String sJson = oParameter.getJson();
			LauncherMain.s_oLogger.debug("DockerProcessorEngine.environmentUpdate: sJson: " + sJson);
			JSONObject oJsonItem = new JSONObject(sJson);

			Object oUpdateCommand = oJsonItem.get("updateCommand");

			if (oUpdateCommand == null || oUpdateCommand.equals(org.json.JSONObject.NULL)) {
				LauncherMain.s_oLogger.debug("DockerProcessorEngine.environmentUpdate: refresh of the list of libraries.");
			} else {
				String sUpdateCommand = (String) oUpdateCommand;
				LauncherMain.s_oLogger.debug("DockerProcessorEngine.environmentUpdate: sUpdateCommand: " + sUpdateCommand);

				String sIp = WasdiConfig.Current.dockers.internalDockersBaseAddress;
				int iPort = oProcessor.getPort();

				IPackageManager oPackageManager = getPackageManager(sIp, iPort);
				oPackageManager.operatePackageChange(sUpdateCommand);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

			return true;
		} catch (Exception oEx) {
			LauncherMain.s_oLogger.error("DockerProcessorEngine.environmentUpdate Exception", oEx);
			try {

				if (oProcessWorkspace != null) {
					// Check and set the operation end-date
					if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
						oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
					}

					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				}
			} catch (Exception e) {
				LauncherMain.s_oLogger.error("DockerProcessorEngine.environmentUpdate Exception", e);
			}

			return false;
		}
	}

	public boolean refreshPackagesInfo(ProcessorParameter oParameter) {
		if (oParameter == null) {
			LauncherMain.s_oLogger.error("DockerProcessorEngine.refreshPackagesInfo: oParameter is null");
			return false;
		}

		String sProcessorName = oParameter.getName();
		String sProcessorId = oParameter.getProcessorID();

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);


		// Set the processor path
		String sProcessorFolder = this.getProcessorFolder(sProcessorName);
		File oProcessorFolder = new File(sProcessorFolder);

		// Is the processor installed in this node?
		if (!oProcessorFolder.exists()) {
			LauncherMain.s_oLogger.error("DockerProcessorEngine.refreshPackagesInfo: Processor [" + sProcessorName
					+ "] environment not updated in this node, return");
			return true;
		}

		String sIp = WasdiConfig.Current.dockers.internalDockersBaseAddress;
		int iPort = oProcessor.getPort();

		try {
			IPackageManager oPackageManager = getPackageManager(sIp, iPort);

			Map<String, Object> aoPackagesInfo = oPackageManager.getPackagesInfo();

			String sFileFullPath = sProcessorFolder + "packagesInfo.json";
			LauncherMain.s_oLogger.debug("DockerProcessorEngine.refreshPackagesInfo | sFileFullPath: " + sFileFullPath);

			boolean bResult = WasdiFileUtils.writeMapAsJsonFile(aoPackagesInfo, sFileFullPath);

			if (bResult) {
				LauncherMain.s_oLogger.debug("the file was created.");
			} else {
				LauncherMain.s_oLogger.debug("the file was not created.");
			}

			return bResult;
		} catch (Exception oEx) {
			LauncherMain.s_oLogger.debug("DockerProcessorEngine.refreshPackagesInfo: " + oEx);
		}

		return false;
	}

	/**
	 * Waits some time to let application start
	 */
	public void waitForApplicationToStart(ProcessorParameter oParameter) {
		try {
	        LauncherMain.s_oLogger.debug("DockerProcessorEngine.waitForApplicationToStart: wait 5 sec to let docker start");

	        Integer iNumberOfAttemptsToPingTheServer = WasdiConfig.Current.dockers.numberOfAttemptsToPingTheServer;
	        Integer iMillisBetweenAttmpts = WasdiConfig.Current.dockers.millisBetweenAttmpts;

	        for (int i = 0; i < iNumberOfAttemptsToPingTheServer; i++) {
	        	
	        	Thread.sleep(iMillisBetweenAttmpts);
	        	
	        	if (isDockerServerUp(oParameter)) {
	        		return;
	        	}
	        }
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.debug("DockerProcessorEngine.waitForApplicationToStart: exception " + oEx.toString());
		}
	}

	public boolean isDockerServerUp(ProcessorParameter oParameter) {
		try {
			String sProcessorId = oParameter.getProcessorID();

			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

			String sIp = WasdiConfig.Current.dockers.internalDockersBaseAddress;
			int iPort = oProcessor.getPort();


			String sUrl = "http://" + sIp + ":" + iPort + "/hello";
			LauncherMain.s_oLogger.debug("CondaPackageManagerImpl.isDockerServerUp: sUrl: " + sUrl);

			Map<String, String> asHeaders = Collections.emptyMap();

			HttpCallResponse oHttpCallResponse = HttpUtils.newStandardHttpGETQuery(sUrl, asHeaders);
			Integer iResult = oHttpCallResponse.getResponseCode();

			LauncherMain.s_oLogger.debug("CondaPackageManagerImpl.isDockerServerUp: iResult: " + iResult);

			return (iResult != null && iResult.intValue() == 200);
		} catch (Exception oEx) {
			LauncherMain.s_oLogger.error("DockerProcessorEngine.isDockerServerUp: exception " + oEx.toString());
		}

		return false;
	}

	@Override
	protected boolean reconstructEnvironment(ProcessorParameter oParameter, int iPort) {
		
		boolean bRet = true;
		
		try {
			
			// We need to reach the main server
			String sBaseUrl = WasdiConfig.Current.baseUrl;
			String sUrl = sBaseUrl + "/packageManager/environmentActions?name=" + oParameter.getName();
			
			// Create the headers
			Map<String, String> asHeaders = HttpUtils.getStandardHeaders(oParameter.getSessionID());
			
			LauncherMain.s_oLogger.debug("DockerProcessorEngine.reconstructEnvironment: calling url " + sUrl);
			// Call the API to get the lastest action list
			String sResult = HttpUtils.httpGet(sUrl, asHeaders);
			
			// Convert to an array of strings
			ArrayList<String> asActions = MongoRepository.s_oMapper.readValue(sResult, new TypeReference<ArrayList<String>>(){});
			
			// Do we have actions?
			if (asActions.size()>0) {
				
				LauncherMain.s_oLogger.debug("DockerProcessorEngine.reconstructEnvironment: got " + asActions.size() + " actions");
				
				// Yes! Lets re-do all
				
				// Take the ip
				String sIp = WasdiConfig.Current.dockers.internalDockersBaseAddress;
				
				// Create package manager info
				IPackageManager oPackageManager = getPackageManager(sIp, iPort);
				
				// For each command
				for (String sUpdateCommand : asActions) {
					
					LauncherMain.s_oLogger.debug("DockerProcessorEngine.reconstructEnvironment: executing " + sUpdateCommand);
					bRet &= oPackageManager.operatePackageChange(sUpdateCommand);
					
					if (!bRet) {
						LauncherMain.s_oLogger.debug("DockerProcessorEngine.reconstructEnvironment: error executing " + sUpdateCommand);
						break;
					}
				}
				
			}
			else {
				LauncherMain.s_oLogger.debug("DockerProcessorEngine.reconstructEnvironment: no actions to do");
			}
		} 
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("DockerProcessorEngine.reconstructEnvironment: exception " + oEx.toString());
		}
		
		// execute all the ops with the binded Package Manager for the app
		LauncherMain.s_oLogger.debug("DockerProcessorEngine.reconstructEnvironment: done");
		
		return bRet;
	}
}
