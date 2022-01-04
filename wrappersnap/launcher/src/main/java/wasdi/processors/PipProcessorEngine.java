package wasdi.processors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.core.type.TypeReference;

import wasdi.LauncherMain;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;

public class PipProcessorEngine extends DockerProcessorEngine {
	
	
	protected String [] asDockerTemplatePackages = { "flask", "gunicorn", "requests", "numpy", "pandas", "rasterio", "wheel", "wasdi" };
	
	public PipProcessorEngine() {
		super();
		
	}

	public PipProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser)  {
		super(sWorkingRootPath,sDockerTemplatePath, sTomcatUser);		
	}
	
	@Override
	protected void onAfterUnzipProcessor(String sProcessorFolder) {
		super.onAfterUnzipProcessor(sProcessorFolder);
		
		try {
			LauncherMain.s_oLogger.info("PipProcessorEngine.onAfterUnzipProcessor: sanitize pip.txt");
			
			// Check the pip file
			File oPipFile = new File(sProcessorFolder+"pip.txt");
			
			if (!oPipFile.exists()) {
				LauncherMain.s_oLogger.info("PipProcessorEngine.onAfterUnzipProcessor: pip file not present, done");
				return;
			}
			
			// Read all the packages requested by the user
			ArrayList<String> asUserPackages = new ArrayList<String>(); 
			
			try (BufferedReader oPipBufferedReader = new BufferedReader(new FileReader(oPipFile))) {
			    String sLine;
			    while ((sLine = oPipBufferedReader.readLine()) != null) {
			    	asUserPackages.add(sLine);
			    }
			}
			
			// For all the packages already included in the docker template
			for (int iPackages = 0; iPackages <asDockerTemplatePackages.length; iPackages ++) {
				
				// Take the name
				String sExistingPackage = asDockerTemplatePackages[iPackages];
				
				// Check if it was included also by the user
				if (!Utils.isNullOrEmpty(sExistingPackage)) {
					if (asUserPackages.contains(sExistingPackage)) {
						
						// Remove it
						LauncherMain.s_oLogger.info("PipProcessorEngine.onAfterUnzipProcessor: removing already existing package " + sExistingPackage);
						asUserPackages.remove(sExistingPackage);
					}					
				}
			}
			
			// Do we still have packages
			if (asUserPackages.size()>0) {
				LauncherMain.s_oLogger.info("PipProcessorEngine.onAfterUnzipProcessor: writing new pip.txt file");
				
				FileOutputStream oNewPipFile = new FileOutputStream(oPipFile);
				 
				BufferedWriter oPipWriter = new BufferedWriter(new OutputStreamWriter(oNewPipFile));
			 
				for (int iPackages = 0; iPackages < asUserPackages.size(); iPackages++) {
					
					String sPackage = asUserPackages.get(iPackages);
					
					if (!checkPipPackage(sPackage)) {
						m_oProcessWorkspaceLogger.log("We did not find PIP package [" + sPackage + "], are you sure is correct?");
					}
					
					oPipWriter.write(sPackage);
					oPipWriter.newLine();
				}
			 
				oPipWriter.close();	
				oNewPipFile.flush();
				oNewPipFile.close();
			}
			else {
				LauncherMain.s_oLogger.info("PipProcessorEngine.onAfterUnzipProcessor: no more packages after filtering: delete pip file");
				oPipFile.delete();
			}
			
		}
		catch (Exception oEx) {
			LauncherMain.s_oLogger.error("PipProcessorEngine.onAfterUnzipProcessor: exception " + oEx.toString());
		}
		
	}
	
	protected boolean checkPipPackage(String sPackage) {
		try {
			
			String sPipApi = "https://pypi.org/pypi/" + sPackage + "/json/";
			
			String sResult = HttpUtils.httpGet(sPipApi, null);
			
			if (Utils.isNullOrEmpty(sResult)==false) {
				try {
					TypeReference<HashMap<String,Object>> oMapType = new TypeReference<HashMap<String,Object>>() {};
					HashMap<String,Object> oResults = MongoRepository.s_oMapper.readValue(sResult, oMapType);			
					
					if (oResults.containsKey("info")) {
						return true;
					}
				}
				catch (Exception oEx) {
				}				
			}
		}
		catch (Exception e) {
		}
		
		return false;
	}

}
