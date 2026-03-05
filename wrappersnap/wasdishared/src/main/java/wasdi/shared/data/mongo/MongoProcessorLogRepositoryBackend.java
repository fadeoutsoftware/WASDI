package wasdi.shared.data.mongo;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import wasdi.shared.business.processors.ProcessorLog;
import wasdi.shared.data.CounterRepository;
import wasdi.shared.data.interfaces.IProcessorLogRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for processor log repository.
 */
public class MongoProcessorLogRepositoryBackend extends MongoRepository implements IProcessorLogRepositoryBackend {

	public MongoProcessorLogRepositoryBackend() {
		m_sThisCollection = "processorlog";
		m_sRepoDb = "local";
	}

	@Override
	public String insertProcessLog(ProcessorLog oProcessLog) {
		try {
			if (null == oProcessLog) {
				WasdiLog.debugLog("ProcessorLogRepository.InsertProcessLog: oProcessorLog is null");
				return null;
			}
			CounterRepository oCounterRepo = new CounterRepository();

			oProcessLog.setRowNumber(oCounterRepo.getNextValue(oProcessLog.getProcessWorkspaceId()));

			String sJSON = s_oMapper.writeValueAsString(oProcessLog);
			Document oDocument = Document.parse(sJSON);

			getCollection(m_sThisCollection).insertOne(oDocument);
			return oDocument.getObjectId("_id").toHexString();

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorLogRepository.InsertProcessLog: ", oEx);
		}
		return "";
	}

	@Override
	public void insertProcessLogList(List<ProcessorLog> aoProcessLogs) {
		try {
			if (null == aoProcessLogs) {
				WasdiLog.debugLog("ProcessorLogRepository.InsertProcessLogList: aoProcessorLog is null");
				return;
			}

			List<Document> aoDocs = new ArrayList<>();
			for (ProcessorLog oProcessorLog : aoProcessLogs) {
				if (null != oProcessorLog) {
					String sJSON = s_oMapper.writeValueAsString(oProcessorLog);
					Document oDocument = Document.parse(sJSON);
					aoDocs.add(oDocument);
				}
			}
			getCollection(m_sThisCollection).insertMany(aoDocs);

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorLogRepository.InsertProcessLog: ", oEx);
		}
		return;
	}

	@Override
	public boolean deleteProcessorLog(String sId) {
		try {
			getCollection(m_sThisCollection).deleteOne(new Document("_id", new ObjectId(sId)));

			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorLogRepository.DeleteProcessorLog( " + sId + " )", oEx);
		}

		return false;
	}

	@Override
	public List<ProcessorLog> getLogsByProcessWorkspaceId(String sProcessWorkspaceId) {

		final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
		if (!Utils.isNullOrEmpty(sProcessWorkspaceId)) {
			try {
				FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
						.find(new Document("processWorkspaceId", sProcessWorkspaceId));
				if (oWSDocuments != null) {
					fillList(aoReturnList, oWSDocuments, ProcessorLog.class);
				}
			} catch (Exception oEx) {
				WasdiLog.errorLog("ProcessorLogRepository.GetLogsByProcessWorkspaceId( " + sProcessWorkspaceId + " )", oEx);
			}
		}
		return aoReturnList;
	}

	@Override
	public List<ProcessorLog> getLogsByArrayProcessWorkspaceId(List<String> asProcessWorkspaceId) {
		BasicDBObject oInQuery = new BasicDBObject("$in", asProcessWorkspaceId);
		BasicDBObject oQuery = new BasicDBObject("processWorkspaceId", oInQuery);

		final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oQuery);
			if (oWSDocuments != null) {
				fillList(aoReturnList, oWSDocuments, ProcessorLog.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorLogRepository.getLogsByArrayProcessWorkspaceId", oEx);
		}
		return aoReturnList;
	}

	@Override
	public boolean deleteLogsOlderThan(String sDate) {

		if (Utils.isNullOrEmpty(sDate)) {
			return false;
		}

		sDate = sDate + " 00:00:00";

		BasicDBObject oLessThanQuery = new BasicDBObject("$lt", sDate);
		BasicDBObject oQuery = new BasicDBObject("logDate", oLessThanQuery);

		try {
			getCollection(m_sThisCollection).deleteMany(oQuery);
			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorLogRepository.deleteLogsOlderThan", oEx);
		}
		return false;
	}

