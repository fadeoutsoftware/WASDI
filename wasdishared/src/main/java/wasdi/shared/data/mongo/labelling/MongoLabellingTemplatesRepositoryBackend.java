package wasdi.shared.data.mongo.labelling;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.labelling.Template;
import wasdi.shared.data.interfaces.labelling.ILabellingTemplateRepositoryBackend;
import wasdi.shared.data.mongo.MongoRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class MongoLabellingTemplatesRepositoryBackend extends MongoRepository implements ILabellingTemplateRepositoryBackend{

	public MongoLabellingTemplatesRepositoryBackend() {
		m_sThisCollection = "templates";
	}

	/**
	 * Insert a new Template
	 * @param oTemplate Template object to insert
	 * @return true if successful, false otherwise
	 */
	public boolean insertTemplate(Template oTemplate) {
		try {
			if (oTemplate == null) {
				return false;
			}

			String sJSON = s_oMapper.writeValueAsString(oTemplate);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoLabellingTemplatesRepositoryBackend.insertTemplate : error ", oEx);
		}

		return false;
	}

	/**
	 * Get a Template by ID
	 * @param sTemplateId Template ID (GUID string)
	 * @return Template object or null if not found
	 */
	public Template getTemplate(String sTemplateId) {
		try {
			if (Utils.isNullOrEmpty(sTemplateId)) {
				return null;
			}

			long lCounter = getCollection(m_sThisCollection).countDocuments(new Document("id", sTemplateId));

			if (lCounter == 0) {
				return null;
			}

			Document oDocument = getCollection(m_sThisCollection).find(new Document("id", sTemplateId)).first();

			String sJSON = oDocument.toJson();

			Template oTemplate = s_oMapper.readValue(sJSON, Template.class);

			return oTemplate;
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoLabellingTemplatesRepositoryBackend.getTemplate : error ", oEx);
		}

		return null;
	}

	/**
	 * Update an existing Template
	 * @param oTemplate Template object with updated values
	 * @return true if successful, false otherwise
	 */
	public boolean updateTemplate(Template oTemplate) {
		try {
			if (oTemplate == null || Utils.isNullOrEmpty(oTemplate.getId())) {
				return false;
			}

			String sJSON = s_oMapper.writeValueAsString(oTemplate);
			Document filter = new Document("id", oTemplate.getId());
			Document update = new Document("$set", new Document(Document.parse(sJSON)));
			getCollection(m_sThisCollection).updateOne(filter, update);

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoLabellingTemplatesRepositoryBackend.updateTemplate : error ", oEx);
		}

		return false;
	}

	/**
	 * Delete a Template by ID
	 * @param sTemplateId Template ID (GUID string)
	 * @return true if successful, false otherwise
	 */
	public boolean deleteTemplate(String sTemplateId) {
		if (Utils.isNullOrEmpty(sTemplateId)) {
			return false;
		}

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("id", sTemplateId));

			if (oDeleteResult != null) {
				if (oDeleteResult.getDeletedCount() == 1) {
					return true;
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoLabellingTemplatesRepositoryBackend.deleteTemplate : error ", oEx);
		}

		return false;
	}

	/**
	 * Get all Templates created by a specific user
	 * @param sCreatorId WASDI UserId (GUID string)
	 * @return List of Templates created by the user, empty list if none found or error
	 */
	public List<Template> getTemplatesByCreator(String sCreatorId) {
		final List<Template> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sCreatorId)) {
			return aoReturnList;
		}

		try {
			FindIterable<Document> oDocuments = getCollection(m_sThisCollection).find(new Document("creator", sCreatorId)).sort(new Document("name", 1));

			fillList(aoReturnList, oDocuments, Template.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoLabellingTemplatesRepositoryBackend.getTemplatesByCreator : error ", oEx);
		}

		return aoReturnList;
	}

	/**
	 * Get all Templates
	 * @return List of all Templates, sorted by name, empty list if none found or error
	 */
	public List<Template> getAll() {
		final List<Template> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oDocuments = getCollection(m_sThisCollection).find().sort(new Document("name", 1));

			fillList(aoReturnList, oDocuments, Template.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MongoLabellingTemplatesRepositoryBackend.getAll : error ", oEx);
		}

		return aoReturnList;
	}
}