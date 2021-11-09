package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.WorkflowSharing;
import wasdi.shared.utils.Utils;

/**
 * Created by M.Menapace on 29/04/2021.
 */
public class WorkflowSharingRepository extends  MongoRepository{
	
	public WorkflowSharingRepository() {
		m_sThisCollection = "workflowssharing";
	}

	/**
	 * Insert a New Workflow sharing
	 * @param oWorkflowSharing
	 * @return
	 */
    public boolean insertWorkflowSharing(WorkflowSharing oWorkflowSharing) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oWorkflowSharing);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    /**
     * Get all the workflows shared by this owner User 
     * @param sUserId
     * @return
     */
    public List<WorkflowSharing> getWorkflowSharingByOwner(String sUserId) {

        final ArrayList<WorkflowSharing> aoReturnList = new ArrayList<WorkflowSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("ownerId", sUserId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    WorkflowSharing oWorkflowSharing = null;
                    try {
                        oWorkflowSharing = s_oMapper.readValue(sJSON,WorkflowSharing.class);
                        aoReturnList.add(oWorkflowSharing);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get all the workflows shared with this User
     * @param sUserId
     * @return
     */
    public List<WorkflowSharing> getWorkflowSharingByUser(String sUserId) {

        final ArrayList<WorkflowSharing> aoReturnList = new ArrayList<WorkflowSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    WorkflowSharing oWorkflowSharing = null;
                    try {
                        oWorkflowSharing = s_oMapper.readValue(sJSON,WorkflowSharing.class);
                        aoReturnList.add(oWorkflowSharing);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

    /**
     * Get all the sharings of this workflow
     * @param sWorkflowId
     * @return
     */
    public List<WorkflowSharing> getWorkflowSharingByWorkflow(String sWorkflowId) {

        final ArrayList<WorkflowSharing> aoReturnList = new ArrayList<WorkflowSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("workflowId", sWorkflowId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    WorkflowSharing oWorkflowSharing = null;
                    try {
                        oWorkflowSharing = s_oMapper.readValue(sJSON,WorkflowSharing.class);
                        aoReturnList.add(oWorkflowSharing);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get all the sharings 
     * @param sWorkflowId
     * @return
     */
    public List<WorkflowSharing> getWorkflowSharings() {

        final ArrayList<WorkflowSharing> aoReturnList = new ArrayList<WorkflowSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    WorkflowSharing oWorkflowSharing = null;
                    try {
                        oWorkflowSharing = s_oMapper.readValue(sJSON,WorkflowSharing.class);
                        aoReturnList.add(oWorkflowSharing);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }    
    
    /**
     * Deletes all the instances of sharing of an user for a specific workflow
     * @param sWorkflowId The string representing the workflow
     * @param sUserId The user id to identify the user
     * @return An integer, reporting the count of the deleted items 
     */
    public int deleteByWorkflowIdUserId(String sWorkflowId,String sUserId ) {
    	if (Utils.isNullOrEmpty(sWorkflowId) || Utils.isNullOrEmpty(sUserId)) return 0;
    	
        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(Filters.and(Filters.eq("userId", sUserId), Filters.eq("workflowId", sWorkflowId)));

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
    
    
    /**
     * Delete all the sharings of a specific Workflow
     * @param sWorkflowId
     * @return
     */
    public int deleteByWorkflowId(String sWorkflowId) {
    	
    	if (Utils.isNullOrEmpty(sWorkflowId)) return 0;

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("workflowId", sWorkflowId));

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
     * Delete all the sharings with User
     * @param sUserId
     * @return
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
     * Delete a specific Sharing of this workflow with this user
     * @param sUserId
     * @param sWorkflowId
     * @return
     */
    public int deleteByUserIdWorkflowId(String sUserId, String sWorkflowId) {
    	
    	if (Utils.isNullOrEmpty(sWorkflowId)) return 0;
    	if (Utils.isNullOrEmpty(sUserId)) return 0;
    	
        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(Filters.and(Filters.eq("userId", sUserId), Filters.eq("workflowId", sWorkflowId)));

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
    
    /**
     * Checks if workflow is shared with user
     * @param sUserId
     * @param sWorkflowId
     * @return
     */
    public boolean isSharedWithUser(String sUserId, String sWorkflowId) {
    	return (getWorkflowSharingByUserIdWorkflowId(sUserId, sWorkflowId ) != null);
    }
    
    /**
     * Returns the WorkflowSharing for the user and workflow
     * @param sUserId 
     * @param sWorkflowId 
     * @return
     */
	public WorkflowSharing getWorkflowSharingByUserIdWorkflowId(String sUserId, String sWorkflowId) {
		 	
		try {
		Document oWSDocument = getCollection(m_sThisCollection).find(
				Filters.and(
						Filters.eq("userId", sUserId),
						Filters.eq("workflowId", sWorkflowId)
						)
		).first();
		if(null!=oWSDocument) {
			String sJSON = oWSDocument.toJson();
			WorkflowSharing oWorkflowSharing;
		    try {
                oWorkflowSharing = s_oMapper.readValue(sJSON,WorkflowSharing.class);
                return oWorkflowSharing;
            } catch (IOException e) {
                e.printStackTrace();
            }
		   
		}
		
	} catch (Exception oE) {
		Utils.debugLog("WorkflowSharingRepository.getWorkflowSharingByUserIdWorkflowId( " + sUserId + ", " + sWorkflowId + "): error: " + oE);
	}
	return null;	
	}
}
	
