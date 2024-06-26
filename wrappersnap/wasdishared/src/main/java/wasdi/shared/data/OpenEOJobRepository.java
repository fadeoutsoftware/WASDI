package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;

import wasdi.shared.business.OpenEOJob;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class OpenEOJobRepository extends MongoRepository  {
	/**
	 * Initialize the collection
	 */
	public OpenEOJobRepository () {
		m_sThisCollection = "openeojobs";
	}
	
	/**
	 * Inserts a new Open EO Job
	 * @param oOpenEOJob Entity to insert
	 * @return _id of the new object.
	 */
	public String insertOpenEOJob(OpenEOJob oOpenEOJob) {
		return add(oOpenEOJob);
	}
	
	/**
	 * Delete an Open EO Job
	 * @param sJobId Id of the Process Workspace
	 * @return number of elements deleted
	 */
    public int deleteOpenEOJob(String sJobId) {
    	if (Utils.isNullOrEmpty(sJobId)) return 0;
    	
    	try {
    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("jobId", sJobId);

            return delete(oCriteria);    		
    	}
        catch (Exception oEx) {
        	WasdiLog.errorLog("OpenEOJobRepository.deleteOpenEOJob: error ", oEx);
        	return -1;
        }
    }
    
    /**
     * Delete all the Open EO Job of an user
     * @param sUserId Owner of the processes
     * @return number of elements deleted
     */
    public int deleteOpenEOJobsByUser(String sUserId) {
    	if (Utils.isNullOrEmpty(sUserId)) return 0;
    	
    	try {
    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("userId", sUserId);

            return deleteMany(oCriteria);
    	}
        catch (Exception oEx) {
        	WasdiLog.errorLog("OpenEOJobRepository.deleteOpenEOJobsByUser: error ", oEx);
        	return -1;
        }
    }
    
    /**
     * Delete all the Open EO Job in a workspace
     * @param sWorkspaceId Id of the workspace
     * @return number of elements deleted
     */
    public int deleteOpenEOJobsByWorkspace(String sWorkspaceId) {
    	if (Utils.isNullOrEmpty(sWorkspaceId)) return 0;
    	
    	try {
    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("workspaceId", sWorkspaceId);

            return deleteMany(oCriteria);    		
    	}
        catch (Exception oEx) {
        	WasdiLog.errorLog("OpenEOJobRepository.deleteOpenEOJobsByWorkspace: error ", oEx);
        	return -1;
        }
    }
    
    /**
     * Updates an  Open EO Job
     * @param oOpenEOJob Entity updated
     * @return true if updated, false if not
     */
    public boolean updateOpenEOJob(OpenEOJob oOpenEOJob) {
    	
    	try {
    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("jobId", oOpenEOJob.getJobId());
    		return  update(oCriteria, oOpenEOJob, m_sThisCollection);	
    	}
        catch (Exception oEx) {
        	WasdiLog.errorLog("OpenEOJobRepository.updateOpenEOJob: error ", oEx);
        	return false;
        }
    }
    
    /**
     * Read an Open EO Job
     * @param sJobId Id of the related Process Workspace
     * @return OgcProcessesTask or null
     */
    public OpenEOJob getOpenEOJob(String sJobId) {
        try {
            Document oWSDocument = getCollection(m_sThisCollection).find(new Document("jobId", sJobId)).first();

            if (null != oWSDocument) {
            	String sJSON = oWSDocument.toJson();
            	return s_oMapper.readValue(sJSON, OpenEOJob.class);
            }
        } catch (Exception oEx) {
            WasdiLog.errorLog("OpenEOJobRepository.getOpenEOJob: error ", oEx);
        }

        return  null;
    	
    }
    
    /**
     * Read an Open EO Job
     * @param sJobId Id of the related Process Workspace
     * @return OgcProcessesTask or null
     */
    public List<OpenEOJob> getOpenEOJobsByUser(String sUserId) {
    	
        final ArrayList<OpenEOJob> aoReturnList = new ArrayList<OpenEOJob>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId));
            
            fillList(aoReturnList, oWSDocuments, OpenEOJob.class);
            
        } 
        catch (Exception oEx) {
        	WasdiLog.errorLog("OpenEOJobRepository.getOpenEOJobsByUser: error ", oEx);
        }

        return aoReturnList; 
    	
    }    
}
