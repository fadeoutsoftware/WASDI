package wasdi.shared.data;

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

import wasdi.shared.business.Subscription;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class SubscriptionRepository extends MongoRepository {

	public SubscriptionRepository() {
		m_sThisCollection = "subscriptions";
	}

	/**
	 * Insert a new subscription.
	 * 
	 * @param oSubscription the subscription to be inserted
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean insertSubscription(Subscription oSubscription) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oSubscription);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return false;
	}

	/**
	 * Update an subscription.
	 * 
	 * @param oSubscription the subscription to be updated
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean updateSubscription(Subscription oSubscription) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oSubscription);

			Bson oFilter = new Document("subscriptionId", oSubscription.getSubscriptionId());
			Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));

			UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

			if (oResult.getModifiedCount() == 1)
				return true;
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return false;
	}

	/**
	 * Get a subscription by its id.
	 * @param sSubscriptionId the id of the subscription
	 * @return the subscription if found, null otherwise
	 */
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
			oEx.printStackTrace();
		}

		return null;
	}

	/**
	 * Get the subscriptions by their IDs.
	 * @param asSubscriptionIds the list of subscriptionIds
	 * @return the list of subscriptions
	 */
	public List<Subscription> getSubscriptionsBySubscriptionIds(Collection<String> asSubscriptionIds) {
		final List<Subscription> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(Filters.in("subscriptionId", asSubscriptionIds));

			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	/**
	 * Check whether or not a subscription is valid.
	 * @param sSubscriptionId the id of the subscription
	 * @return true if the subscription if valid, false otherwise
	 */
	public boolean checkValidSubscriptionBySubscriptionId(String sSubscriptionId) {
		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			return false;
		}

		Subscription oSubscription = this.getSubscriptionById(sSubscriptionId);

		return checkValidSubscription(oSubscription);
	}

	/**
	 * Check whether or not a subscription is valid.
	 * @param oSubscription the subscription
	 * @return true if the subscription if valid, false otherwise
	 */
	public static boolean checkValidSubscription(Subscription oSubscription) {
		if (oSubscription == null
				|| oSubscription.getStartDate() == null
				|| oSubscription.getEndDate() == null) {
			return false;
		}

		double dNowInMillis = Utils.nowInMillis();

		return dNowInMillis >= oSubscription.getStartDate()
				&& dNowInMillis <= oSubscription.getEndDate();
	}

	/**
	 * Get the subscriptions by owner.
	 * @param sUserId the owner of the subscriptions
	 * @return the list of subscriptions found
	 */
	public List<Subscription> getSubscriptionsByUser(String sUserId) {
		final List<Subscription> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("userId", sUserId));

			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	/**
	 * Get the subscriptions of an organization.
	 * @param sOrganizationId the organizationId of the subscriptions
	 * @return the list of subscriptions found
	 */
	public List<Subscription> getSubscriptionsByOrganization(String sOrganizationId) {
		final List<Subscription> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("organizationId", sOrganizationId));

			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	/**
	 * Check whether or not the organization has subscriptions.
	 * @param sOrganizationId the organizationId of the subscriptions
	 * @return true if the organization has associated subscriptions
	 */
	public boolean organizationHasSubscriptions(String sOrganizationId) {
		try {
			long lCounter = getCollection(m_sThisCollection)
					.countDocuments(new Document("organizationId", sOrganizationId));

			return lCounter > 0;
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return true;
	}

	/**
	 * Get the subscriptions related to many organizations.
	 * @param asOrganizationIds the list of organizationIds
	 * @return the list of subscriptions associated with the organizations
	 */
	public List<Subscription> getSubscriptionsByOrganizations(Collection<String> asOrganizationIds) {
		final List<Subscription> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(Filters.in("organizationId", asOrganizationIds));

			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	/**
	 * Get an subscription by its name.
	 * @param sName the name of the subscription
	 * @return the subscription if found, null otherwise
	 */
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
					e.printStackTrace();
				}

				return oSubscription;
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("SubscriptionRepository.getByName( " + sName + "): error: " + oE);
		}

		return null;
	}

	/**
	 * Delete the subscription.
	 * @param sSubscriptionId the id of the subscription
	 * @return true if the deletion was successful, false otherwise
	 */
	public boolean deleteSubscription(String sSubscriptionId) {
		if (Utils.isNullOrEmpty(sSubscriptionId))
			return false;

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection)
					.deleteOne(new Document("subscriptionId", sSubscriptionId));

			if (oDeleteResult != null) {
				if (oDeleteResult.getDeletedCount() == 1) {
					return true;
				}
			}
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return false;
	}

	/**
	 * Delete all subscriptions belonging to a specific user.
	 * @param sUserId the owner of the subscription
	 * @return the number of subscriptions deleted, 0 if none
	 */
	public int deleteByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId))
			return 0;

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("userId", sUserId));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return 0;
	}

	/**
	 * Check if User is the owner of Subscription.
	 * 
	 * @param sUserId the owner's id
	 * @param sSubscriptionId the subscription's id
	 * @return true if the user is the owner, false otherwise
	 */
	public boolean isOwnedByUser(String sUserId, String sSubscriptionId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("subscriptionId", sSubscriptionId))).first();

			if (null != oWSDocument) {
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.debugLog(
					"SubscriptionRepository.belongsToUser( " + sUserId + ", " + sSubscriptionId + " ): error: " + oE);
		}

		return false;
	}

	/**
	 * Get the list of subscriptions.
	 * @return the list of subscriptions
	 */
	public List<Subscription> getSubscriptionsList() {
		final List<Subscription> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	/**
	 * Find subscriptions by partial name or by partial id
	 * @param sPartialName the partial name or partial id
	 * @return the list of subscriptions that partially match the name or id
	 */
	public List<Subscription> findSubscriptionsByPartialName(String sPartialName) {
		List<Subscription> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoReturnList;
		}

		Pattern regex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);

		Bson oFilterLikeSubscriptionId = Filters.eq("subscriptionId", regex);
		Bson oFilterLikeName = Filters.eq("name", regex);

		Bson oFilter = Filters.or(oFilterLikeSubscriptionId, oFilterLikeName);

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oFilter)
					.sort(new Document("name", 1));

			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

}
