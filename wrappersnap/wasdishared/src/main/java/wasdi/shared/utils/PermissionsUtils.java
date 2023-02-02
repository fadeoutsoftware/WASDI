/**
 * Created by Cristiano Nattero on 2020-04-14
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

import wasdi.shared.business.ImagesCollections;
import wasdi.shared.data.OrganizationRepository;
import wasdi.shared.business.Processor;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorParametersTemplateRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.StyleRepository;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.utils.log.WasdiLog;

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
			WasdiLog.debugLog("PermissionsUtils.canUserAccessWorkspace( " + sUserId + ", " + sNodeCode + " ): error: " + oE);
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
			WasdiLog.debugLog("PermissionsUtils.canUserAccessWorkspace( " + sUserId + ", " + sWorkspaceId + " ): error: " + oE);
		}

		return false;
	}

	/**
	 * @param sUserId a valid userId
	 * @param sOrganizationId a valid organizationId
	 * @return true if the user owns the organization, or if the owner shared the organization with the user, false otherwise
	 */
	public static boolean canUserAccessOrganization(String sUserId, String sOrganizationId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sOrganizationId)) {
				return false;
			}

			OrganizationRepository oOrganizationRepository = new OrganizationRepository();
			if (oOrganizationRepository.isOwnedByUser(sUserId, sOrganizationId)) {
				return true;
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			return oUserResourcePermissionRepository.isOrganizationSharedWithUser(sUserId, sOrganizationId);
		} catch (Exception oE) {
			WasdiLog.debugLog("PermissionsUtils.canUserAccessOrganization( " + sUserId + ", " + sOrganizationId + " ): error: " + oE);
		}

		return false;
	}

	/**
	 * @param sUserId a valid userId
	 * @param sSubscriptionId a valid subscriptionId
	 * @return true if the user owns the subscription, or if the owner shared the subscription with the user, false otherwise
	 */
	public static boolean canUserAccessSubscription(String sUserId, String sSubscriptionId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sSubscriptionId)) {
				return false;
			}

			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			if (oSubscriptionRepository.isOwnedByUser(sUserId, sSubscriptionId)) {
				return true;
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			return oUserResourcePermissionRepository.isSubscriptionSharedWithUser(sUserId, sSubscriptionId);
		} catch (Exception oE) {
			WasdiLog.debugLog("PermissionsUtils.canUserAccessSubscription( " + sUserId + ", " + sSubscriptionId + " ): error: " + oE);
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
			WasdiLog.debugLog("PermissionsUtils.canUserAccessStyle( " + sUserId + ", " + sStyleId + " ): error: " + oE);
		}

		return false;
	}



	/**
	 * @param sUserId a valid user id
	 * @param sProcessObjId a valid process obj id
	 * @return true if the user can access the process, false otherwise
	 */
	public static boolean canUserAccessProcessWorkspace(String sUserId, String sProcessObjId) {
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
			WasdiLog.debugLog("PermissionsUtils.canUserAccessProcess( " + sUserId + ", " + sProcessObjId + " ): " + oE);
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

			if (oProcessorParametersTemplateRepository.isTheOwnerOfTheTemplate(sTemplateId, sUserId)) {
				return true;
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			return oUserResourcePermissionRepository.isProcessorParametersTemplateSharedWithUser(sUserId, sTemplateId);
		} catch (Exception oE) {
			WasdiLog.debugLog("PermissionsUtils.canUserAccessProcessorParametersTemplate( " + sUserId + ", " + sTemplateId + " ): error: " + oE);
		}

		return false;
	}
	
	/**
	 * Check if a user can access a processor
	 * @param sUserId
	 * @param sProcessorId
	 * @return
	 */
	public static boolean canUserAccessProcessor(String sUserId, String sProcessorId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sProcessorId)) {
				return false;
			}

			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
			
			if (oProcessor == null) return false;
			
			if (oProcessor.getIsPublic()>0) return true;
			
			if (oProcessor.getUserId().equals(sUserId)) return true;

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			return oUserResourcePermissionRepository.isProcessorSharedWithUser(sUserId, sProcessorId);
		} catch (Exception oE) {
			WasdiLog.debugLog("PermissionsUtils.canUserAccessProcessor( " + sUserId + ", " + sProcessorId + " ): error: " + oE);
		}

		return false;		
	}
	
	/**
	 * Check if a user can access a specific image
	 * @param sUserId User requesting the access
	 * @param sCollection Image Collection 
	 * @param sFolder Folder name
	 * @param sImage Image name
	 * @return
	 */
	public static boolean canUserAccessImage(String sUserId, String sCollection, String sFolder, String sImage) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sCollection)) {
				return false;
			}
			
			if (!ImageResourceUtils.isValidCollection(sCollection)) {
				return false;
			}
			
			if (sCollection.equals(ImagesCollections.PROCESSORS.getFolder())) {
				ProcessorRepository oProcessorRepository = new ProcessorRepository();
				Processor oProcessor = oProcessorRepository.getProcessorByName(sFolder);
				
				if (oProcessor == null) return false;
				return canUserAccessProcessor(sUserId, oProcessor.getProcessorId());
			}
			else if (sCollection.equals(ImagesCollections.USERS.getFolder())) {
				if (sUserId.equals(sFolder)) return true;
				else return false;
			}
			else if (sCollection.equals(ImagesCollections.ORGANIZATIONS.getFolder())) {
				//TODO: check if the user can manipulate the organization
				return true;
			}
			
			return false;
			
		} catch (Exception oE) {
			WasdiLog.debugLog("PermissionsUtils.canUserAccessImage error: " + oE);
		}

		return false;			
	}
}
 