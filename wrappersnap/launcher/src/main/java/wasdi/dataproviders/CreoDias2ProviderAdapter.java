package wasdi.dataproviders;

import wasdi.shared.business.ProcessWorkspace;

public class CreoDias2ProviderAdapter extends ProviderAdapter {
	
	public CreoDias2ProviderAdapter() {
		super();
		m_sDataProviderCode = "CREODIAS2";
	}

	@Override
	protected void internalReadConfig() {
		// TODO Auto-generated method srub. 
		// read from WasdiConfig specific configurations
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		// TODO Auto-generated method stub
		// receives the file URI and must return the size of the file. Useful to give progress to the user
		return 0;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		// TODO Auto-generated method stub
		// main method. sFileUrl is the url received by the lined data provider. sSaveDirOnSerer is the local folder. 
		// must return the valid file full path or "" if the download was not possible. 
		return null;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		// TODO Auto-generated method stub
		// extracts the file name from the URL
		return null;
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		// TODO Auto-generated method stub
		// returns the score auto-evaluated by the Provider Adapter to download sFileName of sPlatformType.
		return 0;
	}
	
}
