package wasdi.shared.data;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.Utils;

/**
 * Created by s.adamo on 31/01/2017.
 */
/**
 * @author s.adamo
 * @author c.nattero
 * @author p.campanella
 *
 */
/**
 * @author c.nattero
 *
 */
/**
 * @author c.nattero
 *
 */
public class ProcessWorkspaceRepository extends MongoRepository {
	
	public ProcessWorkspaceRepository() {
		m_sThisCollection = "processworkpsace";
		m_sRepoDb = "local";
	}

	/**
	 * Insert a new Process Workspace 
	 * @param oProcessWorkspace Process Workpsace to insert
	 * @return Mongo obj id
	 */
    public String insertProcessWorkspace(ProcessWorkspace oProcessWorkspace) {
    	
    	if (oProcessWorkspace == null) return "";

        try {
        	
        	// Initialize the Last State Change Date
        	oProcessWorkspace.setLastStateChangeDate(Utils.getFormatDate(new Date()));
        	
            String sJSON = s_oMapper.writeValueAsString(oProcessWorkspace);
            Document oDocument = Document.parse(sJSON);
            getCollection(m_sThisCollection).insertOne(oDocument);
            return oDocument.getObjectId("_id").toHexString();

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return "";
    }
    
	/**
	 * Insert a list of Process Workspace Objects 
	 * @param oProcessWorkspace Process Workpsace list to insert
	 */
    public void insertProcessListWorkspace(List<ProcessWorkspace> aoProcessWorkspace) {
    	
    	if (aoProcessWorkspace == null) return;

        try {
        	
        	List<Document> aoDocs = new ArrayList<>();
        	for (ProcessWorkspace oProcessWorkspace : aoProcessWorkspace) {
        		// Initialize the Last State Change Date
        		oProcessWorkspace.setLastStateChangeDate(Utils.getFormatDate(new Date()));
        		String sJSON = s_oMapper.writeValueAsString(oProcessWorkspace);
                Document oDocument = Document.parse(sJSON);
                aoDocs.add(oDocument);
			}
        	getCollection(m_sThisCollection).insertMany(aoDocs);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }
    }

    /**
     * Delete an Entity by Mongo Id
     * @param sId Process Workspace Mongo Id
     * @return
     */
    public boolean deleteProcessWorkspace(String sId) {
    	
    	if (Utils.isNullOrEmpty(sId)) return false;

        try {
            getCollection(m_sThisCollection).deleteOne(new Document("_id", new ObjectId(sId)));

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
            getCollection(m_sThisCollection).deleteOne(new Document("pid", iPid));

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
    	
    	if (!Utils.isNullOrEmpty(sProcessObjId)) {
            try {
                getCollection(m_sThisCollection).deleteOne(new Document("processObjId", sProcessObjId));

                return true;

            } catch (Exception oEx) {
                oEx.printStackTrace();
            }    		
    	}

        return false;
    }
    
    /**
     * Delete Process Workspace by WASDI ID
     * @param sProcessObjId
     * @return
     */
    public boolean deleteProcessWorkspaceByWorkspaceId(String sWorkspaceId) {

    	if(!Utils.isNullOrEmpty(sWorkspaceId)) {
            try {
                getCollection(m_sThisCollection).deleteMany(new Document("workspaceId", sWorkspaceId));
                return true;
            } catch (Exception oEx) {
                oEx.printStackTrace();
            }
    	}
        return false;
    }


    /**
     * Get a list of root processes from a list of children
     * @param aoChildren
     * @return the list of root processes
     */
    public List<ProcessWorkspace> getRoots(List<ProcessWorkspace> aoChildren){
    	if(null==aoChildren) {
    		Utils.debugLog("ProcessWorkspaceRepository.getRoots: list of children is null, aborting");
    		return new ArrayList<ProcessWorkspace>(0);
    	}
    	if(aoChildren.size()<=0) {
    		Utils.debugLog("ProcessWorkspaceRepository.getRoots: list of children is empty, aborting");
    		return new ArrayList<ProcessWorkspace>(0);
    	}
    	try {
    		List<ProcessWorkspace> aoFathers = getFathers(aoChildren);

    		//TODO learn how to write a better query and let mongo filter these results out
    		List<ProcessWorkspace> aoRoots = new LinkedList<>();
			//for each of these launch killProcessTree
			for (ProcessWorkspace oFather : aoFathers) {
				if(null==oFather) {
					//programmers look both ways when crossing a one-way street...
					continue;
				}
				if(null == oFather.getParentId()) {
					aoRoots.add(oFather);
				}
			}
    		return aoFathers;
		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceRepository.getRoots: " + oE);
		}
    	return new ArrayList<ProcessWorkspace>(0);
    }

    /**
     * Retrieves the hierarchy of fathers of a list of processes
     * @param aoChildProcesses a list of child processes
     * @return a list of fathers
     */
    public List<ProcessWorkspace> getFathers(List<ProcessWorkspace> aoChildProcesses){

    	//check inputs
    	if(null == aoChildProcesses) {
    		Utils.debugLog("ProcessWorkspaceRepository.getFathers: the list is null, aborting");
    		return new LinkedList<ProcessWorkspace>();
    	}
    	if(aoChildProcesses.size() <= 0) {
    		Utils.debugLog("ProcessWorkspaceRepository.getFathers: the list has size 0, aborting");
    		return new LinkedList<ProcessWorkspace>();
    	}

    	//prepare iterable collection of processObjIds of given children
    	List<String> asChildrenProcessObjIds = null;
    	try {
    		asChildrenProcessObjIds = new ArrayList<String>(aoChildProcesses.size());
    		for (ProcessWorkspace oChildProcess : aoChildProcesses) {
				if(null==oChildProcess) {
					continue;
				}
				if(Utils.isNullOrEmpty(oChildProcess.getProcessObjId())) {
					continue;
				}
				asChildrenProcessObjIds.add(oChildProcess.getProcessObjId());
			}
    	} catch (Exception oE) {
    		Utils.debugLog("ProcessWorkspaceRepository.getFathers: extracting processObjIds failed due to: " + oE);
    		return new LinkedList<ProcessWorkspace>();
    	}

    	//query DB
    	AggregateIterable<Document> oWSDocuments = null;
    	try {
    		//create pipeline for aggregation using graphLookup
    		Bson oLookup = Aggregates.graphLookup(
    				"processworkpsace",
    				"$parentId",
    				"parentId",
    				"processObjId",
    				"fathers"
    				);

    		//create matching condition for aggregation
    		Bson oMatch = Aggregates.match(Filters.in("processObjId", asChildrenProcessObjIds));

    		//create aggregation
    		oWSDocuments = getCollection(m_sThisCollection).aggregate(
				Arrays.asList(
						//pipeline
						oLookup,
						oMatch
				)
			);
    	} catch (Exception oE) {
    		Utils.debugLog("ProcessWorkspaceRepository.getFathers: aggregation failed due to " + oE );
    		return new LinkedList<ProcessWorkspace>();
		}

    	//parse results
    	List<ProcessWorkspace> aoFathers = new LinkedList<ProcessWorkspace>();
    	try {
    		MongoCursor<Document> oIterator = oWSDocuments.iterator();
    		while(oIterator.hasNext()) {
	    		Document next = oIterator.next();
	    		List<Document> aoLocalFathers = null;
	    		//TODO cast to List<?> and then iterate and cast each element to Document
	    		aoLocalFathers = (List<Document>) next.get("fathers");

	    		for (Document oFather : aoLocalFathers) {
	    			String sJSON = oFather.toJson();
	    			ProcessWorkspace oProcessWorkspace = null;
	    			try {
	    				oProcessWorkspace = s_oMapper.readValue(sJSON, ProcessWorkspace.class);
	    				aoFathers.add(oProcessWorkspace);
	    			} catch (IOException oE) {
	    				Utils.debugLog("ProcessWorkspaceRepository.getFathers: error parsing one of fathers: " + oE + ", skipping");
	    			}
	    		}
    		}

		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceRepository.getFathers: could not parse query results due to " + oE);
		}
    	return aoFathers;
    }

    /**
     * Get List of Process Workspaces in a Workspace filtering by desired process status
     * @param sWorkspaceId unique id of the workspace
     * @param aeDesiredStatuses a list of desired statuses
     */
    public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, List<ProcessStatus> aeDesiredStatuses) {
        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        if(Utils.isNullOrEmpty(sWorkspaceId)) {
        	Utils.debugLog("ProcessWorkspaceRepository.getProcessByWorkspace( \"" + sWorkspaceId + "\", aeDesiredStatuses ): sWorkspaceId null or empty, aborting");
        	return aoReturnList;
        }
        if(null == aeDesiredStatuses) {
        	Utils.debugLog("ProcessWorkspaceRepository.getProcessByWorkspace( \"" + sWorkspaceId + "\", null ): aeDesiredStatuses is null, aborting");
        	return aoReturnList;
        }
        if(aeDesiredStatuses.size() <= 0) {
        	Utils.debugLog("ProcessWorkspaceRepository.getProcessByWorkspace( \"" + sWorkspaceId + "\", aeDesiredStatuses ): aeDesiredStatuses is empty, returning empty list");
        	return aoReturnList;
        }

        try {
        	//build filter on statuses
        	List<String> asDesiredStatuses = new ArrayList<String>(aeDesiredStatuses.size());
        	for (ProcessStatus eStatus : aeDesiredStatuses) {
        		if(null!=eStatus && !Utils.isNullOrEmpty(eStatus.name())) {
        			asDesiredStatuses.add(eStatus.name());
        		}
        	}
        	Bson oInFilter = Filters.in("status", asDesiredStatuses);
        	Bson oFilter = Filters.and(Filters.eq("workspaceId", sWorkspaceId), oInFilter);
        	FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oFilter)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments);
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }



