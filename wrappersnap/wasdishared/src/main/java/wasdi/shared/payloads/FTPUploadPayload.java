package wasdi.shared.payloads;

/**
 * Payload of the FTP Upload Operation
 * 
 * @author p.campanella
 *
 */
public class FTPUploadPayload extends OperationPayload {
	
	public FTPUploadPayload() {
		operation = "FTPUPLOAD";
	}
	
	private String file;
	private String server;
	private String remotePath;
	private int port;
	
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
