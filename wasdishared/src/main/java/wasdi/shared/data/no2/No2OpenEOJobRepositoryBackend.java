package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.OpenEOJob;
import wasdi.shared.data.interfaces.IOpenEOJobRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for OpenEO job repository.
 */
public class No2OpenEOJobRepositoryBackend extends No2Repository implements IOpenEOJobRepositoryBackend {

	private static final String s_sCollectionName = "openeojobs";

	@Override
	public String insertOpenEOJob(OpenEOJob oOpenEOJob) {
		if (oOpenEOJob == null) {
			return "";
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return "";
			}

			oCollection.insert(toDocument(oOpenEOJob));
			return oOpenEOJob.getJobId();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OpenEOJobRepositoryBackend.insertOpenEOJob: error", oEx);
		}

		return "";
	}

	@Override
	public int deleteOpenEOJob(String sJobId) {
		if (Utils.isNullOrEmpty(sJobId)) {
			return 0;
		}
		return deleteByField("jobId", sJobId, false);
	}

	@Override
	public int deleteOpenEOJobsByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}
		return deleteByField("userId", sUserId, true);
	}

	@Override
	public int deleteOpenEOJobsByWorkspace(String sWorkspaceId) {
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			return 0;
		}
		return deleteByField("workspaceId", sWorkspaceId, true);
	}

	@Override
	public boolean updateOpenEOJob(OpenEOJob oOpenEOJob) {
		if (oOpenEOJob == null || Utils.isNullOrEmpty(oOpenEOJob.getJobId())) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}
			oCollection.update(where("jobId").eq(oOpenEOJob.getJobId()), toDocument(oOpenEOJob));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OpenEOJobRepositoryBackend.updateOpenEOJob: error", oEx);
		}

		return false;
	}

	@Override
	public OpenEOJob getOpenEOJob(String sJobId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("jobId").eq(sJobId))) {
				return fromDocument(oDocument, OpenEOJob.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OpenEOJobRepositoryBackend.getOpenEOJob: error", oEx);
		}

		return null;
	}

	@Override
	public List<OpenEOJob> getOpenEOJobsByUser(String sUserId) {
		List<OpenEOJob> aoReturnList = new ArrayList<>();
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find(where("userId").eq(sUserId)) : null;
			aoReturnList = toList(oCursor, OpenEOJob.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OpenEOJobRepositoryBackend.getOpenEOJobsByUser: error", oEx);
		}
		return aoReturnList;
	}

	private int deleteByField(String sField, String sValue, boolean bMany) {
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
			if (bMany) {
				return iCount;
			}
			return iCount > 0 ? 1 : 0;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2OpenEOJobRepositoryBackend.deleteByField: error", oEx);
			return -1;
		}
	}
}
