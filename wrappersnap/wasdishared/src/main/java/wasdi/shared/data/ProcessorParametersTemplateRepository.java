package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import wasdi.shared.business.processors.ProcessorParametersTemplate;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * ProcessorParametersTemplate repository
 * @author PetruPetrescu
 *
 */
public class ProcessorParametersTemplateRepository extends MongoRepository {

	public ProcessorParametersTemplateRepository() {
		m_sThisCollection = "processorParametersTemplates";
	}

	/**
	 * Get the list of ProcessorParametersTemplates by user and processor.
	 * 
	 * @param sUserId the id of the user
	 * @return the list of processorParameterTemplates
	 */
	public List<ProcessorParametersTemplate> getProcessorParametersTemplatesByUser(String sUserId) {
		final List<ProcessorParametersTemplate> aoReturnList = new ArrayList<>();

		try {

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId)).sort(new Document("name", 1));
			fillList(aoReturnList, oWSDocuments, ProcessorParametersTemplate.class);
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.getProcessorParametersTemplatesByUser :error ", oEx);
		}

		return aoReturnList;
	}

	/**
	 * Get the list of ProcessorParametersTemplates by user and processor.
	 * 
	 * @param sUserId      the id of the user
	 * @param sProcessorId the id of the processor
	 * @return the list of processorParameterTemplates
	 */
	public List<ProcessorParametersTemplate> getProcessorParametersTemplatesByUserAndProcessor(String sUserId,
			String sProcessorId) {
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

	/**
	 * Get the list of ProcessorParametersTemplates by user and processor.
	 * 
	 * @param sProcessorId the id of the processor
	 * @return the list of processorParameterTemplates
	 */
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

	/**
	 * Get a ProcessorParametersTemplate by templateId.
	 * 
	 * @param sTemplateId the id of the template
	 * @return Entity
	 */
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

	/**
	 * Get a ProcessorParametersTemplate by user, processor and name.
	 * 
	 * @param sUserId      the id of the user
	 * @param sProcessorId the id of the processor
	 * @param sName        the name of the parameters template
	 * @return Entity
	 */
	public ProcessorParametersTemplate getProcessorParametersTemplatesByUserAndProcessorAndName(String sUserId,
			String sProcessorId, String sName) {

		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("userId", sUserId),
					Filters.eq("processorId", sProcessorId), Filters.eq("name", sName))).first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();
				return s_oMapper.readValue(sJSON, ProcessorParametersTemplate.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.getProcessorParametersTemplatesByUserAndProcessorAndName :error ", oEx);
		}

		return null;
	}

	/**
	 * Create a new ProcessorParametersTemplate.
	 * 
	 * @param oProcessorParametersTemplate Entity
	 * @return Obj Id
	 */
	public String insertProcessorParametersTemplate(ProcessorParametersTemplate oProcessorParametersTemplate) {
		try {
			if (oProcessorParametersTemplate == null) {
				WasdiLog.debugLog(
						"ProcessorParametersTemplateRepository.InsertProcessorParametersTemplate: oProcessorParametersTemplate is null");
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

	/**
	 * Delete the ProcessorParametersTemplate by templateId.
	 * 
	 * @param sTemplateId the id of the template
	 * @return 1 if it was deleted, 0 if it did not exists
	 */
	public int deleteByTemplateId(String sTemplateId) {
		if (Utils.isNullOrEmpty(sTemplateId)) {
			return 0;
		}

		try {
			
			BasicDBObject oCriteria = new BasicDBObject();
			oCriteria.append("templateId", sTemplateId);

			return delete(oCriteria, m_sThisCollection);			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.deleteByTemplateId:error ", oEx);
			return -1;
		}
	}

	/**
	 * Update an existing ProcessorParametersTemplate.
	 * 
	 * @param oProcessorParametersTemplate Entity
	 * @return true if the record was updated, false otherwise
	 */
	public boolean updateProcessorParametersTemplate(ProcessorParametersTemplate oProcessorParametersTemplate) {
		
		try {
			BasicDBObject oCriteria = new BasicDBObject();

			oCriteria.append("templateId", oProcessorParametersTemplate.getTemplateId());

			return update(oCriteria, oProcessorParametersTemplate, m_sThisCollection);			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.updateProcessorParametersTemplate:error ", oEx);
			return false;
		}

	}

	/**
	 * Check if the indicated user is the owner of the template.
	 * @param sTemplateId the template Id
	 * @param sUserId the user Id
	 * @return true if the user is the owner of the template, false otherwise
	 */
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
	
	/**
	 * Delete the ProcessorParametersTemplates by UserId.
	 * 
	 * @param sUSerId the id of the user
	 * @return number of elements deleted
	 */
	public int deleteByUserId(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			
			BasicDBObject oCriteria = new BasicDBObject();
			oCriteria.append("userId", sUserId);

			return deleteMany(oCriteria, m_sThisCollection);			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.deleteByUserId: error ", oEx);
			return -1;
		}
	}	

}
