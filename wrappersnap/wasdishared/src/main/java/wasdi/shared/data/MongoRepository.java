package wasdi.shared.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

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
     * Replica Set Name, if Any
     */
    public static String REPLICA_NAME = "WASDI_PROD_RS";
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
     * Flag to notify with a log if the repository has no local connection and 
     * reverts to the default wasdi connection
     */
    private static boolean s_bDbSwitchLogged = false;
    
    public static void readConfig() {
        MongoRepository.SERVER_ADDRESS = WasdiConfig.Current.mongoMain.address;
        MongoRepository.DB_NAME = WasdiConfig.Current.mongoMain.dbName;
        MongoRepository.DB_USER = WasdiConfig.Current.mongoMain.user;
        MongoRepository.DB_PWD = WasdiConfig.Current.mongoMain.password;
        MongoRepository.REPLICA_NAME = WasdiConfig.Current.mongoMain.replicaName;
    }
    
	/**
     * Add a new Mongo Connection
     * @param sDbCode Code of the connection
     * @param sUser User 
     * @param sPassword Pw
     * @param sAddress Address 
     * @param iServerPort Port
     * @param sDbName db name
     */
    public static void addMongoConnection(String sDbCode, String sUser, String sPassword, String sAddress, String sReplicaName, String sDbName) {
    	try 
    	{
    		// Check if this does not exists yet
    		MongoConnection oMongoConnection = s_aoDbConnections.get(sDbCode);
    		
    		if (oMongoConnection == null) {
    			
    			// Create the connection config
    			oMongoConnection = new MongoConnection();
    			
    			String []asAddressParts = sAddress.split(",");
    			boolean bReplica = false;
    			
    			if (asAddressParts != null) {
    				if (asAddressParts.length>1) {
    					bReplica = true;
    				}
    			}
            	
            	String sConnectionString = "mongodb://" + sUser+":"+sPassword+"@"+sAddress+"/?authSource=" + sDbName;
            	
            	if (bReplica) {
            		sConnectionString = sConnectionString + "&replicaSet="+sReplicaName;
            	}
            	
            	oMongoConnection.m_oMongoClient = MongoClients.create(sConnectionString);
            	oMongoConnection.m_oMongoDatabase = oMongoConnection.m_oMongoClient.getDatabase(sDbName);
            	
            	// Add it to the dictionary
            	s_aoDbConnections.put(sDbCode, oMongoConnection);
            	
            	WasdiLog.debugLog("MongoRepository.addMongoConnection: Configuration added: " + sDbCode);
    		}
    	}
    	catch (Exception e) 
    	{
    		WasdiLog.errorLog("MongoRepository.addMongoConnection: exception " + e.getMessage());
    	}    	
    }
    
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
    				addMongoConnection("wasdi", MongoRepository.DB_USER, MongoRepository.DB_PWD, MongoRepository.SERVER_ADDRESS, MongoRepository.REPLICA_NAME, MongoRepository.DB_NAME);
    				return getMongoDatabase("wasdi");
    			}
    			else {
    				
    				if (!s_bDbSwitchLogged) {
    					WasdiLog.debugLog("MongoRepository.getMongoDatabase: Db Code " + sDbCode + " NOT FOUND. try to recover with default db");
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
    		WasdiLog.errorLog("MongoRepository.getMongoDatabase: exception " + e.getMessage());
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
    				WasdiLog.errorLog("MongoRepository.shutDownConnection: exception " + e.getMessage());
    			}
			}
    	}
    	catch (Exception e) {
			WasdiLog.errorLog("MongoRepository.shutDownConnection: exception " + e.getMessage());
		}    	
    }
    
	/**
	 * Set the repo db
	 * @param sRepoDb
	 */
	public void setRepoDb(String sRepoDb) {	
			this.m_sRepoDb = sRepoDb;
	}
    
    
    /**
     * Fill a list of entities from the result of a Query.
     * @param <T> Type of the output entity
     * @param aoReturnList List of T-elements to fill
     * @param oMongoDocuments List of the mongo documents returned by the query
     * @param oClass Class object of Type T
     */
    public <T> void fillList(List<T> aoReturnList, MongoIterable<Document> oMongoDocuments, Class<T> oClass) {
    	
        MongoCursor<Document> oCursor = oMongoDocuments.iterator();
        
        try {
        	while (oCursor.hasNext()) {
                String sJSON = oCursor.next().toJson();
                
                T oEntity = null;
                try {
                	oEntity = s_oMapper.readValue(sJSON,oClass);
                    aoReturnList.add(oEntity);
                } catch (Exception oEx) {
                	WasdiLog.errorLog(m_sThisCollection + ".fillList: " + oEx);
                }            		
        	}
        }
        finally {
			oCursor.close();
		}
	}
    
    /**
     * Fill a list of entities from the result of a Query.
     * @param <T> Type of the output entity
     * @param aoReturnList Set of T-elements to fill
     * @param oMongoDocuments List of the mongo documents returned by the query
     * @param oClass Class object of Type T
     */
    public <T> void fillList(Set<T> aoReturnList, FindIterable<Document> oMongoDocuments, Class<T> oClass) {
    	
        MongoCursor<Document> oCursor = oMongoDocuments.iterator();
        
        try {
        	while (oCursor.hasNext()) {
                String sJSON = oCursor.next().toJson();
                
                T oEntity = null;
                try {
                	oEntity = s_oMapper.readValue(sJSON,oClass);
                    aoReturnList.add(oEntity);
                } catch (Exception oEx) {
                	WasdiLog.errorLog(m_sThisCollection + ".fillList: " + oEx);
                }            		
        	}
        }
        finally {
			oCursor.close();
		}
	}
    
    /**
     * Add a new document to the default repo collection
     * @param oNewDocument Object to add
     * @return Mongo Object id (_id)
     */
    protected String add(Object oNewDocument) {
		return add(oNewDocument, m_sThisCollection, "MongoRepository.add");
	}
    
    /**
     * Add a new document to a collection
     * @param oNewDocument Object to add
     * @param sCollection Name of the collection
     * @return Mongo Object id (_id)
     */
	protected String add(Object oNewDocument, String sCollection) {
		return add(oNewDocument, sCollection, "MongoRepository.add");
	}
    
    /**
     * Adds a New Document to a collection
     * @param oNewDocument Object to add
     * @param sCollection Name of the collection
     * @param sRepositoryCommand Text prefix to log exceptions
     * @return Mongo Object id (_id)
     */
	protected String add(Object oNewDocument, String sCollection, String sRepositoryCommand) {
		String sResult = "";
		if(oNewDocument != null) {
			try {
				String sJSON = s_oMapper.writeValueAsString(oNewDocument);
				Document oDocument = Document.parse(sJSON);
				getCollection(sCollection).insertOne(oDocument);
				sResult = oDocument.getObjectId("_id").toHexString();
	
			} catch (Exception oEx) {
				String sText = "";
				
				if (Utils.isNullOrEmpty(sRepositoryCommand)) {
					sText = oEx.toString();
				}
				else {
					sText = sRepositoryCommand + ": " + oEx.toString();
				}
				WasdiLog.errorLog(sText);
			}
		}
		return sResult;
	}

	/**
	 * Deletes an object that match the criteria in the repo default collection
	 * @param oCriteria Mongo db Criteria
	 * @return number of elements deleted
	 */
	protected int delete(BasicDBObject oCriteria){
		return delete(oCriteria,m_sThisCollection);
	}
	
	/**
	 * Deletes an object that match the criteria in Collection
	 * @param oCriteria Mongo db Criteria
	 * @param sCollectionName Name of the collection
	 * @return number of elements deleted
	 */
	protected int delete(BasicDBObject oCriteria, String sCollectionName ){
        try {
            DeleteResult oDeleteResult = getCollection(sCollectionName).deleteOne(oCriteria);
            if (oDeleteResult != null)
            {
                return (int) oDeleteResult.getDeletedCount();
            }
        } catch (Exception oEx) {
        	WasdiLog.errorLog("MongoRepository.delete: excetpion " + oEx.toString());
        }

        return 0;
	}
	
	/**
	 * Delete a set of objects matching the criteria in the repo default collection
	 * @param oCriteria Mongo db Criteria
	 * @return number of elements deleted
	 */	
	protected int deleteMany(BasicDBObject oCriteria){
		return deleteMany(oCriteria, m_sThisCollection);
	}
	
	/**
	 * Delete a set of objects matching the criteria in Collection
	 * @param oCriteria Mongo db Criteria
	 * @param sCollectionName Name of the collection
	 * @return number of elements deleted
	 */
	protected int deleteMany(BasicDBObject oCriteria, String sCollectionName){
        try {
            DeleteResult oDeleteResult = getCollection(sCollectionName).deleteMany(oCriteria);
            if (oDeleteResult != null)
            {
                return (int) oDeleteResult.getDeletedCount();
            }
        } catch (Exception oEx) {
        	WasdiLog.errorLog("MongoRepository.deleteMany: excetpion " + oEx.toString());
        }

        return 0;
	}
	
	/**
	 * Update an object matching the criteria in the repo default collection collection
	 * @param oCriteria Criteria to find the object to update
	 * @param oNewDocument Updated document
	 * @return True if updated, false otherwise
	 */
	protected boolean update(BasicDBObject oCriteria, Object oNewDocument) {
		return update(oCriteria,oNewDocument,m_sThisCollection);
	}
		
	/**
	 * Update an object matching the criteria in collection
	 * @param oCriteria Criteria to find the object to update
	 * @param oNewDocument Updated document
	 * @param sCollectionName Name of the collection
	 * @return True if updated, false otherwise
	 */
	protected boolean update(BasicDBObject oCriteria, Object oNewDocument, String sCollectionName) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oNewDocument);
            
            Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));
            
            UpdateResult oResult = getCollection(sCollectionName).updateOne(oCriteria, oUpdateOperationDocument);
            
            if (oResult.getModifiedCount()==1) return true;
        }
        catch (Exception oEx) {
        	WasdiLog.errorLog("MongoRepository.update: excetpion " + oEx.toString());
        }

        return  false;
    }
	
}
