package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.OpenEOJob;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IOpenEOJobRepositoryBackend;

public class OpenEOJobRepository {

    private final IOpenEOJobRepositoryBackend m_oBackend;

	/**
	 * Initialize the collection
	 */
	public OpenEOJobRepository () {
        m_oBackend = createBackend();
    }

    private IOpenEOJobRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createOpenEOJobRepository();
	}
	
	/**
	 * Inserts a new Open EO Job
	 * @param oOpenEOJob Entity to insert
	 * @return _id of the new object.
	 */
	public String insertOpenEOJob(OpenEOJob oOpenEOJob) {
        return m_oBackend.insertOpenEOJob(oOpenEOJob);
	}
	
	/**
	 * Delete an Open EO Job
	 * @param sJobId Id of the Process Workspace
	 * @return number of elements deleted
	 */
    public int deleteOpenEOJob(String sJobId) {
        return m_oBackend.deleteOpenEOJob(sJobId);
    }
    
    /**
     * Delete all the Open EO Job of an user
     * @param sUserId Owner of the processes
     * @return number of elements deleted
     */
    public int deleteOpenEOJobsByUser(String sUserId) {
        return m_oBackend.deleteOpenEOJobsByUser(sUserId);
    }
    
    /**
     * Delete all the Open EO Job in a workspace
     * @param sWorkspaceId Id of the workspace
     * @return number of elements deleted
     */
    public int deleteOpenEOJobsByWorkspace(String sWorkspaceId) {
        return m_oBackend.deleteOpenEOJobsByWorkspace(sWorkspaceId);
    }
    
    /**
     * Updates an  Open EO Job
     * @param oOpenEOJob Entity updated
     * @return true if updated, false if not
     */
    public boolean updateOpenEOJob(OpenEOJob oOpenEOJob) {
        return m_oBackend.updateOpenEOJob(oOpenEOJob);
    }
    
    /**
     * Read an Open EO Job
     * @param sJobId Id of the related Process Workspace
     * @return OgcProcessesTask or null
     */
    public OpenEOJob getOpenEOJob(String sJobId) {
        return m_oBackend.getOpenEOJob(sJobId);
    }
    
    /**
     * Read an Open EO Job
     * @param sJobId Id of the related Process Workspace
     * @return OgcProcessesTask or null
     */
    public List<OpenEOJob> getOpenEOJobsByUser(String sUserId) {
	    return m_oBackend.getOpenEOJobsByUser(sUserId);
    }
}

