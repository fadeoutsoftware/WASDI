package wasdi.shared.data;

import java.util.Collection;
import java.util.List;

import wasdi.shared.business.Project;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IProjectRepositoryBackend;

public class ProjectRepository {

	private final IProjectRepositoryBackend m_oBackend;

	public ProjectRepository() {
		m_oBackend = createBackend();
	}

	private IProjectRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createProjectRepository();
	}

	/**
	 * Insert a new project.
	 * 
	 * @param oProject the project to be inserted
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean insertProject(Project oProject) {
		return m_oBackend.insertProject(oProject);
	}

	/**
	 * Update a project.
	 * 
	 * @param oProject the project to be updated
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean updateProject(Project oProject) {
		return m_oBackend.updateProject(oProject);
	}

	/**
	 * Get a project by its id.
	 * @param sProjectId the id of the project
	 * @return the project if found, null otherwise
	 */
	public Project getProjectById(String sProjectId) {
		return m_oBackend.getProjectById(sProjectId);
	}

	/**
	 * Get the list of projects by subscriptions.
	 * @param sSubscriptionId the subscription of the projects
	 * @return the list of projects found
	 */
	public List<Project> getProjectsBySubscription(String sSubscriptionId) {
		return m_oBackend.getProjectsBySubscription(sSubscriptionId);
	}

	/**
	 * Get the projects related to many subscriptions.
	 * @param asSubscriptionIds the list of subscriptionIds
	 * @return the list of projects associated with the subscriptions
	 */
	public List<Project> getProjectsBySubscriptions(Collection<String> asSubscriptionIds) {
		return m_oBackend.getProjectsBySubscriptions(asSubscriptionIds);
	}


	/**
	 * Check whether or not the subscription of the project is valid.
	 * @param sProjectId the id of the project
	 * @return true if the subscription of the project if valid, false otherwise
	 */
	public boolean checkValidSubscription(String sProjectId) {
		return m_oBackend.checkValidSubscription(sProjectId);
	}

	/**
	 * Check whether or not the subscription of the project is valid.
	 * @param oProject the project
	 * @return true if the subscription of the project if valid, false otherwise
	 */
	public boolean checkValidSubscription(Project oProject) {
		return m_oBackend.checkValidSubscription(oProject);
	}

	/**
	 * Get a project by its name.
	 * @param sName the name of the project
	 * @return the project if found, null otherwise
	 */
	public Project getByName(String sName) {
		return m_oBackend.getByName(sName);
	}

	/**
	 * Delete the project.
	 * @param sProjectId the id of the project
	 * @return true if the deletion was successful, false otherwise
	 */
	public boolean deleteProject(String sProjectId) {
		return m_oBackend.deleteProject(sProjectId);
	}

	/**
	 * Delete all projects belonging to a specific subscription.
	 * @param sSubscriptionId the subscription of the project
	 * @return the number of projects deleted, 0 if none
	 */
	public int deleteBySubscription(String sSubscriptionId) {
		return m_oBackend.deleteBySubscription(sSubscriptionId);
	}

	/**
	 * Get the list of projects.
	 * @return the list of projects
	 */
	public List<Project> getProjectsList() {
		return m_oBackend.getProjectsList();
	}

	/**
	 * Find projects by partial name or by partial id
	 * @param sPartialName the partial name or partial id
	 * @return the list of projects that partially match the name or id
	 */
	public List<Project> findProjectsByPartialName(String sPartialName) {
		return m_oBackend.findProjectsByPartialName(sPartialName);
	}

}

