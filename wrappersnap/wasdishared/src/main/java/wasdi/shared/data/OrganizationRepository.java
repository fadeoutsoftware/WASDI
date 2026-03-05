package wasdi.shared.data;

import java.util.Collection;
import java.util.List;

import wasdi.shared.business.Organization;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IOrganizationRepositoryBackend;

public class OrganizationRepository {

	private final IOrganizationRepositoryBackend m_oBackend;

	public OrganizationRepository() {
		m_oBackend = createBackend();
	}

	private IOrganizationRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createOrganizationRepository();
	}

	/**
	 * Insert a new organization.
	 * 
	 * @param oOrganization the organization to be inserted
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean insertOrganization(Organization oOrganization) {
		return m_oBackend.insertOrganization(oOrganization);
	}

	/**
	 * Update an organization.
	 * 
	 * @param oOrganization the organization to be updated
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean updateOrganization(Organization oOrganization) {
		return m_oBackend.updateOrganization(oOrganization);
	}

	/**
	 * Get an organization by its Id.
	 * @param sOrganizationId the Id of the organization
	 * @return the organization if found, null otherwise
	 */
	public Organization getById(String sOrganizationId) {
		return m_oBackend.getById(sOrganizationId);
	}

	/**
	 * Get organizations by their ids.
	 * @param asOrganizationIds the ids of the organizations
	 * @return the list of organizations corresponding to the ids
	 */
	public List<Organization> getOrganizations(Collection<String> asOrganizationIds) {
		return m_oBackend.getOrganizations(asOrganizationIds);
	}

	/**
	 * Get an organization by its owner.
	 * @param sUserId the owner of the organization
	 * @return the organization if found, null otherwise
	 */
	public List<Organization> getOrganizationsOwnedByUser(String sUserId) {
		return m_oBackend.getOrganizationsOwnedByUser(sUserId);
	}

	/**
	 * Get an organization by its name.
	 * @param sName the name of the organization
	 * @return the organization if found, null otherwise
	 */
	public Organization getByName(String sName) {
		return m_oBackend.getByName(sName);
	}

	/**
	 * Delete the organization.
	 * @param sOrganizationId the id of the organization
	 * @return true if the deletion was successful, false otherwise
	 */
	public boolean deleteOrganization(String sOrganizationId) {
		return m_oBackend.deleteOrganization(sOrganizationId);
	}

	/**
	 * Delete all organizations belonging to a specific user.
	 * @param sUserId the owner of the organization
	 * @return the number of organizations deleted, 0 if none
	 */
	public int deleteByUser(String sUserId) {
		return m_oBackend.deleteByUser(sUserId);
	}

	/**
	 * Check if User is the owner of Organization.
	 * 
	 * @param sUserId the owner's id
	 * @param sOrganizationId the organization's id
	 * @return true if the user is the owner, false otherwise
	 */
	public boolean isOwnedByUser(String sUserId, String sOrganizationId) {
		return m_oBackend.isOwnedByUser(sUserId, sOrganizationId);
	}

	/**
	 * Get the list of organizations.
	 * @return the list of organizations
	 */
	public List<Organization> getOrganizationsList() {
		return m_oBackend.getOrganizationsList();
	}
	
	/**
	 * Get the list of organizations.
	 * @return the list of organizations
	 */
	public long getOrganizationsCount() {
		return m_oBackend.getOrganizationsCount();
	}


	/**
	 * Find organizations by partial name or by partial id
	 * @param sPartialName the partial name or partial id
	 * @return the list of organizations that partially match the name or id
	 */
	public List<Organization> findOrganizationsByPartialName(String sPartialName) {
		return m_oBackend.findOrganizationsByPartialName(sPartialName);
	}

}

