package wasdi.shared.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import wasdi.shared.business.Review;
import wasdi.shared.utils.Utils;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.File;
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
    
	public <T> String add(Object oNewDocument, String sCollection, String sRepositoryCommand) {
		String sResult = "";
		if(oNewDocument != null) {
			try {
				String sJSON = s_oMapper.writeValueAsString(oNewDocument);
				Document oDocument = Document.parse(sJSON);
				getCollection(sCollection).insertOne(oDocument);
				sResult = oDocument.getObjectId("_id").toHexString();
	
			} catch (Exception oEx) {
				Utils.debugLog(sRepositoryCommand + ": " + oEx);
			}
		}
		return sResult;
	}
	
	
	public int delete(BasicDBObject oCriteria, String sCollectionName ){

        try {

            DeleteResult oDeleteResult = getCollection(sCollectionName).deleteOne(oCriteria);

            if (oDeleteResult != null)
            {
                return (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
	}
	
	public boolean update(BasicDBObject oCriteria, Object oNewDocument, String sCollectionName) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oNewDocument);
            
            Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));
            
            UpdateResult oResult = getCollection(sCollectionName).updateOne(oCriteria, oUpdateOperationDocument);
            
            if (oResult.getModifiedCount()==1) return true;
        }
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  false;
    }
	
//	public void uploadImage (File oImageFile){
//		
//		GridFS gfsPhoto = new GridFS( s_oMongoClient.getDB(dbName), "userphotos");
//
//		String newFileName = "userimage";
//		try {
//			GridFSInputFile gfsFile = gfsPhoto.createFile(oImageFile);
//			gfsFile.setFilename(newFileName);
//			gfsFile.save();
//
//		} catch (IOException e) {
//			Utils.debugLog("MongoRepository.uploadImage:" + e);
//			e.printStackTrace();
//		}
//	}
//	public void getImage (){
//		GridFS gfsPhoto = new GridFS((DB) s_oMongoDatabase, "photo");
//		String newFileName = "userimage";
//		GridFSDBFile imageForOutput = gfsPhoto.findOne(newFileName);
//		// save it into a new image file
//		try {
//			imageForOutput.writeTo("C:\\temp\\wasdi\\data\\alessio");
//		} catch (IOException e) {
//			Utils.debugLog("MongoRepository.getImage:" + e);
//			e.printStackTrace();
//		}
//	}
	
}
