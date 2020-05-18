package wasdi.shared.data;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

/**
 * Represents the connection to a MongoDb
 * @author p.campanella
 *
 */
public class MongoConnection {
    /**
     * Mongo Client
     */
    public MongoClient m_oMongoClient = null;
    /**
     * Mongo Database
     */
    public MongoDatabase m_oMongoDatabase = null;
}
