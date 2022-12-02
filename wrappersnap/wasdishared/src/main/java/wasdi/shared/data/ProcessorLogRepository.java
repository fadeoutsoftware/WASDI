package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import wasdi.shared.business.ProcessorLog;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * User Processors Log repository
 * @author p.campanella
 *
 */
public class ProcessorLogRepository extends MongoRepository {
	
	public ProcessorLogRepository() {
		m_sThisCollection = "processorlog";
		m_sRepoDb = "local";
	}
	
	/**
	 * Insert a new log row
	 * @param oProcessLog row to add
	 * @return Mongo Obj Id
	 */
    public String insertProcessLog(ProcessorLog oProcessLog) {
        try {
        	if(null == oProcessLog) {
        		WasdiLog.debugLog("ProcessorLogRepository.InsertProcessLog: oProcessorLog is null");
        		return null;
        	}
        	CounterRepository oCounterRepo = new CounterRepository();
        	
        	oProcessLog.setRowNumber(oCounterRepo.getNextValue(oProcessLog.getProcessWorkspaceId()));
        	
            String sJSON = s_oMapper.writeValueAsString(oProcessLog);
            Document oDocument = Document.parse(sJSON);
            
            getCollection(m_sThisCollection).insertOne(oDocument);
            return oDocument.getObjectId("_id").toHexString();

        } catch (Exception oEx) {
            WasdiLog.debugLog("ProcessorLogRepository.InsertProcessLog: "+oEx);
        }
        return "";
    }
    
	/**
	 * Insert a new log row
	 * @param aoProcessLogs a list of rows to add
	 */
    public void insertProcessLogList(List<ProcessorLog> aoProcessLogs) {
        try {
        	if(null == aoProcessLogs) {
        		WasdiLog.debugLog("ProcessorLogRepository.InsertProcessLogList: aoProcessorLog is null");
        		return;
        	}
        	
        	List<Document> aoDocs = new ArrayList<>();
        	for (ProcessorLog oProcessorLog: aoProcessLogs) {
        		if(null!=oProcessorLog) {
        			String sJSON = s_oMapper.writeValueAsString(oProcessorLog);
                    Document oDocument = Document.parse(sJSON);
                    aoDocs.add(oDocument);
        		}
			}
        	getCollection(m_sThisCollection).insertMany(aoDocs);

        } catch (Exception oEx) {
            WasdiLog.debugLog("ProcessorLogRepository.InsertProcessLog: "+oEx);
        }
        return;
    }
    
    /**
     * Delete a Log row by Mongo Id
     * @param sId Mongo Obj Id
     * @return True or False
     */
    public boolean deleteProcessorLog(String sId) {
        try {
            getCollection(m_sThisCollection).deleteOne(new Document("_id", new ObjectId(sId)));

            return true;

        } catch (Exception oEx) {
        	WasdiLog.debugLog("ProcessorLogRepository.DeleteProcessorLog( "+sId+" )" +oEx);
        }

        return false;
    }
    
