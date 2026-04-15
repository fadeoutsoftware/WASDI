package wasdi.shared.payloads;

import wasdi.shared.LauncherOperations;

/**
 * Payload of the FTP Upload Operation
 * 
 * @author p.campanella
 *
 */
public class FTPUploadPayload extends OperationPayload {
	
	/**
	 * Name of uploaded file
	 */
	private String file;
	
	/**
	 * Ftp server address
	 */
	private String server;
	
	/**
	 * Remote path used
	 */
	private String remotePath;
	
	/**
	 * Ftp server port
	 */
	private int port;
	
	
	public FTPUploadPayload() {
		operation = LauncherOperations.FTPUPLOAD.name();
	}
		
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}
	public String getServer() {
		return server;
	}
	public void setServer(String server) {
		this.server = server;
	}
	public String getRemotePath() {
		return remotePath;
	}
	public void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}
}
