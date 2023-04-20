/**
 * Created by Cristiano Nattero on 2020-04-14
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

import wasdi.shared.business.ImagesCollections;
import wasdi.shared.business.Processor;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.business.Style;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.User;
import wasdi.shared.business.UserResourcePermission;
import wasdi.shared.data.OrganizationRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorParametersTemplateRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.ProjectRepository;
import wasdi.shared.data.SnapWorkflowRepository;
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
		// private constructor to hide the public implicit one 
	}
	
	/**
	 * Check if a User has a Valid Subscription
	 * @param oUser
	 * @return
	 */
	public static boolean userHasValidSubscription(User oUser) {
		
		return true;
		
		/*
		if (oUser == null) return false;
	
		try {
			String sActiveProjectOfUser = oUser.getActiveProjectId();
			ProjectRepository oProjectRepository = new ProjectRepository();
			boolean bUserHasAValidSubscription = oProjectRepository.checkValidSubscription(sActiveProjectOfUser);
			
			return bUserHasAValidSubscription;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PermissionsUtils.userHasValidSubscription: error: " + oEx);
		}
		
		return false;
		*/
	}
	
	/**
	 * Check if a User Can Access a Workspace
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
			WasdiLog.errorLog("PermissionsUtils.canUserAccessWorkspace( " + sUserId + ", " + sWorkspaceId + " ): error: " + oE);
		}

		return false;
	}

	/**
	 * Check if a User can Access and Organization
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
	 * Check if a User can Access a Subscription
	 * 
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

			Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);

			if (sUserId.equals(oSubscription.getUserId())) {
				return true;
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			if (oUserResourcePermissionRepository.isSubscriptionSharedWithUser(sUserId, sSubscriptionId)) {
				return true;
			}
			
			String sOrganizationId = oSubscription.getOrganizationId();

			if (sOrganizationId != null) {
				UserResourcePermission oPermision = oUserResourcePermissionRepository.getOrganizationSharingByUserIdAndOrganizationId(sUserId, sOrganizationId);
				return oPermision != null;
			}
			
		} catch (Exception oE) {
			WasdiLog.debugLog("PermissionsUtils.canUserAccessSubscription( " + sUserId + ", " + sSubscriptionId + " ): error: " + oE);
		}

		return false;
	}

	/**
	 * Check if a User can access a Style
	 * 
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
			Style oStyle = oStyleRepository.getStyle(sStyleId);
			
			if (oStyle == null) {
				return false;
			}
			
			if (oStyle.getIsPublic()) {
				return true;
			}
			
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
	 * Check if a User can access a Process Workspace
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
	 * Check if a User can access a Processor Parameter Template
	 * 
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
			
			return canUserAccessProcessor(sUserId, oProcessor);
			
		} catch (Exception oE) {
			WasdiLog.debugLog("PermissionsUtils.canUserAccessProcessor( " + sUserId + ", " + sProcessorId + " ): error: " + oE);
		}

		return false;		
	}
	
	/**
	 * Check if a user can access a Processor starting from the name
	 * @param sUserId User that request the access
	 * @param sName Name of the processor
	 * @return
	 */
	public static boolean canUserAccessProcessorByName(String sUserId, String sName) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sName)) {
				return false;
			}

			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			
			Processor oProcessor = oProcessorRepository.getProcessorByName(sName);
			
			return canUserAccessProcessor(sUserId, oProcessor);			
		} catch (Exception oE) {
			WasdiLog.debugLog("PermissionsUtils.canUserAccessProcessor( " + sUserId + ", " + sName + " ): error: " + oE);
		}

		return false;		
	}
	
	/**
	 * Check if a user can access a Processor
	 * @param sUserId User requesting access
	 * @param oProcessor Processor requested
	 * @return true if can access, false if not
	 */
	public static boolean canUserAccessProcessor(String sUserId, Processor oProcessor) {
		try {			
			if (oProcessor == null) return false;
			
			if (oProcessor.getIsPublic()>0) return true;
			
			if (oProcessor.getUserId().equals(sUserId)) return true;

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			return oUserResourcePermissionRepository.isProcessorSharedWithUser(sUserId, oProcessor.getProcessorId());
		} catch (Exception oE) {
			WasdiLog.debugLog("PermissionsUtils.canUserAccessProcessor error: " + oE);
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
				return canUserAccessOrganization(sUserId, sFolder);
			}
			
			return false;
			
		} 
		catch (Exception oE) {
			WasdiLog.errorLog("PermissionsUtils.canUserAccessImage error: " + oE);
		}

		return false;			
	}
	
	/**
	 * Check if a user can access a workflow or not
	 * @param sUserId User requesting the access
	 * @param sWorkflowId workflow id
	 * @return true if can be accessed, false otherwise
	 */
	public static boolean canUserAccessWorkflow(String sUserId, String sWorkflowId) {
		try {
			if (Utils.isNullOrEmpty(sUserId)) return false;
			if (Utils.isNullOrEmpty(sWorkflowId)) return false;
			
			SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
			SnapWorkflow oWorkflow = oSnapWorkflowRepository.getSnapWorkflow(sWorkflowId);
			
			if (oWorkflow == null) return false;
			
			if (oWorkflow.getUserId().equals(sUserId)) return true;
			
			if (oWorkflow.getIsPublic()) return true;
			
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			return oUserResourcePermissionRepository.isWorkflowSharedWithUser(sUserId, sWorkflowId);			
		}
		catch (Exception oE) {
			WasdiLog.errorLog("PermissionsUtils.canUserAccessWorkflow error: " + oE);
		}
		
		return false;
	}
}
 