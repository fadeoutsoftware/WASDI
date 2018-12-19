package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.SnapWorkflow;

public class SnapWorkflowRepository extends  MongoRepository {
	
    public boolean InsertSnapWorkflow(SnapWorkflow oWorkflow) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oWorkflow);
            getCollection("snapworkflows").insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    public SnapWorkflow GetSnapWorkflow(String sWorkflowId) {

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


    public List<SnapWorkflow> GetSnapWorkflowPublicAndByUser(String sUserId) {

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

    public boolean DeleteSnapWorkflow(String sWorkflowId) {

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

    public int DeleteSnapWorkflowByUser(String sUserId) {

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
