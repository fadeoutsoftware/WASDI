package wasdi.dataproviders;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class GlobathyProviderAdapter extends ProviderAdapter {
	
	private String m_sGloBathRootFolderPath = null;

	public GlobathyProviderAdapter() {
		m_sDataProviderCode = "GLOBATHY";
	}

	@Override
	protected void internalReadConfig() {
		if (m_oDataProviderConfig != null && !Utils.isNullOrEmpty(m_oDataProviderConfig.adapterConfig)) {
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(m_oDataProviderConfig.adapterConfig);
			m_sGloBathRootFolderPath = oAppConf.getString("globathyRootFolderPath");
		}

	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		
		long lFileSize = 0L;
		
		String sFilePath = getFileLocation(sFileURL);
		
		if (Utils.isNullOrEmpty(sFilePath)) {
			WasdiLog.warnLog("GlobathyProviderAdapter.getDownloadFileSize. Impossible to get file size " + sFileURL);
			return lFileSize;
		}
		
		WasdiLog.debugLog("GlobathyProviderAdapter.getDownloadFileSize. Path: " + sFilePath);
		
		Path oPath = Paths.get(sFilePath);
		try {
			lFileSize = Files.size(oPath);
		} catch(IOException oE) {
			WasdiLog.warnLog("GlobathyProviderAdapter.getDownloadFileSize. Cannot read size of file: " + sFilePath);
		}
				
		return lFileSize;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		WasdiLog.debugLog("GlobathyProviderAdapter.executeDownloadFile. Copying bathymetry file to " + sSaveDirOnServer);
		
		String sDownloadedFilePath = null;
		
		try {
			String sFilePath = getFileLocation(sFileURL);
			
			if (Utils.isNullOrEmpty(sFilePath)) {
				WasdiLog.warnLog("GlobathyProviderAdapter.executeDownloadFile. Impossible to get file size " + sFileURL);
				return sDownloadedFilePath;
			}
			
			String sFileName = this.getFileName(sFileURL);
			String sDestinationFilePath = sSaveDirOnServer;
			if (!sDestinationFilePath.endsWith(File.separator))
				sDestinationFilePath += File.separator;
			sDestinationFilePath += sFileName;
			
			File oSourceProduct = new File(sFilePath);
			File oDestinationProduct = new File(sDestinationFilePath);
			
			if (!oSourceProduct.exists()) {
				WasdiLog.warnLog("GlobathyProviderAdapter.executeDownloadFile. Source file do not exist");
				return sDownloadedFilePath;
			}
			
			FileUtils.copyFile(oSourceProduct, oDestinationProduct);
			
			sDownloadedFilePath = sDestinationFilePath;
			
			WasdiLog.debugLog("GlobathyProviderAdapter.executeDownloadFile. File saved at path " + sDestinationFilePath);
		
		} catch (Exception oE) {
			WasdiLog.errorLog("GlobathyProviderAdapter.executeDownloadFile. Error copyting bathymetry file", oE);
		}

		return sDownloadedFilePath;
	}

	@Override
	public String getFileName(String sFileURL, String sDownloadPath) throws Exception {
		if (sFileURL.startsWith("https://")) {
			return sFileURL.replace("https://", "");
		}
		return sFileURL;
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (sPlatformType.equals(Platforms.GLOBATHY)) {
			return DataProviderScores.FILE_ACCESS.getValue();
		}
		
		return 0;
		
	}
	
	private String getFileLocation(String sFileUrl) {
		
		String sFilePath = null;
		
		try {
		
			String sLakeId = getFileName(sFileUrl, null).replace("_bathymetry.tif", "");
			int iLakeId = Integer.parseInt(sLakeId);
			
			String sSubFolderPath = this.getLakeFolderPath(iLakeId);
			
			sFilePath = m_sGloBathRootFolderPath + File.separator + sSubFolderPath + File.separator + sLakeId + "_bathymetry.tif";
		
		} catch(Exception oE) {
			WasdiLog.errorLog("GlobathyProviderAdapter.getFileLocation. Exception", oE);
		}
		
		return sFilePath;
	}	
		
		
	private String getLakeFolderPath(int iId) {
	    int iMacroLower = (iId / 100000) * 100;
	    int iMacroUpper = iMacroLower + 100;
	    
	    String sMacroFolder;
	    if (iId <= 100000) {
	    	sMacroFolder = "1_100K";
	    } else if (iMacroLower >= 1400) {
	    	sMacroFolder = "1400K_1427688";
	    } else {
	    	sMacroFolder = iMacroLower + "K_" + iMacroUpper + "K";
	    }

	    int iMicroLower = ((iId - 1) / 1000) * 1000 + 1;
	    int iMicroUpper = iMicroLower + 999;
	    
	    String microFolder = iMicroLower + "_" + iMicroUpper;

	    return sMacroFolder + File.separator + microFolder;
	}

}
