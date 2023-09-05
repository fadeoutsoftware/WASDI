package wasdi.shared.data.modis11a2;

import org.bson.Document;

import wasdi.shared.business.modis11a2.ModisItem;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.log.WasdiLog;

public class ModisRepository extends MongoRepository  {
	
	public ModisRepository() {
		m_sThisCollection = "catalog";
		m_sRepoDb = "ecostress";
	}
	
	public void insertModisItem(ModisItem oNewDocument) {
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

				WasdiLog.debugLog("EcoStressRepository | sResult: " + sResult);
			} catch (Exception oEx) {
				WasdiLog.debugLog("EcoStressRepository: " + oEx);
			}
		}
	}

}
