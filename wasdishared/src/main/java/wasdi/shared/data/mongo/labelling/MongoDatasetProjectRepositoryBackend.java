package wasdi.shared.data.mongo.labelling;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.labelling.DatasetProject;
import wasdi.shared.data.interfaces.labelling.IDatasetProjectRepositoryBackend;
import wasdi.shared.data.mongo.MongoRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class MongoDatasetProjectRepositoryBackend  extends MongoRepository  implements IDatasetProjectRepositoryBackend {

	public MongoDatasetProjectRepositoryBackend() {
		m_sThisCollection = "labelling_datasets";
	}

	/**
	 * Insert a new Dataset
	 * @param oDataset Templ	ate object to insert
	 * @return true if successful, false otherwise
	 */
	public boolean insertDataset(DatasetProject oDataset) {
		try {
			if (oDataset == null) {
				return false;
			}

			String sJSON = s_oMapper.writeValueAsString(oDataset);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoDatasetProjectRepositoryBackend.insertDataset : error ", oEx);
		}

		return false;
	}

	/**
	 * Get a Dataset by ID
	 * @param sDatasetId Dataset ID (GUID string)
	 * @return Dataset object or null if not found
	 */
	public DatasetProject getDataset(String sDatasetId) {
		try {
			if (Utils.isNullOrEmpty(sDatasetId)) {
				return null;
			}

			long lCounter = getCollection(m_sThisCollection).countDocuments(new Document("id", sDatasetId));

			if (lCounter == 0) {
				return null;
			}

			Document oDocument = getCollection(m_sThisCollection).find(new Document("id", sDatasetId)).first();

			String sJSON = oDocument.toJson();

			DatasetProject oDataset = s_oMapper.readValue(sJSON, DatasetProject.class);

			return oDataset;
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoDatasetProjectRepositoryBackend.getDataset : error ", oEx);
		}

		return null;
	}

	/**
	 * Update an existing Dataset
	 * @param oDataset Dataset object with updated values
	 * @return true if successful, false otherwise
	 */
	public boolean updateDataset(DatasetProject oDataset) {
		try {
			if (oDataset == null || Utils.isNullOrEmpty(oDataset.getId())) {
				return false;
			}

			String sJSON = s_oMapper.writeValueAsString(oDataset);
			Document filter = new Document("id", oDataset.getId());
			Document update = new Document("$set", new Document(Document.parse(sJSON)));
			getCollection(m_sThisCollection).updateOne(filter, update);

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoDatasetProjectRepositoryBackend.updateDataset : error ", oEx);
		}

		return false;
	}

	/**
	 * Delete a Dataset by ID
	 * @param sDatasetId Dataset ID (GUID string)
	 * @return true if successful, false otherwise
	 */
	public boolean deleteDataset(String sDatasetId) {
		if (Utils.isNullOrEmpty(sDatasetId)) {
			return false;
		}

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("id", sDatasetId));

			if (oDeleteResult != null) {
				if (oDeleteResult.getDeletedCount() == 1) {
					return true;
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoDatasetProjectRepositoryBackend.deleteDataset : error ", oEx);
		}

		return false;
	}

	/**
	 * Get all Datasets created by a specific user
	 * @param sOwnerId WASDI UserId (GUID string)
	 * @return List of Datasets created by the user, empty list if none found or error
	 */
	public List<DatasetProject> getDatasetsByOwner(String sOwnerId) {
		final List<DatasetProject> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sOwnerId)) {
			return aoReturnList;
		}

		try {
			FindIterable<Document> oDocuments = getCollection(m_sThisCollection).find(new Document("creator", sOwnerId)).sort(new Document("name", 1));

			fillList(aoReturnList, oDocuments, DatasetProject.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoDatasetProjectRepositoryBackend.getDatasetsByCreator : error ", oEx);
		}

		return aoReturnList;
	}
	
	/**
	 * Get all Datasets accessible by a specific user
	 * @param sUserId WASDI UserId (GUID string)
	 * @return List of Datasets created by the user or public
	 */
	public List<DatasetProject> getDatasetsForUser(String sUserId) {
		final List<DatasetProject> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sUserId)) {
			return aoReturnList;
		}

		try {
			
			Bson oFilter= Filters.or(new Document("creator", sUserId), new Document("isPublic", true));
			
			FindIterable<Document> oDocuments = getCollection(m_sThisCollection).find(oFilter).sort(new Document("name", 1));

			fillList(aoReturnList, oDocuments, DatasetProject.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoDatasetProjectRepositoryBackend.getDatasetsForUser : error ", oEx);
		}

		return aoReturnList;
	}	
	

	/**
	 * Get all Datasets
	 * @return List of all Datasets, sorted by name, empty list if none found or error
	 */
	public List<DatasetProject> getAll() {
		final List<DatasetProject> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oDocuments = getCollection(m_sThisCollection).find().sort(new Document("name", 1));

			fillList(aoReturnList, oDocuments, DatasetProject.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoDatasetProjectRepositoryBackend.getAll : error ", oEx);
		}

		return aoReturnList;
	}

}
