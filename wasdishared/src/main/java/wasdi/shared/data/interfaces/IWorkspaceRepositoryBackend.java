package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.Workspace;

/**
 * Backend contract for workspace repository.
 */
public interface IWorkspaceRepositoryBackend {

	boolean insertWorkspace(Workspace oWorkspace);

	boolean updateWorkspaceName(Workspace oWorkspace);

	boolean updateWorkspacePublicFlag(Workspace oWorkspace);

	boolean updateWorkspaceNodeCode(Workspace oWorkspace);

	boolean updateWorkspace(Workspace oWorkspace);

	Workspace getWorkspace(String sWorkspaceId);

	List<Workspace> getWorkspaceByUser(String sUserId);

	List<Workspace> getWorkspaceByNode(String sNodeCode);

	List<Workspace> getWorkspacesSortedByOldestUpdate(String sUserId);

	Workspace getByUserIdAndWorkspaceName(String sUserId, String sName);

	Workspace getByNameAndNode(String sName, String sNode);

	boolean deleteWorkspace(String sWorkspaceId);

	int deleteByUser(String sUserId);

	boolean isOwnedByUser(String sUserId, String sWorkspaceId);

	List<Workspace> getWorkspacesList();

	List<Workspace> findWorkspacesByPartialName(String sPartialName);

	Long getStorageUsageForUser(String sUserId);
}
