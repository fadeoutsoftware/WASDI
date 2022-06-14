package wasdi.shared.data.ecostress;

import org.bson.Document;

import wasdi.shared.business.ecostress.EcoStressItem;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.Utils;

public class EcoStressRepository extends MongoRepository {
	
	public EcoStressRepository() {
		m_sThisCollection = "catalog";
		m_sRepoDb = "ecostress";
	}	

	
	public void insertEcoStressItem(EcoStressItem oNewDocument) {
		String sResult = "";
		
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
				sResult = oDocument.getObjectId("_id").toHexString();
	
			} catch (Exception oEx) {
				Utils.debugLog("EcoStressRepository: " + oEx);
			}
		}
	}
}
