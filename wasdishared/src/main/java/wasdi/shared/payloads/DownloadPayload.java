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
	
	/**
	 * In case of AUTO provider, this is the one finally used
	 */
	private String selectedProvider;
	
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
	public String getSelectedProvider() {
		return selectedProvider;
	}
	public void setSelectedProvider(String selectedProvider) {
		this.selectedProvider = selectedProvider;
	}

}
