package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.business.WorkflowSharing;
import wasdi.shared.utils.Utils;

public class SnapWorkflowRepository extends  MongoRepository {
	
	public SnapWorkflowRepository() {
		m_sThisCollection = "snapworkflows";
	}
	
	/**
	 * Insert a new workflow
	 * @param oWorkflow
	 * @return
	 */
    public boolean insertSnapWorkflow(SnapWorkflow oWorkflow) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oWorkflow);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    /**
     * Get a workflow by Id
     * @param sWorkflowId
     * @return
     */
    public SnapWorkflow getSnapWorkflow(String sWorkflowId) {

        try {
            Document oWSDocument = getCollection(m_sThisCollection).find(new Document("workflowId", sWorkflowId)).first();

            String sJSON = oWSDocument.toJson();

            SnapWorkflow oWorkflow = s_oMapper.readValue(sJSON,SnapWorkflow.class);

            return oWorkflow;
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }

    /**
     * Get all the workflows that can be accessed by UserId
     * @param sUserId
     * @return List of private workflows of users plus all the public ones plus the the ones shared with the user
     */
    public List<SnapWorkflow> getSnapWorkflowPublicAndByUser(String sUserId) {

        final ArrayList<SnapWorkflow> aoReturnList = new ArrayList<SnapWorkflow>();
        try {
        	// find sharings by userId
        	WorkflowSharingRepository oWorkflowSharingRepository = new WorkflowSharingRepository();
        	List<WorkflowSharing> aoWorkflowSharing = oWorkflowSharingRepository.getWorkflowSharingByUser(sUserId);
        	
        	// fetch snapworflows using the ids in the sharings 
        	// append the to return list 
        	Bson oOrFilter = Filters.or(new Document("userId", sUserId),new Document("isPublic", true));
        	
            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oOrFilter);

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    SnapWorkflow oWorkflow = null;
                    try {
                        oWorkflow = s_oMapper.readValue(sJSON,SnapWorkflow.class);
                        aoReturnList.add(oWorkflow);
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
     * Get the list of all workflows
     * @return
     */
    public List<SnapWorkflow> getList() {

        final ArrayList<SnapWorkflow> aoReturnList = new ArrayList<SnapWorkflow>();
        
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    SnapWorkflow oWorkflow = null;
                    try {
                        oWorkflow = s_oMapper.readValue(sJSON,SnapWorkflow.class);
                        aoReturnList.add(oWorkflow);
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
     * Update a Workflow
     * @param oSnapWorkflow
     * @return
     */
    public boolean updateSnapWorkflow(SnapWorkflow oSnapWorkflow) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oSnapWorkflow);
            Document filter = new Document("workflowId", oSnapWorkflow.getWorkflowId());
			Document update = new Document("$set", new Document(Document.parse(sJSON)));
			getCollection(m_sThisCollection).updateOne(filter, update);

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    /**
     * Deletes a workflow
     */
    public boolean deleteSnapWorkflow(String sWorkflowId) {
    	
    	if (Utils.isNullOrEmpty(sWorkflowId)) return false;

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("workflowId", sWorkflowId));

            if (oDeleteResult != null)
            {
                if (oDeleteResult.getDeletedCount() == 1 )
                {
                    return  true;
                }
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  false;
    }
    
    /**
     * Delete all the workflows of User
     * @param sUserId
     * @return
     */
    public int deleteSnapWorkflowByUser(String sUserId) {
    	
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
}
