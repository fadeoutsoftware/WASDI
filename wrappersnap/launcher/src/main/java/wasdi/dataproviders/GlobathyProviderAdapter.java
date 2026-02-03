package wasdi.dataproviders;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONObject;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class GlobathyProviderAdapter extends ProviderAdapter {
	
	private String m_sGloBathRootFolderPath = null;

	public GlobathyProviderAdapter() {
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
		
		String sFileName = sFileURL;
		if (sFileName.startsWith("https://")) {
			sFileName = sFileName.replace("https://", "");
		}
		
		String sLakeId = sFileName.replace(".tif", "");
		int iLakeId = Integer.parseInt(sLakeId);
		
		String sSubFolderPath = this.getLakeFolderPath(iLakeId);
		
		String sFilePath = m_sGloBathRootFolderPath + File.separator + sSubFolderPath + sFileName;
		
		
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
		// TODO Auto-generated method stub
		return null;
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
	
	public String getLakeFolderPath(int iId) {
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
