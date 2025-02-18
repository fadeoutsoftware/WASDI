package wasdi.dataproviders;

import java.io.File;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.lsa.LSAHttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class LSAProviderAdapter extends ProviderAdapter {
	
	/**
	 * Flag to know if we already authenticated to the LSA Data Center or no
	 */
    boolean m_bAuthenticated = false;
    
	/**
	 * Base path of the folder mounted with EO Data
	 */
	private String m_sProviderBasePath = "";    
    
	/**
	 * URL domain (i.e. https://collgs.lu/repository/).
	 */
	private String m_sProviderUrlDomain = "";
	
	public LSAProviderAdapter() {
		super();
		m_sDataProviderCode = "LSA";
	}


	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		
		if (!m_bAuthenticated) {
			LSAHttpUtils.authenticate(m_sProviderUser, m_sProviderPassword);
			m_bAuthenticated = true;
		}

		long lSizeInBytes = 0L;

		if (isFileProtocol(m_sDefaultProtocol)) {
			String sPath = null;
			if (isFileProtocol(sFileURL)) {
				sPath = removePrefixFile(sFileURL);
			} else if (isHttpsProtocol(sFileURL)) {
				sPath = extractFilePathFromHttpsUrl(sFileURL);
			} else {
				WasdiLog.debugLog("LSAProviderAdapter.getDownloadFileSize: unknown protocol " + sFileURL);
			}

			if (sPath != null) {
				File oSourceFile = new File(sPath);

				if (oSourceFile != null && oSourceFile.exists()) {
					lSizeInBytes = getSourceFileLength(oSourceFile);
					
					return lSizeInBytes;
				}
			}
		}

		if (isHttpsProtocol(sFileURL)) {
			lSizeInBytes = getDownloadFileSizeViaHttp(sFileURL);
		}

		return lSizeInBytes;
	}

	/**
	 * Extract the file-system path of the file out of an HTTPS URL.
	 * @param sHttpsURL the HTTPS URL (i.e. https://collgs.lu/repository/data_192/Sentinel-1/B/SAR-C/L1/GRD/2021/09/29/S1B_IW_GRDH_1SDV_20210929T153558_20210929T153623_028915_037365_9ADA.zip)
	 * @return the file-system path (i.e. C:/temp/wasdi/mount/lucollgs/data_192/Sentinel-1/B/SAR-C/L1/GRD/2021/09/29/S1B_IW_GRDH_1SDV_20210929T153558_20210929T153623_028915_037365_9ADA.zip)
	 */
	private String extractFilePathFromHttpsUrl(String sHttpsURL) {
		String filesystemPath = m_sProviderBasePath + sHttpsURL.replace(m_sProviderUrlDomain, "");

		WasdiLog.debugLog("LSAProviderAdapter.extractFilePathFromHttpsUrl: HTTPS URL: " + sHttpsURL);
		WasdiLog.debugLog("LSAProviderAdapter.extractFilePathFromHttpsUrl: file path: " + filesystemPath);

		return filesystemPath;
	}

	/**
	 * sFileURL = https://collgs.lu/repository/data_191/Sentinel-2/A/MSI/Level-2A/S2MSI2A/2021/09/24/S2A_MSIL2A_20210924T163021_N0301_R083_T15SYR_20210924T211718.zip
	 * 
	 * m_sProviderBasePath = /mount/lucollgs/
	 * 
	 * simplePath = /data_191/Sentinel-1/A/SAR-C/L1/GRD/2021/08/15
	 */
	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		WasdiLog.debugLog("LSAProviderAdapter.executeDownloadFile: try to get " + sFileURL);
		
		if (!m_bAuthenticated) {
			LSAHttpUtils.authenticate(m_sProviderUser, m_sProviderPassword);
			m_bAuthenticated = true;
		}		
		
		String sResult = "";

		if (isFileProtocol(m_sDefaultProtocol)) {

			String sPathLinux = null;
			if (isFileProtocol(sFileURL)) {
				sPathLinux = removePrefixFile(sFileURL);
			} else if (isHttpsProtocol(sFileURL)) {
				sPathLinux = extractFilePathFromHttpsUrl(sFileURL);
			} else {
				WasdiLog.debugLog("LSAProviderAdapter.executeDownloadFile: unknown protocol " + sFileURL);
			}

			if (sPathLinux != null) {
				File oSourceFile = new File(sPathLinux);

				if (oSourceFile != null && oSourceFile.exists()) {
					sResult = copyFile("file:" + sPathLinux, sDownloadUser, sDownloadPassword, sSaveDirOnServer, oProcessWorkspace, iMaxRetry);

					return sResult;
				}
			}
		}

		if(isHttpsProtocol(sFileURL)) {
			sResult = downloadHttps(sFileURL, sSaveDirOnServer, iMaxRetry, sResult);
		}

		return sResult;
	}
	
	private String copyFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		String sResult = "";
		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.debugLog("LSAProviderAdapter.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			WasdiLog.debugLog("LSAProviderAdapter.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}
		
		WasdiLog.debugLog("LSAProviderAdapter.ExecuteDownloadFile: start");
		
		setProcessWorkspace(oProcessWorkspace);

		if (isFileProtocol(sFileURL)) {
			sResult = localFileCopy(sFileURL, sSaveDirOnServer, iMaxRetry);
		} 
		
		return sResult;
	}

	private String downloadHttps(String sFileURL, String sSaveDirOnServer, int iMaxRetry, String sResult) {
		for (int iAttemp = 0; iAttemp < iMaxRetry; iAttemp ++) {

			WasdiLog.debugLog("LSAProviderAdapter.downloadHttps: attemp #" + iAttemp);
			
			try {
				sResult = downloadViaHttp(sFileURL, "", "", sSaveDirOnServer);
			}
			catch (Exception oEx) {
				WasdiLog.debugLog("LSAProviderAdapter.downloadHttps: exception in download via http call: " + oEx.toString());
			}
			
			if (!Utils.isNullOrEmpty(sResult)) {
				return sResult;
			}

			try {
				int iMsSleep = (int) ( (Math.random()*15000.0) + 10000.0 );
				Thread.sleep(iMsSleep);
			}
			catch (InterruptedException oEx) {
				Thread.currentThread().interrupt();
				WasdiLog.debugLog("LSAProviderAdapter.downloadHttps: exception in sleep for retry: " + oEx.toString());
			}
		}
		
		return sResult;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		if (Utils.isNullOrEmpty(sFileURL)) return "";
		
		String sFileName = "";
		
		String [] asParts = sFileURL.split("/");
		
		if (asParts != null) {
			sFileName = asParts[asParts.length-1];
		}
		
		return sFileName;
	}
	
	@Override
	protected void internalReadConfig() {
		
		try {
			m_sDefaultProtocol = m_oDataProviderConfig.defaultProtocol; 
			m_sProviderBasePath = m_oDataProviderConfig.localFilesBasePath;
			m_sProviderUrlDomain = m_oDataProviderConfig.urlDomain;
			
		} catch (Exception e) {
			WasdiLog.errorLog("CREODIASProvierAdapter: Config reader is null");
		}		
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (sPlatformType.equals(Platforms.SENTINEL1) || sPlatformType.equals(Platforms.SENTINEL2)) {
			if (isWorkspaceOnSameCloud()) {
				return DataProviderScores.SAME_CLOUD_DOWNLOAD.getValue();
			}
			else {
				return DataProviderScores.DOWNLOAD.getValue();
			}
		}
		
		return 0;
		
	}
	
}
