package wasdi.shared.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import wasdi.shared.utils.Utils;

import org.bson.Document;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

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
     * Static Connections Dictionary
     */
    private static HashMap<String, MongoConnection> s_aoDbConnections = new HashMap<>();
    
    /**
     * Name of the collection related to this Repository
     */
    protected String m_sThisCollection;
    
    /**
     * Name of the database connected to this repo
     */
    protected String m_sRepoDb = "wasdi";
    
	/**
     * Add a new Mongo Connection
     * @param sDbCode Code of the connection
     * @param sUser User 
     * @param sPassword Pw
     * @param sAddress Address 
     * @param iServerPort Port
     * @param sDbName db name
     */
    public static void addMongoConnection(String sDbCode, String sUser, String sPassword, String sAddress, int iServerPort, String sDbName) {
    	try 
    	{
    		// Check if this does not exists yet
    		MongoConnection oMongoConnection = s_aoDbConnections.get(sDbCode);
    		
    		if (oMongoConnection == null) {
    			
    			// Create the connection config
    			oMongoConnection = new MongoConnection();
            	MongoCredential oCredential = MongoCredential.createCredential(sUser, sDbName, sPassword.toCharArray());
            	oMongoConnection.m_oMongoClient = new MongoClient(new ServerAddress(sAddress, iServerPort), Arrays.asList(oCredential));
            	oMongoConnection.m_oMongoDatabase = oMongoConnection.m_oMongoClient.getDatabase(sDbName);
            	
            	// Add it to the dictionary
            	s_aoDbConnections.put(sDbCode, oMongoConnection);
            	
            	Utils.debugLog("MongoRepository.addMongoConnection: Configuration added: " + sDbCode);
    		}
    	}
    	catch (Exception e) 
    	{
    		Utils.debugLog("MongoRepository.addMongoConnection: exception " + e.getMessage());
			e.printStackTrace();
    	}    	
    }
    
    private static boolean s_bDbSwitchLogged = false;
    
    /**
     * Get The database Object
     * @return
     */
    public static MongoDatabase getMongoDatabase(String sDbCode) {
    	try 
    	{
    		// Get the connection config from the dictionary
    		MongoConnection oMongoConnection = s_aoDbConnections.get(sDbCode);
    		
    		// Check if exists
    		if (oMongoConnection == null) {
    			// If is the default wasdi and does not exist
    			if (sDbCode.equals("wasdi")) {
    				// Create default connection
        			oMongoConnection = new MongoConnection();
                	MongoCredential oCredential = MongoCredential.createCredential(DB_USER, DB_NAME, DB_PWD.toCharArray());
                	oMongoConnection.m_oMongoClient = new MongoClient(new ServerAddress(SERVER_ADDRESS, SERVER_PORT), Arrays.asList(oCredential));
                	oMongoConnection.m_oMongoDatabase = oMongoConnection.m_oMongoClient.getDatabase(DB_NAME);
                	// Add it to the dictionary
                	s_aoDbConnections.put("wasdi", oMongoConnection);
    			}
    			else {
    				
    				if (!s_bDbSwitchLogged) {
    					Utils.debugLog("MongoRepository.getMongoDatabase: Db Code " + sDbCode + " NOT FOUND. try to recover with default db");
    					s_bDbSwitchLogged = true;
    				}
    				
    				
    				// If is not the default one, but does not exists, recover with the defualt
    				return getMongoDatabase("wasdi");
    			}
    		}
    		
    		// Return the database
    		return oMongoConnection.m_oMongoDatabase;
    	}
    	catch (Exception e) 
    	{
    		Utils.debugLog("MongoRepository.getMongoDatabase: exception " + e.getMessage());
			e.printStackTrace();
    	}
        return null;
    }

    /**
     * Get a named collection
     * @param sCollection
     * @return
     */
    public MongoCollection<Document> getCollection(String sCollection) {
    	MongoDatabase oMongoDb = getMongoDatabase(m_sRepoDb);
    	
    	if (oMongoDb != null) {
    		return oMongoDb.getCollection(sCollection);
    	}
    	else {
    		return null;
    	}
    }
    
    /**
     * Shut down the connection
     */
    public static void shutDownConnection() {
    	
    	try {
    		
    		Collection<MongoConnection> aoConnections = s_aoDbConnections.values();
    		
    		for (MongoConnection oConnection : aoConnections) {
        		try {
        			oConnection.m_oMongoClient.close();
        		}
        		catch (Exception e) {
    				Utils.debugLog("MongoRepository.shutDownConnection: exception " + e.getMessage());
    				e.printStackTrace();
    			}
			}
    	}
    	catch (Exception e) {
			Utils.debugLog("MongoRepository.shutDownConnection: exception " + e.getMessage());
			e.printStackTrace();
		}    	
    }
    
    /**
     * Get the repo db of this Repository
     * @return
     */
    public String getRepoDb() {
		return m_sRepoDb;
	}

    /**
     * Set the repo db of this Repository
     * @param sRepoDb
     */
	public void setRepoDb(String sRepoDb) {
		this.m_sRepoDb = sRepoDb;
	}
    
}
