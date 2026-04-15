package wasdi.shared.data.mongo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.data.interfaces.IProductWorkspaceRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for product workspace repository.
 */
public class MongoProductWorkspaceRepositoryBackend extends MongoRepository implements IProductWorkspaceRepositoryBackend {

	public MongoProductWorkspaceRepositoryBackend() {
		m_sThisCollection = "productworkpsace";
	}

	@Override
	public boolean insertProductWorkspace(ProductWorkspace oProductWorkspace) {

		try {
			// check if product exists
			boolean bExists = existsProductWorkspace(oProductWorkspace.getProductName(), oProductWorkspace.getWorkspaceId());
			if (bExists) {
				return true;
			}

			String sJSON = s_oMapper.writeValueAsString(oProductWorkspace);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.insertProductWorkspace: exception ", oEx);
		}

		return false;
	}

	@Override
	public List<ProductWorkspace> getProductsByWorkspace(String sWorkspaceId) {
		final ArrayList<ProductWorkspace> aoReturnList = new ArrayList<ProductWorkspace>();
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			WasdiLog.debugLog("ProductWorkspaceRepository.GetProductsByWorkspace( " + sWorkspaceId + " ): null workspace");
			return aoReturnList;
		}
		try {

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("workspaceId", sWorkspaceId));

			fillList(aoReturnList, oWSDocuments, ProductWorkspace.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.getProductsByWorkspace: exception ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<String> getWorkspaces(String sProductId) {
		ArrayList<ProductWorkspace> aoProductWorkspaces = new ArrayList<ProductWorkspace>();
		List<String> asWorkspaces = new ArrayList<String>();

		try {
			FindIterable<Document> aoDocuments = getCollection(m_sThisCollection).find(new Document("productName", sProductId));

			fillList(aoProductWorkspaces, aoDocuments, ProductWorkspace.class);

			for (ProductWorkspace productWorkspace : aoProductWorkspaces) {
				asWorkspaces.add(productWorkspace.getWorkspaceId());
			}

			return asWorkspaces;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.getWorkspaces: exception ", oEx);
			return asWorkspaces;
		}
	}

	@Override
	public boolean existsProductWorkspace(String sProductId, String sWorkspaceId) {

		final ArrayList<ProductWorkspace> aoReturnList = new ArrayList<ProductWorkspace>();
		boolean bExists = false;
		try {

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("productName", sProductId), Filters.eq("workspaceId", sWorkspaceId)));

			fillList(aoReturnList, oWSDocuments, ProductWorkspace.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.existsProductWorkspace: exception ", oEx);
		}

		if (aoReturnList.size() > 0) {
			bExists = true;
		}

		return bExists;
	}

	@Override
	public ProductWorkspace getProductWorkspace(String sProductId, String sWorkspaceId) {

		final ArrayList<ProductWorkspace> aoReturnList = new ArrayList<ProductWorkspace>();

		try {

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("productName", sProductId), Filters.eq("workspaceId", sWorkspaceId)));

			fillList(aoReturnList, oWSDocuments, ProductWorkspace.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.getProductWorkspace: exception ", oEx);
		}

		if (aoReturnList.size() > 0) {
			return aoReturnList.get(0);
		}

		return null;
	}

	@Override
	public int deleteByWorkspaceId(String sWorkspaceId) {

		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			return 0;
		}

		try {

			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("workspaceId", sWorkspaceId));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.deleteByWorkspaceId: exception ", oEx);
		}

		return 0;
	}

	@Override
	public int deleteByProductNameWorkspace(String sProductName, String sWorkspaceId) {

		if (Utils.isNullOrEmpty(sProductName)) {
			return 0;
		}
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			return 0;
		}

		try {

			DeleteResult oDeleteResult = getCollection(m_sThisCollection)
					.deleteOne(Filters.and(Filters.eq("productName", sProductName), Filters.eq("workspaceId", sWorkspaceId)));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}

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

			DeleteResult oDeleteResult = getCollection(m_sThisCollection)
					.deleteMany(Filters.and(Filters.eq("productName", sProductName)));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.deleteByProductName: exception ", oEx);
		}

		return 0;
	}

	@Override
	public List<ProductWorkspace> getList() {
		final ArrayList<ProductWorkspace> aoReturnList = new ArrayList<ProductWorkspace>();
		try {

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

			fillList(aoReturnList, oWSDocuments, ProductWorkspace.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.getList: exception ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public boolean updateProductWorkspace(ProductWorkspace oProductWorkspace, String sOldProductName) {
		try {
			String sJSON = s_oMapper.writeValueAsString(oProductWorkspace);

			Bson oFilter = new Document("productName", sOldProductName);

			Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));

			UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

			if (oResult.getModifiedCount() == 1) {
				return true;
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.updateProductWorkspace: exception ", oEx);
		}

		return false;
	}

	@Override
	public boolean updateProductWorkspace(ProductWorkspace oProductWorkspace) {
		try {
			String sJSON = s_oMapper.writeValueAsString(oProductWorkspace);

			// Select by Product Name and Workspace Id
			Bson oFilter = Filters.and(Filters.eq("", oProductWorkspace.getProductName()), Filters.eq("workspaceId", oProductWorkspace.getWorkspaceId()));

			Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));

			UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

			if (oResult.getModifiedCount() == 1) {
				return true;
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.updateProductWorkspace: exception ", oEx);
		}

		return false;
	}

	@Override
	public List<ProductWorkspace> getProductWorkspaceListByPath(String sFilePath) {
		final ArrayList<ProductWorkspace> aoReturnList = new ArrayList<ProductWorkspace>();
		try {

			BasicDBObject oLikeQuery = new BasicDBObject();
			Pattern oRegEx = Pattern.compile(sFilePath);
			oLikeQuery.put("productName", oRegEx);

			FindIterable<Document> oDFDocuments = getCollection(m_sThisCollection).find(oLikeQuery);

			MongoCursor<Document> oCursor = oDFDocuments.iterator();

			try {
				while (oCursor.hasNext()) {
					String sJSON = oCursor.next().toJson();
					ProductWorkspace oProductWorkspace = null;
					try {
						oProductWorkspace = s_oMapper.readValue(sJSON, ProductWorkspace.class);
						aoReturnList.add(oProductWorkspace);
					} catch (IOException e) {
						WasdiLog.errorLog("ProductWorkspaceRepository.getProductWorkspaceListByPath: exception ", e);
					}
				}
			} finally {
				oCursor.close();
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProductWorkspaceRepository.getProductWorkspaceListByPath: exception ", oEx);
		}

		return aoReturnList;

	}
}
