package wasdi.filebuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.Utils;

public class SOBLOOProviderAdapter extends ProviderAdapter{

	@Override
	public long GetDownloadFileSize(String sFileURL) throws Exception {
		
		m_oLogger.debug("SOBLOOProviderAdapter.GetDownloadSize: start " + sFileURL);
		
		long lLenght = 0L;
//		
//		if(sFileURL.startsWith("file:")) {
//
//			String sPrefix = "file:";
//			// Remove the prefix
//			int iStart = sFileURL.indexOf(sPrefix) +sPrefix.length();
//			String sPath = sFileURL.substring(iStart);
//
//			m_oLogger.debug("SOBLOOProviderAdapter.GetDownloadSize: full path " + sPath);
//			File oSourceFile = new File(sPath);
//			lLenght = oSourceFile.length();
//			if (!oSourceFile.exists()) {
//				m_oLogger.debug("SOBLOOProviderAdapter.GetDownloadSize: FILE DOES NOT EXISTS");
//			}
//			m_oLogger.debug("SOBLOOProviderAdapter.GetDownloadSize: Found length " + lLenght);
//		} else if(sFileURL.startsWith("https:")) {
//			//lLenght = getSizeViaHttp(sFileURL);
			lLenght = getDownloadFileSizeViaHttp(sFileURL);
//		}
		
		return lLenght;
	}
	
	@Override
	protected long getDownloadFileSizeViaHttp(String sFileURL)  throws Exception  {
		long lLenght = 0L;
	    // Domain check
        if (Utils.isNullOrEmpty(sFileURL)) {
            m_oLogger.debug("SOBLOOProviderAdapter.getDownloadFileSizeViaHttp: sFileURL is null");
            return lLenght;
        }
		
		System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

		
		m_oLogger.debug("SOBLOOProviderAdapter.fileSizeViaHttp: sDownloadUser = " + m_sProviderUser);
		
		m_oLogger.debug("SOBLOOProviderAdapter.fileSizeViaHttp: FileUrl = " + sFileURL);
		
		URL oUrl = new URL(sFileURL);
		HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
		oHttpConn.setRequestMethod("GET");
		oHttpConn.setRequestProperty("Accept", "*/*");
		oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
		
		//DEBUG
		// Basic HTTP Authentication "by hand"
		String sBasicAuth = "Apikey " + m_sProviderPassword;

		oHttpConn.setRequestProperty("Authorization",sBasicAuth);

		int responseCode = oHttpConn.getResponseCode();
		
        if (responseCode == HttpURLConnection.HTTP_OK) {

            lLenght = oHttpConn.getHeaderFieldLong("Content-Length", 0L);

            m_oLogger.debug("ProviderAdapter.getDownloadFileSizeViaHttp: File size = " + lLenght);

            return lLenght;

        } else {
            m_oLogger.debug("ProviderAdapter.getDownloadFileSizeViaHttp: No file to download. Server replied HTTP code: " + responseCode);
            m_iLastError = responseCode;
        }
        oHttpConn.disconnect();
        return lLenght;
	}
	
	@Override
	public String ExecuteDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws Exception {
		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.debug("SOBLOOProviderAdapter.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			m_oLogger.debug("SOBLOOProviderAdapter.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}
		
		m_oLogger.debug("SOBLOOProviderAdapter.ExecuteDownloadFile: start");
		
		setProcessWorkspace(oProcessWorkspace);
		
//		String sReturnFilePath = CopyLocalFile(sFileURL, sDownloadUser, sDownloadPassword, sSaveDirOnServer, oProcessWorkspace);
//
//		if (!Utils.isNullOrEmpty(sReturnFilePath)) {
//			m_oLogger.debug("PROBAVProviderAdapter.ExecuteDownloadFile: File found in local repo. Return");
//
//			return sReturnFilePath;
//		} else {
			m_oLogger.debug( "SOBLOOProviderAdapter.ExecuteDownloadFile: File NOT found in local repo, try to donwload from provider");
			
			return downloadViaHttp(sFileURL, sDownloadUser, sDownloadPassword, sSaveDirOnServer);
//		}
//		return null;
		
	}

