package wasdi.filebuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

import org.apache.log4j.Logger;

import wasdi.ConfigReader;
import wasdi.shared.business.ProcessWorkspace;
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
    
    public DhUSProviderAdapter(Logger logger) {
		super(logger);
	}

    @Override
	public long GetDownloadFileSize(String sFileURL)  throws Exception  {
    	// Get File size using http
    	return getDownloadFileSizeViaHttp(sFileURL);
    }

    @Override
    public String ExecuteDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws IOException {
    	// Download using HTTP 
    	setProcessWorkspace(oProcessWorkspace);
    	return downloadViaHttp(sFileURL, sDownloadUser, sDownloadPassword, sSaveDirOnServer);
    }

    @Override
    public String GetFileName(String sFileURL) throws IOException {
    	// Get File Name via http
    	return getFileNameViaHttp(sFileURL);
    }
}
