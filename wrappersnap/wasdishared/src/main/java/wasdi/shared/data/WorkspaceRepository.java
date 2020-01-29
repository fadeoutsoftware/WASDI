package wasdi.shared.data;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import wasdi.shared.business.Workspace;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

/**
 * Created by p.campanella on 25/10/2016.
 */
public class WorkspaceRepository extends  MongoRepository{

    public boolean insertWorkspace(Workspace oWorkspace) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oWorkspace);
            getCollection("workspaces").insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    public boolean updateWorkspace(Workspace oWorkspace) {

        try {
            getCollection("workspaces").updateOne(eq("workspaceId", oWorkspace.getWorkspaceId()), new Document("$set", new Document("name",oWorkspace.getName())));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }


    public Workspace getWorkspace(String sWorkspaceId) {

        try {
            Document oWSDocument = getCollection("workspaces").find(new Document("workspaceId", sWorkspaceId)).first();
            
            if (oWSDocument != null) {
                String sJSON = oWSDocument.toJson();

                Workspace oWorkspace = s_oMapper.readValue(sJSON,Workspace.class);

                return oWorkspace;            	
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }


    public List<Workspace> getWorkspaceByUser(String sUserId) {

        final ArrayList<Workspace> aoReturnList = new ArrayList<Workspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("workspaces").find(new Document("userId", sUserId));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    Workspace oWorkspace = null;
                    try {
                        oWorkspace = s_oMapper.readValue(sJSON,Workspace.class);
                        aoReturnList.add(oWorkspace);
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

    public boolean deleteWorkspace(String sWorkspaceId) {

        try {

            DeleteResult oDeleteResult = getCollection("workspaces").deleteOne(new Document("workspaceId", sWorkspaceId));

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

    public int deleteByUser(String sUserId) {

        try {

            DeleteResult oDeleteResult = getCollection("workspaces").deleteMany(new Document("userId", sUserId));

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
