package wasdi.shared.data.interfaces;

import java.util.Collection;
import java.util.List;

import wasdi.shared.business.Subscription;

/**
 * Backend contract for subscription repository.
 */
public interface ISubscriptionRepositoryBackend {

	boolean insertSubscription(Subscription oSubscription);

	boolean updateSubscription(Subscription oSubscription);

	Subscription getSubscriptionById(String sSubscriptionId);

	List<Subscription> getSubscriptionsBySubscriptionIds(Collection<String> asSubscriptionIds);

	boolean checkValidSubscriptionBySubscriptionId(String sSubscriptionId);

	List<Subscription> getSubscriptionsByUser(String sUserId);

	List<Subscription> getSubscriptionsByOrganization(String sOrganizationId);

	boolean organizationHasSubscriptions(String sOrganizationId);

	List<Subscription> getSubscriptionsByOrganizations(Collection<String> asOrganizationIds);

	Subscription getByName(String sName);

	Subscription getByNameAndUserId(String sName, String sUserId);

	boolean deleteSubscription(String sSubscriptionId);

	int deleteByUser(String sUserId);

	boolean isOwnedByUser(String sUserId, String sSubscriptionId);

	List<Subscription> getSubscriptionsList();

	long getSubscriptionsCount();

	List<Subscription> getSubscriptionsSortedList(String sOrderBy, int iOrder);

	List<Subscription> findSubscriptionsByFilters(String sNameFilter, String sIdFilter, String sUserIdFilter, String sOrderBy, int iOrder);

	List<Subscription> getAllSubscriptionsOfUser(String sUserId);

	List<Subscription> findSubscriptionByPartialName(String sPartialName);
}
