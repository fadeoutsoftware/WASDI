package wasdi.dataproviders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.esa.snap.core.datamodel.Product;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;

import wasdi.ProcessWorkspaceUpdateNotifier;
import wasdi.ProcessWorkspaceUpdateSubscriber;
import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductReaderFactory;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Base Download Utility Class 
 * Created by s.adamo on 06/10/2016.
 */
public abstract class ProviderAdapter implements ProcessWorkspaceUpdateNotifier {
	
	/**
	 * Unique Code of this data provider
	 */
	protected String m_sDataProviderCode = "";
	
	/**
	 * Size of the buffer for the copy stream operations
	 */
	protected final int BUFFER_SIZE = 4096;
	
	/**
	 * Max number of zeros allowed to read during the read
	 */
    protected final int MAX_NUM_ZEORES_DURING_READ = 20;
    
    /**
     * Last error as returned by http operations
     */
    protected int m_iLastError = 0;
	
    /**
     * Provider User 
     */
    protected String m_sProviderUser;
    
    /**
     * Provider Password
     */
    protected String m_sProviderPassword;
    
    /**
     * Default Protocol
     */
    protected String m_sDefaultProtocol = "https://";
    
    /**
     * List of supported classes
     */
    protected ArrayList<String> m_asSupportedPlatforms = new ArrayList<String>();
    
    /**
     * Process workspace representing the actual operation
     */
    ProcessWorkspace m_oProcessWorkspace;
    
    /**
     * Cloud Provider of the Data Provider
     */
    protected String m_sCloudProvider = "";
    
    /**
     * List of subscriber that receives the progress update of the download operation
     */
    private List<ProcessWorkspaceUpdateSubscriber> m_aoSubscribers;
    
    /**
     * Data Provider Config
     */
    protected DataProviderConfig m_oDataProviderConfig;
    
    /**
     * Multiply the std WASDI read timeout for the query of this specific Data Provider. May be useful for slow providers
     */
    protected int m_iHttpDownloadReadTimeoutMultiplier = 1;

    /**
     * Constructor: uses LauncerMain logger
     */
    public ProviderAdapter() {
		m_aoSubscribers = new ArrayList<ProcessWorkspaceUpdateSubscriber>();
	}
        
    /**
     * Read base configuration
     */
    public void readConfig() {
    	try {
    		// Get the config
    		m_oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(m_sDataProviderCode);
    		// Read the cloud provider
    		m_sCloudProvider = m_oDataProviderConfig.cloudProvider;
    		
    		// Add supported platform
    		for (String sSupportedPlatform : m_oDataProviderConfig.supportedPlatforms) {
    			m_asSupportedPlatforms.add(sSupportedPlatform);
			}
    		
    		// Call internal function for further specialized config
    		internalReadConfig();
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("Exception reading Data Provider Config: " + oEx.toString());
		}
    }
    
    protected abstract void internalReadConfig(); 

    /**
     * Abstract method to get the size of the downloaded file
     * @param sFileURL URL of the file
     * @return
     */
	public abstract long getDownloadFileSize(String sFileURL) throws Exception;
	
	/**
	 * Abstract method to download the file
	 * @param sFileURL URL of the file
	 * @param sDownloadUser User to authenticate to the provider
	 * @param sDownloadPassword Password to authenticate to the provider
	 * @param sSaveDirOnServer Local save path
	 * @param oProcessWorkspace Process Workspace to update the user
	 * @return Downloaded File Full Path, or "" in case of problems
	 */
    public abstract String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception;
    
    /**
     * Abstract method to get the name of the file from the url
     * @param sFileURL URL of the file
     * @return
     */
    public String getFileName(String sFileURL) throws Exception {
    	return getFileName(sFileURL, "");
    }
    
    public abstract String getFileName(String sFileURL,String sDownloadPath) throws Exception;
    
    /**
     * Get the Data Provider Code
     */
    public String getCode() {
    	return m_sDataProviderCode;
    }
    
    /**
     * Set the Data Provider Code
     * @param sCode
     */
    public void setCode(String sCode) {
    	m_sDataProviderCode = sCode;
    }
    
    /**
     * Get the score of this Data Provider for the specified file. 
     * The score is -1 if the file is not supported.
     * High score means the fast availalbilty of the file
     * Low score means slow availability of the file 
     * 
     * @param sFileName File to investigate
     * @return Provider score. -1 if not supported
     */
    public int getScoreForFile(String sFileName) {
    	return getScoreForFile(sFileName, null);
    }
    
