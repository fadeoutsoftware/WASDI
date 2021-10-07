/**
 * Created by Cristiano Nattero on 2018-11-21
 * 
 * Fadeout software
 *
 */
package wasdi.shared.viewmodels.products;

/**
 * @author c.nattero
 *
 */
public class FtpTransferViewModel {

	//FTP server
	private String server;
	private Integer port;
	private String user;
	private String password;
	private Boolean sftp = true;
	
	//file to upload
	private String fileName;
	private String destinationAbsolutePath;
	

	public String getServer() {
		return server;
	}

	public void setServer(String sServer) {
		this.server = sServer;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer iPort) {
		this.port = iPort;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String sUser) {
		this.user = sUser;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String sPassword) {
		this.password = sPassword;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String sfileName) {
		this.fileName = sfileName;
	}

	public String getDestinationAbsolutePath() {
		return destinationAbsolutePath;
	}

	public void setDestinationAbsolutePath(String sDestinationAbsolutePath) {
		this.destinationAbsolutePath = sDestinationAbsolutePath;
	}

	public Boolean getSftp() {
		return sftp;
	}

	public void setSftp(Boolean sftp) {
		this.sftp = sftp;
	}
}
