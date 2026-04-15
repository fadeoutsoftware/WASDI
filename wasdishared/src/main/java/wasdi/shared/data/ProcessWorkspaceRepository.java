package wasdi.shared.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.aggregators.ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult;
import wasdi.shared.business.aggregators.ProcessWorkspaceAggregatorBySubscriptionAndProject;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IProcessWorkspaceRepositoryBackend;

/**
 * Compatibility facade for process workspace repository.
 * Mongo implementation lives in the mongo subpackage.
 */
public class ProcessWorkspaceRepository implements IProcessWorkspaceRepositoryBackend {
	
	private final IProcessWorkspaceRepositoryBackend m_oBackend;

	public ProcessWorkspaceRepository() {
		m_oBackend = createBackend();
	}
	
    public void setRepoDb(String sRepoDb) {
    	m_oBackend.setRepoDb(sRepoDb);
    }

    private IProcessWorkspaceRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createProcessWorkspaceRepository();
	}
	@Override
	public String insertProcessWorkspace(ProcessWorkspace oProcessWorkspace) {
		return m_oBackend.insertProcessWorkspace(oProcessWorkspace);
	}

	@Override
	public void insertProcessListWorkspace(List<ProcessWorkspace> aoProcessWorkspace) {
		m_oBackend.insertProcessListWorkspace(aoProcessWorkspace);
		
	}

	@Override
	public boolean deleteProcessWorkspace(String sId) {
		return m_oBackend.deleteProcessWorkspace(sId);
	}

	@Override
	public boolean deleteProcessWorkspaceByPid(int iPid) {
		return m_oBackend.deleteProcessWorkspaceByPid(iPid);
	}

	@Override
	public boolean deleteProcessWorkspaceByProcessObjId(String sProcessObjId) {
		return m_oBackend.deleteProcessWorkspaceByProcessObjId(sProcessObjId);
	}

	@Override
	public boolean deleteProcessWorkspaceByWorkspaceId(String sWorkspaceId) {
		return m_oBackend.deleteProcessWorkspaceByWorkspaceId(sWorkspaceId);
	}

	@Override
	public List<ProcessWorkspace> getRoots(List<ProcessWorkspace> aoChildren) {
		return m_oBackend.getRoots(aoChildren);
	}

	@Override
	public List<ProcessWorkspace> getFathers(List<ProcessWorkspace> aoChildProcesses) {
		return m_oBackend.getFathers(aoChildProcesses);
	}

	@Override
	public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, List<ProcessStatus> aeDesiredStatuses) {
		return m_oBackend.getProcessByWorkspace(sWorkspaceId, aeDesiredStatuses);
	}

	@Override
	public List<String> getProcessObjIdsFromWorkspaceId(String sWorkspaceId) {
		return m_oBackend.getProcessObjIdsFromWorkspaceId(sWorkspaceId);
	}

	@Override
	public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId) {
		return m_oBackend.getProcessByWorkspace(sWorkspaceId);
	}

	@Override
	public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, ProcessStatus eStatus,
			LauncherOperations eOperation, String sProductNameSubstring, Instant oDateFrom, Instant oDateTo) {
		return m_oBackend.getProcessByWorkspace(sWorkspaceId, eStatus, eOperation, sProductNameSubstring, oDateFrom, oDateTo);
	}

	@Override
	public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, int iStartIndex, int iEndIndex) {
		return m_oBackend.getProcessByWorkspace(sWorkspaceId, iStartIndex, iEndIndex);
	}

	@Override
	public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, ProcessStatus eStatus,
			LauncherOperations eOperation, String sProductNameSubstring, Instant oDateFrom, Instant oDateTo,
			int iStartIndex, int iEndIndex) {
		return m_oBackend.getProcessByWorkspace(sWorkspaceId, eStatus, eOperation, sProductNameSubstring, oDateFrom, oDateTo, iStartIndex, iEndIndex);
	}

	@Override
	public String getProcessStatusFromId(String sProcessId) {
		return m_oBackend.getProcessStatusFromId(sProcessId);
	}

	@Override
	public long countByWorkspace(String sWorkspaceId) {
		return m_oBackend.countByWorkspace(sWorkspaceId);
	}

	@Override
	public long countByProcessor(String sProcessorName) {
		return m_oBackend.countByProcessor(sProcessorName);
	}

	@Override
	public List<ProcessWorkspace> getProcessByUser(String sUserId) {
		return m_oBackend.getProcessByUser(sUserId);
	}

	@Override
	public List<ProcessWorkspace> getOGCProcessByUser(String sUserId) {
		return m_oBackend.getOGCProcessByUser(sUserId);
	}

	@Override
	public List<ProcessWorkspace> getCreatedProcesses() {
		return m_oBackend.getCreatedProcesses();
	}

	@Override
	public List<ProcessWorkspace> getCreatedProcessesByNode(String sComputingNodeCode) {
		return m_oBackend.getCreatedProcessesByNode(sComputingNodeCode);
	}

	@Override
	public List<ProcessWorkspace> getReadyProcessesByNode(String sComputingNodeCode) {
		return m_oBackend.getReadyProcessesByNode(sComputingNodeCode);
	}

	@Override
	public List<ProcessWorkspace> getRunningProcesses() {
		return m_oBackend.getRunningProcesses();
	}

	@Override
	public List<ProcessWorkspace> getRunningProcessesByNode(String sComputingNodeCode) {
		return m_oBackend.getRunningProcessesByNode(sComputingNodeCode);
	}

	@Override
	public List<ProcessWorkspace> getCreatedDownloads() {
		return m_oBackend.getCreatedDownloads();
	}

	@Override
	public List<ProcessWorkspace> getCreatedDownloadsByNode(String sComputingNodeCode) {
		return m_oBackend.getCreatedDownloadsByNode(sComputingNodeCode);
	}

	@Override
	public List<ProcessWorkspace> getCreatedIDL() {
		return m_oBackend.getCreatedIDL();
	}

	@Override
	public List<ProcessWorkspace> getCreatedIDLByNode(String sComputingNode) {
		return m_oBackend.getCreatedIDLByNode(sComputingNode);
	}

	@Override
	public List<ProcessWorkspace> getRunningDownloads() {
		return m_oBackend.getRunningDownloads();
	}

	@Override
	public List<ProcessWorkspace> getRunningDownloadsByNode(String sComputingNode) {
		return m_oBackend.getRunningDownloadsByNode(sComputingNode);
	}

	@Override
	public List<ProcessWorkspace> getReadyDownloads() {
		return m_oBackend.getReadyDownloads();
	}

	@Override
	public List<ProcessWorkspace> getReadyDownloadsByNode(String sComputingNode) {
		return m_oBackend.getReadyDownloadsByNode(sComputingNode);
	}

	@Override
	public List<ProcessWorkspace> getRunningIDL() {
		return m_oBackend.getRunningIDL();
	}

	@Override
	public List<ProcessWorkspace> getRunningIDLByNode(String sComputingNode) {
		return m_oBackend.getRunningIDLByNode(sComputingNode);
	}

	@Override
	public List<ProcessWorkspace> getReadyIDL() {
		return m_oBackend.getReadyIDL();
	}

	@Override
	public List<ProcessWorkspace> getReadyIDLByNode(String sComputingNode) {
		return m_oBackend.getReadyIDLByNode(sComputingNode);
	}

	@Override
	public List<ProcessWorkspace> getProcessesByStateNode(String sProcessStatus, String sComputingNodeCode) {
		return m_oBackend.getProcessesByStateNode(sProcessStatus, sComputingNodeCode);
	}

	@Override
	public List<ProcessWorkspace> getProcessesByStateNode(String sProcessStatus, String sComputingNodeCode,
			String sOrderByTimestamp, String sOrderByDate) {
		return m_oBackend.getProcessesByStateNode(sProcessStatus, sComputingNodeCode, sOrderByTimestamp, sOrderByDate);
	}

	@Override
	public List<ProcessWorkspace> getProcessesForSchedulerNode(String sComputingNodeCode, String sOrderBy) {
		return m_oBackend.getProcessesForSchedulerNode(sComputingNodeCode, sOrderBy);
	}

	@Override
	public List<ProcessWorkspace> getLastProcessByWorkspace(String sWorkspaceId) {
		return m_oBackend.getLastProcessByWorkspace(sWorkspaceId);
	}

	@Override
	public List<ProcessWorkspace> getLastProcessByUser(String sUserId) {
		return m_oBackend.getLastProcessByUser(sUserId);
	}

	@Override
	public List<ProcessWorkspace> getProcessOlderThan(Long dTimestamp) {
		return m_oBackend.getProcessOlderThan(dTimestamp);
	}

	@Override
	public ArrayList<ProcessWorkspace> getProcessByProductName(String sProductName) {
		return m_oBackend.getProcessByProductName(sProductName);
	}

	@Override
	public List<ProcessWorkspace> getProcessByProductNameAndWorkspace(String sProductName, String sWorkspace) {
		return m_oBackend.getProcessByProductNameAndWorkspace(sProductName, sWorkspace);
	}

	@Override
	public List<ProcessWorkspace> getProcessByProductNameAndUserId(String sProductName, String sUserId) {
		return m_oBackend.getProcessByProductNameAndUserId(sProductName, sUserId);
	}

	@Override
	public ProcessWorkspace getProcessByProcessObjId(String sProcessObjId) {
		return m_oBackend.getProcessByProcessObjId(sProcessObjId);
	}

	@Override
	public ArrayList<String> getProcessesStatusByProcessObjId(ArrayList<String> asProcessesObjId) {
		return m_oBackend.getProcessesStatusByProcessObjId(asProcessesObjId);
	}

	@Override
	public boolean updateProcess(ProcessWorkspace oProcessWorkspace) {
		return m_oBackend.updateProcess(oProcessWorkspace);
	}

	@Override
	public boolean cleanQueue() {
		return m_oBackend.cleanQueue();
	}

	@Override
	public boolean existsPidProcessWorkspace(Integer iPid) {
		return m_oBackend.existsPidProcessWorkspace(iPid);
	}

	@Override
	public long getRunningCountByUser(String sUserId, boolean bIncluded, String sWorkspaceId) {
		return m_oBackend.getRunningCountByUser(sUserId, bIncluded, sWorkspaceId);
	}

	@Override
	public List<ProcessWorkspace> getRunningSummary() {
		return m_oBackend.getRunningSummary();
	}

	@Override
	public long getCreatedCountByUser(String sUserId, boolean bIncluded, String sWorkspaceId) {
		return m_oBackend.getCreatedCountByUser(sUserId, bIncluded, sWorkspaceId);
	}

	@Override
	public List<ProcessWorkspace> getCreatedSummary() {
		return m_oBackend.getCreatedSummary();
	}

	@Override
	public List<ProcessWorkspace> getList() {
		return m_oBackend.getList();
	}

	@Override
	public boolean isProcessOwnedByUser(String sUserId, String sProcessObjId) {
		return m_oBackend.isProcessOwnedByUser(sUserId, sProcessObjId);
	}

	@Override
	public String getWorkspaceByProcessObjId(String sProcessObjId) {
		return m_oBackend.getWorkspaceByProcessObjId(sProcessObjId);
	}

	@Override
	public String getPayload(String sProcessObjId) {
		return m_oBackend.getPayload(sProcessObjId);
	}

	@Override
	public List<ProcessWorkspace> getByNode(String sComputingNodeCode) {
		return m_oBackend.getByNode(sComputingNodeCode);
	}

	@Override
	public List<ProcessWorkspace> getByNodeUnsorted(String sComputingNodeCode) {
		return m_oBackend.getByNodeUnsorted(sComputingNodeCode);
	}

	@Override
	public List<ProcessWorkspace> getUnfinishedByNode(String sComputingNodeCode) {
		return m_oBackend.getUnfinishedByNode(sComputingNodeCode);
	}

	@Override
	public List<ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult> getQueuesByNodeAndStatuses(String sNodeCode, String... asStatuses) {
		return m_oBackend.getQueuesByNodeAndStatuses(sNodeCode, asStatuses);
	}

	@Override
	public List<ProcessWorkspace> getProcessByParentId(String sParentId) {
		return m_oBackend.getProcessByParentId(sParentId);
	}

	@Override
	public Long getRunningTime(String sUserId, Long lDateFrom, Long lDateTo) {
		return m_oBackend.getRunningTime(sUserId, lDateFrom, lDateTo);
	}

	@Override
	public List<ProcessWorkspaceAggregatorBySubscriptionAndProject> getRunningTimeInfo( Collection<String> asSubscriptionIds) {
		return m_oBackend.getRunningTimeInfo(asSubscriptionIds);
	}

	@Override
	public List<ProcessWorkspaceAggregatorBySubscriptionAndProject> getProjectRunningTime(String sUserId, Collection<String> asSubscriptionIds) {
		return m_oBackend.getProjectRunningTime(sUserId, asSubscriptionIds);
	}

	@Override
	public boolean cleanPastProcessWorkspaces() {
		return m_oBackend.cleanPastProcessWorkspaces();
	}
	
	
}

