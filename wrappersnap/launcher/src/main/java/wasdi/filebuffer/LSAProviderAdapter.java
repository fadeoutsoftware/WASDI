package wasdi.filebuffer;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.opensearch.lsa.LSAHttpUtils;
import wasdi.shared.utils.Utils;

public class LSAProviderAdapter extends ProviderAdapter {
	
	/**
	 * Flag to know if we already authenticated to the LSA Data Center or no
	 */
    boolean m_bAuthenticated = false;


	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		
		if (!m_bAuthenticated) {
			LSAHttpUtils.authenticate(m_sProviderUser, m_sProviderPassword);
			m_bAuthenticated = true;
		}

		return getDownloadFileSizeViaHttp(sFileURL);
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		Utils.debugLog("LSAProviderAdapter.executeDownloadFile: try to get " + sFileURL);
		
		if (!m_bAuthenticated) {
			LSAHttpUtils.authenticate(m_sProviderUser, m_sProviderPassword);
			m_bAuthenticated = true;
		}		
		
		String sResult = "";
		
		int iAttemp = 0;
		
		while (Utils.isNullOrEmpty(sResult) && iAttemp<iMaxRetry) {
			
			Utils.debugLog("LSAProviderAdapter.executeDownloadFile: attemp #" + iAttemp);
			
			try {
				sResult = downloadViaHttp(sFileURL, "", "", sSaveDirOnServer);
			}
			catch (Exception oEx) {
				Utils.debugLog("LSAProviderAdapter.executeDownloadFile: exception in download via http call: " + oEx.toString());
			}
			
			try {
				int iMsSleep = (int) ( (Math.random()*15000.0) + 10000.0 );
				Thread.sleep(iMsSleep);
			}
			catch (Exception oEx) {
				Utils.debugLog("LSAProviderAdapter.executeDownloadFile: exception in sleep for retry: " + oEx.toString());
			}
			
			iAttemp ++;
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
	
}
