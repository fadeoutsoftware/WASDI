package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;

import wasdi.shared.business.S3Volume;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class S3VolumeRepository extends MongoRepository {

	public S3VolumeRepository() {
		m_sThisCollection = "s3volumes";
	}
	
	public boolean insert(S3Volume oJob) {
        try {        	
        	this.add(oJob);
            return true;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("S3VolumeRepository.insert: error ", oEx);
        }
        return false;
	}
	
    public int delete(String sVolumeId) {
    	if (Utils.isNullOrEmpty(sVolumeId)) return 0;
    	
    	try {
    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("volumeId", sVolumeId);

            return delete(oCriteria);    		
    	}
        catch (Exception oEx) {
        	WasdiLog.errorLog("S3VolumeRepository.delete: error ", oEx);
        	return -1;
        }
    }
    
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
    
    /**
     * Get a volume by Id
     * @param sVolumeId id of the volume
     * @return Entity
     */
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

        return  null;
    }    
    
    
    /**
     * Get a volume by Folder Name
     * @param sVolumeId id of the volume
     * @return Entity
     */
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

        return  null;
    }        

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
    
    /**
     * Delete all the volumes of a user
     * @param sUserId
     * @return
     */
    public int deleteByUserId(String sUserId) {
    	if (Utils.isNullOrEmpty(sUserId)) return 0;
    	
    	try {
    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("userId", sUserId);

            return deleteMany(oCriteria);    		
    	}
        catch (Exception oEx) {
        	WasdiLog.errorLog("S3VolumeRepository.delete: error ", oEx);
        	return -1;
        }
    }    
}
