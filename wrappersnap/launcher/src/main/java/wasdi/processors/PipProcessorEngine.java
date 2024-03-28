package wasdi.processors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.packagemanagers.PipPackageManagerImpl;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.packagemanagers.PackageManagerUtils;

public class PipProcessorEngine extends DockerProcessorEngine {


	protected String [] m_asDockerTemplatePackages = { "flask", "gunicorn", "requests", "numpy", "wheel", "wasdi", "time", "datetime" };

	public PipProcessorEngine() {
		super();
	}

	@Override
	protected IPackageManager getPackageManager(String sUrl) {
		IPackageManager oPackageManager = new PipPackageManagerImpl(sUrl);

		return oPackageManager;
	}

	@Override
	protected void onAfterUnzipProcessor(String sProcessorFolder) {
		super.onAfterUnzipProcessor(sProcessorFolder);
		
		FileOutputStream oNewPipFile = null;

		BufferedWriter oPipWriter = null;

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

				oNewPipFile = new FileOutputStream(oPipFile);

				oPipWriter = new BufferedWriter(new OutputStreamWriter(oNewPipFile));

				for (int iPackages = 0; iPackages < asUserPackages.size(); iPackages++) {

					String sPackage = asUserPackages.get(iPackages);

					if (!PackageManagerUtils.checkPipPackage(sPackage)) {
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

				oNewPipFile.flush();
			}
			else {
				boolean bIsfileDeleted = oPipFile.delete();
				WasdiLog.infoLog("PipProcessorEngine.onAfterUnzipProcessor. No more packages after filtering, result of the deletion of pip file: " + bIsfileDeleted);
			}

		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PipProcessorEngine.onAfterUnzipProcessor: exception ", oEx);
		} finally {
			try {
				if (oPipWriter != null) oPipWriter.close();
				if (oNewPipFile != null) oNewPipFile.close();
			} catch (IOException oEx) {
				WasdiLog.errorLog("PipProcessorEngine.onAfterUnzipProcessor: exception when closing the I/O resources", oEx);
			}
		}
	}
}
