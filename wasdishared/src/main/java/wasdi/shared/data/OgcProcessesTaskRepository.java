package wasdi.shared.data;

import wasdi.shared.business.OgcProcessesTask;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IOgcProcessesTaskRepositoryBackend;

/**
 * Ogc Processes Task Repository.
 * Offers methods to create, read, update and search OgcProcessesTask entities from mongo db 
 * @author p.campanella
 *
 */
public class OgcProcessesTaskRepository {

	private final IOgcProcessesTaskRepositoryBackend m_oBackend;

	/**
	 * Initialize the collection
	 */
	public OgcProcessesTaskRepository () {
		m_oBackend = createBackend();
	}

	private IOgcProcessesTaskRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createOgcProcessesTaskRepository();
	}
	
	/**
	 * Inserts a new OgcProcessesTask
	 * @param oOgcProcessesTask Entity to insert
	 * @return _id of the new object.
	 */
	public String insertOgcProcessesTask(OgcProcessesTask oOgcProcessesTask) {
		return m_oBackend.insertOgcProcessesTask(oOgcProcessesTask);
	}
	
	/**
	 * Delete an Ogc Processes Task
	 * @param sProcessWorkspaceId Id of the Process Workspace
	 * @return number of elements deleted
	 */
    public int deleteOgcProcessesTask(String sProcessWorkspaceId) {
	    return m_oBackend.deleteOgcProcessesTask(sProcessWorkspaceId);
    }
    
    /**
     * Delete all the Ogc Processes Task of an user
     * @param sUserId Owner of the processes
     * @return number of elements deleted
     */
    public int deleteOgcProcessesTaskByUser(String sUserId) {
	    return m_oBackend.deleteOgcProcessesTaskByUser(sUserId);
    }
    
    /**
     * Delete all the Ogc Processes Task in a workspace
     * @param sWorkspaceId Id of the workspace
     * @return number of elements deleted
     */
    public int deleteOgcProcessesTaskByWorkspace(String sWorkspaceId) {
	    return m_oBackend.deleteOgcProcessesTaskByWorkspace(sWorkspaceId);
    }
    
    /**
     * Updates an  OgcProcessesTask
     * @param oOgcProcessesTask Entity updated
     * @return true if updated, false if not
     */
    public boolean updateOgcProcessesTask(OgcProcessesTask oOgcProcessesTask) {
	    return m_oBackend.updateOgcProcessesTask(oOgcProcessesTask);
    }
    
    /**
     * Read an OgcProcessesTask
     * @param sProcessWorkspaceId Id of the related Process Workspace
     * @return OgcProcessesTask or null
     */
    public OgcProcessesTask getOgcProcessesTask(String sProcessWorkspaceId) {
		return m_oBackend.getOgcProcessesTask(sProcessWorkspaceId);
    }

}

