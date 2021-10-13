package wasdi.filebuffer;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.esa.snap.core.datamodel.Product;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;

import wasdi.ConfigReader;
import wasdi.LauncherMain;
import wasdi.ProcessWorkspaceUpdateNotifier;
import wasdi.ProcessWorkspaceUpdateSubscriber;
import wasdi.io.WasdiProductReader;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.LoggerWrapper;
import wasdi.shared.utils.Utils;

/**
 * Base Download Utility Class 
 * Created by s.adamo on 06/10/2016.
 */
public abstract class ProviderAdapter implements ProcessWorkspaceUpdateNotifier {

	protected final int BUFFER_SIZE = 4096;
    protected final int MAX_NUM_ZEORES_DURING_READ = 20;
    protected LoggerWrapper m_oLogger;
    protected int m_iLastError = 0;
	
    protected String m_sProviderUser;
    protected String m_sProviderPassword;
    
    protected String m_sDefaultProtocol = "https://";
    
    ProcessWorkspace m_oProcessWorkspace;
    
    private List<ProcessWorkspaceUpdateSubscriber> m_aoSubscribers;

    /**
     * Constructor: uses LauncerMain logger
     */
    public ProviderAdapter() {
		this(LauncherMain.s_oLogger);	
	}
    
    public abstract void readConfig();
    
    /**
     * Constructor with user defined logger
     * @param logger
     */
    public ProviderAdapter(LoggerWrapper logger) {
		this.m_oLogger = logger;
		m_aoSubscribers = new ArrayList<ProcessWorkspaceUpdateSubscriber>();
	}

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
	 * @return
	 */
    public abstract String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception;
    
    /**
     * Abstract method to get the name of the file from the url
     * @param sFileURL URL of the file
     * @return
     */
    public abstract String getFileName(String sFileURL) throws Exception;
    

    @Override
	public void subscribe(ProcessWorkspaceUpdateSubscriber oSubscriber) {
    	try {
	    	if(null != oSubscriber) {
	    		m_aoSubscribers.add(oSubscriber);
	    	} else {
	    		m_oLogger.warn("ProviderAdapter.subscribe: null subscriber");
	    	}
    	} catch (Exception oE) {
    		m_oLogger.error("ProviderAdapter.subscribe: " + oE); 
		}
	}

	@Override
	public void unsubscribe(ProcessWorkspaceUpdateSubscriber oSubscriber) {
		try {
			if(!m_aoSubscribers.remove(oSubscriber)) {
				m_oLogger.warn("ProviderAdapter.subscribe: subscriber not found");
			}
		} catch (Exception oE) {
    		m_oLogger.error("ProviderAdapter.unsubscribe: " + oE); 
		}
	}
	
