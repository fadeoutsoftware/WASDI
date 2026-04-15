package wasdi.shared.data.sqlite;

import wasdi.shared.business.Counter;
import wasdi.shared.data.interfaces.ICounterRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for counter repository.
 */
public class SqliteCounterRepositoryBackend extends SqliteRepository implements ICounterRepositoryBackend {

	public SqliteCounterRepositoryBackend() {
		m_sThisCollection = "counter";
		ensureTable(m_sThisCollection);
	}

	@Override
	public int getNextValue(String sSequence) {
		Counter oCounter = getCounterBySequence(sSequence);
		if (oCounter == null) {
			oCounter = new Counter();
			oCounter.setSequence(sSequence);
			oCounter.setValue(0);
			insertCounter(oCounter);
			return 0;
		} else {
			int iNext = oCounter.getValue() + 1;
			oCounter.setValue(iNext);
			updateCounter(oCounter);
			return iNext;
		}
	}

	@Override
	public String insertCounter(Counter oCounter) {
		if (oCounter == null) {
			return "";
		}

		try {
			insert(m_sThisCollection, oCounter.getSequence(), oCounter);
			return oCounter.getSequence();
		} catch (Exception oEx) {
			WasdiLog.debugLog("SqliteCounterRepositoryBackend.insertCounter: " + oEx);
		}

		return "";
	}

	@Override
	public Counter getCounterBySequence(String sSequence) {
		if (Utils.isNullOrEmpty(sSequence)) {
			return null;
		}

		try {
			return findOneWhere(m_sThisCollection, "sequence", sSequence, Counter.class);
		} catch (Exception oEx) {
			WasdiLog.debugLog("SqliteCounterRepositoryBackend.getCounterBySequence(" + sSequence + "): " + oEx);
		}

		return null;
	}

	@Override
	public boolean updateCounter(Counter oCounter) {
		if (oCounter == null) {
			return false;
		}

		try {
			return updateWhere(m_sThisCollection, "sequence", oCounter.getSequence(), oCounter);
		} catch (Exception oEx) {
			WasdiLog.debugLog("SqliteCounterRepositoryBackend.updateCounter: " + oEx);
		}

		return false;
	}
}
