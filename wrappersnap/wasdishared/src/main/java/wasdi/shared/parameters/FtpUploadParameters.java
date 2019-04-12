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
	public Integer getPort() {
		return m_iPort;
	}
	public void setPort(Integer iPort) {
		this.m_iPort = iPort;
	}
	public String getUsername() {
		return m_sUsername;
	}
	public void setUsername(String sUsername) {
		this.m_sUsername = sUsername;
	}
	public String getPassword() {
		return m_sPassword;
	}
	public void setPassword(String sPassword) {
		this.m_sPassword = sPassword;
	}
	public String getRemoteFileName() {
		return m_sRemoteFileName;
	}
	public void setRemoteFileName(String sFileName) {
		this.m_sRemoteFileName = sFileName;
	}
	public String getRemotePath() {
		return m_sRemotePath;
	}
	public void setRemotePath(String sPath) {
		this.m_sRemotePath = sPath;
	}
	public String getLocalFileName() {
		return m_sLocalFileName;
	}
	public void setLocalFileName(String sLocalFileName) {
		this.m_sLocalFileName = sLocalFileName;
	}
	public String getLocalPath() {
		return m_sLocalPath;
	}
	public void setLocalPath(String sLocalPath) {
		if(null == sLocalPath) {
			throw new IllegalArgumentException();
		}
		if(!Utils.isFilePathPlausible(sLocalPath)) {
			throw new IllegalArgumentException();
		}
		this.m_sLocalPath=new String( FilenameUtils.normalizeNoEndSeparator(sLocalPath, true) );
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
