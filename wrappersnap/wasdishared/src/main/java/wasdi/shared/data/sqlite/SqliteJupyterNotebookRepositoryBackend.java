package wasdi.shared.data.sqlite;

import wasdi.shared.business.JupyterNotebook;
import wasdi.shared.data.interfaces.IJupyterNotebookRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class SqliteJupyterNotebookRepositoryBackend extends SqliteRepository implements IJupyterNotebookRepositoryBackend {

	public SqliteJupyterNotebookRepositoryBackend() {
		m_sThisCollection = "jupyternotebook";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insertJupyterNotebook(JupyterNotebook oJupyterNotebook) {
		try {
			return insert(m_sThisCollection, oJupyterNotebook.getCode(), oJupyterNotebook);
		} catch (Exception oEx) {
			WasdiLog.errorLog("JupyterNotebookRepository.insertJupyterNotebook: error", oEx);
		}

		return false;
	}

	@Override
	public JupyterNotebook getJupyterNotebook(String sJupyterNotebookId) {
		try {
			return findOneWhere(m_sThisCollection, "code", sJupyterNotebookId, JupyterNotebook.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("JupyterNotebookRepository.getJupyterNotebook: error", oEx);
		}

		return null;
	}

	@Override
	public JupyterNotebook getJupyterNotebookByCode(String sCode) {
		try {
			return findOneWhere(m_sThisCollection, "code", sCode, JupyterNotebook.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("JupyterNotebookRepository.getJupyterNotebookByCode: error", oEx);
		}

		return null;
	}

	@Override
	public boolean updateJupyterNotebook(JupyterNotebook oJupyterNotebook) {
		try {
			return updateWhere(m_sThisCollection, "code", oJupyterNotebook.getCode(), oJupyterNotebook);
		} catch (Exception oEx) {
			WasdiLog.errorLog("JupyterNotebookRepository.updateJupyterNotebook: error", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteJupyterNotebook(String sJupyterNotebookId) {
		if (Utils.isNullOrEmpty(sJupyterNotebookId))
			return false;

		try {
			int iDeleteCount = deleteWhere(m_sThisCollection, "code", sJupyterNotebookId); 
			return iDeleteCount == 1;
		} catch (Exception oEx) {
			WasdiLog.errorLog("JupyterNotebookRepository.deleteJupyterNotebook: error", oEx);
		}

		return false;
	}

	@Override
	public int deleteJupyterNotebookByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId))
			return 0;

		try {
			int iCount = (int) countWhere(m_sThisCollection, "userId", sUserId);
			deleteWhere(m_sThisCollection, "userId", sUserId);
			return iCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("JupyterNotebookRepository.deleteJupyterNotebookByUser: error", oEx);
		}

		return 0;
	}
}
