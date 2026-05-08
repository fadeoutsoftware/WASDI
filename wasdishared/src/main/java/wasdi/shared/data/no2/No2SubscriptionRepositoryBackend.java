package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.Organization;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.data.OrganizationRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.interfaces.ISubscriptionRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for subscription repository.
 */
public class No2SubscriptionRepositoryBackend extends No2Repository implements ISubscriptionRepositoryBackend {

	private static final String s_sCollectionName = "subscriptions";

	@Override
	public boolean insertSubscription(Subscription oSubscription) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oSubscription == null) {
				return false;
			}

			oCollection.insert(toDocument(oSubscription));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SubscriptionRepositoryBackend.insertSubscription: error", oEx);
		}

		return false;
	}

	@Override
	public boolean updateSubscription(Subscription oSubscription) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oSubscription == null) {
				return false;
			}

			oCollection.update(where("subscriptionId").eq(oSubscription.getSubscriptionId()), toDocument(oSubscription));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SubscriptionRepositoryBackend.updateSubscription: error", oEx);
		}

		return false;
	}

	@Override
	public Subscription getSubscriptionById(String sSubscriptionId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("subscriptionId").eq(sSubscriptionId))) {
				return fromDocument(oDocument, Subscription.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SubscriptionRepositoryBackend.getSubscriptionById: error", oEx);
		}

		return null;
	}

	@Override
	public List<Subscription> getSubscriptionsBySubscriptionIds(Collection<String> asSubscriptionIds) {
		List<Subscription> aoResults = new ArrayList<>();
		if (asSubscriptionIds == null || asSubscriptionIds.isEmpty()) {
			return aoResults;
		}

		for (Subscription oSubscription : getSubscriptionsList()) {
			if (oSubscription != null && asSubscriptionIds.contains(oSubscription.getSubscriptionId())) {
				aoResults.add(oSubscription);
			}
		}

		return aoResults;
	}

	@Override
	public boolean checkValidSubscriptionBySubscriptionId(String sSubscriptionId) {
		Subscription oSubscription = getSubscriptionById(sSubscriptionId);
		return oSubscription != null && oSubscription.isValid();
	}

	@Override
	public List<Subscription> getSubscriptionsByUser(String sUserId) {
		return findByEquals("userId", sUserId);
	}

	@Override
	public List<Subscription> getSubscriptionsByOrganization(String sOrganizationId) {
		return findByEquals("organizationId", sOrganizationId);
	}

	@Override
	public boolean organizationHasSubscriptions(String sOrganizationId) {
		return !getSubscriptionsByOrganization(sOrganizationId).isEmpty();
	}

	@Override
	public List<Subscription> getSubscriptionsByOrganizations(Collection<String> asOrganizationIds) {
		List<Subscription> aoResults = new ArrayList<>();
		if (asOrganizationIds == null || asOrganizationIds.isEmpty()) {
			return aoResults;
		}

		for (Subscription oSubscription : getSubscriptionsList()) {
			if (oSubscription != null && asOrganizationIds.contains(oSubscription.getOrganizationId())) {
				aoResults.add(oSubscription);
			}
		}

		return aoResults;
	}

	@Override
	public Subscription getByName(String sName) {
		for (Subscription oSubscription : findByEquals("name", sName)) {
			return oSubscription;
		}
		return null;
	}

	@Override
	public Subscription getByNameAndUserId(String sName, String sUserId) {
		for (Subscription oSubscription : getSubscriptionsByUser(sUserId)) {
			if (oSubscription != null && sName != null && sName.equals(oSubscription.getName())) {
				return oSubscription;
			}
		}
		return null;
	}

	@Override
	public boolean deleteSubscription(String sSubscriptionId) {
		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			oCollection.remove(where("subscriptionId").eq(sSubscriptionId));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SubscriptionRepositoryBackend.deleteSubscription: error", oEx);
		}

		return false;
	}

	@Override
	public int deleteByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		int iCount = getSubscriptionsByUser(sUserId).size();
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			oCollection.remove(where("userId").eq(sUserId));
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SubscriptionRepositoryBackend.deleteByUser: error", oEx);
		}

		return iCount;
	}

	@Override
	public boolean isOwnedByUser(String sUserId, String sSubscriptionId) {
		Subscription oSubscription = getSubscriptionById(sSubscriptionId);
		return oSubscription != null && sUserId != null && sUserId.equals(oSubscription.getUserId());
	}

	@Override
	public List<Subscription> getSubscriptionsList() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find() : null;
			return toList(oCursor, Subscription.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SubscriptionRepositoryBackend.getSubscriptionsList: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public long getSubscriptionsCount() {
		return getSubscriptionsList().size();
	}

	@Override
	public List<Subscription> getSubscriptionsSortedList(String sOrderBy, int iOrder) {
		if (Utils.isNullOrEmpty(sOrderBy)) {
			sOrderBy = "name";
		}
		if (iOrder != -1 && iOrder != 1) {
			iOrder = 1;
		}

		List<Subscription> aoResults = new ArrayList<>(getSubscriptionsList());
		sortSubscriptions(aoResults, sOrderBy, iOrder);
		return aoResults;
	}

	@Override
	public List<Subscription> findSubscriptionsByFilters(String sNameFilter, String sIdFilter, String sUserIdFilter, String sOrderBy, int iOrder) {
		List<Subscription> aoResults = new ArrayList<>();

		for (Subscription oSubscription : getSubscriptionsList()) {
			if (oSubscription == null) {
				continue;
			}

			boolean bMatch = false;
			if (!Utils.isNullOrEmpty(sNameFilter) && containsIgnoreCase(oSubscription.getName(), sNameFilter)) {
				bMatch = true;
			}
			if (!Utils.isNullOrEmpty(sIdFilter) && containsIgnoreCase(oSubscription.getSubscriptionId(), sIdFilter)) {
				bMatch = true;
			}
			if (!Utils.isNullOrEmpty(sUserIdFilter) && containsIgnoreCase(oSubscription.getUserId(), sUserIdFilter)) {
				bMatch = true;
			}

			if (Utils.isNullOrEmpty(sNameFilter) && Utils.isNullOrEmpty(sIdFilter) && Utils.isNullOrEmpty(sUserIdFilter)) {
				bMatch = true;
			}

			if (bMatch) {
				aoResults.add(oSubscription);
			}
		}

		if (Utils.isNullOrEmpty(sOrderBy)) {
			sOrderBy = "name";
		}
		if (iOrder != -1 && iOrder != 1) {
			iOrder = 1;
		}
		sortSubscriptions(aoResults, sOrderBy, iOrder);
		return aoResults;
	}

	@Override
	public List<Subscription> getAllSubscriptionsOfUser(String sUserId) {
		List<Subscription> aoReturnList = new ArrayList<>(getSubscriptionsByUser(sUserId));
		if (Utils.isNullOrEmpty(sUserId)) {
			return aoReturnList;
		}

		try {
			OrganizationRepository oOrganizationRepository = new OrganizationRepository();
			List<Organization> aoOwnedOrgs = oOrganizationRepository.getOrganizationsOwnedByUser(sUserId);

			UserResourcePermissionRepository oPermissionRepository = new UserResourcePermissionRepository();
			List<UserResourcePermission> aoSharedOrgs = oPermissionRepository.getOrganizationSharingsByUserId(sUserId);

			for (UserResourcePermission oSharedOrg : aoSharedOrgs) {
				boolean bFound = false;
				for (Organization oOrg : aoOwnedOrgs) {
					if (oOrg != null && oOrg.getOrganizationId().equals(oSharedOrg.getResourceId())) {
						bFound = true;
						break;
					}
				}
				if (!bFound) {
					Organization oOrgToAdd = oOrganizationRepository.getById(oSharedOrg.getResourceId());
					if (oOrgToAdd != null) {
						aoOwnedOrgs.add(oOrgToAdd);
					}
				}
			}

			List<String> asOrgIds = new ArrayList<>();
			for (Organization oOrg : aoOwnedOrgs) {
				if (oOrg != null) {
					asOrgIds.add(oOrg.getOrganizationId());
				}
			}

			List<Subscription> aoSubscriptionsSharedWithUser = getSubscriptionsByOrganizations(asOrgIds);

			List<UserResourcePermission> aoSharedSubs = oPermissionRepository.getSubscriptionSharingsByUserId(sUserId);
			for (UserResourcePermission oSharedSub : aoSharedSubs) {
				boolean bFound = false;
				for (Subscription oSubscription : aoSubscriptionsSharedWithUser) {
					if (oSubscription != null && oSubscription.getSubscriptionId().equals(oSharedSub.getResourceId())) {
						bFound = true;
						break;
					}
				}
				if (!bFound) {
					Subscription oToAdd = getSubscriptionById(oSharedSub.getResourceId());
					if (oToAdd != null) {
						aoSubscriptionsSharedWithUser.add(oToAdd);
					}
				}
			}

			for (Subscription oSharedSub : aoSubscriptionsSharedWithUser) {
				boolean bFound = false;
				for (Subscription oSubscription : aoReturnList) {
					if (oSubscription != null && oSharedSub != null
							&& oSubscription.getSubscriptionId().equals(oSharedSub.getSubscriptionId())) {
						bFound = true;
						break;
					}
				}
				if (!bFound && oSharedSub != null) {
					aoReturnList.add(oSharedSub);
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SubscriptionRepositoryBackend.getAllSubscriptionsOfUser: error", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<Subscription> findSubscriptionByPartialName(String sPartialName) {
		List<Subscription> aoResults = new ArrayList<>();
		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoResults;
		}

		for (Subscription oSubscription : getSubscriptionsList()) {
			if (oSubscription == null) {
				continue;
			}

			if (containsIgnoreCase(oSubscription.getSubscriptionId(), sPartialName)
					|| containsIgnoreCase(oSubscription.getName(), sPartialName)) {
				aoResults.add(oSubscription);
			}
		}

		sortSubscriptions(aoResults, "name", 1);
		return aoResults;
	}

	private List<Subscription> findByEquals(String sField, String sValue) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return new ArrayList<>();
			}
			DocumentCursor oCursor = oCollection.find(where(sField).eq(sValue));
			return toList(oCursor, Subscription.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SubscriptionRepositoryBackend.findByEquals: error", oEx);
			return new ArrayList<>();
		}
	}

	private void sortSubscriptions(List<Subscription> aoSubscriptions, String sOrderBy, int iOrder) {
		Comparator<Subscription> oComparator;
		switch (sOrderBy) {
			case "subscriptionId":
				oComparator = Comparator.comparing(Subscription::getSubscriptionId, Comparator.nullsLast(String::compareToIgnoreCase));
				break;
			case "userId":
				oComparator = Comparator.comparing(Subscription::getUserId, Comparator.nullsLast(String::compareToIgnoreCase));
				break;
			case "organizationId":
				oComparator = Comparator.comparing(Subscription::getOrganizationId, Comparator.nullsLast(String::compareToIgnoreCase));
				break;
			default:
				oComparator = Comparator.comparing(Subscription::getName, Comparator.nullsLast(String::compareToIgnoreCase));
				break;
		}

		if (iOrder < 0) {
			oComparator = oComparator.reversed();
		}
		aoSubscriptions.sort(oComparator);
	}

	private boolean containsIgnoreCase(String sValue, String sLookup) {
		if (sValue == null || sLookup == null) {
			return false;
		}
		return sValue.toLowerCase().contains(sLookup.toLowerCase());
	}
}
