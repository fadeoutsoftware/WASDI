package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.ProcessorSharing;
import wasdi.shared.utils.Utils;

/**
 * Processor Sharing Repository
 * @author p.campanella
 *
 */
public class ProcessorSharingRepository  extends  MongoRepository {
	
	public ProcessorSharingRepository() {
		m_sThisCollection = "processorsharing";
	}
	
	/**
	 * Create a new processor sharing (share of a processor with another user
	 * @param oProcessorSharing Entity
	 * @return True of False in case of exception
	 */
    public boolean insertProcessorSharing(ProcessorSharing oProcessorSharing) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oProcessorSharing);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    /**
     * Get all the Sharings of a Owner User Id
     * @param sUserId User Id of the owner
     * @return List of all the sharings made by this owner
     */
    public List<ProcessorSharing> getProcessorSharingByOwner(String sUserId) {

        final ArrayList<ProcessorSharing> aoReturnList = new ArrayList<ProcessorSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("ownerId", sUserId));
            
            fillList(aoReturnList, oWSDocuments, ProcessorSharing.class);
            

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get a list of sharing received by a user
     * @param sUserId User that received the sharings
     * @return List of all the processor shared with UserID
     */
    public List<ProcessorSharing> getProcessorSharingByUser(String sUserId) {

        final ArrayList<ProcessorSharing> aoReturnList = new ArrayList<ProcessorSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId));
            
            fillList(aoReturnList, oWSDocuments, ProcessorSharing.class);
            
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

    /**
     * Get all the sharing of a processor
     * @param sProcessorId WASDI Id of the processor
     * @return List of all the sharing entities of this processor
     */
    public List<ProcessorSharing> getProcessorSharingByProcessorId(String sProcessorId) {

        final ArrayList<ProcessorSharing> aoReturnList = new ArrayList<ProcessorSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("processorId", sProcessorId));
            
            fillList(aoReturnList, oWSDocuments, ProcessorSharing.class);
            
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the sharing of a processor with a specific user
     * @param sUserId User that received the sharing 
     * @param sProcessorId Processor Shared
     * @return Entity if exists, null if the processor is not shared with UserId
     */
    public ProcessorSharing getProcessorSharingByUserIdProcessorId(String sUserId, String sProcessorId) {

        final ArrayList<ProcessorSharing> aoReturnList = new ArrayList<ProcessorSharing>();
        try {
        	
            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("processorId", sProcessorId)));
            
            fillList(aoReturnList, oWSDocuments, ProcessorSharing.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }
        
        if (aoReturnList.size() > 0)
            return aoReturnList.get(0);
        

        return null;
    }
    
    /**
     * Delete all the sharings of a processor
     * @param sProcessorId WASDI id of the processor
     * @return number of sharing deleted
     */
    public int deleteByProcessorId(String sProcessorId) {
    	
    	if (Utils.isNullOrEmpty(sProcessorId)) return 0;

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("processorId", sProcessorId));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }
    
    /**
     * Delete all the sharings of a user
     * @param sUserId User Id that received the sharings to delete
     * @return Number of sharing deleted
     */
    public int deleteByUserId(String sUserId) {
    	
    	if (Utils.isNullOrEmpty(sUserId)) return 0;

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("userId", sUserId));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }
    
    /**
     * Delete the sharing of ProcessorId for UserId
     * @param sUserId User that had the sharing
     * @param sProcessorId Processor shared
     * @return 1 if it was deleted, 0 if it did not exists
     */
    public int deleteByUserIdProcessorId(String sUserId, String sProcessorId) {
    	
    	if (Utils.isNullOrEmpty(sProcessorId)) return 0;
    	if (Utils.isNullOrEmpty(sUserId)) return 0;
    	
        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(Filters.and(Filters.eq("userId", sUserId), Filters.eq("processorId", sProcessorId)));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } 
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    } 
}
