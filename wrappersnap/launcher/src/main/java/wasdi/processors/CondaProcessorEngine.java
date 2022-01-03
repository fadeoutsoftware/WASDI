package wasdi.processors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import wasdi.LauncherMain;
import wasdi.shared.utils.Utils;

public class CondaProcessorEngine extends DockerProcessorEngine {
	
	public CondaProcessorEngine() {
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "conda";		
	}

	public CondaProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser) {
		super(sWorkingRootPath, sDockerTemplatePath, sTomcatUser);
		
		m_sDockerTemplatePath = sDockerTemplatePath;		
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += "conda";			
	}
	
	@Override
	protected void onAfterUnzipProcessor(String sProcessorFolder) {
		super.onAfterUnzipProcessor(sProcessorFolder);
		
		try {
			LauncherMain.s_oLogger.info("CondaProcessorEngine.onAfterUnzipProcessor: sanitize pip.txt");
			
			// Check the pip file
			File oPipFile = new File(sProcessorFolder+"env.yml");
			
			if (!oPipFile.exists()) {
				LauncherMain.s_oLogger.info("CondaProcessorEngine.onAfterUnzipProcessor: pip file not present, done");
				return;
			}
			
//			// Read all the packages requested by the user
//			ArrayList<String> asUserPackages = new ArrayList<String>(); 
//			
//			try (BufferedReader oPipBufferedReader = new BufferedReader(new FileReader(oPipFile))) {
//			    String sLine;
//			    while ((sLine = oPipBufferedReader.readLine()) != null) {
//			    	asUserPackages.add(sLine);
//			    }
//			}
//			
//			// For all the packages already included in the docker template
//			for (int iPackages = 0; iPackages <asDockerTemplatePackages.length; iPackages ++) {
//				
//				// Take the name
//				String sExistingPackage = asDockerTemplatePackages[iPackages];
//				
//				// Check if it was included also by the user
//				if (!Utils.isNullOrEmpty(sExistingPackage)) {
//					if (asUserPackages.contains(sExistingPackage)) {
//						
//						// Remove it
//						LauncherMain.s_oLogger.info("CondaProcessorEngine.onAfterUnzipProcessor: removing already existing package " + sExistingPackage);
//						asUserPackages.remove(sExistingPackage);
//					}					
//				}
//			}
//			
//			// Do we still have packages
//			if (asUserPackages.size()>0) {
//				LauncherMain.s_oLogger.info("CondaProcessorEngine.onAfterUnzipProcessor: writing new pip.txt file");
//				
//				FileOutputStream oNewPipFile = new FileOutputStream(oPipFile);
//				 
//				BufferedWriter oPipWriter = new BufferedWriter(new OutputStreamWriter(oNewPipFile));
//			 
//				for (int iPackages = 0; iPackages < asUserPackages.size(); iPackages++) {
//					
//					String sPackage = asUserPackages.get(iPackages);
//					
//					if (!checkPipPackage(sPackage)) {
//						m_oProcessWorkspaceLogger.log("We did not find PIP package [" + sPackage + "], are you sure is correct?");
//					}
//					
//					oPipWriter.write(sPackage);
//					oPipWriter.newLine();
//				}
//			 
//				oPipWriter.close();	
//				oNewPipFile.flush();
//				oNewPipFile.close();
//			}
//			else {
//				LauncherMain.s_oLogger.info("CondaProcessorEngine.onAfterUnzipProcessor: no more packages after filtering: delete pip file");
//				oPipFile.delete();
//			}
			
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("CondaProcessorEngine.onAfterUnzipProcessor: exception " + oEx.toString());
		}		
	}
	
}
