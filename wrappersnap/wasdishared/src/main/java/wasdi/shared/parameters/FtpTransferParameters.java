/**
 * Created by Cristiano Nattero on 2018-11-19
 * 
 * Fadeout software
 *
 */
package wasdi.shared.parameters;

/**
 * @author c.nattero
 *
 */
public class FtpTransferParameters extends BaseParameter {
	public static enum FtpDirection{Upload, Download};
	FtpDirection m_eDirection;
	
	//FTP server-side info
	String m_sFtpServer;
	Integer m_iPort;
	String m_sUsername;
	String m_sPassword;
	String m_sRemoteFileName;
	String m_sRemotePath;
	
	//local file info
	//MAYBE read from config?
	String m_sLocalFileName;
	String m_sLocalPath;
	
	public FtpDirection getM_eDirection() {
		return m_eDirection;
	}
	public void setM_eDirection(FtpDirection eDirection) {
		this.m_eDirection = eDirection;
	}
	public String getM_sFtpServer() {
		return m_sFtpServer;
	}
	public void setM_sFtpServer(String sFtpServer) {
		this.m_sFtpServer = sFtpServer;
	}
	public Integer getM_iPort() {
		return m_iPort;
	}
	public void setM_iPort(Integer iPort) {
		this.m_iPort = iPort;
	}
	public String getM_sUsername() {
		return m_sUsername;
	}
	public void setM_sUsername(String sUsername) {
		this.m_sUsername = sUsername;
	}
	public String getM_sPassword() {
		return m_sPassword;
	}
	public void setM_sPassword(String sPassword) {
		this.m_sPassword = sPassword;
	}
	public String getM_sRemoteFileName() {
		return m_sRemoteFileName;
	}
	public void setM_sRemoteFileName(String sFileName) {
		this.m_sRemoteFileName = sFileName;
	}
	public String getM_sRemotePath() {
		return m_sRemotePath;
	}
	public void setM_sRemotePath(String sPath) {
		this.m_sRemotePath = sPath;
	}
	public String getM_sLocalFileName() {
		return m_sLocalFileName;
	}
	public void setM_sLocalFileName(String sLocalFileName) {
		this.m_sLocalFileName = sLocalFileName;
	}
	public String getM_sLocalPath() {
		return m_sLocalPath;
	}
	public void setM_sLocalPath(String sLocalPath) {
		this.m_sLocalPath=new String(sLocalPath.replace('\\', '/'));
	}
	
	public String getFullLocalPath() {
		if(null == m_sLocalFileName || null == m_sLocalFileName) {
			return null;
		}
		
		String sRes = m_sLocalPath;
		
		if(!m_sLocalPath.endsWith("/")) {
			sRes = sRes + "/";
		}
		sRes = sRes + m_sLocalFileName;
		
		return sRes;
	}
	
}
