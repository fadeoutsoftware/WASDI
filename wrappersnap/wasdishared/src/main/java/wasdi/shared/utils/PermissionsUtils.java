/**
 * Created by Cristiano Nattero on 2020-04-14
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorParametersTemplateRepository;
import wasdi.shared.data.StyleRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.WorkspaceRepository;

/**
 * @author c.nattero
 *
 */
public class PermissionsUtils {
	
	private PermissionsUtils() {
		// / private constructor to hide the public implicit one 
	}

	/**
	 * @param sUserId a valid userId
	 * @param sNodeCode a valid nodeCode
	 * @return true if the user has access the node, false otherwise
	 */
	public static boolean canUserAccessNode(String sUserId, String sNodeCode) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sNodeCode)) {
				return false;
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			return oUserResourcePermissionRepository.isWorkspaceSharedWithUser(sUserId, sNodeCode);
		} catch (Exception oE) {
			Utils.debugLog("PermissionsUtils.canUserAccessWorkspace( " + sUserId + ", " + sNodeCode + " ): error: " + oE);
		}

		return false;
	}

	/**
	 * @param sUserId a valid userId
	 * @param sWorkspaceId a valid workspaceId
	 * @return true if the user owns the workspace, or if the owner shared the workspace with the user, false otherwise
	 */
	public static boolean canUserAccessWorkspace(String sUserId, String sWorkspaceId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sWorkspaceId)) {
				return false;
			}

			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			if (oWorkspaceRepository.isOwnedByUser(sUserId, sWorkspaceId)) {
				return true;
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			return oUserResourcePermissionRepository.isWorkspaceSharedWithUser(sUserId, sWorkspaceId);
		} catch (Exception oE) {
			Utils.debugLog("PermissionsUtils.canUserAccessWorkspace( " + sUserId + ", " + sWorkspaceId + " ): error: " + oE);
		}

		return false;
	}

	/**
	 * @param sUserId a valid userId
	 * @param sStyleId a valid StyleId
	 * @return true if the user owns the Style, or if the owner shared the Style with the user, false otherwise
	 */
	public static boolean canUserAccessStyle(String sUserId, String sStyleId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sStyleId)) {
				return false;
			}

			StyleRepository oStyleRepository = new StyleRepository();
			if (oStyleRepository.isStyleOwnedByUser(sUserId, sStyleId)) {
				return true;
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			return oUserResourcePermissionRepository.isStyleSharedWithUser(sUserId, sStyleId);
		} catch (Exception oE) {
			Utils.debugLog("PermissionsUtils.canUserAccessStyle( " + sUserId + ", " + sStyleId + " ): error: " + oE);
		}

		return false;
	}



	/**
	 * @param sUserId a valid user id
	 * @param sProcessObjId a valid process obj id
	 * @return true if the user can access the process, false otherwise
	 */
	public static boolean canUserAccessProcess(String sUserId, String sProcessObjId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sProcessObjId)) {
				return false;
			}

			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			if (oProcessWorkspaceRepository.isProcessOwnedByUser(sUserId, sProcessObjId)) {
				return true;
			}

			String sWorkspaceId = oProcessWorkspaceRepository.getWorkspaceByProcessObjId(sProcessObjId);
			if (!Utils.isNullOrEmpty(sWorkspaceId)) {
				return canUserAccessWorkspace(sUserId, sWorkspaceId);
			}
		} catch (Exception oE) {
			Utils.debugLog("PermissionsUtils.canUserAccessProcess( " + sUserId + ", " + sProcessObjId + " ): " + oE);
		}

		return false;
	}

	/**
	 * @param sUserId a valid userId
	 * @param sTemplateId a valid templateId
	 * @return true if the user owns the ProcessorParametersTemplate, false otherwise
	 */
	public static boolean canUserAccessProcessorParametersTemplate(String sUserId, String sTemplateId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sTemplateId)) {
				return false;
			}

			ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();

			return oProcessorParametersTemplateRepository.isTheOwnerOfTheTemplate(sTemplateId, sUserId);
		} catch (Exception oE) {
			Utils.debugLog("PermissionsUtils.canUserAccessProcessorParametersTemplate( " + sUserId + ", " + sTemplateId + " ): error: " + oE);
		}

		return false;
	}

}
