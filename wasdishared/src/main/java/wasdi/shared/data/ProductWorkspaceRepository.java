package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IProductWorkspaceRepositoryBackend;

/**
 * Created by p.campanella on 18/11/2016.
 */
public class ProductWorkspaceRepository {

    private final IProductWorkspaceRepositoryBackend m_oBackend;
	
	public ProductWorkspaceRepository() {
        m_oBackend = createBackend();
    }

    private IProductWorkspaceRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createProductWorkspaceRepository();
	}
	
	/**
	 * Insert a new product Workspace
	 * @param oProductWorkspace Entity
	 * @return
	 */
    public boolean insertProductWorkspace(ProductWorkspace oProductWorkspace) {
        return m_oBackend.insertProductWorkspace(oProductWorkspace);
    }
    
    /**
     * Get the list of products in a Workspace
     * @param sWorkspaceId
     * @return
     */
    public List<ProductWorkspace> getProductsByWorkspace(String sWorkspaceId) {
        return m_oBackend.getProductsByWorkspace(sWorkspaceId);
    }
    
    /**
     * Get all the workpsaces where ProductId is present
     * @param sProductId id of the product
     * @return List of string with the workspaceId where product is present
     */
    public List<String> getWorkspaces(String sProductId) {  
        return m_oBackend.getWorkspaces(sProductId);
    }
    
    /**
     * Check if a product workspace exists by the two linked id
     * @param sProductId
     * @param sWorkspaceId
     * @return
     */
    public boolean existsProductWorkspace(String sProductId, String sWorkspaceId) {
        return m_oBackend.existsProductWorkspace(sProductId, sWorkspaceId);
    }
    
    /**
     * Get a Product Workpsace from product id an workspace id
     * @param sProductId
     * @param sWorkspaceId
     * @return
     */
    public ProductWorkspace getProductWorkspace(String sProductId, String sWorkspaceId) {
        return m_oBackend.getProductWorkspace(sProductId, sWorkspaceId);
    }
    
    /**
     * Delete all product workspace of the workpsaceId
     * @param sWorkspaceId
     * @return
     */
    public int deleteByWorkspaceId(String sWorkspaceId) {
        return m_oBackend.deleteByWorkspaceId(sWorkspaceId);
    }
    
    /**
     * Delete a specific product workspace
     * @param sProductName
     * @param sWorkspaceId
     * @return
     */
    public int deleteByProductNameWorkspace(String sProductName, String sWorkspaceId) {
        return m_oBackend.deleteByProductNameWorkspace(sProductName, sWorkspaceId);
    }
    
    /**
     * Delete all product workspaces by product name
     * @param sProductName
     * @return
     */
    public int deleteByProductName(String sProductName) {
        return m_oBackend.deleteByProductName(sProductName);
    }    
    
    /**
     * Get the full list of product workspaces
     * @return
     */
    public List<ProductWorkspace> getList() {
        return m_oBackend.getList();
    }
    
    /**
     * Update a Product Workpsace
     * @param oProductWorkspace
     * @param sOldProductName
     * @return
     */
    public boolean updateProductWorkspace(ProductWorkspace oProductWorkspace, String sOldProductName) {
        return m_oBackend.updateProductWorkspace(oProductWorkspace, sOldProductName);
    }
    
    /**
     * Update a Product Workpsace
     * @param oProductWorkspace
     * @return
     */
    public boolean updateProductWorkspace(ProductWorkspace oProductWorkspace) {
        return m_oBackend.updateProductWorkspace(oProductWorkspace);
    }
    
    /**
     * Get the product workpsace list of elements having the specified path
     * @param sFilePath
     * @return
     */
    public List<ProductWorkspace> getProductWorkspaceListByPath(String sFilePath) {
        return m_oBackend.getProductWorkspaceListByPath(sFilePath);
    }
            
}

