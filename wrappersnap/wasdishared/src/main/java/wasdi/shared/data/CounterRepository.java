package wasdi.shared.data;

import org.bson.Document;

import wasdi.shared.business.Counter;

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
	
	        try {
	            String sJSON = s_oMapper.writeValueAsString(oCounter);
	            Document oDocument = Document.parse(sJSON);
	            getCollection("counter").insertOne(oDocument);
	        return oDocument.getObjectId("_id").toHexString();
	
	    } catch (Exception oEx) {
	        oEx.printStackTrace();
	    }
	
	    return "";
	}
	
	
	public Counter GetCounterBySequence(String sSequence) {

        try {
            Document oWSDocument = getCollection("counter").find(new Document("sequence", sSequence)).first();

            String sJSON = oWSDocument.toJson();

            Counter oCounter = s_oMapper.readValue(sJSON,Counter.class);

            return oCounter;
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
	}
	
    public boolean UpdateCounter(Counter oCounter) {

        try {
        	        	
            String sJSON = s_oMapper.writeValueAsString(oCounter);
            Document filter = new Document("sequence", oCounter.getSequence());
			Document update = new Document("$set", new Document(Document.parse(sJSON)));
			getCollection("counter").updateOne(filter, update);

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
	 
	    
}
