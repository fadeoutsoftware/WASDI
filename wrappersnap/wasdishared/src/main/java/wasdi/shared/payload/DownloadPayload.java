package wasdi.shared.payload;

public class DownloadPayload extends OperationPayload {
	
	public DownloadPayload() {
		operation = "DOWNLOAD";
	}
	
	private String provider;
	private String fileName;
	
	public String getProvider() {
		return provider;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

}
