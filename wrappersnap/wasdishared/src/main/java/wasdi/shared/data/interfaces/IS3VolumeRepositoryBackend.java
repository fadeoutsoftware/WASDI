package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.S3Volume;

/**
 * Backend contract for S3 volume repository.
 */
public interface IS3VolumeRepositoryBackend {

	boolean insert(S3Volume oJob);

	int delete(String sVolumeId);

	List<S3Volume> getVolumesByUser(String sUserId);

	S3Volume getVolume(String sVolumeId);

	S3Volume getVolumeByFolderName(String sMountingFolderName);

	List<S3Volume> getVolumes();

	int deleteByUserId(String sUserId);
}
