package wasdi.shared.data;

import org.bson.Document;

import wasdi.shared.business.Counter;
import wasdi.shared.utils.Utils;

public class CounterRepository extends MongoRepository {

	public int getNextValue(String sSequence) {
		Counter oCounter = getCounterBySequence(sSequence);
		if (oCounter == null) {
			oCounter = new Counter();
			oCounter.setSequence(sSequence);
			oCounter.setValue(0);
			insertCounter(oCounter);
			return 0;
		}
		else {
			int iNext = oCounter.getValue()+1;
			oCounter.setValue(iNext);
			updateCounter(oCounter);
			return iNext;
		}
	}

	public String insertCounter(Counter oCounter) {
		String sResult = "";
		if(oCounter != null) {
			try {
				String sJSON = s_oMapper.writeValueAsString(oCounter);
				Document oDocument = Document.parse(sJSON);
				getCollection("counter").insertOne(oDocument);
				sResult = oDocument.getObjectId("_id").toHexString();
	
			} catch (Exception oEx) {
				Utils.debugLog("CounterRepository.InsertCounter: " + oEx);
			}
		}
		return sResult;
	}


	public Counter getCounterBySequence(String sSequence) {
		Counter oCounter = null;
		if(!Utils.isNullOrEmpty(sSequence)) {
			try {
				Document oWSDocument = getCollection("counter").find(new Document("sequence", sSequence)).first();
				String sJSON = oWSDocument.toJson();
				oCounter = s_oMapper.readValue(sJSON,Counter.class);
			} catch (Exception oEx) {
				Utils.debugLog("CounterRepository.GetCounterBySequence( " + sSequence + " ): " + oEx);
			}
		}
		return oCounter;
	}

	public boolean updateCounter(Counter oCounter) {
		if(oCounter!=null) {
			try {
	
				String sJSON = s_oMapper.writeValueAsString(oCounter);
				Document filter = new Document("sequence", oCounter.getSequence());
				Document update = new Document("$set", new Document(Document.parse(sJSON)));
				getCollection("counter").updateOne(filter, update);
	
				return true;
	
			} catch (Exception oEx) {
				Utils.debugLog("CounterRepository.UpdateCounter: " + oEx);
			}
		}
		return false;
	}
}
