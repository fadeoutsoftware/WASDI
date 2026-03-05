package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.S3Volume;
import wasdi.shared.data.interfaces.IS3VolumeRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for S3 volume repository.
 */
public class No2S3VolumeRepositoryBackend extends No2Repository implements IS3VolumeRepositoryBackend {

	private static final String s_sCollectionName = "s3volumes";

	@Override
	public boolean insert(S3Volume oJob) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oJob == null) {
				return false;
			}
			oCollection.insert(toDocument(oJob));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2S3VolumeRepositoryBackend.insert", oEx);
		}
		return false;
	}

	@Override
	public int delete(String sVolumeId) {
		if (Utils.isNullOrEmpty(sVolumeId)) {
			return 0;
		}
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return -1;
			}
			int iCount = 0;
			for (Document oDoc : oCollection.find(where("volumeId").eq(sVolumeId))) {
				if (oDoc != null) {
					iCount++;
				}
			}
			oCollection.remove(where("volumeId").eq(sVolumeId));
			return iCount > 0 ? 1 : 0;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2S3VolumeRepositoryBackend.delete", oEx);
			return -1;
		}
	}

	@Override
	public List<S3Volume> getVolumesByUser(String sUserId) {
		List<S3Volume> aoReturnList = new ArrayList<>();
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection != null) {
				aoReturnList = toList(oCollection.find(where("userId").eq(sUserId)), S3Volume.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2S3VolumeRepositoryBackend.getVolumesByUser", oEx);
		}
		return aoReturnList;
	}

	@Override
	public S3Volume getVolume(String sVolumeId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}
			for (Document oDoc : oCollection.find(where("volumeId").eq(sVolumeId))) {
				return fromDocument(oDoc, S3Volume.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2S3VolumeRepositoryBackend.getVolume", oEx);
		}
		return null;
	}

	@Override
	public S3Volume getVolumeByFolderName(String sMountingFolderName) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}
			for (Document oDoc : oCollection.find(where("mountingFolderName").eq(sMountingFolderName))) {
				return fromDocument(oDoc, S3Volume.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2S3VolumeRepositoryBackend.getVolumeByFolderName", oEx);
		}
		return null;
	}

	@Override
	public List<S3Volume> getVolumes() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			return toList(oCollection != null ? oCollection.find() : null, S3Volume.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2S3VolumeRepositoryBackend.getVolumes", oEx);
		}
		return new ArrayList<>();
	}

	@Override
	public int deleteByUserId(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return -1;
			}
			int iCount = 0;
			for (Document oDoc : oCollection.find(where("userId").eq(sUserId))) {
				if (oDoc != null) {
					iCount++;
				}
			}
			oCollection.remove(where("userId").eq(sUserId));
			return iCount;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2S3VolumeRepositoryBackend.deleteByUserId", oEx);
			return -1;
		}
	}
}
