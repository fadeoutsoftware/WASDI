package wasdi.shared.data.interfaces;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.aggregators.ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult;
import wasdi.shared.business.aggregators.ProcessWorkspaceAggregatorBySubscriptionAndProject;

/**
 * Backend contract for process workspace repository.
 */
public interface IProcessWorkspaceRepositoryBackend {

	String insertProcessWorkspace(ProcessWorkspace oProcessWorkspace);

	void insertProcessListWorkspace(List<ProcessWorkspace> aoProcessWorkspace);

	boolean deleteProcessWorkspace(String sId);

	boolean deleteProcessWorkspaceByPid(int iPid);

	boolean deleteProcessWorkspaceByProcessObjId(String sProcessObjId);

	boolean deleteProcessWorkspaceByWorkspaceId(String sWorkspaceId);

	List<ProcessWorkspace> getRoots(List<ProcessWorkspace> aoChildren);

	List<ProcessWorkspace> getFathers(List<ProcessWorkspace> aoChildProcesses);

	List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, List<ProcessStatus> aeDesiredStatuses);

	List<String> getProcessObjIdsFromWorkspaceId(String sWorkspaceId);

	List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId);

	List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, ProcessStatus eStatus, LauncherOperations eOperation, String sProductNameSubstring, Instant oDateFrom, Instant oDateTo);

	List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, int iStartIndex, int iEndIndex);

	List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, ProcessStatus eStatus, LauncherOperations eOperation, String sProductNameSubstring, Instant oDateFrom, Instant oDateTo, int iStartIndex, int iEndIndex);

	String getProcessStatusFromId(String sProcessId);

	long countByWorkspace(String sWorkspaceId);

	long countByProcessor(String sProcessorName);

	List<ProcessWorkspace> getProcessByUser(String sUserId);

	List<ProcessWorkspace> getOGCProcessByUser(String sUserId);

	List<ProcessWorkspace> getCreatedProcesses();

	List<ProcessWorkspace> getCreatedProcessesByNode(String sComputingNodeCode);

	List<ProcessWorkspace> getReadyProcessesByNode(String sComputingNodeCode);

	List<ProcessWorkspace> getRunningProcesses();

	List<ProcessWorkspace> getRunningProcessesByNode(String sComputingNodeCode);

	List<ProcessWorkspace> getCreatedDownloads();

	List<ProcessWorkspace> getCreatedDownloadsByNode(String sComputingNodeCode);

	List<ProcessWorkspace> getCreatedIDL();

	List<ProcessWorkspace> getCreatedIDLByNode(String sComputingNode);

	List<ProcessWorkspace> getRunningDownloads();

	List<ProcessWorkspace> getRunningDownloadsByNode(String sComputingNode);

	List<ProcessWorkspace> getReadyDownloads();

	List<ProcessWorkspace> getReadyDownloadsByNode(String sComputingNode);

	List<ProcessWorkspace> getRunningIDL();

	List<ProcessWorkspace> getRunningIDLByNode(String sComputingNode);

	List<ProcessWorkspace> getReadyIDL();

	List<ProcessWorkspace> getReadyIDLByNode(String sComputingNode);

	List<ProcessWorkspace> getProcessesByStateNode(String sProcessStatus, String sComputingNodeCode);

	List<ProcessWorkspace> getProcessesByStateNode(String sProcessStatus, String sComputingNodeCode, String sOrderByTimestamp, String sOrderByDate);

	List<ProcessWorkspace> getProcessesForSchedulerNode(String sComputingNodeCode, String sOrderBy);

	List<ProcessWorkspace> getLastProcessByWorkspace(String sWorkspaceId);

	List<ProcessWorkspace> getLastProcessByUser(String sUserId);

	List<ProcessWorkspace> getProcessOlderThan(Long dTimestamp);

	ArrayList<ProcessWorkspace> getProcessByProductName(String sProductName);

	List<ProcessWorkspace> getProcessByProductNameAndWorkspace(String sProductName, String sWorkspace);

	List<ProcessWorkspace> getProcessByProductNameAndUserId(String sProductName, String sUserId);

	ProcessWorkspace getProcessByProcessObjId(String sProcessObjId);

	ArrayList<String> getProcessesStatusByProcessObjId(ArrayList<String> asProcessesObjId);

	boolean updateProcess(ProcessWorkspace oProcessWorkspace);

	boolean cleanQueue();
	
	boolean cleanPastProcessWorkspaces();

	boolean existsPidProcessWorkspace(Integer iPid);

	long getRunningCountByUser(String sUserId, boolean bIncluded, String sWorkspaceId);

	List<ProcessWorkspace> getRunningSummary();

	long getCreatedCountByUser(String sUserId, boolean bIncluded, String sWorkspaceId);

	List<ProcessWorkspace> getCreatedSummary();

	List<ProcessWorkspace> getList();

	boolean isProcessOwnedByUser(String sUserId, String sProcessObjId);

	String getWorkspaceByProcessObjId(String sProcessObjId);

	String getPayload(String sProcessObjId);

	List<ProcessWorkspace> getByNode(String sComputingNodeCode);

	List<ProcessWorkspace> getByNodeUnsorted(String sComputingNodeCode);

	List<ProcessWorkspace> getUnfinishedByNode(String sComputingNodeCode);

	List<ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult> getQueuesByNodeAndStatuses(String sNodeCode, String... asStatuses);

	List<ProcessWorkspace> getProcessByParentId(String sParentId);

	Long getRunningTime(String sUserId, Long lDateFrom, Long lDateTo);

	List<ProcessWorkspaceAggregatorBySubscriptionAndProject> getRunningTimeInfo(Collection<String> asSubscriptionIds);

	List<ProcessWorkspaceAggregatorBySubscriptionAndProject> getProjectRunningTime(String sUserId, Collection<String> asSubscriptionIds);
	
	void setRepoDb(String sRepoDb);
}
