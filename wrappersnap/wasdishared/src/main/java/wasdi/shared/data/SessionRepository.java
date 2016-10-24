package wasdi.shared.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class SessionRepository extends MongoRepository {

    public boolean InsertSession(UserSession oSession) {
        try {
            ObjectMapper oMapper = new ObjectMapper();
            String sJSON = oMapper.writeValueAsString(oSession);
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

            ObjectMapper oMapper = new ObjectMapper();
            oMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            UserSession oUserSession = oMapper.readValue(sJSON,UserSession.class);

            return oUserSession;
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }

    public boolean DeleteSession(UserSession oSession) {
        try {
            ObjectMapper oMapper = new ObjectMapper();
            getCollection("sessions").deleteOne(new Document("sessionId", oSession.getSessionId()));

            return true;
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
}
