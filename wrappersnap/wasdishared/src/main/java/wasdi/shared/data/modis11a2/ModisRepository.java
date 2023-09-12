package wasdi.shared.data.modis11a2;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import wasdi.shared.business.modis11a2.ModisItemForReading;
import wasdi.shared.business.modis11a2.ModisItemForWriting;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.log.WasdiLog;

public class ModisRepository extends MongoRepository  {
	
	String connectionString;
	String databaseName;
	String collectionName;
	
	public ModisRepository() {
		m_sThisCollection = "catalog"; 
		m_sRepoDb = "modis"; 
//		connectionString = "mongodb://localhost:27017";
//		databaseName = "testDb";
//		collectionName = "testModis";
	}
	
	public void insertModisItem(ModisItemForWriting oNewDocument) {
		
		if(oNewDocument != null) {
			try {
				String sJSON = "{ \"fileName\": \"" + oNewDocument.getSFileName() + "\",";

				sJSON += "\"fileSize\": " + oNewDocument.getLFileSize() + ", ";
				sJSON += "\"dayNightFlag\": \"" + oNewDocument.getSDayNightFlag() + "\",";

				sJSON += "\"startDate\": " + oNewDocument.getDStartDate() + ",";
				sJSON += "\"endDate\": " + oNewDocument.getDEndDate() + ",";

				sJSON += "\"platform\": \"" + oNewDocument.getSPlatform() + "\",";
				sJSON += "\"instrument\": \"" + oNewDocument.getSInstrument() + "\",";
				sJSON += "\"sensor\": \"" + oNewDocument.getSSensor() + "\",";
				sJSON += "\"url\": \"" + oNewDocument.getSUrl() + "\",";
				sJSON += "\"boundingBox\":" + oNewDocument.getSBoundingBox();

				sJSON += "}";

				Document oDocument = Document.parse(sJSON);
				getCollection(m_sThisCollection).insertOne(oDocument);
				String sResult = oDocument.getObjectId("_id").toHexString();
				WasdiLog.debugLog("ModisRepository.insertModisItem. Result id: " + sResult);
				
				
//		        try (MongoClient mongoClient = MongoClients.create(connectionString)) { 
//		            // Get a reference to the database
//		            MongoDatabase database = mongoClient.getDatabase(databaseName);
//
//		            // Get a reference to the collection
//		            MongoCollection<Document> collection = database.getCollection(collectionName);
//
//
//		            // Insert the document into the collection
//		            collection.insertOne(oDocument);
//
//		            WasdiLog.debugLog("ModisRepository.countItems: Document inserted successfully!");
//		        } catch (Exception oEx) {
//					WasdiLog.errorLog("ModisRepository.insertModisItem: Exception when connecting to the db" + oEx);
//		        }			

			} catch (Exception oEx) {
				WasdiLog.errorLog("ModisRepository.insertModisItem: Exception when trying to insert the document in the db" + oEx);
			}
			
		}
		
	}
	

	
	public long countItems(Double dWest, Double dNorth, Double dEast, Double dSouth, Long lDateFrom, Long lDateTo) {
		
		String sQuery = wasdiQueryToMongo(dWest, dNorth, dEast, dSouth, lDateFrom, lDateTo);

		System.out.println(sQuery);

		try {
            
			long lCount = getCollection(m_sThisCollection).countDocuments(Document.parse(sQuery));

			return lCount;
			
		} catch (Exception oEx) {
			WasdiLog.debugLog("ModisRepository.countItems: Exception when counting documents from db" + oEx);
		}

		return -1;
	}
	
	
	private String wasdiQueryToMongo(Double dWest, Double dNorth, Double dEast, Double dSouth, Long lDateFrom, Long lDateTo) {
		// coordinates: WN, EN, ES, WS, WN

		String sCoordinates = "[ "
				+ "[" + dWest + ", " + dNorth + "], "
				+ "[" + dEast + ", " + dNorth + "], "
				+ "[" + dEast + ", " + dSouth + "] , "
				+ "[" + dWest + ", " + dSouth + "], "
				+ "[" + dWest + ", " + dNorth + "] "
				+ "]";

		System.out.println(sCoordinates);

		String sQuery = 
				"   {\r\n" + 
				"     boundingBox: {\r\n" + 
				"       $geoIntersects: {\r\n" + 
				"          $geometry: {\r\n" + 
				"             type: \"Polygon\" ,\r\n" + 
				"             coordinates: [\r\n" + 
				sCoordinates + 
				"             ]\r\n" + 
				"          }\r\n" + 
				"       }\r\n" + 
				"     }\r\n";



		if (lDateFrom != null) {
			sQuery += ", startDate: {$gte: " + lDateFrom + "}";
		}

		if (lDateTo != null) {
			sQuery += ", endDate: {$lte: " + lDateTo + "}";
		}


		sQuery += "   }";

		return sQuery;
	}
	
	
	/**
	 * Get all the Modis Items
	 * @return the full list of items
	 */
	public List<ModisItemForReading> getModisItemList(Double dWest, Double dNorth, Double dEast, Double dSouth, Long lDateFrom, Long lDateTo, int iOffset, int iLimit) {

		final List<ModisItemForReading> aoReturnList = new ArrayList<>();
		
		String sQuery = wasdiQueryToMongo(dWest, dNorth, dEast, dSouth, lDateFrom, lDateTo);

		System.out.println(sQuery);
		
		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Document.parse(sQuery)).skip(iOffset).limit(iLimit);

			fillList(aoReturnList, oWSDocuments, ModisItemForReading.class);


		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}
	

}
