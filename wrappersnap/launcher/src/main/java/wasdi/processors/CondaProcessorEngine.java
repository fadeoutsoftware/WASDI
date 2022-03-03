package wasdi.processors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.mozilla.universalchardet.UniversalDetector;

import wasdi.LauncherMain;

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
						
			String sEnvFileEncoding = UniversalDetector.detectCharset(oEnvFile);
			if (sEnvFileEncoding != null) {
				LauncherMain.s_oLogger.info("CondaProcessorEngine.onAfterUnzipProcessor: detected encoding " + sEnvFileEncoding);
			} else {
				LauncherMain.s_oLogger.info("CondaProcessorEngine.onAfterUnzipProcessor: no encoding detected");
				sEnvFileEncoding = StandardCharsets.UTF_8.toString();
			}			
			
			InputStreamReader oFileReader = new InputStreamReader(new FileInputStream(oEnvFile), sEnvFileEncoding);
			
			// Read all the packages requested by the user
			ArrayList<String> asEnvRows = new ArrayList<String>(); 
			
			
			try (BufferedReader oEnvBufferedReader = new BufferedReader(oFileReader)) {
			    String sLine;
			    while ((sLine = oEnvBufferedReader.readLine()) != null) {
			    	asEnvRows.add(sLine);
			    }
			}
			
			LauncherMain.s_oLogger.info("CondaProcessorEngine.onAfterUnzipProcessor: writing new env.yml file");
			
			FileOutputStream oNewEnvFile = new FileOutputStream(oEnvFile);
			 
			BufferedWriter oEnvWriter = new BufferedWriter(new OutputStreamWriter(oNewEnvFile, StandardCharsets.UTF_8));
		 
			for (int iRows = 0; iRows < asEnvRows.size(); iRows++) {
				
				String sRow = asEnvRows.get(iRows);
								
				if (sRow.contains("name: ")) {
					LauncherMain.s_oLogger.info("CondaProcessorEngine.onAfterUnzipProcessor: changing name");
					sRow = "name: base";
				}
				else if (sRow.startsWith("prefix:")) {
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
