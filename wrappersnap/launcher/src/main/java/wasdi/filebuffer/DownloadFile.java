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

import wasdi.LauncherMain;
import wasdi.ProcessWorkspaceUpdateNotifier;
import wasdi.ProcessWorkspaceUpdateSubscriber;
import wasdi.shared.business.ProcessWorkspace;

/**
 * Base Download Utility Class 
 * Created by s.adamo on 06/10/2016.
 */
public abstract class DownloadFile implements ProcessWorkspaceUpdateNotifier {

	protected final int BUFFER_SIZE = 4096;
    protected final int MAX_NUM_ZEORES_DURING_READ = 20;
    protected Logger m_oLogger;
    protected int m_iLastError = 0;
	
    protected String m_sProviderUser;
    protected String m_sProviderPassword;
    
    private List<ProcessWorkspaceUpdateSubscriber> m_aoSubscribers;

    /**
     * Constructor: uses LauncerMain logger
     */
    public DownloadFile() {
		this(LauncherMain.s_oLogger);	
	}
    
    /**
     * Constructor with user defined logger
     * @param logger
     */
    public DownloadFile(Logger logger) {
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
    		m_oLogger.warn("DownloadFile.subscribe: null subscriber");
    	}
	}

	@Override
	public void unsubscribe(ProcessWorkspaceUpdateSubscriber oSubscriber) {
		if(!m_aoSubscribers.remove(oSubscriber)) {
			m_oLogger.warn("DownloadFile.subscribe: subscriber not found");
		}
		
	}
    
   
    protected void copyStream(ProcessWorkspace oProcessWorkspace, long lContentLength, InputStream oInputStream,
			OutputStream oOutputStream) throws IOException {

		// Cumulative Byte Count
		int iTotalBytes = 0;
		// Byte that represent 10% of the file
		long lOnePercent = lContentLength/100;
		// Percent of the completed download
		int iFilePercent = 0 ;

		int iBytesRead = -1;
		byte[] abBuffer = new byte[BUFFER_SIZE];
		int nZeroes = MAX_NUM_ZEORES_DURING_READ;
		while ((iBytesRead = oInputStream.read(abBuffer)) != -1) {

			if (iBytesRead <= 0) {
				m_oLogger.debug("ExecuteDownloadFile. Read 0 bytes from stream. Counter: " + nZeroes);
				nZeroes--;
			} else {
				nZeroes = MAX_NUM_ZEORES_DURING_READ;
			}
			if (nZeroes <=0 ) break;

			//logger.debug("ExecuteDownloadFile. Read " + iBytesRead +  " bytes from stream");

			oOutputStream.write(abBuffer, 0, iBytesRead);

			// Sum bytes
			iTotalBytes += iBytesRead;

			// Overcome a 1% limit?
			if(oProcessWorkspace!=null && lContentLength>BUFFER_SIZE && iTotalBytes>=lOnePercent && iFilePercent<=100) {
				// Increase the file
				iFilePercent += 1;
				if (iFilePercent>100) iFilePercent = 100;
				// Reset the count
				iTotalBytes = 0;
				// Update the progress
				if (nZeroes == MAX_NUM_ZEORES_DURING_READ) UpdateProcessProgress(oProcessWorkspace, iFilePercent);
			}
		}

		oOutputStream.close();
		oInputStream.close();
	}
    
    /**
     * 
     * @param oProcessWorkspace
     * @param iProgress
     */
    protected void UpdateProcessProgress(ProcessWorkspace oProcessWorkspace, int iProgress) {
    	
    	if (oProcessWorkspace == null) return;
    	oProcessWorkspace.setProgressPerc(iProgress);
    	//notify all subscribers
    	for (ProcessWorkspaceUpdateSubscriber oSubscriber : m_aoSubscribers) {
			oSubscriber.notify(oProcessWorkspace);
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

	protected String downloadViaHttp(String sFileURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace)
			throws IOException {
				//TODO move this code into superclass, see DhUSDownloadFile
				String sReturnFilePath = "";
			
				// TODO: Here we are assuming dhus authentication. But we have to find a general solution
				m_oLogger.debug("DownloadFile.ExecuteDownloadFile: sDownloadUser = " + sDownloadUser);
				if (sDownloadUser!=null) {
					Authenticator.setDefault(new Authenticator() {
						protected PasswordAuthentication getPasswordAuthentication() {
							try{
								return new PasswordAuthentication(sDownloadUser, sDownloadPassword.toCharArray());
							} catch (Exception oEx){
								m_oLogger.error("DownloadFile.ExecuteDownloadFile: exception setting auth " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
							}
							return null;
						}
					});        	
				}
			
				m_oLogger.debug("DownloadFile.ExecuteDownloadFile: FileUrl = " + sFileURL);
			
				URL oUrl = new URL(sFileURL);
				HttpURLConnection oConnection = (HttpURLConnection) oUrl.openConnection();
				oConnection.setRequestMethod("GET");
				oConnection.setRequestProperty("Accept", "*/*");
			
				int responseCode = oConnection.getResponseCode();
			
				// always check HTTP response code first
				if (responseCode == HttpURLConnection.HTTP_OK) {
			
					m_oLogger.debug("DownloadFile.ExecuteDownloadFile: Connected");
			
					String sFileName = "";
					String sDisposition = oConnection.getHeaderField("Content-Disposition");
					String sContentType = oConnection.getContentType();
					long  lContentLength = oConnection.getContentLength();
			
					m_oLogger.debug("ExecuteDownloadFile. ContentLenght: " + lContentLength);
			
					if (sDisposition != null) {
						// extracts file name from header field
						int index = sDisposition.indexOf("filename=");
						if (index > 0) {
							sFileName = sDisposition.substring(index + 10, sDisposition.length() - 1);
						}
					} else {
						// extracts file name from URL
						sFileName = sFileURL.substring(sFileURL.lastIndexOf("/") + 1,sFileURL.length());
					}
			
					m_oLogger.debug("Content-Type = " + sContentType);
					m_oLogger.debug("Content-Disposition = " + sDisposition);
					m_oLogger.debug("Content-Length = " + lContentLength);
					m_oLogger.debug("fileName = " + sFileName);
			
					// opens input stream from the HTTP connection
					InputStream oInputStream = oConnection.getInputStream();
					String saveFilePath= sSaveDirOnServer + "/" + sFileName;
			
					m_oLogger.debug("DownloadFile.ExecuteDownloadFile: Create Save File Path = " + saveFilePath);
			
					File oTargetFile = new File(saveFilePath);
					File oTargetDir = oTargetFile.getParentFile();
					oTargetDir.mkdirs();
			
					// opens an output stream to save into file
					FileOutputStream oOutputStream = new FileOutputStream(saveFilePath);
			
					copyStream(oProcessWorkspace, lContentLength, oInputStream, oOutputStream);
			
					sReturnFilePath = saveFilePath;
			
					m_oLogger.debug("File downloaded " + sReturnFilePath);
				} else {
					m_oLogger.debug("No file to download. Server replied HTTP code: " + responseCode);
					m_iLastError = responseCode;
				}
				oConnection.disconnect();
				return  sReturnFilePath;
			}

}
