package wasdi.shared.data.sqlite;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.AppPayment;
import wasdi.shared.data.interfaces.IAppPaymentRepositoryBackend;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for app payment repository.
 */
public class SqliteAppPaymentRepositoryBackend extends SqliteRepository implements IAppPaymentRepositoryBackend {

	public SqliteAppPaymentRepositoryBackend() {
		m_sThisCollection = "appspayments";
		ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insertAppPayment(AppPayment oAppPayment) {
		if (oAppPayment == null) {
			return false;
		}

		try {
			return insert(m_sThisCollection, oAppPayment.getAppPaymentId(), oAppPayment);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteAppPaymentRepositoryBackend.insertAppPayment: error", oEx);
		}

		return false;
	}

	@Override
	public AppPayment getAppPaymentById(String sAppPaymentId) {
		try {
			return findOneWhere(m_sThisCollection, "appPaymentId", sAppPaymentId, AppPayment.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteAppPaymentRepositoryBackend.getAppPaymentById: error", oEx);
		}

		return null;
	}

	@Override
	public List<AppPayment> getAppPaymentByProcessorAndUser(String sProcessorId, String sUserId) {
		try {
			Map<String, Object> aoFilter = new LinkedHashMap<>();
			aoFilter.put("processorId", sProcessorId);
			aoFilter.put("userId", sUserId);
			return findAllWhere(m_sThisCollection, aoFilter, AppPayment.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteAppPaymentRepositoryBackend.getAppPaymentByProcessorAndUser: error", oEx);
		}

		return null;
	}

	@Override
	public List<AppPayment> getAppPaymentByNameAndUser(String sName, String sUserId) {
		try {
			Map<String, Object> aoFilter = new LinkedHashMap<>();
			aoFilter.put("name", sName);
			aoFilter.put("userId", sUserId);
			return findAllWhere(m_sThisCollection, aoFilter, AppPayment.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteAppPaymentRepositoryBackend.getAppPaymentByNameAndUser: error", oEx);
		}

		return null;
	}

	@Override
	public boolean updateAppPayment(AppPayment oAppPayment) {
		if (oAppPayment == null) {
			return false;
		}

		try {
			return updateWhere(m_sThisCollection, "appPaymentId", oAppPayment.getAppPaymentId(), oAppPayment);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteAppPaymentRepositoryBackend.updateAppPayment: error", oEx);
		}

		return false;
	}
}
