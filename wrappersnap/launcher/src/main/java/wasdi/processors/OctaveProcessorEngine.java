package wasdi.processors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import wasdi.LauncherMain;
import wasdi.shared.parameters.ProcessorParameter;

public class OctaveProcessorEngine extends DockerProcessorEngine {

	public OctaveProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser)  {
		super(sWorkingRootPath,sDockerTemplatePath, sTomcatUser);

		m_sDockerTemplatePath = sDockerTemplatePath;		
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "octave";
		
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
		
		// Generate shell script file
		String sMainFile = sProcessorFolder+"myProcessor.m";
		
		File oMainFile = new File(sMainFile);
		
		try (BufferedWriter oMainFileWriter = new BufferedWriter(new FileWriter(oMainFile))) {
			// Fill the script file
			if(oMainFileWriter != null ) {
				LauncherMain.s_oLogger.debug("OctaveProcessorEngine.deploy: Creating "+sMainFile+" file");

				oMainFileWriter.write("function myProcessor()");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\taddpath(\"/home/wasdi/\");");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\tWasdi = startWasdi(\"/home/wasdi/config.properties\")");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\ttry");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\t\twLog(Wasdi, \"Starting Octave WASDI App\");");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\t\t" + sProcessorName + "(Wasdi)");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\t\tsMyProcId = wGetMyProcId(Wasdi);");				
				oMainFileWriter.newLine();
				oMainFileWriter.write("\t\tsOutStatus = wGetProcessStatus(Wasdi, sMyProcId);");				
				oMainFileWriter.newLine();
				oMainFileWriter.write("\t\tif ( !strcmp(sOutStatus,\"DONE\") && !strcmp(sOutStatus,\"ERROR\") && !strcmp(sOutStatus,\"STOPPED\"))");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\t\t\twLog(Wasdi, \"Forcing status DONE\");");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\t\t\twUpdateStatus(Wasdi, \"DONE\");");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\t\tendif");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\t\twLog(Wasdi, \"Octave Application Done, bye\");");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\tcatch oError");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\twLog(Wasdi, [\"Error:\",oError.message])");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\twUpdateStatus(Wasdi, \"ERROR\")");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\tend_try_catch");
				oMainFileWriter.newLine();				

				oMainFileWriter.flush();
				oMainFileWriter.close();
			}
		} catch (IOException e) {
			LauncherMain.s_oLogger.debug("OctaveProcessorEngine.deploy: Exception Creating Main File :"+e.toString());
		}
		
		LauncherMain.s_oLogger.debug("OctaveProcessorEngine.deploy: call super Docker Proc Engine deploy method");
		
		return super.deploy(oParameter, bFirstDeploy);
	}

}
