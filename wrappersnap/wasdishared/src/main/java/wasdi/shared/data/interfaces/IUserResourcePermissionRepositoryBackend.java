package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.users.UserResourcePermission;

/**
 * Backend contract for user resource permission repository.
 */
public interface IUserResourcePermissionRepositoryBackend {

	boolean insertPermission(UserResourcePermission oUserResourcePermission);

	List<UserResourcePermission> getPermissionsByOwnerId(String sUserId);

	List<UserResourcePermission> getPermissionsByTypeAndOwnerId(String sType, String sUserId);

	List<UserResourcePermission> getOrganizationSharingsByOwnerId(String sUserId);

	List<UserResourcePermission> getSubscriptionSharingsByOwnerId(String sUserId);

	List<UserResourcePermission> getWorkspaceSharingsByOwnerId(String sUserId);

	List<UserResourcePermission> getStyleSharingsByOwnerId(String sUserId);

	List<UserResourcePermission> getWorkflowSharingsByOwnerId(String sUserId);

	List<UserResourcePermission> getProcessorSharingsByOwnerId(String sUserId);

	List<UserResourcePermission> getPermissionsByUserId(String sUserId);

	List<UserResourcePermission> getPermissionsByTypeAndUserId(String sType, String sUserId);

	List<UserResourcePermission> getOrganizationSharingsByUserId(String sUserId);

	List<UserResourcePermission> getSubscriptionSharingsByUserId(String sUserId);

	List<UserResourcePermission> getWorkspaceSharingsByUserId(String sUserId);

	List<UserResourcePermission> getStyleSharingsByUserId(String sUserId);

	List<UserResourcePermission> getWorkflowSharingsByUserId(String sUserId);

	List<UserResourcePermission> getProcessorSharingsByUserId(String sUserId);

	List<UserResourcePermission> getProcessorParametersTemplateSharingsByUserId(String sUserId);

	List<UserResourcePermission> getMissionsharingByUserId(String sUserId);

	List<UserResourcePermission> getPermissionsByResourceId(String sResourceId);

	List<UserResourcePermission> getPermissionsByTypeAndResourceId(String sType, String sResourceId);

	boolean isResourceShared(String sType, String sResourceId);

	List<UserResourcePermission> getPermissionsByTypeAndResourceIds(String sType, List<String> asResourceIds);

	List<UserResourcePermission> getNodeSharingsByNodeCode(String sNodeCode);

	List<UserResourcePermission> getOrganizationSharingsByOrganizationId(String sOrganizationId);

	List<UserResourcePermission> getOrganizationSharingsByOrganizationIds(List<String> asOrganizationIds);

	List<UserResourcePermission> getSubscriptionSharingsBySubscriptionId(String sSubscriptionId);

	List<UserResourcePermission> getWorkspaceSharingsByWorkspaceId(String sWorkspaceId);

	List<UserResourcePermission> getStyleSharingsByStyleId(String sStyleId);

	List<UserResourcePermission> getWorkflowSharingsByWorkflowId(String sWorkflowId);

	List<UserResourcePermission> getProcessorSharingsByProcessorId(String sProcessorId);

	List<UserResourcePermission> getProcessorParametersTemplateSharingsByProcessorParametersTemplateId(String sProcessorParametersTemplateId);

	UserResourcePermission getPermissionByUserIdAndResourceId(String sUserId, String sResourceId);

