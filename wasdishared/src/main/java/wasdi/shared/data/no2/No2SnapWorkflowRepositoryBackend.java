package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.data.interfaces.ISnapWorkflowRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for snap workflow repository.
 */
public class No2SnapWorkflowRepositoryBackend extends No2Repository implements ISnapWorkflowRepositoryBackend {

	private static final String s_sCollectionName = "snapworkflows";

	@Override
	public boolean insertSnapWorkflow(SnapWorkflow oWorkflow) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oWorkflow == null) {
				return false;
			}

			oCollection.insert(toDocument(oWorkflow));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SnapWorkflowRepositoryBackend.insertSnapWorkflow", oEx);
		}

		return false;
	}

	@Override
	public SnapWorkflow getSnapWorkflow(String sWorkflowId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("workflowId").eq(sWorkflowId))) {
				return fromDocument(oDocument, SnapWorkflow.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SnapWorkflowRepositoryBackend.getSnapWorkflow", oEx);
		}

		return null;
	}
	
	@Override
	public SnapWorkflow getByName(String sWorkflowName) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("name").eq(sWorkflowName))) {
				return fromDocument(oDocument, SnapWorkflow.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SnapWorkflowRepositoryBackend.getByName", oEx);
		}

		return null;
	}


	@Override
	public List<SnapWorkflow> getSnapWorkflowPublicAndByUser(String sUserId) {
		HashSet<SnapWorkflow> aoReturnSet = new HashSet<>();

		try {
			for (SnapWorkflow oWorkflow : getList()) {
				if (oWorkflow != null && (equalsSafe(oWorkflow.getUserId(), sUserId) || oWorkflow.getIsPublic())) {
					aoReturnSet.add(oWorkflow);
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SnapWorkflowRepositoryBackend.getSnapWorkflowPublicAndByUser", oEx);
		}

		return new ArrayList<>(aoReturnSet);
	}

	@Override
	public List<SnapWorkflow> getList() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			return toList(oCollection != null ? oCollection.find() : null, SnapWorkflow.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SnapWorkflowRepositoryBackend.getList", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<SnapWorkflow> findWorkflowByPartialName(String sPartialName) {
		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return null;
		}

		ArrayList<SnapWorkflow> aoReturnList = new ArrayList<>();
		Pattern oRegex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);

		try {
			for (SnapWorkflow oWorkflow : getList()) {
				if (oWorkflow == null) {
					continue;
				}

				if (matches(oRegex, oWorkflow.getWorkflowId())
						|| matches(oRegex, oWorkflow.getName())
						|| matches(oRegex, oWorkflow.getDescription())) {
					aoReturnList.add(oWorkflow);
				}
			}

			aoReturnList.sort((oA, oB) -> compareNullableString(
					oA != null ? oA.getName() : null,
					oB != null ? oB.getName() : null));
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SnapWorkflowRepositoryBackend.findWorkflowByPartialName", oEx);
		}

		return aoReturnList;
	}

	@Override
	public boolean updateSnapWorkflow(SnapWorkflow oSnapWorkflow) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oSnapWorkflow == null) {
				return false;
			}

			oCollection.update(where("workflowId").eq(oSnapWorkflow.getWorkflowId()), toDocument(oSnapWorkflow));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SnapWorkflowRepositoryBackend.updateSnapWorkflow", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteSnapWorkflow(String sWorkflowId) {
		if (Utils.isNullOrEmpty(sWorkflowId)) {
			return false;
		}

		try {
			int iCount = 0;
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			for (Document oDoc : oCollection.find(where("workflowId").eq(sWorkflowId))) {
				if (oDoc != null) {
					iCount++;
				}
			}

			oCollection.remove(where("workflowId").eq(sWorkflowId));
			return iCount == 1;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SnapWorkflowRepositoryBackend.deleteSnapWorkflow", oEx);
		}

		return false;
	}

	@Override
	public int deleteSnapWorkflowByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			int iCount = 0;
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return 0;
			}

			for (Document oDoc : oCollection.find(where("userId").eq(sUserId))) {
				if (oDoc != null) {
					iCount++;
				}
			}

			oCollection.remove(where("userId").eq(sUserId));
			return iCount;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SnapWorkflowRepositoryBackend.deleteSnapWorkflowByUser", oEx);
		}

		return 0;
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}

	private boolean matches(Pattern oPattern, String sValue) {
		return sValue != null && oPattern.matcher(sValue).find();
	}

	private int compareNullableString(String sA, String sB) {
		if (sA == null && sB == null) {
			return 0;
		}
		if (sA == null) {
			return 1;
		}
		if (sB == null) {
			return -1;
		}
		return sA.compareToIgnoreCase(sB);
	}
}
