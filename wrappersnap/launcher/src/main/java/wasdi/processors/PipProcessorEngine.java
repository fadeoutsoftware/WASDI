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

import wasdi.shared.data.MongoRepository;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.packagemanagers.PipPackageManagerImpl;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class PipProcessorEngine extends DockerProcessorEngine {


	protected String [] m_asDockerTemplatePackages = { "flask", "gunicorn", "requests", "numpy", "pandas", "rasterio", "wheel", "wasdi", "time", "datetime" };

	public PipProcessorEngine() {
		super();
	}

	@Override
	protected IPackageManager getPackageManager(String sIp, int iPort) {
		IPackageManager oPackageManager = new PipPackageManagerImpl(sIp, iPort);

		return oPackageManager;
	}

	@Override
	protected void onAfterUnzipProcessor(String sProcessorFolder) {
		super.onAfterUnzipProcessor(sProcessorFolder);

		try {
			WasdiLog.infoLog("PipProcessorEngine.onAfterUnzipProcessor: sanitize pip.txt");

			// Check the pip file
			File oPipFile = new File(sProcessorFolder+"pip.txt");

			if (!oPipFile.exists()) {
				WasdiLog.infoLog("PipProcessorEngine.onAfterUnzipProcessor: pip file not present, done");
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
			for (int iPackages = 0; iPackages <m_asDockerTemplatePackages.length; iPackages ++) {

				// Take the name
				String sExistingPackage = m_asDockerTemplatePackages[iPackages];

				// Check if it was included also by the user
				if (!Utils.isNullOrEmpty(sExistingPackage)) {
					if (asUserPackages.contains(sExistingPackage)) {

						// Remove it
						WasdiLog.infoLog("PipProcessorEngine.onAfterUnzipProcessor: removing already existing package " + sExistingPackage);
						asUserPackages.remove(sExistingPackage);
					}
				}
			}

			// Do we still have packages
			if (asUserPackages.size() > 0) {
				WasdiLog.infoLog("PipProcessorEngine.onAfterUnzipProcessor: writing new pip.txt file");

				FileOutputStream oNewPipFile = new FileOutputStream(oPipFile);

				BufferedWriter oPipWriter = new BufferedWriter(new OutputStreamWriter(oNewPipFile));

				for (int iPackages = 0; iPackages < asUserPackages.size(); iPackages++) {

					String sPackage = asUserPackages.get(iPackages);

					if (!checkPipPackage(sPackage)) {
						m_oProcessWorkspaceLogger.log("We did not find PIP package [" + sPackage + "], are you sure is correct?");
						WasdiLog.infoLog("We did not find PIP package [" + sPackage + "], jump it");
						continue;
					}
					else {
						WasdiLog.infoLog("Adding [" + sPackage + "]");
						oPipWriter.write(sPackage);
						oPipWriter.newLine();						
					}
				}

				oPipWriter.close();	
				oNewPipFile.flush();
				oNewPipFile.close();
			}
			else {
				WasdiLog.infoLog("PipProcessorEngine.onAfterUnzipProcessor: no more packages after filtering: delete pip file");
				oPipFile.delete();
			}

		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PipProcessorEngine.onAfterUnzipProcessor: exception " + oEx.toString());
		}
		
	}
	
	/**
	 * Checks if a pip package is valid or not. It does this searching for the package json file
	 * @param sPackage
	 * @return
	 */
	protected boolean checkPipPackage(String sPackage) {
		try {
			
			if (Utils.isNullOrEmpty(sPackage)) {
				return false;
			}
			
			if (sPackage.contains("==")) {
				sPackage = sPackage.split("==")[0];
			}

			String sPipApi = "https://pypi.org/pypi/" + sPackage + "/json/";

			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sPipApi, null); 
			String sResult = oHttpCallResponse.getResponseBody();

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
		catch (Exception oExtEx) {
			WasdiLog.infoLog("PipProcessorEngine.checkPipPackage: exception " + oExtEx.toString());
		}

		return false;
	}

}
