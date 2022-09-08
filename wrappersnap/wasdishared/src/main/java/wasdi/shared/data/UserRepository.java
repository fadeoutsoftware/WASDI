package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.User;
import wasdi.shared.utils.Utils;

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
            oEx.printStackTrace();
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
            oEx.printStackTrace();
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
            oEx.printStackTrace();
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
            oEx.printStackTrace();
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
            oEx.printStackTrace();
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
		catch (JsonProcessingException e) 
		{
			e.printStackTrace();
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

	public List<User> findUsersByPartialName(String sPartialName) {
		List<User> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoReturnList;
		}

		Pattern regex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);

		Bson oFilterLikeUserId = Filters.eq("userId", regex);
		Bson oFilterLikeName = Filters.eq("name", regex);
		Bson oFilterLikeSurname = Filters.eq("surname", regex);

		Bson oFilter = Filters.or(oFilterLikeUserId, oFilterLikeName, oFilterLikeSurname);

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(oFilter)
					.sort(new Document("userId", 1));

			fillList(aoReturnList, oWSDocuments, User.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
    }
    
    /**
     * Update a list of users
     * @param aoUsers
     */
    public void updateAllUsers(ArrayList<User> aoUsers)
    {
    	try
    	{
    		for (int i = 0; i < aoUsers.size(); i++) 
    		{
    			User oUser = aoUsers.get(i);	
    			updateUser(oUser);
    		}
    	}
    	catch(Exception oEx)
    	{
    		oEx.printStackTrace();
    	}
 	
    }

	public long countUsers() {
		return getCollection(m_sThisCollection).countDocuments();
		
	}


}
