package wasdi.shared.viewmodels.processors;

/**
 * Image Import View Model
 * 
 * Represents the parameters of an image import operation
 * 
 * @author PetruPetrescu
 *
 */
public class ImageImportViewModel {

	/**
	 * Url of the file as obtained by the Query Result View Model
	 */
	private String fileUrl;
	/**
	 * Name of the file to import
	 */
	private String name;
	/**
	 * Code of the Data Provider that found the image or that should be used to get it
	 */
	private String provider;
	/**
	 * Target workspace
	 */
	private String workspace;
	/**
	 * WKT of the footprint
	 */
	private String bbox;
	/**
	 * Process Workspace Id of the parent process (if present) that is requesting the import
	 */
	private String parent;
	/**
	 * If this is accessible in a Volume, here we have the name
	 */
	protected String volumeName;
	/**
	 * If this is accessible in a Volume, here we have the path in the volume
	 */
	protected String volumePath;	
	
	/**
	 * Platform type of the product to import
	 */
	protected String platform;
	
	
	public String getFileUrl() {
		return fileUrl;
	}
	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getProvider() {
		return provider;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}
	public String getWorkspace() {
		return workspace;
	}
	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}
	public String getBbox() {
		return bbox;
	}
	public void setBbox(String bbox) {
		this.bbox = bbox;
	}
	public String getParent() {
		return parent;
	}
	public void setParent(String parent) {
		this.parent = parent;
	}
	public String getVolumeName() {
		return volumeName;
	}
	public void setVolumeName(String volumeName) {
		this.volumeName = volumeName;
	}
	public String getVolumePath() {
		return volumePath;
	}
	public void setVolumePath(String volumePath) {
		this.volumePath = volumePath;
	}
	public String getPlatform() {
		return platform;
	}
	public void setPlatformType(String platform) {
		this.platform = platform;
	}

}
