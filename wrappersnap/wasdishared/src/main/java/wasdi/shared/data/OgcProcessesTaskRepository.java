package wasdi.shared.data;

import org.bson.Document;

import com.mongodb.BasicDBObject;

import wasdi.shared.business.OgcProcessesTask;
import wasdi.shared.utils.Utils;

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
    	if (Utils.isNullOrEmpty(sProcessWorkspaceId)) return 0;

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("processWorkspaceId", sProcessWorkspaceId);

        return delete(oCriteria);
    }
    
    /**
     * Delete all the Ogc Processes Task of an user
     * @param sUserId Owner of the processes
     * @return number of elements deleted
     */
    public int deleteOgcProcessesTaskByUser(String sUserId) {
    	if (Utils.isNullOrEmpty(sUserId)) return 0;

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("userId", sUserId);

        return deleteMany(oCriteria);
    }
    
    /**
     * Delete all the Ogc Processes Task in a workspace
     * @param sWorkspaceId Id of the workspace
     * @return number of elements deleted
     */
    public int deleteOgcProcessesTaskByWorkspace(String sWorkspaceId) {
    	if (Utils.isNullOrEmpty(sWorkspaceId)) return 0;

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("workspaceId", sWorkspaceId);

        return deleteMany(oCriteria);
    }
    
    /**
     * Updates an  OgcProcessesTask
     * @param oOgcProcessesTask Entity updated
     * @return true if updated, false if not
     */
    public boolean updateOgcProcessesTask(OgcProcessesTask oOgcProcessesTask) {
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("processWorkspaceId", oOgcProcessesTask.getProcessWorkspaceId());

        return  update(oCriteria, oOgcProcessesTask, m_sThisCollection);
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
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    	
    }

}
