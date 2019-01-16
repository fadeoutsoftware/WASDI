package wasdi.filebuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import wasdi.ConfigReader;
import wasdi.LauncherMain;
import wasdi.ProcessWorkspaceUpdateNotifier;
import wasdi.ProcessWorkspaceUpdateSubscriber;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.Utils;

/**
 * Base Download Utility Class 
 * Created by s.adamo on 06/10/2016.
 */
public abstract class ProviderAdapter implements ProcessWorkspaceUpdateNotifier {

	protected final int BUFFER_SIZE = 4096;
    protected final int MAX_NUM_ZEORES_DURING_READ = 20;
    protected Logger m_oLogger;
    protected int m_iLastError = 0;
	
    protected String m_sProviderUser;
    protected String m_sProviderPassword;
    
    ProcessWorkspace m_oProcessWorkspace;
    
    private List<ProcessWorkspaceUpdateSubscriber> m_aoSubscribers;

    /**
     * Constructor: uses LauncerMain logger
     */
    public ProviderAdapter() {
		this(LauncherMain.s_oLogger);	
	}
    
    /**
     * Constructor with user defined logger
     * @param logger
     */
    public ProviderAdapter(Logger logger) {
		this.m_oLogger = logger;
		m_aoSubscribers = new ArrayList<ProcessWorkspaceUpdateSubscriber>();
	}

    /**
     * Abstract method to get the size of the downloaded file
     * @param sFileURL URL of the file
     * @return
     */
	public abstract long GetDownloadFileSize(String sFileURL) throws Exception;
	
