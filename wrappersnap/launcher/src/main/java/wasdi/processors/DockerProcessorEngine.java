package wasdi.processors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;

public abstract class  DockerProcessorEngine extends WasdiProcessorEngine {
	
	//protected String m_sDockerTemplatePath = "";

	public DockerProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath) {
		super(sWorkingRootPath, sDockerTemplatePath);
	}
	
	/**
	 * Deploy a new Processor in WASDI
	 * @param oParameter
	 */
	@Override
	public boolean deploy(ProcessorParameter oParameter) {
		return deploy(oParameter, true);
	}
	
	/**
	 * Deploy a new Processor in WASDI
	 * @param oParameter
	 */
	public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {
		
		LauncherMain.s_oLogger.debug("DockerProcessorEngine.DeployProcessor: start");
		
		if (oParameter == null) return false;
		
		ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
		ProcessWorkspace oProcessWorkspace = null;		
		
		try {
			
			oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oParameter.getProcessObjId());
			
			if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			// First Check if processor exists
			String sProcessorName = oParameter.getName();
			String sProcessorId = oParameter.getProcessorID();
			
			// Set the processor path
			String sDownloadRootPath = m_sWorkingRootPath;
			if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
			
			String sProcessorFolder = sDownloadRootPath+ "processors/" + sProcessorName + "/" ;
			// Create the file
			File oProcessorZipFile = new File(sProcessorFolder + sProcessorId + ".zip");
			
			LauncherMain.s_oLogger.debug("WasdiProcessorEngine.DeployProcessor: check processor exists");
			
			// Check it
			if (oProcessorZipFile.exists()==false) {
				LauncherMain.s_oLogger.debug("DockerProcessorEngine.DeployProcessor the Processor [" + sProcessorName + "] does not exists in path " + oProcessorZipFile.getPath());
				if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;
			}
			
			if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 2);
			LauncherMain.s_oLogger.error("DockerProcessorEngine.DeployProcessor: unzip processor");
			
			// Unzip the processor (and check for entry point myProcessor.py)
			if (!UnzipProcessor(sProcessorFolder, sProcessorId + ".zip")) {
				LauncherMain.s_oLogger.debug("DockerProcessorEngine.DeployProcessor error unzipping the Processor [" + sProcessorName + "]");
				if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;
			}
		    
			if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 20);
			LauncherMain.s_oLogger.debug("DockerProcessorEngine.DeployProcessor: copy container image template");
			
			// Copy Docker template files in the processor folder
			File oDockerTemplateFolder = new File(m_sDockerTemplatePath);
			File oProcessorFolder = new File(sProcessorFolder);
			
			FileUtils.copyDirectory(oDockerTemplateFolder, oProcessorFolder);

			if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 25);

			// Generate the image
			LauncherMain.s_oLogger.debug("DockerProcessorEngine.DeployProcessor: building image");
			
			handleUnzippedProcessor(sProcessorFolder);
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
			
			String sDockerName = "wasdi/"+sProcessorName+":"+oProcessor.getVersion();
			
			ArrayList<String> asArgs = new ArrayList<>();
			
			String sCommand = "docker";
			String sRunFile = sProcessorFolder+"deploywasdidocker.sh";
			
			File oRunFile = new File(sRunFile);
			
			BufferedWriter oRunWriter = new BufferedWriter(new FileWriter(oRunFile));
			
			if(null!= oRunWriter) {
				LauncherMain.s_oLogger.debug("DockerProcessorEngine.deploy: Creating "+sRunFile+" file");

				oRunWriter.write("#!/bin/bash");
				oRunWriter.newLine();
				oRunWriter.write("echo Deploy Docker Started > /usr/lib/wasdi/launcher/logs/echo.txt");
				oRunWriter.newLine();
				oRunWriter.write("docker build -t" + sDockerName + " " + sProcessorFolder + " $1 > /usr/lib/wasdi/launcher/logs/deploy.txt 2>1");
				oRunWriter.newLine();
				oRunWriter.write("echo Deploy Docker Done");
				oRunWriter.flush();
				oRunWriter.close();
			}			
			
			Runtime.getRuntime().exec("chmod u+x "+sRunFile);
			
			shellExec(sRunFile, asArgs);
			
			if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 70);
			LauncherMain.s_oLogger.debug("DockerProcessorEngine.DeployProcessor: created image " + sDockerName);
			
			// Run the container
			int iProcessorPort = oProcessorRepository.getNextProcessorPort();
			
			if (!bFirstDeploy) {
				iProcessorPort = oProcessor.getPort();
			}
			
			//docker run -it -p 8888:5000 fadeout/wasdi:0.6
			asArgs.clear();
			asArgs.add("run");
			// P.Campanella 11/06/2018: mounted volume
			// NOTA: QUI INVECE SI CHE ABBIAMO PROBLEMI DI DIRITTI!!!!!!!!!!!!
			asArgs.add("-v"+ m_sWorkingRootPath + ":/data/wasdi");
			
			//asArgs.add("--user=tomcat8");
			
			
			asArgs.add("--mount");
			asArgs.add("type=bind,src="+sProcessorFolder+",dst=/wasdi");
			asArgs.add("-p127.0.0.1:"+iProcessorPort+":5000");
			asArgs.add(sDockerName);
			
			handleRunCommand(sCommand, asArgs);
			
			shellExec(sCommand, asArgs, false);
			if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 90);
			
			// Save Processor Port in the Repo
			if (bFirstDeploy) {
				oProcessor.setPort(iProcessorPort);
				oProcessorRepository.updateProcessor(oProcessor);
			}
			
			LauncherMain.s_oLogger.debug("DockerProcessorEngine.DeployProcessor: container " + sDockerName + " is running");
			
			if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
			
		}
		catch (Exception oEx) {
			
			LauncherMain.s_oLogger.error("DockerProcessorEngine.DeployProcessor Exception", oEx);
			try {
				if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			} catch (JsonProcessingException e) {
				LauncherMain.s_oLogger.error("DockerProcessorEngine.DeployProcessor Exception", e);
			}
			return false;
		}
		
		return true;
	}
	
	protected abstract void handleRunCommand(String sCommand, ArrayList<String> asArgs);

	protected abstract void handleBuildCommand(String sCommand, ArrayList<String> asArgs);

	protected abstract void handleUnzippedProcessor(String sProcessorFolder);
	
	/**
	 * Unzip the processor
	 * @param sProcessorFolder
	 * @param sZipFileName
	 * @return
	 */
	public boolean UnzipProcessor(String sProcessorFolder, String sZipFileName) {
		try {
			// Create the file
			File oProcessorZipFile = new File(sProcessorFolder+sZipFileName);
						
			// Unzip the file and, meanwhile, check if myProcessor.py exists
			
			boolean bMyProcessorExists = false;
			
			byte[] ayBuffer = new byte[1024];
		    ZipInputStream oZipInputStream = new ZipInputStream(new FileInputStream(oProcessorZipFile));
		    ZipEntry oZipEntry = oZipInputStream.getNextEntry();
		    while(oZipEntry != null){
		    	
		    	try {
			    	String sZippedFileName = oZipEntry.getName();
			    	
			    	if (sZippedFileName.equals("myProcessor.py")) bMyProcessorExists = true;
			    	
			    	String sUnzipFilePath = sProcessorFolder+sZippedFileName;
			    	
			    	if (oZipEntry.isDirectory()) {
			    		File oUnzippedDir = new File(sUnzipFilePath);
		                oUnzippedDir.mkdir();
			    	}
			    	else {
				        File oUnzippedFile = new File(sProcessorFolder + sZippedFileName);
				        FileOutputStream oOutputStream = new FileOutputStream(oUnzippedFile);
				        int iLen;
				        while ((iLen = oZipInputStream.read(ayBuffer)) > 0) {
				        	oOutputStream.write(ayBuffer, 0, iLen);
				        }
				        oOutputStream.close();		    		
			    	}		    		
		    	}
		    	catch (Exception oInnerEx) {
		    		LauncherMain.s_oLogger.error("DockerProcessorEngine.UnzipProcessor Exception unzipping Zip entry", oInnerEx);
				}

		        oZipEntry = oZipInputStream.getNextEntry();
	        }
		    oZipInputStream.closeEntry();
		    oZipInputStream.close();
		    
		    if (!bMyProcessorExists) {
		    	LauncherMain.s_oLogger.error("DockerProcessorEngine.UnzipProcessor myProcessor.py not present in processor " + sZipFileName);
		    	return false;
		    }
		    
		    try {
			    // Remove the zip?
			    oProcessorZipFile.delete();		    	
		    }
		    catch (Exception e) {
				//String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(e);
				//if (LauncherMain.s_oSendToRabbit!=null) LauncherMain.s_oSendToRabbit.SendRabbitMessage(false, sOperation, sWorkspace,sError,sExchange);			
				LauncherMain.s_oLogger.error("DockerProcessorEngine.UnzipProcessor Exception Deleting Zip File", e);
				//return false;
			}
		    
		}
		catch (Exception oEx) {
			//String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
			//if (LauncherMain.s_oSendToRabbit!=null) LauncherMain.s_oSendToRabbit.SendRabbitMessage(false, sOperation, sWorkspace,sError,sExchange);			
			LauncherMain.s_oLogger.error("DockerProcessorEngine.DeployProcessor Exception", oEx);
			return false;
		}
		
		return true;
	}
	
	
	public void shellExec(String sCommand, List<String> asArgs) {
		shellExec(sCommand,asArgs,true);
	}
	
	public void shellExec(String sCommand, List<String> asArgs, boolean bWait) {
		try {
			if (asArgs==null) asArgs = new ArrayList<String>();
			asArgs.add(0, sCommand);
			
			String sCommandLine = "";
			
			for (String sArg : asArgs) {
				sCommandLine += sArg + " ";
			}
			
			LauncherMain.s_oLogger.debug("ShellExec CommandLine: " + sCommandLine);
			
			ProcessBuilder oProcessBuilder = new ProcessBuilder(asArgs.toArray(new String[0]));
			//oProcessBuilder.redirectErrorStream(true);
			Process oProcess = oProcessBuilder.start();
			if (bWait) {
				//BufferedReader oReader = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
				//String sLine;
				//while ((sLine = oReader.readLine()) != null)
					//LauncherMain.s_oLogger.debug("[docker]: " + sLine);
				int iProcOuptut = oProcess.waitFor();
				
				LauncherMain.s_oLogger.debug("ShellExec CommandLine RETURNED: " + iProcOuptut);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Run a Docker Processor
	 */
	public boolean run(ProcessorParameter oParameter) {
		
		LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: start");
		
		if (oParameter == null) return false;
		
		ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
		ProcessWorkspace oProcessWorkspace = null;		
		
		try {
			
			// Get Repo and Process Workspace
			oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oParameter.getProcessObjId());
			
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
				
				String sProcessorZipFile = downloadProcessor(oProcessor, oParameter.getSessionID());
				
				LauncherMain.s_oLogger.info("DockerProcessorEngine.run: processor zip file downloaded: " + sProcessorZipFile);
				
				if (!Utils.isNullOrEmpty(sProcessorZipFile)) {
					deploy(oParameter, false);
				}
				else {
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
			String sUrl = "http://localhost:"+oProcessor.getPort()+"/run/"+oParameter.getProcessObjId();
			
			sUrl += "?user=" + oParameter.getUserId();
			sUrl += "&sessionid=" + oParameter.getSessionID();
			sUrl += "&workspaceid=" + oParameter.getWorkspace();
			
			LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: calling URL = " + sUrl);
			
			// Create connection
			URL oProcessorUrl = new URL(sUrl);
			
			LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: call open connection");
			HttpURLConnection oConnection = (HttpURLConnection) oProcessorUrl.openConnection();
			oConnection.setDoOutput(true);
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Content-Type", "application/json");

			OutputStream oOutputStream = null;
			try {
				
				// Try to fetch the result from docker
				oOutputStream = oConnection.getOutputStream();
				oOutputStream.write(sJson.getBytes());
				oOutputStream.flush();
				
				if (! (oConnection.getResponseCode() == HttpURLConnection.HTTP_OK || oConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED )) {
					throw new Exception();
				}
			}
			catch (Exception e) {
				LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: connection failed: try to start container again");
				
				// Try to start Again the docker
				
				// Set the processor path
				String sDownloadRootPath = m_sWorkingRootPath;
				if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
				
				String sProcessorFolder = sDownloadRootPath+ "processors/" + sProcessorName + "/" ;				
				
				// Set the docker Name
				String sDockerName = "wasdi/"+sProcessorName+":"+oProcessor.getVersion();
				
				ArrayList<String> asArgs = new ArrayList<>();
				// Run the container
				int iProcessorPort = oProcessor.getPort();
				//docker run -it -p 8888:5000 fadeout/wasdi:0.6
				asArgs.clear();
				asArgs.add("run");
				
				//asArgs.add("--user=tomcat8");
				
				// P.Campanella 11/06/2018: mounted volume
				// NOTA: QUI INVECE SI CHE ABBIAMO PROBLEMI DI DIRITTI!!!!!!!!!!!!
				asArgs.add("-v"+ m_sWorkingRootPath + ":/data/wasdi");
				asArgs.add("--mount");
				asArgs.add("type=bind,src="+sProcessorFolder+",dst=/wasdi");				
				asArgs.add("-p127.0.0.1:"+iProcessorPort+":5000");
				asArgs.add(sDockerName);
				
				String sCommand = "docker";
				
				handleRunCommand(sCommand, asArgs);
				
				shellExec(sCommand, asArgs, false);
				
				LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: wait 5 sec to let docker start");
				Thread.sleep(5000);
				
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
				
				if (! (oConnection.getResponseCode() == HttpURLConnection.HTTP_OK || oConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED )) {
					// Nothing to do
					throw new RuntimeException("Failed Again: HTTP error code : " + oConnection.getResponseCode());
				}
				
				LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: ok container recovered");
			}
				
			// Get Result from server
			BufferedReader oBufferedReader = new BufferedReader(new InputStreamReader((oConnection.getInputStream())));

			String sJsonOutput="";
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
			int iTimeSpentMs = 0;
			int iThreadSleepMs = 2000;
			
			String sStatus = oProcessWorkspace.getStatus();
			
			LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: process Status: " + sStatus);
			
			LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: process output: " + sJsonOutput);
			
			Map<String, String> oOutputJsonMap = null;
			
			try {
				ObjectMapper oMapper = new ObjectMapper();
				oOutputJsonMap = oMapper.readValue(sJsonOutput, Map.class);				
			}
			catch (Exception oEx) {
				LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: exception converting proc output in Json " + oEx.toString());
			}
			
			// Check if is a processor > 1.0: 
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
					}
					catch (Exception e) {
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
						while ( ! (sStatus.equals("DONE") || sStatus.equals("STOPPED") || sStatus.equals("ERROR"))) {
							oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());
							
							sStatus = oProcessWorkspace.getStatus();
							try {
								Thread.sleep(iThreadSleepMs);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							
							// Increase the time
							iTimeSpentMs += iThreadSleepMs;
							
							if (oProcessor.getTimeoutMs()>0) {
								if (iTimeSpentMs > oProcessor.getTimeoutMs()) {
									// Timeout
									LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: Timeout of Processor with ProcId " + oProcessWorkspace.getProcessObjId() + " Time spent [ms] " + iTimeSpentMs );
									
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
						
					}
					else {
						// Old processor engine: force safe status
						LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: processor engine v1.0 - force process as done");
						LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);						
					}
				}
				else {
					// Old processor engine: force safe status
					LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: processor engine v1.0 - force process as done");
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
				}
			}
			else {
				// Old processor engine: force safe status
				LauncherMain.s_oLogger.debug("DockerProcessorEngine.run: impossible to read processor outptu in a json. Force closed");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
			}
			
			// Check and set the operation end-date
			if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndDate())) {
				oProcessWorkspace.setOperationEndDate(Utils.GetFormatDate(new Date()));
				// P.Campanella 20200115: I think this is to add, but I cannot test it now :( ...
				//LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.valueOf(oProcessWorkspace.getStatus()), oProcessWorkspace.getProgressPerc());
			}
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("DockerProcessorEngine.run Exception", oEx);
			try {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			} catch (Exception oInnerEx) {
				LauncherMain.s_oLogger.error("DockerProcessorEngine.run Exception", oInnerEx);
			}
			
			return false;
		}
		
		return true;
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
			oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oParameter.getProcessObjId());
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			// First Check if processor exists
			String sProcessorName = oParameter.getName();
			String sProcessorId = oParameter.getProcessorID();
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
			
			// Check processor
			if (oProcessor == null) { 
				LauncherMain.s_oLogger.error("DockerProcessorEngine.delete: oProcessor is null [" + sProcessorId +"]");
				return false;
			}
			
			// Set the processor path
			String sDownloadRootPath = m_sWorkingRootPath;
			if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
			
			String sProcessorFolder = sDownloadRootPath+ "/processors/" + sProcessorName + "/" ;
			
			File oProcessorFolder = new File(sProcessorFolder);

			
			// docker ps -a | awk '{ print $1,$2 }' | grep <imagename> | awk '{print $1 }' | xargs -I {} docker rm -f {}
			// docker rmi -f <imagename>
			
			
			String sDockerName = "wasdi/"+sProcessorName+":"+oProcessor.getVersion();
			
			String sRunFile = sProcessorFolder+"cleanwasdidocker.sh";
			
			File oRunFile = new File(sRunFile);
			
			BufferedWriter oRunWriter = new BufferedWriter(new FileWriter(oRunFile));
			
			if(null!= oRunWriter) {
				LauncherMain.s_oLogger.debug("DockerProcessorEngine.deleteProcessor: Creating "+sRunFile+" file");

				oRunWriter.write("#!/bin/bash");
				oRunWriter.newLine();
				oRunWriter.write("docker ps -a | awk '{ print $1,$2 }' | grep " + sDockerName + " | awk '{print $1 }' | xargs -I {} docker rm -f {}");
				oRunWriter.newLine();
				oRunWriter.flush();
				oRunWriter.close();
			}			
			
			Runtime.getRuntime().exec("chmod u+x "+sRunFile);			

			Runtime.getRuntime().exec(sRunFile);

			// Wait for docker to finish
			Thread.sleep(10000);
			
			// Delete this image
			ArrayList<String> asArgs = new ArrayList<>();
			// Remove the container image
			asArgs.add("rmi");
			asArgs.add("-f");
			asArgs.add(sDockerName);
			
			String sCommand = "docker";
			
			
			shellExec(sCommand, asArgs, false);
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 33);
						
			// delete the folder
			
			FileUtils.deleteDirectory(oProcessorFolder);
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 66);
			
			// delete the db entry
			oProcessorRepository.deleteProcessor(oProcessor.getProcessorId());
			
			// Check and set the operation end-date
			if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndDate())) {
				oProcessWorkspace.setOperationEndDate(Utils.GetFormatDate(new Date()));
			}			
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
			
			return true;
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("DockerProcessorEngine.delete Exception", oEx);
			try {
				// Check and set the operation end-date
				if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndDate())) {
					oProcessWorkspace.setOperationEndDate(Utils.GetFormatDate(new Date()));
				}			
				
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			} catch (Exception e) {
				LauncherMain.s_oLogger.error("DockerProcessorEngine.delete Exception", e);
			}
			
			return false;
		}
	}
}
