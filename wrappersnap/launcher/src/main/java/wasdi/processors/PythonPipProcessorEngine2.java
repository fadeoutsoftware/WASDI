package wasdi.processors;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.LauncherMain;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Python Pip 2 Engine: creates a WASDI Processor developed in Python-Pip
 * and push the image to the register. To start the application the nodes 
 * will take directly the image from the registry.
 * 
 * @author p.campanella
 *
 */
public class PythonPipProcessorEngine2 extends PipProcessorEngine {
		
	/**
	 * Default constructor
	 */
	public PythonPipProcessorEngine2() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.PYTHON_PIP_2);
	}
	
	
	/**
	 * Deploy the processor in ADES.
	 * The method creates the docker in "single run mode".
	 * Then it pushes the image in Nexus
	 * Then creates the CWL and the body to post to the ades api to deploy a processor.
	 */
	@Override
	public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {
				
		// We read  the registers from the config
		List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
		
		if (aoRegisters == null) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.deploy: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.deploy: registers list is empty, return false.");
			return false;			
		}
		
		WasdiLog.debugLog("PythonPipProcessorEngine2.deploy: call base class deploy");
		
		// We do not need to start after the build
		m_bRunAfterDeploy = false;
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;
		
		WasdiLog.debugLog("PythonPipProcessorEngine2.deploy: Docker Registry = " + m_sDockerRegistry);
		
		// Build the image of the docker
		boolean bResult = super.deploy(oParameter, bFirstDeploy);
		
		if (!bResult) {
			// This is not good
			WasdiLog.errorLog("PythonPipProcessorEngine2.deploy: super class deploy returned false. So we stop here.");
			return false;
		}
		
		// Get Processor Id
		String sProcessorId = oParameter.getProcessorID();
		
		// Read the processor from the db
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		// Here we save the address of the image
		String sPushedImageAddress = pushImageInRegisters(oProcessor);
		
		if (Utils.isNullOrEmpty(sPushedImageAddress)) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.deploy: Impossible to push the image.");
			return false;
		}
		
		WasdiLog.debugLog("PythonPipProcessorEngine2.deploy: deploy done, image pushed");
		
        return true;
	}	
	
	/**
	 * Force the redeploy of an image
	 */
	@Override
	public boolean redeploy(ProcessorParameter oParameter) {
		// We read  the registers from the config
		List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
		
		if (aoRegisters == null) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.redeploy: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.redeploy: registers list is empty, return false.");
			return false;			
		}
		
		WasdiLog.debugLog("PythonPipProcessorEngine2.redeploy: call base class deploy");
		
		// We do not need to start after the build
		m_bRunAfterDeploy = false;
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;
		
		// Get Processor Name and Id
		String sProcessorId = oParameter.getProcessorID();		
		
		// Read the processor from the db
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
		
		// Increment the version of the processor
		String sNewVersion = oProcessor.getVersion();
		sNewVersion = StringUtils.incrementIntegerString(sNewVersion);
		oProcessor.setVersion(sNewVersion);
		// Save it
		oProcessorRepository.updateProcessor(oProcessor);
		
		boolean bResult = super.redeploy(oParameter);
		
		if (!bResult) {
			// This is not good
			WasdiLog.errorLog("PythonPipProcessorEngine2.redeploy: super class deploy returned false. So we stop here.");
			return false;
		}
						
		// Here we save the address of the image
		String sPushedImageAddress = pushImageInRegisters(oProcessor);
		
        return true;
	}
	
	@Override
	public boolean run(ProcessorParameter oParameter) {
        WasdiLog.debugLog("PythonPipProcessorEngine2.run: start");

        if (oParameter == null) {
            WasdiLog.errorLog("PythonPipProcessorEngine2.run: parameter is null");
            return false;
        }
        
		// We read  the registers from the config
		List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
		
		if (aoRegisters == null) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.deploy: registers list is null, return false.");
			return false;
		}
		
		if (aoRegisters.size() == 0) {
			WasdiLog.errorLog("PythonPipProcessorEngine2.deploy: registers list is empty, return false.");
			return false;			
		}
		
		WasdiLog.debugLog("PythonPipProcessorEngine2.deploy: call base class deploy");
		
		// And we work with our main register
		m_sDockerRegistry = aoRegisters.get(0).address;        

        // Get Repo and Process Workspace
        ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
        ProcessWorkspace oProcessWorkspace = m_oProcessWorkspace;

        try {

            // Check workspace folder
            String sWorkspacePath = LauncherMain.getWorkspacePath(oParameter);

            File oWorkspacePath = new File(sWorkspacePath);

            if (!oWorkspacePath.exists()) {
                try {
                    WasdiLog.infoLog("PythonPipProcessorEngine2.run: creating ws folder");
                    oWorkspacePath.mkdirs();
                } catch (Exception oWorkspaceFolderException) {
                    WasdiLog.errorLog("PythonPipProcessorEngine2.run: exception creating ws: " + oWorkspaceFolderException);
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
                throw new Exception("PythonPipProcessorEngine2.run: Impossible to find processor " + sProcessorId);
            }

            // Check if the processor is available on the node
            if (!isProcessorOnNode(oParameter)) {
                WasdiLog.infoLog("PythonPipProcessorEngine2.run: processor not available on node download it");
                
                m_oSendToRabbit.SendRabbitMessage(true, LauncherOperations.INFO.name(), m_oParameter.getExchange(), "APP NOT ON NODE<BR>INSTALLATION STARTED", m_oParameter.getExchange());

                String sProcessorZipFile = downloadProcessor(oProcessor, oParameter.getSessionID());

                WasdiLog.infoLog("PythonPipProcessorEngine2.run: processor zip file downloaded: " + sProcessorZipFile);

                if (!unzipProcessor(getProcessorFolder(oProcessor), sProcessorId + ".zip", oParameter.getProcessObjId())) {
                	WasdiLog.infoLog("PythonPipProcessorEngine2.run: error un zipping the processor");
                	LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                	return false;
                }
            }

            // Decode JSON
            String sEncodedJson = oParameter.getJson();
            String sJson = java.net.URLDecoder.decode(sEncodedJson, "UTF-8");

            WasdiLog.debugLog("PythonPipProcessorEngine2.run: calling " + sProcessorName + " at port " + oProcessor.getPort());

            // Json sanity check
            if (Utils.isNullOrEmpty(sJson)) {
                sJson = "{}";
            }

            // Json sanity check
            if (sJson.equals("\"\"")) {
                sJson = "{}";
            }

            WasdiLog.debugLog("PythonPipProcessorEngine2.run: Decoded JSON Parameter " + sJson);

            // Call localhost:port
            String sUrl = "http://" + WasdiConfig.Current.dockers.internalDockersBaseAddress + ":" + oProcessor.getPort() + "/run/" + oParameter.getProcessObjId();

            sUrl += "?user=" + oParameter.getUserId();
            sUrl += "&sessionid=" + oParameter.getSessionID();
            sUrl += "&workspaceid=" + oParameter.getWorkspace();

            WasdiLog.debugLog("PythonPipProcessorEngine2.run: calling URL = " + sUrl);

            // Create connection
            URL oProcessorUrl = new URL(sUrl);
            
			int iConnectionTimeOut = WasdiConfig.Current.connectionTimeout;
			int iReadTimeOut = WasdiConfig.Current.readTimeout;
			            

            WasdiLog.debugLog("PythonPipProcessorEngine2.run: call open connection");
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
                    throw new Exception("PythonPipProcessorEngine2.run: response code is: " + oConnection.getResponseCode());
                }
            } catch (Exception oE) {
                WasdiLog.debugLog("PythonPipProcessorEngine2.run: connection failed due to: " + oE + ", try to start container again");

                // Try to start Again the docker
                String sProcessorFolder = getProcessorFolder(sProcessorName);

                // Start it
                DockerUtils oDockerUtils = new DockerUtils(oProcessor, sProcessorFolder, m_sTomcatUser);
                oDockerUtils.setPullDuringRun(true);
                oDockerUtils.setDockerRegistry(m_sDockerRegistry);
                oDockerUtils.run();
                
                // Wait a little bit
                waitForApplicationToStart(oParameter);

                // Try again the connection
                WasdiLog.debugLog("PythonPipProcessorEngine2.run: connection failed: try to connect again");
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

                WasdiLog.debugLog("PythonPipProcessorEngine2.run: ok container recovered");
            }

            // Get Result from server
            BufferedReader oBufferedReader = new BufferedReader(new InputStreamReader((oConnection.getInputStream())));

            String sJsonOutput = "";
            String sOutputResult;

            WasdiLog.debugLog("PythonPipProcessorEngine2.run: Output from Server .... \n");

            while ((sOutputResult = oBufferedReader.readLine()) != null) {
                WasdiLog.debugLog("PythonPipProcessorEngine2.run: " + sOutputResult);
                sJsonOutput += sOutputResult;
            }

            WasdiLog.debugLog("PythonPipProcessorEngine2.run: out from the read Line loop");

            oConnection.disconnect();

            // Read Again Process Workspace: the user may have changed it!
            oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());

            // Here we can wait for the process to finish with the status check
            // we can also handle a timeout, that is property (with default) of the processor
            long lTimeSpentMs = 0;
            int iThreadSleepMs = 2000;

            String sStatus = oProcessWorkspace.getStatus();

            WasdiLog.debugLog("PythonPipProcessorEngine2.run: process Status: " + sStatus);

            WasdiLog.debugLog("PythonPipProcessorEngine2.run: process output: " + sJsonOutput);

            Map<String, String> oOutputJsonMap = null;

            try {
                ObjectMapper oMapper = new ObjectMapper();
                oOutputJsonMap = oMapper.readValue(sJsonOutput, Map.class);
            } catch (Exception oEx) {
                WasdiLog.debugLog("PythonPipProcessorEngine2.run: exception converting proc output in Json " + oEx);
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

                    WasdiLog.debugLog("PythonPipProcessorEngine2.run: processor engine version " + dVersion);

                    // New, Asynch, Processor?
                    if (dVersion > 1.0) {

                        boolean bForcedError = false;

                        // Yes
                        WasdiLog.debugLog("PythonPipProcessorEngine2.run: processor engine version > 1.0: wait for the processor to finish");

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
                                    WasdiLog.debugLog("PythonPipProcessorEngine2.run: Timeout of Processor with ProcId " + oProcessWorkspace.getProcessObjId() + " Time spent [ms] " + lTimeSpentMs);

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

                        WasdiLog.debugLog("PythonPipProcessorEngine2.run: processor done");

                    } else {
                        // Old processor engine: force safe status
                        WasdiLog.debugLog("PythonPipProcessorEngine2.run: processor engine v1.0 - force process as done");
                        LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
                    }
                } else {
                    // Old processor engine: force safe status
                    WasdiLog.debugLog("PythonPipProcessorEngine2.run: processor engine v1.0 - force process as done");
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
                }
            } else {
                // Old processor engine: force safe status
                WasdiLog.debugLog("PythonPipProcessorEngine2.run: impossible to read processor output in a json. Force closed");
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
            }

            // Check and set the operation end-date
            if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
                oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
                // P.Campanella 20200115: I think this is to add, but I cannot test it now :( ...
                //LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.valueOf(oProcessWorkspace.getStatus()), oProcessWorkspace.getProgressPerc());
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("PythonPipProcessorEngine2.run Exception", oEx);
            try {
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
            } catch (Exception oInnerEx) {
                WasdiLog.errorLog("PythonPipProcessorEngine2.run Exception", oInnerEx);
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
	

}
