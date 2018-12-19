package wasdi.filebuffer;

import org.apache.log4j.Logger;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.utils.Utils;

/**
 * Base Download Utility Class 
 * Created by s.adamo on 06/10/2016.
 */
public abstract class DownloadFile {

    protected final int BUFFER_SIZE = 4096;
    protected final int MAX_NUM_ZEORES_DURING_READ = 20;
    protected Logger logger;
    protected int m_iLastError = 0;
	
    protected String m_sProviderUser;
    protected String m_sProviderPassword;

    /**
     * Constructor: uses LauncerMain logger
     */
    public DownloadFile() {
		logger = LauncherMain.s_oLogger;
	}
    
    /**
     * Constructor with user defined logger
     * @param logger
     */
    public DownloadFile(Logger logger) {
		this.logger = logger;
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
    
    /**
     * 
     * @param oProcessWorkspace
     * @param iProgress
     */
    public void UpdateProcessProgress(ProcessWorkspace oProcessWorkspace, int iProgress) {
    	
    	if (oProcessWorkspace == null) return;
    	
        try {
	    	ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
	    	
	        oProcessWorkspace.setProgressPerc(iProgress);
	        //update the process
	        if (!oProcessWorkspaceRepository.UpdateProcess(oProcessWorkspace))
	            logger.debug("LauncherMain.DownloadFile: Error during process update with process Perc");
	
	        //send update process message
	        if (LauncherMain.s_oSendToRabbit != null) {
				if (!LauncherMain.s_oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace)) {
					logger.debug("LauncherMain.DownloadFile: Error sending rabbitmq message to update process list");
				}	        	
	        }
		} catch (Exception oEx) {
			logger.error("LauncherMain.DownloadFile: Exception " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
			oEx.printStackTrace();
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

}
