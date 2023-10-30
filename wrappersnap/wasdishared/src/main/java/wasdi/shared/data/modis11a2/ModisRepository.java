package wasdi.shared.data.modis11a2;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import wasdi.shared.business.modis11a2.ModisItemForReading;
import wasdi.shared.business.modis11a2.ModisItemForWriting;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class ModisRepository extends MongoRepository  {
	
	public ModisRepository() {
		m_sThisCollection = "catalog"; 
		m_sRepoDb = "modis"; 
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
							

			} catch (Exception oEx) {
				WasdiLog.errorLog("ModisRepository.insertModisItem: Exception when trying to insert the document in the db" + oEx);
			}
			
		}
		
	}
	

	
	public long countItems(Double dWest, Double dNorth, Double dEast, Double dSouth, Long lDateFrom, Long lDateTo, String sFileName) {
		
		String sQuery = wasdiQueryToMongo(dWest, dNorth, dEast, dSouth, lDateFrom, lDateTo, sFileName);
		
		try {	
			
			long lCount = getCollection(m_sThisCollection).countDocuments(Document.parse(sQuery));

			return lCount;
			
		} catch (Exception oEx) {
			WasdiLog.debugLog("ModisRepository.countItems: Exception when counting documents from db" + oEx);
		}

		return -1;
	}
	
	
	private String wasdiQueryToMongo(Double dWest, Double dNorth, Double dEast, Double dSouth, Long lDateFrom, Long lDateTo, String sFileName) {		
		
		List<String> asQueryFilters = new ArrayList<>();

		// coordinates: WN, EN, ES, WS, WN
		String sCoordinates = "[ "
				+ "[" + dWest + ", " + dNorth + "], "
				+ "[" + dEast + ", " + dNorth + "], "
				+ "[" + dEast + ", " + dSouth + "] , "
				+ "[" + dWest + ", " + dSouth + "], "
				+ "[" + dWest + ", " + dNorth + "] "
				+ "]";

		boolean bValidCoordinates = dWest != null && dNorth != null && dEast != null && dSouth != null; 
		
		if (bValidCoordinates) {
		asQueryFilters.add(  
				"     boundingBox: {\r\n" + 
				"       $geoIntersects: {\r\n" + 
				"          $geometry: {\r\n" + 
				"             type: \"Polygon\" ,\r\n" + 
				"             coordinates: [\r\n" + 
								sCoordinates + 
				"             ]\r\n" + 
				"          }\r\n" + 
				"       }\r\n" + 
				"     }\r\n"
				);
		}



		if (lDateFrom != null) {
			asQueryFilters.add("startDate: {$gte: " + lDateFrom + "}");
		}

		if (lDateTo != null) {
			asQueryFilters.add("endDate: {$lte: " + lDateTo + "}");
		}
		
		if (!Utils.isNullOrEmpty(sFileName)) {
			asQueryFilters.add("fileName: \"" + sFileName + "\"");
		}

		String sQuery = "   { " + String.join(", ", asQueryFilters) + "   }";
		
		
		return sQuery;
	}
	
	

	public List<ModisItemForReading> getModisItemList(Double dWest, Double dNorth, Double dEast, Double dSouth, 
			Long lDateFrom, Long lDateTo, int iOffset, int iLimit, String sFileName) {

		final List<ModisItemForReading> aoReturnList = new ArrayList<>();
		
		String sQuery = wasdiQueryToMongo(dWest, dNorth, dEast, dSouth, lDateFrom, lDateTo, sFileName);
		
		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Document.parse(sQuery)).skip(iOffset).limit(iLimit);

			fillList(aoReturnList, oWSDocuments, ModisItemForReading.class);


		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("ModisRepository.getModisItemList: error", oEx);
		}

		return aoReturnList;
	}
	
	public long countDocumentsMatchingFileName(String sFileName) {
			
		try {
			Document oQuery = new Document("fileName", sFileName);
			
			return getCollection(m_sThisCollection).countDocuments(oQuery);
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("ModisRepository.countDocumentsMatchingFileName: Exception when counting documents from db" + oEx);
		}
		
		return -1;
		
	}
	
	
	public static void main(String[]args) throws Exception {
		ModisRepository oNewRepo = new ModisRepository();
		
		System.out.println(oNewRepo.countItems(null, null, null, null, 950832000000L, 951523199000L, null));
		
		System.out.println(oNewRepo.countItems(128d, -9d, 130d, -20d, 950832000000L, 951896424000L, null));
		
		System.out.println("950832000000L: " + TimeEpochUtils.fromEpochToDateString(950832000000L));
		System.out.println("951523199000L: " + TimeEpochUtils.fromEpochToDateString(951523199000L));
		System.out.println("951896424000L: " + TimeEpochUtils.fromEpochToDateString(951896424000L));
		
	}
	

}
