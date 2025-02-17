/**
 * Created by Cristiano Nattero on 2020-04-14
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ImagesCollections;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Project;
import wasdi.shared.business.S3Volume;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.business.Style;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.missions.Mission;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserAccessRights;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.business.users.UserType;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.CommentRepository;
import wasdi.shared.data.JupyterNotebookRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.OpenEOJobRepository;
import wasdi.shared.data.OrganizationRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorParametersTemplateRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.ProjectRepository;
import wasdi.shared.data.ReviewRepository;
import wasdi.shared.data.S3VolumeRepository;
import wasdi.shared.data.ScheduleRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.SnapWorkflowRepository;
import wasdi.shared.data.StyleRepository;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.data.missions.MissionsRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.wasdiAPI.ProcessorAPIClient;
import wasdi.shared.utils.wasdiAPI.WorkspaceAPIClient;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.organizations.SubscriptionType;

/**
 * Wrap all the methods to check user rights and permissions
 * 
 * @author c.nattero
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
		
		if (oUser == null) return false;
		
	
		try {
			
			String sActiveProjectOfUser = oUser.getActiveProjectId();
			
			if (sActiveProjectOfUser == null) {
				WasdiLog.warnLog("PermissionsUtils.userHasValidSubscription. User " + oUser.getUserId() + " has not an active project selected");
				return false;
			}
			
			ProjectRepository oProjectRepository = new ProjectRepository();
			Project oUserProject = oProjectRepository.getProjectById(sActiveProjectOfUser);
			String sSubscriptionId = oUserProject.getSubscriptionId();
			
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			Subscription oUserSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);
			
			// time-based model for not-free subscriptions
			if (!oUserSubscription.getType().equals(SubscriptionType.Free.getTypeId())) {				
				return oProjectRepository.checkValidSubscription(sActiveProjectOfUser);
				
			}
			
			// storage-based model for free subscriptions
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Long dTotalStorageUsage = oWorkspaceRepository.getStorageUsageForUser(oUser.getUserId());			
			
			if (dTotalStorageUsage < 0) {
				WasdiLog.warnLog("PermissionsUtils.userHasValidSubscription. There was an error computing the total storage space for the user");
				return false;
			}
			
			
			if (dTotalStorageUsage < WasdiConfig.Current.storageUsageControl.storageSizeFreeSubscription) {
				return true;
			} 
			else {
				WasdiLog.warnLog("PermissionsUtils.userHasValidSubscription. User " + oUser.getUserId() + 
						" exceed the maximum storage size for free subscriptions: " + Utils.getNormalizedSize(Double.parseDouble(dTotalStorageUsage.toString())));
				return false;
			}
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PermissionsUtils.userHasValidSubscription: error: ", oEx);
		}
		
		return false;
	}
	
	
	/**
	 * Get the type of a user
	 * @param oUser User Entity
	 * @return Type String (see UserType enum)
	 */
	public static String getUserType(User oUser) {
		if (oUser == null) return UserType.NONE.name();
		return getUserType(oUser.getUserId());
	}
	
	
	/**
	 * Get the user type detecting it from the available subscriptions
	 * @param sUserId
	 * @return
	 */
	public static String getUserType(String sUserId) {
		
		// Check the user id
		if (Utils.isNullOrEmpty(sUserId)) return UserType.NONE.name();
		
		try {
			
			// Take ALL the subscriptions related to this user
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			List<Subscription> aoUserSubscriptions = oSubscriptionRepository.getAllSubscriptionsOfUser(sUserId);
			
			// No Subscription No Party
			if (aoUserSubscriptions == null) return UserType.NONE.name();
			if (aoUserSubscriptions.size() == 0) return UserType.NONE.name();
			
			// Initialize our user as NONE (the lower)
			UserType oUserType = UserType.NONE;
			
			// For each subscription
			for (Subscription oSubscription : aoUserSubscriptions) {
				
				// If it is valid
				if (oSubscription.isValid()) {
					// Free can replace None
					if (oSubscription.getRelatedUserType().equals(UserType.FREE.name())) {
						if (oUserType.name().equals(UserType.NONE.name())) {
							oUserType = UserType.FREE;
						}
					}
					else if (oSubscription.getRelatedUserType().equals(UserType.STANDARD.name())) {
						// Std can replace none and free
						if (oUserType.name().equals(UserType.NONE.name()) || oUserType.name().equals(UserType.FREE.name())) {
							oUserType = UserType.STANDARD;
						}						
					}
					else if (oSubscription.getRelatedUserType().equals(UserType.PROFESSIONAL.name())) {
						// Pro wins and also no need to search anymore
						oUserType = UserType.PROFESSIONAL;
						break;
					}					
				}
			}
			
			return oUserType.name();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PermissionsUtils.getUserType: error: " + oEx);
		}
		
		return UserType.NONE.name();		
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
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
			
			if (oWorkspace == null) {
				return false;
			}
			
			if (oWorkspace.getUserId().equals(sUserId)) {
				return true;
			}
			
			if (oWorkspace.isPublic()) {
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
	 * Check if a User Can Write in a Workspace
	 * @param sUserId a valid userId
	 * @param sWorkspaceId a valid workspaceId
	 * @return true if the user can write in the workspace, false otherwise
	 */
	public static boolean canUserWriteWorkspace(String sUserId, String sWorkspaceId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sWorkspaceId)) {
				return false;
			}

			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			
			if (oWorkspaceRepository.isOwnedByUser(sUserId, sWorkspaceId)) {
				return true;
			}

			return canUserWriteResource(ResourceTypes.WORKSPACE.getResourceType(), sUserId, sWorkspaceId);
			
		} catch (Exception oE) {
			WasdiLog.errorLog("PermissionsUtils.canUserWriteWorkspace( " + sUserId + ", " + sWorkspaceId + " ): error: " + oE);
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
	 * Check if a User Can Write in a Organization
	 * @param sUserId a valid userId
	 * @param sOrganizationId a valid organization
	 * @return true if the user can write the organization, false otherwise
	 */
	public static boolean canUserWriteOrganization(String sUserId, String sOrganizationId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sOrganizationId)) {
				return false;
			}

			OrganizationRepository oOrganizationRepository = new OrganizationRepository();
			
			if (oOrganizationRepository.isOwnedByUser(sUserId, sOrganizationId)) {
				return true;
			}

			return canUserWriteResource(ResourceTypes.ORGANIZATION.getResourceType(), sUserId, sOrganizationId);
			
		} catch (Exception oE) {
			WasdiLog.errorLog("PermissionsUtils.canUserWriteOrganization( " + sUserId + ", " + sOrganizationId + " ): error: " + oE);
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
			
			if (UserApplicationRole.isAdmin(sUserId)) {
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
	 * Check if a User Can Write a Subscription
	 * @param sUserId a valid userId
	 * @param sSubscriptionId a valid Subscription
	 * @return true if the user can write the subscription, false otherwise
	 */
	public static boolean canUserWriteSubscription(String sUserId, String sSubscriptionId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sSubscriptionId)) {
				return false;
			}

			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			
			if (oSubscriptionRepository.isOwnedByUser(sUserId, sSubscriptionId)) {
				return true;
			}
			
			if (UserApplicationRole.isAdmin(sUserId)) {
				return true;
			}

			return canUserWriteResource(ResourceTypes.SUBSCRIPTION.getResourceType(), sUserId, sSubscriptionId);
			
		} catch (Exception oE) {
			WasdiLog.errorLog("PermissionsUtils.canUserWriteSubscription( " + sUserId + ", " + sSubscriptionId + " ): error: " + oE);
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
			
			if (oStyleRepository.isOwnedByUser(sUserId, sStyleId)) {
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
	 * Check if a User Can Write a Style
	 * @param sUserId a valid userId
	 * @param sStyleId a valid style
	 * @return true if the user can write the style, false otherwise
	 */
	public static boolean canUserWriteStyle(String sUserId, String sStyleId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sStyleId)) {
				return false;
			}

			StyleRepository oSubscriptionRepository = new StyleRepository();
			
			if (oSubscriptionRepository.isOwnedByUser(sUserId, sStyleId)) {
				return true;
			}

			return canUserWriteResource(ResourceTypes.STYLE.getResourceType(), sUserId, sStyleId);
			
		} catch (Exception oE) {
			WasdiLog.errorLog("PermissionsUtils.canUserWriteStyle( " + sUserId + ", " + sStyleId + " ): error: " + oE);
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
	 * Check if a User Can Write a ProcessWorkspace
	 * @param sUserId a valid userId
	 * @param sProcessWorkspaceId a valid ProcessWorkspace
	 * @return true if the user can write the ProcessWorkspace, false otherwise
	 */
	public static boolean canUserWriteProcessWorkspace(String sUserId, String sProcessWorkspaceId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sProcessWorkspaceId)) {
				return false;
			}

			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			
			if (oProcessWorkspaceRepository.isProcessOwnedByUser(sUserId, sProcessWorkspaceId)) {
				return true;
			}
						
			if (UserApplicationRole.isAdmin(sUserId)) {
				return true;
			}
			
			ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(sProcessWorkspaceId);
			
			if (oProcessWorkspace == null) {
				return false;
			}

			return canUserWriteResource(ResourceTypes.WORKSPACE.getResourceType(), sUserId, oProcessWorkspace.getWorkspaceId());
			
		} catch (Exception oE) {
			WasdiLog.errorLog("PermissionsUtils.canUserWriteProcessWorkspace( " + sUserId + ", " + sProcessWorkspaceId + " ): error: " + oE);
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
	 * Check if a User Can Write a ProcessorParametersTemplate
	 * @param sUserId a valid userId
	 * @param sProcessorParametersTemplateId a valid ProcessorParametersTemplate
	 * @return true if the user can write the ProcessorParametersTemplate, false otherwise
	 */
	public static boolean canUserWriteProcessorParametersTemplate(String sUserId, String sProcessorParametersTemplateId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sProcessorParametersTemplateId)) {
				return false;
			}

			ProcessorParametersTemplateRepository oSubscriptionRepository = new ProcessorParametersTemplateRepository();
			
			if (oSubscriptionRepository.isTheOwnerOfTheTemplate(sProcessorParametersTemplateId, sUserId)) {
				return true;
			}

			return canUserWriteResource(ResourceTypes.PARAMETER.getResourceType(), sUserId, sProcessorParametersTemplateId);
			
		} catch (Exception oE) {
			WasdiLog.errorLog("PermissionsUtils.canUserWriteProcessorParametersTemplate( " + sUserId + ", " + sProcessorParametersTemplateId + " ): error: " + oE);
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
	 * Check if a user can write a processor
	 * @param sUserId
	 * @param sProcessorId
	 * @return
	 */
	public static boolean canUserWriteProcessor(String sUserId, String sProcessorId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sProcessorId)) {
				return false;
			}

			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
			
			return canUserWriteProcessor(sUserId, oProcessor);
			
		} catch (Exception oE) {
			WasdiLog.debugLog("PermissionsUtils.canUserAccessProcessor( " + sUserId + ", " + sProcessorId + " ): error: " + oE);
		}

		return false;		
	}	
	
	
	
	/**
	 * Check if a User Can Write a Processor by name
	 * @param sUserId
	 * @param sName
	 * @return
	 */
	public static boolean canUserWriteProcessorByName(String sUserId, String sName) { 
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sName)) {
				return false;
			}

			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			
			Processor oProcessor = oProcessorRepository.getProcessorByName(sName);
			
			return canUserWriteProcessor(sUserId, oProcessor);			
		} catch (Exception oE) {
			WasdiLog.debugLog("PermissionsUtils.canUserWriteProcessorByName( " + sUserId + ", " + sName + " ): error: " + oE);
		}

		return false;		
	}
	
	/**
	 * Check if a User Can Write a Processor
	 * @param sUserId a valid userId
	 * @param oProcessor a valid Processor
	 * @return true if the user can write the Processor, false otherwise
	 */
	public static boolean canUserWriteProcessor(String sUserId, Processor oProcessor) {
		try {
			if (oProcessor == null) return false;
			if (oProcessor.getUserId().equals(sUserId)) return true;

			return canUserWriteResource(ResourceTypes.PROCESSOR.getResourceType(), sUserId, oProcessor.getProcessorId());

		} catch (Exception oE) {
			WasdiLog.errorLog("PermissionsUtils.canUserWriteProcessor( " + sUserId + ", " + oProcessor.getProcessorId() + " ): error: " + oE);
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
			else if (sCollection.equals(ImagesCollections.STYLES.getFolder())) {
				String sStyleName = WasdiFileUtils.getFileNameWithoutExtensionsAndTrailingDots(sImage);
				
				if (!Utils.isNullOrEmpty(sStyleName)) {
					StyleRepository oStyleRepository = new StyleRepository();
					Style oStyle = oStyleRepository.getStyleByName(sStyleName);
					if (oStyle != null) {
						return canUserAccessStyle(sUserId, oStyle.getStyleId());
					}					
				}				
			}
			
			
			return false;
			
		} 
		catch (Exception oE) {
			WasdiLog.errorLog("PermissionsUtils.canUserAccessImage error: " + oE);
		}

		return false;			
	}
	
	
	/**
	 * Check if a user can write a specific image
	 * @param sUserId User requesting the access
	 * @param sCollection Image Collection 
	 * @param sFolder Folder name
	 * @param sImage Image name
	 * @return
	 */
	public static boolean canUserWriteImage(String sUserId, String sCollection, String sFolder, String sImage) {
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
				return canUserWriteProcessor(sUserId, oProcessor.getProcessorId());
			}
			else if (sCollection.equals(ImagesCollections.USERS.getFolder())) {
				if (sUserId.equals(sFolder)) return true;
				else return false;
			}
			else if (sCollection.equals(ImagesCollections.ORGANIZATIONS.getFolder())) {
				return canUserWriteOrganization(sUserId, sFolder);
			}
			else if (sCollection.equals(ImagesCollections.STYLES.getFolder())) {
				String sStyleName = WasdiFileUtils.getFileNameWithoutExtensionsAndTrailingDots(sImage);
				
				if (!Utils.isNullOrEmpty(sStyleName)) {
					StyleRepository oStyleRepository = new StyleRepository();
					Style oStyle = oStyleRepository.getStyleByName(sStyleName);
					if (oStyle != null) {
						return canUserWriteStyle(sUserId, oStyle.getStyleId());
					}					
				}				
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
	
	
	/**
	 * Check if a User Can Write a Workflow
	 * @param sUserId a valid userId
	 * @param sWorkflowId a valid Workflow
	 * @return true if the user can write the Workflow, false otherwise
	 */
	public static boolean canUserWriteWorkflow(String sUserId, String sWorkflowId) {
		try {
			if (Utils.isNullOrEmpty(sUserId) || Utils.isNullOrEmpty(sWorkflowId)) {
				return false;
			}

			SnapWorkflowRepository oSubscriptionRepository = new SnapWorkflowRepository();
			SnapWorkflow oWorkflow = oSubscriptionRepository.getSnapWorkflow(sWorkflowId);
			
			if (oWorkflow == null) return false;
			
			if (oWorkflow.getUserId().equals(sUserId)) {
				return true;
			}

			return canUserWriteResource(ResourceTypes.WORKFLOW.getResourceType(), sUserId, sWorkflowId);
			
		} catch (Exception oE) {
			WasdiLog.errorLog("PermissionsUtils.canUserWriteWorkflow( " + sUserId + ", " + sWorkflowId + " ): error: " + oE);
		}

		return false;
	}	
	
	/**
	 * Check if a User can write a specific resource
	 * @param sResourceType
	 * @param sUserId
	 * @param sWorkspaceId
	 * @return
	 */
	public static boolean canUserWriteResource(String sResourceType, String sUserId, String sWorkspaceId) {
		
		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			UserResourcePermission oPermission = oUserResourcePermissionRepository.getPermissionByTypeAndUserIdAndResourceId(sResourceType, sUserId, sWorkspaceId);
			
			if (oPermission == null) return false;
			
			if (oPermission.getPermissions().equals(UserAccessRights.WRITE.getAccessRight())) return true;
			
			return false;			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PermissionsUtils.canUserWriteResource error: " + oEx);
			return false;
		}
	}
	
	/**
	 * Check if a User can access a Mission (Data Collection) or not
	 * @param sUserId User 
	 * @param sMissionIndexValue Code of the mission
	 * @return
	 */
	public static boolean canUserAccessMission(String sUserId, String sMissionIndexValue) {
		MissionsRepository oMissionsRepository = new MissionsRepository();
		Mission oMission =  oMissionsRepository.getMissionsByIndexValue(sMissionIndexValue);
		return canUserAccessMission(sUserId, oMission);
	}
	
	/**
	 * Check if a User can access a Mission (Data Collection) or not
	 * @param sUserId
	 * @param sMissionId
	 * @return
	 */
	public static boolean canUserAccessMission(String sUserId, Mission oMission) {
		try {
			
			if (oMission == null) return false;
			if (oMission.isIspublic()) return true;
			if (Utils.isNullOrEmpty(oMission.getUserid()) == false) {
				if (oMission.getUserid().equals(sUserId)) return true;
			}
			
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			UserResourcePermission oPermission = oUserResourcePermissionRepository.getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.MISSION.getResourceType(), sUserId, oMission.getIndexvalue());
			
			if (oPermission == null) return false;
			else return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PermissionsUtils.canUserWriteResource error: " + oEx);
			return false;
		}		
	}
	
	
	/**
	 * Return a list of S3 Volumes to be mounted on the workspace given the ProcessorParameter
	 * @param oProcessorParameter ProcessorParameter in input
	 * @return List of volumes to mount
	 */

	public static List<S3Volume> getVolumesToMount(ProcessorParameter oProcessorParameter) {
		
		// Here we save the ones to return for this app
		List<S3Volume> aoOutputList = new ArrayList<>();
		
		if (oProcessorParameter == null) {
			return aoOutputList;
		}
		else {
			return getVolumesToMount(oProcessorParameter.getWorkspace(), oProcessorParameter.getProcessorID(), oProcessorParameter.getUserId());
		}
	}
	
	/**
	 * Return a list of S3 Volumes to be mounted on the workspace given the ProcessorParameter
	 * @param oProcessorParameter ProcessorParameter in input
	 * @return List of volumes to mount
	 */
	public static List<S3Volume> getVolumesToMount(String sWorkspaceId, String sProcessorId, String sRequestingUserId) {
		
		// Here we save the ones to return for this app
		List<S3Volume> aoOutputList = new ArrayList<>();
		
		try {
			// Repo for S3 Volumes and Resource Permissions
			S3VolumeRepository oS3VolumeRepository = new S3VolumeRepository();
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			
			// Get the list of all volumes
			List<S3Volume> aoAllVolumes = oS3VolumeRepository.getVolumes();
						
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
			
			Processor oProcessor = null;
			
			if (!Utils.isNullOrEmpty(sProcessorId)) {
				oProcessor = oProcessorRepository.getProcessor(sProcessorId);
			}
			
			
			// For all the volumes
			for (S3Volume oS3Volume : aoAllVolumes) {
				
				// If is a volume of the user starting the app ok
				if (oS3Volume.getUserId().equals(sRequestingUserId)) {
					aoOutputList.add(oS3Volume);
					continue;
				}
				
				// If we are in a workspace of the owner
				if (oWorkspace.getUserId().equals(oS3Volume.getUserId())) {
					aoOutputList.add(oS3Volume);
					continue;					
				}
				
				// If the owner of the volume has the workspace shared is ok 
				if (oUserResourcePermissionRepository.isWorkspaceSharedWithUser(oS3Volume.getUserId(), sWorkspaceId)) {
					aoOutputList.add(oS3Volume);
					continue;
				}
				
				if (oProcessor!=null) {
					if (oProcessor.getUserId().equals(oS3Volume.getUserId())) {
						aoOutputList.add(oS3Volume);
						continue;					
					}					
				}
			}
			
			
			return aoOutputList;			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PermissionsUtils.getVolumesToMount error: " + oEx);
			return aoOutputList;
		}		
	}
	
	public static File getFileFromS3Volume(String sUserId, String sFileName, String sWorkspaceId, String sProcessObjId) {
		// The file is not in the WASDI db. Can be a file on an S3 Volume?
		
		// Split the file: if it is a S3 Volume MUST have at least one folder
		String [] asFileParts = sFileName.split("/");
		
		if (asFileParts == null) {
			// Strange
			return null;				
		}

		if (asFileParts.length == 1) {
			// It is a normal file, cannot be a volume
			return null;					
		}
		
		// We take the root part that MAY be a Volume
		String sRootPart = asFileParts[0];
		
		// Here I wanted to manage the situation where there is a folder with the same name of a volume.
		// But does not work beacuse in any case the docker creates the folder when the volume is mounted 
		// So the folder is always there.
		
		// Get the local path
//		String sTargetFilePath = PathsConfig.getWorkspacePath(sUserId,sWorkspaceId) + sRootPart;
		
		// If exists in the workspace a folder with the same name, we stop here.
//		File oFolderFile = new File(sTargetFilePath);
//		
//		if (oFolderFile.exists()) {
//			WasdiLog.debugLog("PermissionsUtils.getFileFromS3Volume: " + sRootPart + " is a subfolder of the workspace. We stop here and do not verify Volumes");
//			return null;
//		}
		
		
		// We need to understand if this request is related to a processor
		String sProcessorId = "";
		
		// if we have a process obj id
		if (!Utils.isNullOrEmpty(sProcessObjId)) {
			try {
				
				// Try to read it
				ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
				ProcessWorkspace oProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(sProcessObjId);
				
				if (oProcessWorkspace != null) {
					// If it is an app
					if (oProcessWorkspace.getOperationType().equals(LauncherOperations.RUNPROCESSOR.toString()) || oProcessWorkspace.getOperationType().equals(LauncherOperations.RUNIDL.toString())) {
						// Take the name
						String sProcessorName = oProcessWorkspace.getProductName();
						
						// Search for the app
						ProcessorRepository oProcessorRepository = new ProcessorRepository();
						Processor oProcessor = oProcessorRepository.getProcessorByName(sProcessorName);
						
						// If we fuond it
						if (oProcessor != null) {
							// We can get the id!!
							sProcessorId = oProcessor.getProcessorId();
							WasdiLog.debugLog("PermissionsUtils.getFileFromS3Volume: found processor " + sProcessorId);
						}
					}
				}
			}
			catch (Exception oEx) {
				WasdiLog.errorLog("PermissionsUtils.getFileFromS3Volume: error trying to detect the application ", oEx);
			}
		}
		
		List<S3Volume> aoVolumes = PermissionsUtils.getVolumesToMount(sWorkspaceId,sProcessorId,sUserId);
		
		if (aoVolumes == null) {
			return null;
		}
		
		if (aoVolumes.size()<=0) {
			return null;
		}
		
		for (S3Volume oS3Volume : aoVolumes) {
			if (oS3Volume.getMountingFolderName().equals(sRootPart)) {
				
				String sFileInVolume = PathsConfig.getS3VolumesBasePath() + sFileName;
				
				WasdiLog.debugLog("PermissionsUtils.getFileFromS3Volume: found Volume " + oS3Volume.getMountingFolderName() + " test file " + sFileInVolume);
				
				// Check if the file exists
				File oFileInVolume = new File(sFileInVolume);
				if (oFileInVolume.exists()) {
					PrimitiveResult oResult = new PrimitiveResult();
					oResult.setBoolValue(true);
					return oFileInVolume;									
				}
				else {
					WasdiLog.debugLog("PermissionsUtils.getFileFromS3Volume: no, we cannot read it");
				}
			}
		}
		
		return null;
	}
	
	public static boolean deleteUser(User oUser, String sSessionId) {
		try {
			
			String sUserId = oUser.getUserId();
			
            // Get all the workspaces
            WorkspaceRepository oWorkspaceRepo = new WorkspaceRepository();
            List<Workspace> aoWorkspaces = oWorkspaceRepo.getWorkspaceByUser(sUserId);
            
            NodeRepository oNodeRepository = new NodeRepository();
            
            HashMap<String, Node> aoNodes = new HashMap<>();
            
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting Workspaces ");

            // Clean Workspaces (and product workspace, process workspace, published bands)
            for (Workspace oWorkspace : aoWorkspaces) {
            	
            	if (!aoNodes.containsKey(oWorkspace.getNodeCode())) {
            		Node oNode = oNodeRepository.getNodeByCode(oWorkspace.getNodeCode());
            		aoNodes.put(oWorkspace.getNodeCode(), oNode);
            	}
            	
            	if (aoNodes.containsKey(oWorkspace.getNodeCode())) {
            		WorkspaceAPIClient.deleteWorkspace(aoNodes.get(oWorkspace.getNodeCode()), sSessionId, oWorkspace.getWorkspaceId());
            	}
            	else {
            		WasdiLog.warnLog("PermissionsUtils.deleteUser: it looks we cannot find the node " + oWorkspace.getNodeCode());
            	}
            }
            
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting Sharings");
            
            // Clean all the sharings
            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
            oUserResourcePermissionRepository.deletePermissionsByUserId(sUserId);
            oUserResourcePermissionRepository.deletePermissionsByOwnerId(sUserId);
            
            // Clean Comments
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting Comments");
            CommentRepository oCommentRepository = new CommentRepository();
            oCommentRepository.deleteCommentsByUser(sUserId);
            
            // Clean Notebooks
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting Notebooks");
            JupyterNotebookRepository oJupyterNotebookRepository = new JupyterNotebookRepository();
            oJupyterNotebookRepository.deleteJupyterNotebookByUser(sUserId);
            
            // Clean open EO Jobs
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting EO Jobs");
            OpenEOJobRepository oOpenEOJobRepository = new OpenEOJobRepository();
            oOpenEOJobRepository.deleteOpenEOJobsByUser(sUserId);
            
            // Clean organizations
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting Organizations");
            OrganizationRepository oOrganizationRepository = new OrganizationRepository();
            oOrganizationRepository.deleteByUser(sUserId);
            
            // Clean Processor Parameters
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting Processor Parameters");
            ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();
            oProcessorParametersTemplateRepository.deleteByUserId(sUserId);
            
            // Clean Processor (and ui)
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting Processors");
            ProcessorRepository oProcessorRepository = new ProcessorRepository();
            List<Processor> aoProcessors = oProcessorRepository.getProcessorByUser(sUserId);
            
            for (Processor oProcessor : aoProcessors) {
				ProcessorAPIClient.delete(sSessionId, oProcessor.getProcessorId());
			}
            
            // Clean Reviews
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting Reviews");
            ReviewRepository oReviewRepository = new ReviewRepository();
            oReviewRepository.deleteReviewsByUser(sUserId);
            
            // Clean S3 Volumes
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting Volumes");
            S3VolumeRepository oS3VolumeRepository = new S3VolumeRepository();
            oS3VolumeRepository.deleteByUserId(sUserId);
            
            // Clean Scheduling
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting Schedulings");
            ScheduleRepository oScheduleRepository = new ScheduleRepository();
            oScheduleRepository.deleteScheduleByUserId(sUserId);
            
            // Clean Sessions
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting Sessions");
            SessionRepository oSessionRepository = new SessionRepository();
            oSessionRepository.deleteSessionsByUserId(sUserId);
            
            // Clean Snap Workflows
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting Workflows");
            SnapWorkflowRepository oSnapWorkflowRepository = new SnapWorkflowRepository();
            oSnapWorkflowRepository.deleteSnapWorkflowByUser(sUserId);
            
            // Clean the styles
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting Styles");
            StyleRepository oStyleRepository = new StyleRepository();
            oStyleRepository.deleteStyleByUser(sUserId);
            
            // Clean the subscriptions
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting Subscriptions");
            SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
            oSubscriptionRepository.deleteByUser(sUserId);
            
            // Clean the user table
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting User");
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting User Db Entry ");

            UserRepository oUserRepository = new UserRepository();
            oUserRepository.deleteUser(sUserId);

            // Clean the user folder
            WasdiLog.debugLog("PermissionsUtils.deleteUser: Deleting User Folder ");

            String sBasePath = PathsConfig.getWasdiBasePath();
            sBasePath += sUserId;
            sBasePath += "/";

            FileUtils.deleteDirectory(new File(sBasePath));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PermissionsUtils.deleteUser: exception ", oEx);
			return false;
		}
	}
}
 