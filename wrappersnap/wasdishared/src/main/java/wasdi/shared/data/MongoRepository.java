package wasdi.shared.data;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class MongoRepository {
    public static String DB_NAME = "wasdi";
    public static String SERVER_ADDRESS = "localhost";
    public static int SERVER_PORT = 27017;

    private static MongoClient s_oMongoClient = null;
    private static  MongoDatabase s_oMongoDatabase = null;

    public static MongoDatabase getMongoDatabase() {
        if (s_oMongoClient == null) {
            s_oMongoClient = new MongoClient(SERVER_ADDRESS, SERVER_PORT);
            s_oMongoDatabase = s_oMongoClient.getDatabase(DB_NAME);
        }

        return s_oMongoDatabase;
    }

    public MongoCollection<Document> getCollection(String sCollection) {
        return getMongoDatabase().getCollection(sCollection);
    }
}
