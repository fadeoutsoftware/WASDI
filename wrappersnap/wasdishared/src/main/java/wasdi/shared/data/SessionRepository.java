package wasdi.shared.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.business.Workspace;
import wasdi.shared.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class SessionRepository extends MongoRepository {

    public boolean InsertSession(UserSession oSession) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oSession);
            getCollection("sessions").insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    public UserSession GetSession(String sSessionId) {
        try {
            Document oSessionDocument = getCollection("sessions").find(new Document("sessionId", sSessionId)).first();

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

    public List<UserSession> GetAllActiveSessions(String sUserId) {
        final ArrayList<UserSession> aoReturnList = new ArrayList<>();
        try {
            long lNow = new Date().getTime();
            FindIterable<Document> oWSDocuments = getCollection("sessions").find(Filters.and(Filters.gte("lastTouch", lNow - 24*60*60*1000), Filters.eq("userId", sUserId)));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    UserSession oUserSession = null;
                    try {
                        oUserSession = s_oMapper.readValue(sJSON, UserSession.class);
                        aoReturnList.add(oUserSession);
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

    public List<UserSession> GetAllExpiredSessions(String sUserId) {
        final ArrayList<UserSession> aoReturnList = new ArrayList<>();
        try {
            long lNow = new Date().getTime();
            FindIterable<Document> oWSDocuments = getCollection("sessions").find(Filters.and(Filters.lt("lastTouch", lNow - 24*60*60*1000), Filters.eq("userId", sUserId)));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    UserSession oUserSession = null;
                    try {
                        oUserSession = s_oMapper.readValue(sJSON, UserSession.class);
                        aoReturnList.add(oUserSession);
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

    public boolean TouchSession(UserSession oSession) {
        try {
            UpdateResult oResult = getCollection("sessions").updateOne(Filters.eq("sessionId",oSession.getSessionId()), Updates.set("lastTouch", (double)new Date().getTime()));

            if (oResult.getModifiedCount()==1) return  true;
        }
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  false;
    }

    public boolean DeleteSession(UserSession oSession) {
        try {
            if (oSession == null || Utils.isNullOrEmpty(oSession.getSessionId()))
                return true;
            getCollection("sessions").deleteOne(new Document("sessionId", oSession.getSessionId()));
            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
}
