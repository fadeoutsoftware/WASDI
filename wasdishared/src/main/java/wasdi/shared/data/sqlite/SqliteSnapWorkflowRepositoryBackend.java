package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.data.interfaces.ISnapWorkflowRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for snap workflow repository.
 */
public class SqliteSnapWorkflowRepositoryBackend extends SqliteRepository implements ISnapWorkflowRepositoryBackend {

	public SqliteSnapWorkflowRepositoryBackend() {
		m_sThisCollection = "snapworkflows";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insertSnapWorkflow(SnapWorkflow oWorkflow) {

		try {
			if (oWorkflow == null || Utils.isNullOrEmpty(oWorkflow.getWorkflowId())) {
				return false;
			}
			return insert(oWorkflow.getWorkflowId(), oWorkflow);

		} catch (Exception oEx) {
			WasdiLog.errorLog("SnapWorkflowRepository.insertSnapWorkflow : error ", oEx);
		}

		return false;
	}

	@Override
	public SnapWorkflow getSnapWorkflow(String sWorkflowId) {

		try {
			return findOneWhere("workflowId", sWorkflowId, SnapWorkflow.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SnapWorkflowRepository.getSnapWorkflow : error ", oEx);
		}

		return null;
	}
	
	@Override
	public SnapWorkflow getByName(String sWorkflowName) {

		try {
			return findOneWhere("name", sWorkflowName, SnapWorkflow.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SnapWorkflowRepository.getByName : error ", oEx);
		}

		return null;
	}

	@Override
	public List<SnapWorkflow> getSnapWorkflowPublicAndByUser(String sUserId) {
		// migrated to set in order to avoid redundancy
		final HashSet<SnapWorkflow> aoReturnList = new HashSet<SnapWorkflow>();
		try {
			aoReturnList.addAll(queryList(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.userId') = ?" +
					" OR json_extract(data,'$.isPublic') = 1",
					new Object[]{sUserId}, SnapWorkflow.class));

		} catch (Exception oEx) {
			WasdiLog.errorLog("SnapWorkflowRepository.getSnapWorkflowPublicAndByUser : error ", oEx);
		}

		return new ArrayList<SnapWorkflow>(aoReturnList);
	}

	@Override
	public List<SnapWorkflow> getList() {

		try {
			return findAll(SnapWorkflow.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("SnapWorkflowRepository.getList : error ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<SnapWorkflow> findWorkflowByPartialName(String sPartialName) {

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return null;
		}

		try {
			String sLike = "%" + sPartialName + "%";
			return queryList(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE LOWER(json_extract(data,'$.workflowId')) LIKE LOWER(?)" +
					" OR LOWER(json_extract(data,'$.name')) LIKE LOWER(?)" +
					" OR LOWER(json_extract(data,'$.description')) LIKE LOWER(?)" +
					" ORDER BY json_extract(data,'$.name') ASC",
					new Object[]{sLike, sLike, sLike},
					SnapWorkflow.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("SnapWorkflowRepository.findWorkflowByPartialName : error ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public boolean updateSnapWorkflow(SnapWorkflow oSnapWorkflow) {

		try {
			if (oSnapWorkflow == null || Utils.isNullOrEmpty(oSnapWorkflow.getWorkflowId())) {
				return false;
			}
			return updateById(oSnapWorkflow.getWorkflowId(), oSnapWorkflow);

		} catch (Exception oEx) {
			WasdiLog.errorLog("SnapWorkflowRepository.updateSnapWorkflow : error ", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteSnapWorkflow(String sWorkflowId) {

		if (Utils.isNullOrEmpty(sWorkflowId)) {
			return false;
		}

		try {
			return deleteById(sWorkflowId) > 0;

		} catch (Exception oEx) {
			WasdiLog.errorLog("SnapWorkflowRepository.deleteSnapWorkflow : error ", oEx);
		}

		return false;
	}

	@Override
	public int deleteSnapWorkflowByUser(String sUserId) {

		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			return deleteWhere("userId", sUserId);

		} catch (Exception oEx) {
			WasdiLog.errorLog("SnapWorkflowRepository.deleteSnapWorkflowByUser : error ", oEx);
		}

		return 0;
	}
}
