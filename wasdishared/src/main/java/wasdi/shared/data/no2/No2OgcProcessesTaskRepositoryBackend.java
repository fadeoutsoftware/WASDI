package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.OgcProcessesTask;
import wasdi.shared.data.interfaces.IOgcProcessesTaskRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for OGC processes task repository.
 */
public class No2OgcProcessesTaskRepositoryBackend extends No2Repository implements IOgcProcessesTaskRepositoryBackend {

	private static final String s_sCollectionName = "ogcprocessestask";

	@Override
	public String insertOgcProcessesTask(OgcProcessesTask oOgcProcessesTask) {
		if (oOgcProcessesTask == null) {
			return "";
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return "";
			}

			oCollection.insert(toDocument(oOgcProcessesTask));
			return oOgcProcessesTask.getProcessWorkspaceId();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OgcProcessesTaskRepositoryBackend.insertOgcProcessesTask: error", oEx);
		}

		return "";
	}

	@Override
	public int deleteOgcProcessesTask(String sProcessWorkspaceId) {
		if (Utils.isNullOrEmpty(sProcessWorkspaceId)) {
			return 0;
		}

		return deleteByField("processWorkspaceId", sProcessWorkspaceId);
	}

	@Override
	public int deleteOgcProcessesTaskByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		return deleteManyByField("userId", sUserId);
	}

	@Override
	public int deleteOgcProcessesTaskByWorkspace(String sWorkspaceId) {
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			return 0;
		}

		return deleteManyByField("workspaceId", sWorkspaceId);
	}

	@Override
	public boolean updateOgcProcessesTask(OgcProcessesTask oOgcProcessesTask) {
		if (oOgcProcessesTask == null || Utils.isNullOrEmpty(oOgcProcessesTask.getProcessWorkspaceId())) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.update(where("processWorkspaceId").eq(oOgcProcessesTask.getProcessWorkspaceId()), toDocument(oOgcProcessesTask));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OgcProcessesTaskRepositoryBackend.updateOgcProcessesTask: error", oEx);
		}

		return false;
	}

	@Override
	public OgcProcessesTask getOgcProcessesTask(String sProcessWorkspaceId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("processWorkspaceId").eq(sProcessWorkspaceId))) {
				return fromDocument(oDocument, OgcProcessesTask.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OgcProcessesTaskRepositoryBackend.getOgcProcessesTask: error", oEx);
		}

		return null;
	}

	private int deleteByField(String sField, String sValue) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return -1;
			}

			int iCount = 0;
			for (Document oDocument : oCollection.find(where(sField).eq(sValue))) {
				if (oDocument != null) {
					iCount++;
				}
			}

			oCollection.remove(where(sField).eq(sValue));
			return iCount > 0 ? 1 : 0;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OgcProcessesTaskRepositoryBackend.deleteByField: error", oEx);
			return -1;
		}
	}

	private int deleteManyByField(String sField, String sValue) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return -1;
			}

			int iCount = 0;
			for (Document oDocument : oCollection.find(where(sField).eq(sValue))) {
				if (oDocument != null) {
					iCount++;
				}
			}

			oCollection.remove(where(sField).eq(sValue));
			return iCount;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OgcProcessesTaskRepositoryBackend.deleteManyByField: error", oEx);
			return -1;
		}
	}
}
