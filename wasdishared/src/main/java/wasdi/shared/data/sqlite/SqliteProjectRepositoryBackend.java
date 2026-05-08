package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import wasdi.shared.business.Project;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.interfaces.IProjectRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for project repository.
 */
public class SqliteProjectRepositoryBackend extends SqliteRepository implements IProjectRepositoryBackend {

	public SqliteProjectRepositoryBackend() {
		m_sThisCollection = "projects";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insertProject(Project oProject) {
		try {
			insert(oProject.getProjectId(), oProject);
			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.insertProject: exception ", oEx);
		}
		return false;
	}

	@Override
	public boolean updateProject(Project oProject) {
		try {
			updateById(oProject.getProjectId(), oProject);
			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.updateProject: exception ", oEx);
		}
		return false;
	}

	@Override
	public Project getProjectById(String sProjectId) {
		try {
			return findById(sProjectId, Project.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.getProjectById: exception ", oEx);
		}
		return null;
	}

	@Override
	public List<Project> getProjectsBySubscription(String sSubscriptionId) {
		try {
			return findAllWhere("subscriptionId", sSubscriptionId, Project.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.getProjectsBySubscription: exception ", oEx);
		}
		return new ArrayList<>();
	}

	@Override
	public List<Project> getProjectsBySubscriptions(Collection<String> asSubscriptionIds) {
		try {
			if (asSubscriptionIds == null || asSubscriptionIds.isEmpty()) return new ArrayList<>();
			StringBuilder oSql = new StringBuilder("SELECT data FROM ").append(m_sThisCollection)
					.append(" WHERE json_extract(data,'$.subscriptionId') IN (");
			for (int i = 0; i < asSubscriptionIds.size(); i++) {
				if (i > 0) oSql.append(',');
				oSql.append('?');
			}
			oSql.append(')');
			return queryList(oSql.toString(), asSubscriptionIds.toArray(), Project.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.getProjectsBySubscriptions: exception ", oEx);
		}
		return new ArrayList<>();
	}

	@Override
	public boolean checkValidSubscription(String sProjectId) {
		if (Utils.isNullOrEmpty(sProjectId)) {
			return false;
		}
		
		Project oProject = this.getProjectById(sProjectId);

		return checkValidSubscription(oProject);
	}

	@Override
	public boolean checkValidSubscription(Project oProject) {
		if (oProject == null || Utils.isNullOrEmpty(oProject.getSubscriptionId())) {
			return false;
		}

		return new SubscriptionRepository().checkValidSubscriptionBySubscriptionId(oProject.getSubscriptionId());
	}

	@Override
	public Project getByName(String sName) {
		try {
			return findOneWhere("name", sName, Project.class);
		} catch (Exception oE) {
			WasdiLog.errorLog("ProjectRepository.getByName( " + sName + "): error: ", oE);
		}
		return null;
	}

	@Override
	public boolean deleteProject(String sProjectId) {
		if (Utils.isNullOrEmpty(sProjectId)) {
			return false;
		}
		try {
			return deleteById(sProjectId) > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.deleteProject: exception ", oEx);
		}
		return false;
	}

	@Override
	public int deleteBySubscription(String sSubscriptionId) {
		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			return 0;
		}
		try {
			return deleteWhere("subscriptionId", sSubscriptionId);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.deleteBySubscription: exception ", oEx);
		}
		return 0;
	}

	@Override
	public List<Project> getProjectsList() {
		try {
			return findAll(Project.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.getProjectsList: exception ", oEx);
		}
		return new ArrayList<>();
	}

	@Override
	public List<Project> findProjectsByPartialName(String sPartialName) {
		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return new ArrayList<>();
		}
		try {
			String sLike = "%" + sPartialName + "%";
			return queryList(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.projectId') LIKE ? OR json_extract(data,'$.name') LIKE ?" +
					" ORDER BY json_extract(data,'$.name') ASC",
					new Object[]{sLike, sLike}, Project.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.findProjectsByPartialName: exception ", oEx);
		}
		return new ArrayList<>();
	}
}
