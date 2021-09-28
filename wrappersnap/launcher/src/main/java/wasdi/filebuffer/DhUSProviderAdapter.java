package wasdi.filebuffer;

import java.io.IOException;

import wasdi.ConfigReader;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.LoggerWrapper;
import wasdi.shared.utils.Utils;

/**
 * Donwload File Utility Class for DhUS
 * @author p.campanella
 *
 */
public class DhUSProviderAdapter extends ProviderAdapter {
	
    public DhUSProviderAdapter() {
		super();
	}
    
    public DhUSProviderAdapter(LoggerWrapper logger) {
		super(logger);
	}

    @Override
	public long getDownloadFileSize(String sFileURL)  throws Exception  {
    	// Get File size using http
    	return getDownloadFileSizeViaHttp(sFileURL);
    }

    @Override
    public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws IOException {
    	// Download using HTTP 
    	setProcessWorkspace(oProcessWorkspace);
    	
    	int iAttemps = iMaxRetry;
    	
    	String sResult = "";
    	
    	while (iAttemps>0) {
    		m_oLogger.debug("DhUS Provider: attemp # " + (iMaxRetry-iAttemps+1));
    		sResult = downloadViaHttp(sFileURL, sDownloadUser, sDownloadPassword, sSaveDirOnServer);
    		if (!Utils.isNullOrEmpty(sResult)) break;
    		iAttemps--;
    	}
    	
    	return sResult;
    }

    @Override
    public String getFileName(String sFileURL) throws IOException {
    	// Get File Name via http
    	return getFileNameViaHttp(sFileURL);
    }
    
	@Override
	public void readConfig() {
		
	}
    
}
