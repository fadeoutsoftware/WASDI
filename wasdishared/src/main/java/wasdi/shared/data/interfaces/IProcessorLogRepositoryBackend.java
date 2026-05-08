package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.processors.ProcessorLog;

/**
 * Backend contract for processor log repository.
 */
public interface IProcessorLogRepositoryBackend {

	String insertProcessLog(ProcessorLog oProcessLog);

	void insertProcessLogList(List<ProcessorLog> aoProcessLogs);

	boolean deleteProcessorLog(String sId);

	List<ProcessorLog> getLogsByProcessWorkspaceId(String sProcessWorkspaceId);

	List<ProcessorLog> getLogsByArrayProcessWorkspaceId(List<String> asProcessWorkspaceId);

	boolean deleteLogsOlderThan(String sDate);

	boolean deleteLogsByProcessWorkspaceId(String sProcessWorkspaceId);

	List<ProcessorLog> getLogRowsByText(String sLogText);

	List<ProcessorLog> getLogRowsByTextAndProcessId(String sLogText, String sProcessWorkspaceId);

	List<ProcessorLog> getLogsByWorkspaceIdInRange(String sProcessWorkspaceId, Integer iLo, Integer iUp);

	List<ProcessorLog> getList();
	
	void setRepoDb(String sRepoDb);
}