    /**
     * Get the logs of a ProcessWorkspace
     * @param sProcessWorkspaceId Id of the process
     * @return List of all the log rows
     */
    public List<ProcessorLog> getLogsByProcessWorkspaceId(String sProcessWorkspaceId) {

        final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
        if(!Utils.isNullOrEmpty(sProcessWorkspaceId)) {
	        try {
	            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("processWorkspaceId", sProcessWorkspaceId));
	            if(oWSDocuments!=null) {
	            	fillList(aoReturnList, oWSDocuments, ProcessorLog.class);
	            }
	        } catch (Exception oEx) {
	        	WasdiLog.debugLog("ProcessorLogRepository.GetLogsByProcessWorkspaceId( " + sProcessWorkspaceId + " )" +oEx);
	        }
        }
        return aoReturnList;
    }
    
    /**
     * Get all the logs of an array of ProcessWorkspaceId
     * @param asProcessWorkspaceId
     * @return
     */
    public List<ProcessorLog> getLogsByArrayProcessWorkspaceId(List<String> asProcessWorkspaceId) {
    	
    	/*
        ObjectId[] aoObjarray = new ObjectId[asProcessWorkspaceId.size()];

        for(int i=0;i<asProcessWorkspaceId.size();i++)
        {
            aoObjarray[i] = new ObjectId(asProcessWorkspaceId.get(i));
        }
        */

        BasicDBObject oInQuery = new BasicDBObject("$in", asProcessWorkspaceId);
        BasicDBObject oQuery = new BasicDBObject("processWorkspaceId", oInQuery);

        final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
        
        try {
            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oQuery);
            if(oWSDocuments!=null) {
            	fillList(aoReturnList, oWSDocuments, ProcessorLog.class);
            }
        } catch (Exception oEx) {
        	WasdiLog.debugLog("ProcessorLogRepository.getLogsByArrayProcessWorkspaceId" +oEx);
        }
        return aoReturnList;
    }
    
    //
    /**
     * Deletes logs older than a specified date
     * makes a query like: {logDate: {"$lt":"2019-05-04 00:00:00"}}
     * @param sDate Date in format YYYY-MM-DD
     * @return
     */
    public boolean deleteLogsOlderThan(String sDate) {
    	
    	if (Utils.isNullOrEmpty(sDate)) return false;
    	
    	sDate = sDate + " 00:00:00";
    	
        BasicDBObject oLessThanQuery = new BasicDBObject("$lt", sDate);
        BasicDBObject oQuery = new BasicDBObject("logDate", oLessThanQuery);

        
        try {
            getCollection(m_sThisCollection).deleteMany(oQuery);
            return true;
            
        } catch (Exception oEx) {
        	WasdiLog.debugLog("ProcessorLogRepository.deleteLogsOlderThan" +oEx);
        }
        return false;
    }
    
    /**
     * Delete all the logs of a Process Workspace
     * @param sProcessWorkspaceId Id of the process
     * @return True or False in case of error
     */
    public boolean deleteLogsByProcessWorkspaceId(String sProcessWorkspaceId) {
        
        if(!Utils.isNullOrEmpty(sProcessWorkspaceId)) {
        	
	        try {
	            getCollection(m_sThisCollection).deleteMany(new Document("processWorkspaceId", sProcessWorkspaceId));
	            return true;
	        } catch (Exception oEx) {
	        	WasdiLog.debugLog("ProcessorLogRepository.GetLogsByProcessWorkspaceId( " + sProcessWorkspaceId + " )" +oEx);
	        }
        }
        return false;
    }
    
    public boolean deleteLogsByProcessWorkspaceIds(List<String> asProcessObjIds) {
    	return false;
    }
    
    /**
     * Get all the log rows containing a specified text
     * @param sLogText Text to search for
     * @return List of log rows containing the text
     */
    public List<ProcessorLog> getLogRowsByText(String sLogText) {

        final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
        if(!Utils.isNullOrEmpty(sLogText)) {
	        try {
	        	
	        	BasicDBObject oRegexQuery = new BasicDBObject();
	        	oRegexQuery.put("logRow", new BasicDBObject("$regex", sLogText));
	        	
	            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oRegexQuery);
	            if(oWSDocuments!=null) {
	            	fillList(aoReturnList, oWSDocuments, ProcessorLog.class);
	            }
	        } catch (Exception oEx) {
	        	WasdiLog.debugLog("ProcessorLogRepository.GetLogRowsByText( " + sLogText + " )" +oEx);
	        }
        }
        return aoReturnList;
    }
    
    /**
     * Get all the log rows of a ProcessWorkpsace containing a specified text
     * @param sLogText sLogText Text to search for
     * @param sProcessWorkspaceId WASDI Id of the Process Workspace
     * @return List of found rows
     */
    public List<ProcessorLog> getLogRowsByTextAndProcessId(String sLogText, String sProcessWorkspaceId) {

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
	        	
	        	
	            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oAndQuery);
	            if(oWSDocuments!=null) {
	            	fillList(aoReturnList, oWSDocuments, ProcessorLog.class);
	            }
	        } catch (Exception oEx) {
	        	WasdiLog.debugLog("ProcessorLogRepository.GetLogRowsByText( " + sLogText + " )" +oEx);
	        }
        }
        return aoReturnList;
    }
    
	/**
	 * Get a list of logs row in a range
	 * iLo and iUp are included
	 * @param sProcessWorkspaceId Process Workpsace Id
	 * @param iLo Lower Bound
	 * @param iUp Upper Bound
	 * @return List of Log Rows in the range
	 */
    public List<ProcessorLog> getLogsByWorkspaceIdInRange(String sProcessWorkspaceId, Integer iLo, Integer iUp) {
    	
    	final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
    	
		if(null == sProcessWorkspaceId || iLo == null || iUp == null) {
			WasdiLog.debugLog("ProcessorLogRepository.getLogsByWorkspaceIdInRange( " + sProcessWorkspaceId + ", " + iLo + ", " + iUp + " ): null argument passed");
			return aoReturnList;
		}
		if(iLo < 0 || iLo >iUp) {
			WasdiLog.debugLog("ProcessorLogRepository.getLogsByWorkspaceIdInRange: 0 <= "+iLo+" <= "+iUp+" range invalid or no logs available");
			return aoReturnList;
		}
        
        try {
			//MongoDB query is:
        	/*
    		//db.getCollection('processorlog').find({ "rowNumber":{$gte: iLo, $lte: iUp}, processWorkspaceId: sProceessWorkspaceId })
        	DBObject oQuery = QueryBuilder.start().and(
        					QueryBuilder.start().put("processWorkspaceId").is(sProcessWorkspaceId).get(),
        					QueryBuilder.start().and(
        						QueryBuilder.start().put("rowNumber").greaterThanEquals(iLo).get(),
        						QueryBuilder.start().put("rowNumber").lessThanEquals(iUp).get()
        					).get()
        		    ).get();
        	*/
        	Bson oFilter = Filters.and(Filters.eq("processWorkspaceId", sProcessWorkspaceId), Filters.and(Filters.gte("rowNumber", iLo), Filters.lte("rowNumber", iUp)));
        	MongoCollection<Document> aoProcessorLogCollection =  getCollection(m_sThisCollection);
        	FindIterable<Document> oWSDocuments = aoProcessorLogCollection.find(oFilter);
        	if(oWSDocuments != null) {
        		fillList(aoReturnList, oWSDocuments, ProcessorLog.class);
        	}
        } catch (Exception oEx) {
        	WasdiLog.debugLog("ProcessorLogRepository.getLogsByWorkspaceIdInRange" + oEx);
        }

        return aoReturnList;

	}
    
    /**
     * Get the list of all processes
     * @return
     */
    public List<ProcessorLog> getList() {

        final ArrayList<ProcessorLog> aoReturnList = new ArrayList<ProcessorLog>();
        try {
            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();
            fillList(aoReturnList, oWSDocuments, ProcessorLog.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

	

}
