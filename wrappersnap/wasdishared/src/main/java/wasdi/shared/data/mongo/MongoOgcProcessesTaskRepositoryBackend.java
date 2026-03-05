package wasdi.shared.data.mongo;

import org.bson.Document;

import com.mongodb.BasicDBObject;

import wasdi.shared.business.OgcProcessesTask;
import wasdi.shared.data.interfaces.IOgcProcessesTaskRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for OGC processes task repository.
 */
public class MongoOgcProcessesTaskRepositoryBackend extends MongoRepository implements IOgcProcessesTaskRepositoryBackend {

	public MongoOgcProcessesTaskRepositoryBackend() {
		m_sThisCollection = "ogcprocessestask";
	}

	@Override
	public String insertOgcProcessesTask(OgcProcessesTask oOgcProcessesTask) {
		return add(oOgcProcessesTask);
	}

	@Override
	public int deleteOgcProcessesTask(String sProcessWorkspaceId) {
		try {
			if (Utils.isNullOrEmpty(sProcessWorkspaceId)) {
				return 0;
			}

			BasicDBObject oCriteria = new BasicDBObject();
			oCriteria.append("processWorkspaceId", sProcessWorkspaceId);

			return delete(oCriteria);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesTaskRepository.deleteOgcProcessesTask: error", oEx);
			return -1;
		}
	}

	@Override
	public int deleteOgcProcessesTaskByUser(String sUserId) {
		try {
			if (Utils.isNullOrEmpty(sUserId)) {
				return 0;
			}

			BasicDBObject oCriteria = new BasicDBObject();
			oCriteria.append("userId", sUserId);

			return deleteMany(oCriteria);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesTaskRepository.deleteOgcProcessesTaskByUser: error", oEx);
			return -1;
		}
	}

	@Override
	public int deleteOgcProcessesTaskByWorkspace(String sWorkspaceId) {
		try {
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				return 0;
			}

			BasicDBObject oCriteria = new BasicDBObject();
			oCriteria.append("workspaceId", sWorkspaceId);

			return deleteMany(oCriteria);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesTaskRepository.deleteOgcProcessesTaskByWorkspace: error", oEx);
			return -1;
		}
	}

	@Override
	public boolean updateOgcProcessesTask(OgcProcessesTask oOgcProcessesTask) {
		try {
			BasicDBObject oCriteria = new BasicDBObject();
			oCriteria.append("processWorkspaceId", oOgcProcessesTask.getProcessWorkspaceId());

			return update(oCriteria, oOgcProcessesTask, m_sThisCollection);
		} catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesTaskRepository.updateOgcProcessesTask: error", oEx);
			return false;
		}
	}

	@Override
	public OgcProcessesTask getOgcProcessesTask(String sProcessWorkspaceId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(new Document("processWorkspaceId", sProcessWorkspaceId))
					.first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();
				return s_oMapper.readValue(sJSON, OgcProcessesTask.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesTaskRepository.getOgcProcessesTask: error", oEx);
		}

		return null;
	}
}
