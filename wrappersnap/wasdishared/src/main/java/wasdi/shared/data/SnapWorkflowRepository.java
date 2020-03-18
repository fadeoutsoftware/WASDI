package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.utils.Utils;

public class SnapWorkflowRepository extends  MongoRepository {
	
    public boolean insertSnapWorkflow(SnapWorkflow oWorkflow) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oWorkflow);
            getCollection("snapworkflows").insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    public SnapWorkflow getSnapWorkflow(String sWorkflowId) {

        try {
            Document oWSDocument = getCollection("snapworkflows").find(new Document("workflowId", sWorkflowId)).first();

            String sJSON = oWSDocument.toJson();

            SnapWorkflow oWorkflow = s_oMapper.readValue(sJSON,SnapWorkflow.class);

            return oWorkflow;
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }


    public List<SnapWorkflow> getSnapWorkflowPublicAndByUser(String sUserId) {

        final ArrayList<SnapWorkflow> aoReturnList = new ArrayList<SnapWorkflow>();
        try {

        	Bson oOrFilter = Filters.or(new Document("userId", sUserId),new Document("isPublic", true));
        	
            FindIterable<Document> oWSDocuments = getCollection("snapworkflows").find(oOrFilter);

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
    
    public List<SnapWorkflow> getList() {

        final ArrayList<SnapWorkflow> aoReturnList = new ArrayList<SnapWorkflow>();
        
        try {

            FindIterable<Document> oWSDocuments = getCollection("snapworkflows").find();

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
    
    public boolean updateSnapWorkflow(SnapWorkflow oSnapWorkflow) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oSnapWorkflow);
            Document filter = new Document("workflowId", oSnapWorkflow.getWorkflowId());
			Document update = new Document("$set", new Document(Document.parse(sJSON)));
			getCollection("snapworkflows").updateOne(filter, update);

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    

    public boolean deleteSnapWorkflow(String sWorkflowId) {

        try {

            DeleteResult oDeleteResult = getCollection("snapworkflows").deleteOne(new Document("workflowId", sWorkflowId));

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

    public int deleteSnapWorkflowByUser(String sUserId) {

        try {

            DeleteResult oDeleteResult = getCollection("snapworkflows").deleteMany(new Document("userId", sUserId));

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