	/**
	 * Abstract method to download the file
	 * @param sFileURL URL of the file
	 * @param sDownloadUser User to authenticate to the provider
	 * @param sDownloadPassword Password to authenticate to the provider
	 * @param sSaveDirOnServer Local save path
	 * @param oProcessWorkspace Process Workspace to update the user
	 * @return
	 */
    public abstract String ExecuteDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace) throws Exception;
    
    /**
     * Abstract method to get the name of the file from the url
     * @param sFileURL URL of the file
     * @return
     */
    public abstract String GetFileName(String sFileURL) throws Exception;
    
    @Override
	public void subscribe(ProcessWorkspaceUpdateSubscriber oSubscriber) {
    	if(null != oSubscriber) {
    		m_aoSubscribers.add(oSubscriber);
    	} else {
    		m_oLogger.warn("ProviderAdapter.subscribe: null subscriber");
    	}
	}

	@Override
	public void unsubscribe(ProcessWorkspaceUpdateSubscriber oSubscriber) {
		if(!m_aoSubscribers.remove(oSubscriber)) {
			m_oLogger.warn("ProviderAdapter.subscribe: subscriber not found");
		}
	}
     
    /**
     * 
     * @param oProcessWorkspace
     * @param iProgress
     */
    protected void UpdateProcessProgress(int iProgress) {
    	
    	if (m_oProcessWorkspace == null) return;
    	m_oProcessWorkspace.setProgressPerc(iProgress);
    	//notify all subscribers
    	for (ProcessWorkspaceUpdateSubscriber oSubscriber : m_aoSubscribers) {
			oSubscriber.notify(m_oProcessWorkspace);
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
	 * Download a file via Http using Basic HTTP authentication is sDownloadUser is not null
	 */
	protected String downloadViaHttp(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer) throws IOException {
		String sReturnFilePath = "";

		// Basic HTTP Authentication
		m_oLogger.debug("ProviderAdapter.downloadViaHttp: sDownloadUser = " + sDownloadUser);

		if (sDownloadUser != null) {
			Authenticator.setDefault(new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					try {
						return new PasswordAuthentication(sDownloadUser, sDownloadPassword.toCharArray());
					} catch (Exception oEx) {
						m_oLogger.error("ProviderAdapter.downloadViaHttp: exception setting auth "
								+ org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
					}
					return null;
				}
			});
		}

		m_oLogger.debug("ProviderAdapter.downloadViaHttp: FileUrl = " + sFileURL);

		URL oUrl = new URL(sFileURL);
		HttpURLConnection oConnection = (HttpURLConnection) oUrl.openConnection();
		oConnection.setRequestMethod("GET");
		oConnection.setRequestProperty("Accept", "*/*");

		int responseCode = oConnection.getResponseCode();

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {

			m_oLogger.debug("ProviderAdapter.downloadViaHttp: Connected");

			String sFileName = "";
			String sDisposition = oConnection.getHeaderField("Content-Disposition");
			String sContentType = oConnection.getContentType();
			long lContentLength = oConnection.getContentLength();

			m_oLogger.debug("ProviderAdapter.downloadViaHttp. ContentLenght: " + lContentLength);

			if (sDisposition != null) {
				// extracts file name from header field
				int index = sDisposition.indexOf("filename=");
				if (index > 0) {
					sFileName = sDisposition.substring(index + 10, sDisposition.length() - 1);
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
			InputStream oInputStream = oConnection.getInputStream();
			String sSaveFilePath = sSaveDirOnServer + "/" + sFileName;

			m_oLogger.debug("ProviderAdapter.downloadViaHttp: Create Save File Path = " + sSaveFilePath);

			File oTargetFile = new File(sSaveFilePath);
			File oTargetDir = oTargetFile.getParentFile();
			oTargetDir.mkdirs();

			// opens an output stream to save into file
			FileOutputStream oOutputStream = new FileOutputStream(sSaveFilePath);

			copyStream(m_oProcessWorkspace, lContentLength, oInputStream, oOutputStream);

			sReturnFilePath = sSaveFilePath;

			m_oLogger.debug("ProviderAdapter.downloadViaHttp File downloaded " + sReturnFilePath);
		} else {
			m_oLogger.debug(
					"ProviderAdapter.downloadViaHttp No file to download. Server replied HTTP code: " + responseCode);
			m_iLastError = responseCode;
		}
		oConnection.disconnect();
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
	protected void copyStream(ProcessWorkspace oProcessWorkspace, long lContentLength, InputStream oInputStream, OutputStream oOutputStream) throws IOException {

		// Cumulative Byte Count
		int iTotalBytes = 0;
		// Byte that represent 10% of the file
		long lOnePercent = lContentLength / 100;
		// Percent of the completed download
		int iFilePercent = 0;
		
		m_oLogger.debug("ProviderAdapter.copyStream: start copy");

		int iBytesRead = -1;
		byte[] abBuffer = new byte[BUFFER_SIZE];
		int nZeroes = MAX_NUM_ZEORES_DURING_READ;
		while ((iBytesRead = oInputStream.read(abBuffer)) != -1) {

			if (iBytesRead <= 0) {
				m_oLogger.debug("ProviderAdapter.copyStream: Read 0 bytes from stream. Counter: " + nZeroes);
				nZeroes--;
			} else {
				nZeroes = MAX_NUM_ZEORES_DURING_READ;
			}
			if (nZeroes <= 0)
				break;

			// logger.debug("ExecuteDownloadFile. Read " + iBytesRead + " bytes from
			// stream");

			oOutputStream.write(abBuffer, 0, iBytesRead);

			// Sum bytes
			iTotalBytes += iBytesRead;

			// Overcome a 1% limit?
			if (oProcessWorkspace != null && lContentLength > BUFFER_SIZE && iTotalBytes >= lOnePercent
					&& iFilePercent <= 100) {
				// Increase the file
				iFilePercent += 1;
				if (iFilePercent > 100)
					iFilePercent = 100;
				// Reset the count
				iTotalBytes = 0;
				// Update the progress
				if (nZeroes == MAX_NUM_ZEORES_DURING_READ)
					UpdateProcessProgress(iFilePercent);
			}
		}

		oOutputStream.close();
		oInputStream.close();
		
		m_oLogger.debug("ProviderAdapter.copyStream: copy done");
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

        URL url = new URL(sFileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();

        // always check HTTP response code first
        if (responseCode == HttpURLConnection.HTTP_OK) {

            lLenght = httpConn.getHeaderFieldLong("Content-Length", 0L);

            m_oLogger.debug("ProviderAdapter.getDownloadFileSizeViaHttp: File size = " + lLenght);

            return lLenght;

        } else {
            m_oLogger.debug("ProviderAdapter.getDownloadFileSizeViaHttp: No file to download. Server replied HTTP code: " + responseCode);
            m_iLastError = responseCode;
        }
        httpConn.disconnect();

        return lLenght;
    }
	
	
	/**
	 * Get File Name via http
	 * @param sFileURL
	 * @return
	 * @throws IOException
	 */
	public String getFileNameViaHttp(String sFileURL) throws IOException {

		try {
			// Domain check
			if (Utils.isNullOrEmpty(sFileURL)) {
				m_oLogger.debug("ProviderAdapter.getFileNameViaHttp: sFileURL is null or Empty");
				return "";
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
				m_oLogger.error(org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
			}
			try {
				iReadTimeOut = Integer.parseInt(sReadTimeOut);
			} catch (Exception oEx) {
				m_oLogger.error(org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
			}

			URL oUrl = new URL(sFileURL);
			HttpURLConnection oHttpConn = (HttpURLConnection) oUrl.openConnection();
			m_oLogger.debug("ProviderAdapter.getFileNameViaHttp: Connection Created");
			
			//NOTE: the DhUS version did not set GET and Accept. ONDA did. TEST 
			oHttpConn.setRequestMethod("GET");
			oHttpConn.setRequestProperty("Accept", "*/*");
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
			m_oLogger.error(org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
		}

		return "";
	}
}
