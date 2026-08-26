package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.OgcProcessesTask;

/**
 * Backend contract for OGC processes task repository.
 */
public interface IOgcProcessesTaskRepositoryBackend {

	String insertOgcProcessesTask(OgcProcessesTask oOgcProcessesTask);

	int deleteOgcProcessesTask(String sProcessWorkspaceId);

	int deleteOgcProcessesTaskByUser(String sUserId);

	int deleteOgcProcessesTaskByWorkspace(String sWorkspaceId);

	boolean updateOgcProcessesTask(OgcProcessesTask oOgcProcessesTask);

	OgcProcessesTask getOgcProcessesTask(String sProcessWorkspaceId);

	List<String> getProcessWsIds(String sUserId);
}
