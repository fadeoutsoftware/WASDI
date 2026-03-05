package wasdi.shared.data;

import java.util.Collection;
import java.util.List;

import wasdi.shared.business.Subscription;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.ISubscriptionRepositoryBackend;

public class SubscriptionRepository {
	private final ISubscriptionRepositoryBackend m_oBackend;

	public SubscriptionRepository() {
		m_oBackend = createBackend();
	}

	private ISubscriptionRepositoryBackend createBackend() {
		return DataRepositoryFactoryProvider.getFactory().createSubscriptionRepository();
	}

	/**
	 * Insert a new subscription.
	 * 
	 * @param oSubscription the subscription to be inserted
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean insertSubscription(Subscription oSubscription) {
		return m_oBackend.insertSubscription(oSubscription);
	}

	/**
	 * Update an subscription.
	 * 
	 * @param oSubscription the subscription to be updated
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean updateSubscription(Subscription oSubscription) {
		return m_oBackend.updateSubscription(oSubscription);
	}

	/**
	 * Get a subscription by its id.
	 * @param sSubscriptionId the id of the subscription
	 * @return the subscription if found, null otherwise
	 */
	public Subscription getSubscriptionById(String sSubscriptionId) {
		return m_oBackend.getSubscriptionById(sSubscriptionId);
	}

	/**
	 * Get the subscriptions by their IDs.
	 * @param asSubscriptionIds the list of subscriptionIds
	 * @return the list of subscriptions
	 */
	public List<Subscription> getSubscriptionsBySubscriptionIds(Collection<String> asSubscriptionIds) {
		return m_oBackend.getSubscriptionsBySubscriptionIds(asSubscriptionIds);
	}

	/**
	 * Check whether or not a subscription is valid.
	 * @param sSubscriptionId the id of the subscription
	 * @return true if the subscription if valid, false otherwise
	 */
	public boolean checkValidSubscriptionBySubscriptionId(String sSubscriptionId) {
		return m_oBackend.checkValidSubscriptionBySubscriptionId(sSubscriptionId);
	}

	/**
	 * Get the subscriptions by owner.
	 * @param sUserId the owner of the subscriptions
	 * @return the list of subscriptions found
	 */
	public List<Subscription> getSubscriptionsByUser(String sUserId) {
		return m_oBackend.getSubscriptionsByUser(sUserId);
	}

	/**
	 * Get the subscriptions of an organization.
	 * @param sOrganizationId the organizationId of the subscriptions
	 * @return the list of subscriptions found
	 */
	public List<Subscription> getSubscriptionsByOrganization(String sOrganizationId) {
		return m_oBackend.getSubscriptionsByOrganization(sOrganizationId);
	}

	/**
	 * Check whether or not the organization has subscriptions.
	 * @param sOrganizationId the organizationId of the subscriptions
	 * @return true if the organization has associated subscriptions
	 */
	public boolean organizationHasSubscriptions(String sOrganizationId) {
		return m_oBackend.organizationHasSubscriptions(sOrganizationId);
	}

	/**
	 * Get the subscriptions related to many organizations.
	 * @param asOrganizationIds the list of organizationIds
	 * @return the list of subscriptions associated with the organizations
	 */
	public List<Subscription> getSubscriptionsByOrganizations(Collection<String> asOrganizationIds) {
		return m_oBackend.getSubscriptionsByOrganizations(asOrganizationIds);
	}

	/**
	 * Get an subscription by its name.
	 * @param sName the name of the subscription
	 * @return the subscription if found, null otherwise
	 */
	public Subscription getByName(String sName) {
		return m_oBackend.getByName(sName);
	}

	/**
	 * Get an subscription by its name and the user id
	 * @param sName the name of the subscription
	 * @return the subscription if found, null otherwise
	 */
	public Subscription getByNameAndUserId(String sName, String sUserId) {
		return m_oBackend.getByNameAndUserId(sName, sUserId);
	}

	/**
	 * Delete the subscription.
	 * @param sSubscriptionId the id of the subscription
	 * @return true if the deletion was successful, false otherwise
	 */
	public boolean deleteSubscription(String sSubscriptionId) {
		return m_oBackend.deleteSubscription(sSubscriptionId);
	}

	/**
	 * Delete all subscriptions belonging to a specific user.
	 * @param sUserId the owner of the subscription
	 * @return the number of subscriptions deleted, 0 if none
	 */
	public int deleteByUser(String sUserId) {
		return m_oBackend.deleteByUser(sUserId);
	}

	/**
	 * Check if User is the owner of Subscription.
	 * @param sUserId the owner's id
	 * @param sSubscriptionId the subscription's id
	 * @return true if the user is the owner, false otherwise
	 */
	public boolean isOwnedByUser(String sUserId, String sSubscriptionId) {
		return m_oBackend.isOwnedByUser(sUserId, sSubscriptionId);
	}

	/**
	 * Get the list of subscriptions.
	 * @return the list of subscriptions
	 */
	public List<Subscription> getSubscriptionsList() {
		return m_oBackend.getSubscriptionsList();
	}

	/**
	 * Get the list of subscriptions.
	 * @return the list of subscriptions
	 */
	public long getSubscriptionsCount() {
		return m_oBackend.getSubscriptionsCount();
	}

	/**
	 * Get the sorted list of subscriptions.
	 * @return the sorted list of subscriptions
	 */
	public List<Subscription> getSubscriptionsSortedList(String sOrderBy, int iOrder) {
		return m_oBackend.getSubscriptionsSortedList(sOrderBy, iOrder);
	}

	/**
	 * Find subscriptions by partial name, by partial id or partial user id
	 * @param sNameFilter the partial name of the subscription
	 * @param sIdFilter the partial id of the subscription
	 * @param sUserIdFilter the partial user id of the subscription
	 * @return the list of subscriptions that partially match the name or id
	 */
	public List<Subscription> findSubscriptionsByFilters(String sNameFilter, String sIdFilter, String sUserIdFilter, String sOrderBy, int iOrder) {
		return m_oBackend.findSubscriptionsByFilters(sNameFilter, sIdFilter, sUserIdFilter, sOrderBy, iOrder);
	}

	/**
	 * Get a List of all the subscriptions avaiable for one user.
	 * Can be the owner, or linked in an organization, or shared by a user
	 * @param sUserId
	 * @return
	 */
	public List<Subscription> getAllSubscriptionsOfUser(String sUserId) {
		return m_oBackend.getAllSubscriptionsOfUser(sUserId);
	}

	/**
	 * Find a subscription that  matches a given partial name, description or id
	 * @param sPartialName the partial name of the subscription
	 * @return
	 */
	public List<Subscription> findSubscriptionByPartialName(String sPartialName) {
		return m_oBackend.findSubscriptionByPartialName(sPartialName);
	}
}

