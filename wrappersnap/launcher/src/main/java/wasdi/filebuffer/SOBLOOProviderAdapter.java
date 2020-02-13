package wasdi.filebuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.Utils;

public class SOBLOOProviderAdapter extends ProviderAdapter{
	private static final String SEPARATOR = "\\|\\|\\|";


	public String GetUrl(String sFileURL) {
		Preconditions.checkNotNull(sFileURL, "SOBLOOProviderAdapter.GetUrl: input URL is null");
		Preconditions.checkArgument(!sFileURL.isEmpty(), "SOBLOOProviderAdapter.GetUrl: input URL is empty");

		String[] asTokens = sFileURL.split(SEPARATOR);

		if(asTokens.length < 3){
			throw new IllegalArgumentException("The URL is in the wrong format"); 
		}

		return asTokens[0];
	}


	@Override
	public long GetDownloadFileSize(String sFileURL) throws Exception {
		Preconditions.checkNotNull(sFileURL, "SOBLOOProviderAdapter.GetDownloadSize: input URL is null");
		Preconditions.checkArgument(!sFileURL.isEmpty(), "SOBLOOProviderAdapter.GetDownloadSize: input URL is empty");

		m_oLogger.debug("SOBLOOProviderAdapter.GetDownloadSize: start " + sFileURL);
		long lLenght = 0L;
		boolean bEstimate = false;
		try {
			String[] asTokens = sFileURL.split(SEPARATOR);

			if(asTokens.length < 3) {
				throw new IllegalArgumentException("The URL is in the wrong format"); 
			}

			try {
				lLenght = getDownloadFileSizeViaHttp(asTokens[0]);
				if(0>= lLenght) {
					bEstimate = true;
				}
			}catch (Exception oE) {
				m_oLogger.warn("SOBLOOProviderAdapter.GetDownloadSize: cannot read file size via http due to " + oE + ", using rough value");
				bEstimate = true;
			}
			if(bEstimate) {
				try {
					String[] asLen = asTokens[2].split("=");
					lLenght = Long.parseLong(asLen[1]);
				} catch (Exception oEin) {
					m_oLogger.error("SOBLOOProviderAdapter.GetDownloadFileSize: " + oEin);
				}
			}
		} catch (Exception oE) {
			m_oLogger.error("SOBLOOProviderAdapter.GetDownloadFileSize: " + oE);
		}

		return lLenght;
	}

	@Override
	protected long getDownloadFileSizeViaHttp(String sFileURL)  throws Exception  {
		Preconditions.checkNotNull(sFileURL, "SOBLOOProviderAdapter.getDownloadFileSizeViaHttp: input URL is null");
		Preconditions.checkArgument(!sFileURL.isEmpty(), "SOBLOOProviderAdapter.getDownloadFileSizeViaHttp: input URL is empty");

		long lLenght = 0L;

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

			//lLenght = oHttpConn.getHeaderFieldLong("Content-Length", 0L);
			lLenght = oHttpConn.getContentLengthLong();
			m_oLogger.debug("ProviderAdapter.getDownloadFileSizeViaHttp: File size = " + lLenght);

			return lLenght;

		} else {
			m_oLogger.error("ProviderAdapter.getDownloadFileSizeViaHttp: No file to download. Server replied HTTP code: " + responseCode);
			InputStream oErrorStream = oHttpConn.getErrorStream();
			String sError = CharStreams.toString(new InputStreamReader(oErrorStream, Charsets.UTF_8));
			m_oLogger.error("ProviderAdapter.getDownloadFileSizeViaHttp: message is: " + sError);
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
		if(sFileURL== null || sFileURL.isEmpty() == true)
		{
			throw new IllegalArgumentException("The argument cannot be null or empty"); 
		}

		String[] asTokens = sFileURL.split(SEPARATOR);

		if(asTokens.length < 3)
		{
			throw new IllegalArgumentException("The URL is in the wrong format"); 
		}

		return asTokens[1];
	}

	@Override
	protected String downloadViaHttp(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer) throws IOException {

		String sReturnFilePath = "";

		System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");

		String sURL = GetUrl(sFileURL);

		m_oLogger.debug("SOBLOOProviderAdapter.downloadViaHttp: sDownloadUser = " + sDownloadUser);

		m_oLogger.debug("SOBLOOProviderAdapter.downloadViaHttp: FileUrl = " + sURL);

		//		sFileURL = "https://sobloo.eu/api/v1/services/search?f=acquisition.missionName:eq:Sentinel-1A";//DEBUG 
		URL oUrl = new URL(sURL);
		HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
		oHttpConn.setRequestMethod("GET");
		oHttpConn.setRequestProperty("Accept", "*/*");
		oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");

		//DEBUG
		// Basic HTTP Authentication "by hand"
		String sBasicAuth = "Apikey " + sDownloadPassword;

		oHttpConn.setRequestProperty("Authorization",sBasicAuth);

		int responseCode = oHttpConn.getResponseCode();

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {

			m_oLogger.debug("SOBLOOProviderAdapter.downloadViaHttp: Connected");

			String sFileName = "";
			String sDisposition = oHttpConn.getHeaderField("Content-Disposition");
			String sContentType = oHttpConn.getContentType();
			long lContentLength = 0;
			try {
				lContentLength = GetDownloadFileSize(sFileURL);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


			m_oLogger.debug("SOBLOOProviderAdapter.downloadViaHttp. ContentLenght: " + lContentLength);

			if (sDisposition != null) {
				// extracts file name from header field
				int index = sDisposition.indexOf("filename=");
				if (index > 0) {
					sFileName = sDisposition.substring(index + 9, sDisposition.length() );
				}
			} else {
				// extracts file name from URL
				sFileName = sURL.substring(sURL.lastIndexOf("/") + 1, sURL.length());
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
