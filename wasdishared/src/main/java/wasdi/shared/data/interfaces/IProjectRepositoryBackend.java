package wasdi.shared.data.interfaces;

import java.util.Collection;
import java.util.List;

import wasdi.shared.business.Project;

/**
 * Backend contract for project repository.
 */
public interface IProjectRepositoryBackend {

	boolean insertProject(Project oProject);

	boolean updateProject(Project oProject);

	Project getProjectById(String sProjectId);

	List<Project> getProjectsBySubscription(String sSubscriptionId);

	List<Project> getProjectsBySubscriptions(Collection<String> asSubscriptionIds);

	boolean checkValidSubscription(String sProjectId);

	boolean checkValidSubscription(Project oProject);

	Project getByName(String sName);

	boolean deleteProject(String sProjectId);

	int deleteBySubscription(String sSubscriptionId);

	List<Project> getProjectsList();

	List<Project> findProjectsByPartialName(String sPartialName);
}
