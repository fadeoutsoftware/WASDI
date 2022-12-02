package wasdi.processors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.google.common.io.Files;

import wasdi.shared.managers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.log.WasdiLog;

public class OctaveProcessorEngine extends DockerProcessorEngine {
	
	public OctaveProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "octave";		
	}

	public OctaveProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser)  {
		super(sWorkingRootPath,sDockerTemplatePath, sTomcatUser);

		m_sDockerTemplatePath = sDockerTemplatePath;		
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "octave";
		
	}

	@Override
	protected IPackageManager getPackageManager(String sIp, int iPort) {
		throw new UnsupportedOperationException("The functionality is not yet implemented for this processor engine!");
	}
		
	@Override
	public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {
		
		String sProcessorName = oParameter.getName();
		
		String sProcessorFolder = getProcessorFolder(sProcessorName);
		
		// Generate shell script file
		String sMainFile = sProcessorFolder+"myProcessor.m";
		
		File oMainFile = new File(sMainFile);
		
		try (BufferedWriter oMainFileWriter = new BufferedWriter(new FileWriter(oMainFile))) {
			// Fill the script file
			if(oMainFileWriter != null ) {
				WasdiLog.debugLog("OctaveProcessorEngine.deploy: Creating "+sMainFile+" file");

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
			WasdiLog.debugLog("OctaveProcessorEngine.deploy: Exception Creating Main File :"+e.toString());
		}
		
		WasdiLog.debugLog("OctaveProcessorEngine.deploy: call super Docker Proc Engine deploy method");
		
		return super.deploy(oParameter, bFirstDeploy);
	}
	
	
	@Override
	public boolean libraryUpdate(ProcessorParameter oParameter) {
		
		try {
			WasdiLog.debugLog("OctaveProcessorEngine.libraryUpdate: move lib in the processor folder");
			
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

			// Lib folder
			File oLibFolder = new File(sLibFilePath);
						
			if (oLibFolder.exists()) {
				File [] aoFiles = oLibFolder.listFiles();
				
				for (File oFile : aoFiles) {
					
					if (oFile.getName().endsWith(".m") || oFile.getName().endsWith(".jar")) {
						// Create the file in the destination folder
						File oDestinationFile = new File(sDestinationFilePath+oFile.getName());
						// Copy the file
						Files.copy(oFile, oDestinationFile);						
					}
				}
			}
						
			WasdiLog.debugLog("OctaveProcessorEngine.libraryUpdate: call super implementation to update the docker");
			
			return super.libraryUpdate(oParameter);
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("IDL2ProcessorEngine.libraryUpdate: Exception in lib update: " + oEx.toString());
			return false;
		}
	}

}
