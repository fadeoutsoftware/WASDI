package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.processors.ProcessorLog;
import wasdi.shared.data.CounterRepository;
import wasdi.shared.data.interfaces.IProcessorLogRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for processor log repository.
 */
public class No2ProcessorLogRepositoryBackend extends No2Repository implements IProcessorLogRepositoryBackend {

	private static final String s_sCollectionName = "processorlog";
	private String m_sRepoDb = "local";

	@Override
	public void setRepoDb(String sRepoDb) {
		m_sRepoDb = sRepoDb;
	}

	@Override
	public String insertProcessLog(ProcessorLog oProcessLog) {
		try {
			if (oProcessLog == null) {
				WasdiLog.debugLog("No2ProcessorLogRepositoryBackend.insertProcessLog: oProcessLog is null");
				return null;
			}

			CounterRepository oCounterRepo = new CounterRepository();
			oProcessLog.setRowNumber(oCounterRepo.getNextValue(oProcessLog.getProcessWorkspaceId()));

			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return "";
			}

			Document oDocument = toDocument(oProcessLog);
			oCollection.insert(oDocument);

			Object oId = oDocument.get("_id");
			return oId != null ? oId.toString() : "";
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorLogRepositoryBackend.insertProcessLog", oEx);
		}
		return "";
	}

	@Override
	public void insertProcessLogList(List<ProcessorLog> aoProcessLogs) {
		try {
			if (aoProcessLogs == null) {
				WasdiLog.debugLog("No2ProcessorLogRepositoryBackend.insertProcessLogList: aoProcessLogs is null");
				return;
			}

			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return;
			}

			for (ProcessorLog oProcessorLog : aoProcessLogs) {
				if (oProcessorLog != null) {
					oCollection.insert(toDocument(oProcessorLog));
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorLogRepositoryBackend.insertProcessLogList", oEx);
		}
	}

	@Override
	public boolean deleteProcessorLog(String sId) {
		if (Utils.isNullOrEmpty(sId)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			for (Document oDocument : oCollection.find()) {
				Object oId = oDocument.get("_id");
				if (oId != null && sId.equals(oId.toString())) {
					oCollection.remove(where("_id").eq(oId));
					return true;
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorLogRepositoryBackend.deleteProcessorLog", oEx);
		}

		return false;
	}

	@Override
	public List<ProcessorLog> getLogsByProcessWorkspaceId(String sProcessWorkspaceId) {
		List<ProcessorLog> aoReturnList = new ArrayList<>();
		if (Utils.isNullOrEmpty(sProcessWorkspaceId)) {
			return aoReturnList;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection != null) {
				aoReturnList = toList(oCollection.find(where("processWorkspaceId").eq(sProcessWorkspaceId)), ProcessorLog.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorLogRepositoryBackend.getLogsByProcessWorkspaceId", oEx);
		}
		return aoReturnList;
	}

	@Override
	public List<ProcessorLog> getLogsByArrayProcessWorkspaceId(List<String> asProcessWorkspaceId) {
		List<ProcessorLog> aoReturnList = new ArrayList<>();
		if (asProcessWorkspaceId == null || asProcessWorkspaceId.isEmpty()) {
			return aoReturnList;
		}

		for (ProcessorLog oProcessorLog : getList()) {
			if (oProcessorLog != null && asProcessWorkspaceId.contains(oProcessorLog.getProcessWorkspaceId())) {
				aoReturnList.add(oProcessorLog);
			}
		}
		return aoReturnList;
	}

	@Override
	public boolean deleteLogsOlderThan(String sDate) {
		if (Utils.isNullOrEmpty(sDate)) {
			return false;
		}

		String sThreshold = sDate + " 00:00:00";
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			for (Document oDocument : oCollection.find()) {
				ProcessorLog oLog = fromDocument(oDocument, ProcessorLog.class);
				if (oLog != null && oLog.getLogDate() != null && oLog.getLogDate().compareTo(sThreshold) < 0) {
					Object oId = oDocument.get("_id");
					if (oId != null) {
						oCollection.remove(where("_id").eq(oId));
					}
				}
			}
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorLogRepositoryBackend.deleteLogsOlderThan", oEx);
		}
		return false;
	}

	@Override
	public boolean deleteLogsByProcessWorkspaceId(String sProcessWorkspaceId) {
		if (Utils.isNullOrEmpty(sProcessWorkspaceId)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}
			oCollection.remove(where("processWorkspaceId").eq(sProcessWorkspaceId));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorLogRepositoryBackend.deleteLogsByProcessWorkspaceId", oEx);
		}
		return false;
	}

	@Override
	public List<ProcessorLog> getLogRowsByText(String sLogText) {
		List<ProcessorLog> aoReturnList = new ArrayList<>();
		if (Utils.isNullOrEmpty(sLogText)) {
			return aoReturnList;
		}

		Pattern oPattern = Pattern.compile(sLogText);
		for (ProcessorLog oLog : getList()) {
			if (oLog != null && oLog.getLogRow() != null && oPattern.matcher(oLog.getLogRow()).find()) {
				aoReturnList.add(oLog);
			}
		}
		return aoReturnList;
	}

	@Override
	public List<ProcessorLog> getLogRowsByTextAndProcessId(String sLogText, String sProcessWorkspaceId) {
		List<ProcessorLog> aoReturnList = new ArrayList<>();
		if (Utils.isNullOrEmpty(sLogText)) {
			return aoReturnList;
		}

		Pattern oPattern = Pattern.compile(sLogText);
		for (ProcessorLog oLog : getList()) {
			if (oLog != null
					&& oLog.getLogRow() != null
					&& oPattern.matcher(oLog.getLogRow()).find()
					&& equalsSafe(oLog.getProcessWorkspaceId(), sProcessWorkspaceId)) {
				aoReturnList.add(oLog);
			}
		}
		return aoReturnList;
	}

	@Override
	public List<ProcessorLog> getLogsByWorkspaceIdInRange(String sProcessWorkspaceId, Integer iLo, Integer iUp) {
		List<ProcessorLog> aoReturnList = new ArrayList<>();
		if (sProcessWorkspaceId == null || iLo == null || iUp == null || iLo < 0 || iLo > iUp) {
			return aoReturnList;
		}

		for (ProcessorLog oLog : getLogsByProcessWorkspaceId(sProcessWorkspaceId)) {
			if (oLog != null && oLog.getRowNumber() >= iLo && oLog.getRowNumber() <= iUp) {
				aoReturnList.add(oLog);
			}
		}
		return aoReturnList;
	}

	@Override
	public List<ProcessorLog> getList() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			return toList(oCollection != null ? oCollection.find() : null, ProcessorLog.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorLogRepositoryBackend.getList", oEx);
		}
		return new ArrayList<>();
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}
}
