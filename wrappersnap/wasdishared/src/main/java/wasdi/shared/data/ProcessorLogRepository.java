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

public class ProcessorLogRepository extends MongoRepository {
	

    public String InsertProcessLog(ProcessorLog oProcessLog) {

        try {
        	
        	CounterRepository oCounterRepo = new CounterRepository();
        	
        	oProcessLog.setRowNumber(oCounterRepo.getNextValue(oProcessLog.getProcessWorkspaceId()));
        	
            String sJSON = s_oMapper.writeValueAsString(oProcessLog);
            Document oDocument = Document.parse(sJSON);
            
            getCollection("processorlog").insertOne(oDocument);
            return oDocument.getObjectId("_id").toHexString();

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return "";
    }

    public boolean DeleteProcessorLog(String sId) {

        try {
            getCollection("processorlog").deleteOne(new Document("_id", new ObjectId(sId)));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    public List<ProcessorLog> GetLogsByProcessWorkspaceId(String sProcessWorkspaceId) {

        final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processorlog").find(new Document("processWorkspaceId", sProcessWorkspaceId));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    
    //note iLo and iUp are included
	public List<ProcessorLog> getLogsByWorkspaceIdInRange(String sProcessWorkspaceId, Integer iLo, Integer iUp){
		if(null == sProcessWorkspaceId || iLo == null || iUp == null) {
			throw new NullPointerException("ProcessorLogRepository.getLogsByWorkspaceIdInRange: null argument passed");
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
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;

	}
/*
    public List<ProcessWorkspace> GetQueuedProcess() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processorlog").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.not(Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())),
            				Filters.not(Filters.eq("operationType", LauncherOperations.RUNIDL.name()))
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
 */
 
    
	private void fillList(final ArrayList<ProcessorLog> aoReturnList, FindIterable<Document> oWSDocuments) {
		oWSDocuments.forEach(new Block<Document>() {
		    public void apply(Document document) {
		        String sJSON = document.toJson();
		        ProcessorLog oProcessorLog= null;
		        try {
		        	oProcessorLog = s_oMapper.readValue(sJSON,ProcessorLog.class);
		            aoReturnList.add(oProcessorLog);
		        } catch (IOException e) {
		            e.printStackTrace();
		        }

		    }
		});
	}

}