	@Override
	public boolean deleteLogsByProcessWorkspaceId(String sProcessWorkspaceId) {

		if (!Utils.isNullOrEmpty(sProcessWorkspaceId)) {

			try {
				getCollection(m_sThisCollection).deleteMany(new Document("processWorkspaceId", sProcessWorkspaceId));
				return true;
			} catch (Exception oEx) {
				WasdiLog.errorLog("ProcessorLogRepository.GetLogsByProcessWorkspaceId( " + sProcessWorkspaceId + " )", oEx);
			}
		}
		return false;
	}

	@Override
	public List<ProcessorLog> getLogRowsByText(String sLogText) {

		final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
		if (!Utils.isNullOrEmpty(sLogText)) {
			try {

				BasicDBObject oRegexQuery = new BasicDBObject();
				oRegexQuery.put("logRow", new BasicDBObject("$regex", sLogText));

				FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oRegexQuery);
				if (oWSDocuments != null) {
					fillList(aoReturnList, oWSDocuments, ProcessorLog.class);
				}
			} catch (Exception oEx) {
				WasdiLog.errorLog("ProcessorLogRepository.GetLogRowsByText( " + sLogText + " )", oEx);
			}
		}
		return aoReturnList;
	}

	@Override
	public List<ProcessorLog> getLogRowsByTextAndProcessId(String sLogText, String sProcessWorkspaceId) {

		final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
		if (!Utils.isNullOrEmpty(sLogText)) {
			try {

				BasicDBObject oRegexQuery = new BasicDBObject();
				oRegexQuery.put("logRow", new BasicDBObject("$regex", sLogText));

				List<BasicDBObject> aoFilters = new ArrayList<BasicDBObject>();
				aoFilters.add(oRegexQuery);
				aoFilters.add(new BasicDBObject("processWorkspaceId", sProcessWorkspaceId));

				BasicDBObject oAndQuery = new BasicDBObject();
				oAndQuery.put("$and", aoFilters);

				FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oAndQuery);
				if (oWSDocuments != null) {
					fillList(aoReturnList, oWSDocuments, ProcessorLog.class);
				}
			} catch (Exception oEx) {
				WasdiLog.errorLog("ProcessorLogRepository.GetLogRowsByText( " + sLogText + " )", oEx);
			}
		}
		return aoReturnList;
	}

	@Override
	public List<ProcessorLog> getLogsByWorkspaceIdInRange(String sProcessWorkspaceId, Integer iLo, Integer iUp) {

		final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();

		if (null == sProcessWorkspaceId || iLo == null || iUp == null) {
			WasdiLog.debugLog("ProcessorLogRepository.getLogsByWorkspaceIdInRange( " + sProcessWorkspaceId + ", " + iLo + ", " + iUp + " ): null argument passed");
			return aoReturnList;
		}
		if (iLo < 0 || iLo > iUp) {
			WasdiLog.debugLog("ProcessorLogRepository.getLogsByWorkspaceIdInRange: 0 <= " + iLo + " <= " + iUp + " range invalid or no logs available");
			return aoReturnList;
		}

		try {
			// MongoDB query is:
			Bson oFilter = Filters.and(
					Filters.eq("processWorkspaceId", sProcessWorkspaceId),
					Filters.and(Filters.gte("rowNumber", iLo), Filters.lte("rowNumber", iUp)));
			MongoCollection<Document> aoProcessorLogCollection = getCollection(m_sThisCollection);
			FindIterable<Document> oWSDocuments = aoProcessorLogCollection.find(oFilter);
			if (oWSDocuments != null) {
				fillList(aoReturnList, oWSDocuments, ProcessorLog.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorLogRepository.getLogsByWorkspaceIdInRange", oEx);
		}

		return aoReturnList;

	}

	@Override
	public List<ProcessorLog> getList() {

		final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();
			fillList(aoReturnList, oWSDocuments, ProcessorLog.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorLogRepository.getList", oEx);
		}

		return aoReturnList;
	}
}
