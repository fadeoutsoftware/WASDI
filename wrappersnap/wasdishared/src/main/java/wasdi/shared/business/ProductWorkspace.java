package wasdi.shared.business;

/**
 * Product Workspace entity
 * Represents an association between a product and a workspace
 * Created by p.campanella on 18/11/2016.
 */
public class ProductWorkspace {
    private String productName;
    private String workspaceId;

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
