package wasdi.shared.data;

import org.bson.Document;

import wasdi.shared.business.Counter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class CounterRepository extends MongoRepository {
	
	public CounterRepository() {
		m_sThisCollection = "counter";
	}
	
	/**
	 * Get the next value of a sequence
	 * @param sSequence
	 * @return
	 */
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

	/**
	 * Create a new sequence counter
	 * @param oCounter Counter Entity
	 * @return Obj Id
	 */
	public String insertCounter(Counter oCounter) {
		String sResult = "";
		if(oCounter != null) {
			try {
				String sJSON = s_oMapper.writeValueAsString(oCounter);
				Document oDocument = Document.parse(sJSON);
				getCollection(m_sThisCollection).insertOne(oDocument);
				sResult = oDocument.getObjectId("_id").toHexString();
	
			} catch (Exception oEx) {
				WasdiLog.debugLog("CounterRepository.InsertCounter: " + oEx);
			}
		}
		return sResult;
	}

	/**
	 * Get the counter of a sequence
	 * @param sSequence
	 * @return
	 */
	public Counter getCounterBySequence(String sSequence) {
		Counter oCounter = null;
		if(!Utils.isNullOrEmpty(sSequence)) {
			try {
				Document oWSDocument = getCollection(m_sThisCollection).find(new Document("sequence", sSequence)).first();
				String sJSON = oWSDocument.toJson();
				oCounter = s_oMapper.readValue(sJSON,Counter.class);
			} catch (Exception oEx) {
				WasdiLog.debugLog("CounterRepository.GetCounterBySequence( " + sSequence + " ): Counter still not existing");
			}
		}
		return oCounter;
	}

	/**
	 * Update a counter
	 * @param oCounter Counter to update
	 * @return
	 */
	public boolean updateCounter(Counter oCounter) {
		if(oCounter!=null) {
			try {
	
				String sJSON = s_oMapper.writeValueAsString(oCounter);
				Document filter = new Document("sequence", oCounter.getSequence());
				Document update = new Document("$set", new Document(Document.parse(sJSON)));
				getCollection(m_sThisCollection).updateOne(filter, update);
	
				return true;
	
			} catch (Exception oEx) {
				WasdiLog.debugLog("CounterRepository.UpdateCounter: " + oEx);
			}
		}
		return false;
	}
}
