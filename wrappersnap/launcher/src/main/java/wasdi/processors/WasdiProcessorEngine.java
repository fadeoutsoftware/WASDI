package wasdi.processors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.DeployProcessorParameter;
import wasdi.shared.utils.Utils;

public abstract class WasdiProcessorEngine {
	
	protected String m_sWorkingRootPath = "";
	protected String m_sDockerTemplatePath = "";
	
	
	public static WasdiProcessorEngine GetProcessorEngine(String sType,String sWorkingRootPath, String sDockerTemplatePath) {
		
		if (Utils.isNullOrEmpty(sType)) {
			sType = ProcessorTypes.UBUNTU_PYTHON_SNAP;
		}
		
		if (sType.equals(ProcessorTypes.UBUNTU_PYTHON_SNAP)) {
			return new UbuntuPythonProcessorEngine(sWorkingRootPath,sDockerTemplatePath);
		}
		else {
			return new UbuntuPythonProcessorEngine(sWorkingRootPath, sDockerTemplatePath);
		}
	}
	
	public WasdiProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath) {
		m_sWorkingRootPath = sWorkingRootPath;
		m_sDockerTemplatePath = sDockerTemplatePath;
	}
	
	/**
	 * Deploy a new Processor in WASDI
	 * @param oParameter
	 */
	public boolean DeployProcessor(DeployProcessorParameter oParameter) {
		
		LauncherMain.s_oLogger.debug("WasdiProcessorEngine.DeployProcessor: start");
		
		if (oParameter == null) return false;
		
		ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
		ProcessWorkspace oProcessWorkspace = null;		
		
		try {
			
			oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProcessObjId(oParameter.getProcessObjId());
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			// First Check if processor exists
			String sProcessorName = oParameter.getName();
			String sProcessorId = oParameter.getProcessorID();
			
			// Set the processor path
			String sDownloadRootPath = m_sWorkingRootPath;
			if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
			
			String sProcessorFolder = sDownloadRootPath+ "/processors/" + sProcessorName + "/" ;
			// Create the file
			File oProcessorZipFile = new File(sProcessorFolder + sProcessorId + ".zip");
			
			LauncherMain.s_oLogger.debug("WasdiProcessorEngine.DeployProcessor: check processor exists");
			
			// Check it
			if (oProcessorZipFile.exists()==false) {
				LauncherMain.s_oLogger.debug("WasdiProcessorEngine.DeployProcessor the Processor [" + sProcessorName + "] does not exists in path " + oProcessorZipFile.getPath());
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;
			}
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 2);
			LauncherMain.s_oLogger.error("WasdiProcessorEngine.DeployProcessor: unzip processor");
			
			// Unzip the processor (and check for entry point myProcessor.py)
			if (!UnzipProcessor(sProcessorFolder, sProcessorId + ".zip")) {
				LauncherMain.s_oLogger.debug("WasdiProcessorEngine.DeployProcessor error unzipping the Processor [" + sProcessorName + "]");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;
			}
		    
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 20);
			LauncherMain.s_oLogger.debug("WasdiProcessorEngine.DeployProcessor: copy container image template");
			
			// Copy Docker template files in the processor folder
			File oDockerTemplateFolder = new File(m_sDockerTemplatePath);
			File oProcessorFolder = new File(sProcessorFolder);
			
			FileUtils.copyDirectory(oDockerTemplateFolder, oProcessorFolder);

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 25);

			// Generate the image
			LauncherMain.s_oLogger.debug("WasdiProcessorEngine.DeployProcessor: building image");
			
			handleUnzippedProcessor(sProcessorFolder);
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.GetProcessor(sProcessorId);
			
			String sDockerName = "wasdi/"+sProcessorName+":"+oProcessor.getVersion();
			
			String sCommand = "docker";
			ArrayList<String> asArgs = new ArrayList<>();
			
			asArgs.add("build");
			asArgs.add("-t"+sDockerName);
			asArgs.add(sProcessorFolder);
			
			handleBuildCommand(sCommand, asArgs);
			
			shellExec(sCommand,asArgs);
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 70);
			LauncherMain.s_oLogger.debug("WasdiProcessorEngine.DeployProcessor: created image " + sDockerName);
			
			// Run the container
			int iProcessorPort = oProcessorRepository.GetNextProcessorPort();
			//docker run -it -p 8888:5000 fadeout/wasdi:0.6
			asArgs.clear();
			asArgs.add("run");
			// P.Campanella 11/06/2018: mounted volume
			// NOTA: QUI INVECE SI CHE ABBIAMO PROBLEMI DI DIRITTI!!!!!!!!!!!!
			asArgs.add("-v"+ m_sWorkingRootPath + ":/data/wasdi");
			asArgs.add("-p127.0.0.1:"+iProcessorPort+":5000");
			asArgs.add(sDockerName);
			
			handleRunCommand(sCommand, asArgs);
			
			shellExec(sCommand, asArgs, false);
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 90);
			
			// Save Processor Port in the Repo
			oProcessor.setPort(iProcessorPort);
			oProcessorRepository.UpdateProcessor(oProcessor);
			
			LauncherMain.s_oLogger.debug("WasdiProcessorEngine.DeployProcessor: container " + sDockerName + " is running");
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
			
		}
		catch (Exception oEx) {
			//String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
			//if (LauncherMain.s_oSendToRabbit!=null) LauncherMain.s_oSendToRabbit.SendRabbitMessage(false, sOperation, sWorkspace,sError,sExchange);			
			LauncherMain.s_oLogger.error("WasdiProcessorEngine.DeployProcessor Exception", oEx);
			try {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			} catch (JsonProcessingException e) {
				LauncherMain.s_oLogger.error("WasdiProcessorEngine.DeployProcessor Exception", e);
			}
			return false;
		}
		
		return true;
	}
	
	protected abstract void handleRunCommand(String sCommand, ArrayList<String> asArgs);

	protected abstract void handleBuildCommand(String sCommand, ArrayList<String> asArgs);

	protected abstract void handleUnzippedProcessor(String sProcessorFolder);

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
		        oZipEntry = oZipInputStream.getNextEntry();
	        }
		    oZipInputStream.closeEntry();
		    oZipInputStream.close();
		    
		    if (!bMyProcessorExists) {
		    	LauncherMain.s_oLogger.error("WasdiProcessorEngine.UnzipProcessor myProcess.py not present in processor " + sZipFileName);
		    	return false;
		    }
		    
		    try {
			    // Remove the zip?
			    oProcessorZipFile.delete();		    	
		    }
		    catch (Exception e) {
				//String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(e);
				//if (LauncherMain.s_oSendToRabbit!=null) LauncherMain.s_oSendToRabbit.SendRabbitMessage(false, sOperation, sWorkspace,sError,sExchange);			
				LauncherMain.s_oLogger.error("WasdiProcessorEngine.UnzipProcessor Exception Deleting Zip File", e);
				//return false;
			}
		    
		}
		catch (Exception oEx) {
			//String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
			//if (LauncherMain.s_oSendToRabbit!=null) LauncherMain.s_oSendToRabbit.SendRabbitMessage(false, sOperation, sWorkspace,sError,sExchange);			
			LauncherMain.s_oLogger.error("WasdiProcessorEngine.DeployProcessor Exception", oEx);
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
			ProcessBuilder pb = new ProcessBuilder(asArgs.toArray(new String[0]));
			pb.redirectErrorStream(true);
			Process process = pb.start();
			if (bWait) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null)
					LauncherMain.s_oLogger.debug("[docker]: " + line);
				process.waitFor();				
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean run(DeployProcessorParameter oParameter) {
		
		LauncherMain.s_oLogger.debug("WasdiProcessorEngine.run: start");
		
		if (oParameter == null) return false;
		
		ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
		ProcessWorkspace oProcessWorkspace = null;		
		
		try {
			
			oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oProcessWorkspace = oProcessWorkspaceRepository.GetProcessByProcessObjId(oParameter.getProcessObjId());
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			// First Check if processor exists
			String sProcessorName = oParameter.getName();
			String sProcessorId = oParameter.getProcessorID();
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.GetProcessor(sProcessorId);
			
			// Check processor
			if (oProcessor == null) {
				// Catch block will handle 
				throw new Exception("Impossible to find processor " + sProcessorId);
			}
			
			// Decode JSON
			String sEncodedJson = oParameter.getJson();
			String sJson = java.net.URLDecoder.decode(sEncodedJson, "UTF-8");
			//String sJson = oParameter.getJson();
			
			LauncherMain.s_oLogger.debug("WasdiProcessorEngine.run: calling " + sProcessorName + " at port " + oProcessor.getPort());
			
			LauncherMain.s_oLogger.debug("WasdiProcessorEngine.run: Encoded JSON Parameter " + sEncodedJson);
			LauncherMain.s_oLogger.debug("WasdiProcessorEngine.run: Dencoded JSON Parameter " + sJson);
			
			// Call localhost:port
			String sUrl = "http://localhost:"+oProcessor.getPort()+"/run/"+oParameter.getProcessObjId();
			
			LauncherMain.s_oLogger.debug("WasdiProcessorEngine.run: calling URL = " + sUrl);
			
			
			URL oProcessorUrl = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oProcessorUrl.openConnection();
			oConnection.setDoOutput(true);
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Content-Type", "application/json");

			OutputStream oOutputStream = oConnection.getOutputStream();
			oOutputStream.write(sJson.getBytes());
			oOutputStream.flush();
			
			if (! (oConnection.getResponseCode() == HttpURLConnection.HTTP_OK || oConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED )) {
				throw new RuntimeException("Failed : HTTP error code : " + oConnection.getResponseCode());
			}

			BufferedReader oBufferedReader = new BufferedReader(new InputStreamReader((oConnection.getInputStream())));

			String sOutputResult;
			LauncherMain.s_oLogger.debug("WasdiProcessorEngine.run: Output from Server .... \n");
			while ((sOutputResult = oBufferedReader.readLine()) != null) {
				LauncherMain.s_oLogger.debug("WasdiProcessorEngine.run: " + sOutputResult);
			}

			oConnection.disconnect();
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
		}
		catch (Exception oEx) {
			//String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
			//if (LauncherMain.s_oSendToRabbit!=null) LauncherMain.s_oSendToRabbit.SendRabbitMessage(false, sOperation, sWorkspace,sError,sExchange);			
			LauncherMain.s_oLogger.error("WasdiProcessorEngine.run Exception", oEx);
			try {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			} catch (JsonProcessingException e) {
				LauncherMain.s_oLogger.error("WasdiProcessorEngine.run Exception", e);
			}
			
			return false;
		}
		
		return true;
	}
	
	
}
