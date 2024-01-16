package wasdi.processors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.log.WasdiLog;

public class OctaveProcessorEngine extends DockerBuildOnceEngine {
	
	public OctaveProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.OCTAVE);		
	}
	
	@Override
	protected IPackageManager getPackageManager(String sUrl) {
		return null;
	}
		
	@Override
	public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {
		
		WasdiLog.debugLog("OctaveProcessorEngine.deploy: Calling base class deploy");
		return super.deploy(oParameter, bFirstDeploy);
	}
	
	@Override
	protected void onAfterCopyTemplate(String sProcessorFolder, Processor oProcessor) {
		// Generate shell script file
		String sMainFile = sProcessorFolder+"myProcessor.m";
		
		String sApplicationHome = "/home/appwasdi/application/";
		
		File oMainFile = new File(sMainFile);
		
		try (BufferedWriter oMainFileWriter = new BufferedWriter(new FileWriter(oMainFile))) {
			// Fill the script file
			if(oMainFileWriter != null ) {
				WasdiLog.debugLog("OctaveProcessorEngine.onAfterDeploy: Creating "+sMainFile+" file");

				oMainFileWriter.write("function myProcessor()");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\taddpath(\""+sApplicationHome+"\");");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\tWasdi = startWasdi(\""+sApplicationHome+"config.properties\")");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\ttry");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\t\twLog(Wasdi, \"Starting Octave WASDI App\");");
				oMainFileWriter.newLine();
				oMainFileWriter.write("\t\t" + oProcessor.getName() + "(Wasdi)");
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
			WasdiLog.debugLog("OctaveProcessorEngine.onAfterDeploy: Exception Creating Main File :"+e.toString());
		}
	}
}
