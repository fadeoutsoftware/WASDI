package wasdi.processors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;

public class IDL2ProcessorEngine extends DockerBuildOnceEngine {
	
	public IDL2ProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.IDL);		
	}
	
	@Override
	protected IPackageManager getPackageManager(String sUrl) {
		throw new UnsupportedOperationException("The functionality is not yet implemented for this processor engine!");
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
					WasdiLog.debugLog("IDL2ProcessorEngine.onAfterUnzipProcessor: ERROR deleting runwasdidocker.sh");
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("IDL2ProcessorEngine.onAfterUnzipProcessor: Exception :"+oEx.toString());
		}
	}

	/**
	 * After copy template, overwrite call_idl, wasdi_wrapper e run processor
	 */
	@Override
	protected void onAfterCopyTemplate(String sProcessorFolder, Processor oProcessor) {
		
		// Docker local processor folder
		String sLocalProcessorFolder = "/home/appwasdi/application/";
		
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
			String sGetPIDFile = sProcessorFolder + "/getPid.sh";
						
			// GENERATE Call IDL File
			File oCallIdlFile = new File (sCallIdlFile);
			
			try (BufferedWriter oCallIdlWriter = new BufferedWriter(new FileWriter(oCallIdlFile))) {
				if(null!= oCallIdlWriter) {
					WasdiLog.debugLog("IDL2ProcessorEngine.DeployProcessor: Creating call_idl.pro file");
					
					oCallIdlWriter.write("iArgs = 0");
					oCallIdlWriter.newLine();
					
					oCallIdlWriter.write("aoArgs = COMMAND_LINE_ARGS(COUNT=iArgs)");
					oCallIdlWriter.newLine();
					
					oCallIdlWriter.write("sConfigFile = 'config.properties'");
					oCallIdlWriter.newLine();					

					oCallIdlWriter.write("IF (iArgs GT 0) THEN sConfigFile=aoArgs[0]");
					oCallIdlWriter.newLine();

					oCallIdlWriter.write("sConfigFile = '"+sLocalProcessorFolder+"'+sConfigFile");
					oCallIdlWriter.newLine();

					oCallIdlWriter.write("print, 'Config File ', sConfigFile");
					oCallIdlWriter.newLine();		

					if (bAddLibraryFit) {
						oCallIdlWriter.write("!PATH = EXPAND_PATH('<IDL_DEFAULT>:+" + sLocalProcessorFolder + "mpfit')");
						oCallIdlWriter.newLine();						
					}
					
					oCallIdlWriter.write(".r " + sLocalProcessorFolder + "idlwasdilib.pro");
					oCallIdlWriter.newLine();
					oCallIdlWriter.write("STARTWASDI, sConfigFile");
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
					WasdiLog.debugLog("IDL2ProcessorEngine.DeployProcessor: Creating wasdi_wrapper.pro file");

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
						
			RunTimeUtils.addRunPermission(sRunFile);
			RunTimeUtils.addRunPermission(sGetPIDFile);
			
		} catch (Exception e) {
			WasdiLog.debugLog("IDL2ProcessorEngine.deploy: Exception Creating Files :"+e.toString());
		}
	}
	
	@Override
	protected void onAfterDeploy(String sProcessorFolder, Processor oProcessor) {
		try {
			String sFileName = sProcessorFolder+"envi552-linux.tar";
			File oFile = new File(sFileName);
			boolean bIsFileDeleted = false;
			if (oFile.exists()) {
				bIsFileDeleted = oFile.delete();
				if (!bIsFileDeleted) 
					WasdiLog.debugLog("IDL2ProcessorEngine.onAfterDeploy: error deleting file " + sFileName); 				
			}
			
			
			sFileName = sProcessorFolder+"o_licenseserverurl.txt";
			oFile = new File(sFileName);
			if (oFile.exists()) {
				bIsFileDeleted = oFile.delete();
				if (!bIsFileDeleted) 
					WasdiLog.debugLog("IDL2ProcessorEngine.onAfterDeploy: error deleting file " + sFileName);				
			}
			
			
			sFileName = sProcessorFolder + "install.sh";
			oFile = new File(sFileName);			
			if (oFile.exists()) {
				bIsFileDeleted = oFile.delete();
				if (!bIsFileDeleted) 
					WasdiLog.debugLog("IDL2ProcessorEngine.onAfterDeploy: error deleting file " + sFileName);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("IDL2ProcessorEngine.onAfterDeploy: Exception Deleting install files: ", oEx);
		}
	}
	
	@Override
	public boolean environmentUpdate(ProcessorParameter oParameter) {
		WasdiLog.errorLog("IDL2ProcessorEngine.onAfterDeploy: there is environment in IDL apps");
		return true;
	}
}
