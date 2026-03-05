package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IUserResourcePermissionRepositoryBackend;

public class UserResourcePermissionRepository {
    private final IUserResourcePermissionRepositoryBackend m_oBackend;

    public UserResourcePermissionRepository() {
        this.m_oBackend = createBackend();
    }

    private IUserResourcePermissionRepositoryBackend createBackend() {
        return DataRepositoryFactoryProvider.getFactory().createUserResourcePermissionRepository();
    }

    public boolean insertPermission(UserResourcePermission oUserResourcePermission) {
        return m_oBackend.insertPermission(oUserResourcePermission);
    }

    public List<UserResourcePermission> getPermissionsByOwnerId(String sUserId) {
        return m_oBackend.getPermissionsByOwnerId(sUserId);
    }

    public List<UserResourcePermission> getPermissionsByTypeAndOwnerId(String sType, String sUserId) {
        return m_oBackend.getPermissionsByTypeAndOwnerId(sType, sUserId);
    }

    public List<UserResourcePermission> getOrganizationSharingsByOwnerId(String sUserId) {
        return m_oBackend.getOrganizationSharingsByOwnerId(sUserId);
    }

    public List<UserResourcePermission> getSubscriptionSharingsByOwnerId(String sUserId) {
        return m_oBackend.getSubscriptionSharingsByOwnerId(sUserId);
    }

    public List<UserResourcePermission> getWorkspaceSharingsByOwnerId(String sUserId) {
        return m_oBackend.getWorkspaceSharingsByOwnerId(sUserId);
    }

    public List<UserResourcePermission> getStyleSharingsByOwnerId(String sUserId) {
        return m_oBackend.getStyleSharingsByOwnerId(sUserId);
    }

    public List<UserResourcePermission> getWorkflowSharingsByOwnerId(String sUserId) {
        return m_oBackend.getWorkflowSharingsByOwnerId(sUserId);
    }

    public List<UserResourcePermission> getProcessorSharingsByOwnerId(String sUserId) {
        return m_oBackend.getProcessorSharingsByOwnerId(sUserId);
    }

    public List<UserResourcePermission> getPermissionsByUserId(String sUserId) {
        return m_oBackend.getPermissionsByUserId(sUserId);
    }

    public List<UserResourcePermission> getPermissionsByTypeAndUserId(String sType, String sUserId) {
        return m_oBackend.getPermissionsByTypeAndUserId(sType, sUserId);
    }

    public List<UserResourcePermission> getOrganizationSharingsByUserId(String sUserId) {
        return m_oBackend.getOrganizationSharingsByUserId(sUserId);
    }

    public List<UserResourcePermission> getSubscriptionSharingsByUserId(String sUserId) {
        return m_oBackend.getSubscriptionSharingsByUserId(sUserId);
    }

    public List<UserResourcePermission> getWorkspaceSharingsByUserId(String sUserId) {
        return m_oBackend.getWorkspaceSharingsByUserId(sUserId);
    }

    public List<UserResourcePermission> getStyleSharingsByUserId(String sUserId) {
        return m_oBackend.getStyleSharingsByUserId(sUserId);
    }

    public List<UserResourcePermission> getWorkflowSharingsByUserId(String sUserId) {
        return m_oBackend.getWorkflowSharingsByUserId(sUserId);
    }

    public List<UserResourcePermission> getProcessorSharingsByUserId(String sUserId) {
        return m_oBackend.getProcessorSharingsByUserId(sUserId);
    }

    public List<UserResourcePermission> getProcessorParametersTemplateSharingsByUserId(String sUserId) {
        return m_oBackend.getProcessorParametersTemplateSharingsByUserId(sUserId);
    }

    public List<UserResourcePermission> getMissionsharingByUserId(String sUserId) {
        return m_oBackend.getMissionsharingByUserId(sUserId);
    }

    public List<UserResourcePermission> getPermissionsByResourceId(String sResourceId) {
        return m_oBackend.getPermissionsByResourceId(sResourceId);
    }

    public List<UserResourcePermission> getPermissionsByTypeAndResourceId(String sType, String sResourceId) {
        return m_oBackend.getPermissionsByTypeAndResourceId(sType, sResourceId);
    }

    public boolean isResourceShared(String sType, String sResourceId) {
        return m_oBackend.isResourceShared(sType, sResourceId);
    }

