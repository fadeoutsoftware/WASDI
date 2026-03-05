package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.aggregators.ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult;
import wasdi.shared.business.aggregators.ProcessWorkspaceAggregatorBySubscriptionAndProject;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.interfaces.IProcessWorkspaceRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for process workspace repository.
 */
public class No2ProcessWorkspaceRepositoryBackend extends No2Repository implements IProcessWorkspaceRepositoryBackend {

	private static final String s_sCollectionName = "processworkpsace";
	private String m_sRepoDb = "local";

	@Override
	public void setRepoDb(String sRepoDb) {
		m_sRepoDb = sRepoDb;
	}

	@Override
	public String insertProcessWorkspace(ProcessWorkspace oProcessWorkspace) {
		if (oProcessWorkspace == null) {
			return "";
		}

		try {
			oProcessWorkspace.setLastStateChangeTimestamp(Utils.nowInMillis());
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return "";
			}

			oCollection.insert(toDocument(oProcessWorkspace));
			return oProcessWorkspace.getProcessObjId();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessWorkspaceRepositoryBackend.insertProcessWorkspace: exception ", oEx);
		}

		return "";
	}

	@Override
	public void insertProcessListWorkspace(List<ProcessWorkspace> aoProcessWorkspace) {
		if (aoProcessWorkspace == null) {
			return;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return;
			}

			for (ProcessWorkspace oProcessWorkspace : aoProcessWorkspace) {
				if (oProcessWorkspace == null) {
					continue;
				}
				oProcessWorkspace.setLastStateChangeTimestamp(Utils.nowInMillis());
				oCollection.insert(toDocument(oProcessWorkspace));
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessWorkspaceRepositoryBackend.insertProcessListWorkspace: exception ", oEx);
		}
	}

	@Override
	public boolean deleteProcessWorkspace(String sId) {
		if (Utils.isNullOrEmpty(sId)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.remove(where("_id").eq(sId));
			oCollection.remove(where("processObjId").eq(sId));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessWorkspaceRepositoryBackend.deleteProcessWorkspace: exception ", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteProcessWorkspaceByPid(int iPid) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}
			oCollection.remove(where("pid").eq(iPid));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessWorkspaceRepositoryBackend.deleteProcessWorkspaceByPid: exception ", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteProcessWorkspaceByProcessObjId(String sProcessObjId) {
		if (Utils.isNullOrEmpty(sProcessObjId)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}
			oCollection.remove(where("processObjId").eq(sProcessObjId));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessWorkspaceRepositoryBackend.deleteProcessWorkspaceByProcessObjId: exception ", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteProcessWorkspaceByWorkspaceId(String sWorkspaceId) {
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
			WasdiLog.errorLog("No2ProcessWorkspaceRepositoryBackend.deleteProcessWorkspaceByWorkspaceId: exception ", oEx);
		}

		return false;
	}

	@Override
	public List<ProcessWorkspace> getRoots(List<ProcessWorkspace> aoChildren) {
		if (aoChildren == null || aoChildren.isEmpty()) {
			return new ArrayList<>(0);
		}

		try {
			List<ProcessWorkspace> aoFathers = getFathers(aoChildren);
			List<ProcessWorkspace> aoRoots = new LinkedList<>();
			for (ProcessWorkspace oFather : aoFathers) {
				if (oFather != null && oFather.getParentId() == null) {
					aoRoots.add(oFather);
				}
			}
			// Keep Mongo behavior compatibility: method historically returned fathers list.
			return aoFathers;
		}
		catch (Exception oE) {
			WasdiLog.errorLog("No2ProcessWorkspaceRepositoryBackend.getRoots: ", oE);
		}

		return new ArrayList<>(0);
	}

	@Override
	public List<ProcessWorkspace> getFathers(List<ProcessWorkspace> aoChildProcesses) {
		if (aoChildProcesses == null || aoChildProcesses.isEmpty()) {
			return new LinkedList<>();
		}

		try {
			Map<String, ProcessWorkspace> aoByProcessObjId = new HashMap<>();
			for (ProcessWorkspace oProcessWorkspace : getList()) {
				if (oProcessWorkspace != null && !Utils.isNullOrEmpty(oProcessWorkspace.getProcessObjId())) {
					aoByProcessObjId.put(oProcessWorkspace.getProcessObjId(), oProcessWorkspace);
				}
			}

			List<ProcessWorkspace> aoFathers = new LinkedList<>();
			for (ProcessWorkspace oChild : aoChildProcesses) {
				String sParentId = oChild != null ? oChild.getParentId() : null;
				while (!Utils.isNullOrEmpty(sParentId)) {
					ProcessWorkspace oFather = aoByProcessObjId.get(sParentId);
					if (oFather == null) {
						break;
					}
					aoFathers.add(oFather);
					sParentId = oFather.getParentId();
				}
			}

			return aoFathers;
		}
		catch (Exception oE) {
			WasdiLog.errorLog("No2ProcessWorkspaceRepositoryBackend.getFathers: ", oE);
		}

		return new LinkedList<>();
	}

	@Override
	public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, List<ProcessStatus> aeDesiredStatuses) {
		if (Utils.isNullOrEmpty(sWorkspaceId) || aeDesiredStatuses == null || aeDesiredStatuses.isEmpty()) {
			return new ArrayList<>();
		}

		List<String> asDesiredStatuses = aeDesiredStatuses.stream()
				.filter(oStatus -> oStatus != null && !Utils.isNullOrEmpty(oStatus.name()))
				.map(ProcessStatus::name)
				.collect(Collectors.toList());

		return getList().stream()
				.filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getWorkspaceId(), sWorkspaceId)
						&& asDesiredStatuses.contains(oProcessWorkspace.getStatus()))
				.sorted(getOperationSortDesc())
				.collect(Collectors.toList());
	}

	@Override
	public List<String> getProcessObjIdsFromWorkspaceId(String sWorkspaceId) {
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			return new ArrayList<>(0);
		}

		return getList().stream()
				.filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getWorkspaceId(), sWorkspaceId))
				.sorted(getOperationSortDesc())
				.map(ProcessWorkspace::getProcessObjId)
				.filter(sProcessObjId -> !Utils.isNullOrEmpty(sProcessObjId))
				.collect(Collectors.toList());
	}

	@Override
	public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId) {
		return getProcessByWorkspace(sWorkspaceId, null, null, null, null, null);
	}

	@Override
	public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, ProcessStatus eStatus,
			LauncherOperations eOperation, String sProductNameSubstring, java.time.Instant oDateFrom,
			java.time.Instant oDateTo) {
		List<ProcessWorkspace> aoReturnList = new ArrayList<>();
		try {
			Predicate<ProcessWorkspace> oFilter = buildFilter(sWorkspaceId, eStatus, eOperation, sProductNameSubstring, oDateFrom,
					oDateTo);
			return getList().stream().filter(oFilter).sorted(getOperationSortDesc()).collect(Collectors.toList());
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessWorkspaceRepositoryBackend.getProcessByWorkspace: exception ", oEx);
		}
		return aoReturnList;
	}

	@Override
	public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, int iStartIndex, int iEndIndex) {
		return getProcessByWorkspace(sWorkspaceId, null, null, null, null, null, iStartIndex, iEndIndex);
	}

	@Override
	public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, ProcessStatus eStatus,
			LauncherOperations eOperation, String sProductNameSubstring, java.time.Instant oDateFrom,
			java.time.Instant oDateTo, int iStartIndex, int iEndIndex) {
		List<ProcessWorkspace> aoAll = getProcessByWorkspace(sWorkspaceId, eStatus, eOperation, sProductNameSubstring, oDateFrom, oDateTo);
		if (aoAll.isEmpty()) {
			return aoAll;
		}

		int iFrom = Math.max(0, iStartIndex);
		int iTo = Math.max(iFrom, Math.min(iEndIndex, aoAll.size()));
		if (iFrom >= aoAll.size()) {
			return new ArrayList<>();
		}
		return new ArrayList<>(aoAll.subList(iFrom, iTo));
	}

	@Override
	public String getProcessStatusFromId(String sProcessId) {
		ProcessWorkspace oProcessWorkspace = getProcessByProcessObjId(sProcessId);
		return oProcessWorkspace != null ? oProcessWorkspace.getStatus() : null;
	}

	@Override
	public long countByWorkspace(String sWorkspaceId) {
		return getList().stream().filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getWorkspaceId(), sWorkspaceId)).count();
	}

	@Override
	public long countByProcessor(String sProcessorName) {
		return getList().stream().filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getProductName(), sProcessorName)).count();
	}

	@Override
	public List<ProcessWorkspace> getProcessByUser(String sUserId) {
		return getList().stream().filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getUserId(), sUserId))
				.sorted(getOperationSortDesc()).collect(Collectors.toList());
	}

	@Override
	public List<ProcessWorkspace> getOGCProcessByUser(String sUserId) {
		return getProcessByUser(sUserId);
	}

	@Override
	public List<ProcessWorkspace> getCreatedProcesses() {
		return getProcessesByStatusAndOps(ProcessStatus.CREATED.name(), null, LauncherOperations.DOWNLOAD.name(), LauncherOperations.RUNIDL.name());
	}

	@Override
	public List<ProcessWorkspace> getCreatedProcessesByNode(String sComputingNodeCode) {
		return getProcessesByStatusAndOps(ProcessStatus.CREATED.name(), sComputingNodeCode, LauncherOperations.DOWNLOAD.name(), LauncherOperations.RUNIDL.name());
	}

	@Override
	public List<ProcessWorkspace> getReadyProcessesByNode(String sComputingNodeCode) {
		return getProcessesByStatusAndOpsSortedByLastChange(ProcessStatus.READY.name(), sComputingNodeCode,
				LauncherOperations.DOWNLOAD.name(), LauncherOperations.RUNIDL.name());
	}

	@Override
	public List<ProcessWorkspace> getRunningProcesses() {
		return getProcessesByStatusAndOps(ProcessStatus.RUNNING.name(), null, LauncherOperations.DOWNLOAD.name(), LauncherOperations.RUNIDL.name());
	}

	@Override
	public List<ProcessWorkspace> getRunningProcessesByNode(String sComputingNodeCode) {
		return getProcessesByStatusAndOps(ProcessStatus.RUNNING.name(), sComputingNodeCode,
				LauncherOperations.DOWNLOAD.name(), LauncherOperations.RUNIDL.name());
	}

	@Override
	public List<ProcessWorkspace> getCreatedDownloads() {
		return getProcessesByStatusAndOperation(ProcessStatus.CREATED.name(), LauncherOperations.DOWNLOAD.name(), null, false);
	}

	@Override
	public List<ProcessWorkspace> getCreatedDownloadsByNode(String sComputingNodeCode) {
		return getProcessesByStatusAndOperation(ProcessStatus.CREATED.name(), LauncherOperations.DOWNLOAD.name(), sComputingNodeCode, false);
	}

	@Override
	public List<ProcessWorkspace> getCreatedIDL() {
		return getProcessesByStatusAndOperation(ProcessStatus.CREATED.name(), LauncherOperations.RUNIDL.name(), null, false);
	}

	@Override
	public List<ProcessWorkspace> getCreatedIDLByNode(String sComputingNode) {
		return getProcessesByStatusAndOperation(ProcessStatus.CREATED.name(), LauncherOperations.RUNIDL.name(), sComputingNode, false);
	}

	@Override
	public List<ProcessWorkspace> getRunningDownloads() {
		return getProcessesByStatusAndOperation(ProcessStatus.RUNNING.name(), LauncherOperations.DOWNLOAD.name(), null, false);
	}

	@Override
	public List<ProcessWorkspace> getRunningDownloadsByNode(String sComputingNode) {
		return getProcessesByStatusAndOperation(ProcessStatus.RUNNING.name(), LauncherOperations.DOWNLOAD.name(), sComputingNode, false);
	}

	@Override
	public List<ProcessWorkspace> getReadyDownloads() {
		return getProcessesByStatusAndOperation(ProcessStatus.READY.name(), LauncherOperations.DOWNLOAD.name(), null, true);
	}

	@Override
	public List<ProcessWorkspace> getReadyDownloadsByNode(String sComputingNode) {
		return getProcessesByStatusAndOperation(ProcessStatus.READY.name(), LauncherOperations.DOWNLOAD.name(), sComputingNode, true);
	}

	@Override
	public List<ProcessWorkspace> getRunningIDL() {
		return getProcessesByStatusAndOperation(ProcessStatus.RUNNING.name(), LauncherOperations.RUNIDL.name(), null, false);
	}

	@Override
	public List<ProcessWorkspace> getRunningIDLByNode(String sComputingNode) {
		return getProcessesByStatusAndOperation(ProcessStatus.RUNNING.name(), LauncherOperations.RUNIDL.name(), sComputingNode, false);
	}

	@Override
	public List<ProcessWorkspace> getReadyIDL() {
		return getProcessesByStatusAndOperation(ProcessStatus.READY.name(), LauncherOperations.RUNIDL.name(), null, true);
	}

	@Override
	public List<ProcessWorkspace> getReadyIDLByNode(String sComputingNode) {
		return getProcessesByStatusAndOperation(ProcessStatus.READY.name(), LauncherOperations.RUNIDL.name(), sComputingNode, true);
	}

	@Override
	public List<ProcessWorkspace> getProcessesByStateNode(String sProcessStatus, String sComputingNodeCode) {
		return getProcessesByStateNode(sProcessStatus, sComputingNodeCode, "operationTimestamp", "operationDate");
	}

	@Override
	public List<ProcessWorkspace> getProcessesByStateNode(String sProcessStatus, String sComputingNodeCode,
			String sOrderByTimestamp, String sOrderByDate) {
		List<ProcessWorkspace> aoReturnList = getList().stream()
				.filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getStatus(), sProcessStatus)
						&& equalsSafe(oProcessWorkspace.getNodeCode(), sComputingNodeCode))
				.collect(Collectors.toList());

		Comparator<ProcessWorkspace> oComparator = comparatorForFieldsDesc(sOrderByTimestamp, sOrderByDate);
		aoReturnList.sort(oComparator);
		return aoReturnList;
	}

	@Override
	public List<ProcessWorkspace> getProcessesForSchedulerNode(String sComputingNodeCode, String sOrderBy) {
		List<ProcessWorkspace> aoReturnList = getList().stream()
				.filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getNodeCode(), sComputingNodeCode)
						&& !equalsSafe(oProcessWorkspace.getStatus(), ProcessStatus.DONE.name())
						&& !equalsSafe(oProcessWorkspace.getStatus(), ProcessStatus.ERROR.name())
						&& !equalsSafe(oProcessWorkspace.getStatus(), ProcessStatus.STOPPED.name()))
				.sorted(comparatorForFieldsDesc(sOrderBy, null))
				.collect(Collectors.toList());

		if (WasdiConfig.Current != null && WasdiConfig.Current.scheduler != null
				&& WasdiConfig.Current.scheduler.lastStateChangeDateOrderBy == 1) {
			Collections.reverse(aoReturnList);
		}

		return aoReturnList;
	}

	@Override
	public List<ProcessWorkspace> getLastProcessByWorkspace(String sWorkspaceId) {
		List<ProcessWorkspace> aoReturnList = getList().stream()
				.filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getWorkspaceId(), sWorkspaceId))
				.sorted(getOperationSortDesc())
				.limit(5)
				.collect(Collectors.toList());
		Collections.reverse(aoReturnList);
		return aoReturnList;
	}

	@Override
	public List<ProcessWorkspace> getLastProcessByUser(String sUserId) {
		List<ProcessWorkspace> aoReturnList = getList().stream()
				.filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getUserId(), sUserId))
				.sorted(getOperationSortDesc())
				.limit(5)
				.collect(Collectors.toList());
		Collections.reverse(aoReturnList);
		return aoReturnList;
	}

	@Override
	public List<ProcessWorkspace> getProcessOlderThan(Long dTimestamp) {
		if (dTimestamp == null) {
			return new ArrayList<>();
		}

		return getList().stream()
				.filter(oProcessWorkspace -> getDouble(oProcessWorkspace.getOperationTimestamp()) > dTimestamp.doubleValue())
				.sorted(Comparator.comparing(ProcessWorkspace::getOperationTimestamp, Comparator.nullsLast(Double::compareTo)))
				.collect(Collectors.toList());
	}

	@Override
	public ArrayList<ProcessWorkspace> getProcessByProductName(String sProductName) {
		return new ArrayList<>(getList().stream()
				.filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getProductName(), sProductName))
				.sorted(getOperationSortDesc())
				.collect(Collectors.toList()));
	}

	@Override
	public List<ProcessWorkspace> getProcessByProductNameAndWorkspace(String sProductName, String sWorkspace) {
		return getList().stream()
				.filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getProductName(), sProductName)
						&& equalsSafe(oProcessWorkspace.getWorkspaceId(), sWorkspace))
				.sorted(getOperationSortDesc())
				.collect(Collectors.toList());
	}

	@Override
	public List<ProcessWorkspace> getProcessByProductNameAndUserId(String sProductName, String sUserId) {
		return getList().stream()
				.filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getProductName(), sProductName)
						&& equalsSafe(oProcessWorkspace.getUserId(), sUserId))
				.sorted(getOperationSortDesc())
				.collect(Collectors.toList());
	}

	@Override
	public ProcessWorkspace getProcessByProcessObjId(String sProcessObjId) {
		if (Utils.isNullOrEmpty(sProcessObjId)) {
			return null;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}
			for (Document oDocument : oCollection.find(where("processObjId").eq(sProcessObjId))) {
				return fromDocument(oDocument, ProcessWorkspace.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessWorkspaceRepositoryBackend.getProcessByProcessObjId: exception ", oEx);
		}

		return null;
	}

	@Override
	public ArrayList<String> getProcessesStatusByProcessObjId(ArrayList<String> asProcessesObjId) {
		ArrayList<String> asReturnStatus = new ArrayList<>();
		if (asProcessesObjId == null) {
			return asReturnStatus;
		}

		Map<String, String> aoStatusById = getList().stream()
				.filter(oProcessWorkspace -> oProcessWorkspace != null && !Utils.isNullOrEmpty(oProcessWorkspace.getProcessObjId()))
				.collect(Collectors.toMap(ProcessWorkspace::getProcessObjId, ProcessWorkspace::getStatus, (a, b) -> a));

		for (String sProcessObjId : asProcessesObjId) {
			String sStatus = aoStatusById.get(sProcessObjId);
			asReturnStatus.add(sStatus != null ? sStatus : ProcessStatus.ERROR.name());
		}

		return asReturnStatus;
	}

	@Override
	public boolean updateProcess(ProcessWorkspace oProcessWorkspace) {
		if (oProcessWorkspace == null || Utils.isNullOrEmpty(oProcessWorkspace.getProcessObjId())) {
			return false;
		}

		try {
			ProcessWorkspace oOriginal = getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());
			if (oOriginal != null && !equalsSafe(oOriginal.getStatus(), oProcessWorkspace.getStatus())) {
				double dNowInMillis = Utils.nowInMillis();
				oProcessWorkspace.setLastStateChangeTimestamp(dNowInMillis);

				if (equalsSafeIgnoreCase(oOriginal.getStatus(), ProcessStatus.RUNNING.name())) {
					double dLastChange = getDouble(oOriginal.getLastStateChangeTimestamp());
					long lDelta = (long) dNowInMillis - (long) dLastChange;
					long lOriginalRunning = oOriginal.getRunningTime() != null ? oOriginal.getRunningTime() : 0L;
					oProcessWorkspace.setRunningTime(lOriginalRunning + lDelta);
				}
			}

			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}
			oCollection.update(where("processObjId").eq(oProcessWorkspace.getProcessObjId()), toDocument(oProcessWorkspace));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessWorkspaceRepositoryBackend.updateProcess: exception ", oEx);
		}

		return false;
	}

	@Override
	public boolean cleanQueue() {
		try {
			for (ProcessWorkspace oProcessWorkspace : getCreatedSummary()) {
				oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
				updateProcess(oProcessWorkspace);
			}
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessWorkspaceRepositoryBackend.cleanQueue: exception ", oEx);
		}
		return false;
	}

	@Override
	public boolean existsPidProcessWorkspace(Integer iPid) {
		if (iPid == null) {
			return false;
		}
		return getList().stream().anyMatch(oProcessWorkspace -> oProcessWorkspace != null && oProcessWorkspace.getPid() == iPid.intValue());
	}

	@Override
	public long getRunningCountByUser(String sUserId, boolean bIncluded, String sWorkspaceId) {
		return getList().stream()
				.filter(oProcessWorkspace -> isRunningSummaryStatus(oProcessWorkspace.getStatus()))
				.filter(oProcessWorkspace -> bIncluded ? equalsSafe(oProcessWorkspace.getUserId(), sUserId)
						: !equalsSafe(oProcessWorkspace.getUserId(), sUserId))
				.filter(oProcessWorkspace -> Utils.isNullOrEmpty(sWorkspaceId)
						|| equalsSafe(oProcessWorkspace.getWorkspaceId(), sWorkspaceId))
				.count();
	}

	@Override
	public List<ProcessWorkspace> getRunningSummary() {
		return getList().stream()
				.filter(oProcessWorkspace -> isRunningSummaryStatus(oProcessWorkspace.getStatus()))
				.sorted(getOperationSortDesc())
				.collect(Collectors.toList());
	}

	@Override
	public long getCreatedCountByUser(String sUserId, boolean bIncluded, String sWorkspaceId) {
		return getList().stream()
				.filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getStatus(), ProcessStatus.CREATED.name()))
				.filter(oProcessWorkspace -> bIncluded ? equalsSafe(oProcessWorkspace.getUserId(), sUserId)
						: !equalsSafe(oProcessWorkspace.getUserId(), sUserId))
				.filter(oProcessWorkspace -> Utils.isNullOrEmpty(sWorkspaceId)
						|| equalsSafe(oProcessWorkspace.getWorkspaceId(), sWorkspaceId))
				.count();
	}

	@Override
	public List<ProcessWorkspace> getCreatedSummary() {
		return getList().stream()
				.filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getStatus(), ProcessStatus.CREATED.name()))
				.sorted(getOperationSortDesc())
				.collect(Collectors.toList());
	}

	@Override
	public List<ProcessWorkspace> getList() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find() : null;
			return toList(oCursor, ProcessWorkspace.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessWorkspaceRepositoryBackend.getList: exception ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public boolean isProcessOwnedByUser(String sUserId, String sProcessObjId) {
		return getList().stream()
				.anyMatch(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getUserId(), sUserId)
						&& equalsSafe(oProcessWorkspace.getProcessObjId(), sProcessObjId));
	}

	@Override
	public String getWorkspaceByProcessObjId(String sProcessObjId) {
		ProcessWorkspace oProcessWorkspace = getProcessByProcessObjId(sProcessObjId);
		return oProcessWorkspace != null ? oProcessWorkspace.getWorkspaceId() : null;
	}

	@Override
	public String getPayload(String sProcessObjId) {
		ProcessWorkspace oProcessWorkspace = getProcessByProcessObjId(sProcessObjId);
		return oProcessWorkspace != null ? oProcessWorkspace.getPayload() : null;
	}

	@Override
	public List<ProcessWorkspace> getByNode(String sComputingNodeCode) {
		return getList().stream().filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getNodeCode(), sComputingNodeCode))
				.sorted(getOperationSortDesc()).collect(Collectors.toList());
	}

	@Override
	public List<ProcessWorkspace> getByNodeUnsorted(String sComputingNodeCode) {
		return getList().stream().filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getNodeCode(), sComputingNodeCode))
				.collect(Collectors.toList());
	}

	@Override
	public List<ProcessWorkspace> getUnfinishedByNode(String sComputingNodeCode) {
		List<String> asStatuses = Arrays.asList(ProcessStatus.WAITING.name(), ProcessStatus.READY.name(),
				ProcessStatus.CREATED.name(), ProcessStatus.RUNNING.name());

		Comparator<ProcessWorkspace> oComparator = Comparator
				.comparing(ProcessWorkspace::getStatus, Comparator.nullsLast(String::compareToIgnoreCase))
				.thenComparing(ProcessWorkspace::getOperationType, Comparator.nullsLast(String::compareToIgnoreCase))
				.thenComparing(ProcessWorkspace::getOperationSubType, Comparator.nullsLast(String::compareToIgnoreCase));

		return getList().stream()
				.filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getNodeCode(), sComputingNodeCode)
						&& asStatuses.contains(oProcessWorkspace.getStatus()))
				.sorted(oComparator)
				.collect(Collectors.toList());
	}

	@Override
	public List<ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult> getQueuesByNodeAndStatuses(String sNodeCode,
			String... asStatuses) {
		List<ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult> aoReturnList = new ArrayList<>();

		if (asStatuses == null || asStatuses.length == 0) {
			return aoReturnList;
		}

		try {
			List<String> asStatusList = Arrays.asList(asStatuses);
			Map<String, Integer> aoCounts = new HashMap<>();

			for (ProcessWorkspace oProcessWorkspace : getList()) {
				if (!equalsSafe(oProcessWorkspace.getNodeCode(), sNodeCode)
						|| !asStatusList.contains(oProcessWorkspace.getStatus())) {
					continue;
				}

				String sKey = safe(oProcessWorkspace.getOperationType()) + "|" + safe(oProcessWorkspace.getOperationSubType())
						+ "|" + safe(oProcessWorkspace.getStatus());
				aoCounts.put(sKey, aoCounts.getOrDefault(sKey, 0) + 1);
			}

			List<String> asSortedKeys = new ArrayList<>(aoCounts.keySet());
			Collections.sort(asSortedKeys);

			for (String sKey : asSortedKeys) {
				String[] asParts = sKey.split("\\|", -1);
				Map<String, Object> oIdMap = new HashMap<>();
				oIdMap.put("operationType", nullIfEmpty(asParts[0]));
				oIdMap.put("operationSubType", nullIfEmpty(asParts[1]));
				oIdMap.put("status", nullIfEmpty(asParts[2]));

				Map<String, Object> oDocMap = new HashMap<>();
				oDocMap.put("_id", oIdMap);
				oDocMap.put("count", aoCounts.get(sKey));

				aoReturnList.add(s_oMapper.convertValue(oDocMap,
						ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult.class));
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessWorkspaceRepositoryBackend.getQueuesByNodeAndStatuses: exception ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<ProcessWorkspace> getProcessByParentId(String sParentId) {
		return getList().stream().filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getParentId(), sParentId))
				.sorted(getOperationSortDesc()).collect(Collectors.toList());
	}

	@Override
	public Long getRunningTime(String sUserId, Long lDateFrom, Long lDateTo) {
		long lTotalRunningTime = 0L;
		if (Utils.isNullOrEmpty(sUserId) || lDateFrom == null || lDateTo == null) {
			return lTotalRunningTime;
		}

		String sUserIdLower = sUserId.toLowerCase();
		for (ProcessWorkspace oProcessWorkspace : getList()) {
			if (oProcessWorkspace == null) {
				continue;
			}
			if (!equalsSafe(lower(oProcessWorkspace.getUserId()), sUserIdLower)) {
				continue;
			}
			double dTs = getDouble(oProcessWorkspace.getOperationTimestamp());
			if (dTs < lDateFrom.doubleValue() || dTs > lDateTo.doubleValue()) {
				continue;
			}
			lTotalRunningTime += oProcessWorkspace.getRunningTime() != null ? oProcessWorkspace.getRunningTime() : 0L;
		}

		return lTotalRunningTime;
	}

	@Override
	public List<ProcessWorkspaceAggregatorBySubscriptionAndProject> getRunningTimeInfo(Collection<String> asSubscriptionIds) {
		return aggregateRunningTimeBySubscriptionAndProject(null, asSubscriptionIds);
	}

	@Override
	public List<ProcessWorkspaceAggregatorBySubscriptionAndProject> getProjectRunningTime(String sUserId,
			Collection<String> asSubscriptionIds) {
		return aggregateRunningTimeBySubscriptionAndProject(sUserId, asSubscriptionIds);
	}

	private List<ProcessWorkspaceAggregatorBySubscriptionAndProject> aggregateRunningTimeBySubscriptionAndProject(
			String sUserId, Collection<String> asSubscriptionIds) {
		List<ProcessWorkspaceAggregatorBySubscriptionAndProject> aoResultList = new ArrayList<>();
		if (asSubscriptionIds == null || asSubscriptionIds.isEmpty()) {
			return aoResultList;
		}

		Map<String, Long> aoGroupedTotals = new HashMap<>();
		for (ProcessWorkspace oProcessWorkspace : getList()) {
			if (oProcessWorkspace == null) {
				continue;
			}

			if (!asSubscriptionIds.contains(oProcessWorkspace.getSubscriptionId())) {
				continue;
			}
			if (oProcessWorkspace.getProjectId() == null) {
				continue;
			}
			if (!Utils.isNullOrEmpty(sUserId) && !equalsSafe(oProcessWorkspace.getUserId(), sUserId)) {
				continue;
			}

			String sKey = safe(oProcessWorkspace.getSubscriptionId()) + "|" + safe(oProcessWorkspace.getProjectId());
			long lRunningTime = oProcessWorkspace.getRunningTime() != null ? oProcessWorkspace.getRunningTime() : 0L;
			aoGroupedTotals.put(sKey, aoGroupedTotals.getOrDefault(sKey, 0L) + lRunningTime);
		}

		for (Map.Entry<String, Long> oEntry : aoGroupedTotals.entrySet()) {
			String[] asParts = oEntry.getKey().split("\\|", -1);
			Map<String, Object> oIdMap = new HashMap<>();
			oIdMap.put("subscriptionId", nullIfEmpty(asParts[0]));
			oIdMap.put("projectId", nullIfEmpty(asParts[1]));

			Map<String, Object> oDocMap = new HashMap<>();
			oDocMap.put("_id", oIdMap);
			oDocMap.put("total", oEntry.getValue());

			aoResultList.add(s_oMapper.convertValue(oDocMap, ProcessWorkspaceAggregatorBySubscriptionAndProject.class));
		}

		return aoResultList;
	}

	private Predicate<ProcessWorkspace> buildFilter(String sWorkspaceId, ProcessStatus eStatus,
			LauncherOperations eOperation, String sProductNameSubstring, java.time.Instant oDateFrom,
			java.time.Instant oDateTo) {
		return oProcessWorkspace -> {
			if (!equalsSafe(oProcessWorkspace.getWorkspaceId(), sWorkspaceId)) {
				return false;
			}

			if (eStatus != null && !equalsSafe(oProcessWorkspace.getStatus(), eStatus.name())) {
				return false;
			}

			if (eOperation != null && !equalsSafe(oProcessWorkspace.getOperationType(), eOperation.name())) {
				return false;
			}

			if (!Utils.isNullOrEmpty(sProductNameSubstring)
					&& (oProcessWorkspace.getProductName() == null
							|| !oProcessWorkspace.getProductName().contains(sProductNameSubstring))) {
				return false;
			}

			double dOperationTimestamp = getDouble(oProcessWorkspace.getOperationTimestamp());
			if (oDateFrom != null && dOperationTimestamp < (double) oDateFrom.toEpochMilli()) {
				return false;
			}
			if (oDateTo != null && dOperationTimestamp > (double) oDateTo.toEpochMilli()) {
				return false;
			}

			return true;
		};
	}

	private List<ProcessWorkspace> getProcessesByStatusAndOperation(String sStatus, String sOperationType,
			String sNodeCode, boolean bSortByLastStateChange) {
		Predicate<ProcessWorkspace> oPredicate = oProcessWorkspace -> equalsSafe(oProcessWorkspace.getStatus(), sStatus)
				&& equalsSafe(oProcessWorkspace.getOperationType(), sOperationType)
				&& (sNodeCode == null || equalsSafe(oProcessWorkspace.getNodeCode(), sNodeCode));

		Comparator<ProcessWorkspace> oComparator = bSortByLastStateChange ? getLastStateChangeSortDesc() : getOperationSortDesc();

		return getList().stream().filter(oPredicate).sorted(oComparator).collect(Collectors.toList());
	}

	private List<ProcessWorkspace> getProcessesByStatusAndOps(String sStatus, String sNodeCode,
			String... asExcludedOperations) {
		List<String> asExcluded = Arrays.asList(asExcludedOperations);
		return getList().stream()
				.filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getStatus(), sStatus))
				.filter(oProcessWorkspace -> sNodeCode == null || equalsSafe(oProcessWorkspace.getNodeCode(), sNodeCode))
				.filter(oProcessWorkspace -> !asExcluded.contains(oProcessWorkspace.getOperationType()))
				.sorted(getOperationSortDesc())
				.collect(Collectors.toList());
	}

	private List<ProcessWorkspace> getProcessesByStatusAndOpsSortedByLastChange(String sStatus, String sNodeCode,
			String... asExcludedOperations) {
		List<String> asExcluded = Arrays.asList(asExcludedOperations);
		return getList().stream()
				.filter(oProcessWorkspace -> equalsSafe(oProcessWorkspace.getStatus(), sStatus))
				.filter(oProcessWorkspace -> sNodeCode == null || equalsSafe(oProcessWorkspace.getNodeCode(), sNodeCode))
				.filter(oProcessWorkspace -> !asExcluded.contains(oProcessWorkspace.getOperationType()))
				.sorted(getLastStateChangeSortDesc())
				.collect(Collectors.toList());
	}

	private Comparator<ProcessWorkspace> getOperationSortDesc() {
		return Comparator
				.comparing(ProcessWorkspace::getOperationTimestamp, Comparator.nullsLast(Double::compareTo))
				.thenComparing(ProcessWorkspace::getLastStateChangeTimestamp, Comparator.nullsLast(Double::compareTo))
				.reversed();
	}

	private Comparator<ProcessWorkspace> getLastStateChangeSortDesc() {
		return Comparator
				.comparing(ProcessWorkspace::getLastStateChangeTimestamp, Comparator.nullsLast(Double::compareTo))
				.thenComparing(ProcessWorkspace::getOperationTimestamp, Comparator.nullsLast(Double::compareTo))
				.reversed();
	}

	private Comparator<ProcessWorkspace> comparatorForFieldsDesc(String sField1, String sField2) {
		return (oLeft, oRight) -> {
			Comparable oLeft1 = getComparableField(oLeft, sField1);
			Comparable oRight1 = getComparableField(oRight, sField1);
			int iFirst = compareNullableComparableDesc(oLeft1, oRight1);
			if (iFirst != 0 || sField2 == null) {
				return iFirst;
			}

			Comparable oLeft2 = getComparableField(oLeft, sField2);
			Comparable oRight2 = getComparableField(oRight, sField2);
			return compareNullableComparableDesc(oLeft2, oRight2);
		};
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private int compareNullableComparableDesc(Comparable oLeft, Comparable oRight) {
		if (oLeft == null && oRight == null) {
			return 0;
		}
		if (oLeft == null) {
			return 1;
		}
		if (oRight == null) {
			return -1;
		}
		return oRight.compareTo(oLeft);
	}

	@SuppressWarnings("rawtypes")
	private Comparable getComparableField(ProcessWorkspace oProcessWorkspace, String sFieldName) {
		if (oProcessWorkspace == null || Utils.isNullOrEmpty(sFieldName)) {
			return null;
		}

		switch (sFieldName) {
			case "operationTimestamp":
				return oProcessWorkspace.getOperationTimestamp();
			case "lastStateChangeTimestamp":
				return oProcessWorkspace.getLastStateChangeTimestamp();
			case "status":
				return oProcessWorkspace.getStatus();
			case "operationType":
				return oProcessWorkspace.getOperationType();
			case "operationSubType":
				return oProcessWorkspace.getOperationSubType();
			default:
				return oProcessWorkspace.getOperationTimestamp();
		}
	}

	private boolean isRunningSummaryStatus(String sStatus) {
		return equalsSafe(sStatus, ProcessStatus.RUNNING.name())
				|| equalsSafe(sStatus, ProcessStatus.WAITING.name())
				|| equalsSafe(sStatus, ProcessStatus.READY.name());
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}

	private boolean equalsSafeIgnoreCase(String sA, String sB) {
		return sA == null ? sB == null : sA.equalsIgnoreCase(sB);
	}

	private String lower(String sValue) {
		return sValue != null ? sValue.toLowerCase() : null;
	}

	private String safe(String sValue) {
		return sValue != null ? sValue : "";
	}

	private String nullIfEmpty(String sValue) {
		return Utils.isNullOrEmpty(sValue) ? null : sValue;
	}

	private double getDouble(Double dValue) {
		return dValue != null ? dValue.doubleValue() : 0d;
	}
}
