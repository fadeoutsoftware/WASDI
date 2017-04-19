package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.WorkspaceSharing;

/**
 * Created by p.campanella on 25/10/2016.
 */
public class WorkspaceSharingRepository extends  MongoRepository{


    public boolean InsertWorkspaceSharing(WorkspaceSharing oWorkspaceSharing) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oWorkspaceSharing);
            getCollection("workspacessharing").insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    public List<WorkspaceSharing> GetWorkspaceSharingByOwner(String sUserId) {

        final ArrayList<WorkspaceSharing> aoReturnList = new ArrayList<WorkspaceSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("workspacessharing").find(new Document("ownerId", sUserId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    WorkspaceSharing oWorkspaceSharing = null;
                    try {
                        oWorkspaceSharing = s_oMapper.readValue(sJSON,WorkspaceSharing.class);
                        aoReturnList.add(oWorkspaceSharing);
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


    public List<WorkspaceSharing> GetWorkspaceSharingByWorkspace(String sWorkspaceId) {

        final ArrayList<WorkspaceSharing> aoReturnList = new ArrayList<WorkspaceSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("workspacessharing").find(new Document("workspaceId", sWorkspaceId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    WorkspaceSharing oWorkspaceSharing = null;
                    try {
                        oWorkspaceSharing = s_oMapper.readValue(sJSON,WorkspaceSharing.class);
                        aoReturnList.add(oWorkspaceSharing);
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

    public int DeleteByWorkspaceId(String sWorkspaceId) {

        try {

            DeleteResult oDeleteResult = getCollection("workspacessharing").deleteMany(new Document("wokspaceId", sWorkspaceId));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }

    public int DeleteByUserId(String sUserId) {

        try {

            DeleteResult oDeleteResult = getCollection("workspacessharing").deleteMany(new Document("userId", sUserId));

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
