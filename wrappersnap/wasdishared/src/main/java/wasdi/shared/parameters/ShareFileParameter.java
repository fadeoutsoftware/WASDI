package wasdi.shared.parameters;

/**
 * Parameter of the SHARE operation.
 * @author PetruPetrescu
 *
 */
public class ShareFileParameter extends BaseParameter {

	private String productName;

	private String originWorkspaceId;
	private String originWorkspaceNode;
	private String originFilePath;

	private String boundingBox;

	private String destinationWorkspaceNode;
	private String destinationFilePath;
	
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public String getOriginWorkspaceId() {
		return originWorkspaceId;
	}
	public void setOriginWorkspaceId(String originWorkspaceId) {
		this.originWorkspaceId = originWorkspaceId;
	}
	public String getOriginWorkspaceNode() {
		return originWorkspaceNode;
	}
	public void setOriginWorkspaceNode(String originWorkspaceNode) {
		this.originWorkspaceNode = originWorkspaceNode;
	}
	public String getOriginFilePath() {
		return originFilePath;
	}
	public void setOriginFilePath(String originFilePath) {
		this.originFilePath = originFilePath;
	}
	public String getBoundingBox() {
		return boundingBox;
	}
	public void setBoundingBox(String boundingBox) {
		this.boundingBox = boundingBox;
	}
	public String getDestinationWorkspaceNode() {
		return destinationWorkspaceNode;
	}
	public void setDestinationWorkspaceNode(String destinationWorkspaceNode) {
		this.destinationWorkspaceNode = destinationWorkspaceNode;
	}
	public String getDestinationFilePath() {
		return destinationFilePath;
	}
	public void setDestinationFilePath(String destinationFilePath) {
		this.destinationFilePath = destinationFilePath;
	}

//	private String provider;

}
