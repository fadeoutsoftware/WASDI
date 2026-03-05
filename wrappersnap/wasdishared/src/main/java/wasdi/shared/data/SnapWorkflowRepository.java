package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.ISnapWorkflowRepositoryBackend;

public class SnapWorkflowRepository {

	private final ISnapWorkflowRepositoryBackend m_oBackend;

    public SnapWorkflowRepository() {
        m_oBackend = createBackend();
    }

    private ISnapWorkflowRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createSnapWorkflowRepository();
    }

    /**
     * Insert a new workflow
     *
     * @param oWorkflow
     * @return
     */
    public boolean insertSnapWorkflow(SnapWorkflow oWorkflow) {
		return m_oBackend.insertSnapWorkflow(oWorkflow);
    }

    /**
     * Get a workflow by Id
     *
     * @param sWorkflowId
     * @return
     */
    public SnapWorkflow getSnapWorkflow(String sWorkflowId) {
		return m_oBackend.getSnapWorkflow(sWorkflowId);
    }

    /**
     * Get all the workflow that can be accessed by UserId
     *
     * @param sUserId
     * @return List of private workflow of users plus all the public ones
     */
    public List<SnapWorkflow> getSnapWorkflowPublicAndByUser(String sUserId) {
        return m_oBackend.getSnapWorkflowPublicAndByUser(sUserId);
    }

    /**
     * Get the list of all workflows
     *
     * @return
     */
    public List<SnapWorkflow> getList() {
        return m_oBackend.getList();
    }

    
    /**
     * Find a workflow by partial name, by partial description or by partial id
     * @return the list of workflows that partially match the name, the description or the id
     */
    public List<SnapWorkflow> findWorkflowByPartialName(String sPartialName) {
        return m_oBackend.findWorkflowByPartialName(sPartialName);
    }
    
    
    /**
     * Update a Workflow
     *
     * @param oSnapWorkflow
     * @return
     */
    public boolean updateSnapWorkflow(SnapWorkflow oSnapWorkflow) {
        return m_oBackend.updateSnapWorkflow(oSnapWorkflow);
    }

    /**
     * Deletes a workflow
     */
    public boolean deleteSnapWorkflow(String sWorkflowId) {
        return m_oBackend.deleteSnapWorkflow(sWorkflowId);
    }

    /**
     * Delete all the workflows of User
     *
     * @param sUserId
     * @return
     */
    public int deleteSnapWorkflowByUser(String sUserId) {
        return m_oBackend.deleteSnapWorkflowByUser(sUserId);
    }
}

