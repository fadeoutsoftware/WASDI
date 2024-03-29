package wasdi.processors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.mozilla.universalchardet.UniversalDetector;

import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.packagemanagers.CondaPackageManagerImpl;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Processor Engine dedicated to a python Conda Application
 * @author p.campanella
 *
 */
public class CondaProcessorEngine extends DockerBuildOnceEngine {
	
	public CondaProcessorEngine() {
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.CONDA);		
	}
	
	@Override
	protected IPackageManager getPackageManager(String sUrl) {
		IPackageManager oPackageManager = new CondaPackageManagerImpl(sUrl);

		return oPackageManager;
	}

	@Override
	protected void onAfterUnzipProcessor(String sProcessorFolder) {
		super.onAfterUnzipProcessor(sProcessorFolder);
		
		InputStreamReader oFileReader = null;
		
		
		try {
			WasdiLog.infoLog("CondaProcessorEngine.onAfterUnzipProcessor: adjust env.yml");
			
			// Check the pip file
			File oEnvFile = new File(sProcessorFolder+"env.yml");
			
			if (!oEnvFile.exists()) {
				WasdiLog.infoLog("CondaProcessorEngine.onAfterUnzipProcessor: env file not present, done");
				return;
			}
						
			String sEnvFileEncoding = UniversalDetector.detectCharset(oEnvFile);
			if (sEnvFileEncoding != null) {
				WasdiLog.infoLog("CondaProcessorEngine.onAfterUnzipProcessor: detected encoding " + sEnvFileEncoding);
			} else {
				WasdiLog.infoLog("CondaProcessorEngine.onAfterUnzipProcessor: no encoding detected");
				sEnvFileEncoding = StandardCharsets.UTF_8.toString();
			}			
			
			oFileReader = new InputStreamReader(new FileInputStream(oEnvFile), sEnvFileEncoding);
			
			// Read all the packages requested by the user
			ArrayList<String> asEnvRows = new ArrayList<String>(); 
			
			
			try (BufferedReader oEnvBufferedReader = new BufferedReader(oFileReader)) {
			    String sLine;
			    while ((sLine = oEnvBufferedReader.readLine()) != null) {
			    	asEnvRows.add(sLine);
			    }
			}
			
			WasdiLog.infoLog("CondaProcessorEngine.onAfterUnzipProcessor: writing new env.yml file");
			
			try (FileOutputStream oNewEnvFile = new FileOutputStream(oEnvFile);
					BufferedWriter oEnvWriter = new BufferedWriter(new OutputStreamWriter(oNewEnvFile, StandardCharsets.UTF_8)); ) {
			 
				for (int iRows = 0; iRows < asEnvRows.size(); iRows++) {
					
					String sRow = asEnvRows.get(iRows);
									
					if (sRow.contains("name: ")) {
						WasdiLog.infoLog("CondaProcessorEngine.onAfterUnzipProcessor: changing name");
						sRow = "name: base";
					}
					else if (sRow.startsWith("prefix:")) {
						WasdiLog.infoLog("CondaProcessorEngine.onAfterUnzipProcessor: changing prefix");
						sRow = "prefix: /home/" + WasdiConfig.Current.systemUserName + "/venv/bin";
					}
									
					oEnvWriter.write(sRow);
					oEnvWriter.newLine();
				}
			 
				oNewEnvFile.flush();
			}
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CondaProcessorEngine.onAfterUnzipProcessor: exception ", oEx);
		} finally {
			try {
				if (oFileReader != null) oFileReader.close();
			} catch (IOException oEx) {
				WasdiLog.errorLog("CondaProcessorEngine.onAfterUnzipProcessor: exception when closing resources", oEx);
			}
		}
	}
	
}
