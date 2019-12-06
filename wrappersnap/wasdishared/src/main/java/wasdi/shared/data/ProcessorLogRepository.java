package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

import wasdi.shared.business.ProcessorLog;
import wasdi.shared.utils.Utils;

public class ProcessorLogRepository extends MongoRepository {
	

    public String InsertProcessLog(ProcessorLog oProcessLog) {
        try {
        	if(null == oProcessLog) {
        		Utils.debugLog("ProcessorLogRepository.InsertProcessLog: oProcessorLog is null");
        		return null;
        	}
        	CounterRepository oCounterRepo = new CounterRepository();
        	
        	oProcessLog.setRowNumber(oCounterRepo.getNextValue(oProcessLog.getProcessWorkspaceId()));
        	
            String sJSON = s_oMapper.writeValueAsString(oProcessLog);
            Document oDocument = Document.parse(sJSON);
            
            getCollection("processorlog").insertOne(oDocument);
            return oDocument.getObjectId("_id").toHexString();

        } catch (Exception oEx) {
            Utils.debugLog("ProcessorLogRepository.InsertProcessLog: "+oEx);
        }
        return "";
    }

    public boolean DeleteProcessorLog(String sId) {
        try {
            getCollection("processorlog").deleteOne(new Document("_id", new ObjectId(sId)));

            return true;

        } catch (Exception oEx) {
        	Utils.debugLog("ProcessorLogRepository.DeleteProcessorLog( "+sId+" )" +oEx);
        }

        return false;
    }

    public List<ProcessorLog> GetLogsByProcessWorkspaceId(String sProcessWorkspaceId) {

        final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
        if(!Utils.isNullOrEmpty(sProcessWorkspaceId)) {
	        try {
	            FindIterable<Document> oWSDocuments = getCollection("processorlog").find(new Document("processWorkspaceId", sProcessWorkspaceId));
	            if(oWSDocuments!=null) {
	            	fillList(aoReturnList, oWSDocuments);
	            }
	        } catch (Exception oEx) {
	        	Utils.debugLog("ProcessorLogRepository.GetLogsByProcessWorkspaceId( " + sProcessWorkspaceId + " )" +oEx);
	        }
        }
        return aoReturnList;
    }
    
    
    public boolean DeleteLogsByProcessWorkspaceId(String sProcessWorkspaceId) {
        
        if(!Utils.isNullOrEmpty(sProcessWorkspaceId)) {
        	
	        try {
	            getCollection("processorlog").deleteMany(new Document("processWorkspaceId", sProcessWorkspaceId));
	            return true;
	        } catch (Exception oEx) {
	        	Utils.debugLog("ProcessorLogRepository.GetLogsByProcessWorkspaceId( " + sProcessWorkspaceId + " )" +oEx);
	        }
        }
        return false;
    }
    
    public List<ProcessorLog> GetLogRowsByText(String sLogText) {

        final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
        if(!Utils.isNullOrEmpty(sLogText)) {
	        try {
	        	
	        	BasicDBObject oRegexQuery = new BasicDBObject();
	        	oRegexQuery.put("logRow", new BasicDBObject("$regex", sLogText));
	        	
	            FindIterable<Document> oWSDocuments = getCollection("processorlog").find(oRegexQuery);
	            if(oWSDocuments!=null) {
	            	fillList(aoReturnList, oWSDocuments);
	            }
	        } catch (Exception oEx) {
	        	Utils.debugLog("ProcessorLogRepository.GetLogRowsByText( " + sLogText + " )" +oEx);
	        }
        }
        return aoReturnList;
    }
    
    public List<ProcessorLog> GetLogRowsByTextAndProcessId(String sLogText, String sProcessWorkspaceId) {

        final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
        if(!Utils.isNullOrEmpty(sLogText)) {
	        try {
	        	
	        	BasicDBObject oRegexQuery = new BasicDBObject();
	        	oRegexQuery.put("logRow", new BasicDBObject("$regex", sLogText));

	        	List<BasicDBObject> aoFilters = new ArrayList<BasicDBObject>();
	        	aoFilters.add(oRegexQuery);
	        	aoFilters.add(new BasicDBObject("processWorkspaceId", sProcessWorkspaceId));
	        	
	        	BasicDBObject oAndQuery = new BasicDBObject();
	        	oAndQuery.put("$and", aoFilters);
	        	
	        	
	            FindIterable<Document> oWSDocuments = getCollection("processorlog").find(oAndQuery);
	            if(oWSDocuments!=null) {
	            	fillList(aoReturnList, oWSDocuments);
	            }
	        } catch (Exception oEx) {
	        	Utils.debugLog("ProcessorLogRepository.GetLogRowsByText( " + sLogText + " )" +oEx);
	        }
        }
        return aoReturnList;
    }
    
    
    //note iLo and iUp are included
	public List<ProcessorLog> getLogsByWorkspaceIdInRange(String sProcessWorkspaceId, Integer iLo, Integer iUp){
		if(null == sProcessWorkspaceId || iLo == null || iUp == null) {
			throw new NullPointerException("ProcessorLogRepository.getLogsByWorkspaceIdInRange( " +
					sProcessWorkspaceId + ", " + iLo + ", " + iUp + " ): null argument passed");
		}
		if(iLo < 0 || iLo >iUp) {
			throw new IllegalArgumentException("ProcessorLogRepository.getLogsByWorkspaceIdInRange: 0 <= "+iLo+" <= "+iUp+" is unverified");
		}
	
        final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
        try {
        	//new QueryBuilder();
			//MongoDB query is:
    		//db.getCollection('processorlog').find({ "rowNumber":{$gte: iLo, $lte: iUp}, processWorkspaceId: sProceessWorkspaceId })
        	DBObject oQuery = QueryBuilder.start().and(
        					QueryBuilder.start().put("processWorkspaceId").is(sProcessWorkspaceId).get(),
        					QueryBuilder.start().and(
        						QueryBuilder.start().put("rowNumber").greaterThanEquals(iLo).get(),
        						QueryBuilder.start().put("rowNumber").lessThanEquals(iUp).get()
        					).get()
        		    ).get();
        	BasicDBObject oDocument = new BasicDBObject();
        	oDocument.putAll(oQuery);
        	MongoCollection<Document> aoProcessorLogCollection =  getCollection("processorlog");
        	FindIterable<Document> oWSDocuments = aoProcessorLogCollection.find(oDocument);
        	if(oWSDocuments != null) {
        		fillList(aoReturnList, oWSDocuments);
        	}
        } catch (Exception oEx) {
        	Utils.debugLog("ProcessorLogRepository.getLogsByWorkspaceIdInRange" + oEx);
        }

        return aoReturnList;

	}
 
    
	private void fillList(final ArrayList<ProcessorLog> aoReturnList, FindIterable<Document> oWSDocuments) {
		oWSDocuments.forEach(new Block<Document>() {
		    public void apply(Document document) {
		        String sJSON = document.toJson();
		        ProcessorLog oProcessorLog= null;
		        try {
		        	oProcessorLog = s_oMapper.readValue(sJSON,ProcessorLog.class);
		            aoReturnList.add(oProcessorLog);
		        } catch (IOException oEx) {
		        	Utils.debugLog("ProcessorLogRepository.fillList: "+oEx);
		        }

		    }
		});
	}

}
