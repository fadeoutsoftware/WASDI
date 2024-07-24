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

import wasdi.shared.business.Organization;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.users.UserResourcePermission;
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
			WasdiLog.errorLog("SubscriptionRepository.insertSubscription: error " + oEx.toString());
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
			WasdiLog.errorLog("SubscriptionRepository.updateSubscription: error " + oEx.toString());
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
			WasdiLog.errorLog("SubscriptionRepository.getSubscriptionById: error " + oEx.toString());
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
			WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsBySubscriptionIds : error " + oEx.toString());
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
		
		if (oSubscription == null) {
			return false;
		}

		return oSubscription.isValid();
	}
	
	/**
	 * Get the subscriptions by owner.
	 * @param sUserId the owner of the subscriptions
	 * @return the list of subscriptions found
	 */
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
			WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsByOrganization : error " + oEx.toString());
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
			WasdiLog.errorLog("SubscriptionRepository.organizationHasSubscriptions : error " + oEx.toString());
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
			WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsByOrganizations : error " + oEx.toString());
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
					WasdiLog.errorLog("SubscriptionRepository.getByName: error", e);
				}

				return oSubscription;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("SubscriptionRepository.getByName( " + sName + "): error: ", oE);
		}

		return null;
	}
	
	/**
	 * Get an subscription by its name and the user id
	 * @param sName the name of the subscription
	 * @return the subscription if found, null otherwise
	 */
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
			WasdiLog.errorLog("SubscriptionRepository.deleteSubscription : error " + oEx.toString());
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
			WasdiLog.errorLog("SubscriptionRepository.deleteByUser : error " + oEx.toString());
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
			WasdiLog.errorLog("SubscriptionRepository.isOwnedByUser( " + sUserId + ", " + sSubscriptionId + " ): error: ", oE);
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
			WasdiLog.errorLog("SubscriptionRepository.getSubscriptionsList : error " + oEx.toString());
		}

		return aoReturnList;
	}
	
	/**
	 * Get the sorted list of subscriptions.
	 * @return the sorted list of subscriptions
	 */
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

	/**
	 * Find subscriptions by partial name, by partial id or partial user id
	 * @param sNameFilter the partial name of the subscription
	 * @param sIdFilter the partial id of the subscription
	 * @param sUserIdFilter the partial user id of the subscription
	 * @return the list of subscriptions that partially match the name or id
	 */
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
			getSubscriptionsSortedList(sOrderBy, iOrder);
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
	
	/**
	 * Get a List of all the subscriptions avaiable for one user.
	 * Can be the owner, or linked in an organization, or shared by a user
	 * @param sUserId
	 * @return
	 */
	public List<Subscription> getAllSubscriptionsOfUser(String sUserId) {
		
		// Create the return list
		final List<Subscription> aoReturnList = new ArrayList<>();
		// Check our user Id 
		if (Utils.isNullOrEmpty(sUserId)) return aoReturnList;
		
		// Get all the subscription owned by the user
		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId));
			fillList(aoReturnList, oWSDocuments, Subscription.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.getAllSubscriptionsOfUser : error ", oEx);
		}
		
		try {
			
			// Create the Organization Repository 
			OrganizationRepository oOrganizationRepository = new OrganizationRepository();
			
			// Get the orgs owned by the user 
			List<Organization> aoOwnedOrgs =  oOrganizationRepository.getOrganizationsOwnedByUser(sUserId);
			
			// Now the the orgs shared with the user
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			List<UserResourcePermission> aoSharedOrgs = oUserResourcePermissionRepository.getOrganizationSharingsByUserId(sUserId);
			
			// Merge the two list avoiding duplicates
			for (UserResourcePermission oSharedOrg : aoSharedOrgs) {
				
				boolean bFound = false;
				
				// Has this already be added?
				for (Organization oOrganization : aoOwnedOrgs) {
					if (oOrganization.getOrganizationId().equals(oSharedOrg.getResourceId())) {
						bFound = true;
						break;
					}
				}
				
				if (!bFound) {
					// Add the org to our list
					Organization oToAdd = oOrganizationRepository.getById(oSharedOrg.getResourceId());
					aoOwnedOrgs.add(oToAdd);
				}
			}
			
			// Extract a list of only Organization Ids
			ArrayList<String> asOrgsId = new ArrayList<>();
			for (Organization oOrganization : aoOwnedOrgs) {
				asOrgsId.add(oOrganization.getOrganizationId());
			}
			
			
			// Get the list of subscriptions related to organizations
			List<Subscription> aoSubscriptionsSharedWithUser =  this.getSubscriptionsByOrganizations(asOrgsId);
			
			// Get the list of subscription shared directly by users
			List<UserResourcePermission> aoSharedSub = oUserResourcePermissionRepository.getSubscriptionSharingsByUserId(sUserId);
			
			// Merge the two list avoiding duplicates
			for (UserResourcePermission oSharedSub : aoSharedSub) {
				
				boolean bFound = false;
				
				// Has this already be added?
				for (Subscription oSubscription : aoSubscriptionsSharedWithUser) {
					if (oSubscription.getSubscriptionId().equals(oSharedSub.getResourceId())) {
						bFound = true;
						break;
					}
				}
				
				if (!bFound) {
					// Add the subscription to our list
					Subscription oToAdd = this.getSubscriptionById(oSharedSub.getResourceId());
					aoSubscriptionsSharedWithUser.add(oToAdd);
				}
			}
			
			
			// Finally we need to merge the owned list with the shared ones
			
			// Merge the two list avoiding duplicates
			for (Subscription oSharedSub : aoSubscriptionsSharedWithUser) {
				
				boolean bFound = false;
				
				// Has this already be added?
				for (Subscription oSubscription : aoReturnList) {
					if (oSubscription.getSubscriptionId().equals(oSharedSub.getSubscriptionId())) {
						bFound = true;
						break;
					}
				}
				
				if (!bFound) {
					// Add the to our list
					aoReturnList.add(oSharedSub);
				}
			}			
						
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.getAllSubscriptionsOfUser : error searching not-owned subscriptions " + oEx.toString());
		}

		return aoReturnList;
		
	}
	
	/**
	 * Find a subscription that  matches a given partial name, description or id
	 * @param sPartialName the partial name of the subscription
	 * @return
	 */
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
