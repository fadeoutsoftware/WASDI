package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.Workspace;
import wasdi.shared.data.interfaces.IWorkspaceRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for workspace repository.
 */
public class No2WorkspaceRepositoryBackend extends No2Repository implements IWorkspaceRepositoryBackend {

	private static final String s_sCollectionName = "workspaces";

	@Override
	public boolean insertWorkspace(Workspace oWorkspace) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oWorkspace == null) {
				return false;
			}

			oCollection.insert(toDocument(oWorkspace));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2WorkspaceRepositoryBackend.insertWorkspace: error", oEx);
		}

		return false;
	}

	@Override
	public boolean updateWorkspaceName(Workspace oWorkspace) {
		return updateWorkspaceField(oWorkspace, "name");
	}

	@Override
	public boolean updateWorkspacePublicFlag(Workspace oWorkspace) {
		return updateWorkspaceField(oWorkspace, "public");
	}

	@Override
	public boolean updateWorkspaceNodeCode(Workspace oWorkspace) {
		return updateWorkspaceField(oWorkspace, "nodeCode");
	}

	@Override
	public boolean updateWorkspace(Workspace oWorkspace) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oWorkspace == null) {
				return false;
			}

			oCollection.update(where("workspaceId").eq(oWorkspace.getWorkspaceId()), toDocument(oWorkspace));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2WorkspaceRepositoryBackend.updateWorkspace: error", oEx);
		}

		return false;
	}

	@Override
	public Workspace getWorkspace(String sWorkspaceId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("workspaceId").eq(sWorkspaceId))) {
				return fromDocument(oDocument, Workspace.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2WorkspaceRepositoryBackend.getWorkspace: error", oEx);
		}

		return null;
	}

	@Override
	public List<Workspace> getWorkspaceByUser(String sUserId) {
		return filterWorkspaces(oWorkspace -> oWorkspace != null && equalsSafe(oWorkspace.getUserId(), sUserId));
	}

	@Override
	public List<Workspace> getWorkspaceByNode(String sNodeCode) {
		return filterWorkspaces(oWorkspace -> oWorkspace != null && equalsSafe(oWorkspace.getNodeCode(), sNodeCode));
	}

	@Override
	public List<Workspace> getWorkspacesSortedByOldestUpdate(String sUserId) {
		List<Workspace> aoResults = getWorkspaceByUser(sUserId);
		aoResults.sort(Comparator.comparing(Workspace::getLastEditDate, Comparator.nullsLast(Double::compareTo)));
		return aoResults;
	}

	@Override
	public Workspace getByUserIdAndWorkspaceName(String sUserId, String sName) {
		for (Workspace oWorkspace : filterWorkspaces(
				oItem -> oItem != null && equalsSafe(oItem.getUserId(), sUserId) && equalsSafe(oItem.getName(), sName))) {
			return oWorkspace;
		}

		return null;
	}

	@Override
	public Workspace getByNameAndNode(String sName, String sNode) {
		for (Workspace oWorkspace : filterWorkspaces(
				oItem -> oItem != null && equalsSafe(oItem.getName(), sName) && equalsSafe(oItem.getNodeCode(), sNode))) {
			return oWorkspace;
		}

		return null;
	}

	@Override
	public boolean deleteWorkspace(String sWorkspaceId) {
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.remove(where("workspaceId").eq(sWorkspaceId));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2WorkspaceRepositoryBackend.deleteWorkspace: error", oEx);
		}

		return false;
	}

	@Override
	public int deleteByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		int iDeleted = getWorkspaceByUser(sUserId).size();

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection != null) {
				oCollection.remove(where("userId").eq(sUserId));
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2WorkspaceRepositoryBackend.deleteByUser: error", oEx);
		}

		return iDeleted;
	}

	@Override
	public boolean isOwnedByUser(String sUserId, String sWorkspaceId) {
		Workspace oWorkspace = getWorkspace(sWorkspaceId);
		return oWorkspace != null && equalsSafe(oWorkspace.getUserId(), sUserId);
	}

	@Override
	public List<Workspace> getWorkspacesList() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find() : null;
			return toList(oCursor, Workspace.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2WorkspaceRepositoryBackend.getWorkspacesList: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<Workspace> findWorkspacesByPartialName(String sPartialName) {
		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return new ArrayList<>();
		}

		String sLookup = sPartialName.toLowerCase();
		List<Workspace> aoResults = filterWorkspaces(
				oWorkspace -> oWorkspace != null
					&& (containsIgnoreCase(oWorkspace.getWorkspaceId(), sLookup)
							|| containsIgnoreCase(oWorkspace.getName(), sLookup)));

		aoResults.sort(Comparator.comparing(Workspace::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
		return aoResults;
	}

	@Override
	public Long getStorageUsageForUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return -1L;
		}

		long lStorageUsage = 0L;

		try {
			for (Workspace oWorkspace : getWorkspaceByUser(sUserId)) {
				if (oWorkspace != null && oWorkspace.getStorageSize() != null) {
					lStorageUsage += oWorkspace.getStorageSize();
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2WorkspaceRepositoryBackend.getStorageUsageForUser: error", oEx);
			return -1L;
		}

		return lStorageUsage;
	}

	private boolean updateWorkspaceField(Workspace oWorkspace, String sFieldName) {
		if (oWorkspace == null || Utils.isNullOrEmpty(oWorkspace.getWorkspaceId())) {
			return false;
		}

		Workspace oStored = getWorkspace(oWorkspace.getWorkspaceId());
		if (oStored == null) {
			return false;
		}

		switch (sFieldName) {
			case "name":
				oStored.setName(oWorkspace.getName());
				break;
			case "public":
				oStored.setPublic(oWorkspace.isPublic());
				break;
			case "nodeCode":
				oStored.setNodeCode(oWorkspace.getNodeCode());
				break;
			default:
				return false;
		}

		return updateWorkspace(oStored);
	}

	private interface WorkspaceFilter {
		boolean match(Workspace oWorkspace);
	}

	private List<Workspace> filterWorkspaces(WorkspaceFilter oFilter) {
		List<Workspace> aoResults = new ArrayList<>();

		for (Workspace oWorkspace : getWorkspacesList()) {
			if (oFilter.match(oWorkspace)) {
				aoResults.add(oWorkspace);
			}
		}

		return aoResults;
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}

	private boolean containsIgnoreCase(String sValue, String sLookupLower) {
		if (sValue == null || sLookupLower == null) {
			return false;
		}

		return sValue.toLowerCase().contains(sLookupLower);
	}
}
