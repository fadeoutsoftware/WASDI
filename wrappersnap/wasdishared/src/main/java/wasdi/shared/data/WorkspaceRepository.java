package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.Workspace;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IWorkspaceRepositoryBackend;

/**
 * Created by p.campanella on 25/10/2016.
 */
public class WorkspaceRepository {
	private final IWorkspaceRepositoryBackend m_oBackend;

	public WorkspaceRepository() {
		m_oBackend = createBackend();
	}

	private IWorkspaceRepositoryBackend createBackend() {
		return DataRepositoryFactoryProvider.getFactory().createWorkspaceRepository();
	}

	/**
	 * Insert a new Workspace
	 * @param oWorkspace
	 * @return
	 */
	public boolean insertWorkspace(Workspace oWorkspace) {
		return m_oBackend.insertWorkspace(oWorkspace);
	}

	/**
	 * Update the name of a workpsace
	 * @param oWorkspace
	 * @return
	 */
	public boolean updateWorkspaceName(Workspace oWorkspace) {
		return m_oBackend.updateWorkspaceName(oWorkspace);
	}

	/**
	 * Update the name of a workpsace
	 * @param oWorkspace
	 * @return
	 */
	public boolean updateWorkspacePublicFlag(Workspace oWorkspace) {
		return m_oBackend.updateWorkspacePublicFlag(oWorkspace);
	}

	/**
	 * Update the node of a workspace
	 * @param oWorkspace workspaceViewModel passed as input
	 * @return
	 */
	public boolean updateWorkspaceNodeCode(Workspace oWorkspace) {
		return m_oBackend.updateWorkspaceNodeCode(oWorkspace);
	}

	/**
	 * 
	 * @param oWorkspace
	 * @return
	 */
	public boolean updateWorkspace(Workspace oWorkspace) {
		return m_oBackend.updateWorkspace(oWorkspace);
	}

	/**
	 * Get a workspace by Id
	 * @param sWorkspaceId
	 * @return
	 */
	public Workspace getWorkspace(String sWorkspaceId) {
		return m_oBackend.getWorkspace(sWorkspaceId);
	}

	/**
	 * Get all the workspaces of a user
	 * @param sUserId
	 * @return
	 */
	public List<Workspace> getWorkspaceByUser(String sUserId) {
		return m_oBackend.getWorkspaceByUser(sUserId);
	}

	/**
	 * Get all the workspaces on a certain node
	 * @param sNodeCode the code of the node
	 * @return the list of workspaces on a node
	 */
	public List<Workspace> getWorkspaceByNode(String sNodeCode) {
		return m_oBackend.getWorkspaceByNode(sNodeCode);
	}

	/**
	 * Given a user, retrieves the list of their workspaces ordered by update date, from the less recent to the last recent one
	 * @param sUserId the id of the user
	 * @return the  list of workspaces of a user, ordered by last update date in ascending order
	 */
	public List<Workspace> getWorkspacesSortedByOldestUpdate(String sUserId) {
		return m_oBackend.getWorkspacesSortedByOldestUpdate(sUserId);
	}

	/**
	 * Find a workspace by userId and workspace name.
	 * @param sUserId the userId
	 * @param sName the name of the workspace
	 * @return the first workspace found or null
	 */
	public Workspace getByUserIdAndWorkspaceName(String sUserId, String sName) {
		return m_oBackend.getByUserIdAndWorkspaceName(sUserId, sName);
	}

	/**
	 * Find a workspace by name and node.
	 * @param sName the name of the workspace
	 * @param sNode the node
	 * @return the first workspace found or null
	 */
	public Workspace getByNameAndNode(String sName, String sNode) {
		return m_oBackend.getByNameAndNode(sName, sNode);
	}

	/**
	 * Delete a workspace by Id
	 * @param sWorkspaceId
	 * @return
	 */
	public boolean deleteWorkspace(String sWorkspaceId) {
		return m_oBackend.deleteWorkspace(sWorkspaceId);
	}

	/**
	 * Delete all the workspaces of User
	 * @param sUserId
	 * @return
	 */
	public int deleteByUser(String sUserId) {
		return m_oBackend.deleteByUser(sUserId);
	}

	/**
	 * Check if User is the owner of Workspace
	 * @param sUserId
	 * @param sWorkspaceId
	 * @return
	 */
	public boolean isOwnedByUser(String sUserId, String sWorkspaceId) {
		return m_oBackend.isOwnedByUser(sUserId, sWorkspaceId);
	}

	/**
	 * Get all the workspaces
	 * @param sUserId
	 * @return
	 */
	public List<Workspace> getWorkspacesList() {
		return m_oBackend.getWorkspacesList();
	}

	public List<Workspace> findWorkspacesByPartialName(String sPartialName) {
		return m_oBackend.findWorkspacesByPartialName(sPartialName);
	}

	/**
	 * It returns the total disk storage size (in bytes) occupied by all the workspaces of a user
	 * @param sUserId the user id
	 * @return the size (in bytes) of disk storage occupied by all the workspaces of a certain user
	 */
	public Long getStorageUsageForUser(String sUserId) {
		return m_oBackend.getStorageUsageForUser(sUserId);
	}
}

