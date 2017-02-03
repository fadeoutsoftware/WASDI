package wasdi.shared.data;

import com.mongodb.Block;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import org.bson.Document;
import org.bson.types.ObjectId;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.ProductWorkspace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by s.adamo on 31/01/2017.
 */
public class ProcessWorkspaceRepository extends MongoRepository {

    public String InsertProcessWorkspace(ProcessWorkspace oProcessWorkspace) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oProcessWorkspace);
            Document oDocument = Document.parse(sJSON);
            getCollection("processworkpsace").insertOne(oDocument);
            return oDocument.getObjectId("_id").toHexString();

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return "";
    }

    public boolean DeleteProcessWorkspace(String sId) {

        try {
            getCollection("processworkpsace").deleteOne(new Document("_id", new ObjectId(sId)));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }


    public List<ProcessWorkspace> GetProcessByWorkspace(String sWorkspaceId) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(new Document("workspaceId", sWorkspaceId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    ProcessWorkspace oProcessWorkspace = null;
                    try {
                        oProcessWorkspace = s_oMapper.readValue(sJSON,ProcessWorkspace.class);
                        aoReturnList.add(oProcessWorkspace);
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
}
