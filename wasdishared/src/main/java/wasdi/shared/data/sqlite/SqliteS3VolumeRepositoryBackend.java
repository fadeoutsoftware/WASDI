package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.List;

import wasdi.shared.business.S3Volume;
import wasdi.shared.data.interfaces.IS3VolumeRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for S3 volume repository.
 */
public class SqliteS3VolumeRepositoryBackend extends SqliteRepository implements IS3VolumeRepositoryBackend {

	public SqliteS3VolumeRepositoryBackend() {
		m_sThisCollection = "s3volumes";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insert(S3Volume oJob) {
		try {
			return insert(oJob.getVolumeId(), oJob);
		} catch (Exception oEx) {
			WasdiLog.errorLog("S3VolumeRepository.insert: error ", oEx);
		}
		return false;
	}

	@Override
	public int delete(String sVolumeId) {
		if (Utils.isNullOrEmpty(sVolumeId)) return 0;
		try {
			return deleteWhere("volumeId", sVolumeId);
		} catch (Exception oEx) {
			WasdiLog.errorLog("S3VolumeRepository.delete: error ", oEx);
			return -1;
		}
	}

	@Override
	public List<S3Volume> getVolumesByUser(String sUserId) {
		try {
			return findAllWhere("userId", sUserId, S3Volume.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("S3VolumeRepository.getVolumesByUser: error", oEx);
		}
		return new ArrayList<>();
	}

	@Override
	public S3Volume getVolume(String sVolumeId) {
		try {
			return findOneWhere("volumeId", sVolumeId, S3Volume.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("S3VolumeRepository.getVolume: error", oEx);
		}
		return null;
	}

	@Override
	public S3Volume getVolumeByFolderName(String sMountingFolderName) {
		try {
			return findOneWhere("mountingFolderName", sMountingFolderName, S3Volume.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("S3VolumeRepository.getVolumeByFolderName: error", oEx);
		}
		return null;
	}

	@Override
	public List<S3Volume> getVolumes() {
		try {
			return findAll(S3Volume.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("S3VolumeRepository.getVolumes: error", oEx);
		}
		return new ArrayList<>();
	}

	@Override
	public int deleteByUserId(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) return 0;
		try {
			return deleteWhere("userId", sUserId);
		} catch (Exception oEx) {
			WasdiLog.errorLog("S3VolumeRepository.deleteByUserId: error ", oEx);
			return -1;
		}
	}
}
