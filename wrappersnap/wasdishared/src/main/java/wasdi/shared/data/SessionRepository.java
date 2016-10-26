package wasdi.shared.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;

import java.util.Date;

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

            String sJSON = oSessionDocument.toJson();

            UserSession oUserSession = s_oMapper.readValue(sJSON,UserSession.class);

            return oUserSession;
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
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
            getCollection("sessions").deleteOne(new Document("sessionId", oSession.getSessionId()));
            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
}
