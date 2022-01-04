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
			LauncherMain.s_oLogger.info("CondaProcessorEngine.onAfterUnzipProcessor: adjust env.yml");
			
			// Check the pip file
			File oEnvFile = new File(sProcessorFolder+"env.yml");
			
			if (!oEnvFile.exists()) {
				LauncherMain.s_oLogger.info("CondaProcessorEngine.onAfterUnzipProcessor: env file not present, done");
				return;
			}
			
			// Read all the packages requested by the user
			ArrayList<String> asEnvRows = new ArrayList<String>(); 
			
			try (BufferedReader oEnvBufferedReader = new BufferedReader(new FileReader(oEnvFile))) {
			    String sLine;
			    while ((sLine = oEnvBufferedReader.readLine()) != null) {
			    	asEnvRows.add(sLine);
			    }
			}
			
			LauncherMain.s_oLogger.info("CondaProcessorEngine.onAfterUnzipProcessor: writing new env.txt file");
			
			FileOutputStream oNewEnvFile = new FileOutputStream(oEnvFile);
			 
			BufferedWriter oEnvWriter = new BufferedWriter(new OutputStreamWriter(oNewEnvFile));
		 
			for (int iRows = 0; iRows < asEnvRows.size(); iRows++) {
				
				String sRow = asEnvRows.get(iRows);
				
				if (sRow.startsWith("name: ")) {
					LauncherMain.s_oLogger.info("CondaProcessorEngine.onAfterUnzipProcessor: changing name");
					sRow = "name: base";
				}
				else if (sRow.startsWith("prefix: ")) {
					LauncherMain.s_oLogger.info("CondaProcessorEngine.onAfterUnzipProcessor: changing prefix");
					sRow = "prefix: /home/tomcat/miniconda";
				}
								
				oEnvWriter.write(sRow);
				oEnvWriter.newLine();
			}
		 
			oEnvWriter.close();	
			oNewEnvFile.flush();
			oNewEnvFile.close();			
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("CondaProcessorEngine.onAfterUnzipProcessor: exception " + oEx.toString());
		}		
	}
	
}
