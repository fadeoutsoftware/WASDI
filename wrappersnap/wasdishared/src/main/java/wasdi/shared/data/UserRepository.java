package wasdi.shared.data;

import org.bson.Document;

import com.mongodb.client.result.DeleteResult;

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
    
    public User GoogleLogin(String sUserId, String sEmail, String sAuthProvider) {
        try {
            User oUser = GetUser(sUserId);

            if (oUser != null)
            {
            	 if (oUser.getEmail() != null  && oUser.getAuthServiceProvider() != null)
                 {
                     if ( oUser.getEmail().equals(sEmail) && oUser.getAuthServiceProvider().equals(sAuthProvider) )
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
}
