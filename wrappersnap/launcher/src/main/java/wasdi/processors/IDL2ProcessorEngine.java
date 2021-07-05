package wasdi.processors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import com.google.common.io.Files;

import wasdi.LauncherMain;
import wasdi.shared.parameters.ProcessorParameter;

public class IDL2ProcessorEngine extends DockerProcessorEngine {

	public IDL2ProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser) {
		super(sWorkingRootPath, sDockerTemplatePath, sTomcatUser);
		
		m_sDockerTemplatePath = sDockerTemplatePath;		
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "idl";		
	}
	
	@Override
	protected void onAfterUnzipProcessor(String sProcessorFolder) {
		
		try {
			File oProcessorFolder = new File(sProcessorFolder);
			if (!oProcessorFolder.exists()) return;
			
			if (!sProcessorFolder.endsWith(""+File.separatorChar)) sProcessorFolder = sProcessorFolder + File.separatorChar;
			
			String sRunscript = sProcessorFolder + "runwasdidocker.sh";
			
			File oRunScriptFile = new File(sRunscript);
			
			if (oRunScriptFile.exists()) {
				if (!oRunScriptFile.delete()) {
					LauncherMain.s_oLogger.debug("IDL2ProcessorEngine.onAfterUnzipProcessor: ERROR deleting runwasdidocker.sh");
				}
			}
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.debug("IDL2ProcessorEngine.onAfterUnzipProcessor: Exception :"+oEx.toString());
		}
		
		//super.onAfterUnzipProcessor(sProcessorFolder);
	}

	/**
	 * After copy template, overwrite call_idl, wasdi_wrapper e run processor
	 */
	@Override
	protected void onAfterCopyTemplate(String sProcessorFolder) {
		
		// Docker local processor folder
		String sLocalProcessorFolder = "/home/wasdi/";
		
		String sProcessorName = m_oParameter.getName();
		
		// Node processor folder
		File oProcessorFolder = new File(sProcessorFolder);
		
		// Do we have the mpfit lib to include?
		boolean bAddLibraryFit = false;
		
		if (oProcessorFolder.exists()) {
			File [] aoFiles = oProcessorFolder.listFiles();
			
			for (File oFile : aoFiles) {
				if (!oFile.isDirectory()) continue;
				if (oFile.getName().equals("mpfit")) {
					bAddLibraryFit = true;
					break;
				}
			}
		}
		
		try {
			// Prepare file names
			String sCallIdlFile = sProcessorFolder + "/call_idl.pro";
			String sWasdiWrapperFile = sProcessorFolder + "/wasdi_wrapper.pro";
			String sRunFile = sProcessorFolder + "/runProcessor.sh";
						
			// GENERATE Call IDL File
			File oCallIdlFile = new File (sCallIdlFile);
			
			try (BufferedWriter oCallIdlWriter = new BufferedWriter(new FileWriter(oCallIdlFile))) {
				if(null!= oCallIdlWriter) {
					LauncherMain.s_oLogger.debug("IDL2ProcessorEngine.DeployProcessor: Creating call_idl.pro file");
					
					if (bAddLibraryFit) {
						oCallIdlWriter.write("!PATH = EXPAND_PATH('<IDL_DEFAULT>:+" + sLocalProcessorFolder + "mpfit')");
						oCallIdlWriter.newLine();						
					}
					
					oCallIdlWriter.write(".r " + sLocalProcessorFolder + "idlwasdilib.pro");
					oCallIdlWriter.newLine();
					oCallIdlWriter.write("STARTWASDI, '"+sLocalProcessorFolder+"config.properties'");
					oCallIdlWriter.newLine();
					oCallIdlWriter.write(".r "+sLocalProcessorFolder + sProcessorName + ".pro");
					oCallIdlWriter.newLine();
					
					if (bAddLibraryFit) {
						oCallIdlWriter.write(".compile mpcurvefit");
						oCallIdlWriter.newLine();						
					}
					
					oCallIdlWriter.write(".r "+sLocalProcessorFolder + "wasdi_wrapper.pro");
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
					LauncherMain.s_oLogger.debug("IDL2ProcessorEngine.DeployProcessor: Creating wasdi_wrapper.pro file");

					oWasdiWrapperWriter.write("PRO CALLWASDI");
					oWasdiWrapperWriter.newLine();
					oWasdiWrapperWriter.write("\tCATCH, Error_status");
					oWasdiWrapperWriter.newLine();				
					oWasdiWrapperWriter.write("\tIF (Error_status NE 0L) THEN BEGIN");
					oWasdiWrapperWriter.newLine();
					oWasdiWrapperWriter.write("\t\tstatus=WASDIUPDATESTATUS('ERROR', -1)");
					oWasdiWrapperWriter.newLine();									
					oWasdiWrapperWriter.write("\t\tWASDILOG, 'Error message: ' + !ERROR_STATE.MSG");
					oWasdiWrapperWriter.newLine();				
					oWasdiWrapperWriter.write("\t\tEXIT");
					oWasdiWrapperWriter.newLine();				
					oWasdiWrapperWriter.write("\tENDIF");
					oWasdiWrapperWriter.newLine();				
					oWasdiWrapperWriter.write("\t"+sProcessorName);
					oWasdiWrapperWriter.newLine();				
					oWasdiWrapperWriter.write("\tstatus=WASDIUPDATESTATUS('DONE', -1)");
					oWasdiWrapperWriter.newLine();				
					oWasdiWrapperWriter.write("END");
					oWasdiWrapperWriter.newLine();				
					oWasdiWrapperWriter.flush();
					oWasdiWrapperWriter.close();
				}							
			}
			
			File oRunFile = new File(sRunFile);
			
			try (BufferedWriter oRunWriter = new BufferedWriter(new FileWriter(oRunFile))) {
				if(null!= oRunWriter) {
					LauncherMain.s_oLogger.debug("IDL2ProcessorEngine.DeployProcessor: Creating runProcessor.sh file");

					oRunWriter.write("#!/bin/bash");
					oRunWriter.newLine();
					oRunWriter.write("wkdir=\""+sLocalProcessorFolder+"\"");
					oRunWriter.newLine();
					oRunWriter.write("script_file=\"${wkdir}/call_idl.pro\"");
					oRunWriter.newLine();
					oRunWriter.write("echo \"executing WASDI idl script...\"");
					oRunWriter.newLine();
					oRunWriter.write("umask 000; /usr/local/bin/idl ${script_file}");
					oRunWriter.newLine();
					oRunWriter.write("echo \"IDL Processor done!\"");
					oRunWriter.newLine();
					oRunWriter.flush();
					oRunWriter.close();
				}			
				
			}
						
			Runtime.getRuntime().exec("chmod u+x "+sRunFile);			

			
		} catch (Exception e) {
			LauncherMain.s_oLogger.debug("IDL2ProcessorEngine.deploy: Exception Creating Files :"+e.toString());
		}
	}
	
	@Override
	protected void onAfterDeploy(String sProcessorFolder) {
		try {
			File oFile = new File(sProcessorFolder+"envi552-linux.tar");
			oFile.delete();
			oFile = new File(sProcessorFolder+"o_licenseserverurl.txt");
			oFile.delete();
			oFile = new File(sProcessorFolder+"install.sh");
			oFile.delete();
		}
		catch (Exception e) {
			LauncherMain.s_oLogger.debug("IDL2ProcessorEngine.onAfterDeploy: Exception Deleting install files: "+e.toString());
		}
	}

	
	@Override
	public boolean libraryUpdate(ProcessorParameter oParameter) {
		
		try {
			LauncherMain.s_oLogger.debug("IDL2ProcessorEngine.libraryUpdate: move lib in the processor folder");
			
			// Get the lib path
			String sLibFilePath = m_sDockerTemplatePath;
			
			if (!sLibFilePath.endsWith(File.separator)) {
				sLibFilePath += File.separator;
			}
			
			// Get the processor Path
			String sDestinationFilePath = m_sWorkingRootPath;
			if (!sDestinationFilePath.endsWith(File.separator)) {
				sDestinationFilePath+=File.separator;
			}
			
			sDestinationFilePath = sDestinationFilePath+ "processors" + File.separator + m_oParameter.getName() + File.separator;
			
			File oLibFile = new File(sLibFilePath+"idlwasdilib.pro");
			File oDestinationFile = new File(sDestinationFilePath+"idlwasdilib.pro");
			
			// Copy the lib
			Files.copy(oLibFile, oDestinationFile);
			
			LauncherMain.s_oLogger.debug("IDL2ProcessorEngine.libraryUpdate: call super implementation to update the docker");
			
			return super.libraryUpdate(oParameter);
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.debug("IDL2ProcessorEngine.libraryUpdate: Exception in lib update: " + oEx.toString());
			return false;
		}
	}
}
