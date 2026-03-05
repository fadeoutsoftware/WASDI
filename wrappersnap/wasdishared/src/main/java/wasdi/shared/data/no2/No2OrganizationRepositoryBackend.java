package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.Organization;
import wasdi.shared.data.interfaces.IOrganizationRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for organization repository.
 */
public class No2OrganizationRepositoryBackend extends No2Repository implements IOrganizationRepositoryBackend {

	private static final String s_sCollectionName = "organizations";

	@Override
	public boolean insertOrganization(Organization oOrganization) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oOrganization == null) {
				return false;
			}

			oCollection.insert(toDocument(oOrganization));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OrganizationRepositoryBackend.insertOrganization: error", oEx);
		}

		return false;
	}

	@Override
	public boolean updateOrganization(Organization oOrganization) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oOrganization == null) {
				return false;
			}

			oCollection.update(where("organizationId").eq(oOrganization.getOrganizationId()), toDocument(oOrganization));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OrganizationRepositoryBackend.updateOrganization: error", oEx);
		}

		return false;
	}

	@Override
	public Organization getById(String sOrganizationId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("organizationId").eq(sOrganizationId))) {
				return fromDocument(oDocument, Organization.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OrganizationRepositoryBackend.getById: error", oEx);
		}

		return null;
	}

	@Override
	public List<Organization> getOrganizations(Collection<String> asOrganizationIds) {
		List<Organization> aoResults = new ArrayList<>();

		if (asOrganizationIds == null || asOrganizationIds.isEmpty()) {
			return aoResults;
		}

		try {
			for (Organization oOrganization : getOrganizationsList()) {
				if (asOrganizationIds.contains(oOrganization.getOrganizationId())) {
					aoResults.add(oOrganization);
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OrganizationRepositoryBackend.getOrganizations: error", oEx);
		}

		return aoResults;
	}

	@Override
	public List<Organization> getOrganizationsOwnedByUser(String sUserId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return new ArrayList<>();
			}

			DocumentCursor oCursor = oCollection.find(where("userId").eq(sUserId));
			return toList(oCursor, Organization.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OrganizationRepositoryBackend.getOrganizationsOwnedByUser: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public Organization getByName(String sName) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("name").eq(sName))) {
				return fromDocument(oDocument, Organization.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OrganizationRepositoryBackend.getByName: error", oEx);
		}

		return null;
	}

	@Override
	public boolean deleteOrganization(String sOrganizationId) {
		if (Utils.isNullOrEmpty(sOrganizationId)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.remove(where("organizationId").eq(sOrganizationId));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OrganizationRepositoryBackend.deleteOrganization: error", oEx);
		}

		return false;
	}

	@Override
	public int deleteByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			List<Organization> aoOwned = getOrganizationsOwnedByUser(sUserId);
			NitriteCollection oCollection = getCollection(s_sCollectionName);

			if (oCollection == null) {
				return 0;
			}

			oCollection.remove(where("userId").eq(sUserId));
			return aoOwned.size();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OrganizationRepositoryBackend.deleteByUser: error", oEx);
		}

		return 0;
	}

	@Override
	public boolean isOwnedByUser(String sUserId, String sOrganizationId) {
		Organization oOrganization = getById(sOrganizationId);
		return oOrganization != null && sUserId != null && sUserId.equals(oOrganization.getUserId());
	}

	@Override
	public List<Organization> getOrganizationsList() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find() : null;
			return toList(oCursor, Organization.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OrganizationRepositoryBackend.getOrganizationsList: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public long getOrganizationsCount() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			return oCollection != null ? oCollection.size() : 0;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OrganizationRepositoryBackend.getOrganizationsCount: error", oEx);
		}

		return 0;
	}

	@Override
	public List<Organization> findOrganizationsByPartialName(String sPartialName) {
		List<Organization> aoResults = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoResults;
		}

		String sLookup = sPartialName.toLowerCase();

		for (Organization oOrganization : getOrganizationsList()) {
			if (oOrganization == null) {
				continue;
			}

			String sOrgId = oOrganization.getOrganizationId() != null ? oOrganization.getOrganizationId().toLowerCase() : "";
			String sName = oOrganization.getName() != null ? oOrganization.getName().toLowerCase() : "";

			if (sOrgId.contains(sLookup) || sName.contains(sLookup)) {
				aoResults.add(oOrganization);
			}
		}

		return aoResults;
	}
}
