package wasdi.shared.business;

/**
 * An S3 Volume that a user wants to link to WASDI
 */
public class S3Volume {
	
	/**
	 * Unique volume id	
	 */
	private String volumeId;
	
	/**
	 * Name of the folder mounted
	 */
	private String mountingFolderName;
	
	/**
	 * Path of the config file provided by the user
	 */
	private String configFilePath;
	
	/**
	 * User owner
	 */
	private String userId;
	
	/**
	 * Is it a read only volume or not?
	 */
	private boolean readOnly = true;

	public String getMountingFolderName() {
		return mountingFolderName;
	}

	public void setMountingFolderName(String mountingFolderName) {
		this.mountingFolderName = mountingFolderName;
	}

	public String getConfigFilePath() {
		return configFilePath;
	}

	public void setConfigFilePath(String configFilePath) {
		this.configFilePath = configFilePath;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getVolumeId() {
		return volumeId;
	}

	public void setVolumeId(String volumeId) {
		this.volumeId = volumeId;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

}
