package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class UserRepository extends  MongoRepository{
	
	public UserRepository() {
		m_sThisCollection = "users";
	}
	
	/**
	 * Insert a new User
	 * @param oUser
	 * @return
	 */
    public boolean insertUser(User oUser) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oUser);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
        	WasdiLog.errorLog("UserRepository.insertUser : error ", oEx);
        }

        return false;
    }

    /**
     * Get a user
     * @param sUserId
     * @return
     */
    public User getUser(String sUserId) {

        try {
            Document oUserDocument = getCollection(m_sThisCollection).find(new Document("userId", sUserId)).first();
            if(oUserDocument == null)
            {
            	return null;
            }
            String sJSON = oUserDocument.toJson();

            User oUser = s_oMapper.readValue(sJSON,User.class);

            return oUser;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("UserRepository.getUser : error ", oEx);
        }

        return  null;
    }
    
    /**
     * Check login credentials
     * @param sUserId
     * @param sPassword
     * @return
     */
    public User login(String sUserId, String sPassword) {
        try {
            User oUser = getUser(sUserId);

            if (oUser != null)
            {
                if (oUser.getPassword() != null)
                {
                    if (oUser.getPassword().equals(sPassword))
                    {
                        return oUser;
                    }
                }
            }

            return null;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("UserRepository.login : error ", oEx);
        }

        return  null;
    }
    
    /**
     * Verify login of a google user
     * @param sGoogleIdToken
     * @param sEmail
     * @param sAuthProvider
     * @return
     */
    public User googleLogin(String sGoogleIdToken, String sEmail, String sAuthProvider) {
        try {
            User oUser = getUser(sEmail);

            if (oUser != null){
            	if ( oUser.getUserId() != null && oUser.getAuthServiceProvider() != null && null != oUser.getGoogleIdToken() ) {
            		if(oUser.getGoogleIdToken().equals(sGoogleIdToken)) {
            			if ( oUser.getAuthServiceProvider().equals(sAuthProvider)   ) {
            				return oUser;
            			}
            		}
            	}
            }
            return null;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("UserRepository.googleLogin : error ", oEx);
        }

        return  null;
    }
    
    /**
     * Delete a user
     * @param sUserId
     * @return
     */
    public boolean deleteUser(String sUserId) {
    	
    	if (Utils.isNullOrEmpty(sUserId)) return false;

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("userId", sUserId));

            if (oDeleteResult != null)
            {
                if (oDeleteResult.getDeletedCount() == 1 )
                {
                    return  true;
                }
            }

        } catch (Exception oEx) {
        	WasdiLog.errorLog("UserRepository.deleteUser : error ", oEx);
        }

        return  false;
    }
    
    /**
     * Update a user
     * @param oUser
     * @return
     */
    public boolean updateUser(User oUser)
    {
    	String sJSON;
		try 
		{
			sJSON = s_oMapper.writeValueAsString(oUser);
			Bson oFilter = new Document("userId", oUser.getUserId());
		    Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));
            UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);
            if (oResult.getModifiedCount()==1) return  true;

		} 
		catch (Exception oEx) 
		{
			WasdiLog.errorLog("UserRepository.updateUser : error ", oEx);
		}
		 return  false;
    }
    
    /**
     * Get the list of all users
     * @return
     */
    public ArrayList<User> getAllUsers ()
    {
    	FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();
        final ArrayList<User> aoReturnList = new ArrayList<User>();
        
        fillList(aoReturnList, oWSDocuments, User.class);
    	
    	return aoReturnList;
    }
    
    
    /**
     * Get the list of all users
     * @return
     */
    public ArrayList<User> getAdminUsers ()
    {
    	FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("role", UserApplicationRole.ADMIN.name()));
        final ArrayList<User> aoReturnList = new ArrayList<User>();
        
        fillList(aoReturnList, oWSDocuments, User.class);
    	
    	return aoReturnList;
    }
    
    /**
     * Get a sorted list of users
     * @param sOrderBy db field on which to perform the sorting operation
     * @param iOrder decreasing or increasing order
     * @return
     */
    public ArrayList<User> getAllUsersSorted(String sOrderBy, int iOrder) {
    	
    	ArrayList<User> aoReturnList = new ArrayList<User>();
    	
    	try {
	    	FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
	    			.find()
	    			.sort(new Document(sOrderBy, iOrder));
	        
	        fillList(aoReturnList, oWSDocuments, User.class);
    	} catch(Exception oEx) {
    		WasdiLog.errorLog("UserRepository.getAllUsersSorted. Error retrieving users", oEx);
    	}
    	
    	return aoReturnList;  	
    }
    
    /**
     * Get a list of users matching a partial name
     * @param sPartialName
     * @return
     */
	public List<User> findUsersByPartialName(String sPartialName) {
		return findUsersByPartialName(sPartialName, "userId", 1);
	}
    
    public List<User> findUsersByPartialName(String sPartialName, String sOrderBy, int iOrder) {
		List<User> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) && !Utils.isNullOrEmpty(sOrderBy)) {
			return getAllUsersSorted(sOrderBy, iOrder);
		} else if (Utils.isNullOrEmpty(sPartialName) && Utils.isNullOrEmpty(sOrderBy)) {
			return getAllUsers();
		}

		Pattern oRegex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);

		Bson oFilterLikeUserId = Filters.eq("userId", oRegex);
		Bson oFilterLikeName = Filters.eq("name", oRegex);
		Bson oFilterLikeSurname = Filters.eq("surname", oRegex);

		Bson oFilter = Filters.or(oFilterLikeUserId, oFilterLikeName, oFilterLikeSurname);

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(oFilter)
					.sort(new Document(sOrderBy, iOrder));
			
			

			fillList(aoReturnList, oWSDocuments, User.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("UserRepository.getAllUsers : error ", oEx);
		}

		return aoReturnList;
    }    

	public long getUsersCount() {
		return getCollection(m_sThisCollection).countDocuments();
	}


}
