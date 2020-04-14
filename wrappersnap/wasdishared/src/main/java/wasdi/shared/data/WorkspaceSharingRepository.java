package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.WorkspaceSharing;
import wasdi.shared.utils.Utils;

/**
 * Created by p.campanella on 25/10/2016.
 */
public class WorkspaceSharingRepository extends  MongoRepository{


    public boolean insertWorkspaceSharing(WorkspaceSharing oWorkspaceSharing) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oWorkspaceSharing);
            getCollection("workspacessharing").insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    public List<WorkspaceSharing> getWorkspaceSharingByOwner(String sUserId) {

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
    
    public List<WorkspaceSharing> getWorkspaceSharingByUser(String sUserId) {

        final ArrayList<WorkspaceSharing> aoReturnList = new ArrayList<WorkspaceSharing>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("workspacessharing").find(new Document("userId", sUserId));

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


    public List<WorkspaceSharing> getWorkspaceSharingByWorkspace(String sWorkspaceId) {

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

    public int deleteByWorkspaceId(String sWorkspaceId) {

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

    public int deleteByUserId(String sUserId) {

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
    
    public int deleteByUserIdWorkspaceId(String sUserId, String sWorkspaceId) {
        try {

            DeleteResult oDeleteResult = getCollection("workspacessharing").deleteMany(Filters.and(Filters.eq("userId", sUserId), Filters.eq("workspaceId", sWorkspaceId)));

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
    
    public boolean isSharedWithUser(String sUserId, String sWorkspaceId) {
    	try {
    		Document oWSDocument = getCollection("workspacessharing").find(
    				Filters.and(
    						Filters.eq("userId", sUserId),
    						Filters.eq("workspaceId", sWorkspaceId)
    						)
    		).first();
    		if(null!=oWSDocument) {
    			return true;
    		}
    		
    	} catch (Exception oE) {
			Utils.debugLog("WorkspaceSharingRepository.isSharedWithUser( " + sUserId + ", " + sWorkspaceId + "): error: " + oE);
		}
    	return false;
    }
}