	public void setProcessWorkspace(ProcessWorkspace oProcessWorkspace) {
		if(null!=oProcessWorkspace) {
			m_oProcessWorkspace = oProcessWorkspace;
		} else {
			m_oLogger.error("ProviderAdapter.setProcessWorkspace: oProcessWorkspace is null");
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
				m_oLogger.error("ProviderAdapter.getFileNameViaHttp: sFileURL is null or Empty");
				return null;
			}

			String sReturnFilePath = "";

			String sUser = ConfigReader.getPropValue("DHUS_USER");
			String sPassword = ConfigReader.getPropValue("DHUS_PASSWORD");

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
						m_oLogger.error("ProviderAdapter.getFileNameViaHttp: exception setting auth "
								+ org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
					}
					return null;
				}
			});

			m_oLogger.debug("ProviderAdapter.getFileNameViaHttp: FileUrl = " + sFileURL);

			String sConnectionTimeout = ConfigReader.getPropValue("CONNECTION_TIMEOUT");
			String sReadTimeOut = ConfigReader.getPropValue("READ_TIMEOUT");

			int iConnectionTimeOut = 10000;
			int iReadTimeOut = 10000;

			try {
				iConnectionTimeOut = Integer.parseInt(sConnectionTimeout);
			} catch (Exception oEx) {
				m_oLogger.error("ProviderAdapter.getFileNameViaHttp: connection timed out: " + oEx);
			}
			try {
				iReadTimeOut = Integer.parseInt(sReadTimeOut);
			} catch (Exception oEx) {
				m_oLogger.error("ProviderAdapter.getFileNameViaHttp: read timed out: " + oEx);
			}

			URL oUrl = new URL(sFileURL);
			HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
			m_oLogger.debug("ProviderAdapter.getFileNameViaHttp: Connection Created");
			
			//NOTE: the DhUS version did not set GET and Accept. ONDA did. TEST 
			oHttpConn.setRequestMethod("GET");
			oHttpConn.setRequestProperty("Accept", "*/*");
			oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
			oHttpConn.setConnectTimeout(iConnectionTimeOut);
			oHttpConn.setReadTimeout(iReadTimeOut);
			m_oLogger.debug("ProviderAdapter.getFileNameViaHttp: Timeout Setted: waiting response");
			int responseCode = oHttpConn.getResponseCode();

			// always check HTTP response code first
			if (responseCode == HttpURLConnection.HTTP_OK) {
				m_oLogger.debug("ProviderAdapter.getFileNameViaHttp: Connected");
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

				m_oLogger.debug("Content-Type = " + sContentType);
				m_oLogger.debug("Content-Disposition = " + sDisposition);
				m_oLogger.debug("Content-Length = " + sContentLength);
				m_oLogger.debug("fileName = " + sFileName);
			} else {
				m_oLogger.debug("ProviderAdapter.getFileNameViaHttp No file to download. Server replied HTTP code: " + responseCode);
				m_iLastError = responseCode;
			}
			oHttpConn.disconnect();

			return sReturnFilePath;
			
		} catch (Exception oEx) {
			m_oLogger.error("ProviderAdapter.getFileNameViaHttp: general error: " + oEx);
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
			m_oLogger.error("ProviderAdapter.UpdateProcessProgress: " + oE);
		}
    }
	
	/**
	 * Download a file via Http using Basic HTTP authentication is sDownloadUser is not null
	 */
	protected String downloadViaHttp(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer) throws IOException {
		
		// Return file path
		String sReturnFilePath = null;
		
		try {
			// Basic HTTP Authentication
			//m_oLogger.debug("ProviderAdapter.downloadViaHttp: sDownloadUser = " + sDownloadUser);
			
			if (sDownloadUser != null) {
				Authenticator.setDefault(new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						try {
							return new PasswordAuthentication(sDownloadUser, sDownloadPassword.toCharArray());
						} catch (Exception oEx) {
							m_oLogger.error("ProviderAdapter.downloadViaHttp: exception setting auth " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
						}
						return null;
					}
				});
			}

			m_oLogger.debug("ProviderAdapter.downloadViaHttp: FileUrl = " + sFileURL);

			URL oUrl = new URL(sFileURL);
			HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
			oHttpConn.setRequestMethod("GET");
			oHttpConn.setRequestProperty("Accept", "*/*");
			oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
			int iResponseCode = oHttpConn.getResponseCode();

			// always check HTTP response code first
			if (iResponseCode == HttpURLConnection.HTTP_OK) {

				m_oLogger.debug("ProviderAdapter.downloadViaHttp: Connected");

				String sFileName = "";
				String sDisposition = oHttpConn.getHeaderField("Content-Disposition");
				String sContentType = oHttpConn.getContentType();
				long lContentLength = oHttpConn.getContentLengthLong();

				boolean bFromDisposition = !Utils.isNullOrEmpty(sDisposition); 
				if (bFromDisposition) {
					m_oLogger.debug("ProviderAdapter.downloadViaHttp: Content-Disposition = " + sDisposition);
					// extracts file name from header field
					String sFileNameKey = "filename=";
					int iStart = sDisposition.indexOf(sFileNameKey);
					if (iStart > 0) {
						m_oLogger.debug("ProviderAdapter.downloadViaHttp: trying to extract filename from 'Content-Disposition'");
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
					m_oLogger.debug("ProviderAdapter.downloadViaHttp: trying to extract filename from URL");
					sFileName = sFileURL.substring(sFileURL.lastIndexOf("/") + 1);
				}
				m_oLogger.debug("fileName = " + sFileName);
				
				m_oLogger.debug("Content-Type = " + sContentType);
				m_oLogger.debug("Content-Length = " + lContentLength);
				

				// opens input stream from the HTTP connection
				InputStream oInputStream = oHttpConn.getInputStream();
				
				if (!sSaveDirOnServer.endsWith("/")) sSaveDirOnServer+="/";
				
				String sSaveFilePath = sSaveDirOnServer + sFileName;

				m_oLogger.debug("ProviderAdapter.downloadViaHttp: Create Save File Path = " + sSaveFilePath);

				File oTargetFile = new File(sSaveFilePath);
				File oTargetDir = oTargetFile.getParentFile();
				oTargetDir.mkdirs();

				// opens an output stream to save into file
				FileOutputStream oOutputStream = new FileOutputStream(sSaveFilePath);

				//Retry should be handled by the specific provider ExecuteDownloadingFile Method
				if (copyStream(m_oProcessWorkspace, lContentLength, oInputStream, oOutputStream)) {
					sReturnFilePath = sSaveFilePath;
					m_oLogger.debug("ProviderAdapter.downloadViaHttp File downloaded " + sReturnFilePath);
				}
				else {
					m_oLogger.debug("ProviderAdapter.downloadViaHttp copy stream returned false, not setting return file path" );
				}

			} else {
				m_oLogger.debug("ProviderAdapter.downloadViaHttp: No file to download. Server replied HTTP code: " + iResponseCode);
				//todo retrieve error
				InputStream oErrorStream = oHttpConn.getErrorStream();
				if(null != oErrorStream) {
					InputStreamReader oReader = new InputStreamReader(oErrorStream);
					m_oLogger.debug("ProviderAdapter.downloadViaHttp: error message: " + oReader.toString() );
				} else {
					m_oLogger.debug("ProviderAdapter.downloadViaHttp: provider did not send an error message");
				}
				m_iLastError = iResponseCode;
			}
			oHttpConn.disconnect();			
		}
		catch (Exception oEx) {
			m_oLogger.error("ProviderAdapter.downloadViaHttp: Exception " + oEx);
			return null;
		}


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
		
		m_oLogger.debug("ProviderAdapter.copyStream: start copy");

		int iBytesRead = -1;
		byte[] abBuffer = new byte[BUFFER_SIZE];
		int iZeroes = MAX_NUM_ZEORES_DURING_READ;
		long lTotalLen = 0l;
		
		try {
			while ((iBytesRead = oInputStream.read(abBuffer)) != -1) {

				lTotalLen += (long)iBytesRead;
				if (iBytesRead <= 0) {
					m_oLogger.debug("ProviderAdapter.copyStream: Read 0 bytes from stream. Counter: " + iZeroes);
					iZeroes--;
				} 
				else {
					iZeroes = MAX_NUM_ZEORES_DURING_READ;
				}
				
				if (iZeroes <= 0) {
					//do not break here: sobloo does not send the Content-Length header, so the forecasted length might be wrong
					//instead, let the copy reach the end of the stream
					m_oLogger.debug("ProviderAdapter.copyStream: o bytes have been read for " + MAX_NUM_ZEORES_DURING_READ + "times, which is too many times: aborting copy");
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
			
			m_oLogger.debug("ProviderAdapter.copyStream: EOF received, set process to 100% [was " + iFilePercent + "%]");
			updateProcessProgress(100);			
		}
		catch (Exception oEx) {
			m_oLogger.debug("ProviderAdapter.copyStream: after reading " + lTotalLen + "/" + lContentLength + " an exception was caught: " + oEx);
			return false;
		}
		finally {
			try {
				oOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				oInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}			
			
		}

		m_oLogger.debug("ProviderAdapter.copyStream: read " + lTotalLen + "/" + lContentLength + ", copy done");
		
		return true;
	}
	
	
	protected long getDownloadFileSizeViaHttp(String sFileURL)  throws Exception  {

        long lLenght = 0L;

        // Domain check
        if (Utils.isNullOrEmpty(sFileURL)) {
            m_oLogger.debug("ProviderAdapter.getDownloadFileSizeViaHttp: sFileURL is null");
            return lLenght;
        }
        
        String sUser = "";
        String sPassword = "";
		try {
			sUser = ConfigReader.getPropValue("DHUS_USER");
			sPassword = ConfigReader.getPropValue("DHUS_PASSWORD");
		} catch (IOException e) {
			e.printStackTrace();
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
                    m_oLogger.error("ProviderAdapter.getDownloadFileSizeViaHttp: exception setting auth " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
                }
                return null;
            }
        });

        m_oLogger.debug("ProviderAdapter.getDownloadFileSizeViaHttp: FileUrl = " + sFileURL);

        URL oUrl = new URL(sFileURL);
        HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
        oHttpConn.setRequestMethod("GET");
		oHttpConn.setRequestProperty("Accept", "*/*");
        oHttpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:57.0) Gecko/20100101 Firefox/57.0");
        int responseCode = oHttpConn.getResponseCode();

        // always check HTTP response code first
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
			m_oLogger.warn("ProviderAdapter.handleConnectionError: Server replied HTTP code: " + m_iLastError + " and message is: \n" + sError);
		} catch (Exception oE) {
			m_oLogger.error("ProviderAdapter.handleConnectionError: " + oE);
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

		m_oLogger.debug("ProviderAdapter.removeFilePrefix: full path " + sPath);

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
			m_oLogger.debug("ProviderAdapter.getSourceFileLength: FILE DOES NOT EXISTS");
		}
		m_oLogger.debug("ProviderAdapter.getSourceFileLength: Found length " + lLenght);

		return lLenght;
	}

	protected String localFileCopy(String sFileURL, String sSaveDirOnServer, int iMaxRetry) throws Exception {
		//  file:/mnt/OPTICAL/LEVEL-1C/2018/12/12/S2B_MSIL1C_20181212T010259_N0207_R045_T54PZA_20181212T021706.zip/.value		
		m_oLogger.debug("ProviderAdapter.localFileCopy: this is a \"file:\" protocol, get file name");
		
		String sPath = removePrefixFile(sFileURL);
		File oSourceFile = new File(sPath);

		// Destination file name: start from the simple name
		String sDestinationFileName = getFileName(sFileURL);

		// set the destination folder
		if (sSaveDirOnServer.endsWith("/") == false) sSaveDirOnServer += "/";
		sDestinationFileName = sSaveDirOnServer + sDestinationFileName;
		
		m_oLogger.debug("ProviderAdapter.localFileCopy: destination file: " + sDestinationFileName);
		
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
			
			m_oLogger.debug("ProviderAdapter.localFileCopy: start copy stream");
			
			int iAttempts = iMaxRetry;

			while(iAttempts > 0) {
				
				m_oLogger.debug("ProviderAdapter.localFileCopy: Attemp #" + (iMaxRetry-iAttempts+1));
				
				if (copyStream(m_oProcessWorkspace, oSourceFile.length(), oInputStream, oOutputStream)) {
					
					String sNameOnly = oDestionationFile.getName();
					
					if (sNameOnly.startsWith("S1") || sNameOnly.startsWith("S2")) {
						
						try {
							// Product Reader will be used to test if the image has been downloaded with success.
							WasdiProductReader oReadProduct = new WasdiProductReader();
							
							Product oProduct = oReadProduct.readSnapProduct(oDestionationFile, null);
							
							if (oProduct != null)  {
								// Break the retry attempt cycle
								break;							
							}
							else {
								m_oLogger.debug("ProviderAdapter.localFileCopy: file not readable: " + oDestionationFile.getPath() + " try again");
								try {
									String sDestination = oDestionationFile.getPath();
									sDestination += ".attemp"+ (iMaxRetry-iAttempts+1);
									FileUtils.copyFile(oDestionationFile, new File(sDestination));										
								}
								catch (Exception oEx) {
									m_oLogger.debug("ProviderAdapter.localFileCopy: Exception making copy of attempt file " + oEx.toString());
								}
							}								
						}
						catch (Exception oReadEx) {
							m_oLogger.debug("ProviderAdapter.localFileCopy: exception reading file: " + oReadEx.toString() + " try again");
						}
						
						
						try {
							m_oLogger.debug("ProviderAdapter.localFileCopy: delete corrupted file");
							if (oDestionationFile.delete()== false) {
								m_oLogger.debug("ProviderAdapter.localFileCopy: error deleting corrupted file");
							}
						}
						catch (Exception oDeleteEx) {
							m_oLogger.debug("ProviderAdapter.localFileCopy: exception deleting not valid file ");
						}
					}
					else {
						// Break the retry attempt cycle
						break;							
					}						
				}
				else {
					m_oLogger.debug("ProviderAdapter.localFileCopy: error in the copy stream.");
				}
				
				iAttempts--;
				
				TimeUnit.SECONDS.sleep(2);
			}

		} catch (Exception e) {
			m_oLogger.info("ProviderAdapter.localFileCopy: " + e);
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
