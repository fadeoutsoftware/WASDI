package wasdi.shared.data;

import org.bson.Document;

import wasdi.shared.business.Counter;
import wasdi.shared.utils.Utils;

public class CounterRepository extends MongoRepository {

	public int getNextValue(String sSequence) {
		Counter oCounter = GetCounterBySequence(sSequence);
		if (oCounter == null) {
			oCounter = new Counter();
			oCounter.setSequence(sSequence);
			oCounter.setValue(0);
			InsertCounter(oCounter);
			return 0;
		}
		else {
			int iNext = oCounter.getValue()+1;
			oCounter.setValue(iNext);
			UpdateCounter(oCounter);
			return iNext;
		}
	}

	public String InsertCounter(Counter oCounter) {
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


	public Counter GetCounterBySequence(String sSequence) {
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

	public boolean UpdateCounter(Counter oCounter) {
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
