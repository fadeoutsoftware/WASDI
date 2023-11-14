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

import wasdi.shared.business.UserResourcePermission;
import wasdi.shared.utils.Utils;

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
			oEx.printStackTrace();
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
			oEx.printStackTrace();
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
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	public List<UserResourcePermission> getWorkspaceSharingsByOwnerId(String sUserId) {
		return getPermissionsByTypeAndOwnerId("workspace", sUserId);
	}

	public List<UserResourcePermission> getStyleSharingsByOwnerId(String sUserId) {
		return getPermissionsByTypeAndOwnerId("style", sUserId);
	}

	public List<UserResourcePermission> getWorkflowSharingsByOwnerId(String sUserId) {
		return getPermissionsByTypeAndOwnerId("workflow", sUserId);
	}

	public List<UserResourcePermission> getProcessorSharingsByOwnerId(String sUserId) {
		return getPermissionsByTypeAndOwnerId("processor", sUserId);
	}



	public List<UserResourcePermission> getPermissionsByUserId(String sUserId) {
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("userId", sUserId));

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
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
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	public List<UserResourcePermission> getWorkspaceSharingsByUserId(String sUserId) {
		return getPermissionsByTypeAndUserId("workspace", sUserId);
	}

	public List<UserResourcePermission> getStyleSharingsByUserId(String sUserId) {
		return getPermissionsByTypeAndUserId("style", sUserId);
	}

	public List<UserResourcePermission> getWorkflowSharingsByUserId(String sUserId) {
		return getPermissionsByTypeAndUserId("workflow", sUserId);
	}

	public List<UserResourcePermission> getProcessorSharingsByUserId(String sUserId) {
		return getPermissionsByTypeAndUserId("processor", sUserId);
	}

	public List<UserResourcePermission> getProcessorParametersTemplateSharingsByUserId(String sUserId) {
		return getPermissionsByTypeAndUserId("processorparameterstemplate", sUserId);
	}



	public List<UserResourcePermission> getPermissionsByResourceId(String sResourceId) {
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("resourceId", sResourceId));

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
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
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	public List<UserResourcePermission> getNodeSharingsByNodeCode(String sNodeCode) {
		return getPermissionsByTypeAndResourceId("node", sNodeCode);
	}

	public List<UserResourcePermission> getWorkspaceSharingsByWorkspaceId(String sWorkspaceId) {
		return getPermissionsByTypeAndResourceId("workspace", sWorkspaceId);
	}

	public List<UserResourcePermission> getStyleSharingsByStyleId(String sStyleId) {
		return getPermissionsByTypeAndResourceId("style", sStyleId);
	}

	public List<UserResourcePermission> getWorkflowSharingsByWorkflowId(String sWorkflowId) {
		return getPermissionsByTypeAndResourceId("workflow", sWorkflowId);
	}

	public List<UserResourcePermission> getProcessorSharingsByProcessorId(String sProcessorId) {
		return getPermissionsByTypeAndResourceId("processor", sProcessorId);
	}

	public List<UserResourcePermission> getProcessorParametersTemplateSharingsByProcessorParametersTemplateId(String sProcessorParametersTemplateId) {
		return getPermissionsByTypeAndResourceId("processorparameterstemplate", sProcessorParametersTemplateId);
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
					e.printStackTrace();
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("UserResourcePermissionRepository.getPermissionByUserIdAndResourceId( " + sUserId + ", "
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
				UserResourcePermission oStyleSharing;

				try {
					oStyleSharing = s_oMapper.readValue(sJSON, UserResourcePermission.class);
					return oStyleSharing;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("UserResourcePermissionRepository.getPermissionByTypeAndUserIdAndResourceId( " + sUserId
					+ ", " + sResourceId + "): error: " + oE);
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
			Utils.debugLog("UserResourcePermissionRepository.getPermissionsByTypeAndUserIdAndResourceId( " + sUserId
					+ ", " + sResourceId + "): error: " + oE);
		}

		return aoReturnList;
	}

	public UserResourcePermission getWorkspaceSharingByUserIdAndWorkspaceId(String sUserId, String sWorkspaceId) {
		return getPermissionByTypeAndUserIdAndResourceId("workspace", sUserId, sWorkspaceId);
	}

	public UserResourcePermission getStyleSharingByUserIdAndStyleId(String sUserId, String sStyleId) {
		return getPermissionByTypeAndUserIdAndResourceId("style", sUserId, sStyleId);
	}

	public UserResourcePermission getWorkflowSharingByUserIdAndWorkflowId(String sUserId, String sWorkflowId) {
		return getPermissionByTypeAndUserIdAndResourceId("workflow", sUserId, sWorkflowId);
	}

	public UserResourcePermission getProcessorSharingByUserIdAndProcessorId(String sUserId, String sProcessorId) {
		return getPermissionByTypeAndUserIdAndResourceId("processor", sUserId, sProcessorId);
	}

	public UserResourcePermission getProcessorParametersTemplateSharingByUserIdAndProcessorParametersTemplateId(String sUserId, String sProcessorParametersTemplateId) {
		return getPermissionByTypeAndUserIdAndResourceId("processorparameterstemplate", sUserId, sProcessorParametersTemplateId);
	}



	public List<UserResourcePermission> getPermissions() {
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
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
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	public List<UserResourcePermission> getNodeSharings() {
		return getPermissionsByType("node");
	}

	public List<UserResourcePermission> getWorkspaceSharings() {
		return getPermissionsByType("workspace");
	}

	public List<UserResourcePermission> getStyleSharings() {
		return getPermissionsByType("style");
	}

	public List<UserResourcePermission> getWorkflowSharings() {
		return getPermissionsByType("workflow");
	}

	public List<UserResourcePermission> getProcessorSharings() {
		return getPermissionsByType("processor");
	}

	public List<UserResourcePermission> getProcessorParametersTemplateSharings() {
		return getPermissionsByType("processorparameterstemplate");
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
			oEx.printStackTrace();
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
			oEx.printStackTrace();
		}

		return 0;
	}

	public int deletePermissionsByNodeCode(String sNodeCode) {
		return deletePermissionsByTypeAndResourceId("node", sNodeCode);
	}

	public int deletePermissionsByWorkspaceId(String sWorkspaceId) {
		return deletePermissionsByTypeAndResourceId("workspace", sWorkspaceId);
	}

	public int deletePermissionsByStyleId(String sStyleId) {
		return deletePermissionsByTypeAndResourceId("style", sStyleId);
	}

	public int deletePermissionsByWorkflowId(String sWorkflowId) {
		return deletePermissionsByTypeAndResourceId("workflow", sWorkflowId);
	}

	public int deletePermissionsByProcessorId(String sProcessorId) {
		return deletePermissionsByTypeAndResourceId("processor", sProcessorId);
	}



	public int deletePermissionsByUserId(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection)
					.deleteMany(new Document("userId", sUserId));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return 0;
	}

	public int deletePermissionsByTypeAndUserId(String sType, String sUserId) {
		if (Utils.isNullOrEmpty(sType)) {
			return 0;
		}

		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection)
					.deleteMany(Filters.and(Filters.eq("resourceType", sType), Filters.eq("userId", sUserId)));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return 0;
	}

	public int deleteWorkspacePermissionsByUserId(String sUserId) {
		return deletePermissionsByTypeAndResourceId("workspace", sUserId);
	}

	public int deleteStylePermissionsByUserId(String sUserId) {
		return deletePermissionsByTypeAndResourceId("style", sUserId);
	}

	public int deleteWorkflowPermissionsByUserId(String sUserId) {
		return deletePermissionsByTypeAndResourceId("workflow", sUserId);
	}

	public int deleteProcessorPermissionsByUserId(String sUserId) {
		return deletePermissionsByTypeAndResourceId("processor", sUserId);
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
			oEx.printStackTrace();
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
			oEx.printStackTrace();
		}

		return 0;
	}

	public int deletePermissionsByUserIdAndNodeCode(String sUserId, String sNodeCode) {
		return deletePermissionsByTypeAndUserIdAndResourceId("node", sUserId, sNodeCode);
	}

	public int deletePermissionsByUserIdAndWorkspaceId(String sUserId, String sWorkspaceId) {
		return deletePermissionsByTypeAndUserIdAndResourceId("workspace", sUserId, sWorkspaceId);
	}

	public int deletePermissionsByUserIdAndStyleId(String sUserId, String sStyleId) {
		return deletePermissionsByTypeAndUserIdAndResourceId("style", sUserId, sStyleId);
	}

	public int deletePermissionsByUserIdAndWorkflowId(String sUserId, String sWorkflowId) {
		return deletePermissionsByTypeAndUserIdAndResourceId("workflow", sUserId, sWorkflowId);
	}

	public int deletePermissionsByUserIdAndProcessorId(String sUserId, String sProcessorId) {
		return deletePermissionsByTypeAndUserIdAndResourceId("processor", sUserId, sProcessorId);
	}

	public int deletePermissionsByUserIdAndProcessorParametersTemplateId(String sUserId, String sProcessorParametersTemplateId) {
		return deletePermissionsByTypeAndUserIdAndResourceId("processorparameterstemplate", sUserId, sProcessorParametersTemplateId);
	}



	public boolean isResourceSharedWithUser(String sUserId, String sResourceId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("resourceId", sResourceId))).first();

			if (null != oWSDocument) {
				return true;
			}

		} catch (Exception oE) {
			Utils.debugLog("UserResourcePermissionRepository.isResourceSharedWithUser( " + sUserId + ", "
					+ sResourceId + "): error: " + oE);
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
			Utils.debugLog("UserResourcePermissionRepository.isResourceOfTypeSharedWithUser( " + sType
					+ ", " + sUserId + ", " + sResourceId + "): error: " + oE);
		}

		return false;
	}

	public boolean isNodeSharedWithUser(String sUserId, String sNodeCode) {
		return isResourceOfTypeSharedWithUser("node", sUserId, sNodeCode);
	}

	public boolean isWorkspaceSharedWithUser(String sUserId, String sWorkspaceId) {
		return isResourceOfTypeSharedWithUser("workspace", sUserId, sWorkspaceId);
	}

	public boolean isStyleSharedWithUser(String sUserId, String sStyleId) {
		return isResourceOfTypeSharedWithUser("style", sUserId, sStyleId);
	}

	public boolean isWorkflowSharedWithUser(String sUserId, String sWorkflowId) {
		return isResourceOfTypeSharedWithUser("workflow", sUserId, sWorkflowId);
	}

	public boolean isProcessorSharedWithUser(String sUserId, String sProcessorId) {
		return isResourceOfTypeSharedWithUser("processor", sUserId, sProcessorId);
	}

	public boolean isProcessorParametersTemplateSharedWithUser(String sUserId, String sProcessorParametersTemplateId) {
		return isResourceOfTypeSharedWithUser("processorparameterstemplate", sUserId, sProcessorParametersTemplateId);
	}

}
