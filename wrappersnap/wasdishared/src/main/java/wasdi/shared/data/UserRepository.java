package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.User;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class UserRepository extends  MongoRepository{

    public boolean InsertUser(User oUser) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oUser);
            getCollection("users").insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }


    public User GetUser(String sUserId) {

        try {
            Document oUserDocument = getCollection("users").find(new Document("userId", sUserId)).first();
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
    
    //TODO check: remove?
    //method never used
    public User Login(String sUserId, String sPassword) {
        try {
            User oUser = GetUser(sUserId);

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
    
    //TODO check: can we get rid of sEmail? @sergin13 @kr1zz
    public User GoogleLogin(String sGoogleIdToken, String sEmail, String sAuthProvider) {
        try {
            User oUser = GetUser(sEmail);

            if (oUser != null){
            	if ( oUser.getUserId() != null &&
            			oUser.getAuthServiceProvider() != null &&
            			null != oUser.getGoogleIdToken() ) {
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
    
    public boolean DeleteUser(String sUserId) {

        try {

            DeleteResult oDeleteResult = getCollection("users").deleteOne(new Document("userId", sUserId));

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
   
    public boolean UpdateUser (User oUser)
    {
    	String sJSON;
		try 
		{
			sJSON = s_oMapper.writeValueAsString(oUser);
			Bson oFilter = new Document("userId", oUser.getUserId());
		    Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));
            UpdateResult oResult = getCollection("users").updateOne(oFilter, oUpdateOperationDocument);
            if (oResult.getModifiedCount()==1) return  true;

		} 
		catch (JsonProcessingException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 return  false;
    }
    
    
    public ArrayList<User> getAllUsers ()
    {
    	FindIterable<Document> oDocuments = getCollection("users").find();
        final ArrayList<User> aoReturnList = new ArrayList<User>();

    	oDocuments.forEach(new Block<Document>() 
    	{
    		public void apply(Document document) 
    		{
    			String sJSON = document.toJson();
    			User oUser = null;
    			try {
    				oUser = s_oMapper.readValue(sJSON,User.class);
    				aoReturnList.add(oUser);
    			} catch (IOException e) {
    				e.printStackTrace();
    			}

    		}
          });
    	
    	return aoReturnList;
    }
    
    public void UpdateAllUsers (ArrayList<User> aoUsers)
    {
    	try
    	{
    		for (int i = 0; i < aoUsers.size(); i++) 
    		{
    			User oUser = aoUsers.get(i);	
    			UpdateUser(oUser);
    		}
    	}
    	catch(Exception oEx)
    	{
    		oEx.printStackTrace();
    	}
 	
    }

}
