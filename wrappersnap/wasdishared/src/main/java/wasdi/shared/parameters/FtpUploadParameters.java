/**
 * Created by Cristiano Nattero on 2018-11-19
 * 
 * Fadeout software
 *
 */
package wasdi.shared.parameters;

import org.apache.commons.io.FilenameUtils;

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class FtpUploadParameters extends BaseParameter {
	
	//FTP server-side info
	String ftpServer;
	Integer m_iPort;
	String m_sUsername;
	String m_sPassword;
	String m_sRemoteFileName;
	String m_sRemotePath;
	
	//TODO remove logic
	//TODO rename without m?
	//TODO use just localFileName
	
	//local file info
	//MAYBE read from config?
	String m_sLocalFileName;
	String m_sLocalPath;
	
	public String getFtpServer() {
		return ftpServer;
	}
	public void setFtpServer(String sFtpServer) {
		if(null == sFtpServer) {
			throw new IllegalArgumentException();
		} else if( sFtpServer.toLowerCase().startsWith("http") ){
			throw new IllegalArgumentException();
		}
		this.ftpServer = sFtpServer;
		if(ftpServer.toLowerCase().startsWith("ftp://")) {
			//strip prefix
			ftpServer = ftpServer.substring(6,ftpServer.length());
		}
		while(ftpServer.endsWith("/")) {
			ftpServer = ftpServer.substring(0,ftpServer.length()-1);
		}
		//test if server name is correct after trimming trailing slashes
		if(!Utils.isServerNamePlausible(ftpServer)) {
			ftpServer = null;
			throw new IllegalArgumentException();
		}
	}
	public Integer getM_iPort() {
		return m_iPort;
	}
	public void setM_iPort(Integer iPort) {
		//TODO validate input
		this.m_iPort = iPort;
	}
	public String getM_sUsername() {
		return m_sUsername;
	}
	public void setM_sUsername(String sUsername) {
		//TODO validate input
		this.m_sUsername = sUsername;
	}
	public String getM_sPassword() {
		return m_sPassword;
	}
	public void setM_sPassword(String sPassword) {
		//TODO validate input
		this.m_sPassword = sPassword;
	}
	public String getM_sRemoteFileName() {
		return m_sRemoteFileName;
	}
	public void setM_sRemoteFileName(String sFileName) {
		//TODO validate input
		this.m_sRemoteFileName = sFileName;
	}
	public String getM_sRemotePath() {
		return m_sRemotePath;
	}
	public void setM_sRemotePath(String sPath) {
		//null or "" means root
		this.m_sRemotePath = sPath;
	}
	public String getM_sLocalFileName() {
		return m_sLocalFileName;
	}
	public void setM_sLocalFileName(String sLocalFileName) {
		//TODO validate input
		this.m_sLocalFileName = sLocalFileName;
	}
	public String getM_sLocalPath() {
		return m_sLocalPath;
	}
	public void setM_sLocalPath(String sLocalPath) {
		if(null == sLocalPath) {
			throw new IllegalArgumentException();
		}
		if(!Utils.isFilePathPlausible(sLocalPath)) {
			throw new IllegalArgumentException();
		}
		this.m_sLocalPath=new String( FilenameUtils.normalizeNoEndSeparator(sLocalPath, true) );
		/*
		if(!m_sLocalPath.endsWith("/")) {
			m_sLocalPath+="/";
		}
		while(m_sLocalPath.startsWith("/")) {
			m_sLocalPath = m_sLocalPath.substring(1);
		}
		*/
	}
	
	public String getFullLocalPath() {
		if(null == m_sLocalFileName || null == m_sLocalFileName) {
			return null;
		}
		
		String sRes = m_sLocalPath;
		sRes = sRes + m_sLocalFileName;
		
		return sRes;
	}
	
}
