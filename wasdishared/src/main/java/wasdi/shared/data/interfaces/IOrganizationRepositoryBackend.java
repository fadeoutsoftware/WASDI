package wasdi.shared.data.interfaces;

import java.util.Collection;
import java.util.List;

import wasdi.shared.business.Organization;

/**
 * Backend contract for organization repository.
 */
public interface IOrganizationRepositoryBackend {

	boolean insertOrganization(Organization oOrganization);

	boolean updateOrganization(Organization oOrganization);

	Organization getById(String sOrganizationId);

	List<Organization> getOrganizations(Collection<String> asOrganizationIds);

	List<Organization> getOrganizationsOwnedByUser(String sUserId);

	Organization getByName(String sName);

	boolean deleteOrganization(String sOrganizationId);

	int deleteByUser(String sUserId);

	boolean isOwnedByUser(String sUserId, String sOrganizationId);

	List<Organization> getOrganizationsList();

	long getOrganizationsCount();

	List<Organization> findOrganizationsByPartialName(String sPartialName);
}
