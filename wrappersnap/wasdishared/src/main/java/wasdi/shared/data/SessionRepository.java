package wasdi.shared.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.UserSession;
import wasdi.shared.utils.Utils;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class SessionRepository extends MongoRepository {
	
	public SessionRepository() {
		m_sThisCollection = "sessions";
	}
	
	/**
	 * Create a new session
	 * @param oSession a valid WASDI session
	 * @return true if insert was successful, false otherwise
	 */
    public boolean insertSession(UserSession oSession) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oSession);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    
    
    /**
     * Creates a new unique session for the given user
     * @param sUserId a valid wasdi user id
     * @return a UserSession if it could be created successfully, null otherwise
     */
    public UserSession createUniqueSession(String sUserId) {
    	if(Utils.isNullOrEmpty(sUserId)) {
    		return null;
    	}
    	UserSession oSession = null;
    	String sSessionId = "";
		do {
			sSessionId = UUID.randomUUID().toString();
			oSession = getSession(sSessionId);
		}while(null!=oSession);
		
		oSession = new UserSession();
		oSession.setSessionId(sSessionId);
		oSession.setUserId(sUserId);
		oSession.setLoginDate(Utils.nowInMillis());
		oSession.setLastTouch(Utils.nowInMillis());
    	
    	return oSession;
    }
    
    
    /**
     * Creates a new session for the given user, and inserts it in the DB
     * @param sUserId a valid WASDI user
     * @return true if the creation and insert succeded, false otherwise
     */
    public UserSession insertUniqueSession(String sUserId) {
    	try {
	    	UserSession oSession = createUniqueSession(sUserId);
	    	if(insertSession(oSession)) {
	    		return oSession;
	    	}
    	} catch (Exception oE) {
			oE.printStackTrace();
		}
    	return null;
    }
    
    
    /**
     * Get a session by Id
     * @param sSessionId
     * @return
     */
    public UserSession getSession(String sSessionId) {
        try {
            Document oSessionDocument = getCollection(m_sThisCollection).find(new Document("sessionId", sSessionId)).first();

            if (oSessionDocument != null) {
                String sJSON = oSessionDocument.toJson();

                UserSession oUserSession = s_oMapper.readValue(sJSON, UserSession.class);
                return oUserSession;
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }
    
    /**
     * Get all the active sessions of a user
     * @param sUserId
     * @return
     */
    public List<UserSession> getAllActiveSessions(String sUserId) {
        final ArrayList<UserSession> aoReturnList = new ArrayList<>();
        try {
            long lNow = new Date().getTime();
            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.and(Filters.gte("lastTouch", lNow - 24*60*60*1000), Filters.eq("userId", sUserId)));

            fillList(aoReturnList, oWSDocuments, UserSession.class);
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get all the expired sessions of a user
     * @param sUserId
     * @return
     */
    public List<UserSession> getAllExpiredSessions(String sUserId) {
        final ArrayList<UserSession> aoReturnList = new ArrayList<>();
        try {
            long lNow = new Date().getTime();
            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.and(Filters.lt("lastTouch", lNow - 24*60*60*1000), Filters.eq("userId", sUserId)));
            
            fillList(aoReturnList, oWSDocuments, UserSession.class);
            
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Refresh a session
     * @param oSession
     * @return
     */
    public boolean touchSession(UserSession oSession) {
        try {
            UpdateResult oResult = getCollection(m_sThisCollection).updateOne(Filters.eq("sessionId",oSession.getSessionId()), Updates.set("lastTouch", (double)new Date().getTime()));

            if (oResult.getModifiedCount()==1) return  true;
        }
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  false;
    }
    
    /**
     * Delete a Session
     * @param oSession
     * @return
     */
    public boolean deleteSession(UserSession oSession) {
    	
    	if (oSession == null) return false;
    	
        try {
            if (oSession == null || Utils.isNullOrEmpty(oSession.getSessionId()))
                return true;
            getCollection(m_sThisCollection).deleteOne(new Document("sessionId", oSession.getSessionId()));
            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    //public String createSession()
}
