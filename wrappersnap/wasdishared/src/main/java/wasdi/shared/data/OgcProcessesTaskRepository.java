package wasdi.shared.data;

import org.bson.Document;

import com.mongodb.BasicDBObject;

import wasdi.shared.business.OgcProcessesTask;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Ogc Processes Task Repository.
 * Offers methods to create, read, update and search OgcProcessesTask entities from mongo db 
 * @author p.campanella
 *
 */
public class OgcProcessesTaskRepository  extends MongoRepository {

	/**
	 * Initialize the collection
	 */
	public OgcProcessesTaskRepository () {
		m_sThisCollection = "ogcprocessestask";
	}
	
	/**
	 * Inserts a new OgcProcessesTask
	 * @param oOgcProcessesTask Entity to insert
	 * @return _id of the new object.
	 */
	public String insertOgcProcessesTask(OgcProcessesTask oOgcProcessesTask) {
		return add(oOgcProcessesTask);
	}
	
	/**
	 * Delete an Ogc Processes Task
	 * @param sProcessWorkspaceId Id of the Process Workspace
	 * @return number of elements deleted
	 */
    public int deleteOgcProcessesTask(String sProcessWorkspaceId) {
    	try {
        	if (Utils.isNullOrEmpty(sProcessWorkspaceId)) return 0;

    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("processWorkspaceId", sProcessWorkspaceId);

            return delete(oCriteria);    		
    	}
        catch (Exception oEx) {
        	WasdiLog.errorLog("OgcProcessesTaskRepository.deleteOgcProcessesTask: error", oEx);
        	return -1;
        }        	
    }
    
    /**
     * Delete all the Ogc Processes Task of an user
     * @param sUserId Owner of the processes
     * @return number of elements deleted
     */
    public int deleteOgcProcessesTaskByUser(String sUserId) {
    	try {
        	if (Utils.isNullOrEmpty(sUserId)) return 0;

    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("userId", sUserId);

            return deleteMany(oCriteria);    		
    	}
        catch (Exception oEx) {
        	WasdiLog.errorLog("OgcProcessesTaskRepository.deleteOgcProcessesTaskByUser: error", oEx);
        	return -1;
        }       	
    }
    
    /**
     * Delete all the Ogc Processes Task in a workspace
     * @param sWorkspaceId Id of the workspace
     * @return number of elements deleted
     */
    public int deleteOgcProcessesTaskByWorkspace(String sWorkspaceId) {
    	try {
        	if (Utils.isNullOrEmpty(sWorkspaceId)) return 0;

    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("workspaceId", sWorkspaceId);

            return deleteMany(oCriteria);    		
    	}
        catch (Exception oEx) {
        	WasdiLog.errorLog("OgcProcessesTaskRepository.deleteOgcProcessesTaskByWorkspace: error", oEx);
        	return -1;
        }    	
    }
    
    /**
     * Updates an  OgcProcessesTask
     * @param oOgcProcessesTask Entity updated
     * @return true if updated, false if not
     */
    public boolean updateOgcProcessesTask(OgcProcessesTask oOgcProcessesTask) {
    	try {
			BasicDBObject oCriteria = new BasicDBObject();
			oCriteria.append("processWorkspaceId", oOgcProcessesTask.getProcessWorkspaceId());
	
	        return  update(oCriteria, oOgcProcessesTask, m_sThisCollection);
    	}
        catch (Exception oEx) {
        	WasdiLog.errorLog("OgcProcessesTaskRepository.updateOgcProcessesTask: error", oEx);
        	return false;
        }
    	
    }
    
    /**
     * Read an OgcProcessesTask
     * @param sProcessWorkspaceId Id of the related Process Workspace
     * @return OgcProcessesTask or null
     */
    public OgcProcessesTask getOgcProcessesTask(String sProcessWorkspaceId) {
        try {
            Document oWSDocument = getCollection(m_sThisCollection).find(new Document("processWorkspaceId", sProcessWorkspaceId)).first();

            if (null != oWSDocument) {
            	String sJSON = oWSDocument.toJson();
            	return s_oMapper.readValue(sJSON, OgcProcessesTask.class);
            }
        } 
        catch (Exception oEx) {
        	WasdiLog.errorLog("OgcProcessesTaskRepository.getOgcProcessesTask: error", oEx);
        }

        return  null;
    	
    }

}
