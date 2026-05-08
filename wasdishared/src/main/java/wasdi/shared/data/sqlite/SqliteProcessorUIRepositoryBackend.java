package wasdi.shared.data.sqlite;

import wasdi.shared.business.processors.ProcessorUI;
import wasdi.shared.data.interfaces.IProcessorUIRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class SqliteProcessorUIRepositoryBackend extends SqliteRepository implements IProcessorUIRepositoryBackend {

	public SqliteProcessorUIRepositoryBackend() {
		m_sThisCollection = "processorsui";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insertProcessorUI(ProcessorUI oProcUI) {
		try {
			return insert(m_sThisCollection, oProcUI.getProcessorId(), oProcUI);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorUIRepository.insertProcessorUI :error ", oEx);
		}

		return false;
	}

	@Override
	public ProcessorUI getProcessorUI(String sProcessorId) {
		try {
			return findOneWhere(m_sThisCollection, "processorId", sProcessorId, ProcessorUI.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorUIRepository.getProcessorUI :error ", oEx);
		}

		return null;
	}

	@Override
	public boolean updateProcessorUI(ProcessorUI oProcessorUI) {
		try {
			return updateWhere(m_sThisCollection, "processorId", oProcessorUI.getProcessorId(), oProcessorUI);
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

		try {
			int iCount = (int) countWhere(m_sThisCollection, "processorId", sProcessorId);
			deleteWhere(m_sThisCollection, "processorId", sProcessorId);
			return iCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorUIRepository.deleteProcessorUIByProcessorId :error ", oEx);
		}

		return 0;
	}
}
