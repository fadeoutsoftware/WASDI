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

	private String fileUrl;
	private String name;
	private String provider;
	private String workspace;
	private String bbox;
	private String parent;
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

}
