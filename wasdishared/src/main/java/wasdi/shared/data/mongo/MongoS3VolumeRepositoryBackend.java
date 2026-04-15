package wasdi.shared.data.mongo;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;

import wasdi.shared.business.S3Volume;
import wasdi.shared.data.interfaces.IS3VolumeRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for S3 volume repository.
 */
public class MongoS3VolumeRepositoryBackend extends MongoRepository implements IS3VolumeRepositoryBackend {

	public MongoS3VolumeRepositoryBackend() {
		m_sThisCollection = "s3volumes";
	}

	@Override
	public boolean insert(S3Volume oJob) {
		try {
			this.add(oJob);
			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("S3VolumeRepository.insert: error ", oEx);
		}
		return false;
	}

	@Override
	public int delete(String sVolumeId) {
		if (Utils.isNullOrEmpty(sVolumeId)) {
			return 0;
		}

		try {
			BasicDBObject oCriteria = new BasicDBObject();
			oCriteria.append("volumeId", sVolumeId);

			return delete(oCriteria);
		} catch (Exception oEx) {
			WasdiLog.errorLog("S3VolumeRepository.delete: error ", oEx);
			return -1;
		}
	}

	@Override
	public List<S3Volume> getVolumesByUser(String sUserId) {
		final ArrayList<S3Volume> aoReturnList = new ArrayList<S3Volume>();
		try {

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId));
			fillList(aoReturnList, oWSDocuments, S3Volume.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("S3VolumeRepository.getVolumesByUser: error", oEx);
		}

		return aoReturnList;
	}

	@Override
	public S3Volume getVolume(String sVolumeId) {

		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("volumeId", sVolumeId)).first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();
				return s_oMapper.readValue(sJSON, S3Volume.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("S3VolumeRepository.getVolume: error", oEx);
		}

		return null;
	}

	@Override
	public S3Volume getVolumeByFolderName(String sMountingFolderName) {

		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("mountingFolderName", sMountingFolderName)).first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();
				return s_oMapper.readValue(sJSON, S3Volume.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("S3VolumeRepository.getVolumeByFolderName: error", oEx);
		}

		return null;
	}

	@Override
	public List<S3Volume> getVolumes() {
		final ArrayList<S3Volume> aoReturnList = new ArrayList<S3Volume>();
		try {

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();
			fillList(aoReturnList, oWSDocuments, S3Volume.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("S3VolumeRepository.getVolumes: error", oEx);
		}

		return aoReturnList;
	}

	@Override
	public int deleteByUserId(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			BasicDBObject oCriteria = new BasicDBObject();
			oCriteria.append("userId", sUserId);

			return deleteMany(oCriteria);
		} catch (Exception oEx) {
			WasdiLog.errorLog("S3VolumeRepository.delete: error ", oEx);
			return -1;
		}
	}
}
