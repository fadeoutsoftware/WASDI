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

import wasdi.shared.business.Project;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.interfaces.IProjectRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for project repository.
 */
public class MongoProjectRepositoryBackend extends MongoRepository implements IProjectRepositoryBackend {

	public MongoProjectRepositoryBackend() {
		m_sThisCollection = "projects";
	}

	@Override
	public boolean insertProject(Project oProject) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oProject);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.insertProject: exception ", oEx);
		}

		return false;
	}

	@Override
	public boolean updateProject(Project oProject) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oProject);

			Bson oFilter = new Document("projectId", oProject.getProjectId());
			Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));

			UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

			if (oResult.getMatchedCount() == 1) {
				return true;
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.updateProject: exception ", oEx);
		}

		return false;
	}

	@Override
	public Project getProjectById(String sProjectId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("projectId", sProjectId)).first();

			if (oWSDocument != null) {
				String sJSON = oWSDocument.toJson();

				return s_oMapper.readValue(sJSON, Project.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.getProjectById: exception ", oEx);
		}

		return null;
	}

	@Override
	public List<Project> getProjectsBySubscription(String sSubscriptionId) {
		final List<Project> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("subscriptionId", sSubscriptionId));

			fillList(aoReturnList, oWSDocuments, Project.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.getProjectsBySubscription: exception ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<Project> getProjectsBySubscriptions(Collection<String> asSubscriptionIds) {
		final List<Project> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.in("subscriptionId", asSubscriptionIds));

			fillList(aoReturnList, oWSDocuments, Project.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.getProjectsBySubscriptions: exception ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public boolean checkValidSubscription(String sProjectId) {
		if (Utils.isNullOrEmpty(sProjectId)) {
			return false;
		}

		Project oProject = this.getProjectById(sProjectId);

		return checkValidSubscription(oProject);
	}

	@Override
	public boolean checkValidSubscription(Project oProject) {
		if (oProject == null || Utils.isNullOrEmpty(oProject.getSubscriptionId())) {
			return false;
		}

		return new SubscriptionRepository().checkValidSubscriptionBySubscriptionId(oProject.getSubscriptionId());
	}

	@Override
	public Project getByName(String sName) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(Filters.eq("name", sName)).first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();

				Project oProject = null;
				try {
					oProject = s_oMapper.readValue(sJSON, Project.class);
				} catch (IOException e) {
					WasdiLog.errorLog("ProjectRepository.getByName: exception ", e);
				}

				return oProject;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("ProjectRepository.getByName( " + sName + "): error: ", oE);
		}

		return null;
	}

	@Override
	public boolean deleteProject(String sProjectId) {
		if (Utils.isNullOrEmpty(sProjectId)) {
			return false;
		}

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("projectId", sProjectId));

			if (oDeleteResult != null) {
				if (oDeleteResult.getDeletedCount() == 1) {
					return true;
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.deleteProject: exception ", oEx);
		}

		return false;
	}

	@Override
	public int deleteBySubscription(String sSubscriptionId) {
		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			return 0;
		}

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("subscriptionId", sSubscriptionId));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.deleteBySubscription: exception ", oEx);
		}

		return 0;
	}

	@Override
	public List<Project> getProjectsList() {
		final List<Project> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

			fillList(aoReturnList, oWSDocuments, Project.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.getProjectsList: exception ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<Project> findProjectsByPartialName(String sPartialName) {
		List<Project> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoReturnList;
		}

		Pattern regex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);

		Bson oFilterLikeProjectId = Filters.eq("projectId", regex);
		Bson oFilterLikeName = Filters.eq("name", regex);

		Bson oFilter = Filters.or(oFilterLikeProjectId, oFilterLikeName);

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oFilter).sort(new Document("name", 1));

			fillList(aoReturnList, oWSDocuments, Project.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectRepository.findProjectsByPartialName: exception ", oEx);
		}

		return aoReturnList;
	}
}
