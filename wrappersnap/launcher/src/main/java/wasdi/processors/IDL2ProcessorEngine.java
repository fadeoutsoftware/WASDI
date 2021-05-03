package wasdi.processors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

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
	protected void handleRunCommand(String sCommand, ArrayList<String> asArgs) {

	}

	@Override
	protected void handleBuildCommand(String sCommand, ArrayList<String> asArgs) {

	}

	@Override
	protected void handleUnzippedProcessor(String sProcessorFolder) {

	}

	@Override
	public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {
		
		String sProcessorName = oParameter.getName();
		
		// Set the processor path
		String sDownloadRootPath = m_sWorkingRootPath;
		if (!sDownloadRootPath.endsWith(File.separator)) sDownloadRootPath = sDownloadRootPath + File.separator;
		
		String sProcessorFolder = sDownloadRootPath+ "processors" + File.separator + sProcessorName + File.separator;
		String sLocalProcessorFolder = "/home/wasdi/"; 
		
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
					
					oCallIdlWriter.write(".r " + sLocalProcessorFolder + "idlwasdilib.pro");
					oCallIdlWriter.newLine();
					oCallIdlWriter.write("STARTWASDI, '"+sLocalProcessorFolder+"config.properties'");
					oCallIdlWriter.newLine();
					oCallIdlWriter.write(".r "+sLocalProcessorFolder + sProcessorName + ".pro");
					oCallIdlWriter.newLine();
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
		
		LauncherMain.s_oLogger.debug("IDL2ProcessorEngine.deploy: call super Docker Proc Engine deploy method");
		
		return super.deploy(oParameter);
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
			// TODO: handle exception
		}
	}

}