    /**
     * Get the score of this Data Provider for the specified file. 
     * The score is -1 if the file is not supported.
     * High score means the fast availalbilty of the file
     * Low score means slow availability of the file 
     * 
     * @param sFileName File to investigate
     * @param sPlatform PlatformType
     * @return Provider score. -1 if not supported
     */
    public int getScoreForFile(String sFileName, String sPlatform) {
    	
    	try {
    		
    		// Try to assume we have the platform
    		String sPlatformType = sPlatform;
    		
    		// If not, try to autodetect it
    		if (Utils.isNullOrEmpty(sPlatformType)) {
    			sPlatformType = MissionUtils.getPlatformFromSatelliteImageFileName(sFileName);
    		}
    		
    		// If it is still null, we cannot proceed
    		if (Utils.isNullOrEmpty(sPlatformType)) {
    			WasdiLog.debugLog("ProviderAdapter.getScoreForFile: platform not recognized. DataProvider: " + m_sDataProviderCode + " File: " + sFileName);
    			return -1;
    		}
    		
    		if (!m_asSupportedPlatforms.contains(sPlatformType)) {
    			return -1;
    		}
    		
    		return internalGetScoreForFile(sFileName, sPlatformType);
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ProviderAdapter.getScoreForFile: exception " + oEx.toString());
		}
    	
    	return -1;
    }    
    
    /**
     * Internal abstract method to determine the score of this Data Provider for a input file
     * @param sFileName sFileName File to investigate
     * @param sPlatformType Platform type of the file
     * @return Provider score. -1 if not supported
     */
    protected abstract int internalGetScoreForFile(String sFileName, String sPlatformType);
    
    /**
     * Get the code of the cloud provider of the actual workspace
     * @return Code of the cloud provider of the actual workspace
     */
    protected String getWorkspaceCloud() {
    	if (m_oProcessWorkspace == null) return "";
    	
    	try {
        	String sWorkspaceId = m_oProcessWorkspace.getWorkspaceId();
        	
        	WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
        	Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
        	
        	NodeRepository oNodeRepository = new NodeRepository();
        	Node oNode = oNodeRepository.getNodeByCode(oWorkspace.getNodeCode());
        	
        	if (oNode != null) {
        		return oNode.getCloudProvider();
        	}
        	else {
        		return WasdiConfig.Current.mainNodeCloud;
        	}
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("ProviderAdapter.getScoreForFile: exception " + oEx.toString());
		}    	
    	
    	return "";
    }
    
    /**
     * Return true if the workspace is on the same cloud of the data provider
     * @return true if the workspace is on the same cloud of the data provider
     */
    protected boolean isWorkspaceOnSameCloud() {
		String sCloud = getWorkspaceCloud();
		
		boolean bOnCloud = false;
		if (!Utils.isNullOrEmpty(sCloud)) {
			if (m_sCloudProvider.toUpperCase().equals(sCloud.toUpperCase())) {
				bOnCloud = true;
			}
		}
		
		return bOnCloud;
    }
    

    @Override
	public void subscribe(ProcessWorkspaceUpdateSubscriber oSubscriber) {
    	try {
	    	if(null != oSubscriber) {
	    		m_aoSubscribers.add(oSubscriber);
	    	} else {
	    		WasdiLog.warnLog("ProviderAdapter.subscribe: null subscriber");
	    	}
    	} catch (Exception oE) {
    		WasdiLog.errorLog("ProviderAdapter.subscribe: " + oE); 
		}
	}

	@Override
	public void unsubscribe(ProcessWorkspaceUpdateSubscriber oSubscriber) {
		try {
			if(!m_aoSubscribers.remove(oSubscriber)) {
				WasdiLog.warnLog("ProviderAdapter.subscribe: subscriber not found");
			}
		} catch (Exception oE) {
    		WasdiLog.errorLog("ProviderAdapter.unsubscribe: " + oE); 
		}
	}
	
	/**
	 * Set the process workspace member
	 * @param oProcessWorkspace
	 */
	public void setProcessWorkspace(ProcessWorkspace oProcessWorkspace) {
		if(null!=oProcessWorkspace) {
			m_oProcessWorkspace = oProcessWorkspace;
		} else {
			WasdiLog.errorLog("ProviderAdapter.setProcessWorkspace: oProcessWorkspace is null");
			throw new NullPointerException();
		}
	}
    
    /**
     * Get the last server error
     * @return
     */
    public int getLastServerError() {
    	return m_iLastError;
    }

    /**
     * Get the provider User
     * @return
     */
    public String getProviderUser() {
		return m_sProviderUser;
	}

    /**
     * Set the provider user
     * @param m_sProviderUser
     */
	public void setProviderUser(String m_sProviderUser) {
		this.m_sProviderUser = m_sProviderUser;
	}

	/**
	 * Get the provider password
	 * @return
	 */
	public String getProviderPassword() {
		return m_sProviderPassword;
	}

	/**
	 * Set the provider password
	 * @param m_sProviderPassword
	 */
	public void setProviderPassword(String m_sProviderPassword) {
		this.m_sProviderPassword = m_sProviderPassword;
	}
	
