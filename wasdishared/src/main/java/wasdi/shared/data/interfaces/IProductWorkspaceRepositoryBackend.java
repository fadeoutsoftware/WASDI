package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.ProductWorkspace;

/**
 * Backend contract for product workspace repository.
 */
public interface IProductWorkspaceRepositoryBackend {

	boolean insertProductWorkspace(ProductWorkspace oProductWorkspace);

	List<ProductWorkspace> getProductsByWorkspace(String sWorkspaceId);

	List<String> getWorkspaces(String sProductId);

	boolean existsProductWorkspace(String sProductId, String sWorkspaceId);

	ProductWorkspace getProductWorkspace(String sProductId, String sWorkspaceId);

	int deleteByWorkspaceId(String sWorkspaceId);

	int deleteByProductNameWorkspace(String sProductName, String sWorkspaceId);

	int deleteByProductName(String sProductName);

	List<ProductWorkspace> getList();

	boolean updateProductWorkspace(ProductWorkspace oProductWorkspace, String sOldProductName);

	boolean updateProductWorkspace(ProductWorkspace oProductWorkspace);

	List<ProductWorkspace> getProductWorkspaceListByPath(String sFilePath);
}
