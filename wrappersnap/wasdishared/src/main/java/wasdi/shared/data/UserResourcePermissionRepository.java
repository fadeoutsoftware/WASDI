package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class UserResourcePermissionRepository extends MongoRepository {

	public UserResourcePermissionRepository() {
		m_sThisCollection = "userresourcepermissions";
	}

	public boolean insertPermission(UserResourcePermission oUserResourcePermission) {
		try {
			String sJSON = s_oMapper.writeValueAsString(oUserResourcePermission);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserResourcePermissionRepository.insertPermission : exception ", oEx);
		}

		return false;
	}



	public List<UserResourcePermission> getPermissionsByOwnerId(String sUserId) {
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("ownerId", sUserId));

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByOwnerId : exception ", oEx);
		}

		return aoReturnList;
	}

	public List<UserResourcePermission> getPermissionsByTypeAndOwnerId(String sType, String sUserId) {
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("resourceType", sType), Filters.eq("ownerId", sUserId)));

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByTypeAndOwnerId : exception ", oEx);
		}

		return aoReturnList;
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
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("userId", sUserId));

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByUserId : exception ", oEx);
		}

		return aoReturnList;
	}

	public List<UserResourcePermission> getPermissionsByTypeAndUserId(String sType, String sUserId) {
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("resourceType", sType), Filters.eq("userId", sUserId)));

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByTypeAndUserId : exception ", oEx);
		}

		return aoReturnList;
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
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("resourceId", sResourceId));

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByResourceId : exception ", oEx);
		}

		return aoReturnList;
	}

	public List<UserResourcePermission> getPermissionsByTypeAndResourceId(String sType, String sResourceId) {
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("resourceType", sType), Filters.eq("resourceId", sResourceId)));

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByTypeAndResourceId : exception ", oEx);
		}

		return aoReturnList;
	}

	public boolean isResourceShared(String sType, String sResourceId) {
		try {
			long lCounter = getCollection(m_sThisCollection)
					.countDocuments(Filters.and(Filters.eq("resourceType", sType), Filters.eq("resourceId", sResourceId)));

			return lCounter > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserResourcePermissionRepository.isResourceShared : exception ", oEx);
		}

		return true;
	}

	public List<UserResourcePermission> getPermissionsByTypeAndResourceIds(String sType, List<String> asResourceIds) {
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("resourceType", sType), Filters.in("resourceId", asResourceIds)));

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
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
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("resourceId", sResourceId)))
					.first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();
				UserResourcePermission oStyleSharing;

				try {
					oStyleSharing = s_oMapper.readValue(sJSON, UserResourcePermission.class);
					return oStyleSharing;
				} catch (IOException e) {
					WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionByUserIdAndResourceId : exception ", e);
				}
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("UserResourcePermissionRepository.getPermissionByUserIdAndResourceId( " + sUserId + ", "
					+ sResourceId + "): error: " + oE);
		}

		return null;
	}

	public UserResourcePermission getPermissionByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId) {

		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("resourceType", sType), Filters.eq("userId", sUserId), Filters.eq("resourceId", sResourceId)))
					.first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();
				UserResourcePermission oResourceSharing;

				try {
					oResourceSharing = s_oMapper.readValue(sJSON, UserResourcePermission.class);
					return oResourceSharing;
				} catch (IOException e) {
					WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionByTypeAndUserIdAndResourceId : exception ", e);
				}
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionByTypeAndUserIdAndResourceId( " + sUserId
					+ ", " + sResourceId + "): error: ", oE);
		}

		return null;
	}

	public List<UserResourcePermission> getPermissionsByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId) {
		List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			List<Bson> aoFilters = new ArrayList<>();

			if (!Utils.isNullOrEmpty(sType)) {
				Bson oFilterResourceType = Filters.eq("resourceType", sType.toLowerCase());
				aoFilters.add(oFilterResourceType);
			}

			if (!Utils.isNullOrEmpty(sResourceId)) {
				Pattern regex = Pattern.compile(Pattern.quote(sResourceId), Pattern.CASE_INSENSITIVE);
				Bson oFilterResourceId = Filters.eq("resourceId", regex);
				aoFilters.add(oFilterResourceId);
			}

			if (!Utils.isNullOrEmpty(sUserId)) {
				Bson oFilterUserId = Filters.eq("userId", sUserId.toLowerCase());
				aoFilters.add(oFilterUserId);
			}

			Bson oFilter = Filters.and(aoFilters);

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(oFilter);

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
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
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserResourcePermissionRepository.getPermissions : exception ", oEx);
		}

		return aoReturnList;
	}

	public List<UserResourcePermission> getPermissionsByType(String sType) {
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("resourceType", sType));

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserResourcePermissionRepository.getPermissionsByType : exception ", oEx);
		}

		return aoReturnList;
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
			DeleteResult oDeleteResult = getCollection(m_sThisCollection)
					.deleteMany(new Document("resourceId", sResourceId));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
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
			DeleteResult oDeleteResult = getCollection(m_sThisCollection)
					.deleteMany(Filters.and(Filters.eq("resourceType", sType), Filters.eq("resourceId", sResourceId)));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
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
			DeleteResult oDeleteResult = getCollection(m_sThisCollection)
					.deleteMany(Filters.and(Filters.eq("resourceType", sType), Filters.eq("userId", sResourceId)));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
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
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("userId", sUserId));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
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
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("ownerId", sOwnerId));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
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
			DeleteResult oDeleteResult = getCollection(m_sThisCollection)
					.deleteMany(Filters.and(Filters.eq("userId", sUserId), Filters.eq("resourceId", sResourceId)));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
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
			DeleteResult oDeleteResult = getCollection(m_sThisCollection)
					.deleteMany(Filters.and(Filters.eq("resourceType", sType), Filters.eq("userId", sUserId), Filters.eq("resourceId", sResourceId)));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
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
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("resourceId", sResourceId))).first();

			if (null != oWSDocument) {
				return true;
			}

		} catch (Exception oE) {
			WasdiLog.errorLog("UserResourcePermissionRepository.isResourceSharedWithUser: error: ",  oE);
		}

		return false;
	}

	public boolean isResourceOfTypeSharedWithUser(String sType, String sUserId, String sResourceId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("resourceType", sType), Filters.eq("userId", sUserId), Filters.eq("resourceId", sResourceId)))
					.first();

			if (null != oWSDocument) {
				return true;
			}

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