	/**
	 * Get File Name via http
	 * @param sFileURL
	 * @return
	 * @throws IOException
	 */
	protected String getFileNameViaHttp(String sFileURL) throws IOException {

		try {
			// Domain check
			if (Utils.isNullOrEmpty(sFileURL)) {
				WasdiLog.errorLog("ProviderAdapter.getFileNameViaHttp: sFileURL is null or Empty");
				return null;
			}

			String sReturnFilePath = "";
			
	        String sUser = "";
	        String sPassword = "";

			if (!Utils.isNullOrEmpty(m_sProviderUser)) sUser = m_sProviderUser;
			if (!Utils.isNullOrEmpty(m_sProviderPassword)) sPassword = m_sProviderPassword;

			final String sFinalUser = sUser;
			final String sFinalPassword = sPassword;

			// dhus authentication
			Authenticator.setDefault(new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					try {
						return new PasswordAuthentication(sFinalUser, sFinalPassword.toCharArray());
					} catch (Exception oEx) {
						WasdiLog.errorLog("ProviderAdapter.getFileNameViaHttp: exception setting auth " + ExceptionUtils.getStackTrace(oEx));
					}
					return null;
				}
			});

			WasdiLog.debugLog("ProviderAdapter.getFileNameViaHttp: FileUrl = " + sFileURL);
			
			int iConnectionTimeOut = WasdiConfig.Current.connectionTimeout;
			int iReadTimeOut = WasdiConfig.Current.readTimeout;
			
			URL oUrl = new URL(sFileURL);
			HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
			WasdiLog.debugLog("ProviderAdapter.getFileNameViaHttp: Connection Created");
			
			//NOTE: the DhUS version did not set GET and Accept. ONDA did. TEST 
			oHttpConn.setRequestMethod("GET");
			oHttpConn.setRequestProperty("Accept", "*/*");
			oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
			oHttpConn.setConnectTimeout(iConnectionTimeOut);
			oHttpConn.setReadTimeout(iReadTimeOut);
			WasdiLog.debugLog("ProviderAdapter.getFileNameViaHttp: Timeout Setted: waiting response");
			int responseCode = oHttpConn.getResponseCode();

			// always check HTTP response code first
			if (responseCode == HttpURLConnection.HTTP_OK) {
				WasdiLog.debugLog("ProviderAdapter.getFileNameViaHttp: Connected");
				String sFileName = "";
				String sDisposition = oHttpConn.getHeaderField("Content-Disposition");
				String sContentType = oHttpConn.getContentType();
				int sContentLength = oHttpConn.getContentLength();
				if (sDisposition != null) {
					// extracts file name from header field
					int iIndex = sDisposition.indexOf("filename=");
					if (iIndex > 0) {
						if (sDisposition.endsWith("\"")) {
							sFileName = sDisposition.substring(iIndex + 10, sDisposition.length() - 1);
						}
						else {
							sFileName = sDisposition.substring(iIndex + 9, sDisposition.length());
						}
					}
				} else {
					// extracts file name from URL
					sFileName = sFileURL.substring(sFileURL.lastIndexOf("/") + 1, sFileURL.length());
				}

				sReturnFilePath = sFileName;

				WasdiLog.debugLog("Content-Type = " + sContentType);
				WasdiLog.debugLog("Content-Disposition = " + sDisposition);
				WasdiLog.debugLog("Content-Length = " + sContentLength);
				WasdiLog.debugLog("fileName = " + sFileName);
			} else {
				WasdiLog.debugLog("ProviderAdapter.getFileNameViaHttp No file to download. Server replied HTTP code: " + responseCode);
				m_iLastError = responseCode;
			}
			oHttpConn.disconnect();

