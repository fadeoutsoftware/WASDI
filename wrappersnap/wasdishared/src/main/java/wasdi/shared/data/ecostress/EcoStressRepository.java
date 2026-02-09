package wasdi.shared.data.ecostress;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import wasdi.shared.business.ecostress.EcoStressItemForReading;
import wasdi.shared.business.ecostress.EcoStressItemForWriting;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class EcoStressRepository extends MongoRepository {

	public EcoStressRepository() {
		m_sThisCollection = "eeh2_catalog";
		m_sRepoDb = "ecostress";
	}

	public void insertEcoStressItem(EcoStressItemForWriting oNewDocument) {
		if(oNewDocument != null) {
			try {
				String sJSON = "{ \"fileName\": \""+oNewDocument.getFileName()+"\",";

				sJSON += "\"dayNightFlag\": \""+oNewDocument.getDayNightFlag()+"\",";

				sJSON += "\"beginningDate\": "+oNewDocument.getBeginningDate()+",";
				sJSON += "\"endingDate\": "+oNewDocument.getEndingDate()+",";

				sJSON += "\"s3Path\": \""+oNewDocument.getS3Path()+"\",";
				sJSON += "\"url\": \""+oNewDocument.getUrl()+"\",";
				sJSON += "\"location\":" + oNewDocument.getLocation();

				sJSON += "}";

				Document oDocument = Document.parse(sJSON);
				getCollection(m_sThisCollection).insertOne(oDocument);
				String sResult = oDocument.getObjectId("_id").toHexString();

				WasdiLog.debugLog("EcoStressRepository | sResult: " + sResult);
			} catch (Exception oEx) {
				WasdiLog.debugLog("EcoStressRepository: " + oEx);
			}
		}
	}

	/**
	 * Get all the EcoStress Items
	 * @return the full list of items
	 */
	public List<EcoStressItemForReading> getEcoStressItemList() {

		final List<EcoStressItemForReading> aoReturnList = new ArrayList<>();

		try {
			
			BasicDBObject oSort = new BasicDBObject("beginningDate", 1);
			
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find().sort(oSort);

			fillList(aoReturnList, oWSDocuments, EcoStressItemForReading.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("EcoStressRepository.getEcoStressItemList: error", oEx);
		}

		return aoReturnList;
	}
	
	private String wasdiQueryToMongo(Double dWest, Double dNorth, Double dEast, Double dSouth, String sService,
			Long lDateFrom, Long lDateTo, String sDayNightFlag) {
		String sCoordinates = "[ [" +dWest + ", " + dNorth + "], [" + dWest +", " + dSouth + "], [" + dEast + ", " + dSouth + "] , [" +  dEast + ", " + dNorth + "], [" +dWest + ", " + dNorth + "] ]";

		String sFilter = 
				"   {\r\n" + 
				"     location: {\r\n" + 
				"       $geoIntersects: {\r\n" + 
				"          $geometry: {\r\n" + 
				"             type: \"Polygon\" ,\r\n" + 
				"             coordinates: [\r\n" + 
				sCoordinates + 
				"             ]\r\n" + 
				"          }\r\n" + 
				"       }\r\n" + 
				"     }\r\n";

		if (!Utils.isNullOrEmpty(sService)) {
			sFilter += ", s3Path: {$regex : \"" + sService + "\"}";
		}

		if (lDateFrom != null) {
			sFilter += ", beginningDate: {$gte: " + lDateFrom + "}";
		}

		if (lDateTo != null) {
			sFilter += ", endingDate: {$lte: " + lDateTo + "}";
		}

		if (!Utils.isNullOrEmpty(sDayNightFlag)) {
			sFilter += ", dayNightFlag: {$eq : \"" + sDayNightFlag + "\"}";
		}

		sFilter += "   }" + 
				"";
		
		return sFilter;
	}
	
	private String wasdiQueryToMongoByName(String sName, Double dWest, Double dNorth, Double dEast, Double dSouth, String sService,
			Long lDateFrom, Long lDateTo, String sDayNightFlag) {
		
		String sQuery = "";
		
		if (Utils.isNullOrEmpty(sName)) {
			WasdiLog.warnLog("EcoStressRepository.wasdiQueryToMongoByName. Can not retrieve a product by name if the name is not specified");
			return null;
		}
		
		// if the file name is provided without extension, we try to add it
		if (!sName.endsWith(".h5") ) {
			sName += ".h5";
		}
		
		sQuery += "{ fileName: {$eq : \"" + sName + "\"}";
		
		String sCoordinates = "";
		
		if (!(Utils.isNullOrEmpty(dSouth) 
				|| Utils.isNullOrEmpty(dEast) 
				|| Utils.isNullOrEmpty(dNorth) 
				|| Utils.isNullOrEmpty(dWest))) {
			sCoordinates = "[ [" +dWest + ", " + dNorth + "], [" + dWest +", " + dSouth + "], [" + dEast + ", " + dSouth + "] , [" +  dEast + ", " + dNorth + "], [" +dWest + ", " + dNorth + "] ]";

			sQuery += ",";
			sQuery += 
					"     location: {\r\n" + 
					"       $geoIntersects: {\r\n" + 
					"          $geometry: {\r\n" + 
					"             type: \"Polygon\" ,\r\n" + 
					"             coordinates: [\r\n" + 
					sCoordinates + 
					"             ]\r\n" + 
					"          }\r\n" + 
					"       }\r\n" + 
					"     }\r\n";
		}
		

		if (!Utils.isNullOrEmpty(sService)) {
			sQuery += ", s3Path: {$regex : \"" + sService + "\"}";
		}

		if (lDateFrom != null) {
			sQuery += ", beginningDate: {$gte: " + lDateFrom + "}";
		}

		if (lDateTo != null) {
			sQuery += ", endingDate: {$lte: " + lDateTo + "}";
		}

		if (!Utils.isNullOrEmpty(sDayNightFlag)) {
			sQuery += ", dayNightFlag: {$eq : \"" + sDayNightFlag + "\"}";
		}

		sQuery += "   }" + 
				"";

		return sQuery;
	}
	
	
	/**
	 * Get all the EcoStress Items
	 * @return the full list of items
	 */
	public List<EcoStressItemForReading> getEcoStressItemByName(String sFileName, Double dWest, Double dNorth, Double dEast, Double dSouth, String sService,
			Long lDateFrom, Long lDateTo, String sDayNightFlag) {

		
		
		String sQuery = wasdiQueryToMongoByName(sFileName, dWest, dNorth, dEast, dSouth, sService, lDateFrom, lDateTo, sDayNightFlag);

		try {
			final List<EcoStressItemForReading> aoReturnList = new ArrayList<>();
			
			BasicDBObject oSort = new BasicDBObject("beginningDate", 1);
			
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Document.parse(sQuery)).sort(oSort);

			fillList(aoReturnList, oWSDocuments, EcoStressItemForReading.class);
			
			return aoReturnList;
		} catch (Exception oEx) {
			WasdiLog.errorLog("EcoStressRepository.getEcoStressItemByName: error", oEx);
		}

		return null;
	}

	/**
	 * Get all the EcoStress Items
	 * @return the full list of items
	 */
	public List<EcoStressItemForReading> getEcoStressItemList(Double dWest, Double dNorth, Double dEast, Double dSouth, String sService,
			Long lDateFrom, Long lDateTo, String sDayNightFlag, int iOffset, int iLimit) {
		
		try {
			final List<EcoStressItemForReading> aoReturnList = new ArrayList<>();

			String sQuery = wasdiQueryToMongo(dWest, dNorth, dEast, dSouth, sService, lDateFrom, lDateTo, sDayNightFlag);
			
			BasicDBObject oSort = new BasicDBObject("beginningDate", 1);
			
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Document.parse(sQuery)).skip(iOffset).limit(iLimit).sort(oSort);

			fillList(aoReturnList, oWSDocuments, EcoStressItemForReading.class);
			
			return aoReturnList;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("EcoStressRepository.getEcoStressItemList: error", oEx);
		}

		return null;
	}

	/**
	 * Get a EcoStress Item by file name
	 * @param sFileName
	 * @return the item
	 */
	public EcoStressItemForReading getEcoStressItem(String sFileName) {

		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("fileName", sFileName)).first();

			if (oWSDocument != null) {
				String sJSON = oWSDocument.toJson();

				EcoStressItemForReading oEcoStressItem = s_oMapper.readValue(sJSON, EcoStressItemForReading.class);

				return oEcoStressItem;
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("EcoStressRepository.getEcoStressItem: error", oEx);
		}

		return  null;
	}

	public long countItems(Double dWest, Double dNorth, Double dEast, Double dSouth, String sService, Long lDateFrom, Long lDateTo, String sDayNightFlag) {
		
		String sQuery = wasdiQueryToMongo(dWest, dNorth, dEast, dSouth, sService, lDateFrom, lDateTo, sDayNightFlag);

		try {
			long lCount = getCollection(m_sThisCollection).countDocuments(Document.parse(sQuery));

			return lCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("EcoStressRepository.countItems: error", oEx);
		}

		return -1;
	}
	
	public EcoStressItemForReading getEcoStressByFileNamePrefix(String sFileNamePrefix) {
	    try {
	        final List<EcoStressItemForReading> aoReturnList = new ArrayList<>();
	        	        
	        // query: { "fileName": { "$regex": "^prefisso" } }
	        Bson oQuery = Filters.regex("fileName", "^" + java.util.regex.Pattern.quote(sFileNamePrefix));	        
	        
	        Document oWSDocument = getCollection(m_sThisCollection)
	                .find(oQuery).first();

	        if (oWSDocument != null) {
				String sJSON = oWSDocument.toJson();

				EcoStressItemForReading oEcoStressItem = s_oMapper.readValue(sJSON, EcoStressItemForReading.class);

				return oEcoStressItem;
			}
	        
	    } catch (Exception oEx) {
	        WasdiLog.errorLog("EcoStressRepository.getEcoStressByFileNamePrefix: error", oEx);
	    }

	    return null;
	}

}
