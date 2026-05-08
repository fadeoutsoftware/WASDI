package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.data.interfaces.IProductWorkspaceRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for product workspace repository.
 */
public class SqliteProductWorkspaceRepositoryBackend extends SqliteRepository implements IProductWorkspaceRepositoryBackend {

	public SqliteProductWorkspaceRepositoryBackend() {
		m_sThisCollection = "productworkpsace";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insertProductWorkspace(ProductWorkspace oProductWorkspace) {

		try {
			// check if product exists
			boolean bExists = existsProductWorkspace(oProductWorkspace.getProductName(), oProductWorkspace.getWorkspaceId());
			if (bExists) {
				return true;
			}

			// Use productName+workspaceId as a composite key stored as JSON
			String sKey = oProductWorkspace.getProductName() + "_" + oProductWorkspace.getWorkspaceId();
			insert(sKey, oProductWorkspace);
			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.insertProductWorkspace: exception ", oEx);
		}

		return false;
	}

	@Override
	public List<ProductWorkspace> getProductsByWorkspace(String sWorkspaceId) {
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			WasdiLog.debugLog("ProductWorkspaceRepository.GetProductsByWorkspace( " + sWorkspaceId + " ): null workspace");
			return new ArrayList<>();
		}
		try {
			return findAllWhere("workspaceId", sWorkspaceId, ProductWorkspace.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.getProductsByWorkspace: exception ", oEx);
		}
		return new ArrayList<>();
	}

	@Override
	public List<String> getWorkspaces(String sProductId) {
		List<String> asWorkspaces = new ArrayList<>();
		try {
			List<ProductWorkspace> aoProductWorkspaces = findAllWhere("productName", sProductId, ProductWorkspace.class);
			
			for (ProductWorkspace oProductWorkspace : aoProductWorkspaces) {
				asWorkspaces.add(oProductWorkspace.getWorkspaceId());
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.getWorkspaces: exception ", oEx);
		}
		return asWorkspaces;
	}

	@Override
	public boolean existsProductWorkspace(String sProductId, String sWorkspaceId) {
		try {
			Map<String, Object> oFilter = new HashMap<>();
			oFilter.put("productName", sProductId);
			oFilter.put("workspaceId", sWorkspaceId);
			return countWhere(oFilter) > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.existsProductWorkspace: exception ", oEx);
		}
		return false;
	}

	@Override
	public ProductWorkspace getProductWorkspace(String sProductId, String sWorkspaceId) {
		try {
			return queryOne(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.productName') = ?" +
					" AND json_extract(data,'$.workspaceId') = ?",
					new Object[]{sProductId, sWorkspaceId}, ProductWorkspace.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.getProductWorkspace: exception ", oEx);
		}
		return null;
	}

	@Override
	public int deleteByWorkspaceId(String sWorkspaceId) {
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			return 0;
		}
		try {
			return deleteWhere("workspaceId", sWorkspaceId);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.deleteByWorkspaceId: exception ", oEx);
		}
		return 0;
	}

	@Override
	public int deleteByProductNameWorkspace(String sProductName, String sWorkspaceId) {
		if (Utils.isNullOrEmpty(sProductName) || Utils.isNullOrEmpty(sWorkspaceId)) {
			return 0;
		}
		try {
			return execute(
					"DELETE FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.productName') = ? AND json_extract(data,'$.workspaceId') = ?",
					new Object[]{sProductName, sWorkspaceId});
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.deleteByProductNameWorkspace: exception ", oEx);
		}
		return 0;
	}

	@Override
	public int deleteByProductName(String sProductName) {
		if (Utils.isNullOrEmpty(sProductName)) {
			return 0;
		}
		try {
			return deleteWhere("productName", sProductName);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.deleteByProductName: exception ", oEx);
		}
		return 0;
	}

	@Override
	public List<ProductWorkspace> getList() {
		try {
			return findAll(ProductWorkspace.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.getList: exception ", oEx);
		}
		return new ArrayList<>();
	}

	@Override
	public boolean updateProductWorkspace(ProductWorkspace oProductWorkspace, String sOldProductName) {
		try {
			// Find the record with the old product name in the same workspace
			ProductWorkspace oExisting = getProductWorkspace(sOldProductName, oProductWorkspace.getWorkspaceId());
			if (oExisting == null) return false;
			String sKey = sOldProductName + "_" + oProductWorkspace.getWorkspaceId();
			updateById(sKey, oProductWorkspace);
			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.updateProductWorkspace: exception ", oEx);
		}
		return false;
	}

	@Override
	public boolean updateProductWorkspace(ProductWorkspace oProductWorkspace) {
		try {
			String sKey = oProductWorkspace.getProductName() + "_" + oProductWorkspace.getWorkspaceId();
			updateById(sKey, oProductWorkspace);
			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.updateProductWorkspace: exception ", oEx);
		}
		return false;
	}

	@Override
	public List<ProductWorkspace> getProductWorkspaceListByPath(String sFilePath) {
		try {
			return queryList(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.productName') LIKE ?",
					new Object[]{"%" + sFilePath + "%"}, ProductWorkspace.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.getProductWorkspaceListByPath: exception ", oEx);
		}
		return new ArrayList<>();
	}
}

