package wasdi.shared.data.mongo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.Organization;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.data.OrganizationRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.interfaces.ISubscriptionRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for subscription repository.
 */
public class MongoSubscriptionRepositoryBackend extends MongoRepository implements ISubscriptionRepositoryBackend {

	public MongoSubscriptionRepositoryBackend() {
		m_sThisCollection = "subscriptions";
	}

	@Override
	public boolean insertSubscription(Subscription oSubscription) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oSubscription);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.insertSubscription: error " + oEx.toString());
		}

		return false;
	}

	@Override
	public boolean updateSubscription(Subscription oSubscription) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oSubscription);

			Bson oFilter = new Document("subscriptionId", oSubscription.getSubscriptionId());
			Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));

			UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

			if (oResult.getModifiedCount() == 1) {
				return true;
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.updateSubscription: error " + oEx.toString());
		}

		return false;
	}

	@Override
	public Subscription getSubscriptionById(String sSubscriptionId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(new Document("subscriptionId", sSubscriptionId))
					.first();

			if (oWSDocument != null) {
				String sJSON = oWSDocument.toJson();

				return s_oMapper.readValue(sJSON, Subscription.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.getSubscriptionById: error " + oEx.toString());
		}

		return null;
	}

	@Override
	public List<Subscription> getSubscriptionsBySubscriptionIds(Collection<String> asSubscriptionIds) {
		final List<Subscription> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(Filters.in("subscriptionId", asSubscriptionIds));

			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsBySubscriptionIds : error " + oEx.toString());
		}

		return aoReturnList;
	}

	@Override
	public boolean checkValidSubscriptionBySubscriptionId(String sSubscriptionId) {
		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			return false;
		}

		Subscription oSubscription = this.getSubscriptionById(sSubscriptionId);

		if (oSubscription == null) {
			return false;
		}

		return oSubscription.isValid();
	}

	@Override
	public List<Subscription> getSubscriptionsByUser(String sUserId) {
		final List<Subscription> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId));

			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.checkValidSubscription : error " + oEx.toString());
		}

		return aoReturnList;
	}

	@Override
	public List<Subscription> getSubscriptionsByOrganization(String sOrganizationId) {
		final List<Subscription> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("organizationId", sOrganizationId));

			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsByOrganization : error " + oEx.toString());
		}

		return aoReturnList;
	}

	@Override
	public boolean organizationHasSubscriptions(String sOrganizationId) {
		try {
			long lCounter = getCollection(m_sThisCollection)
					.countDocuments(new Document("organizationId", sOrganizationId));

			return lCounter > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.organizationHasSubscriptions : error " + oEx.toString());
		}

		return true;
	}

	@Override
	public List<Subscription> getSubscriptionsByOrganizations(Collection<String> asOrganizationIds) {
		final List<Subscription> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(Filters.in("organizationId", asOrganizationIds));

			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsByOrganizations : error " + oEx.toString());
		}

		return aoReturnList;
	}

	@Override
	public Subscription getByName(String sName) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.eq("name", sName)).first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();

				Subscription oSubscription = null;
				try {
					oSubscription = s_oMapper.readValue(sJSON, Subscription.class);
				} catch (IOException e) {
					WasdiLog.errorLog("SubscriptionRepository.getByName: error", e);
				}

				return oSubscription;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("SubscriptionRepository.getByName( " + sName + "): error: ", oE);
		}

		return null;
	}

	@Override
	public Subscription getByNameAndUserId(String sName, String sUserId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("name", sName))).first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();

				Subscription oSubscription = null;
				try {
					oSubscription = s_oMapper.readValue(sJSON, Subscription.class);
				} catch (IOException e) {
					WasdiLog.errorLog("SubscriptionRepository.getByNameAndUserId: error: ", e);
				}

				return oSubscription;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("SubscriptionRepository.getByName: error: ", oE);
		}

		return null;
	}

	@Override
	public boolean deleteSubscription(String sSubscriptionId) {
		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			return false;
		}

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection)
					.deleteOne(new Document("subscriptionId", sSubscriptionId));

			if (oDeleteResult != null) {
				if (oDeleteResult.getDeletedCount() == 1) {
					return true;
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.deleteSubscription : error " + oEx.toString());
		}

		return false;
	}

	@Override
	public int deleteByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("userId", sUserId));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.deleteByUser : error " + oEx.toString());
		}

		return 0;
	}

	@Override
	public boolean isOwnedByUser(String sUserId, String sSubscriptionId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("subscriptionId", sSubscriptionId))).first();

			if (null != oWSDocument) {
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("SubscriptionRepository.isOwnedByUser( " + sUserId + ", " + sSubscriptionId + " ): error: ", oE);
		}

		return false;
	}

	@Override
	public List<Subscription> getSubscriptionsList() {
		final List<Subscription> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsList : error " + oEx.toString());
		}

		return aoReturnList;
	}

	@Override
	public long getSubscriptionsCount() {

		try {
			long lCount = getCollection(m_sThisCollection).countDocuments();

			return lCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsCount : error " + oEx.toString());
		}

		return -1L;
	}

	@Override
	public List<Subscription> getSubscriptionsSortedList(String sOrderBy, int iOrder) {
		final List<Subscription> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sOrderBy)) {
			sOrderBy = "name";
		}

		if (iOrder != -1 && iOrder != 1) {
			iOrder = 1;
		}

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find()
					.sort(new Document(sOrderBy, iOrder));

			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsSortedList : error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<Subscription> findSubscriptionsByFilters(String sNameFilter, String sIdFilter, String sUserIdFilter, String sOrderBy, int iOrder) {

		List<Subscription> aoReturnList = new ArrayList<>();

		List<Bson> aoFilters = new ArrayList<>();

		if (!Utils.isNullOrEmpty(sNameFilter)) {
			Pattern oNameRegEx = Pattern.compile(Pattern.quote(sNameFilter), Pattern.CASE_INSENSITIVE);
			Bson oFilterLikeName = Filters.eq("name", oNameRegEx);
			aoFilters.add(oFilterLikeName);
		}

		if (!Utils.isNullOrEmpty(sIdFilter)) {
			Pattern oIdRegEx = Pattern.compile(Pattern.quote(sIdFilter), Pattern.CASE_INSENSITIVE);
			Bson oFilterLikeSubscriptionId = Filters.eq("subscriptionId", oIdRegEx);
			aoFilters.add(oFilterLikeSubscriptionId);
		}

		if (!Utils.isNullOrEmpty(sUserIdFilter)) {
			Pattern oUserIdRegEx = Pattern.compile(Pattern.quote(sUserIdFilter), Pattern.CASE_INSENSITIVE);
			Bson oFilterLikeUserId = Filters.eq("userId", oUserIdRegEx);
			aoFilters.add(oFilterLikeUserId);
		}

		if (Utils.isNullOrEmpty(sOrderBy)) {
			sOrderBy = "name";
		}

		if (iOrder != -1 && iOrder != 1) {
			iOrder = 1;
		}

		Bson oQueryFilter = null;
		if (aoFilters.isEmpty()) {
			return getSubscriptionsSortedList(sOrderBy, iOrder);
		} else if (aoFilters.size() == 1) {
			oQueryFilter = aoFilters.get(0);
		} else {
			oQueryFilter = Filters.or(aoFilters);
		}

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(oQueryFilter)
					.sort(new Document(sOrderBy, iOrder));

			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.findSubscriptionsByFilters : error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<Subscription> getAllSubscriptionsOfUser(String sUserId) {

		final List<Subscription> aoReturnList = new ArrayList<>();
		if (Utils.isNullOrEmpty(sUserId)) {
			return aoReturnList;
		}

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId));
			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.getAllSubscriptionsOfUser : error ", oEx);
		}

		try {

			OrganizationRepository oOrganizationRepository = new OrganizationRepository();

			List<Organization> aoOwnedOrgs = oOrganizationRepository.getOrganizationsOwnedByUser(sUserId);

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			List<UserResourcePermission> aoSharedOrgs = oUserResourcePermissionRepository.getOrganizationSharingsByUserId(sUserId);

			for (UserResourcePermission oSharedOrg : aoSharedOrgs) {

				boolean bFound = false;

				for (Organization oOrganization : aoOwnedOrgs) {
					if (oOrganization.getOrganizationId().equals(oSharedOrg.getResourceId())) {
						bFound = true;
						break;
					}
				}

				if (!bFound) {
					Organization oToAdd = oOrganizationRepository.getById(oSharedOrg.getResourceId());
					aoOwnedOrgs.add(oToAdd);
				}
			}

			ArrayList<String> asOrgsId = new ArrayList<>();
			for (Organization oOrganization : aoOwnedOrgs) {
				asOrgsId.add(oOrganization.getOrganizationId());
			}

			List<Subscription> aoSubscriptionsSharedWithUser = this.getSubscriptionsByOrganizations(asOrgsId);

			List<UserResourcePermission> aoSharedSub = oUserResourcePermissionRepository.getSubscriptionSharingsByUserId(sUserId);

			for (UserResourcePermission oSharedSub : aoSharedSub) {

				boolean bFound = false;

				for (Subscription oSubscription : aoSubscriptionsSharedWithUser) {
					if (oSubscription.getSubscriptionId().equals(oSharedSub.getResourceId())) {
						bFound = true;
						break;
					}
				}

				if (!bFound) {
					Subscription oToAdd = this.getSubscriptionById(oSharedSub.getResourceId());
					aoSubscriptionsSharedWithUser.add(oToAdd);
				}
			}

			for (Subscription oSharedSub : aoSubscriptionsSharedWithUser) {

				boolean bFound = false;

				for (Subscription oSubscription : aoReturnList) {
					if (oSubscription.getSubscriptionId().equals(oSharedSub.getSubscriptionId())) {
						bFound = true;
						break;
					}
				}

				if (!bFound) {
					aoReturnList.add(oSharedSub);
				}
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.getAllSubscriptionsOfUser : error searching not-owned subscriptions " + oEx.toString());
		}

		return aoReturnList;
	}

	@Override
	public List<Subscription> findSubscriptionByPartialName(String sPartialName) {

		List<Subscription> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoReturnList;
		}

		Pattern regex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);

		Bson oFilterLikeSubscriptionId = Filters.eq("subscriptionId", regex);
		Bson oFilterLikeName = Filters.eq("name", regex);

		Bson oFilter = Filters.or(oFilterLikeSubscriptionId, oFilterLikeName);

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(oFilter)
					.sort(new Document("name", 1));

			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.findSubscriptionByPartialName: error: ", oEx);
		}

		return aoReturnList;
	}
}
