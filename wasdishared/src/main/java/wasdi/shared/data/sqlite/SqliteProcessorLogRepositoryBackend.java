package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import wasdi.shared.business.processors.ProcessorLog;
import wasdi.shared.data.CounterRepository;
import wasdi.shared.data.interfaces.IProcessorLogRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class SqliteProcessorLogRepositoryBackend extends SqliteRepository implements IProcessorLogRepositoryBackend {

	public SqliteProcessorLogRepositoryBackend() {
		m_sThisCollection = "processorlog";
		this.ensureTable(m_sThisCollection);
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

			String sId = oProcessLog.getProcessWorkspaceId() + "_" + oProcessLog.getRowNumber();
			insert(m_sThisCollection, sId, oProcessLog);
			return sId;

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

			for (ProcessorLog oProcessorLog : aoProcessLogs) {
				if (null != oProcessorLog) {
					String sId = oProcessorLog.getProcessWorkspaceId() + "_" + oProcessorLog.getRowNumber();
					insert(m_sThisCollection, sId, oProcessorLog);
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorLogRepository.InsertProcessLogList: ", oEx);
		}
	}

	@Override
	public boolean deleteProcessorLog(String sId) {
		try {
			return deleteById(m_sThisCollection, sId);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorLogRepository.DeleteProcessorLog( " + sId + " )", oEx);
		}

		return false;
	}

	@Override
	public List<ProcessorLog> getLogsByProcessWorkspaceId(String sProcessWorkspaceId) {
		final ArrayList<ProcessorLog> aoReturnList = new ArrayList<>();
		if (!Utils.isNullOrEmpty(sProcessWorkspaceId)) {
			try {
				return findAllWhere(m_sThisCollection, "processWorkspaceId", sProcessWorkspaceId, ProcessorLog.class);
			} catch (Exception oEx) {
				WasdiLog.errorLog("ProcessorLogRepository.GetLogsByProcessWorkspaceId( " + sProcessWorkspaceId + " )", oEx);
			}
		}
		return aoReturnList;
	}

	@Override
	public List<ProcessorLog> getLogsByArrayProcessWorkspaceId(List<String> asProcessWorkspaceId) {
		final ArrayList<ProcessorLog> aoReturnList = new ArrayList<>();

		if (asProcessWorkspaceId == null || asProcessWorkspaceId.isEmpty()) {
			return aoReturnList;
		}

		try {
			StringBuilder oSb = new StringBuilder("SELECT data FROM " + m_sThisCollection
					+ " WHERE json_extract(data,'$.processWorkspaceId') IN (");
			List<Object> aoParams = new ArrayList<>();
			for (String sId : asProcessWorkspaceId) {
				oSb.append("?,");
				aoParams.add(sId);
			}
			oSb.setCharAt(oSb.length() - 1, ')');

			return queryList(oSb.toString(), aoParams, ProcessorLog.class);
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

		try {
			String sQuery = "DELETE FROM " + m_sThisCollection
					+ " WHERE json_extract(data,'$.logDate') < ?";
			execute(sQuery, Arrays.asList(sDate));
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
				deleteWhere(m_sThisCollection, "processWorkspaceId", sProcessWorkspaceId);
				return true;
			} catch (Exception oEx) {
				WasdiLog.errorLog("ProcessorLogRepository.GetLogsByProcessWorkspaceId( " + sProcessWorkspaceId + " )", oEx);
			}
		}
		return false;
	}

	@Override
	public List<ProcessorLog> getLogRowsByText(String sLogText) {
		final ArrayList<ProcessorLog> aoReturnList = new ArrayList<>();
		if (!Utils.isNullOrEmpty(sLogText)) {
			try {
				String sQuery = "SELECT data FROM " + m_sThisCollection
						+ " WHERE json_extract(data,'$.logRow') LIKE ?";
				return queryList(sQuery, Arrays.asList("%" + sLogText + "%"), ProcessorLog.class);
			} catch (Exception oEx) {
				WasdiLog.errorLog("ProcessorLogRepository.GetLogRowsByText( " + sLogText + " )", oEx);
			}
		}
		return aoReturnList;
	}

	@Override
	public List<ProcessorLog> getLogRowsByTextAndProcessId(String sLogText, String sProcessWorkspaceId) {
		final ArrayList<ProcessorLog> aoReturnList = new ArrayList<>();
		if (!Utils.isNullOrEmpty(sLogText)) {
			try {
				String sQuery = "SELECT data FROM " + m_sThisCollection
						+ " WHERE json_extract(data,'$.logRow') LIKE ?"
						+ " AND json_extract(data,'$.processWorkspaceId') = ?";
				return queryList(sQuery, Arrays.asList("%" + sLogText + "%", sProcessWorkspaceId), ProcessorLog.class);
			} catch (Exception oEx) {
				WasdiLog.errorLog("ProcessorLogRepository.GetLogRowsByText( " + sLogText + " )", oEx);
			}
		}
		return aoReturnList;
	}

	@Override
	public List<ProcessorLog> getLogsByWorkspaceIdInRange(String sProcessWorkspaceId, Integer iLo, Integer iUp) {
		final ArrayList<ProcessorLog> aoReturnList = new ArrayList<>();

		if (null == sProcessWorkspaceId || iLo == null || iUp == null) {
			WasdiLog.debugLog("ProcessorLogRepository.getLogsByWorkspaceIdInRange( " + sProcessWorkspaceId + ", " + iLo + ", " + iUp + " ): null argument passed");
			return aoReturnList;
		}
		if (iLo < 0 || iLo > iUp) {
			WasdiLog.debugLog("ProcessorLogRepository.getLogsByWorkspaceIdInRange: 0 <= " + iLo + " <= " + iUp + " range invalid or no logs available");
			return aoReturnList;
		}

		try {
			String sQuery = "SELECT data FROM " + m_sThisCollection
					+ " WHERE json_extract(data,'$.processWorkspaceId') = ?"
					+ " AND json_extract(data,'$.rowNumber') >= ?"
					+ " AND json_extract(data,'$.rowNumber') <= ?";
			return queryList(sQuery, Arrays.asList(sProcessWorkspaceId, iLo, iUp), ProcessorLog.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorLogRepository.getLogsByWorkspaceIdInRange", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<ProcessorLog> getList() {
		try {
			return findAll(m_sThisCollection, ProcessorLog.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorLogRepository.getList", oEx);
		}

		return new ArrayList<>();
	}
}