    public List<UserResourcePermission> getPermissionsByTypeAndResourceIds(String sType, List<String> asResourceIds) {
        return m_oBackend.getPermissionsByTypeAndResourceIds(sType, asResourceIds);
    }

    public List<UserResourcePermission> getNodeSharingsByNodeCode(String sNodeCode) {
        return m_oBackend.getNodeSharingsByNodeCode(sNodeCode);
    }

    public List<UserResourcePermission> getOrganizationSharingsByOrganizationId(String sOrganizationId) {
        return m_oBackend.getOrganizationSharingsByOrganizationId(sOrganizationId);
    }

    public List<UserResourcePermission> getOrganizationSharingsByOrganizationIds(List<String> asOrganizationIds) {
        return m_oBackend.getOrganizationSharingsByOrganizationIds(asOrganizationIds);
    }

    public List<UserResourcePermission> getSubscriptionSharingsBySubscriptionId(String sSubscriptionId) {
        return m_oBackend.getSubscriptionSharingsBySubscriptionId(sSubscriptionId);
    }

    public List<UserResourcePermission> getWorkspaceSharingsByWorkspaceId(String sWorkspaceId) {
        return m_oBackend.getWorkspaceSharingsByWorkspaceId(sWorkspaceId);
    }

    public List<UserResourcePermission> getStyleSharingsByStyleId(String sStyleId) {
        return m_oBackend.getStyleSharingsByStyleId(sStyleId);
    }

    public List<UserResourcePermission> getWorkflowSharingsByWorkflowId(String sWorkflowId) {
        return m_oBackend.getWorkflowSharingsByWorkflowId(sWorkflowId);
    }

    public List<UserResourcePermission> getProcessorSharingsByProcessorId(String sProcessorId) {
        return m_oBackend.getProcessorSharingsByProcessorId(sProcessorId);
    }

    public List<UserResourcePermission> getProcessorParametersTemplateSharingsByProcessorParametersTemplateId(String sProcessorParametersTemplateId) {
        return m_oBackend.getProcessorParametersTemplateSharingsByProcessorParametersTemplateId(sProcessorParametersTemplateId);
    }

    public UserResourcePermission getPermissionByUserIdAndResourceId(String sUserId, String sResourceId) {
        return m_oBackend.getPermissionByUserIdAndResourceId(sUserId, sResourceId);
    }

