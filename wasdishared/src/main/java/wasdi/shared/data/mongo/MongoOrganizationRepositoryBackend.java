package wasdi.shared.data.mongo;

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
import wasdi.shared.data.interfaces.IOrganizationRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for organization repository.
 */
public class MongoOrganizationRepositoryBackend extends MongoRepository implements IOrganizationRepositoryBackend {

	public MongoOrganizationRepositoryBackend() {
		m_sThisCollection = "organizations";
	}

	@Override
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

	@Override
	public boolean updateOrganization(Organization oOrganization) {
		try {
			String sJSON = s_oMapper.writeValueAsString(oOrganization);

			Bson oFilter = new Document("organizationId", oOrganization.getOrganizationId());
			Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));

			UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

			if (oResult.getModifiedCount() == 1) {
				return true;
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.updateOrganization: error ", oEx);
		}

		return false;
	}

	@Override
	public Organization getById(String sOrganizationId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.eq("organizationId", sOrganizationId))
					.first();

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

	@Override
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

	@Override
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

	@Override
	public Organization getByName(String sName) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(Filters.eq("name", sName)).first();

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

	@Override
	public boolean deleteOrganization(String sOrganizationId) {
		if (Utils.isNullOrEmpty(sOrganizationId)) {
			return false;
		}

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

	@Override
	public int deleteByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

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

	@Override
	public boolean isOwnedByUser(String sUserId, String sOrganizationId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("organizationId", sOrganizationId)))
					.first();

			if (null != oWSDocument) {
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("OrganizationRepository.isOwnedByUser( " + sUserId + ", " + sOrganizationId + " ): error: ", oE);
		}

		return false;
	}

	@Override
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

	@Override
	public long getOrganizationsCount() {
		return getCollection(m_sThisCollection).countDocuments();
	}

	@Override
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
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(oFilter)
					.sort(new Document("name", 1));

			fillList(aoReturnList, oWSDocuments, Organization.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationRepository.findOrganizationsByPartialName: error ", oEx);
		}

		return aoReturnList;
	}
}
