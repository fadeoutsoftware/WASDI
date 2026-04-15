package wasdi.shared.data.sqlite;

import wasdi.shared.business.OgcProcessesTask;
import wasdi.shared.data.interfaces.IOgcProcessesTaskRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class SqliteOgcProcessesTaskRepositoryBackend extends SqliteRepository implements IOgcProcessesTaskRepositoryBackend {

	public SqliteOgcProcessesTaskRepositoryBackend() {
		m_sThisCollection = "ogcprocessestask";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public String insertOgcProcessesTask(OgcProcessesTask oOgcProcessesTask) {
		try {
			insert(m_sThisCollection, oOgcProcessesTask.getProcessWorkspaceId(), oOgcProcessesTask);
			return oOgcProcessesTask.getProcessWorkspaceId();
		} catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesTaskRepository.insertOgcProcessesTask: error", oEx);
		}
		return null;
	}

	@Override
	public int deleteOgcProcessesTask(String sProcessWorkspaceId) {
		try {
			if (Utils.isNullOrEmpty(sProcessWorkspaceId)) {
				return 0;
			}

			int iCount = (int) countWhere(m_sThisCollection, "processWorkspaceId", sProcessWorkspaceId);
			deleteWhere(m_sThisCollection, "processWorkspaceId", sProcessWorkspaceId);
			return iCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesTaskRepository.deleteOgcProcessesTask: error", oEx);
			return -1;
		}
	}

	@Override
	public int deleteOgcProcessesTaskByUser(String sUserId) {
		try {
			if (Utils.isNullOrEmpty(sUserId)) {
				return 0;
			}

			int iCount = (int) countWhere(m_sThisCollection, "userId", sUserId);
			deleteWhere(m_sThisCollection, "userId", sUserId);
			return iCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesTaskRepository.deleteOgcProcessesTaskByUser: error", oEx);
			return -1;
		}
	}

	@Override
	public int deleteOgcProcessesTaskByWorkspace(String sWorkspaceId) {
		try {
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				return 0;
			}

			int iCount = (int) countWhere(m_sThisCollection, "workspaceId", sWorkspaceId);
			deleteWhere(m_sThisCollection, "workspaceId", sWorkspaceId);
			return iCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesTaskRepository.deleteOgcProcessesTaskByWorkspace: error", oEx);
			return -1;
		}
	}

	@Override
	public boolean updateOgcProcessesTask(OgcProcessesTask oOgcProcessesTask) {
		try {
			return updateWhere(m_sThisCollection, "processWorkspaceId", oOgcProcessesTask.getProcessWorkspaceId(), oOgcProcessesTask);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesTaskRepository.updateOgcProcessesTask: error", oEx);
			return false;
		}
	}

	@Override
	public OgcProcessesTask getOgcProcessesTask(String sProcessWorkspaceId) {
		try {
			return findOneWhere(m_sThisCollection, "processWorkspaceId", sProcessWorkspaceId, OgcProcessesTask.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesTaskRepository.getOgcProcessesTask: error", oEx);
		}

		return null;
	}
}
