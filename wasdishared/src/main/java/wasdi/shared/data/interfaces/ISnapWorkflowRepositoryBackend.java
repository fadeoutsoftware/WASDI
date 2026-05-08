package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.SnapWorkflow;

/**
 * Backend contract for snap workflow repository.
 */
public interface ISnapWorkflowRepositoryBackend {

	boolean insertSnapWorkflow(SnapWorkflow oWorkflow);

	SnapWorkflow getSnapWorkflow(String sWorkflowId);

	List<SnapWorkflow> getSnapWorkflowPublicAndByUser(String sUserId);

	List<SnapWorkflow> getList();

	List<SnapWorkflow> findWorkflowByPartialName(String sPartialName);

	boolean updateSnapWorkflow(SnapWorkflow oSnapWorkflow);

	boolean deleteSnapWorkflow(String sWorkflowId);

	int deleteSnapWorkflowByUser(String sUserId);
	
	public SnapWorkflow getByName(String sWorkflowName);
}
