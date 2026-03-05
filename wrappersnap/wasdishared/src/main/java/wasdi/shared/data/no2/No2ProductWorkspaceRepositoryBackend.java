package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.data.interfaces.IProductWorkspaceRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for product workspace repository.
 */
public class No2ProductWorkspaceRepositoryBackend extends No2Repository implements IProductWorkspaceRepositoryBackend {

	private static final String s_sCollectionName = "productworkpsace";

	@Override
	public boolean insertProductWorkspace(ProductWorkspace oProductWorkspace) {
		try {
			if (oProductWorkspace == null) {
				return false;
			}

			if (existsProductWorkspace(oProductWorkspace.getProductName(), oProductWorkspace.getWorkspaceId())) {
				return true;
			}

			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.insert(toDocument(oProductWorkspace));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProductWorkspaceRepositoryBackend.insertProductWorkspace: exception ", oEx);
		}

		return false;
	}

	@Override
	public List<ProductWorkspace> getProductsByWorkspace(String sWorkspaceId) {
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			WasdiLog.debugLog("No2ProductWorkspaceRepositoryBackend.getProductsByWorkspace: null workspace");
			return new ArrayList<>();
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find(where("workspaceId").eq(sWorkspaceId)) : null;
			return toList(oCursor, ProductWorkspace.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProductWorkspaceRepositoryBackend.getProductsByWorkspace: exception ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<String> getWorkspaces(String sProductId) {
		List<String> asWorkspaces = new ArrayList<>();

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find(where("productName").eq(sProductId)) : null;

			for (ProductWorkspace oProductWorkspace : toList(oCursor, ProductWorkspace.class)) {
				if (oProductWorkspace != null && !Utils.isNullOrEmpty(oProductWorkspace.getWorkspaceId())) {
					asWorkspaces.add(oProductWorkspace.getWorkspaceId());
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProductWorkspaceRepositoryBackend.getWorkspaces: exception ", oEx);
		}

		return asWorkspaces;
	}

	@Override
	public boolean existsProductWorkspace(String sProductId, String sWorkspaceId) {
		return getProductWorkspace(sProductId, sWorkspaceId) != null;
	}

	@Override
	public ProductWorkspace getProductWorkspace(String sProductId, String sWorkspaceId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("productName").eq(sProductId))) {
				ProductWorkspace oProductWorkspace = fromDocument(oDocument, ProductWorkspace.class);
				if (oProductWorkspace != null && equalsSafe(oProductWorkspace.getWorkspaceId(), sWorkspaceId)) {
					return oProductWorkspace;
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProductWorkspaceRepositoryBackend.getProductWorkspace: exception ", oEx);
		}

		return null;
	}

	@Override
	public int deleteByWorkspaceId(String sWorkspaceId) {
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			return 0;
		}

		int iDeleted = getProductsByWorkspace(sWorkspaceId).size();

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection != null) {
				oCollection.remove(where("workspaceId").eq(sWorkspaceId));
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProductWorkspaceRepositoryBackend.deleteByWorkspaceId: exception ", oEx);
		}

		return iDeleted;
	}

	@Override
	public int deleteByProductNameWorkspace(String sProductName, String sWorkspaceId) {
		if (Utils.isNullOrEmpty(sProductName) || Utils.isNullOrEmpty(sWorkspaceId)) {
			return 0;
		}

		int iDeleted = existsProductWorkspace(sProductName, sWorkspaceId) ? 1 : 0;

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection != null) {
				for (Document oDocument : oCollection.find(where("productName").eq(sProductName))) {
					ProductWorkspace oProductWorkspace = fromDocument(oDocument, ProductWorkspace.class);
					if (oProductWorkspace != null && equalsSafe(oProductWorkspace.getWorkspaceId(), sWorkspaceId)) {
						Object oId = oDocument.get("_id");
						if (oId != null) {
							oCollection.remove(where("_id").eq(oId));
						}
						break;
					}
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProductWorkspaceRepositoryBackend.deleteByProductNameWorkspace: exception ", oEx);
		}

		return iDeleted;
	}

	@Override
	public int deleteByProductName(String sProductName) {
		if (Utils.isNullOrEmpty(sProductName)) {
			return 0;
		}

		int iDeleted = (int) getList().stream().filter(o -> equalsSafe(o.getProductName(), sProductName)).count();

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection != null) {
				oCollection.remove(where("productName").eq(sProductName));
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProductWorkspaceRepositoryBackend.deleteByProductName: exception ", oEx);
		}

		return iDeleted;
	}

	@Override
	public List<ProductWorkspace> getList() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find() : null;
			return toList(oCursor, ProductWorkspace.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProductWorkspaceRepositoryBackend.getList: exception ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public boolean updateProductWorkspace(ProductWorkspace oProductWorkspace, String sOldProductName) {
		if (oProductWorkspace == null || Utils.isNullOrEmpty(sOldProductName)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.update(where("productName").eq(sOldProductName), toDocument(oProductWorkspace));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProductWorkspaceRepositoryBackend.updateProductWorkspace(old): exception ", oEx);
		}

		return false;
	}

	@Override
	public boolean updateProductWorkspace(ProductWorkspace oProductWorkspace) {
		if (oProductWorkspace == null) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			// Keep semantic intent of mongo implementation: update by productName + workspaceId
			for (Document oDocument : oCollection.find(where("productName").eq(oProductWorkspace.getProductName()))) {
				ProductWorkspace oStored = fromDocument(oDocument, ProductWorkspace.class);
				if (oStored != null && equalsSafe(oStored.getWorkspaceId(), oProductWorkspace.getWorkspaceId())) {
					Object oId = oDocument.get("_id");
					if (oId != null) {
						oCollection.update(where("_id").eq(oId), toDocument(oProductWorkspace));
						return true;
					}
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProductWorkspaceRepositoryBackend.updateProductWorkspace: exception ", oEx);
		}

		return false;
	}

	@Override
	public List<ProductWorkspace> getProductWorkspaceListByPath(String sFilePath) {
		List<ProductWorkspace> aoReturnList = new ArrayList<>();

		try {
			Pattern oRegEx = Pattern.compile(sFilePath);
			for (ProductWorkspace oProductWorkspace : getList()) {
				if (oProductWorkspace != null && oProductWorkspace.getProductName() != null
						&& oRegEx.matcher(oProductWorkspace.getProductName()).find()) {
					aoReturnList.add(oProductWorkspace);
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProductWorkspaceRepositoryBackend.getProductWorkspaceListByPath: exception ", oEx);
		}

		return aoReturnList;
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}
}