    public UserResourcePermission getPermissionByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId) {
        return m_oBackend.getPermissionByTypeAndUserIdAndResourceId(sType, sUserId, sResourceId);
    }

    public List<UserResourcePermission> getPermissionsByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId) {
        return m_oBackend.getPermissionsByTypeAndUserIdAndResourceId(sType, sUserId, sResourceId);
    }

    public UserResourcePermission getOrganizationSharingByUserIdAndOrganizationId(String sUserId, String sOrganizationId) {
        return m_oBackend.getOrganizationSharingByUserIdAndOrganizationId(sUserId, sOrganizationId);
    }

    public UserResourcePermission getSubscriptionSharingByUserIdAndSubscriptionId(String sUserId, String sSubscriptionId) {
        return m_oBackend.getSubscriptionSharingByUserIdAndSubscriptionId(sUserId, sSubscriptionId);
    }

    public UserResourcePermission getWorkspaceSharingByUserIdAndWorkspaceId(String sUserId, String sWorkspaceId) {
        return m_oBackend.getWorkspaceSharingByUserIdAndWorkspaceId(sUserId, sWorkspaceId);
    }

    public UserResourcePermission getStyleSharingByUserIdAndStyleId(String sUserId, String sStyleId) {
        return m_oBackend.getStyleSharingByUserIdAndStyleId(sUserId, sStyleId);
    }

    public UserResourcePermission getWorkflowSharingByUserIdAndWorkflowId(String sUserId, String sWorkflowId) {
        return m_oBackend.getWorkflowSharingByUserIdAndWorkflowId(sUserId, sWorkflowId);
    }

    public UserResourcePermission getProcessorSharingByUserIdAndProcessorId(String sUserId, String sProcessorId) {
        return m_oBackend.getProcessorSharingByUserIdAndProcessorId(sUserId, sProcessorId);
    }

    public UserResourcePermission getProcessorParametersTemplateSharingByUserIdAndProcessorParametersTemplateId(String sUserId, String sProcessorParametersTemplateId) {
        return m_oBackend.getProcessorParametersTemplateSharingByUserIdAndProcessorParametersTemplateId(sUserId, sProcessorParametersTemplateId);
    }

    public List<UserResourcePermission> getPermissions() {
        return m_oBackend.getPermissions();
    }

    public List<UserResourcePermission> getPermissionsByType(String sType) {
        return m_oBackend.getPermissionsByType(sType);
    }

    public List<UserResourcePermission> getNodeSharings() {
        return m_oBackend.getNodeSharings();
    }

    public List<UserResourcePermission> getOrganizationSharings() {
        return m_oBackend.getOrganizationSharings();
    }

    public List<UserResourcePermission> getSubscriptionSharings() {
        return m_oBackend.getSubscriptionSharings();
    }

    public List<UserResourcePermission> getWorkspaceSharings() {
        return m_oBackend.getWorkspaceSharings();
    }

    public List<UserResourcePermission> getStyleSharings() {
        return m_oBackend.getStyleSharings();
    }

    public List<UserResourcePermission> getWorkflowSharings() {
        return m_oBackend.getWorkflowSharings();
    }

    public List<UserResourcePermission> getProcessorSharings() {
        return m_oBackend.getProcessorSharings();
    }

    public List<UserResourcePermission> getProcessorParametersTemplateSharings() {
        return m_oBackend.getProcessorParametersTemplateSharings();
    }

    public int deletePermissionsByResourceId(String sResourceId) {
        return m_oBackend.deletePermissionsByResourceId(sResourceId);
    }

    public int deletePermissionsByTypeAndResourceId(String sType, String sResourceId) {
        return m_oBackend.deletePermissionsByTypeAndResourceId(sType, sResourceId);
    }

    public int deletePermissionsByTypeAndUserId(String sType, String sResourceId) {
        return m_oBackend.deletePermissionsByTypeAndUserId(sType, sResourceId);
    }

    public int deletePermissionsByNodeCode(String sNodeCode) {
        return m_oBackend.deletePermissionsByNodeCode(sNodeCode);
    }

    public int deletePermissionsByOrganizationId(String sOrganizationId) {
        return m_oBackend.deletePermissionsByOrganizationId(sOrganizationId);
    }

    public int deletePermissionsBySubscriptionId(String sSubscriptionId) {
        return m_oBackend.deletePermissionsBySubscriptionId(sSubscriptionId);
    }

    public int deletePermissionsByWorkspaceId(String sWorkspaceId) {
        return m_oBackend.deletePermissionsByWorkspaceId(sWorkspaceId);
    }

    public int deletePermissionsByStyleId(String sStyleId) {
        return m_oBackend.deletePermissionsByStyleId(sStyleId);
    }

    public int deletePermissionsByWorkflowId(String sWorkflowId) {
        return m_oBackend.deletePermissionsByWorkflowId(sWorkflowId);
    }

    public int deletePermissionsByProcessorId(String sProcessorId) {
        return m_oBackend.deletePermissionsByProcessorId(sProcessorId);
    }

    public int deletePermissionsByProcessorParameterTemplateId(String sProcessorParameterTemplateId) {
        return m_oBackend.deletePermissionsByProcessorParameterTemplateId(sProcessorParameterTemplateId);
    }

    public int deletePermissionsByUserId(String sUserId) {
        return m_oBackend.deletePermissionsByUserId(sUserId);
    }

    public int deletePermissionsByOwnerId(String sOwnerId) {
        return m_oBackend.deletePermissionsByOwnerId(sOwnerId);
    }

    public int deleteOrganizationPermissionsByUserId(String sUserId) {
        return m_oBackend.deleteOrganizationPermissionsByUserId(sUserId);
    }

    public int deleteSubscriptionPermissionsByUserId(String sUserId) {
        return m_oBackend.deleteSubscriptionPermissionsByUserId(sUserId);
    }

    public int deleteWorkspacePermissionsByUserId(String sUserId) {
        return m_oBackend.deleteWorkspacePermissionsByUserId(sUserId);
    }

    public int deleteStylePermissionsByUserId(String sUserId) {
        return m_oBackend.deleteStylePermissionsByUserId(sUserId);
    }

    public int deleteWorkflowPermissionsByUserId(String sUserId) {
        return m_oBackend.deleteWorkflowPermissionsByUserId(sUserId);
    }

    public int deleteProcessorPermissionsByUserId(String sUserId) {
        return m_oBackend.deleteProcessorPermissionsByUserId(sUserId);
    }

    public int deletePermissionsByUserIdAndResourceId(String sUserId, String sResourceId) {
        return m_oBackend.deletePermissionsByUserIdAndResourceId(sUserId, sResourceId);
    }

    public int deletePermissionsByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId) {
        return m_oBackend.deletePermissionsByTypeAndUserIdAndResourceId(sType, sUserId, sResourceId);
    }

    public int deletePermissionsByUserIdAndNodeCode(String sUserId, String sNodeCode) {
        return m_oBackend.deletePermissionsByUserIdAndNodeCode(sUserId, sNodeCode);
    }

    public int deletePermissionsByUserIdAndOrganizationId(String sUserId, String sOrganizationId) {
        return m_oBackend.deletePermissionsByUserIdAndOrganizationId(sUserId, sOrganizationId);
    }

    public int deletePermissionsByUserIdAndSubscriptionId(String sUserId, String sSubscriptionId) {
        return m_oBackend.deletePermissionsByUserIdAndSubscriptionId(sUserId, sSubscriptionId);
    }

    public int deletePermissionsByUserIdAndWorkspaceId(String sUserId, String sWorkspaceId) {
        return m_oBackend.deletePermissionsByUserIdAndWorkspaceId(sUserId, sWorkspaceId);
    }

    public int deletePermissionsByUserIdAndStyleId(String sUserId, String sStyleId) {
        return m_oBackend.deletePermissionsByUserIdAndStyleId(sUserId, sStyleId);
    }

    public int deletePermissionsByUserIdAndWorkflowId(String sUserId, String sWorkflowId) {
        return m_oBackend.deletePermissionsByUserIdAndWorkflowId(sUserId, sWorkflowId);
    }

    public int deletePermissionsByUserIdAndProcessorId(String sUserId, String sProcessorId) {
        return m_oBackend.deletePermissionsByUserIdAndProcessorId(sUserId, sProcessorId);
    }

    public int deletePermissionsByUserIdAndProcessorParametersTemplateId(String sUserId, String sProcessorParametersTemplateId) {
        return m_oBackend.deletePermissionsByUserIdAndProcessorParametersTemplateId(sUserId, sProcessorParametersTemplateId);
    }

    public boolean isResourceSharedWithUser(String sUserId, String sResourceId) {
        return m_oBackend.isResourceSharedWithUser(sUserId, sResourceId);
    }

    public boolean isResourceOfTypeSharedWithUser(String sType, String sUserId, String sResourceId) {
        return m_oBackend.isResourceOfTypeSharedWithUser(sType, sUserId, sResourceId);
    }

    public boolean isNodeSharedWithUser(String sUserId, String sNodeCode) {
        return m_oBackend.isNodeSharedWithUser(sUserId, sNodeCode);
    }

    public boolean isOrganizationSharedWithUser(String sUserId, String sOrganizationId) {
        return m_oBackend.isOrganizationSharedWithUser(sUserId, sOrganizationId);
    }

    public boolean isOrganizationShared(String sOrganizationId) {
        return m_oBackend.isOrganizationShared(sOrganizationId);
    }

    public boolean isSubscriptionShared(String sSubscriptionId) {
        return m_oBackend.isSubscriptionShared(sSubscriptionId);
    }

    public boolean isSubscriptionSharedWithUser(String sUserId, String sSubscriptionId) {
        return m_oBackend.isSubscriptionSharedWithUser(sUserId, sSubscriptionId);
    }

    public boolean isWorkspaceSharedWithUser(String sUserId, String sWorkspaceId) {
        return m_oBackend.isWorkspaceSharedWithUser(sUserId, sWorkspaceId);
    }

    public boolean isStyleSharedWithUser(String sUserId, String sStyleId) {
        return m_oBackend.isStyleSharedWithUser(sUserId, sStyleId);
    }

    public boolean isWorkflowSharedWithUser(String sUserId, String sWorkflowId) {
        return m_oBackend.isWorkflowSharedWithUser(sUserId, sWorkflowId);
    }

    public boolean isProcessorSharedWithUser(String sUserId, String sProcessorId) {
        return m_oBackend.isProcessorSharedWithUser(sUserId, sProcessorId);
    }

    public boolean isProcessorParametersTemplateSharedWithUser(String sUserId, String sProcessorParametersTemplateId) {
        return m_oBackend.isProcessorParametersTemplateSharedWithUser(sUserId, sProcessorParametersTemplateId);
    }

}

