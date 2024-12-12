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
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class OrganizationRepository extends MongoRepository {

	public OrganizationRepository() {
		m_sThisCollection = "organizations";
	}

	/**
	 * Insert a new organization.
	 * 
	 * @param oOrganization the organization to be inserted
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean insertOrganization(Organization oOrganization) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oOrganization);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.insertOrganization: error ", oEx);
		}

		return false;
	}

	/**
	 * Update an organization.
	 * 
	 * @param oOrganization the organization to be updated
	 * @return true if the operation succeeded, false otherwise
	 */
	public boolean updateOrganization(Organization oOrganization) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oOrganization);

			Bson oFilter = new Document("organizationId", oOrganization.getOrganizationId());
			Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));

			UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

			if (oResult.getModifiedCount() == 1)
				return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.updateOrganization: error ", oEx);
		}

		return false;
	}

	/**
	 * Get an organization by its Id.
	 * @param sOrganizationId the Id of the organization
	 * @return the organization if found, null otherwise
	 */
	public Organization getById(String sOrganizationId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.eq("organizationId", sOrganizationId)).first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();

				Organization oOrganization = null;
				try {
					oOrganization = s_oMapper.readValue(sJSON, Organization.class);
				} catch (IOException e) {
					WasdiLog.errorLog("OrganizationRepository.getById: error", e);
				}

				return oOrganization;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("OrganizationRepository.getById( " + sOrganizationId + "): error: ", oE);
		}

		return null;
	}

	/**
	 * Get organizations by their ids.
	 * @param asOrganizationIds the ids of the organizations
	 * @return the list of organizations corresponding to the ids
	 */
	public List<Organization> getOrganizations(Collection<String> asOrganizationIds) {
		final List<Organization> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(Filters.in("organizationId", asOrganizationIds));

			fillList(aoReturnList, oWSDocuments, Organization.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.getOrganizations: error ", oEx);
		}

		return aoReturnList;
	}

	/**
	 * Get an organization by its owner.
	 * @param sUserId the owner of the organization
	 * @return the organization if found, null otherwise
	 */
	public List<Organization> getOrganizationsOwnedByUser(String sUserId) {
		final List<Organization> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("userId", sUserId));

			fillList(aoReturnList, oWSDocuments, Organization.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.getOrganizationsOwnedByUser: error ", oEx);
		}

		return aoReturnList;
	}

	/**
	 * Get an organization by its name.
	 * @param sName the name of the organization
	 * @return the organization if found, null otherwise
	 */
	public Organization getByName(String sName) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.eq("name", sName)).first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();

				Organization oOrganization = null;
				try {
					oOrganization = s_oMapper.readValue(sJSON, Organization.class);
				} catch (IOException e) {
					WasdiLog.errorLog("OrganizationRepository.getByName: error", e);
				}

				return oOrganization;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("OrganizationRepository.getByName( " + sName + "): error: ", oE);
		}

		return null;
	}

	/**
	 * Delete the organization.
	 * @param sOrganizationId the id of the organization
	 * @return true if the deletion was successful, false otherwise
	 */
	public boolean deleteOrganization(String sOrganizationId) {
		if (Utils.isNullOrEmpty(sOrganizationId))
			return false;

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection)
					.deleteOne(new Document("organizationId", sOrganizationId));

			if (oDeleteResult != null) {
				if (oDeleteResult.getDeletedCount() == 1) {
					return true;
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.deleteOrganization: error ", oEx);
		}

		return false;
	}

	/**
	 * Delete all organizations belonging to a specific user.
	 * @param sUserId the owner of the organization
	 * @return the number of organizations deleted, 0 if none
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
			WasdiLog.errorLog("OrganizationRepository.deleteByUser: error ", oEx);
		}

		return 0;
	}

	/**
	 * Check if User is the owner of Organization.
	 * 
	 * @param sUserId the owner's id
	 * @param sOrganizationId the organization's id
	 * @return true if the user is the owner, false otherwise
	 */
	public boolean isOwnedByUser(String sUserId, String sOrganizationId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("organizationId", sOrganizationId))).first();

			if (null != oWSDocument) {
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("OrganizationRepository.isOwnedByUser( " + sUserId + ", " + sOrganizationId + " ): error: ", oE);
		}

		return false;
	}

	/**
	 * Get the list of organizations.
	 * @return the list of organizations
	 */
	public List<Organization> getOrganizationsList() {
		final List<Organization> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

			fillList(aoReturnList, oWSDocuments, Organization.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.getOrganizationsList: error ", oEx);
		}

		return aoReturnList;
	}
	
	/**
	 * Get the list of organizations.
	 * @return the list of organizations
	 */
	public long getOrganizationsCount() {
		return getCollection(m_sThisCollection).countDocuments();
	}


	/**
	 * Find organizations by partial name or by partial id
	 * @param sPartialName the partial name or partial id
	 * @return the list of organizations that partially match the name or id
	 */
	public List<Organization> findOrganizationsByPartialName(String sPartialName) {
		List<Organization> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoReturnList;
		}

		Pattern regex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);

		Bson oFilterLikeOrganizationId = Filters.eq("organizationId", regex);
		Bson oFilterLikeName = Filters.eq("name", regex);

		Bson oFilter = Filters.or(oFilterLikeOrganizationId, oFilterLikeName);

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oFilter)
					.sort(new Document("name", 1));

			fillList(aoReturnList, oWSDocuments, Organization.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.findOrganizationsByPartialName: error ", oEx);
		}

		return aoReturnList;
	}

}
