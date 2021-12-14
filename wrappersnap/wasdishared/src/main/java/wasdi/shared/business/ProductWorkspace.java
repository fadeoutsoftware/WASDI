package wasdi.shared.business;

/**
 * Product Workspace entity
 * Represents an association between a product and a workspace
 * Created by p.campanella on 18/11/2016.
 */
public class ProductWorkspace {
	
	/**
	 * Name of the product
	 */
    private String productName;
    /**
     * Workspace Id
     */
    private String workspaceId;
    /**
     * Bounding box
     */
    private String bbox;

    public String getBbox() {
		return bbox;
	}

	public void setBbox(String bbox) {
		this.bbox = bbox;
	}

	public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }
}
