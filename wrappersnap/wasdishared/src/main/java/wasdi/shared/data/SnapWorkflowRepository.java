package wasdi.shared.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class SnapWorkflowRepository extends MongoRepository {

    public SnapWorkflowRepository() {
        m_sThisCollection = "snapworkflows";
    }

    /**
     * Insert a new workflow
     *
     * @param oWorkflow
     * @return
     */
    public boolean insertSnapWorkflow(SnapWorkflow oWorkflow) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oWorkflow);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
        	WasdiLog.errorLog("SnapWorkflowRepository.insertSnapWorkflow : error ", oEx);
        }

        return false;
    }

    /**
     * Get a workflow by Id
     *
     * @param sWorkflowId
     * @return
     */
    public SnapWorkflow getSnapWorkflow(String sWorkflowId) {

        try {
            Document oWSDocument = getCollection(m_sThisCollection).find(new Document("workflowId", sWorkflowId)).first();

            String sJSON = oWSDocument.toJson();

            SnapWorkflow oWorkflow = s_oMapper.readValue(sJSON, SnapWorkflow.class);

            return oWorkflow;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("SnapWorkflowRepository.getSnapWorkflow : error ", oEx);
        }

        return null;
    }

    /**
     * Get all the workflow that can be accessed by UserId
     *
     * @param sUserId
     * @return List of private workflow of users plus all the public ones
     */
    public List<SnapWorkflow> getSnapWorkflowPublicAndByUser(String sUserId) {
        // migrated to set in order to avoid redundancy
        final HashSet<SnapWorkflow> aoReturnList = new HashSet<SnapWorkflow>();
        try {
            // Then search all the other workflow using UserId of the current user
            // - OR -
            // the public ones
            Bson oOrFilter = Filters.or(new Document("userId", sUserId), new Document("isPublic", true));

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oOrFilter);
            
            fillList(aoReturnList, oWSDocuments, SnapWorkflow.class);

        } catch (Exception oEx) {
        	WasdiLog.errorLog("SnapWorkflowRepository.getSnapWorkflowPublicAndByUser : error ", oEx);
        }

        return new ArrayList<SnapWorkflow>(aoReturnList);
    }

    /**
     * Get the list of all workflows
     *
     * @return
     */
    public List<SnapWorkflow> getList() {

        final ArrayList<SnapWorkflow> aoReturnList = new ArrayList<SnapWorkflow>();

        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();
            
            fillList(aoReturnList, oWSDocuments, SnapWorkflow.class);
            
        } catch (Exception oEx) {
        	WasdiLog.errorLog("SnapWorkflowRepository.getList : error ", oEx);
        }

        return aoReturnList;
    }

    
    /**
     * Find a workflow by partial name, by partial description or by partial id
     * @return the list of workflows that partially match the name, the description or the id
     */
    public List<SnapWorkflow> findWorkflowByPartialName(String sPartialName) {

        final ArrayList<SnapWorkflow> aoReturnList = new ArrayList<SnapWorkflow>();
        
        if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
        	return null;
        }
        
		Pattern regex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);
		
		Bson oFilterLikeWorkflowId= Filters.eq("workflowId", regex);
		Bson oFilterLikeName = Filters.eq("name", regex);
		Bson oFilterLikeDescription = Filters.eq("description", regex);
		
		Bson oFilter = Filters.or(oFilterLikeWorkflowId, oFilterLikeName, oFilterLikeDescription);

        try {

            FindIterable<Document> oRetrievedDocuments = getCollection(m_sThisCollection)
            		.find(oFilter)
            		.sort(new Document("name", 1));
            
            fillList(aoReturnList, oRetrievedDocuments, SnapWorkflow.class);
            
        } catch (Exception oEx) {
        	WasdiLog.errorLog("SnapWorkflowRepository.findWorkflowByPartialName : error ", oEx);
        }

        return aoReturnList;
    }
    
    
    /**
     * Update a Workflow
     *
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
        	WasdiLog.errorLog("SnapWorkflowRepository.updateSnapWorkflow : error ", oEx);
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

            if (oDeleteResult != null) {
                if (oDeleteResult.getDeletedCount() == 1) {
                    return true;
                }
            }

        } catch (Exception oEx) {
        	WasdiLog.errorLog("SnapWorkflowRepository.deleteSnapWorkflow : error ", oEx);
        }

        return false;
    }

    /**
     * Delete all the workflows of User
     *
     * @param sUserId
     * @return
     */
    public int deleteSnapWorkflowByUser(String sUserId) {

        if (Utils.isNullOrEmpty(sUserId)) return 0;

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("userId", sUserId));

            if (oDeleteResult != null) {
                return (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
        	WasdiLog.errorLog("SnapWorkflowRepository.deleteSnapWorkflowByUser : error ", oEx);
        }

        return 0;
    }
}
