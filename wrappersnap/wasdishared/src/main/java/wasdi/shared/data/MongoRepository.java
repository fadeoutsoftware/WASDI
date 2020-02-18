package wasdi.shared.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import wasdi.shared.utils.Utils;

import org.bson.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Base Repository Class
 * Created by p.campanella on 21/10/2016.
 */
public class MongoRepository {
	/**
	 * Db Name
	 */
    public static String DB_NAME = "wasdi";
    /**
     * Server Address
     */
    public static String SERVER_ADDRESS = "127.0.0.1";
    /**
     * Server Port
     */
    public static int SERVER_PORT = 27017;
    /**
     * Db User
     */
    public static String DB_USER = "user";
    /**
     * Db Password
     */
    public static String DB_PWD = "password";

    /**
     * Object Mapper
     */
    public static ObjectMapper s_oMapper = new ObjectMapper();

    static  {
        s_oMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Mongo Client
     */
    private static MongoClient s_oMongoClient = null;
    /**
     * Mongo Database
     */
    private static  MongoDatabase s_oMongoDatabase = null;

    /**
     * Get The database Object
     * @return
     */
    public static MongoDatabase getMongoDatabase() {
    	try 
    	{
    		if (s_oMongoClient == null) {

            	MongoCredential oCredential = MongoCredential.createCredential(DB_USER, DB_NAME, DB_PWD.toCharArray());
            	s_oMongoClient = new MongoClient(new ServerAddress(SERVER_ADDRESS, SERVER_PORT), Arrays.asList(oCredential));
            	s_oMongoDatabase = s_oMongoClient.getDatabase(DB_NAME);
        	}
    	}
    	catch (Exception e) 
    	{
    		Utils.debugLog("MongoRepository.getMongoDatabase: exception " + e.getMessage());
			e.printStackTrace();
    	}
        return s_oMongoDatabase;
    }

    /**
     * Get a named collection
     * @param sCollection
     * @return
     */
    public MongoCollection<Document> getCollection(String sCollection) {
        return getMongoDatabase().getCollection(sCollection);
    }
    
    /**
     * Shut down the connection
     */
    public static void shutDownConnection() {
    	if (s_oMongoClient != null) {
    		try {
    			s_oMongoClient.close();
    		}
    		catch (Exception e) {
				Utils.debugLog("MongoRepository.shutDownConnection: exception " + e.getMessage());
				e.printStackTrace();
			}
    	}
    }
    
    public <T> void fillList(final ArrayList<T> aoReturnList, FindIterable<Document> oWSDocuments, String sRepositoryName,Class<T> oClass) {
		oWSDocuments.forEach(new Block<Document>() {
		    public void apply(Document document) {
		        String sJSON = document.toJson();
		        T oAppCategory= null;
		        try {
		        	oAppCategory = s_oMapper.readValue(sJSON, oClass);
		            aoReturnList.add(oAppCategory);
		        } catch (IOException oEx) {
		        	Utils.debugLog(sRepositoryName + ".fillList: " + oEx);
		        }

		    }
		});
	}
}
