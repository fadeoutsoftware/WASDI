package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.Utils;

/**
 * Created by s.adamo on 31/01/2017.
 */
public class ProcessWorkspaceRepository extends MongoRepository {

	/**
	 * Insert a new Process Workspace 
	 * @param oProcessWorkspace Process Workpsace to insert
	 * @return Mongo obj id
	 */
    public String insertProcessWorkspace(ProcessWorkspace oProcessWorkspace) {

        try {
        	
        	Utils.debugLog("Inserting Process " + oProcessWorkspace.getProcessObjId() + " - status: " + oProcessWorkspace.getStatus());
        	
        	// Initialize the Last State Change Date
        	oProcessWorkspace.setLastStateChangeDate(Utils.GetFormatDate(new Date()));
        	
            String sJSON = s_oMapper.writeValueAsString(oProcessWorkspace);
            Document oDocument = Document.parse(sJSON);
            getCollection("processworkpsace").insertOne(oDocument);
            return oDocument.getObjectId("_id").toHexString();

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return "";
    }

    /**
     * Delete an Entity by Mongo Id
     * @param sId Process Workspace Mongo Id
     * @return
     */
    public boolean deleteProcessWorkspace(String sId) {

        try {
            getCollection("processworkpsace").deleteOne(new Document("_id", new ObjectId(sId)));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    /**
     * Delete Process Workspace by PID
     * @param iPid
     * @return
     */
    public boolean deleteProcessWorkspaceByPid(int iPid) {

        try {
            getCollection("processworkpsace").deleteOne(new Document("pid", iPid));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    /**
     * Delete Process Workspace by WASDI ID
     * @param sProcessObjId
     * @return
     */
    public boolean deleteProcessWorkspaceByProcessObjId(String sProcessObjId) {

        try {
            getCollection("processworkpsace").deleteOne(new Document("processObjId", sProcessObjId));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }    

    /**
     * Get List of Process Workspaces in a Workspace
     * @param sWorkspaceId
     * @return
     */
    public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(new Document("workspaceId", sWorkspaceId));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get paginated list of process workspace in a workspace
     * @param sWorkspaceId
     * @param iStartIndex
     * @param iEndIndex
     * @return
     */
    public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, int iStartIndex, int iEndIndex) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(new Document("workspaceId", sWorkspaceId)).sort(new Document("$natural", -1)).skip(iStartIndex).limit(iEndIndex-iStartIndex);
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the total count of pw in a workspace
     * @param sWorkspaceId
     * @return
     */
    public long countByWorkspace(String sWorkspaceId) {
    	try {
    		long lCount = getCollection("processworkpsace").count(new Document("workspaceId", sWorkspaceId));
    		return lCount;
    	}
    	catch (Exception oEx) {
    		oEx.printStackTrace();
		}
    	
    	return 0;
    }
    
    /**
     * Get the list of process workspace for a user
     * @param sUserId
     * @return
     */
    public List<ProcessWorkspace> getProcessByUser(String sUserId) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(new Document("userId", sUserId));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

    
    /**
     * Get the list of created processes NOT Download or IDL
     * @return
     */
    public List<ProcessWorkspace> getCreatedProcesses() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
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
    
    /**
     * Get the list of created processes NOT Download or IDL in a NODE
     * @param sComputingNodeCode
     * @return
     */
    public List<ProcessWorkspace> getCreatedProcessesByNode(String sComputingNodeCode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.not(Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())),
            				Filters.not(Filters.eq("operationType", LauncherOperations.RUNIDL.name())),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of processes not Download or IDL in ready state for a specific node
     * @param sComputingNodeCode
     * @return
     */
    public List<ProcessWorkspace> getReadyProcessesByNode(String sComputingNodeCode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.not(Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())),
            				Filters.not(Filters.eq("operationType", LauncherOperations.RUNIDL.name())),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of processes not download or idl in running state
     * @return
     */
    public List<ProcessWorkspace> getRunningProcesses() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
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
    
    /**
     * Get the list of processes not download or idl in running state in a specific node
     * @param sComputingNodeCode
     * @return
     */
    public List<ProcessWorkspace> getRunningProcessesByNode(String sComputingNodeCode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.not(Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())),
            				Filters.not(Filters.eq("operationType", LauncherOperations.RUNIDL.name())),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }    
    
    /**
     * Get the list of created download processes
     * @return
     */
    public List<ProcessWorkspace> getCreatedDownloads() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of created download processes in specific node
     * @param sComputingNodeCode
     * @return
     */
    public List<ProcessWorkspace> getCreatedDownloadsByNode(String sComputingNodeCode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name()),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of created idl processes
     * @return
     */
    public List<ProcessWorkspace> getCreatedIDL() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name())
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

    /**
     * Get the list of created idl processes in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getCreatedIDLByNode(String sComputingNode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of running download processes
     * @return
     */
    public List<ProcessWorkspace> getRunningDownloads() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of running download processes in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getRunningDownloadsByNode(String sComputingNode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of ready download processes
     * @return
     */
    public List<ProcessWorkspace> getReadyDownloads() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of ready download processes in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getReadyDownloadsByNode(String sComputingNode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    
    /**
     * Get the list of running IDL
     * @return
     */
    public List<ProcessWorkspace> getRunningIDL() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name())
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of running IDL in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getRunningIDLByNode(String sComputingNode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

    /**
     * Get the list of ready IDL
     * @return
     */
    public List<ProcessWorkspace> getReadyIDL() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name())
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of ready IDL in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getReadyIDLByNode(String sComputingNode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    
    
    
    
    /**
     * Get the list of processes not download or idl in running state in a specific node
     * @param sComputingNodeCode
     * @return
     */
    public List<ProcessWorkspace> getProcessesByStateNode(String sProcessStatus, String sComputingNodeCode) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("status", sProcessStatus),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }    
    
    
    
    
	private void fillList(final ArrayList<ProcessWorkspace> aoReturnList, FindIterable<Document> oWSDocuments) {
		oWSDocuments.forEach(new Block<Document>() {
		    public void apply(Document document) {
		        String sJSON = document.toJson();
		        ProcessWorkspace oProcessWorkspace = null;
		        try {
		            oProcessWorkspace = s_oMapper.readValue(sJSON,ProcessWorkspace.class);
		            aoReturnList.add(oProcessWorkspace);
		        } catch (IOException e) {
		            e.printStackTrace();
		        }

		    }
		});
	}

    public List<ProcessWorkspace> getLastProcessByWorkspace(String sWorkspaceId) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(new Document("workspaceId", sWorkspaceId)).sort(new Document("_id", -1)).limit(5);
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }
        
        // The client expects the most recent last
        Collections.reverse(aoReturnList);

        return aoReturnList;
    }

    public List<ProcessWorkspace> getLastProcessByUser(String sUserId) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(new Document("userId", sUserId)).sort(new Document("_id", -1)).limit(5);
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }
        
        // The client expects the most recent last
        Collections.reverse(aoReturnList);

        return aoReturnList;
    }
    
    
    public ProcessWorkspace getProcessByProductName(String sProductName) {
        ProcessWorkspace oProcessWorkspace = null;
        try {

            Document oWSDocument = getCollection("processworkpsace").find(new Document("productName", sProductName)).first();

            if (oWSDocument==null) return  null;

            String sJSON = oWSDocument.toJson();
            try {
                oProcessWorkspace = s_oMapper.readValue(sJSON, ProcessWorkspace.class);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return oProcessWorkspace;
    }

    public List<ProcessWorkspace> getProcessByProductNameAndWorkspace(String sProductName, String sWorkspace) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(
            		Filters.and(
            				Filters.eq("productName", sProductName),
            				Filters.eq("workspaceId", sWorkspace)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    public ProcessWorkspace getProcessByProcessObjId(String sProcessObjId) {
        ProcessWorkspace oProcessWorkspace = null;
        try {

            Document oWSDocument = getCollection("processworkpsace").find(new Document("processObjId", sProcessObjId)).first();

            if (oWSDocument==null) return  null;

            String sJSON = oWSDocument.toJson();
            try {
                oProcessWorkspace = s_oMapper.readValue(sJSON, ProcessWorkspace.class);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return oProcessWorkspace;
    }

    /**
     * Update a process Workspace. If there is a state change, the system will update also update lastStateChangeDate 
     * @param oProcessWorkspace Process Workpsace to update
     * @return
     */
    public boolean updateProcess(ProcessWorkspace oProcessWorkspace) {

        try {
        	
        	ProcessWorkspace oOriginal = getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());
        	
        	if (oOriginal.getStatus().equals(oProcessWorkspace.getStatus()) == false) {
        		oProcessWorkspace.setLastStateChangeDate(Utils.GetFormatDate(new Date()));
        	}
        	
        	Utils.debugLog("Updating Process " + oProcessWorkspace.getProcessObjId() + " - status: " + oProcessWorkspace.getStatus());
        	
            String sJSON = s_oMapper.writeValueAsString(oProcessWorkspace);
            Document filter = new Document("processObjId", oProcessWorkspace.getProcessObjId());
			Document update = new Document("$set", new Document(Document.parse(sJSON)));
			getCollection("processworkpsace").updateOne(filter, update);

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    public boolean cleanQueue() {

        try {
        	
        	Utils.debugLog("Cleaning ProcessWorkspace Queue");
        	String sJSON = "{\"status\":\"ERROR\"}";
            Document oFilter = new Document("status", "CREATED");
			Document oUpdate = new Document("$set", new Document(Document.parse(sJSON)));
			getCollection("processworkpsace").updateMany(oFilter, oUpdate);

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }


    public boolean existsPidProcessWorkspace(Integer iPid) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        boolean bExists = false;
        try {

            FindIterable<Document> oWSDocuments = getCollection("processworkpsace").find(Filters.and(Filters.eq("pid", iPid)));

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    ProcessWorkspace oProcessWorkspace = null;
                    try {
                        oProcessWorkspace = s_oMapper.readValue(sJSON,ProcessWorkspace.class);
                        aoReturnList.add(oProcessWorkspace);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        if (aoReturnList.size() > 0)
            bExists = true;

        return bExists;
    }


}
