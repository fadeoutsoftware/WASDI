package wasdi.processors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.WorkspaceSharing;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.data.WorkspaceSharingRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipExtractor;

public class IDLProcessorEngine extends WasdiProcessorEngine{
	
    /**
     * Object Mapper
     */
    public static ObjectMapper s_oMapper = new ObjectMapper();

    static  {
        s_oMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

	
	public IDLProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser) {
		super(sWorkingRootPath,sDockerTemplatePath, sTomcatUser);
		
		
		m_sDockerTemplatePath = sDockerTemplatePath;		
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "idl";			
	}

	@Override
	public boolean deploy(ProcessorParameter oParameter) {
		return deploy(oParameter,true);
	}

	public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {
		LauncherMain.s_oLogger.debug("IDLProcessorEngine.DeployProcessor: start");
		
		if (oParameter == null) return false;
		
		ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		ProcessWorkspace oProcessWorkspace = null;		
		
		// First Check if processor exists
		String sProcessorName = oParameter.getName();
		String sProcessorId = oParameter.getProcessorID();
		
		try {
			
			oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oProcessWorkspace = m_oProcessWorkspace;
			
			if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			
			// Set the processor path
			String sDownloadRootPath = m_sWorkingRootPath;
			if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
			
			String sProcessorFolder = sDownloadRootPath+ "processors/" + sProcessorName + "/" ;
			// Create the file
			File oProcessorZipFile = new File(sProcessorFolder + sProcessorId + ".zip");
			
			LauncherMain.s_oLogger.debug("IDLProcessorEngine.DeployProcessor: check processor exists");
			
			// Check it
			if (oProcessorZipFile.exists()==false) {
				LauncherMain.s_oLogger.debug("IDLProcessorEngine.DeployProcessor the Processor [" + sProcessorName + "] does not exists in path " + oProcessorZipFile.getPath());
				if (bFirstDeploy) {
					LauncherMain.s_oLogger.debug("IDLProcessorEngine.DeployProcessor: delete processor db entry");
					oProcessorRepository.deleteProcessor(sProcessorId);
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				}
				return false;
			}
			
			if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 20);
			LauncherMain.s_oLogger.error("IDLProcessorEngine.DeployProcessor: unzip processor");
			
			// Unzip the processor (and check for entry point)
			if (!UnzipProcessor(sProcessorFolder, sProcessorName, sProcessorId + ".zip", oParameter.getProcessObjId())) {
				LauncherMain.s_oLogger.debug("IDLProcessorEngine.DeployProcessor error unzipping the Processor [" + sProcessorName + "]");
				if (bFirstDeploy) {
					LauncherMain.s_oLogger.debug("IDLProcessorEngine.DeployProcessor: delete processor db entry");
					oProcessorRepository.deleteProcessor(sProcessorId);
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				}
				
				try {
					LauncherMain.s_oLogger.debug("IDLProcessorEngine.DeployProcessor: delete processor folder " + sProcessorFolder);
					// If it did not work, clean the folder also
					File oFolder = new File(sProcessorFolder);
					FileUtils.deleteDirectory(oFolder);
				}
				catch (Exception oDelEx) {
					LauncherMain.s_oLogger.error("IDLProcessorEngine.DeployProcessor: error deleting processor folder after bad unzip");
				}
				
				return false;
			}
		    
			if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 40);
			LauncherMain.s_oLogger.debug("IDLProcessorEngine.DeployProcessor: copy container image template");
			
			// Copy Docker template files in the processor folder
			File oDockerTemplateFolder = new File(m_sDockerTemplatePath);
			File oProcessorFolder = new File(sProcessorFolder);
			
			FileUtils.copyDirectory(oDockerTemplateFolder, oProcessorFolder);

