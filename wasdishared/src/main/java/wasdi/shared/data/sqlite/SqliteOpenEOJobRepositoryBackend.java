package wasdi.shared.data.sqlite;

import java.util.List;

import wasdi.shared.business.OpenEOJob;
import wasdi.shared.data.interfaces.IOpenEOJobRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class SqliteOpenEOJobRepositoryBackend extends SqliteRepository implements IOpenEOJobRepositoryBackend {

	public SqliteOpenEOJobRepositoryBackend() {
		m_sThisCollection = "openeojobs";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public String insertOpenEOJob(OpenEOJob oOpenEOJob) {
		try {
			insert(m_sThisCollection, oOpenEOJob.getJobId(), oOpenEOJob);
			return oOpenEOJob.getJobId();
		} catch (Exception oEx) {
			WasdiLog.errorLog("OpenEOJobRepository.insertOpenEOJob: error ", oEx);
		}
		return null;
	}

	@Override
	public int deleteOpenEOJob(String sJobId) {
		if (Utils.isNullOrEmpty(sJobId)) {
			return 0;
		}

		try {
			int iCount = (int) countWhere(m_sThisCollection, "jobId", sJobId);
			deleteWhere(m_sThisCollection, "jobId", sJobId);
			return iCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("OpenEOJobRepository.deleteOpenEOJob: error ", oEx);
			return -1;
		}
	}

	@Override
	public int deleteOpenEOJobsByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			int iCount = (int) countWhere(m_sThisCollection, "userId", sUserId);
			deleteWhere(m_sThisCollection, "userId", sUserId);
			return iCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("OpenEOJobRepository.deleteOpenEOJobsByUser: error ", oEx);
			return -1;
		}
	}

	@Override
	public int deleteOpenEOJobsByWorkspace(String sWorkspaceId) {
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			return 0;
		}

		try {
			int iCount = (int) countWhere(m_sThisCollection, "workspaceId", sWorkspaceId);
			deleteWhere(m_sThisCollection, "workspaceId", sWorkspaceId);
			return iCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("OpenEOJobRepository.deleteOpenEOJobsByWorkspace: error ", oEx);
			return -1;
		}
	}

	@Override
	public boolean updateOpenEOJob(OpenEOJob oOpenEOJob) {
		try {
			return updateWhere(m_sThisCollection, "jobId", oOpenEOJob.getJobId(), oOpenEOJob);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OpenEOJobRepository.updateOpenEOJob: error ", oEx);
			return false;
		}
	}

	@Override
	public OpenEOJob getOpenEOJob(String sJobId) {
		try {
			return findOneWhere(m_sThisCollection, "jobId", sJobId, OpenEOJob.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OpenEOJobRepository.getOpenEOJob: error ", oEx);
		}

		return null;
	}

	@Override
	public List<OpenEOJob> getOpenEOJobsByUser(String sUserId) {
		try {
			return findAllWhere(m_sThisCollection, "userId", sUserId, OpenEOJob.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OpenEOJobRepository.getOpenEOJobsByUser: error ", oEx);
		}

		return new java.util.ArrayList<>();
	}
}
