package wasdi.shared.data.ecostress;

import org.bson.Document;

import wasdi.shared.data.MongoRepository;
import wasdi.shared.utils.Utils;

public class EcoStressRepository extends MongoRepository {
	
	public EcoStressRepository() {
		m_sThisCollection = "catalog";
		m_sRepoDb = "ecostress";
	}	

	
	public void insertEcoStressItem(Object oNewDocument) {
		String sResult = "";
		
		if(oNewDocument != null) {
			try {
				String sJSON = s_oMapper.writeValueAsString(oNewDocument);
				Document oDocument = Document.parse(sJSON);
				getCollection(m_sThisCollection).insertOne(oDocument);
				sResult = oDocument.getObjectId("_id").toHexString();
	
			} catch (Exception oEx) {
				Utils.debugLog("EcoStressRepository: " + oEx);
			}
		}
	}
}
