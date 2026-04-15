package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import wasdi.shared.business.Organization;
import wasdi.shared.data.interfaces.IOrganizationRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class SqliteOrganizationRepositoryBackend extends SqliteRepository implements IOrganizationRepositoryBackend {

	public SqliteOrganizationRepositoryBackend() {
		m_sThisCollection = "organizations";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insertOrganization(Organization oOrganization) {
		try {
			return insert(m_sThisCollection, oOrganization.getOrganizationId(), oOrganization);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.insertOrganization: error ", oEx);
		}

		return false;
	}

	@Override
	public boolean updateOrganization(Organization oOrganization) {
		try {
			return updateWhere(m_sThisCollection, "organizationId", oOrganization.getOrganizationId(), oOrganization);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.updateOrganization: error ", oEx);
		}

		return false;
	}

	@Override
	public Organization getById(String sOrganizationId) {
		try {
			return findOneWhere(m_sThisCollection, "organizationId", sOrganizationId, Organization.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.getById( " + sOrganizationId + "): error: ", oEx);
		}

		return null;
	}

	@Override
	public List<Organization> getOrganizations(Collection<String> asOrganizationIds) {
		final List<Organization> aoReturnList = new ArrayList<>();

		if (asOrganizationIds == null || asOrganizationIds.isEmpty()) {
			return aoReturnList;
		}

		try {
			StringBuilder oSb = new StringBuilder("SELECT data FROM " + m_sThisCollection + " WHERE json_extract(data,'$.organizationId') IN (");
			List<Object> aoParams = new ArrayList<>();
			for (String sId : asOrganizationIds) {
				oSb.append("?,");
				aoParams.add(sId);
			}
			oSb.setCharAt(oSb.length() - 1, ')');

			return queryList(oSb.toString(), aoParams, Organization.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.getOrganizations: error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<Organization> getOrganizationsOwnedByUser(String sUserId) {
		try {
			return findAllWhere(m_sThisCollection, "userId", sUserId, Organization.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.getOrganizationsOwnedByUser: error ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public Organization getByName(String sName) {
		try {
			return findOneWhere(m_sThisCollection, "name", sName, Organization.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.getByName( " + sName + "): error: ", oEx);
		}

		return null;
	}

	@Override
	public boolean deleteOrganization(String sOrganizationId) {
		if (Utils.isNullOrEmpty(sOrganizationId)) {
			return false;
		}

		try {
			int iDeleteCount = deleteWhere(m_sThisCollection, "organizationId", sOrganizationId); 
			return iDeleteCount == 1;
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.deleteOrganization: error ", oEx);
		}

		return false;
	}

	@Override
	public int deleteByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			int iCount = (int) countWhere(m_sThisCollection, "userId", sUserId);
			deleteWhere(m_sThisCollection, "userId", sUserId);
			return iCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.deleteByUser: error ", oEx);
		}

		return 0;
	}

	@Override
	public boolean isOwnedByUser(String sUserId, String sOrganizationId) {
		try {
			LinkedHashMap<String, Object> oFilter = new LinkedHashMap<>();
			oFilter.put("userId", sUserId);
			oFilter.put("organizationId", sOrganizationId);
			return countWhere(m_sThisCollection, oFilter) > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.isOwnedByUser( " + sUserId + ", " + sOrganizationId + " ): error: ", oEx);
		}

		return false;
	}

	@Override
	public List<Organization> getOrganizationsList() {
		try {
			return findAll(m_sThisCollection, Organization.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.getOrganizationsList: error ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public long getOrganizationsCount() {
		return count(m_sThisCollection);
	}

	@Override
	public List<Organization> findOrganizationsByPartialName(String sPartialName) {
		List<Organization> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoReturnList;
		}

		try {
			String sQuery = "SELECT data FROM " + m_sThisCollection
					+ " WHERE LOWER(json_extract(data,'$.organizationId')) LIKE LOWER(?)"
					+ " OR LOWER(json_extract(data,'$.name')) LIKE LOWER(?)"
					+ " ORDER BY json_extract(data,'$.name') ASC";

			String sPattern = "%" + sPartialName + "%";
			return queryList(sQuery, Arrays.asList(sPattern, sPattern), Organization.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.findOrganizationsByPartialName: error ", oEx);
		}

		return aoReturnList;
	}
}