			if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 60);

			// Generate the image
			LauncherMain.s_oLogger.debug("IDLProcessorEngine.DeployProcessor: Creating script files");
			
			// Prepare file names
			String sCallIdlFile = sProcessorFolder + "call_idl.pro";
			String sWasdiWrapperFile = sProcessorFolder + "wasdi_wrapper.pro";
			String sRunFile = sProcessorFolder + "run_"+sProcessorName+".sh";
			
			// GENERATE Call IDL File
			File oCallIdlFile = new File (sCallIdlFile);
			
			try (BufferedWriter oCallIdlWriter = new BufferedWriter(new FileWriter(oCallIdlFile))) {
				if(null!= oCallIdlWriter) {
					LauncherMain.s_oLogger.debug("IDLProcessorEngine.DeployProcessor: Creating call_idl.pro file");
					
					oCallIdlWriter.write(".r " + sProcessorFolder + "idlwasdilib.pro");
					oCallIdlWriter.newLine();
					oCallIdlWriter.write("STARTWASDI, '"+sProcessorFolder+"config.properties'");
					oCallIdlWriter.newLine();
					oCallIdlWriter.write(".r "+sProcessorFolder + sProcessorName + ".pro");
					oCallIdlWriter.newLine();
					oCallIdlWriter.write(".r "+sProcessorFolder + "wasdi_wrapper.pro");
					oCallIdlWriter.newLine();
					oCallIdlWriter.write("CALLWASDI");
					oCallIdlWriter.newLine();
					oCallIdlWriter.write("exit");
					oCallIdlWriter.flush();
					oCallIdlWriter.close();
				}				
			}
						
			// GENERATE WASDI WRAPPER File
			File oWasdiWrapperFile = new File (sWasdiWrapperFile);
			
			try (BufferedWriter oWasdiWrapperWriter = new BufferedWriter(new FileWriter(oWasdiWrapperFile))) {
				if(null!= oWasdiWrapperWriter) {
					LauncherMain.s_oLogger.debug("IDLProcessorEngine.DeployProcessor: Creating wasdi_wrapper.pro file");

					oWasdiWrapperWriter.write("PRO CALLWASDI");
					oWasdiWrapperWriter.newLine();
					oWasdiWrapperWriter.write("\tCATCH, Error_status");
					oWasdiWrapperWriter.newLine();				
					oWasdiWrapperWriter.write("\tIF (Error_status NE 0L) THEN BEGIN");
					oWasdiWrapperWriter.newLine();
					oWasdiWrapperWriter.write("\t\tWASDILOG, 'Error message: ' + !ERROR_STATE.MSG");
					oWasdiWrapperWriter.newLine();				
					oWasdiWrapperWriter.write("\t\tEXIT");
					oWasdiWrapperWriter.newLine();				
					oWasdiWrapperWriter.write("\tENDIF");
					oWasdiWrapperWriter.newLine();				
					oWasdiWrapperWriter.write("\t"+sProcessorName);
					oWasdiWrapperWriter.newLine();				
					oWasdiWrapperWriter.write("END");
					oWasdiWrapperWriter.newLine();				
					oWasdiWrapperWriter.flush();
					oWasdiWrapperWriter.close();
				}							
			}
						
			if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 80);
			
			File oRunFile = new File(sRunFile);
			
			try (BufferedWriter oRunWriter = new BufferedWriter(new FileWriter(oRunFile))) {
				if(null!= oRunWriter) {
					LauncherMain.s_oLogger.debug("IDLProcessorEngine.DeployProcessor: Creating run_"+sProcessorName+".sh file");

					oRunWriter.write("#!/bin/bash");
					oRunWriter.newLine();
					oRunWriter.write("wkdir=\""+sProcessorFolder+"\"");
					oRunWriter.newLine();
					oRunWriter.write("script_file=\"${wkdir}/call_idl.pro\"");
					oRunWriter.newLine();
					oRunWriter.write("echo \"executing WASDI idl script...\"");
					oRunWriter.newLine();
					oRunWriter.write("umask 000; /usr/local/bin/idl ${script_file}  &>> /usr/lib/wasdi/launcher/logs/envi.log");
					oRunWriter.newLine();
					oRunWriter.write("echo \"IDL Processor done!\"");
					oRunWriter.newLine();
					oRunWriter.flush();
					oRunWriter.close();
				}			
				
			}
						
			Runtime.getRuntime().exec("chmod u+x "+sRunFile);
			
			if (bFirstDeploy) LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
			
			LauncherMain.s_oLogger.debug("IDLProcessorEngine.DeployProcessor: processor " + sProcessorName + " deployed");
			
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("IDLProcessorEngine.DeployProcessor Exception", oEx);
			try {
				if (bFirstDeploy) {
					
					try {
						oProcessorRepository.deleteProcessor(sProcessorId);
					}
					catch (Exception oInnerEx) {
						LauncherMain.s_oLogger.error("DockerProcessorEngine.DeployProcessor Exception", oInnerEx);
					}					
					
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				}
			} catch (Exception e) {
				LauncherMain.s_oLogger.error("IDLProcessorEngine.DeployProcessor Exception", e);
			}
			return false;
		}
		
		return true;
	}

	public boolean UnzipProcessor(String sProcessorFolder, String sProcessorName, String sZipFileName, String sProcessObjId) {
		try {
			Path oDirPath = Paths.get(sProcessorFolder);
			
			if(!Files.isDirectory(oDirPath)) {
				LauncherMain.s_oLogger.error("IDLProcessorEngine.UnzipProcessor: " + sProcessorFolder + " is not a valid directory, aborting");
				return false;
			}

			if(sZipFileName.contains("/") || sZipFileName.contains("\\") || !sZipFileName.endsWith(".zip") ) {
				LauncherMain.s_oLogger.error("IDLProcessorEngine.UnzipProcessor: " + sZipFileName + " is not a valid zip file name, aborting" );
				return false;
			}
			
			// file name dentro dir esista
			Path oFilePath = Paths.get(sZipFileName);
			
			File oProcessorZipFile = oDirPath.resolve(oFilePath).toAbsolutePath().normalize().toFile();
			if(!oProcessorZipFile.exists()) {
				LauncherMain.s_oLogger.error("IDLProcessorEngine.UnzipProcessor: " + oDirPath.resolve(oFilePath).toAbsolutePath().normalize().toString() + " not found on file system, aborting" );
				return false;
			}
						
			ZipExtractor oZipExtractor = new ZipExtractor(sProcessObjId);
			oZipExtractor.unzip(oProcessorZipFile.getCanonicalPath(), sProcessorFolder);
					        
			// Unzip the file and, meanwhile, check if a pro file with the same name exists
			AtomicBoolean oMyProcessorExists = new AtomicBoolean(false);
			try(Stream<Path> oWalk = Files.walk(Paths.get(sProcessorFolder));){
				oWalk.map(Path::toFile).forEach(oFile->{
					String sFileName = oFile.getName(); 
					if(sFileName.toLowerCase().equals(sProcessorName + ".pro") && !oFile.isDirectory()) {
						oMyProcessorExists.set(true);
					}
				});
			}
			
		    if (!oMyProcessorExists.get()) {
		    	LauncherMain.s_oLogger.warn("IDLProcessorEngine.UnzipProcessor [" + sProcessorName + ".pro] not present in processor " + sZipFileName);
		    	//return false;
		    }
		    
		    try {
			    // Remove the zip?
			    if (oProcessorZipFile.delete()==false) {
			    	LauncherMain.s_oLogger.error("IDLProcessorEngine.UnzipProcessor Error Deleting Zip File");
			    }
		    }
		    catch (Exception e) {
				LauncherMain.s_oLogger.error("IDLProcessorEngine.UnzipProcessor Exception Deleting Zip File", e);
			}
		    
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("IDLProcessorEngine.DeployProcessor Exception", oEx);
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean run(ProcessorParameter oParameter) {
		
		LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: start");
		
		if (oParameter == null) return false;
		
		ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
		ProcessWorkspace oProcessWorkspace = null;		
		
		try {
			
			// Get My Own Process Workspace
			oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oProcessWorkspace = m_oProcessWorkspace;
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
						
			// Check if the processor is avaialbe on the node
			if (!isProcessorOnNode(oParameter)) {
				
				LauncherMain.s_oLogger.error("IDLProcessorEngine.run: processor not available on node: download it");
				
				ProcessorRepository oProcessorRepository = new ProcessorRepository();
				
				Processor oProcessor = oProcessorRepository.getProcessor(oParameter.getProcessorID());
				String sProcessorZipFile = downloadProcessor(oProcessor, oParameter.getSessionID());
				
				if (!Utils.isNullOrEmpty(sProcessorZipFile)) {
					LauncherMain.s_oLogger.error("IDLProcessorEngine.run: processor downloaded, start local deploy");
					deploy(oParameter, false);
				}
				else {
					LauncherMain.s_oLogger.error("IDLProcessorEngine.run: processor not available on node and not downloaded: exit.. ");
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
					return false;
				}
			}

			// First Check if processor exists
			String sProcessorName = oParameter.getName();
			//String sProcessorId = oParameter.getProcessorID();
			
			// Set the processor path
			String sDownloadRootPath = m_sWorkingRootPath;
			if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
			
			String sProcessorFolder = sDownloadRootPath + "processors/" + sProcessorName + "/" ;
			String sRunPath = sProcessorFolder + "run_" + sProcessorName + ".sh";
			// Create the file
			File oProcessorScriptFile = new File(sRunPath);
			
			LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: check if launch script exists");
			
			// Check it
			if (oProcessorScriptFile.exists()==false) {
				LauncherMain.s_oLogger.debug("IDLProcessorEngine.run the script to run Processor [" + sProcessorName + "] does not exists in path " + oProcessorScriptFile.getPath());
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;
			}
			
			// Get The workspace name for parameters
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			List<Workspace> aoWorkspaces = oWorkspaceRepository.getWorkspaceByUser(oParameter.getUserId());
			
			String sWorkspaceName = "";
			for (int iWorkspaces = 0; iWorkspaces<aoWorkspaces.size(); iWorkspaces++) {
				if (aoWorkspaces.get(iWorkspaces).getWorkspaceId().equals(oParameter.getWorkspace())) {
					sWorkspaceName = aoWorkspaces.get(iWorkspaces).getName();
					break;
				}
			}
			
			// Check if it is a shared workspace
			if (Utils.isNullOrEmpty(sWorkspaceName)) {
				
				WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
				// Get the list of workspace shared with this user
				List<WorkspaceSharing> aoSharedWorkspaces = oWorkspaceSharingRepository
						.getWorkspaceSharingByUser(oParameter.getUserId());

				if (aoSharedWorkspaces.size() > 0) {
					// For each
					for (int iWorkspaces = 0; iWorkspaces < aoSharedWorkspaces.size(); iWorkspaces++) {

						// Create View Model
						Workspace oWorkspace = oWorkspaceRepository.getWorkspace(aoSharedWorkspaces.get(iWorkspaces).getWorkspaceId());

						if (oWorkspace == null) {
							Utils.debugLog("IDLProcessorEngine.run: WS Shared not available " + aoSharedWorkspaces.get(iWorkspaces).getWorkspaceId());
							continue;
						}
						
						if (oWorkspace.getWorkspaceId().equals(oParameter.getWorkspace())) {
							sWorkspaceName = oWorkspace.getName();
							break;
						}						

					}
				}				
			}
			
			// Write Param and Config file
			String sConfigFile = sProcessorFolder + "config.properties";
			String sParamFile = sProcessorFolder + "params.txt";
			
			
			File oParameterFile = new File(sParamFile);
			File oConfigFile = new File (sConfigFile);
			
			try (BufferedWriter oConfigWriter = new BufferedWriter(new FileWriter(oConfigFile))) {
				if(null!= oConfigWriter) {
					LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: Creating config.properties file");

					oConfigWriter.write("BASEPATH=" + m_sWorkingRootPath);
					oConfigWriter.newLine();
					oConfigWriter.write("USER=" + oParameter.getUserId());
					oConfigWriter.newLine();
					oConfigWriter.write("WORKSPACE=" + sWorkspaceName);
					oConfigWriter.newLine();
					oConfigWriter.write("SESSIONID="+oParameter.getSessionID());
					oConfigWriter.newLine();
					oConfigWriter.write("ISONSERVER=1");
					oConfigWriter.newLine();
					oConfigWriter.write("DOWNLOADACTIVE=0");
					oConfigWriter.newLine();
					oConfigWriter.write("UPLOADACTIVE=0");
					oConfigWriter.newLine();
					oConfigWriter.write("VERBOSE=0");
					oConfigWriter.newLine();
					oConfigWriter.write("PARAMETERSFILEPATH=" + sParamFile);
					oConfigWriter.newLine();
					oConfigWriter.write("MYPROCID="+oParameter.getProcessObjId());
					oConfigWriter.newLine();				
					oConfigWriter.flush();
					oConfigWriter.close();
				}				
			}
			
			
			try (BufferedWriter oParamWriter = new BufferedWriter(new FileWriter(oParameterFile))) {
				if(oParamWriter != null) {
					
					LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: Creating parameters file " + sParamFile);
					
					// Get the JSON
					String sJson = oParameter.getJson();
					
					LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: JSON " + sJson);
					
					// URL Decode
					try {
					    sJson = java.net.URLDecoder.decode(sJson, StandardCharsets.UTF_8.name());
					    
					    LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: Decoded JSON " + sJson);
					} catch (UnsupportedEncodingException e) {
						LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: Exception decoding JSON " + e.toString());
					}
					
					// Get the JSON as a Map
					Map<String, Object> aoParametersMap = s_oMapper.readValue(sJson, new TypeReference<Map<String,Object>>(){});
					
					// Write KEY=VALUE in the file
					for (String sKey : aoParametersMap.keySet()) {
						oParamWriter.write(sKey+"="+aoParametersMap.get(sKey).toString());
						oParamWriter.newLine();
					}
					
					// Flush and Close
					oParamWriter.flush();
					oParamWriter.close();
				}
			}
			
			
			// Cmq for the shell exex
			String asCmd[] = new String[] {
					sRunPath
			};
			
			LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: shell exec " + Arrays.toString(asCmd));
			ProcessBuilder oProcBuilder = new ProcessBuilder(asCmd);
			Process oProc = oProcBuilder.start();
			            
            LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: waiting for the process to exit");
            
            int iExitStatus = oProc.waitFor(); 
			
			if (iExitStatus == 0) {
				// ok
				LauncherMain.s_oLogger.debug("IDLProcessorEngine.run: process done with code 0");
				if (oProcessWorkspace != null) {
					oProcessWorkspace.setOperationEndDate(Utils.getFormatDate(new Date()));
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
				}
			}
			else {
				// errore
				LauncherMain.s_oLogger.error("IDLProcessorEngine.run: process done with ERROR code = " + iExitStatus);
				if (oProcessWorkspace != null) {
					oProcessWorkspace.setOperationEndDate(Utils.getFormatDate(new Date()));
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				}
			}			
			
		}
		catch (Exception oEx) {
			//String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);
			//if (LauncherMain.s_oSendToRabbit!=null) LauncherMain.s_oSendToRabbit.SendRabbitMessage(false, sOperation, sWorkspace,sError,sExchange);			
			LauncherMain.s_oLogger.error("IDLProcessorEngine.run Exception", oEx);
			try {
				if (oProcessWorkspace != null) oProcessWorkspace.setOperationEndDate(Utils.getFormatDate(new Date()));
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			} catch (Exception e) {
				LauncherMain.s_oLogger.error("IDLProcessorEngine.run Exception", e);
			}
			return false;
		}
		
		return true;
	}

	public boolean delete(ProcessorParameter oParameter) {
		
		if (oParameter == null) {
			LauncherMain.s_oLogger.error("IDLProcessorEngine.delete: oParameter is null");
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
				LauncherMain.s_oLogger.error("IDLProcessorEngine.delete: oProcessor is already null in the db [" + sProcessorId +"]. Try to delete folder");
			}
			else {
				if (!oParameter.getUserId().equals(oProcessor.getUserId())) {
					LauncherMain.s_oLogger.error("IDLProcessorEngine.delete: oProcessor is not of user [" + oParameter.getUserId() +"]. Exit");
					return false;
				}
			}
			
			LauncherMain.s_oLogger.error("IDLProcessorEngine.delete: Deleting Processor " + sProcessorName + " of User " + oParameter.getUserId());
			
			// Set the processor path
			String sDownloadRootPath = m_sWorkingRootPath;
			if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
			
			// delete the folder
			String sProcessorFolder = sDownloadRootPath+ "/processors/" + sProcessorName + "/" ;
			LauncherMain.s_oLogger.error("IDLProcessorEngine.delete: Deleting Processor Folder");
			File oProcessorFolder = new File(sProcessorFolder);
			FileUtils.deleteDirectory(oProcessorFolder);
			
			// Update the user
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 66);
			
			if (oProcessor != null) { 
				// delete the db entry
				LauncherMain.s_oLogger.error("IDLProcessorEngine.delete: Deleting Processor Db Entry");
				oProcessorRepository.deleteProcessor(sProcessorId);				
			}
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);			
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("IDLProcessorEngine.delete Exception", oEx);
			try {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			} catch (Exception e) {
				LauncherMain.s_oLogger.error("IDLProcessorEngine.delete Exception", e);
			}
			
			return false;
		}

		return true;
	}
	
	@Override
	public boolean redeploy(ProcessorParameter oParameter) {
		return libraryUpdate(oParameter);
	}
	
	@Override
	public boolean libraryUpdate(ProcessorParameter oParameter) {
		
		ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
		ProcessWorkspace oProcessWorkspace = null;				
		
		try {
			oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oProcessWorkspace = m_oProcessWorkspace;
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
			
			String sIDLWasdiLibFile = m_sDockerTemplatePath;
			
			if (!sIDLWasdiLibFile.endsWith("/")) sIDLWasdiLibFile += "/";
			
			sIDLWasdiLibFile += "idlwasdilib.pro";
			
			// Copy Docker template files in the processor folder
			File oIDLLibFile = new File(sIDLWasdiLibFile);
			
			if (!oIDLLibFile.exists()) {
				LauncherMain.s_oLogger.error("IDLProcessorEngine.libraryUpdate: impossibile to find the lib file " + oIDLLibFile.getPath());
				return false;
			}
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 20);
			
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(oParameter.getProcessorID());
			
			// Check processor
			if (oProcessor == null) { 
				LauncherMain.s_oLogger.error("IDLProcessorEngine.libraryUpdate: oProcessor is null [" + oParameter.getProcessorID() +"]");
				return false;
			}
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 30);
			
			// Set the processor path
			String sDownloadRootPath = m_sWorkingRootPath;
			if (!sDownloadRootPath.endsWith("/")) sDownloadRootPath = sDownloadRootPath + "/";
			
			String sProcessorFolder = sDownloadRootPath+ "/processors/" + oParameter.getName() + "/" ;
			
			FileUtils.copyFileToDirectory(oIDLLibFile, new File(sProcessorFolder));
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("IDLProcessorEngine.libraryUpdate: exception " + oEx.toString());
			return false;
		}
		
		return true;
	}
	
}
