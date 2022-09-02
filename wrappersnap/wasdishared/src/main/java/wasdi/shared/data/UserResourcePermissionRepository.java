package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.UserResourcePermission;
import wasdi.shared.utils.Utils;

public class UserResourcePermissionRepository extends MongoRepository {

	public UserResourcePermissionRepository() {
		m_sThisCollection = "userresourcepermissions";
	}

	public boolean insertUserResourcePermission(UserResourcePermission oUserResourcePermission) {
		try {
			String sJSON = s_oMapper.writeValueAsString(oUserResourcePermission);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return false;
	}



	public List<UserResourcePermission> getUserResourcePermissionByOwner(String sUserId) {
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

	public List<UserResourcePermission> getUserResourcePermissionByTypeAndOwner(String sType, String sUserId) {
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

	public List<UserResourcePermission> getWorkspaceSharingByOwner(String sUserId) {
		return getUserResourcePermissionByTypeAndOwner("workspace", sUserId);
	}



	public List<UserResourcePermission> getUserResourcePermissionByUser(String sUserId) {
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

	public List<UserResourcePermission> getUserResourcePermissionByTypeAndUser(String sType, String sUserId) {
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

	public List<UserResourcePermission> getWorkspaceSharingByUser(String sUserId) {
		return getUserResourcePermissionByTypeAndUser("workspace", sUserId);
	}



	public List<UserResourcePermission> getUserResourcePermissionByResourceId(String sResourceId) {
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

	public List<UserResourcePermission> getUserResourcePermissionByTypeAndResourceId(String sType, String sResourceId) {
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

	public List<UserResourcePermission> getWorkspaceSharingByWorkspace(String sWorkspaceId) {
		return getUserResourcePermissionByTypeAndResourceId("workspace", sWorkspaceId);
	}



	public List<UserResourcePermission> getUserResourcePermissions() {
		final List<UserResourcePermission> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

			fillList(aoReturnList, oWSDocuments, UserResourcePermission.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	public List<UserResourcePermission> getUserResourcePermissionsByType(String sType) {
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

	public List<UserResourcePermission> getWorkspaceSharings() {
		return getUserResourcePermissionsByType("workspace");
	}



	public int deleteUserResourcePermissionByResourceId(String sResourceId) {
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

	public int deleteUserResourcePermissionByTypeAndResourceId(String sType, String sResourceId) {
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

	public int deleteByWorkspaceId(String sWorkspaceId) {
		return deleteUserResourcePermissionByTypeAndResourceId("workspace", sWorkspaceId);
	}



	public int deleteUserResourcePermissionByUserId(String sUserId) {
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

	public int deleteUserResourcePermissionByTypeAndUserId(String sType, String sUserId) {
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

	public int deleteByUserId(String sUserId) {
		return deleteUserResourcePermissionByTypeAndResourceId("workspace", sUserId);
	}



	public int deleteUserResourcePermissionByUserIdAndResourceId(String sUserId, String sResourceId) {
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

	public int deleteUserResourcePermissionByTypeAndUserIdAndResourceId(String sType, String sUserId, String sResourceId) {
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

	public int deleteByUserIdWorkspaceId(String sUserId, String sWorkspaceId) {
		return deleteUserResourcePermissionByTypeAndUserIdAndResourceId("workspace", sUserId, sWorkspaceId);
	}



	public boolean isUserResourcePermissionSharedWithUser(String sUserId, String sResourceId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("resourceId", sResourceId))).first();

			if (null != oWSDocument) {
				return true;
			}

		} catch (Exception oE) {
			Utils.debugLog("UserResourcePermissionRepository.isUserResourcePermissionSharedWithUser( " + sUserId + ", "
					+ sResourceId + "): error: " + oE);
		}

		return false;
	}

	public boolean isUserResourcePermissionSharedWithUserByType(String sType, String sUserId, String sResourceId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("resourceType", sType), Filters.eq("userId", sUserId), Filters.eq("resourceId", sResourceId))).first();

			if (null != oWSDocument) {
				return true;
			}

		} catch (Exception oE) {
			Utils.debugLog("UserResourcePermissionRepository.isUserResourcePermissionSharedWithUserByType( " + sType
					+ ", " + sUserId + ", " + sResourceId + "): error: " + oE);
		}

		return false;
	}

	public boolean isWorkspaceSharedWithUser(String sUserId, String sWorkspaceId) {
		return isUserResourcePermissionSharedWithUserByType("workspace", sUserId, sWorkspaceId);
	}

}
