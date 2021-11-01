package wasdi.shared.payloads;

import wasdi.shared.LauncherOperations;

/**
 * Payload of the download operation
 * 
 * @author p.campanella
 *
 */
public class DownloadPayload extends OperationPayload {
	
	/**
	 * Name of the Data Provider
	 */
	private String provider;
	
	/**
	 * File Name
	 */
	private String fileName;
	
	public DownloadPayload() {
		operation = LauncherOperations.DOWNLOAD.name();
	}
	
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
