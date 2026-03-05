package wasdi.shared.data.mongo;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBObject;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.processors.ProcessorUI;
import wasdi.shared.data.interfaces.IProcessorUIRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for processor UI repository.
 */
public class MongoProcessorUIRepositoryBackend extends MongoRepository implements IProcessorUIRepositoryBackend {

	public MongoProcessorUIRepositoryBackend() {
		m_sThisCollection = "processorsui";
	}

	@Override
	public boolean insertProcessorUI(ProcessorUI oProcUI) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oProcUI);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorUIRepository.insertProcessorUI :error ", oEx);
		}

		return false;
	}

	@Override
	public ProcessorUI getProcessorUI(String sProcessorId) {

		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("processorId", sProcessorId)).first();

			if (oWSDocument == null) {
				return null;
			}

			String sJSON = oWSDocument.toJson();

			ProcessorUI oProcessorUI = s_oMapper.readValue(sJSON, ProcessorUI.class);

			return oProcessorUI;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorUIRepository.getProcessorUI :error ", oEx);
		}

		return null;
	}

	@Override
	public boolean updateProcessorUI(ProcessorUI oProcessorUI) {
		try {
			String sJSON = s_oMapper.writeValueAsString(oProcessorUI);

			Bson oFilter = new Document("processorId", oProcessorUI.getProcessorId());
			Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));

			UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

			if (oResult.getModifiedCount() == 1) {
				return true;
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorUIRepository.updateProcessorUI :error ", oEx);
		}

		return false;
	}

	@Override
	public int deleteProcessorUIByProcessorId(String sProcessorId) {
		if (Utils.isNullOrEmpty(sProcessorId)) {
			return 0;
		}

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("processorId", sProcessorId);

		return deleteMany(oCriteria, m_sThisCollection);
	}
}
