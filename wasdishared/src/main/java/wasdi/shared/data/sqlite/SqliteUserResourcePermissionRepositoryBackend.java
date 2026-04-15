package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.data.interfaces.IUserResourcePermissionRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class SqliteUserResourcePermissionRepositoryBackend extends SqliteRepository implements IUserResourcePermissionRepositoryBackend {

    public SqliteUserResourcePermissionRepositoryBackend() {
        m_sThisCollection = "userresourcepermissions";
        this.ensureTable(m_sThisCollection);
    }

    public boolean insertPermission(UserResourcePermission oUserResourcePermission) {
        try {
            if (oUserResourcePermission == null) {
                return false;
            }
            // No natural single key in entity; use generated row id as PK.
            return insert(UUID.randomUUID().toString(), oUserResourcePermission);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.insertPermission : exception ", oEx);
        }

        return false;
    }

    public List<UserResourcePermission> getPermissionsByOwnerId(String sUserId) {
        try {
            return findAllWhere("ownerId", sUserId, UserResourcePermission.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByOwnerId : exception ", oEx);
        }

        return new ArrayList<>();
    }

    public List<UserResourcePermission> getPermissionsByTypeAndOwnerId(String sType, String sUserId) {
        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("resourceType", sType);
            oFilter.put("ownerId", sUserId);
            return findAllWhere(oFilter, UserResourcePermission.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByTypeAndOwnerId : exception ", oEx);
        }

        return new ArrayList<>();
    }

    public List<UserResourcePermission> getOrganizationSharingsByOwnerId(String sUserId) {
        return getPermissionsByTypeAndOwnerId(ResourceTypes.ORGANIZATION.getResourceType(), sUserId);
    }

    public List<UserResourcePermission> getSubscriptionSharingsByOwnerId(String sUserId) {
        return getPermissionsByTypeAndOwnerId(ResourceTypes.SUBSCRIPTION.getResourceType(), sUserId);
    }

    public List<UserResourcePermission> getWorkspaceSharingsByOwnerId(String sUserId) {
        return getPermissionsByTypeAndOwnerId(ResourceTypes.WORKSPACE.getResourceType(), sUserId);
    }

    public List<UserResourcePermission> getStyleSharingsByOwnerId(String sUserId) {
        return getPermissionsByTypeAndOwnerId(ResourceTypes.STYLE.getResourceType(), sUserId);
    }

    public List<UserResourcePermission> getWorkflowSharingsByOwnerId(String sUserId) {
        return getPermissionsByTypeAndOwnerId(ResourceTypes.WORKFLOW.getResourceType(), sUserId);
    }

    public List<UserResourcePermission> getProcessorSharingsByOwnerId(String sUserId) {
        return getPermissionsByTypeAndOwnerId(ResourceTypes.PROCESSOR.getResourceType(), sUserId);
    }

    public List<UserResourcePermission> getPermissionsByUserId(String sUserId) {
        try {
            return findAllWhere("userId", sUserId, UserResourcePermission.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByUserId : exception ", oEx);
        }

        return new ArrayList<>();
    }

    public List<UserResourcePermission> getPermissionsByTypeAndUserId(String sType, String sUserId) {
        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("resourceType", sType);
            oFilter.put("userId", sUserId);
            return findAllWhere(oFilter, UserResourcePermission.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByTypeAndUserId : exception ", oEx);
        }

        return new ArrayList<>();
    }

    public List<UserResourcePermission> getOrganizationSharingsByUserId(String sUserId) {
        return getPermissionsByTypeAndUserId(ResourceTypes.ORGANIZATION.getResourceType(), sUserId);
    }

    public List<UserResourcePermission> getSubscriptionSharingsByUserId(String sUserId) {
        return getPermissionsByTypeAndUserId(ResourceTypes.SUBSCRIPTION.getResourceType(), sUserId);
    }

    public List<UserResourcePermission> getWorkspaceSharingsByUserId(String sUserId) {
        return getPermissionsByTypeAndUserId(ResourceTypes.WORKSPACE.getResourceType(), sUserId);
    }

    public List<UserResourcePermission> getStyleSharingsByUserId(String sUserId) {
        return getPermissionsByTypeAndUserId(ResourceTypes.STYLE.getResourceType(), sUserId);
    }

    public List<UserResourcePermission> getWorkflowSharingsByUserId(String sUserId) {
        return getPermissionsByTypeAndUserId(ResourceTypes.WORKFLOW.getResourceType(), sUserId);
    }

    public List<UserResourcePermission> getProcessorSharingsByUserId(String sUserId) {
        return getPermissionsByTypeAndUserId(ResourceTypes.PROCESSOR.getResourceType(), sUserId);
    }

    public List<UserResourcePermission> getProcessorParametersTemplateSharingsByUserId(String sUserId) {
        return getPermissionsByTypeAndUserId(ResourceTypes.PARAMETER.getResourceType(), sUserId);
    }

    public List<UserResourcePermission> getMissionsharingByUserId(String sUserId) {
        return getPermissionsByTypeAndUserId(ResourceTypes.MISSION.getResourceType(), sUserId);
    }

    public List<UserResourcePermission> getPermissionsByResourceId(String sResourceId) {
        try {
            return findAllWhere("resourceId", sResourceId, UserResourcePermission.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByResourceId : exception ", oEx);
        }

        return new ArrayList<>();
    }

    public List<UserResourcePermission> getPermissionsByTypeAndResourceId(String sType, String sResourceId) {
        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("resourceType", sType);
            oFilter.put("resourceId", sResourceId);
            return findAllWhere(oFilter, UserResourcePermission.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByTypeAndResourceId : exception ", oEx);
        }

        return new ArrayList<>();
    }

    public boolean isResourceShared(String sType, String sResourceId) {
        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("resourceType", sType);
            oFilter.put("resourceId", sResourceId);
            return countWhere(oFilter) > 0;
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.isResourceShared : exception ", oEx);
        }

        // Keep legacy behavior on failure.
        return true;
    }

    public List<UserResourcePermission> getPermissionsByTypeAndResourceIds(String sType, List<String> asResourceIds) {
        final List<UserResourcePermission> aoReturnList = new ArrayList<>();

        try {
            if (asResourceIds == null || asResourceIds.isEmpty()) {
                return aoReturnList;
            }

            StringBuilder oSql = new StringBuilder("SELECT data FROM ").append(m_sThisCollection)
                    .append(" WHERE json_extract(data,'$.resourceType') = ?")
                    .append(" AND json_extract(data,'$.resourceId') IN (");
            for (int i = 0; i < asResourceIds.size(); i++) {
                if (i > 0) oSql.append(',');
                oSql.append('?');
            }
            oSql.append(')');

            List<Object> aoParams = new ArrayList<>();
            aoParams.add(sType);
            aoParams.addAll(asResourceIds);

            return queryList(oSql.toString(), aoParams.toArray(), UserResourcePermission.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByTypeAndResourceIds : exception ", oEx);
        }

        return aoReturnList;
    }

    public List<UserResourcePermission> getNodeSharingsByNodeCode(String sNodeCode) {
        return getPermissionsByTypeAndResourceId(ResourceTypes.NODE.getResourceType(), sNodeCode);
    }

    public List<UserResourcePermission> getOrganizationSharingsByOrganizationId(String sOrganizationId) {
        return getPermissionsByTypeAndResourceId(ResourceTypes.ORGANIZATION.getResourceType(), sOrganizationId);
    }

    public List<UserResourcePermission> getOrganizationSharingsByOrganizationIds(List<String> asOrganizationIds) {
        return getPermissionsByTypeAndResourceIds(ResourceTypes.ORGANIZATION.getResourceType(), asOrganizationIds);
    }

    public List<UserResourcePermission> getSubscriptionSharingsBySubscriptionId(String sSubscriptionId) {
        return getPermissionsByTypeAndResourceId(ResourceTypes.SUBSCRIPTION.getResourceType(), sSubscriptionId);
    }

    public List<UserResourcePermission> getWorkspaceSharingsByWorkspaceId(String sWorkspaceId) {
        return getPermissionsByTypeAndResourceId(ResourceTypes.WORKSPACE.getResourceType(), sWorkspaceId);
    }

    public List<UserResourcePermission> getStyleSharingsByStyleId(String sStyleId) {
        return getPermissionsByTypeAndResourceId(ResourceTypes.STYLE.getResourceType(), sStyleId);
    }

    public List<UserResourcePermission> getWorkflowSharingsByWorkflowId(String sWorkflowId) {
        return getPermissionsByTypeAndResourceId(ResourceTypes.WORKFLOW.getResourceType(), sWorkflowId);
    }

    public List<UserResourcePermission> getProcessorSharingsByProcessorId(String sProcessorId) {
        return getPermissionsByTypeAndResourceId(ResourceTypes.PROCESSOR.getResourceType(), sProcessorId);
    }

    public List<UserResourcePermission> getProcessorParametersTemplateSharingsByProcessorParametersTemplateId(String sProcessorParametersTemplateId) {
        return getPermissionsByTypeAndResourceId(ResourceTypes.PARAMETER.getResourceType(), sProcessorParametersTemplateId);
    }

    public UserResourcePermission getPermissionByUserIdAndResourceId(String sUserId, String sResourceId) {
        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("userId", sUserId);
            oFilter.put("resourceId", sResourceId);
            return findOneWhere(oFilter, UserResourcePermission.class);
        } catch (Exception oE) {
            WasdiLog.debugLog("UserResourcePermissionRepository.getPermissionByUserIdAndResourceId( " + sUserId + ", "
                    + sResourceId + "): error: " + oE);
        }

        return null;
    }

    public UserResourcePermission getPermissionByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId) {
        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("resourceType", sType);
            oFilter.put("userId", sUserId);
            oFilter.put("resourceId", sResourceId);
            return findOneWhere(oFilter, UserResourcePermission.class);
        } catch (Exception oE) {
            WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionByTypeAndUserIdAndResourceId( " + sUserId
                    + ", " + sResourceId + "): error: ", oE);
        }

        return null;
    }

    public List<UserResourcePermission> getPermissionsByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId) {
        List<UserResourcePermission> aoReturnList = new ArrayList<>();

        try {
            StringBuilder oSql = new StringBuilder("SELECT data FROM ").append(m_sThisCollection).append(" WHERE 1=1");
            List<Object> aoParams = new ArrayList<>();

            if (!Utils.isNullOrEmpty(sType)) {
                oSql.append(" AND LOWER(json_extract(data,'$.resourceType')) = LOWER(?)");
                aoParams.add(sType.toLowerCase());
            }

            if (!Utils.isNullOrEmpty(sResourceId)) {
                oSql.append(" AND LOWER(json_extract(data,'$.resourceId')) LIKE LOWER(?)");
                aoParams.add("%" + sResourceId + "%");
            }

            if (!Utils.isNullOrEmpty(sUserId)) {
                oSql.append(" AND LOWER(json_extract(data,'$.userId')) = LOWER(?)");
                aoParams.add(sUserId.toLowerCase());
            }

            aoReturnList.addAll(queryList(oSql.toString(), aoParams.toArray(), UserResourcePermission.class));
        } catch (Exception oE) {
            WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByTypeAndUserIdAndResourceId( " + sUserId
                    + ", " + sResourceId + "): error: ", oE);
        }

        return aoReturnList;
    }

    public UserResourcePermission getOrganizationSharingByUserIdAndOrganizationId(String sUserId, String sOrganizationId) {
        return getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.ORGANIZATION.getResourceType(), sUserId, sOrganizationId);
    }

    public UserResourcePermission getSubscriptionSharingByUserIdAndSubscriptionId(String sUserId, String sSubscriptionId) {
        return getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.SUBSCRIPTION.getResourceType(), sUserId, sSubscriptionId);
    }

    public UserResourcePermission getWorkspaceSharingByUserIdAndWorkspaceId(String sUserId, String sWorkspaceId) {
        return getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.WORKSPACE.getResourceType(), sUserId, sWorkspaceId);
    }

    public UserResourcePermission getStyleSharingByUserIdAndStyleId(String sUserId, String sStyleId) {
        return getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.STYLE.getResourceType(), sUserId, sStyleId);
    }

    public UserResourcePermission getWorkflowSharingByUserIdAndWorkflowId(String sUserId, String sWorkflowId) {
        return getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.WORKFLOW.getResourceType(), sUserId, sWorkflowId);
    }

    public UserResourcePermission getProcessorSharingByUserIdAndProcessorId(String sUserId, String sProcessorId) {
        return getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.PROCESSOR.getResourceType(), sUserId, sProcessorId);
    }

    public UserResourcePermission getProcessorParametersTemplateSharingByUserIdAndProcessorParametersTemplateId(String sUserId, String sProcessorParametersTemplateId) {
        return getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.PARAMETER.getResourceType(), sUserId, sProcessorParametersTemplateId);
    }

    public List<UserResourcePermission> getPermissions() {
        try {
            return findAll(UserResourcePermission.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.getPermissions : exception ", oEx);
        }

        return new ArrayList<>();
    }

    public List<UserResourcePermission> getPermissionsByType(String sType) {
        try {
            return findAllWhere("resourceType", sType, UserResourcePermission.class);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByType : exception ", oEx);
        }

        return new ArrayList<>();
    }

    public List<UserResourcePermission> getNodeSharings() {
        return getPermissionsByType(ResourceTypes.NODE.getResourceType());
    }

    public List<UserResourcePermission> getOrganizationSharings() {
        return getPermissionsByType(ResourceTypes.ORGANIZATION.getResourceType());
    }

    public List<UserResourcePermission> getSubscriptionSharings() {
        return getPermissionsByType(ResourceTypes.SUBSCRIPTION.getResourceType());
    }

    public List<UserResourcePermission> getWorkspaceSharings() {
        return getPermissionsByType(ResourceTypes.WORKSPACE.getResourceType());
    }

    public List<UserResourcePermission> getStyleSharings() {
        return getPermissionsByType(ResourceTypes.STYLE.getResourceType());
    }

    public List<UserResourcePermission> getWorkflowSharings() {
        return getPermissionsByType(ResourceTypes.WORKFLOW.getResourceType());
    }

    public List<UserResourcePermission> getProcessorSharings() {
        return getPermissionsByType(ResourceTypes.PROCESSOR.getResourceType());
    }

    public List<UserResourcePermission> getProcessorParametersTemplateSharings() {
        return getPermissionsByType(ResourceTypes.PARAMETER.getResourceType());
    }

    public int deletePermissionsByResourceId(String sResourceId) {
        if (Utils.isNullOrEmpty(sResourceId)) {
            return 0;
        }

        try {
            return deleteWhere("resourceId", sResourceId);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.deletePermissionsByResourceId : exception ", oEx);
        }

        return 0;
    }

    public int deletePermissionsByTypeAndResourceId(String sType, String sResourceId) {
        if (Utils.isNullOrEmpty(sType)) {
            return 0;
        }

        if (Utils.isNullOrEmpty(sResourceId)) {
            return 0;
        }

        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("resourceType", sType);
            oFilter.put("resourceId", sResourceId);
            return deleteWhere(oFilter);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.deletePermissionsByTypeAndResourceId : exception ", oEx);
        }

        return 0;
    }

    public int deletePermissionsByTypeAndUserId(String sType, String sResourceId) {
        if (Utils.isNullOrEmpty(sType)) {
            return 0;
        }

        if (Utils.isNullOrEmpty(sResourceId)) {
            return 0;
        }

        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("resourceType", sType);
            oFilter.put("userId", sResourceId);
            return deleteWhere(oFilter);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.deletePermissionsByTypeAndResourceId : exception ", oEx);
        }

        return 0;
    }

    public int deletePermissionsByNodeCode(String sNodeCode) {
        return deletePermissionsByTypeAndResourceId(ResourceTypes.NODE.getResourceType(), sNodeCode);
    }

    public int deletePermissionsByOrganizationId(String sOrganizationId) {
        return deletePermissionsByTypeAndResourceId(ResourceTypes.ORGANIZATION.getResourceType(), sOrganizationId);
    }

    public int deletePermissionsBySubscriptionId(String sSubscriptionId) {
        return deletePermissionsByTypeAndResourceId(ResourceTypes.SUBSCRIPTION.getResourceType(), sSubscriptionId);
    }

    public int deletePermissionsByWorkspaceId(String sWorkspaceId) {
        return deletePermissionsByTypeAndResourceId(ResourceTypes.WORKSPACE.getResourceType(), sWorkspaceId);
    }

    public int deletePermissionsByStyleId(String sStyleId) {
        return deletePermissionsByTypeAndResourceId(ResourceTypes.STYLE.getResourceType(), sStyleId);
    }

    public int deletePermissionsByWorkflowId(String sWorkflowId) {
        return deletePermissionsByTypeAndResourceId(ResourceTypes.WORKFLOW.getResourceType(), sWorkflowId);
    }

    public int deletePermissionsByProcessorId(String sProcessorId) {
        return deletePermissionsByTypeAndResourceId(ResourceTypes.PROCESSOR.getResourceType(), sProcessorId);
    }

    public int deletePermissionsByProcessorParameterTemplateId(String sProcessorParameterTemplateId) {
        return deletePermissionsByTypeAndResourceId(ResourceTypes.PARAMETER.getResourceType(), sProcessorParameterTemplateId);
    }

    /**
     * Deletes all the permissions where User Id is the target
     * @param sUserId
     * @return
     */
    public int deletePermissionsByUserId(String sUserId) {
        if (Utils.isNullOrEmpty(sUserId)) {
            return 0;
        }

        try {
            return deleteWhere("userId", sUserId);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.deletePermissionsByUserId : exception ", oEx);
        }

        return 0;
    }

    /**
     * Deletes all the permissions where User Id is the owner
     * @param sOwnerId
     * @return
     */
    public int deletePermissionsByOwnerId(String sOwnerId) {
        if (Utils.isNullOrEmpty(sOwnerId)) {
            return 0;
        }

        try {
            return deleteWhere("ownerId", sOwnerId);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.deletePermissionsByUserId : exception ", oEx);
        }

        return 0;
    }

    public int deleteOrganizationPermissionsByUserId(String sUserId) {
        return deletePermissionsByTypeAndUserId(ResourceTypes.ORGANIZATION.getResourceType(), sUserId);
    }

    public int deleteSubscriptionPermissionsByUserId(String sUserId) {
        return deletePermissionsByTypeAndUserId(ResourceTypes.SUBSCRIPTION.getResourceType(), sUserId);
    }

    public int deleteWorkspacePermissionsByUserId(String sUserId) {
        return deletePermissionsByTypeAndUserId(ResourceTypes.WORKSPACE.getResourceType(), sUserId);
    }

    public int deleteStylePermissionsByUserId(String sUserId) {
        return deletePermissionsByTypeAndUserId(ResourceTypes.STYLE.getResourceType(), sUserId);
    }

    public int deleteWorkflowPermissionsByUserId(String sUserId) {
        return deletePermissionsByTypeAndUserId(ResourceTypes.WORKFLOW.getResourceType(), sUserId);
    }

    public int deleteProcessorPermissionsByUserId(String sUserId) {
        return deletePermissionsByTypeAndUserId(ResourceTypes.PROCESSOR.getResourceType(), sUserId);
    }

    public int deletePermissionsByUserIdAndResourceId(String sUserId, String sResourceId) {
        if (Utils.isNullOrEmpty(sUserId)) {
            return 0;
        }

        if (Utils.isNullOrEmpty(sResourceId)) {
            return 0;
        }

        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("userId", sUserId);
            oFilter.put("resourceId", sResourceId);
            return deleteWhere(oFilter);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.deletePermissionsByUserIdAndResourceId : exception ", oEx);
        }

        return 0;
    }

    public int deletePermissionsByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId) {
        if (Utils.isNullOrEmpty(sType)) {
            return 0;
        }

        if (Utils.isNullOrEmpty(sUserId)) {
            return 0;
        }

        if (Utils.isNullOrEmpty(sResourceId)) {
            return 0;
        }

        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("resourceType", sType);
            oFilter.put("userId", sUserId);
            oFilter.put("resourceId", sResourceId);
            return deleteWhere(oFilter);
        } catch (Exception oEx) {
            WasdiLog.errorLog("UserResourcePermissionRepository.deletePermissionsByTypeAndUserIdAndResourceId : exception ", oEx);
        }

        return 0;
    }

    public int deletePermissionsByUserIdAndNodeCode(String sUserId, String sNodeCode) {
        return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.NODE.getResourceType(), sUserId, sNodeCode);
    }

    public int deletePermissionsByUserIdAndOrganizationId(String sUserId, String sOrganizationId) {
        return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.ORGANIZATION.getResourceType(), sUserId, sOrganizationId);
    }

    public int deletePermissionsByUserIdAndSubscriptionId(String sUserId, String sSubscriptionId) {
        return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.SUBSCRIPTION.getResourceType(), sUserId, sSubscriptionId);
    }

    public int deletePermissionsByUserIdAndWorkspaceId(String sUserId, String sWorkspaceId) {
        return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.WORKSPACE.getResourceType(), sUserId, sWorkspaceId);
    }

    public int deletePermissionsByUserIdAndStyleId(String sUserId, String sStyleId) {
        return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.STYLE.getResourceType(), sUserId, sStyleId);
    }

    public int deletePermissionsByUserIdAndWorkflowId(String sUserId, String sWorkflowId) {
        return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.WORKFLOW.getResourceType(), sUserId, sWorkflowId);
    }

    public int deletePermissionsByUserIdAndProcessorId(String sUserId, String sProcessorId) {
        return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.PROCESSOR.getResourceType(), sUserId, sProcessorId);
    }

    public int deletePermissionsByUserIdAndProcessorParametersTemplateId(String sUserId, String sProcessorParametersTemplateId) {
        return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.PARAMETER.getResourceType(), sUserId, sProcessorParametersTemplateId);
    }

    public boolean isResourceSharedWithUser(String sUserId, String sResourceId) {
        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("userId", sUserId);
            oFilter.put("resourceId", sResourceId);
            return countWhere(oFilter) > 0;

        } catch (Exception oE) {
            WasdiLog.errorLog("UserResourcePermissionRepository.isResourceSharedWithUser: error: ", oE);
        }

        return false;
    }

    public boolean isResourceOfTypeSharedWithUser(String sType, String sUserId, String sResourceId) {
        try {
            Map<String, Object> oFilter = new HashMap<>();
            oFilter.put("resourceType", sType);
            oFilter.put("userId", sUserId);
            oFilter.put("resourceId", sResourceId);
            return countWhere(oFilter) > 0;

        } catch (Exception oE) {
            WasdiLog.errorLog("UserResourcePermissionRepository.isResourceOfTypeSharedWithUser: error: ", oE);
        }

        return false;
    }

    public boolean isNodeSharedWithUser(String sUserId, String sNodeCode) {
        return isResourceOfTypeSharedWithUser(ResourceTypes.NODE.getResourceType(), sUserId, sNodeCode);
    }

    public boolean isOrganizationSharedWithUser(String sUserId, String sOrganizationId) {
        return isResourceOfTypeSharedWithUser(ResourceTypes.ORGANIZATION.getResourceType(), sUserId, sOrganizationId);
    }

    public boolean isOrganizationShared(String sOrganizationId) {
        return isResourceShared(ResourceTypes.ORGANIZATION.getResourceType(), sOrganizationId);
    }

    public boolean isSubscriptionShared(String sSubscriptionId) {
        return isResourceShared(ResourceTypes.SUBSCRIPTION.getResourceType(), sSubscriptionId);
    }

    public boolean isSubscriptionSharedWithUser(String sUserId, String sSubscriptionId) {
        return isResourceOfTypeSharedWithUser(ResourceTypes.SUBSCRIPTION.getResourceType(), sUserId, sSubscriptionId);
    }

    public boolean isWorkspaceSharedWithUser(String sUserId, String sWorkspaceId) {
        return isResourceOfTypeSharedWithUser(ResourceTypes.WORKSPACE.getResourceType(), sUserId, sWorkspaceId);
    }

    public boolean isStyleSharedWithUser(String sUserId, String sStyleId) {
        return isResourceOfTypeSharedWithUser(ResourceTypes.STYLE.getResourceType(), sUserId, sStyleId);
    }

    public boolean isWorkflowSharedWithUser(String sUserId, String sWorkflowId) {
        return isResourceOfTypeSharedWithUser(ResourceTypes.WORKFLOW.getResourceType(), sUserId, sWorkflowId);
    }

    public boolean isProcessorSharedWithUser(String sUserId, String sProcessorId) {
        return isResourceOfTypeSharedWithUser(ResourceTypes.PROCESSOR.getResourceType(), sUserId, sProcessorId);
    }

    public boolean isProcessorParametersTemplateSharedWithUser(String sUserId, String sProcessorParametersTemplateId) {
        return isResourceOfTypeSharedWithUser(ResourceTypes.PARAMETER.getResourceType(), sUserId, sProcessorParametersTemplateId);
    }
}