    /**
     * Gets the list of processObjIds from a given workspace
     * @param sWorkspaceId the workspace id
     * @return the list of processObjIds in given workspace
     */
    public List<String> getProcessObjIdsFromWorkspaceId(String sWorkspaceId){
    	if(Utils.isNullOrEmpty(sWorkspaceId)) {
    		Utils.debugLog("ProcessWorkspaceRepository.getProcessObjIdsFromWorkspaceId: workspace id null or empty, aborting");
    		return new ArrayList<String>(0);
    	}

    	try {

    		Bson oFilter = Filters.eq("workspaceId", sWorkspaceId);
    		List<Document> oWSDocuments = getCollection(m_sThisCollection)
    				.find(oFilter)
    				.sort(new Document("operationDate", -1))
    				.projection(new Document("processObjId",1))
    				.into(new ArrayList<Document>());
    		List<String> asProcessObjIds = new ArrayList<String>(oWSDocuments.size());
    		for (Document oDocument : oWSDocuments) {
				if(null==oDocument) {
					continue;
				}
				try {
					String sProcessObjId = (String)oDocument.get("processObjId");
					asProcessObjIds.add(sProcessObjId);
				} catch (Exception oE) {
					Utils.debugLog("ProcessWorkspaceRepository.getProcessObjIdsFromWorkspaceId: cannot parse document due to " + oE + ", skipping");
					continue;
				}
			}
    		return asProcessObjIds;
    	} catch (Exception oEx) {
    		oEx.printStackTrace();
    	}

    	return new ArrayList<String>(0);
    }

