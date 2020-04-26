package wasdi.shared.data;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Workspace;
import wasdi.shared.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

/**
 * Created by p.campanella on 25/10/2016.
 */
public class WorkspaceRepository extends  MongoRepository {
	
	public WorkspaceRepository() {
		m_sThisCollection = "workspaces";
	}
	
	/**
	 * Insert a new Workspace
	 * @param oWorkspace
	 * @return
	 */
    public boolean insertWorkspace(Workspace oWorkspace) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oWorkspace);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    /**
     * Update a workpsace
     * @param oWorkspace
     * @return
     */
    public boolean updateWorkspace(Workspace oWorkspace) {

        try {
            getCollection(m_sThisCollection).updateOne(eq("workspaceId", oWorkspace.getWorkspaceId()), new Document("$set", new Document("name",oWorkspace.getName())));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    /**
     * Get a workspace by Id
     * @param sWorkspaceId
     * @return
     */
    public Workspace getWorkspace(String sWorkspaceId) {

        try {
            Document oWSDocument = getCollection(m_sThisCollection).find(new Document("workspaceId", sWorkspaceId)).first();
            
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

    /**
     * Get all the workspaces of a user
     * @param sUserId
     * @return
     */
    public List<Workspace> getWorkspaceByUser(String sUserId) {

        final ArrayList<Workspace> aoReturnList = new ArrayList<Workspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId));

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
    
    /**
     * Delete a workspace by Id
     * @param sWorkspaceId
     * @return
     */
    public boolean deleteWorkspace(String sWorkspaceId) {

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("workspaceId", sWorkspaceId));

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
     * Delete all the workspaces of User
     * @param sUserId
     * @return
     */
    public int deleteByUser(String sUserId) {

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
     * Check if User is the owner of Workspace
     * @param sUserId
     * @param sWorkspaceId
     * @return
     */
    public boolean isOwnedByUser(String sUserId, String sWorkspaceId) {
    	try {
	    	Document oWSDocument = getCollection(m_sThisCollection).find(
	        		Filters.and(
	        				Filters.eq("userId", sUserId),
	        				Filters.eq("workspaceId", sWorkspaceId)
	        				)
	        		).first();
	    	if(null!=oWSDocument) {
	    		return true;
	    	}
    	}catch (Exception oE) {
			Utils.debugLog("WorkspaceRepository.belongsToUser( " + sUserId + ", " + sWorkspaceId + " ): error: " + oE);
		}
    	return false;
    }
    
    /**
     * Fill a list of Workpaces Entites
     * @param aoReturnList
     * @param oWSDocuments
     */
	private void fillList(final ArrayList<Workspace> aoReturnList, FindIterable<Document> oWSDocuments) {
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
	}
    
}