	@Override
	public String GetFileName(String sFileURL) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected String downloadViaHttp(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer) throws IOException {
		
		String sReturnFilePath = "";
		
		System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

		
		m_oLogger.debug("SOBLOOProviderAdapter.downloadViaHttp: sDownloadUser = " + sDownloadUser);
		
		m_oLogger.debug("SOBLOOProviderAdapter.downloadViaHttp: FileUrl = " + sFileURL);
		
//		sFileURL = "https://sobloo.eu/api/v1/services/search?f=acquisition.missionName:eq:Sentinel-1A";//DEBUG 
		URL oUrl = new URL(sFileURL);
		HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
		oHttpConn.setRequestMethod("GET");
		oHttpConn.setRequestProperty("Accept", "*/*");
		oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
		
		//DEBUG
		// Basic HTTP Authentication "by hand"
		String sBasicAuth = "Apikey " + sDownloadPassword;
//		String sEncoded = Base64.getEncoder().encodeToString(sBasicAuth.getBytes());
//		
//		oHttpConn.setRequestProperty("Authorization",sEncoded);
		oHttpConn.setRequestProperty("Authorization",sBasicAuth);

		int responseCode = oHttpConn.getResponseCode();

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {

			m_oLogger.debug("SOBLOOProviderAdapter.downloadViaHttp: Connected");

			String sFileName = "";
			String sDisposition = oHttpConn.getHeaderField("Content-Disposition");
			String sContentType = oHttpConn.getContentType();
			long lContentLength = oHttpConn.getContentLengthLong();

			m_oLogger.debug("SOBLOOProviderAdapter.downloadViaHttp. ContentLenght: " + lContentLength);

			if (sDisposition != null) {
				// extracts file name from header field
				int index = sDisposition.indexOf("filename=");
				if (index > 0) {
					sFileName = sDisposition.substring(index + 9, sDisposition.length() );
				}
			} else {
				// extracts file name from URL
				sFileName = sFileURL.substring(sFileURL.lastIndexOf("/") + 1, sFileURL.length());
			}

			m_oLogger.debug("Content-Type = " + sContentType);
			m_oLogger.debug("Content-Disposition = " + sDisposition);
			m_oLogger.debug("Content-Length = " + lContentLength);
			m_oLogger.debug("fileName = " + sFileName);

			// opens input stream from the HTTP connection
			InputStream oInputStream = oHttpConn.getInputStream();
			
			if (!sSaveDirOnServer.endsWith("/")) sSaveDirOnServer+="/";
			
			String sSaveFilePath = sSaveDirOnServer + sFileName;

			m_oLogger.debug("SOBLOOProviderAdapter.downloadViaHttp: Create Save File Path = " + sSaveFilePath);

			File oTargetFile = new File(sSaveFilePath);
			File oTargetDir = oTargetFile.getParentFile();
			oTargetDir.mkdirs();

			// opens an output stream to save into file
			FileOutputStream oOutputStream = new FileOutputStream(sSaveFilePath);

			//TODO take countermeasures in case of failure, e.g. retry if timeout. Here or in copyStream?
			copyStream(m_oProcessWorkspace, lContentLength, oInputStream, oOutputStream);

			sReturnFilePath = sSaveFilePath;

			m_oLogger.debug("SOBLOOProviderAdapter.downloadViaHttp File downloaded " + sReturnFilePath);
		} else {
			m_oLogger.debug("SOBLOOProviderAdapter.downloadViaHttp No file to download. Server replied HTTP code: " + responseCode);
			m_iLastError = responseCode;
		}
		oHttpConn.disconnect();
		return sReturnFilePath;		
	}
}