    /**
     * Get List of Process Workspaces in a Workspace
     * @param sWorkspaceId unique id of the workspace
     */
    public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId) {

        return getProcessByWorkspace(sWorkspaceId, null, null, null, null, null);
    }
    

    /**
     *  Get List of Process Workspaces in a Workspace
     * @param sWorkspaceId Workspace
     * @param eStatus State
     * @param eOperation Op type
     * @param sProductNameSubstring Product Name
     * @param oDateFRom Start Date
     * @param oDateTo End Date
     * @return Found processworkspaces with applied filters
     */
    public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, ProcessStatus eStatus, LauncherOperations eOperation, String sProductNameSubstring, Instant oDateFRom, Instant oDateTo) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

        	Bson oFilter = buildFilter(sWorkspaceId, eStatus, eOperation, sProductNameSubstring, oDateFRom, oDateTo);
        	FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oFilter)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get paginated list of process workspace in a workspace
     * @param sWorkspaceId unique id of the workspace
     * @param iStartIndex start index for pagination
     * @param iEndIndex end index for pagination
     * @return list of results
     */
    public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, int iStartIndex, int iEndIndex) {

        return getProcessByWorkspace(sWorkspaceId, null, null, null, null, null, iStartIndex, iEndIndex);
    }
    
    /**
     * Get paginated list of process workspace in a workspace
     * @param sWorkspaceId unique id of the workspace
     * @param eStatus the status of the process
     * @param eOperation the type of Launcher Operation
     * @param oDateFRom starting date (included)
     * @param oDateTo ending date (included)
     * @param iStartIndex start index for pagination
     * @param iEndIndex end index for pagination
     * @return list of results
     */
    public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, ProcessStatus eStatus, LauncherOperations eOperation, String sProductNameSubstring, Instant oDateFRom, Instant oDateTo, int iStartIndex, int iEndIndex) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

        	Bson oFilter = buildFilter(sWorkspaceId, eStatus, eOperation, sProductNameSubstring, oDateFRom, oDateTo);
        	
        	FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oFilter)
            		.sort(new Document("operationDate", -1))
            		.skip(iStartIndex)
            		.limit(iEndIndex-iStartIndex);
        	
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);
        	

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    

	/**
	 * @param sWorkspaceId
	 * @param eStatus
	 * @param eOperation
	 * @param oDateFRom
	 * @param oDateTo
	 * @return
	 */
	private Bson buildFilter(String sWorkspaceId, ProcessStatus eStatus, LauncherOperations eOperation, String sProductNameSubstring, Instant oDateFRom, Instant oDateTo) {
		Bson oFilter = Filters.eq("workspaceId", sWorkspaceId);
		if(null!=eStatus) {
			Bson oCond = Filters.eq("status", eStatus.name());
			oFilter = Filters.and(oFilter, oCond);
		}
		if(null!=eOperation) {
			Bson oCond = Filters.eq("operationType", eOperation.name());
			oFilter = Filters.and(oFilter, oCond);
		}
		if(!Utils.isNullOrEmpty(sProductNameSubstring)) {
			Pattern regex = Pattern.compile(Pattern.quote(sProductNameSubstring));
			Bson oCond = Filters.eq("productName", regex);
			oFilter = Filters.and(oFilter, oCond);
		}
		if(null!=oDateFRom) {
			Bson oCond = Filters.gte("operationDate", oDateFRom);
			oFilter = Filters.and(oFilter, oCond);
		}
		if(null!=oDateTo) {
			Bson oCond = Filters.lte("operationDate", oDateFRom);
			oFilter = Filters.and(oFilter, oCond);
		}
		return oFilter;
	}
	
	/**
	 * @param sProcessId the process id
	 * @return process status as a string, null if the process is not found
	 */
	public String getProcessStatusFromId(String sProcessId) {
		Document oDocument = getCollection(m_sThisCollection).find(Filters.eq("processObjId", sProcessId)).first();
		String sJson = oDocument.toJson();
		try {
			ProcessWorkspace oProcessWorkspace = s_oMapper.readValue(sJson, ProcessWorkspace.class);
			return oProcessWorkspace.getStatus();
		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceRepository.getProcessStatusFromId( " + sProcessId + " ): " + oE );
		}
		return null;
	}
	
    /**
     * Get the total count of pw in a workspace
     * @param sWorkspaceId
     * @return
     */
    public long countByWorkspace(String sWorkspaceId) {
    	try {
    		long lCount = getCollection(m_sThisCollection).countDocuments(new Document("workspaceId", sWorkspaceId));
    		return lCount;
    	}
    	catch (Exception oEx) {
    		oEx.printStackTrace();
		}
    	
    	return -1;
    }
    
    /**
     * Get the total count of run of a processor
     * @param sProcessorName
     * @return
     */
    public long countByProcessor(String sProcessorName) {
    	try {
    		long lCount = getCollection(m_sThisCollection).countDocuments(new Document("productName", sProcessorName));
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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId)).sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.not(Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())),
            				Filters.not(Filters.eq("operationType", LauncherOperations.RUNIDL.name()))
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.not(Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())),
            				Filters.not(Filters.eq("operationType", LauncherOperations.RUNIDL.name())),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.not(Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())),
            				Filters.not(Filters.eq("operationType", LauncherOperations.RUNIDL.name())),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.not(Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())),
            				Filters.not(Filters.eq("operationType", LauncherOperations.RUNIDL.name()))
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.not(Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())),
            				Filters.not(Filters.eq("operationType", LauncherOperations.RUNIDL.name())),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name()),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name())
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.CREATED.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name())
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.eq("operationType", LauncherOperations.DOWNLOAD.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name())
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name())
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

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

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", ProcessStatus.READY.name()),
            				Filters.eq("operationType", LauncherOperations.RUNIDL.name()),
            				Filters.eq("nodeCode", sComputingNode)
            				)
            		)
            		.sort(new Document("lastStateChangeDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    
    /**
     * Get the list of processes in a specific state in a specific node
     * @param sProcessStatus status of the process
     * @param sComputingNodeCode computing node
     * @return
     */    
    public List<ProcessWorkspace> getProcessesByStateNode(String sProcessStatus, String sComputingNodeCode) {
    	return getProcessesByStateNode(sProcessStatus, sComputingNodeCode, "operationDate");
    }
    
    
    
    /**
     * Get the list of processes in a specific state in a specific node
     * @param sProcessStatus status of the process
     * @param sComputingNodeCode computing node
     * @return
     */        
    public List<ProcessWorkspace> getProcessesByStateNode(String sProcessStatus, String sComputingNodeCode, String sOrderBy) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("status", sProcessStatus),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document(sOrderBy, -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }    
    
    
    /**
     * Get the list of processes in a specific state in a specific node
     * @param sProcessStatus status of the process
     * @param sComputingNodeCode computing node
     * @return
     */        
    public List<ProcessWorkspace> getProcessesForSchedulerNode(String sComputingNodeCode, String sOrderBy) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.ne("status", "DONE"),
            				Filters.ne("status", "ERROR"),
            				Filters.ne("status", "STOPPED"),
            				Filters.eq("nodeCode", sComputingNodeCode)
            				)
            		)
            		.sort(new Document(sOrderBy, -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }    
    
    
    /**
     * Fill a list of ProcessWorkpsace Entities
     * @param aoReturnList
     * @param oWSDocuments
     */
	private void fillList(final List<ProcessWorkspace> aoReturnList, FindIterable<Document> oWSDocuments) {
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
	
	/**
	 * Get the last 5 Process Workpsace
	 * @param sWorkspaceId
	 * @return
	 */
    public List<ProcessWorkspace> getLastProcessByWorkspace(String sWorkspaceId) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {
        	FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("workspaceId", sWorkspaceId)).sort(new Document("operationDate", -1)).limit(5);
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }
        
        // The client expects the most recent last
        Collections.reverse(aoReturnList);

        return aoReturnList;
    }
    
    /**
     * Get the last 5 Process Workspace of a user
     * @param sUserId
     * @return
     */
    public List<ProcessWorkspace> getLastProcessByUser(String sUserId) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId)).sort(new Document("operationDate", -1)).limit(5);
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }
        
        // The client expects the most recent last
        Collections.reverse(aoReturnList);

        return aoReturnList;
    }
    
    /**
     * Get a Process Workpsace from product name
     * @param sProductName
     * @return
     */
    public ArrayList<ProcessWorkspace> getProcessByProductName(String sProductName) {
        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.eq("productName", sProductName))
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get a list of Process Workpsace by Product name in a Workpsace
     * @param sProductName Product Name to search
     * @param sWorkspace Workpsace
     * @return List of found proc ws
     */
    public List<ProcessWorkspace> getProcessByProductNameAndWorkspace(String sProductName, String sWorkspace) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("productName", sProductName),
            				Filters.eq("workspaceId", sWorkspace)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get a list of Process Workpsace by Product name and User Id
     * @param sProductName Product Name to search
     * @param sUserId User Id
     * @return List of found proc ws
     */
    public List<ProcessWorkspace> getProcessByProductNameAndUserId(String sProductName, String sUserId) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.and(
            				Filters.eq("productName", sProductName),
            				Filters.eq("userId", sUserId)
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    
    /**
     * Get a Process Workpsace by the WASDI Id
     * @param sProcessObjId Process Workspace Id
     * @return Entity found or null
     */
    public ProcessWorkspace getProcessByProcessObjId(String sProcessObjId) {
        ProcessWorkspace oProcessWorkspace = null;
        try {

            Document oWSDocument = getCollection(m_sThisCollection).find(new Document("processObjId", sProcessObjId)).first();

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
     * Get the status of a list of process workspace
     * @param asProcessesObjId List of string each with a ProcessWorkspace Id to query
     * @return List of string, corresponding to the input one, with the status of the relative proc id
     */
    public ArrayList<String> getProcessesStatusByProcessObjId(ArrayList<String> asProcessesObjId) {
    	
    	ArrayList<String> asReturnStatus = new ArrayList<>();
    	
    	final ArrayList<ProcessWorkspace> aoProcessesList = new ArrayList<ProcessWorkspace>();
        try {
        	
        	BasicDBObject oInQuery = new BasicDBObject();
        	
            oInQuery.put("processObjId", new BasicDBObject("$in", asProcessesObjId));

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oInQuery);
            fillList(aoProcessesList, oWSDocuments, ProcessWorkspace.class);
            
            for (int i=0; i<aoProcessesList.size(); i++) {
            	asReturnStatus.add(aoProcessesList.get(i).getStatus());
            }
        } 
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return asReturnStatus;
    }
    
    /**
     * Update a process Workspace. If there is a state change, the system will update also update lastStateChangeDate 
     * @param oProcessWorkspace Process Workspace to update
     * @return
     */
    public boolean updateProcess(ProcessWorkspace oProcessWorkspace) {

        try {
        	
        	ProcessWorkspace oOriginal = getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());
        	
        	if (oOriginal.getStatus().equals(oProcessWorkspace.getStatus()) == false) {
        		oProcessWorkspace.setLastStateChangeDate(Utils.getFormatDate(new Date()));
        	}
        	
        	//Utils.debugLog("Updating Process " + oProcessWorkspace.getProcessObjId() + " - status: " + oProcessWorkspace.getStatus());
        	
            String sJSON = s_oMapper.writeValueAsString(oProcessWorkspace);
            Document filter = new Document("processObjId", oProcessWorkspace.getProcessObjId());
			Document update = new Document("$set", new Document(Document.parse(sJSON)));
			getCollection(m_sThisCollection).updateOne(filter, update);

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }
    
    /**
     * Clean the queue of Process Workpsace deleting all the created ones
     * @return True of False in case of exception
     */
    public boolean cleanQueue() {

        try {
        	
        	Utils.debugLog("Cleaning ProcessWorkspace Queue");
        	String sJSON = "{\"status\":\"ERROR\"}";
            Document oFilter = new Document("status", "CREATED");
			Document oUpdate = new Document("$set", new Document(Document.parse(sJSON)));
			getCollection(m_sThisCollection).updateMany(oFilter, oUpdate);

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    /**
     * Check if a Process Workpsace with that pid exists
     * @param iPid
     * @return
     */
    public boolean existsPidProcessWorkspace(Integer iPid) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        boolean bExists = false;
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("pid", iPid)));

            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        if (aoReturnList.size() > 0)
            bExists = true;

        return bExists;
    }

    /**
     * Get the list of all running (including Waiting and Ready) process worksapces
     * @return List of RUNING, WAITING and READY Process Workspaces
     */
    public List<ProcessWorkspace> getRunningSummary() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            		Filters.or(
            				Filters.eq("status", ProcessStatus.RUNNING.name()),
            				Filters.eq("status", ProcessStatus.WAITING.name()),
            				Filters.eq("status", ProcessStatus.READY.name())
            				)
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get a list of all the CREATED processes
     * @return
     */
    public List<ProcessWorkspace> getCreatedSummary() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
            			Filters.eq("status", ProcessStatus.CREATED.name())
            		)
            		.sort(new Document("operationDate", -1));
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    /**
     * Get the list of all processes
     * @return
     */
    public List<ProcessWorkspace> getList() {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {
            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();
            fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

	/**
	 * Check if the user is the owner of the process workspace
	 * @param sUserId a valid user id
	 * @param sProcessObjId a valid process obj id
	 * @return true if the user launched the process, false otherwise
	 */
	public boolean isProcessOwnedByUser(String sUserId, String sProcessObjId) {
		try {
			Document oDoc = getCollection(m_sThisCollection).find(Filters.and(
					Filters.eq("userId", sUserId),
					Filters.eq("processObjId", sProcessObjId)
					)).first();
			if(null!=oDoc) {
				return true;
			}
		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceRepository.isProcessOwnedByUser( " + sUserId + ", " + sProcessObjId + " ): " + oE);
		}
		return false;
	}
	
	/**
	 * @param sProcessObjId a valid process obj id
	 * @return the corresponding workspace id, if found, null otherwise
	 */
	public String getWorkspaceByProcessObjId(String sProcessObjId) {
		try {
			Document oDoc = getCollection(m_sThisCollection).find(
					Filters.eq("processObjId", sProcessObjId)
					).first();
			if(null==oDoc) {
				Utils.debugLog("ProcessWorkspaceRepository.getWorkspaceByProcessObjId: " + sProcessObjId + " is not a valid process obj id, aborting");
				return null;
			}
			if(oDoc.containsKey("workspaceId")) {
				String sWorkspaceId = oDoc.getString("workspaceId");
				return sWorkspaceId;
			}
		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceRepository.getWorkspaceByProcessObjId( " + sProcessObjId + " ): " + oE);
		}
		return null;
	}

	
	/**
	 * @param sProcessObjId a valid process obj id
	 * @return the corresponding payload
	 */
	public String getPayload(String sProcessObjId) {
		try {
			Document oDoc = getCollection(m_sThisCollection).find(
					Filters.eq("processObjId", sProcessObjId)
					).first();
			if(null==oDoc) {
				Utils.debugLog("ProcessWorkspaceRepository.getPayload: " + sProcessObjId + " is not a valid process obj id, aborting");
				return null;
			}
			if(oDoc.containsKey("payload")) {
				String sPayload = oDoc.getString("payload");
				return sPayload;
			}
		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceRepository.getPayload( " + sProcessObjId + " ): " + oE);
		}
		return null;
	}

	
	/**
	 * Get the list of processes in a specific node. WARNING: use this method for
	 * maintenance, since if you need to further filter the results it can be pretty
	 * inefficient, so prefer other more specific queries
	 * 
	 * @param sComputingNodeCode computing node
	 * @return
	 */
	public List<ProcessWorkspace> getByNode(String sComputingNodeCode) {
		final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
					Filters.eq("nodeCode", sComputingNodeCode))
					.sort(new Document("operationDate", -1));
			fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}
	
	/**
     *  Get List of Process Workspaces in a Workspace
     * @param sWorkspaceId Workspace
     * @param eStatus State
     * @param eOperation Op type
     * @param sProductNameSubstring Product Name
     * @param oDateFRom Start Date
     * @param oDateTo End Date
     * @return Found processworkspaces with applied filters
     */
    public List<ProcessWorkspace> getProcessByParentId(String sParentId) {

        final ArrayList<ProcessWorkspace> aoReturnList = new ArrayList<ProcessWorkspace>();
        try {

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(
					Filters.eq("parentId", sParentId))
					.sort(new Document("operationDate", -1));
			fillList(aoReturnList, oWSDocuments, ProcessWorkspace.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }	

}
