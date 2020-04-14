/**
 * Created by Cristiano Nattero on 2020-04-14
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.data.WorkspaceSharingRepository;

/**
 * @author c.nattero
 *
 */
public class PermissionsUtils {

	/**
	 * @param sUserId a valid userId
	 * @param sWorkspaceId a valid workspaceId
	 * @return true if the user owns the workspace, or if the owner shared the workspace with the user, false otherwise
	 */
	public static boolean canUserAccessWorkspace(String sUserId, String sWorkspaceId) {
		try {
			if(Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sWorkspaceId)) {
				return false;
			}
			
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			if(oWorkspaceRepository.isOwnedByUser(sUserId, sWorkspaceId)) {
				return true;
			}
			
			WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
			if(oWorkspaceSharingRepository.isSharedWithUser(sUserId, sWorkspaceId)) {
				return true;
			}
		} catch (Exception oE) {
			Utils.debugLog("PermissionsUtils.canUserAccessWorkspace( " + sUserId + ", " + sWorkspaceId + " ): error: " + oE);
		}
		return false;
	}
}