	UserResourcePermission getPermissionByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId);

	List<UserResourcePermission> getPermissionsByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId);

	UserResourcePermission getOrganizationSharingByUserIdAndOrganizationId(String sUserId, String sOrganizationId);

	UserResourcePermission getSubscriptionSharingByUserIdAndSubscriptionId(String sUserId, String sSubscriptionId);

	UserResourcePermission getWorkspaceSharingByUserIdAndWorkspaceId(String sUserId, String sWorkspaceId);

	UserResourcePermission getStyleSharingByUserIdAndStyleId(String sUserId, String sStyleId);

	UserResourcePermission getWorkflowSharingByUserIdAndWorkflowId(String sUserId, String sWorkflowId);

	UserResourcePermission getProcessorSharingByUserIdAndProcessorId(String sUserId, String sProcessorId);

	UserResourcePermission getProcessorParametersTemplateSharingByUserIdAndProcessorParametersTemplateId(String sUserId, String sProcessorParametersTemplateId);

	List<UserResourcePermission> getPermissions();

	List<UserResourcePermission> getPermissionsByType(String sType);

	List<UserResourcePermission> getNodeSharings();

	List<UserResourcePermission> getOrganizationSharings();

	List<UserResourcePermission> getSubscriptionSharings();

	List<UserResourcePermission> getWorkspaceSharings();

	List<UserResourcePermission> getStyleSharings();

	List<UserResourcePermission> getWorkflowSharings();

	List<UserResourcePermission> getProcessorSharings();

	List<UserResourcePermission> getProcessorParametersTemplateSharings();

	int deletePermissionsByResourceId(String sResourceId);

	int deletePermissionsByTypeAndResourceId(String sType, String sResourceId);

	int deletePermissionsByTypeAndUserId(String sType, String sResourceId);

	int deletePermissionsByNodeCode(String sNodeCode);

	int deletePermissionsByOrganizationId(String sOrganizationId);

	int deletePermissionsBySubscriptionId(String sSubscriptionId);

	int deletePermissionsByWorkspaceId(String sWorkspaceId);

	int deletePermissionsByStyleId(String sStyleId);

	int deletePermissionsByWorkflowId(String sWorkflowId);

	int deletePermissionsByProcessorId(String sProcessorId);

	int deletePermissionsByProcessorParameterTemplateId(String sProcessorParameterTemplateId);

	int deletePermissionsByUserId(String sUserId);

	int deletePermissionsByOwnerId(String sOwnerId);

	int deleteOrganizationPermissionsByUserId(String sUserId);

	int deleteSubscriptionPermissionsByUserId(String sUserId);

	int deleteWorkspacePermissionsByUserId(String sUserId);

	int deleteStylePermissionsByUserId(String sUserId);

	int deleteWorkflowPermissionsByUserId(String sUserId);

	int deleteProcessorPermissionsByUserId(String sUserId);

	int deletePermissionsByUserIdAndResourceId(String sUserId, String sResourceId);

	int deletePermissionsByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId);

	int deletePermissionsByUserIdAndNodeCode(String sUserId, String sNodeCode);

	int deletePermissionsByUserIdAndOrganizationId(String sUserId, String sOrganizationId);

	int deletePermissionsByUserIdAndSubscriptionId(String sUserId, String sSubscriptionId);

	int deletePermissionsByUserIdAndWorkspaceId(String sUserId, String sWorkspaceId);

	int deletePermissionsByUserIdAndStyleId(String sUserId, String sStyleId);

	int deletePermissionsByUserIdAndWorkflowId(String sUserId, String sWorkflowId);

	int deletePermissionsByUserIdAndProcessorId(String sUserId, String sProcessorId);

	int deletePermissionsByUserIdAndProcessorParametersTemplateId(String sUserId, String sProcessorParametersTemplateId);

	boolean isResourceSharedWithUser(String sUserId, String sResourceId);

	boolean isResourceOfTypeSharedWithUser(String sType, String sUserId, String sResourceId);

	boolean isNodeSharedWithUser(String sUserId, String sNodeCode);

	boolean isOrganizationSharedWithUser(String sUserId, String sOrganizationId);

	boolean isOrganizationShared(String sOrganizationId);

	boolean isSubscriptionShared(String sSubscriptionId);

	boolean isSubscriptionSharedWithUser(String sUserId, String sSubscriptionId);

	boolean isWorkspaceSharedWithUser(String sUserId, String sWorkspaceId);

	boolean isStyleSharedWithUser(String sUserId, String sStyleId);

	boolean isWorkflowSharedWithUser(String sUserId, String sWorkflowId);

	boolean isProcessorSharedWithUser(String sUserId, String sProcessorId);

	boolean isProcessorParametersTemplateSharedWithUser(String sUserId, String sProcessorParametersTemplateId);

}