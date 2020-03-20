package wasdi.filebuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;

import wasdi.LauncherMain;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.Utils;

public class SOBLOOProviderAdapter extends ProviderAdapter{
	
	private static final String s_sFileNamePrefix = "filename=";
	private static final String s_sSEPARATOR = "\\|\\|\\|";
	private static final int s_iUrl = 0;
	private static final int s_iName = 1;
	private static final int s_iSize = 2;
	private static final int s_iExpectedUrlLen = 3;
	private static final int s_iValue = 1;
	//Sobloo declares they can gather images from the Long Term Archive in no more than 24 hourss
	private static final int s_iNUMATTEMPTS = 24;
	//Sobloo claims most images are available in 1h, so let's wait that plus some slack
	private static final int s_iSLACKTOWAIT = 5;
	


	@Override
	public long GetDownloadFileSize(String sFileURL) throws Exception {
		Preconditions.checkNotNull(sFileURL, "SOBLOOProviderAdapter.GetDownloadSize: input URL is null");
		Preconditions.checkArgument(!sFileURL.isEmpty(), "SOBLOOProviderAdapter.GetDownloadSize: input URL is empty");

		m_oLogger.debug("SOBLOOProviderAdapter.GetDownloadSize: start " + sFileURL);
		long lLenght = 0L;
		boolean bEstimate = false;
		try {
			String sUrl = extractUrl(sFileURL);

			try {
				lLenght = getDownloadFileSizeViaHttp(sUrl);
				if(0>= lLenght) {
					bEstimate = true;
				}
			}catch (Exception oE) {
				m_oLogger.warn("SOBLOOProviderAdapter.GetDownloadSize: cannot read file size via http due to " + oE + ", using rough value");
				bEstimate = true;
			}
			if(bEstimate) {
				try {
					lLenght = extractSize(sFileURL);
					m_oLogger.warn("SOBLOOProviderAdapter.GetDownloadSize: sobloo did not provide 'Content-Length' header, estimating content length from query result as " + lLenght + " B");
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
	public String GetFileName(String sComplexUrl) throws Exception {
		Preconditions.checkNotNull(sComplexUrl);
		Preconditions.checkArgument(!sComplexUrl.isEmpty());
		
		String[] asTokens = tokenize(sComplexUrl);
		String sName = extractValueFromToken(asTokens[s_iName]);
		return sName;
	}
	
	
	private String extractUrl(String sComplexUrl) {
		Preconditions.checkNotNull(sComplexUrl);
		Preconditions.checkArgument(!sComplexUrl.isEmpty());
		
		String[] asTokens = tokenize(sComplexUrl);
		String sUrl = asTokens[s_iUrl];
		
		if(Utils.isNullOrEmpty(sUrl)) {
			throw new IllegalArgumentException("SOBLOOProviderAdapter.extractUrl( " + sComplexUrl + " ): url is null or empty");
		}
		return sUrl;
	}
		
	
	private long extractSize(String sComplexUrl) {
		Preconditions.checkNotNull(sComplexUrl);
		Preconditions.checkArgument(!sComplexUrl.isEmpty());
		
		String[] asTokens = tokenize(sComplexUrl);
		String sLength = extractValueFromToken(asTokens[s_iSize]);
		Long lLenght = Long.parseLong(sLength);
		return lLenght;
	}
	

	private String[] tokenize(String sComplexUrl) {
		Preconditions.checkNotNull(sComplexUrl);
		Preconditions.checkArgument(!sComplexUrl.isEmpty());
		
		String[] asTokens = sComplexUrl.split(s_sSEPARATOR);
		if(asTokens.length != s_iExpectedUrlLen) {
			throw new IllegalArgumentException("SOBLOOProviderAdapter.tokenize( " + sComplexUrl + " ): tokens should be "
					+ s_iExpectedUrlLen + " but " + asTokens.length + " were found");
		}
		return asTokens;
	}
	
	
	private String extractValueFromToken(String sToken) {
		Preconditions.checkNotNull(sToken);
		Preconditions.checkArgument(!sToken.isEmpty());
		String[] asKeyValue = sToken.split("=");
		if(asKeyValue.length != 2) {
			throw new IllegalArgumentException("SOBLOOProviderAdapter.extractValueFromToken( " + sToken + " ): tokens should be "
					+ s_iExpectedUrlLen + " but " + asKeyValue.length + " were found");
		}
		if(Utils.isNullOrEmpty(asKeyValue[s_iValue])) {
			throw new IllegalArgumentException("SOBLOOProviderAdapter.extractValueFromToken( " + sToken + " ): value is null or empty ");
		}
		return asKeyValue[1];
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
		setConnectionHeaders(oHttpConn);


		int responseCode = oHttpConn.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {

			//lLenght = oHttpConn.getHeaderFieldLong("Content-Length", 0L);
			lLenght = oHttpConn.getContentLengthLong();
			m_oLogger.debug("ProviderAdapter.getDownloadFileSizeViaHttp: File size = " + lLenght);

			return lLenght;

		} else {
			handleConnectionError(oHttpConn);
		}
		oHttpConn.disconnect();
		return lLenght;
	}



	@Override
	public String ExecuteDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws Exception {
		
		m_oLogger.debug("SOBLOOProviderAdapter.ExecuteDownloadFile (" + sFileURL + ", " + sDownloadUser + ", " +
				sDownloadPassword + ", " + sSaveDirOnServer + ", <ProcessWorkspace> )");
		
		Preconditions.checkNotNull(sFileURL, "SOBLOOProviderAdapter.ExecuteDownloadFile: URL is null");
		Preconditions.checkArgument(!sFileURL.isEmpty(), "SOBLOOProviderAdapter.ExecuteDownloadFile: URL is empty");
		Preconditions.checkNotNull(sDownloadPassword, "SOBLOOProviderAdapter.ExecuteDownloadFile: password is null");
		Preconditions.checkArgument(!sDownloadPassword.isEmpty(), "SOBLOOProviderAdapter.ExecuteDownloadFile: password is empty");
		Preconditions.checkNotNull(sSaveDirOnServer, "SOBLOOProviderAdapter.ExecuteDownloadFile: save dir is null");
		Preconditions.checkArgument(!sSaveDirOnServer.isEmpty(), "SOBLOOProviderAdapter.ExecuteDownloadFile: save dir is empty");
		Preconditions.checkNotNull(oProcessWorkspace, "SOBLOOProviderAdapter.ExecuteDownloadFile: process workspace is null");

		setProcessWorkspace(oProcessWorkspace);

		String sReturnFilePath = "";
		System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
		String sURL = extractUrl(sFileURL);
		
		Instant oStart = Instant.now();
		Duration oMaxDuration = Duration.ofMinutes((long)(24 * 60 + s_iSLACKTOWAIT));
		
		int iAttempts = SOBLOOProviderAdapter.s_iNUMATTEMPTS;
		while(iAttempts > 0 && Duration.between(oStart, Instant.now()).compareTo(oMaxDuration) <= 0 ) {
			 
			URL oUrl = new URL(sURL);
			HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
			setConnectionHeaders(oHttpConn);
	
			int iResponseCode = oHttpConn.getResponseCode();
	
			// always check HTTP response code first
			if (iResponseCode == HttpURLConnection.HTTP_OK) {
	
				m_oLogger.debug("SOBLOOProviderAdapter.ExecuteDownloadFile: Connected");
	
				String sFileName = "";
				String sDisposition = oHttpConn.getHeaderField("Content-Disposition");
				String sContentType = oHttpConn.getContentType();
				long lContentLength = 0;
				try {
					lContentLength = GetDownloadFileSize(sFileURL);
				} catch (Exception oE) {
					m_oLogger.error("SOBLOOProviderAdapter.ExecuteDownloadFile: " + oE);
				}
	
	
				m_oLogger.debug("SOBLOOProviderAdapter.ExecuteDownloadFile: ContentLenght: " + lContentLength);
	
				if (sDisposition != null) {
					// extracts file name from header field
					int index = sDisposition.indexOf(SOBLOOProviderAdapter.s_sFileNamePrefix);
					if (index > 0) {
						int iLen = SOBLOOProviderAdapter.s_sFileNamePrefix.length();
						sFileName = sDisposition.substring(index + iLen, sDisposition.length() );
						sFileName = sFileName.trim();
						while(sFileName.startsWith("\"")) {
							sFileName = sFileName.substring(1);
						}
						while(sFileName.endsWith("\"")) {
							sFileName = sFileName.substring(0, sFileName.length()-1);
						}
					}
				} else {
					// extracts file name from URL
					sFileName = GetFileName(sFileURL);
				}
	
				m_oLogger.debug("Content-Type: " + sContentType);
				m_oLogger.debug("Content-Disposition: " + sDisposition);
				m_oLogger.debug("Content-Length: " + lContentLength);
				m_oLogger.debug("fileName: " + sFileName);
	
				// opens input stream from the HTTP connection
				InputStream oInputStream = oHttpConn.getInputStream();
	
				if (!sSaveDirOnServer.endsWith("/")) sSaveDirOnServer+="/";
	
				String sSaveFilePath = sSaveDirOnServer + sFileName;
	
				m_oLogger.debug("SOBLOOProviderAdapter.ExecuteDownloadFile: Create Save File Path = " + sSaveFilePath);
	
				File oTargetFile = new File(sSaveFilePath);
				File oTargetDir = oTargetFile.getParentFile();
				oTargetDir.mkdirs();
	
				// opens an output stream to save into file
				FileOutputStream oOutputStream = new FileOutputStream(sSaveFilePath);
	
				//TODO take countermeasures in case of failure, e.g. retry if timeout. Here or in copyStream?
				copyStream(m_oProcessWorkspace, lContentLength, oInputStream, oOutputStream);
				
				try {
					File oFile = new File(sSaveFilePath);
					m_oLogger.debug("SOBLOOProviderAdapter.ExecuteDownloadFile: file size: expected/actual: " + lContentLength + "/" + oFile.length());
				} catch (Exception oE) {
					
				}
	
				sReturnFilePath = sSaveFilePath;
	
				m_oLogger.debug("SOBLOOProviderAdapter.ExecuteDownloadFile File downloaded " + sReturnFilePath);
				break;
			} else {
				String sError = handleConnectionError(oHttpConn);
				if(503 == iResponseCode) {
					try {
						String sInfo = "Waiting for the transfer of the image from Sobloo Long Term Archive, this may take up to 24 hours from the request";
						LauncherMain.s_oSendToRabbit.SendRabbitMessage(true,LauncherOperations.INFO.name(),oProcessWorkspace.getWorkspaceId(), sInfo,oProcessWorkspace.getWorkspaceId());
						m_oLogger.info("SOBLOOProviderAdapter.ExecuteDownloadFile: LTA status: " + sError);
						TimeUnit.MINUTES.sleep(60 + SOBLOOProviderAdapter.s_iSLACKTOWAIT);
					} catch (InterruptedException oE) {
						m_oLogger.error("SOBLOOProviderAdapter.ExecuteDownloadFile: Could not complete sleep: " + oE);
					}
					
				}
			}
			oHttpConn.disconnect();
			iAttempts--;
		}
		return sReturnFilePath;		
	}


	/**
	 * @param oHttpConn
	 * @throws ProtocolException
	 */
	protected void setConnectionHeaders(HttpURLConnection oHttpConn) throws ProtocolException {
		Preconditions.checkNotNull(oHttpConn);
		
		oHttpConn.setRequestMethod("GET");
		oHttpConn.setRequestProperty("Accept", "*/*");
		oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
		String sBasicAuth = "Apikey " + m_sProviderPassword;
		oHttpConn.setRequestProperty("Authorization",sBasicAuth);
	}
}
