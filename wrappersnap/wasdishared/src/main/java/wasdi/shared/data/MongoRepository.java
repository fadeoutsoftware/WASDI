package wasdi.shared.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Arrays;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class MongoRepository {
    public static String DB_NAME = "wasdi";
    public static String SERVER_ADDRESS = "127.0.0.1";
    public static int SERVER_PORT = 27017;
    public static String DB_USER = "***REMOVED***";
    public static String DB_PWD = "***REMOVED***";

    public static ObjectMapper s_oMapper = new ObjectMapper();

    static  {
        s_oMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static MongoClient s_oMongoClient = null;
    private static  MongoDatabase s_oMongoDatabase = null;

    public static MongoDatabase getMongoDatabase() {
        if (s_oMongoClient == null) {

            MongoCredential oCredential = MongoCredential.createCredential(DB_USER, DB_NAME, DB_PWD.toCharArray());
            s_oMongoClient = new MongoClient(new ServerAddress(SERVER_ADDRESS, SERVER_PORT), Arrays.asList(oCredential));
            s_oMongoDatabase = s_oMongoClient.getDatabase(DB_NAME);
        }

        return s_oMongoDatabase;
    }

    public MongoCollection<Document> getCollection(String sCollection) {
        return getMongoDatabase().getCollection(sCollection);
    }
}
