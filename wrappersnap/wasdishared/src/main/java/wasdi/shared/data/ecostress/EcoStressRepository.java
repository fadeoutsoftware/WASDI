package wasdi.shared.data.ecostress;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;

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

				sJSON += "\"startOrbitNumber\": "+oNewDocument.getStartOrbitNumber()+",";
				sJSON += "\"stopOrbitNumber\": "+oNewDocument.getStopOrbitNumber()+",";
				sJSON += "\"dayNightFlag\": \""+oNewDocument.getDayNightFlag()+"\",";

				sJSON += "\"beginningDate\": "+oNewDocument.getBeginningDate()+",";
				sJSON += "\"endingDate\": "+oNewDocument.getEndingDate()+",";

				sJSON += "\"platform\": \""+oNewDocument.getPlatform()+"\",";
				sJSON += "\"instrument\": \""+oNewDocument.getInstrument()+"\",";
				sJSON += "\"sensor\": \""+oNewDocument.getSensor()+"\",";
				sJSON += "\"parameterName\": \""+oNewDocument.getParameterName()+"\",";
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
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

			fillList(aoReturnList, oWSDocuments, EcoStressItemForReading.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("EcoStressRepository.getEcoStressItemList: error", oEx);
		}

		return aoReturnList;
	}
	
	private String wasdiQueryToMongo(Double dWest, Double dNorth, Double dEast, Double dSouth, String sService,
			Long lDateFrom, Long lDateTo, int iRelativeOrbit, String sDayNightFlag) {
		String sCoordinates = "[ [" +dWest + ", " + dNorth + "], [" + dWest +", " + dSouth + "], [" + dEast + ", " + dSouth + "] , [" +  dEast + ", " + dNorth + "], [" +dWest + ", " + dNorth + "] ]";

		String sQuery = 
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
			sQuery += ", s3Path: {$regex : \"" + sService + "\"}";
		}

		if (lDateFrom != null) {
			sQuery += ", beginningDate: {$gte: " + lDateFrom + "}";
		}

		if (lDateTo != null) {
			sQuery += ", endingDate: {$lte: " + lDateTo + "}";
		}

		if (iRelativeOrbit != -1) {
			sQuery += ", startOrbitNumber: {$eq: " + iRelativeOrbit + "}";
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
	public List<EcoStressItemForReading> getEcoStressItemList(Double dWest, Double dNorth, Double dEast, Double dSouth, String sService,
			Long lDateFrom, Long lDateTo, int iRelativeOrbit, String sDayNightFlag, int iOffset, int iLimit) {

		final List<EcoStressItemForReading> aoReturnList = new ArrayList<>();
		
		String sQuery = wasdiQueryToMongo(dWest, dNorth, dEast, dSouth, sService, lDateFrom, lDateTo, iRelativeOrbit, sDayNightFlag);

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Document.parse(sQuery)).skip(iOffset).limit(iLimit);

			fillList(aoReturnList, oWSDocuments, EcoStressItemForReading.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("EcoStressRepository.getEcoStressItemList: error", oEx);
		}

		return aoReturnList;
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

	public long countItems(Double dWest, Double dNorth, Double dEast, Double dSouth, String sService, Long lDateFrom, Long lDateTo,
			int iRelativeOrbit, String sDayNightFlag) {
		
		String sQuery = wasdiQueryToMongo(dWest, dNorth, dEast, dSouth, sService, lDateFrom, lDateTo, iRelativeOrbit, sDayNightFlag);

		try {
			long lCount = getCollection(m_sThisCollection).countDocuments(Document.parse(sQuery));

			return lCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("EcoStressRepository.countItems: error", oEx);
		}

		return -1;
	}

}