			return sReturnFilePath;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProviderAdapter.getFileNameViaHttp: general error: " + oEx);
		}

		return null;
	}
	
    /**
     * @param oProcessWorkspace
     * @param iProgress
     */
    protected void updateProcessProgress(int iProgress) {
    	try {
	    	if (m_oProcessWorkspace == null) return;
	    	
	    	m_oProcessWorkspace.setProgressPerc(iProgress);
	    	//notify all subscribers
	    	for (ProcessWorkspaceUpdateSubscriber oSubscriber : m_aoSubscribers) {
				oSubscriber.notify(m_oProcessWorkspace);
			}
    	} catch (Exception oE) {
			WasdiLog.errorLog("ProviderAdapter.UpdateProcessProgress: " + oE);
		}
    }

    /**
     * Download a file via Http GET using Basic HTTP authentication if sDownloadUser is not null
     * @param sFileURL Url of the file to download
     * @param sDownloadUser Downlaod User
     * @param sDownloadPassword Download Password
     * @param sSaveDirOnServer Folder where to save the file
     * @return Full Path of the downlaoded file. Empty in case of problems
     * @throws IOException
     */
    protected String downloadViaHttp(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer) throws IOException {
    	return downloadViaHttp(sFileURL, sDownloadUser, sDownloadPassword, sSaveDirOnServer, "");
    }


    /**
     * Download a file via Http GET using Basic HTTP authentication if sDownloadUser is not null
     * @param sFileURL Url of the file to download
     * @param sDownloadUser Downlaod User
     * @param sDownloadPassword Download Password
     * @param sSaveDirOnServer Folder where to save the file
     * @param sOutputFileName Force Output file Name
     * @return Full Path of the downlaoded file. Empty in case of problems
     * @throws IOException
     */
	protected String downloadViaHttp(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, String sOutputFileName) throws IOException {

		// Return file path
		String sReturnFilePath = null;

		try {
			// Basic HTTP Authentication
			//WasdiLog.debugLog("ProviderAdapter.downloadViaHttp: sDownloadUser = " + sDownloadUser);

			if (sDownloadUser != null) {
				Authenticator.setDefault(new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						try {
							return new PasswordAuthentication(sDownloadUser, sDownloadPassword.toCharArray());
						} catch (Exception oEx) {
							WasdiLog.errorLog("ProviderAdapter.downloadViaHttp: exception setting auth " + ExceptionUtils.getStackTrace(oEx));
						}
						return null;
					}
				});
			}

			sReturnFilePath = downloadViaHttp(sFileURL, Collections.emptyMap(), sSaveDirOnServer, sOutputFileName);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProviderAdapter.downloadViaHttp: Exception " + oEx);
		}

		return sReturnFilePath;
	}

	/**
	 * Download a file via Http GET
	 * @param sFileURL Url of the file to download
	 * @param asHeaders Dictionary with additional headers
	 * @param sSaveDirOnServer Folder where to save the file
	 * @return Full Path of the downlaoded file. Empty in case of problems
	 * @throws IOException
	 */
	protected String downloadViaHttp(String sFileURL, Map<String, String> asHeaders, String sSaveDirOnServer) throws IOException {
		return downloadViaHttp(sFileURL, "GET", asHeaders, null, sSaveDirOnServer, null);
	}
	
	/**
	 * Download a file via Http GET
	 * @param sFileURL Url of the file to download
	 * @param asHeaders Dictionary with additional headers
	 * @param sSaveDirOnServer Folder where to save the file
	 * @param sOutputFileName Force the output name of the file
	 * @return Full Path of the downlaoded file. Empty in case of problems
	 * @throws IOException
	 */
	protected String downloadViaHttp(String sFileURL, Map<String, String> asHeaders, String sSaveDirOnServer, String sOutputFileName) throws IOException {
		return downloadViaHttp(sFileURL, "GET", asHeaders, null, sSaveDirOnServer, sOutputFileName);
	}
	
	/**
	 * Download a file via Http POST
	 * @param sFileURL Url of the file to download
	 * @param asHeaders Dictionary with additional headers
	 * @param sPayload Payload to add to the http POST request
	 * @param sSaveDirOnServer Folder where to save the file
	 * @return Full Path of the downlaoded file. Empty in case of problems
	 * @throws IOException
	 */
	protected String downloadViaHttpPost(String sFileURL, Map<String, String> asHeaders, String sPayload, String sSaveDirOnServer) throws IOException {
		return downloadViaHttp(sFileURL, "POST", asHeaders, sPayload, sSaveDirOnServer, null);
	}

	/**
	 * Download a file via Http using the specified HTTP Method
	 * @param sFileURL Url of the file to download
	 * @param sRequestMethod HTTP Request Method: supported "GET" and "POST"
	 * @param asHeaders Dictionary with additional headers
	 * @param sPayload Payload to add to the http POST request
	 * @param sSaveDirOnServer Folder where to save the file
	 * @param sOutputFileName Force output file name. Can be null to retrive it from the content disposition or if not available from the url (last part)
	 * @return Full Path of the downlaoded file. Empty in case of problems
	 * @throws IOException
	 */
	protected String downloadViaHttp(String sFileURL, String sRequestMethod, Map<String, String> asHeaders, String sPayload, String sSaveDirOnServer, String sOutputFileName) throws IOException {
		
		// Return file path
		String sReturnFilePath = null;

		WasdiLog.debugLog("ProviderAdapter.downloadViaHttp: FileUrl = " + sFileURL);

		URL oUrl = new URL(sFileURL);
		HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
		oHttpConn.setRequestMethod(sRequestMethod);

		for (Entry<String, String> asEntry : asHeaders.entrySet()) {
			oHttpConn.setRequestProperty(asEntry.getKey(), asEntry.getValue());
		}

		oHttpConn.setRequestProperty("Accept", "*/*");
		oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
		
		// Set Read Timeout
		oHttpConn.setReadTimeout(WasdiConfig.Current.readTimeout*m_iHttpDownloadReadTimeoutMultiplier);
		// Set Connection Timeout
		oHttpConn.setConnectTimeout(WasdiConfig.Current.connectionTimeout);			
		

		if (sPayload != null) {
			oHttpConn.setRequestProperty("Content-Type", "application/json");
			oHttpConn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
			oHttpConn.setDoOutput(true);
			byte[] ayBytes = sPayload.getBytes();
			oHttpConn.setFixedLengthStreamingMode(ayBytes.length);
			oHttpConn.connect();
			try (OutputStream os = oHttpConn.getOutputStream()) {
				os.write(ayBytes);
			}
		}
		
		int iResponseCode = oHttpConn.getResponseCode();

		// always check HTTP response code first
		if (iResponseCode == HttpURLConnection.HTTP_OK) {

			WasdiLog.debugLog("ProviderAdapter.downloadViaHttp: Connected");

			String sFileName = "";
			String sContentType = oHttpConn.getContentType();
			long lContentLength = oHttpConn.getContentLengthLong();

			if (Utils.isNullOrEmpty(sOutputFileName)) {
				String sDisposition = oHttpConn.getHeaderField("Content-Disposition");
				boolean bFromDisposition = !Utils.isNullOrEmpty(sDisposition); 
				if (bFromDisposition) {
					WasdiLog.debugLog("ProviderAdapter.downloadViaHttp: Content-Disposition = " + sDisposition);
					// extracts file name from header field
					String sFileNameKey = "filename=";
					int iStart = sDisposition.indexOf(sFileNameKey);
					if (iStart > 0) {
						WasdiLog.debugLog("ProviderAdapter.downloadViaHttp: trying to extract filename from 'Content-Disposition'");
						int iEnd = sDisposition.indexOf(' ', iStart);
						if(iEnd < 0) {
							iEnd = sDisposition.length();
						}
						sFileName = sDisposition.substring(iStart + sFileNameKey.length(), iEnd);
						while(sFileName.startsWith("\"") && sFileName.length()>1) {
							sFileName=sFileName.substring(1);
						}
						assert(!sFileName.startsWith("\""));
						while(sFileName.endsWith("\"") && sFileName.length()>1) {
							sFileName=sFileName.substring(0, sFileName.length()-1);
						}
						assert(!sFileName.endsWith("\""));
						
						
					} else {
						bFromDisposition = false;
					}
				}
				if(!bFromDisposition) {
					// extracts file name from URL
					WasdiLog.debugLog("ProviderAdapter.downloadViaHttp: trying to extract filename from URL");
					sFileName = sFileURL.substring(sFileURL.lastIndexOf("/") + 1);
				}				
			}
			else {
				sFileName = sOutputFileName;
			}
			WasdiLog.debugLog("fileName = " + sFileName);
			
			WasdiLog.debugLog("Content-Type = " + sContentType);
			WasdiLog.debugLog("Content-Length = " + lContentLength);
			

			// opens input stream from the HTTP connection
			InputStream oInputStream = oHttpConn.getInputStream();
			
			if (!sSaveDirOnServer.endsWith("/")) sSaveDirOnServer+="/";
			
			String sSaveFilePath = sSaveDirOnServer + sFileName;

			WasdiLog.debugLog("ProviderAdapter.downloadViaHttp: Create Save File Path = " + sSaveFilePath);

			File oTargetFile = new File(sSaveFilePath);
			File oTargetDir = oTargetFile.getParentFile();
			oTargetDir.mkdirs();

			// opens an output stream to save into file
			FileOutputStream oOutputStream = new FileOutputStream(sSaveFilePath);

			//Retry should be handled by the specific provider ExecuteDownloadingFile Method
			if (copyStream(m_oProcessWorkspace, lContentLength, oInputStream, oOutputStream)) {
				sReturnFilePath = sSaveFilePath;
				WasdiLog.debugLog("ProviderAdapter.downloadViaHttp File downloaded " + sReturnFilePath);
			}
			else {
				WasdiLog.debugLog("ProviderAdapter.downloadViaHttp copy stream returned false, not setting return file path" );
			}

		} 
		else {
			WasdiLog.debugLog("ProviderAdapter.downloadViaHttp: No file to download. Server replied HTTP code: " + iResponseCode);
			
			// Retrieve error
			InputStream oErrorStream = oHttpConn.getErrorStream();
			
			if(null != oErrorStream) {
				String sResult = IOUtils.toString(oErrorStream, StandardCharsets.UTF_8.toString());
				WasdiLog.debugLog("ProviderAdapter.downloadViaHttp: error message: " + sResult );
			} 
			else {
				WasdiLog.debugLog("ProviderAdapter.downloadViaHttp: provider did not send an error message");
			}
			m_iLastError = iResponseCode;
		}
		oHttpConn.disconnect();


		return sReturnFilePath;
	}

	/**
	 * Copy a source stream to a output stream notifying the progress
	 * @param oProcessWorkspace
	 * @param lContentLength
	 * @param oInputStream
	 * @param oOutputStream
	 * @throws IOException
	 */
	protected boolean copyStream(ProcessWorkspace oProcessWorkspace, long lContentLength, InputStream oInputStream, OutputStream oOutputStream) {

		// Cumulative Byte Count
		int iTotalBytes = 0;
		// Byte that represent 1% of the file
		long lOnePercent = lContentLength / 100;
		// Percent of the completed download
		int iFilePercent = 0;
		
		WasdiLog.debugLog("ProviderAdapter.copyStream: start copy");

		int iBytesRead = -1;
		byte[] abBuffer = new byte[BUFFER_SIZE];
		int iZeroes = MAX_NUM_ZEORES_DURING_READ;
		long lTotalLen = 0l;
		
		try {
			while ((iBytesRead = oInputStream.read(abBuffer)) != -1) {

				lTotalLen += (long)iBytesRead;
				if (iBytesRead <= 0) {
					WasdiLog.debugLog("ProviderAdapter.copyStream: Read 0 bytes from stream. Counter: " + iZeroes);
					iZeroes--;
				} 
				else {
					iZeroes = MAX_NUM_ZEORES_DURING_READ;
				}
				
				if (iZeroes <= 0) {
					//do not break here: sobloo does not send the Content-Length header, so the forecasted length might be wrong
					//instead, let the copy reach the end of the stream
					WasdiLog.debugLog("ProviderAdapter.copyStream: o bytes have been read for " + MAX_NUM_ZEORES_DURING_READ + "times, which is too many times: aborting copy");
					break;
					
				}

				oOutputStream.write(abBuffer, 0, iBytesRead);

				// Sum bytes
				iTotalBytes += iBytesRead;

				// Overcome a 1% limit?
				if (oProcessWorkspace != null && lContentLength > BUFFER_SIZE && iTotalBytes >= lOnePercent && iFilePercent <= 100) {
					
					// Increase the file
					iFilePercent += 1;
					if (iFilePercent > 100) {
						iFilePercent = 100;
					}
					
					// Reset the count
					iTotalBytes = 0;
					
					// Update the progress
					if (iZeroes == MAX_NUM_ZEORES_DURING_READ) {
						if ((iFilePercent%10) == 0) {
							updateProcessProgress(iFilePercent);
						}
					}
				}
			}
			
			WasdiLog.debugLog("ProviderAdapter.copyStream: EOF received, set process to 100% [was " + iFilePercent + "%]");
			updateProcessProgress(100);			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ProviderAdapter.copyStream: after reading " + lTotalLen + "/" + lContentLength + " an exception was caught: " + oEx);
			return false;
		}
		finally {
			try {
				oOutputStream.close();
			} catch (IOException e) {
				WasdiLog.errorLog("ProviderAdapter.copyStream: error", e);
			}
			try {
				oInputStream.close();
			} catch (IOException e) {
				WasdiLog.errorLog("ProviderAdapter.copyStream: error", e);
			}			
			
		}

		WasdiLog.debugLog("ProviderAdapter.copyStream: read " + lTotalLen + "/" + lContentLength + ", copy done");
		
		return true;
	}
	
	
	protected long getDownloadFileSizeViaHttp(String sFileURL)  throws Exception  {

        long lLenght = 0L;

        // Domain check
        if (Utils.isNullOrEmpty(sFileURL)) {
            WasdiLog.debugLog("ProviderAdapter.getDownloadFileSizeViaHttp: sFileURL is null");
            return lLenght;
        }
        
        String sUser = "";
        String sPassword = "";
        
        // TODO: Still needed? Really?
		try {
			DataProviderConfig oConfig = WasdiConfig.Current.getDataProviderConfig("SENTINEL");
			
			if (oConfig!=null) {
				sUser = oConfig.user;
				sPassword = oConfig.password;				
			}
		} catch (Exception e) {
			WasdiLog.errorLog("ProviderAdapter.getDownloadFileSizeViaHttp: error", e);
		}
        
        if (!Utils.isNullOrEmpty(m_sProviderUser)) sUser = m_sProviderUser;
        if (!Utils.isNullOrEmpty(m_sProviderPassword)) sPassword = m_sProviderPassword;

        final String sFinalUser = sUser;
        final String sFinalPassword = sPassword;
        
        // dhus authentication
        Authenticator.setDefault(new Authenticator() {
        	@Override
            protected PasswordAuthentication getPasswordAuthentication() {
                try{
                    return new PasswordAuthentication(sFinalUser, sFinalPassword.toCharArray());
                }
                catch (Exception oEx){
                    WasdiLog.errorLog("ProviderAdapter.getDownloadFileSizeViaHttp: exception setting auth " + ExceptionUtils.getStackTrace(oEx));
                }
                return null;
            }
        });

        WasdiLog.debugLog("ProviderAdapter.getDownloadFileSizeViaHttp: FileUrl = " + sFileURL);

        URL oUrl = new URL(sFileURL);
        HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
        oHttpConn.setRequestMethod("GET");
		oHttpConn.setRequestProperty("Accept", "*/*");
        oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
        int responseCode = oHttpConn.getResponseCode();

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {

            lLenght = oHttpConn.getHeaderFieldLong("Content-Length", 0L);

            WasdiLog.debugLog("ProviderAdapter.getDownloadFileSizeViaHttp: File size = " + lLenght);

            return lLenght;

        } else {
            WasdiLog.debugLog("ProviderAdapter.getDownloadFileSizeViaHttp: No file to download. Server replied HTTP code: " + responseCode);
            m_iLastError = responseCode;
        }
        oHttpConn.disconnect();

        return lLenght;
    }

	
	/**
	 * @param oHttpConn
	 * @param responseCode
	 * @throws IOException
	 */
	protected String  handleConnectionError(HttpURLConnection oHttpConn) throws IOException {
		Preconditions.checkNotNull(oHttpConn);

		String sError = null;
		try{
			m_iLastError = oHttpConn.getResponseCode();
			InputStream oErrorStream = oHttpConn.getErrorStream();
			sError = CharStreams.toString(new InputStreamReader(oErrorStream, Charsets.UTF_8));
			WasdiLog.warnLog("ProviderAdapter.handleConnectionError: Server replied HTTP code: " + m_iLastError + " and message is: \n" + sError);
		} catch (Exception oE) {
			WasdiLog.errorLog("ProviderAdapter.handleConnectionError: " + oE);
		}
		
		return sError;
	}

	/**
	 * Check if the protocol of the URL is <strong>https</strong>.
	 * @param sFileURL the URL of the file
	 * @return true if the protocol is <strong>https</strong>, false otherwise
	 */
	protected boolean isHttpsProtocol(String sFileURL) {
		return sFileURL.startsWith("https:");
	}

	/**
	 * Check if the protocol of the URL is <strong>file</strong>.
	 * @param sFileURL the URL of the file
	 * @return true if the protocol is <strong>file</strong>, false otherwise
	 */
	protected boolean isFileProtocol(String sFileURL) {
		return sFileURL.startsWith("file:");
	}

	/**
	 * Check if the file name corresponds to a <strong>.zip</strong> file.
	 * @param sFileName the name of the file
	 * @return true if the file is <strong>.zip</strong>, false otherwise
	 */
	protected boolean isZipFile(String sFileName) {
		return sFileName.endsWith(".zip");
	}

	/**
	 * Check if the file name corresponds to a <strong>.SAFE</strong> directory.
	 * @param sFileName the name of the file
	 * @return true if the file is <strong>.SAFE</strong> directory, false otherwise
	 */
	protected boolean isSafeDirectory(String sFileName) {
		return sFileName.endsWith(".SAFE");
	}

	/**
	 * Remove the .zip extension from the name, if it exists.
	 * @param sName the name of the file
	 * @return the name of the file without the .zip extension
	 */
	protected String removeZipExtension(String sName) {
		if (sName == null || !sName.endsWith(".zip")) {
			return sName;
		} else {
			return sName.replace(".zip", "");
		}
	}

	/**
	 * Add the .zip extension to the name, if it does not yet exists.
	 * @param sName the name of the file
	 * @return the name of the file with the .zip extension
	 */
	protected String addZipExtension(String sName) {
		if (sName == null || sName.endsWith(".zip")) {
			return sName;
		} else {
			return sName.concat(".zip");
		}
	}

	/**
	 * Remove the .SAFE termination from the name, if it exists.
	 * @param sName the name of the directory
	 * @return the name of the directory without the .SAFE termination
	 */
	protected String removeSafeTermination(String sName) {
		if (sName == null || !sName.endsWith(".SAFE")) {
			return sName;
		} else {
			return sName.replace(".SAFE", "");
		}
	}

	/**
	 * Remove the <strong>file</strong> prefix from the URL.
	 * @param sFileURL the URL of the file
	 * @return the path of the file without the <strong>file</strong> prefix
	 */
	protected String removePrefixFile(String sFileURL) {
		String sPrefix = "file:";
		int iStart = sFileURL.indexOf(sPrefix) + sPrefix.length();
		String sPath = sFileURL.substring(iStart);

		WasdiLog.debugLog("ProviderAdapter.removeFilePrefix: full path " + sPath);

		return sPath;
	}

	/**
	 * Remove the <strong>/.value</strong> suffix from the path of the file.
	 * @param sPath the path of the file
	 * @return the path of the file without the <strong>/.value</strong> suffix
	 */
	protected String removeSuffixValue(String sPath) {
		String sSuffix = "/.value";
		sPath = sPath.substring(0, sPath.lastIndexOf(sSuffix));

		return sPath;
	}

	/**
	 * Extract the destination file name by removing the directory path.
	 * @param sPath the path of the file
	 * @return the resulting destination file name
	 */
	protected String extractDestinationFileName(String sPath) {
		// Destination file name: start from the simple name, i.e., exclude the containing dir, slash included:
		String sDestinationFileName = sPath.substring( sPath.lastIndexOf("/") + 1);

		return sDestinationFileName;
	}

	/**
	 * Get The length, in bytes, of the source-file.
	 * @param oSourceFile the source-file
	 * @return the length, in bytes, of the source-file, or 0L if the file does not exist
	 */
	protected long getSourceFileLength(File oSourceFile) {
		long lLenght;
		if (oSourceFile.isDirectory()) {
			lLenght = 0L;

			for (File f : oSourceFile.listFiles()) {
				lLenght += getSourceFileLength(f);
			}
		} else {
			lLenght = oSourceFile.length();
		}
		
		if (!oSourceFile.exists()) {
			WasdiLog.debugLog("ProviderAdapter.getSourceFileLength: FILE DOES NOT EXISTS");
		}
		WasdiLog.debugLog("ProviderAdapter.getSourceFileLength: Found length " + lLenght);

		return lLenght;
	}

	protected String localFileCopy(String sFileURL, String sSaveDirOnServer, int iMaxRetry) throws Exception {
		//  file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip/.value		
		WasdiLog.debugLog("ProviderAdapter.localFileCopy: this is a \"file:\" protocol, get file name");
		
		String sPath = removePrefixFile(sFileURL);
		File oSourceFile = new File(sPath);

		// Destination file name: start from the simple name
		String sDestinationFileName = getFileName(sFileURL);

		// set the destination folder
		if (sSaveDirOnServer.endsWith("/") == false) sSaveDirOnServer += "/";
		sDestinationFileName = sSaveDirOnServer + sDestinationFileName;
		
		WasdiLog.debugLog("ProviderAdapter.localFileCopy: destination file: " + sDestinationFileName);
		
		InputStream oInputStream = null;
		OutputStream oOutputStream = null;

		// copy the product from file system
		try {
			File oDestionationFile = new File(sDestinationFileName);
			
			if (oDestionationFile.getParentFile() != null) { 
				if (oDestionationFile.getParentFile().exists() == false) {
					oDestionationFile.getParentFile().mkdirs();
				}
			}
			
			oInputStream = new FileInputStream(oSourceFile);
			oOutputStream = new FileOutputStream(oDestionationFile);
			
			WasdiLog.debugLog("ProviderAdapter.localFileCopy: start copy stream");
			
			int iAttempts = iMaxRetry;

			while(iAttempts > 0) {
				
				WasdiLog.debugLog("ProviderAdapter.localFileCopy: Attemp #" + (iMaxRetry-iAttempts+1));
				
				if (copyStream(m_oProcessWorkspace, oSourceFile.length(), oInputStream, oOutputStream)) {
					
					String sNameOnly = oDestionationFile.getName();
					
					if (sNameOnly.startsWith("S1") || sNameOnly.startsWith("S2")) {
						
						try {
							// Product Reader will be used to test if the image has been downloaded with success.
							WasdiProductReader oReadProduct = WasdiProductReaderFactory.getProductReader(oDestionationFile);
							
							Product oProduct = oReadProduct.getSnapProduct();
							
							if (oProduct != null)  {
								// Break the retry attempt cycle
								break;							
							}
							else {
								WasdiLog.debugLog("ProviderAdapter.localFileCopy: file not readable: " + oDestionationFile.getPath() + " try again");
							}								
						}
						catch (Exception oReadEx) {
							WasdiLog.debugLog("ProviderAdapter.localFileCopy: exception reading file: " + oReadEx.toString() + " try again");
						}
						
						
						try {
							WasdiLog.debugLog("ProviderAdapter.localFileCopy: delete corrupted file");
							if (oDestionationFile.delete()== false) {
								WasdiLog.debugLog("ProviderAdapter.localFileCopy: error deleting corrupted file");
							}
						}
						catch (Exception oDeleteEx) {
							WasdiLog.debugLog("ProviderAdapter.localFileCopy: exception deleting not valid file ");
						}
					}
					else {
						// Break the retry attempt cycle
						break;							
					}						
				}
				else {
					WasdiLog.debugLog("ProviderAdapter.localFileCopy: error in the copy stream.");
				}
				
				iAttempts--;
				
				TimeUnit.SECONDS.sleep(2);
			}

		} 
		catch(InterruptedException oEx) {
			Thread.currentThread().interrupt();
			WasdiLog.errorLog("ProviderAdapter.localFileCopy: current thread interrupted ", oEx);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProviderAdapter.localFileCopy: ", oEx);
		}
		finally {
			try {
				if (oOutputStream != null) {
					oOutputStream.close();
				}
			} catch (IOException e) {
				
			}
			try {
				if (oInputStream!= null) {
					oInputStream.close();
				}
			} catch (IOException e) {
				
			}
		}
		
		return sDestinationFileName;
	}

}
