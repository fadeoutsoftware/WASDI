package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.Project;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.interfaces.IProjectRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for project repository.
 */
public class No2ProjectRepositoryBackend extends No2Repository implements IProjectRepositoryBackend {

	private static final String s_sCollectionName = "projects";

	@Override
	public boolean insertProject(Project oProject) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oProject == null) {
				return false;
			}

			oCollection.insert(toDocument(oProject));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProjectRepositoryBackend.insertProject: exception", oEx);
		}

		return false;
	}

	@Override
	public boolean updateProject(Project oProject) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oProject == null) {
				return false;
			}

			oCollection.update(where("projectId").eq(oProject.getProjectId()), toDocument(oProject));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProjectRepositoryBackend.updateProject: exception", oEx);
		}

		return false;
	}

	@Override
	public Project getProjectById(String sProjectId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("projectId").eq(sProjectId))) {
				return fromDocument(oDocument, Project.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProjectRepositoryBackend.getProjectById: exception", oEx);
		}

		return null;
	}

	@Override
	public List<Project> getProjectsBySubscription(String sSubscriptionId) {
		return filterProjects(oProject -> oProject != null && equalsSafe(oProject.getSubscriptionId(), sSubscriptionId));
	}

	@Override
	public List<Project> getProjectsBySubscriptions(Collection<String> asSubscriptionIds) {
		if (asSubscriptionIds == null || asSubscriptionIds.isEmpty()) {
			return new ArrayList<>();
		}

		return filterProjects(oProject -> oProject != null && asSubscriptionIds.contains(oProject.getSubscriptionId()));
	}

	@Override
	public boolean checkValidSubscription(String sProjectId) {
		if (Utils.isNullOrEmpty(sProjectId)) {
			return false;
		}

		Project oProject = getProjectById(sProjectId);
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
		for (Project oProject : filterProjects(oItem -> oItem != null && equalsSafe(oItem.getName(), sName))) {
			return oProject;
		}

		return null;
	}

	@Override
	public boolean deleteProject(String sProjectId) {
		if (Utils.isNullOrEmpty(sProjectId)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.remove(where("projectId").eq(sProjectId));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProjectRepositoryBackend.deleteProject: exception", oEx);
		}

		return false;
	}

	@Override
	public int deleteBySubscription(String sSubscriptionId) {
		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			return 0;
		}

		int iDeleted = getProjectsBySubscription(sSubscriptionId).size();

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection != null) {
				oCollection.remove(where("subscriptionId").eq(sSubscriptionId));
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProjectRepositoryBackend.deleteBySubscription: exception", oEx);
		}

		return iDeleted;
	}

	@Override
	public List<Project> getProjectsList() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find() : null;
			return toList(oCursor, Project.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProjectRepositoryBackend.getProjectsList: exception", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<Project> findProjectsByPartialName(String sPartialName) {
		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return new ArrayList<>();
		}

		String sLookup = sPartialName.toLowerCase();
		List<Project> aoResults = filterProjects(oProject ->
			oProject != null
				&& (containsIgnoreCase(oProject.getProjectId(), sLookup) || containsIgnoreCase(oProject.getName(), sLookup)));

		aoResults.sort(Comparator.comparing(Project::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
		return aoResults;
	}

	private interface ProjectFilter {
		boolean match(Project oProject);
	}

	private List<Project> filterProjects(ProjectFilter oFilter) {
		List<Project> aoResults = new ArrayList<>();

		for (Project oProject : getProjectsList()) {
			if (oFilter.match(oProject)) {
				aoResults.add(oProject);
			}
		}

		return aoResults;
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}

	private boolean containsIgnoreCase(String sValue, String sLookupLower) {
		if (sValue == null || sLookupLower == null) {
			return false;
		}

		return sValue.toLowerCase().contains(sLookupLower);
	}
}
