package wasdi.shared.data.mongo;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import wasdi.shared.business.processors.ProcessorParametersTemplate;
import wasdi.shared.data.interfaces.IProcessorParametersTemplateRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for processor parameters template repository.
 */
public class MongoProcessorParametersTemplateRepositoryBackend extends MongoRepository implements IProcessorParametersTemplateRepositoryBackend {

	public MongoProcessorParametersTemplateRepositoryBackend() {
		m_sThisCollection = "processorParametersTemplates";
	}

	@Override
	public List<ProcessorParametersTemplate> getProcessorParametersTemplatesByUser(String sUserId) {
		final List<ProcessorParametersTemplate> aoReturnList = new ArrayList<>();

		try {

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("userId", sUserId))
					.sort(new Document("name", 1));
			fillList(aoReturnList, oWSDocuments, ProcessorParametersTemplate.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.getProcessorParametersTemplatesByUser :error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<ProcessorParametersTemplate> getProcessorParametersTemplatesByUserAndProcessor(String sUserId, String sProcessorId) {
		final List<ProcessorParametersTemplate> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("processorId", sProcessorId)))
					.sort(new Document("name", 1));
			fillList(aoReturnList, oWSDocuments, ProcessorParametersTemplate.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.getProcessorParametersTemplatesByUserAndProcessor :error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<ProcessorParametersTemplate> getProcessorParametersTemplatesByProcessor(String sProcessorId) {
		final List<ProcessorParametersTemplate> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(Filters.eq("processorId", sProcessorId))
					.sort(new Document("name", 1));
			fillList(aoReturnList, oWSDocuments, ProcessorParametersTemplate.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.getProcessorParametersTemplatesByProcessor :error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public ProcessorParametersTemplate getProcessorParametersTemplateByTemplateId(String sTemplateId) {

		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(Filters.eq("templateId", sTemplateId)).first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();
				return s_oMapper.readValue(sJSON, ProcessorParametersTemplate.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.getProcessorParametersTemplateByTemplateId :error ", oEx);
		}

		return null;
	}

	@Override
	public ProcessorParametersTemplate getProcessorParametersTemplatesByUserAndProcessorAndName(String sUserId, String sProcessorId, String sName) {

		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("processorId", sProcessorId), Filters.eq("name", sName)))
					.first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();
				return s_oMapper.readValue(sJSON, ProcessorParametersTemplate.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.getProcessorParametersTemplatesByUserAndProcessorAndName :error ", oEx);
		}

		return null;
	}

	@Override
	public String insertProcessorParametersTemplate(ProcessorParametersTemplate oProcessorParametersTemplate) {
		try {
			if (oProcessorParametersTemplate == null) {
				WasdiLog.debugLog("ProcessorParametersTemplateRepository.InsertProcessorParametersTemplate: oProcessorParametersTemplate is null");
				return null;
			}

			String sJSON = s_oMapper.writeValueAsString(oProcessorParametersTemplate);
			Document oDocument = Document.parse(sJSON);

			getCollection(m_sThisCollection).insertOne(oDocument);
			return oDocument.getObjectId("_id").toHexString();
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.InsertProcessorParametersTemplate: ", oEx);
		}

		return "";
	}

	@Override
	public int deleteByTemplateId(String sTemplateId) {
		if (Utils.isNullOrEmpty(sTemplateId)) {
			return 0;
		}

		try {

			BasicDBObject oCriteria = new BasicDBObject();
			oCriteria.append("templateId", sTemplateId);

			return delete(oCriteria, m_sThisCollection);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.deleteByTemplateId:error ", oEx);
			return -1;
		}
	}

	@Override
	public boolean updateProcessorParametersTemplate(ProcessorParametersTemplate oProcessorParametersTemplate) {

		try {
			BasicDBObject oCriteria = new BasicDBObject();

			oCriteria.append("templateId", oProcessorParametersTemplate.getTemplateId());

			return update(oCriteria, oProcessorParametersTemplate, m_sThisCollection);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.updateProcessorParametersTemplate:error ", oEx);
			return false;
		}

	}

	@Override
	public boolean isTheOwnerOfTheTemplate(String sTemplateId, String sUserId) {
		boolean bIsTheOwner = false;

		final List<ProcessorParametersTemplate> aoReturnList = new ArrayList<>();

		try {
			BasicDBObject oCriteria = new BasicDBObject();
			oCriteria.append("templateId", sTemplateId);
			oCriteria.append("userId", sUserId);

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oCriteria);
			fillList(aoReturnList, oWSDocuments, ProcessorParametersTemplate.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.isTheOwnerOfTheTemplate :error ", oEx);
		}

		if (aoReturnList.size() > 0) {
			bIsTheOwner = true;
		}

		return bIsTheOwner;
	}

	@Override
	public int deleteByUserId(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {

			BasicDBObject oCriteria = new BasicDBObject();
			oCriteria.append("userId", sUserId);

			return deleteMany(oCriteria, m_sThisCollection);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.deleteByUserId: error ", oEx);
			return -1;
		}
	}
}
