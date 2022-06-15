package wasdi.shared.data.ecostress;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import wasdi.shared.business.ecostress.EcoStressItemForReading;
import wasdi.shared.business.ecostress.EcoStressItemForWriting;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.Utils;

public class EcoStressRepository extends MongoRepository {

	public EcoStressRepository() {
		m_sThisCollection = "catalog";
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

				Utils.debugLog("EcoStressRepository | sResult: " + sResult);
			} catch (Exception oEx) {
				Utils.debugLog("EcoStressRepository: " + oEx);
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
			oEx.printStackTrace();
		}

		return aoReturnList;
	}
	
	private String wasdiQueryToMongo(Double dWest, Double dNorth, Double dEast, Double dSouth, String sService) {
		String sCoordinates = "[ [" +dWest + ", " + dNorth + "], [" + dWest +", " + dSouth + "], [" + dEast + ", " + dSouth + "] , [" +  dEast + ", " + dNorth + "], [" +dWest + ", " + dNorth + "] ]";

		System.out.println(sCoordinates);

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
			// {$regex : "son"}
			sQuery += ", s3Path: {$regex : \"" + sService + "\"}";
		}
		
		sQuery += "   }" + 
				"";
		
		return sQuery;
	}

	/**
	 * Get all the EcoStress Items
	 * @return the full list of items
	 */
	public List<EcoStressItemForReading> getEcoStressItemList(Double dWest, Double dNorth, Double dEast, Double dSouth, String sService) {

		final List<EcoStressItemForReading> aoReturnList = new ArrayList<>();
		
		String sQuery = wasdiQueryToMongo(dWest, dNorth, dEast, dSouth, sService);

		System.out.println(sQuery);

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Document.parse(sQuery)).limit(10);

			fillList(aoReturnList, oWSDocuments, EcoStressItemForReading.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
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
			oEx.printStackTrace();
		}

		return  null;
	}

	public long countItems(Double dWest, Double dNorth, Double dEast, Double dSouth, String sService) {
		
		String sQuery = wasdiQueryToMongo(dWest, dNorth, dEast, dSouth, sService);

		System.out.println(sQuery);

		try {
			long lCount = getCollection(m_sThisCollection).countDocuments(Document.parse(sQuery));

			return lCount;
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return -1;
	}

}
