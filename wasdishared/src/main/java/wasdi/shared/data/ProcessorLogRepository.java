package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.processors.ProcessorLog;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IProcessorLogRepositoryBackend;

/**
 * User Processors Log repository
 * @author p.campanella
 *
 */
public class ProcessorLogRepository {

    private final IProcessorLogRepositoryBackend m_oBackend;
	
	public ProcessorLogRepository() {
        m_oBackend = createBackend();
    }

    private IProcessorLogRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createProcessorLogRepository();
	}
    
    public void setRepoDb(String sRepoDb) {
    	m_oBackend.setRepoDb(sRepoDb);
    }
	
	/**
	 * Insert a new log row
	 * @param oProcessLog row to add
	 * @return Mongo Obj Id
	 */
    public String insertProcessLog(ProcessorLog oProcessLog) {
        return m_oBackend.insertProcessLog(oProcessLog);
    }
    
	/**
	 * Insert a new log row
	 * @param aoProcessLogs a list of rows to add
	 */
    public void insertProcessLogList(List<ProcessorLog> aoProcessLogs) {
        m_oBackend.insertProcessLogList(aoProcessLogs);
        return;
    }
    
    /**
     * Delete a Log row by Mongo Id
     * @param sId Mongo Obj Id
     * @return True or False
     */
    public boolean deleteProcessorLog(String sId) {
        return m_oBackend.deleteProcessorLog(sId);
    }
    
    /**
     * Get the logs of a ProcessWorkspace
     * @param sProcessWorkspaceId Id of the process
     * @return List of all the log rows
     */
    public List<ProcessorLog> getLogsByProcessWorkspaceId(String sProcessWorkspaceId) {
        return m_oBackend.getLogsByProcessWorkspaceId(sProcessWorkspaceId);
    }
    
    /**
     * Get all the logs of an array of ProcessWorkspaceId
     * @param asProcessWorkspaceId
     * @return
     */
    public List<ProcessorLog> getLogsByArrayProcessWorkspaceId(List<String> asProcessWorkspaceId) {
        return m_oBackend.getLogsByArrayProcessWorkspaceId(asProcessWorkspaceId);
    }
    
    //
    /**
     * Deletes logs older than a specified date
     * makes a query like: {logDate: {"$lt":"2019-05-04 00:00:00"}}
     * @param sDate Date in format YYYY-MM-DD
     * @return
     */
    public boolean deleteLogsOlderThan(String sDate) {
        return m_oBackend.deleteLogsOlderThan(sDate);
    }
    
    /**
     * Delete all the logs of a Process Workspace
     * @param sProcessWorkspaceId Id of the process
     * @return True or False in case of error
     */
    public boolean deleteLogsByProcessWorkspaceId(String sProcessWorkspaceId) {
        return m_oBackend.deleteLogsByProcessWorkspaceId(sProcessWorkspaceId);
    }
        
    /**
     * Get all the log rows containing a specified text
     * @param sLogText Text to search for
     * @return List of log rows containing the text
     */
    public List<ProcessorLog> getLogRowsByText(String sLogText) {
        return m_oBackend.getLogRowsByText(sLogText);
    }
    
    /**
     * Get all the log rows of a ProcessWorkpsace containing a specified text
     * @param sLogText sLogText Text to search for
     * @param sProcessWorkspaceId WASDI Id of the Process Workspace
     * @return List of found rows
     */
    public List<ProcessorLog> getLogRowsByTextAndProcessId(String sLogText, String sProcessWorkspaceId) {
        return m_oBackend.getLogRowsByTextAndProcessId(sLogText, sProcessWorkspaceId);
    }
    
	/**
	 * Get a list of logs row in a range
	 * iLo and iUp are included
	 * @param sProcessWorkspaceId Process Workpsace Id
	 * @param iLo Lower Bound
	 * @param iUp Upper Bound
	 * @return List of Log Rows in the range
	 */
    public List<ProcessorLog> getLogsByWorkspaceIdInRange(String sProcessWorkspaceId, Integer iLo, Integer iUp) {
        return m_oBackend.getLogsByWorkspaceIdInRange(sProcessWorkspaceId, iLo, iUp);

	}
    
    /**
     * Get the list of all processes
     * @return
     */
    public List<ProcessorLog> getList() {
        return m_oBackend.getList();
    }

	

}

