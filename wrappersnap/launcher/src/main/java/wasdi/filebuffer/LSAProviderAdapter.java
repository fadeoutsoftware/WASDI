package wasdi.filebuffer;

import java.io.File;
import java.io.IOException;

import wasdi.ConfigReader;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.opensearch.lsa.LSAHttpUtils;
import wasdi.shared.utils.Utils;

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
				Utils.debugLog("LSAProviderAdapter.getDownloadFileSize: unknown protocol " + sFileURL);
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
	 * Extract the file-system path of the file
	 * @param sHttpsURL
	 * @return
	 */
	private String extractFilePathFromHttpsUrl(String sHttpsURL) {
		String filesystemPath = m_sProviderBasePath + sHttpsURL.replace(m_sProviderUrlDomain, "");

		Utils.debugLog("LSAProviderAdapter.extractFilePathFromHttpsUrl: HTTPS URL: " + sHttpsURL);
		Utils.debugLog("LSAProviderAdapter.extractFilePathFromHttpsUrl: file path: " + filesystemPath);

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
		
		Utils.debugLog("LSAProviderAdapter.executeDownloadFile: try to get " + sFileURL);
		
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
				Utils.debugLog("LSAProviderAdapter.executeDownloadFile: unknown protocol " + sFileURL);
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
			m_oLogger.debug("LSAProviderAdapter.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			m_oLogger.debug("LSAProviderAdapter.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}
		
		m_oLogger.debug("LSAProviderAdapter.ExecuteDownloadFile: start");
		
		setProcessWorkspace(oProcessWorkspace);

		if (isFileProtocol(sFileURL)) {
			sResult = localFileCopy(sFileURL, sSaveDirOnServer, iMaxRetry);
		} 
		
		return sResult;
	}

	private String downloadHttps(String sFileURL, String sSaveDirOnServer, int iMaxRetry, String sResult) {
		for (int iAttemp = 0; iAttemp < iMaxRetry; iAttemp ++) {

			Utils.debugLog("LSAProviderAdapter.executeDownloadFile: attemp #" + iAttemp);
			
			try {
				sResult = downloadViaHttp(sFileURL, "", "", sSaveDirOnServer);
			}
			catch (Exception oEx) {
				Utils.debugLog("LSAProviderAdapter.executeDownloadFile: exception in download via http call: " + oEx.toString());
			}
			
			if (!Utils.isNullOrEmpty(sResult)) {
				return sResult;
			}

			try {
				int iMsSleep = (int) ( (Math.random()*15000.0) + 10000.0 );
				Thread.sleep(iMsSleep);
			}
			catch (Exception oEx) {
				Utils.debugLog("LSAProviderAdapter.executeDownloadFile: exception in sleep for retry: " + oEx.toString());
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
	public void readConfig() {
		try {
			m_sDefaultProtocol = ConfigReader.getPropValue("LSA_DEFAULT_PROTOCOL", "https://");
		} catch (IOException e) {
			m_oLogger.error("LSAProviderAdapter: Config reader is null");
		}
		
		try {
			m_sProviderBasePath = ConfigReader.getPropValue("LSA_BASE_PATH", "/mount/lucollgs/data_192/");
		} catch (IOException e) {
			m_oLogger.error("LSAProviderAdapter: Config reader is null");
		}		

		try {
			m_sProviderUrlDomain = ConfigReader.getPropValue("LSA_URL_DOMAIN", "https://collgs.lu/repository/");
		} catch (IOException e) {
			m_oLogger.error("LSAProviderAdapter: Config reader is null");
		}
	}

	
}
