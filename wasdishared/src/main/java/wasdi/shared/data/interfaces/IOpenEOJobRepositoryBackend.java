package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.OpenEOJob;

/**
 * Backend contract for OpenEO job repository.
 */
public interface IOpenEOJobRepositoryBackend {

	String insertOpenEOJob(OpenEOJob oOpenEOJob);

	int deleteOpenEOJob(String sJobId);

	int deleteOpenEOJobsByUser(String sUserId);

	int deleteOpenEOJobsByWorkspace(String sWorkspaceId);

	boolean updateOpenEOJob(OpenEOJob oOpenEOJob);

	OpenEOJob getOpenEOJob(String sJobId);

	List<OpenEOJob> getOpenEOJobsByUser(String sUserId);
}
