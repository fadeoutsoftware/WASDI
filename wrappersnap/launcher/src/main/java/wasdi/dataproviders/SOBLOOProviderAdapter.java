package wasdi.dataproviders;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;

import wasdi.LauncherMain;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class SOBLOOProviderAdapter extends ProviderAdapter{
	
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
	

	public SOBLOOProviderAdapter() {
		super();
		m_sDataProviderCode = "SOBLOO";
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		Preconditions.checkNotNull(sFileURL, "SOBLOOProviderAdapter.GetDownloadSize: input URL is null");
		Preconditions.checkArgument(!sFileURL.isEmpty(), "SOBLOOProviderAdapter.GetDownloadSize: input URL is empty");

		WasdiLog.debugLog("SOBLOOProviderAdapter.GetDownloadSize: start " + sFileURL);
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
				WasdiLog.warnLog("SOBLOOProviderAdapter.GetDownloadSize: cannot read file size via http due to " + oE + ", using rough value");
				bEstimate = true;
			}
			if(bEstimate) {
				try {
					lLenght = extractSize(sFileURL);
					WasdiLog.warnLog("SOBLOOProviderAdapter.GetDownloadSize: sobloo did not provide 'Content-Length' header, estimating content length from query result as " + lLenght + " B");
				} catch (Exception oEin) {
					WasdiLog.errorLog("SOBLOOProviderAdapter.GetDownloadFileSize: " + oEin);
				}
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("SOBLOOProviderAdapter.GetDownloadFileSize: " + oE);
		}

		return lLenght;
	}
	
	
	@Override
	public String getFileName(String sComplexUrl) throws Exception {
		Preconditions.checkNotNull(sComplexUrl);
		Preconditions.checkArgument(!sComplexUrl.isEmpty());
		
		String[] asTokens = tokenize(sComplexUrl);
		String sName = extractValueFromToken(asTokens[s_iName]);
		
		// P.Campanella 2020-04-02: the launcher download method needs the file name with extension
		// This is maybe a trick, is there a more secure way to have the complete file name?
		if (!sName.contains(".")) {
			sName += ".zip";
		}
		
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

		WasdiLog.debugLog("SOBLOOProviderAdapter.fileSizeViaHttp: sDownloadUser = " + m_sProviderUser);
		WasdiLog.debugLog("SOBLOOProviderAdapter.fileSizeViaHttp: FileUrl = " + sFileURL);

		URL oUrl = new URL(sFileURL);
		HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
		setConnectionHeaders(oHttpConn);


		int responseCode = oHttpConn.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {

			//lLenght = oHttpConn.getHeaderFieldLong("Content-Length", 0L);
			lLenght = oHttpConn.getContentLengthLong();
			WasdiLog.debugLog("ProviderAdapter.getDownloadFileSizeViaHttp: File size = " + lLenght);

			return lLenght;

		} else {
			handleConnectionError(oHttpConn);
		}
		oHttpConn.disconnect();
		return lLenght;
	}



	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		WasdiLog.debugLog("SOBLOOProviderAdapter.ExecuteDownloadFile (" + sFileURL + ", " + sDownloadUser + ", " + sDownloadPassword + ", " + sSaveDirOnServer + ", <ProcessWorkspace> )");
		
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
		
		int iDownloadAttemp = 0;
		
		while(iAttempts > 0 && Duration.between(oStart, Instant.now()).compareTo(oMaxDuration) <= 0 && iDownloadAttemp < iMaxRetry ) {
			 
			URL oUrl = new URL(sURL);
			HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
			setConnectionHeaders(oHttpConn);
	
			int iResponseCode = oHttpConn.getResponseCode();
	
			// always check HTTP response code first
			if (iResponseCode == HttpURLConnection.HTTP_OK) {
				
				if (m_oProcessWorkspace.getStatus().equals(ProcessStatus.WAITING.name())) {
					
					WasdiLog.debugLog("SOBLOOProviderAdapter.ExecuteDownloadFile: set process in READY State");
					
					LauncherMain.updateProcessStatus(new ProcessWorkspaceRepository(), m_oProcessWorkspace, ProcessStatus.READY, m_oProcessWorkspace.getProgressPerc());
					
					String sResumedStatus = LauncherMain.waitForProcessResume(m_oProcessWorkspace);
					m_oProcessWorkspace.setStatus(sResumedStatus);
					
					if (sResumedStatus.equals(ProcessStatus.ERROR.name()) || sResumedStatus.equals(ProcessStatus.STOPPED.name()) ) {
						WasdiLog.errorLog("SOBLOOProviderAdapter.ExecuteDownloadFile: Process resumed with status ERROR or STOPPED: exit");
						break;
					}
					
					
					WasdiLog.debugLog("SOBLOOProviderAdapter.ExecuteDownloadFile: Process Resumed, let's go!");
				}
	
				WasdiLog.debugLog("SOBLOOProviderAdapter.ExecuteDownloadFile: Connected");
	
				String sFileName = "";
				String sDisposition = oHttpConn.getHeaderField("Content-Disposition");
				String sContentType = oHttpConn.getContentType();
				long lContentLength = 0;
				try {
					lContentLength = getDownloadFileSize(sFileURL);
				} catch (Exception oE) {
					WasdiLog.errorLog("SOBLOOProviderAdapter.ExecuteDownloadFile: " + oE);
				}
	
	
				WasdiLog.debugLog("SOBLOOProviderAdapter.ExecuteDownloadFile: ContentLenght: " + lContentLength);
				
				sFileName = getFileName(sFileURL);
	
				WasdiLog.debugLog("Content-Type: " + sContentType);
				WasdiLog.debugLog("Content-Disposition: " + sDisposition);
				WasdiLog.debugLog("Content-Length: " + lContentLength);
				WasdiLog.debugLog("fileName: " + sFileName);
	
				// opens input stream from the HTTP connection
				InputStream oInputStream = oHttpConn.getInputStream();
	
				if (!sSaveDirOnServer.endsWith("/")) sSaveDirOnServer+="/";
	
				String sSaveFilePath = sSaveDirOnServer + sFileName;
	
				WasdiLog.debugLog("SOBLOOProviderAdapter.ExecuteDownloadFile: Create Save File Path = " + sSaveFilePath);
	
				File oTargetFile = new File(sSaveFilePath);
				File oTargetDir = oTargetFile.getParentFile();
				oTargetDir.mkdirs();
	
				// opens an output stream to save into file
				FileOutputStream oOutputStream = new FileOutputStream(sSaveFilePath);

				iDownloadAttemp ++;
				
				WasdiLog.debugLog("SOBLOOProviderAdapter.ExecuteDownloadFile: Download Attemp # " + iDownloadAttemp);
				
				boolean bCopyStreamResult = copyStream(m_oProcessWorkspace, lContentLength, oInputStream, oOutputStream);
				
				if (!bCopyStreamResult) {
					WasdiLog.debugLog("SOBLOOProviderAdapter.ExecuteDownloadFile: error in the copy stream, try again if we have more attemps");
					continue;
				}
				
				try {
					File oFile = new File(sSaveFilePath);
					WasdiLog.debugLog("SOBLOOProviderAdapter.ExecuteDownloadFile: file size: expected/actual: " + lContentLength + "/" + oFile.length());
				} catch (Exception oE) {
					WasdiLog.debugLog("SOBLOOProviderAdapter.ExecuteDownloadFile: exception reading downloaded file " + oE.toString());
					continue;
				}
	
				sReturnFilePath = sSaveFilePath;
	
				WasdiLog.debugLog("SOBLOOProviderAdapter.ExecuteDownloadFile File downloaded " + sReturnFilePath);
				break;
			} 
			else {
				
				String sError = handleConnectionError(oHttpConn);
				if(503 == iResponseCode) {
					try {
						
						WasdiLog.debugLog("SOBLOOProviderAdapter.ExecuteDownloadFile: Product in LTA, put this process in WAITING");
						// Set this task in waiting state
						LauncherMain.updateProcessStatus(new ProcessWorkspaceRepository(), m_oProcessWorkspace, ProcessStatus.WAITING, m_oProcessWorkspace.getProgressPerc());
						
						String sInfo = "Waiting for the transfer of the image from Sobloo Long Term Archive, this may take up to 24 hours from the request";
						LauncherMain.s_oSendToRabbit.SendRabbitMessage(true,LauncherOperations.INFO.name(),m_oProcessWorkspace.getWorkspaceId(), sInfo,m_oProcessWorkspace.getWorkspaceId());
						WasdiLog.infoLog("SOBLOOProviderAdapter.ExecuteDownloadFile: LTA status: " + sError);
						
						int iMinutesToSleep = 60 + SOBLOOProviderAdapter.s_iSLACKTOWAIT;
						
						WasdiLog.infoLog("SOBLOOProviderAdapter.ExecuteDownloadFile: Going to sleep for: " + iMinutesToSleep);
						TimeUnit.MINUTES.sleep(iMinutesToSleep);
					} catch (InterruptedException oE) {
						WasdiLog.errorLog("SOBLOOProviderAdapter.ExecuteDownloadFile: Could not complete sleep: " + oE);
						Thread.currentThread().interrupt();
					}
					
				}
			}
			oHttpConn.disconnect();
			iAttempts--;
		}
		
		if (m_oProcessWorkspace.getStatus().equals(ProcessStatus.WAITING.name())) {
			WasdiLog.infoLog("SOBLOOProviderAdapter.ExecuteDownloadFile: process is still in waiting, attemps finished, set ERROR state ");
			LauncherMain.updateProcessStatus(new ProcessWorkspaceRepository(), m_oProcessWorkspace, ProcessStatus.ERROR, m_oProcessWorkspace.getProgressPerc());
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
	
	@Override
	protected void internalReadConfig() {
		
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		
		if (sPlatformType.equals(Platforms.SENTINEL1) || sPlatformType.equals(Platforms.SENTINEL2) 
				|| sPlatformType.equals(Platforms.SENTINEL3)) {
			Date oImageDate = MissionUtils.getDateFromSatelliteImageFileName(sFileName);
			
			if (oImageDate!=null) {
				Date oNow = new Date();
				
				long lDistance = oNow.getTime() - oImageDate.getTime();
				
				if (lDistance> 2*30*24*60*60*1000) {
					return DataProviderScores.LTA.getValue();
				}
				
				if (isWorkspaceOnSameCloud()) {
					return DataProviderScores.SAME_CLOUD_DOWNLOAD.getValue();
				}
				else {
					return DataProviderScores.DOWNLOAD.getValue();
				}				
			}
			else {
				return DataProviderScores.LTA.getValue();
			}

		}
		
		return 0;
	}

}
