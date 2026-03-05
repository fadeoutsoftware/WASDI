package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.Filter.and;
import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.data.interfaces.IUserResourcePermissionRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for user resource permission repository.
 */
public class No2UserResourcePermissionRepositoryBackend extends No2Repository implements IUserResourcePermissionRepositoryBackend {

	private static final String s_sCollectionName = "userresourcepermissions";

	@Override
	public boolean insertPermission(UserResourcePermission oUserResourcePermission) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oUserResourcePermission == null) {
				return false;
			}
			oCollection.insert(toDocument(oUserResourcePermission));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2UserResourcePermissionRepositoryBackend.insertPermission: exception", oEx);
		}
		return false;
	}

	@Override
	public List<UserResourcePermission> getPermissionsByOwnerId(String sUserId) {
		return filterPermissions(o -> equalsSafe(o.getOwnerId(), sUserId));
	}

	@Override
	public List<UserResourcePermission> getPermissionsByTypeAndOwnerId(String sType, String sUserId) {
		return filterPermissions(o -> equalsSafe(o.getResourceType(), sType) && equalsSafe(o.getOwnerId(), sUserId));
	}

	@Override
	public List<UserResourcePermission> getOrganizationSharingsByOwnerId(String sUserId) {
		return getPermissionsByTypeAndOwnerId(ResourceTypes.ORGANIZATION.getResourceType(), sUserId);
	}

	@Override
	public List<UserResourcePermission> getSubscriptionSharingsByOwnerId(String sUserId) {
		return getPermissionsByTypeAndOwnerId(ResourceTypes.SUBSCRIPTION.getResourceType(), sUserId);
	}

	@Override
	public List<UserResourcePermission> getWorkspaceSharingsByOwnerId(String sUserId) {
		return getPermissionsByTypeAndOwnerId(ResourceTypes.WORKSPACE.getResourceType(), sUserId);
	}

	@Override
	public List<UserResourcePermission> getStyleSharingsByOwnerId(String sUserId) {
		return getPermissionsByTypeAndOwnerId(ResourceTypes.STYLE.getResourceType(), sUserId);
	}

	@Override
	public List<UserResourcePermission> getWorkflowSharingsByOwnerId(String sUserId) {
		return getPermissionsByTypeAndOwnerId(ResourceTypes.WORKFLOW.getResourceType(), sUserId);
	}

	@Override
	public List<UserResourcePermission> getProcessorSharingsByOwnerId(String sUserId) {
		return getPermissionsByTypeAndOwnerId(ResourceTypes.PROCESSOR.getResourceType(), sUserId);
	}

	@Override
	public List<UserResourcePermission> getPermissionsByUserId(String sUserId) {
		return filterPermissions(o -> equalsSafe(o.getUserId(), sUserId));
	}

	@Override
	public List<UserResourcePermission> getPermissionsByTypeAndUserId(String sType, String sUserId) {
		return filterPermissions(o -> equalsSafe(o.getResourceType(), sType) && equalsSafe(o.getUserId(), sUserId));
	}

	@Override
	public List<UserResourcePermission> getOrganizationSharingsByUserId(String sUserId) {
		return getPermissionsByTypeAndUserId(ResourceTypes.ORGANIZATION.getResourceType(), sUserId);
	}

	@Override
	public List<UserResourcePermission> getSubscriptionSharingsByUserId(String sUserId) {
		return getPermissionsByTypeAndUserId(ResourceTypes.SUBSCRIPTION.getResourceType(), sUserId);
	}

	@Override
	public List<UserResourcePermission> getWorkspaceSharingsByUserId(String sUserId) {
		return getPermissionsByTypeAndUserId(ResourceTypes.WORKSPACE.getResourceType(), sUserId);
	}

	@Override
	public List<UserResourcePermission> getStyleSharingsByUserId(String sUserId) {
		return getPermissionsByTypeAndUserId(ResourceTypes.STYLE.getResourceType(), sUserId);
	}

	@Override
	public List<UserResourcePermission> getWorkflowSharingsByUserId(String sUserId) {
		return getPermissionsByTypeAndUserId(ResourceTypes.WORKFLOW.getResourceType(), sUserId);
	}

	@Override
	public List<UserResourcePermission> getProcessorSharingsByUserId(String sUserId) {
		return getPermissionsByTypeAndUserId(ResourceTypes.PROCESSOR.getResourceType(), sUserId);
	}

	@Override
	public List<UserResourcePermission> getProcessorParametersTemplateSharingsByUserId(String sUserId) {
		return getPermissionsByTypeAndUserId(ResourceTypes.PARAMETER.getResourceType(), sUserId);
	}

	@Override
	public List<UserResourcePermission> getMissionsharingByUserId(String sUserId) {
		return getPermissionsByTypeAndUserId(ResourceTypes.MISSION.getResourceType(), sUserId);
	}

	@Override
	public List<UserResourcePermission> getPermissionsByResourceId(String sResourceId) {
		return filterPermissions(o -> equalsSafe(o.getResourceId(), sResourceId));
	}

	@Override
	public List<UserResourcePermission> getPermissionsByTypeAndResourceId(String sType, String sResourceId) {
		return filterPermissions(o -> equalsSafe(o.getResourceType(), sType) && equalsSafe(o.getResourceId(), sResourceId));
	}

	@Override
	public boolean isResourceShared(String sType, String sResourceId) {
		return !getPermissionsByTypeAndResourceId(sType, sResourceId).isEmpty();
	}

	@Override
	public List<UserResourcePermission> getPermissionsByTypeAndResourceIds(String sType, List<String> asResourceIds) {
		if (asResourceIds == null || asResourceIds.isEmpty()) {
			return new ArrayList<>();
		}
		return filterPermissions(o -> equalsSafe(o.getResourceType(), sType) && asResourceIds.contains(o.getResourceId()));
	}

	@Override
	public List<UserResourcePermission> getNodeSharingsByNodeCode(String sNodeCode) {
		return getPermissionsByTypeAndResourceId(ResourceTypes.NODE.getResourceType(), sNodeCode);
	}

	@Override
	public List<UserResourcePermission> getOrganizationSharingsByOrganizationId(String sOrganizationId) {
		return getPermissionsByTypeAndResourceId(ResourceTypes.ORGANIZATION.getResourceType(), sOrganizationId);
	}

	@Override
	public List<UserResourcePermission> getOrganizationSharingsByOrganizationIds(List<String> asOrganizationIds) {
		return getPermissionsByTypeAndResourceIds(ResourceTypes.ORGANIZATION.getResourceType(), asOrganizationIds);
	}

	@Override
	public List<UserResourcePermission> getSubscriptionSharingsBySubscriptionId(String sSubscriptionId) {
		return getPermissionsByTypeAndResourceId(ResourceTypes.SUBSCRIPTION.getResourceType(), sSubscriptionId);
	}

	@Override
	public List<UserResourcePermission> getWorkspaceSharingsByWorkspaceId(String sWorkspaceId) {
		return getPermissionsByTypeAndResourceId(ResourceTypes.WORKSPACE.getResourceType(), sWorkspaceId);
	}

	@Override
	public List<UserResourcePermission> getStyleSharingsByStyleId(String sStyleId) {
		return getPermissionsByTypeAndResourceId(ResourceTypes.STYLE.getResourceType(), sStyleId);
	}

	@Override
	public List<UserResourcePermission> getWorkflowSharingsByWorkflowId(String sWorkflowId) {
		return getPermissionsByTypeAndResourceId(ResourceTypes.WORKFLOW.getResourceType(), sWorkflowId);
	}

	@Override
	public List<UserResourcePermission> getProcessorSharingsByProcessorId(String sProcessorId) {
		return getPermissionsByTypeAndResourceId(ResourceTypes.PROCESSOR.getResourceType(), sProcessorId);
	}

	@Override
	public List<UserResourcePermission> getProcessorParametersTemplateSharingsByProcessorParametersTemplateId(String sProcessorParametersTemplateId) {
		return getPermissionsByTypeAndResourceId(ResourceTypes.PARAMETER.getResourceType(), sProcessorParametersTemplateId);
	}

	@Override
	public UserResourcePermission getPermissionByUserIdAndResourceId(String sUserId, String sResourceId) {
		for (UserResourcePermission oPermission : filterPermissions(o -> equalsSafe(o.getUserId(), sUserId) && equalsSafe(o.getResourceId(), sResourceId))) {
			return oPermission;
		}
		return null;
	}

	@Override
	public UserResourcePermission getPermissionByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId) {
		for (UserResourcePermission oPermission : getPermissionsByTypeAndUserIdAndResourceId(sType, sUserId, sResourceId)) {
			return oPermission;
		}
		return null;
	}

	@Override
	public List<UserResourcePermission> getPermissionsByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId) {
		return filterPermissions(o ->
			equalsSafeIgnoreCase(o.getResourceType(), sType)
				&& equalsSafeIgnoreCase(o.getUserId(), sUserId)
				&& containsSafeIgnoreCase(o.getResourceId(), sResourceId));
	}

	@Override
	public UserResourcePermission getOrganizationSharingByUserIdAndOrganizationId(String sUserId, String sOrganizationId) {
		return getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.ORGANIZATION.getResourceType(), sUserId, sOrganizationId);
	}

	@Override
	public UserResourcePermission getSubscriptionSharingByUserIdAndSubscriptionId(String sUserId, String sSubscriptionId) {
		return getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.SUBSCRIPTION.getResourceType(), sUserId, sSubscriptionId);
	}

	@Override
	public UserResourcePermission getWorkspaceSharingByUserIdAndWorkspaceId(String sUserId, String sWorkspaceId) {
		return getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.WORKSPACE.getResourceType(), sUserId, sWorkspaceId);
	}

	@Override
	public UserResourcePermission getStyleSharingByUserIdAndStyleId(String sUserId, String sStyleId) {
		return getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.STYLE.getResourceType(), sUserId, sStyleId);
	}

	@Override
	public UserResourcePermission getWorkflowSharingByUserIdAndWorkflowId(String sUserId, String sWorkflowId) {
		return getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.WORKFLOW.getResourceType(), sUserId, sWorkflowId);
	}

	@Override
	public UserResourcePermission getProcessorSharingByUserIdAndProcessorId(String sUserId, String sProcessorId) {
		return getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.PROCESSOR.getResourceType(), sUserId, sProcessorId);
	}

	@Override
	public UserResourcePermission getProcessorParametersTemplateSharingByUserIdAndProcessorParametersTemplateId(String sUserId, String sProcessorParametersTemplateId) {
		return getPermissionByTypeAndUserIdAndResourceId(ResourceTypes.PARAMETER.getResourceType(), sUserId, sProcessorParametersTemplateId);
	}

	@Override
	public List<UserResourcePermission> getPermissions() {
		return getAllPermissions();
	}

	@Override
	public List<UserResourcePermission> getPermissionsByType(String sType) {
		return filterPermissions(o -> equalsSafe(o.getResourceType(), sType));
	}

	@Override
	public List<UserResourcePermission> getNodeSharings() {
		return getPermissionsByType(ResourceTypes.NODE.getResourceType());
	}

	@Override
	public List<UserResourcePermission> getOrganizationSharings() {
		return getPermissionsByType(ResourceTypes.ORGANIZATION.getResourceType());
	}

	@Override
	public List<UserResourcePermission> getSubscriptionSharings() {
		return getPermissionsByType(ResourceTypes.SUBSCRIPTION.getResourceType());
	}

	@Override
	public List<UserResourcePermission> getWorkspaceSharings() {
		return getPermissionsByType(ResourceTypes.WORKSPACE.getResourceType());
	}

	@Override
	public List<UserResourcePermission> getStyleSharings() {
		return getPermissionsByType(ResourceTypes.STYLE.getResourceType());
	}

	@Override
	public List<UserResourcePermission> getWorkflowSharings() {
		return getPermissionsByType(ResourceTypes.WORKFLOW.getResourceType());
	}

	@Override
	public List<UserResourcePermission> getProcessorSharings() {
		return getPermissionsByType(ResourceTypes.PROCESSOR.getResourceType());
	}

	@Override
	public List<UserResourcePermission> getProcessorParametersTemplateSharings() {
		return getPermissionsByType(ResourceTypes.PARAMETER.getResourceType());
	}

	@Override
	public int deletePermissionsByResourceId(String sResourceId) {
		return deleteByPredicate(o -> equalsSafe(o.getResourceId(), sResourceId));
	}

	@Override
	public int deletePermissionsByTypeAndResourceId(String sType, String sResourceId) {
		return deleteByPredicate(o -> equalsSafe(o.getResourceType(), sType) && equalsSafe(o.getResourceId(), sResourceId));
	}

	@Override
	public int deletePermissionsByTypeAndUserId(String sType, String sResourceId) {
		return deleteByPredicate(o -> equalsSafe(o.getResourceType(), sType) && equalsSafe(o.getUserId(), sResourceId));
	}

	@Override
	public int deletePermissionsByNodeCode(String sNodeCode) {
		return deletePermissionsByTypeAndResourceId(ResourceTypes.NODE.getResourceType(), sNodeCode);
	}

	@Override
	public int deletePermissionsByOrganizationId(String sOrganizationId) {
		return deletePermissionsByTypeAndResourceId(ResourceTypes.ORGANIZATION.getResourceType(), sOrganizationId);
	}

	@Override
	public int deletePermissionsBySubscriptionId(String sSubscriptionId) {
		return deletePermissionsByTypeAndResourceId(ResourceTypes.SUBSCRIPTION.getResourceType(), sSubscriptionId);
	}

	@Override
	public int deletePermissionsByWorkspaceId(String sWorkspaceId) {
		return deletePermissionsByTypeAndResourceId(ResourceTypes.WORKSPACE.getResourceType(), sWorkspaceId);
	}

	@Override
	public int deletePermissionsByStyleId(String sStyleId) {
		return deletePermissionsByTypeAndResourceId(ResourceTypes.STYLE.getResourceType(), sStyleId);
	}

	@Override
	public int deletePermissionsByWorkflowId(String sWorkflowId) {
		return deletePermissionsByTypeAndResourceId(ResourceTypes.WORKFLOW.getResourceType(), sWorkflowId);
	}

	@Override
	public int deletePermissionsByProcessorId(String sProcessorId) {
		return deletePermissionsByTypeAndResourceId(ResourceTypes.PROCESSOR.getResourceType(), sProcessorId);
	}

	@Override
	public int deletePermissionsByProcessorParameterTemplateId(String sProcessorParameterTemplateId) {
		return deletePermissionsByTypeAndResourceId(ResourceTypes.PARAMETER.getResourceType(), sProcessorParameterTemplateId);
	}

	@Override
	public int deletePermissionsByUserId(String sUserId) {
		return deleteByPredicate(o -> equalsSafe(o.getUserId(), sUserId));
	}

	@Override
	public int deletePermissionsByOwnerId(String sOwnerId) {
		return deleteByPredicate(o -> equalsSafe(o.getOwnerId(), sOwnerId));
	}

	@Override
	public int deleteOrganizationPermissionsByUserId(String sUserId) {
		return deletePermissionsByTypeAndUserId(ResourceTypes.ORGANIZATION.getResourceType(), sUserId);
	}

	@Override
	public int deleteSubscriptionPermissionsByUserId(String sUserId) {
		return deletePermissionsByTypeAndUserId(ResourceTypes.SUBSCRIPTION.getResourceType(), sUserId);
	}

	@Override
	public int deleteWorkspacePermissionsByUserId(String sUserId) {
		return deletePermissionsByTypeAndUserId(ResourceTypes.WORKSPACE.getResourceType(), sUserId);
	}

	@Override
	public int deleteStylePermissionsByUserId(String sUserId) {
		return deletePermissionsByTypeAndUserId(ResourceTypes.STYLE.getResourceType(), sUserId);
	}

	@Override
	public int deleteWorkflowPermissionsByUserId(String sUserId) {
		return deletePermissionsByTypeAndUserId(ResourceTypes.WORKFLOW.getResourceType(), sUserId);
	}

	@Override
	public int deleteProcessorPermissionsByUserId(String sUserId) {
		return deletePermissionsByTypeAndUserId(ResourceTypes.PROCESSOR.getResourceType(), sUserId);
	}

	@Override
	public int deletePermissionsByUserIdAndResourceId(String sUserId, String sResourceId) {
		return deleteByPredicate(o -> equalsSafe(o.getUserId(), sUserId) && equalsSafe(o.getResourceId(), sResourceId));
	}

	@Override
	public int deletePermissionsByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId) {
		return deleteByPredicate(o ->
			equalsSafe(o.getResourceType(), sType)
				&& equalsSafe(o.getUserId(), sUserId)
				&& equalsSafe(o.getResourceId(), sResourceId));
	}

	@Override
	public int deletePermissionsByUserIdAndNodeCode(String sUserId, String sNodeCode) {
		return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.NODE.getResourceType(), sUserId, sNodeCode);
	}

	@Override
	public int deletePermissionsByUserIdAndOrganizationId(String sUserId, String sOrganizationId) {
		return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.ORGANIZATION.getResourceType(), sUserId, sOrganizationId);
	}

	@Override
	public int deletePermissionsByUserIdAndSubscriptionId(String sUserId, String sSubscriptionId) {
		return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.SUBSCRIPTION.getResourceType(), sUserId, sSubscriptionId);
	}

	@Override
	public int deletePermissionsByUserIdAndWorkspaceId(String sUserId, String sWorkspaceId) {
		return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.WORKSPACE.getResourceType(), sUserId, sWorkspaceId);
	}

	@Override
	public int deletePermissionsByUserIdAndStyleId(String sUserId, String sStyleId) {
		return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.STYLE.getResourceType(), sUserId, sStyleId);
	}

	@Override
	public int deletePermissionsByUserIdAndWorkflowId(String sUserId, String sWorkflowId) {
		return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.WORKFLOW.getResourceType(), sUserId, sWorkflowId);
	}

	@Override
	public int deletePermissionsByUserIdAndProcessorId(String sUserId, String sProcessorId) {
		return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.PROCESSOR.getResourceType(), sUserId, sProcessorId);
	}

	@Override
	public int deletePermissionsByUserIdAndProcessorParametersTemplateId(String sUserId, String sProcessorParametersTemplateId) {
		return deletePermissionsByTypeAndUserIdAndResourceId(ResourceTypes.PARAMETER.getResourceType(), sUserId, sProcessorParametersTemplateId);
	}

	@Override
	public boolean isResourceSharedWithUser(String sUserId, String sResourceId) {
		return getPermissionByUserIdAndResourceId(sUserId, sResourceId) != null;
	}

	@Override
	public boolean isResourceOfTypeSharedWithUser(String sType, String sUserId, String sResourceId) {
		return getPermissionByTypeAndUserIdAndResourceId(sType, sUserId, sResourceId) != null;
	}

	@Override
	public boolean isNodeSharedWithUser(String sUserId, String sNodeCode) {
		return isResourceOfTypeSharedWithUser(ResourceTypes.NODE.getResourceType(), sUserId, sNodeCode);
	}

	@Override
	public boolean isOrganizationSharedWithUser(String sUserId, String sOrganizationId) {
		return isResourceOfTypeSharedWithUser(ResourceTypes.ORGANIZATION.getResourceType(), sUserId, sOrganizationId);
	}

	@Override
	public boolean isOrganizationShared(String sOrganizationId) {
		return isResourceShared(ResourceTypes.ORGANIZATION.getResourceType(), sOrganizationId);
	}

	@Override
	public boolean isSubscriptionShared(String sSubscriptionId) {
		return isResourceShared(ResourceTypes.SUBSCRIPTION.getResourceType(), sSubscriptionId);
	}

	@Override
	public boolean isSubscriptionSharedWithUser(String sUserId, String sSubscriptionId) {
		return isResourceOfTypeSharedWithUser(ResourceTypes.SUBSCRIPTION.getResourceType(), sUserId, sSubscriptionId);
	}

	@Override
	public boolean isWorkspaceSharedWithUser(String sUserId, String sWorkspaceId) {
		return isResourceOfTypeSharedWithUser(ResourceTypes.WORKSPACE.getResourceType(), sUserId, sWorkspaceId);
	}

	@Override
	public boolean isStyleSharedWithUser(String sUserId, String sStyleId) {
		return isResourceOfTypeSharedWithUser(ResourceTypes.STYLE.getResourceType(), sUserId, sStyleId);
	}

	@Override
	public boolean isWorkflowSharedWithUser(String sUserId, String sWorkflowId) {
		return isResourceOfTypeSharedWithUser(ResourceTypes.WORKFLOW.getResourceType(), sUserId, sWorkflowId);
	}

	@Override
	public boolean isProcessorSharedWithUser(String sUserId, String sProcessorId) {
		return isResourceOfTypeSharedWithUser(ResourceTypes.PROCESSOR.getResourceType(), sUserId, sProcessorId);
	}

	@Override
	public boolean isProcessorParametersTemplateSharedWithUser(String sUserId, String sProcessorParametersTemplateId) {
		return isResourceOfTypeSharedWithUser(ResourceTypes.PARAMETER.getResourceType(), sUserId, sProcessorParametersTemplateId);
	}

	private List<UserResourcePermission> getAllPermissions() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return new ArrayList<>();
			}
			DocumentCursor oCursor = oCollection.find();
			return toList(oCursor, UserResourcePermission.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2UserResourcePermissionRepositoryBackend.getAllPermissions: exception", oEx);
			return new ArrayList<>();
		}
	}

	private List<UserResourcePermission> filterPermissions(Predicate<UserResourcePermission> oPredicate) {
		List<UserResourcePermission> aoReturn = new ArrayList<>();
		for (UserResourcePermission oPermission : getAllPermissions()) {
			if (oPermission != null && oPredicate.test(oPermission)) {
				aoReturn.add(oPermission);
			}
		}
		return aoReturn;
	}

	private int deleteByPredicate(Predicate<UserResourcePermission> oPredicate) {
		List<UserResourcePermission> aoToDelete = filterPermissions(oPredicate);
		if (aoToDelete.isEmpty()) {
			return 0;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return 0;
			}

			for (UserResourcePermission oPermission : aoToDelete) {
				oCollection.remove(
					and(
						where("resourceType").eq(oPermission.getResourceType()),
						where("resourceId").eq(oPermission.getResourceId()),
						where("userId").eq(oPermission.getUserId()),
						where("ownerId").eq(oPermission.getOwnerId())));
			}
			return aoToDelete.size();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2UserResourcePermissionRepositoryBackend.deleteByPredicate: exception", oEx);
			return 0;
		}
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}

	private boolean equalsSafeIgnoreCase(String sA, String sB) {
		return sA == null ? sB == null : sA.equalsIgnoreCase(sB);
	}

	private boolean containsSafeIgnoreCase(String sA, String sLookup) {
		if (Utils.isNullOrEmpty(sLookup)) {
			return true;
		}
		if (sA == null) {
			return false;
		}
		return sA.toLowerCase().contains(sLookup.toLowerCase());
	}
}
