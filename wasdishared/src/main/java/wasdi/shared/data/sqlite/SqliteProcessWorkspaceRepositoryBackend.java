package wasdi.shared.data.sqlite;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import wasdi.shared.LauncherOperations;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.aggregators.ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult;
import wasdi.shared.business.aggregators.ProcessWorkspaceAggregatorBySubscriptionAndProject;
import wasdi.shared.data.interfaces.IProcessWorkspaceRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Created by s.adamo on 31/01/2017.
 */
/**
 * @author s.adamo
 * @author c.nattero
 * @author p.campanella
 *
 */
public class SqliteProcessWorkspaceRepositoryBackend extends SqliteRepository implements IProcessWorkspaceRepositoryBackend {
	
	public SqliteProcessWorkspaceRepositoryBackend() {
		m_sThisCollection = "processworkpsace";
		this.ensureTable(m_sThisCollection);
	}

	/**
	 * Insert a new Process Workspace 
	 * @param oProcessWorkspace Process Workpsace to insert
	 * @return processObjId
	 */
    public String insertProcessWorkspace(ProcessWorkspace oProcessWorkspace) {
    	
    	if (oProcessWorkspace == null) return "";

        try {
        	oProcessWorkspace.setLastStateChangeTimestamp(Utils.nowInMillis());
            insert(oProcessWorkspace.getProcessObjId(), oProcessWorkspace);
            return oProcessWorkspace.getProcessObjId();
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.insertProcessWorkspace: exception ", oEx);
        }

        return "";
    }
    
	/**
	 * Insert a list of Process Workspace Objects 
	 * @param aoProcessWorkspace Process Workpsace list to insert
	 */
    public void insertProcessListWorkspace(List<ProcessWorkspace> aoProcessWorkspace) {
    	
    	if (aoProcessWorkspace == null) return;

        try {
        	for (ProcessWorkspace oProcessWorkspace : aoProcessWorkspace) {
        		oProcessWorkspace.setLastStateChangeTimestamp(Utils.nowInMillis());
        		insert(oProcessWorkspace.getProcessObjId(), oProcessWorkspace);
			}
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.insertProcessListWorkspace: exception ", oEx);
        }
    }

    /**
     * Delete an Entity by processObjId
     * @param sId processObjId
     * @return
     */
    public boolean deleteProcessWorkspace(String sId) { 
    	
    	if (Utils.isNullOrEmpty(sId)) return false;

        try {
            deleteById(sId);
            return true;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.deleteProcessWorkspace: exception ", oEx);
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
            execute("DELETE FROM " + m_sThisCollection + " WHERE json_extract(data,'$.pid') = ?",
            		new Object[]{iPid});
            return true;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.deleteProcessWorkspaceByPid: exception ", oEx);
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
                deleteWhere("processObjId", sProcessObjId);
                return true;
            } catch (Exception oEx) {
            	WasdiLog.errorLog("ProcessWorkspaceRepository.deleteProcessWorkspaceByProcessObjId: exception ", oEx);
            }    		
    	}

        return false;
    }
    
    /**
     * Delete Process Workspace by Workspace ID
     * @param sWorkspaceId
     * @return
     */
    public boolean deleteProcessWorkspaceByWorkspaceId(String sWorkspaceId) {

    	if(!Utils.isNullOrEmpty(sWorkspaceId)) {
            try {
                deleteWhere("workspaceId", sWorkspaceId);
                return true;
            } catch (Exception oEx) {
            	WasdiLog.errorLog("ProcessWorkspaceRepository.deleteProcessWorkspaceByWorkspaceId: exception ", oEx);
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
    		WasdiLog.debugLog("ProcessWorkspaceRepository.getRoots: list of children is null, aborting");
    		return new ArrayList<ProcessWorkspace>(0);
    	}
    	if(aoChildren.size()<=0) {
    		WasdiLog.debugLog("ProcessWorkspaceRepository.getRoots: list of children is empty, aborting");
    		return new ArrayList<ProcessWorkspace>(0);
    	}
    	try {
    		List<ProcessWorkspace> aoFathers = getFathers(aoChildren);

    		List<ProcessWorkspace> aoRoots = new LinkedList<>();
			for (ProcessWorkspace oFather : aoFathers) {
				if(null==oFather) {
					continue;
				}
				if(null == oFather.getParentId()) {
					aoRoots.add(oFather);
				}
			}
    		return aoFathers;
		} catch (Exception oE) {
			WasdiLog.errorLog("ProcessWorkspaceRepository.getRoots: ", oE);
		}
    	return new ArrayList<ProcessWorkspace>(0);
    }

    /**
     * Retrieves the hierarchy of fathers of a list of processes.
     * For SQLite, performs iterative parent lookup since graphLookup is not available.
     * @param aoChildProcesses a list of child processes
     * @return a list of fathers
     */
    public List<ProcessWorkspace> getFathers(List<ProcessWorkspace> aoChildProcesses){

    	if(null == aoChildProcesses) {
    		WasdiLog.debugLog("ProcessWorkspaceRepository.getFathers: the list is null, aborting");
    		return new LinkedList<ProcessWorkspace>();
    	}
    	if(aoChildProcesses.size() <= 0) {
    		WasdiLog.debugLog("ProcessWorkspaceRepository.getFathers: the list has size 0, aborting");
    		return new LinkedList<ProcessWorkspace>();
    	}

    	List<String> asChildrenProcessObjIds = new ArrayList<String>(aoChildProcesses.size());
    	try {
    		for (ProcessWorkspace oChildProcess : aoChildProcesses) {
				if(null==oChildProcess) continue;
				if(Utils.isNullOrEmpty(oChildProcess.getProcessObjId())) continue;
				asChildrenProcessObjIds.add(oChildProcess.getProcessObjId());
			}
    	} catch (Exception oE) {
    		WasdiLog.errorLog("ProcessWorkspaceRepository.getFathers: extracting processObjIds failed due to: ", oE);
    		return new LinkedList<ProcessWorkspace>();
    	}

    	// Iteratively walk up via parentId
    	List<ProcessWorkspace> aoFathers = new LinkedList<ProcessWorkspace>();
    	try {
    		for (String sChildId : asChildrenProcessObjIds) {
    			ProcessWorkspace oChild = findOneWhere("processObjId", sChildId, ProcessWorkspace.class);
    			if (oChild == null) continue;
    			String sParentId = oChild.getParentId();
    			while (!Utils.isNullOrEmpty(sParentId)) {
    				ProcessWorkspace oParent = findOneWhere("processObjId", sParentId, ProcessWorkspace.class);
    				if (oParent == null) break;
    				aoFathers.add(oParent);
    				sParentId = oParent.getParentId();
    			}
    		}
		} catch (Exception oE) {
			WasdiLog.errorLog("ProcessWorkspaceRepository.getFathers: lookup failed due to ", oE);
		}
    	return aoFathers;
    }

    /**
     * Get List of Process Workspaces in a Workspace filtering by desired process status
     * @param sWorkspaceId unique id of the workspace
     * @param aeDesiredStatuses a list of desired statuses
     */
    public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, List<ProcessStatus> aeDesiredStatuses) {
        if(Utils.isNullOrEmpty(sWorkspaceId)) {
        	WasdiLog.debugLog("ProcessWorkspaceRepository.getProcessByWorkspace( \"" + sWorkspaceId + "\", aeDesiredStatuses ): sWorkspaceId null or empty, aborting");
        	return new ArrayList<>();
        }
        if(null == aeDesiredStatuses) {
        	WasdiLog.debugLog("ProcessWorkspaceRepository.getProcessByWorkspace( \"" + sWorkspaceId + "\", null ): aeDesiredStatuses is null, aborting");
        	return new ArrayList<>();
        }
        if(aeDesiredStatuses.size() <= 0) {
        	WasdiLog.debugLog("ProcessWorkspaceRepository.getProcessByWorkspace( \"" + sWorkspaceId + "\", aeDesiredStatuses ): aeDesiredStatuses is empty, returning empty list");
        	return new ArrayList<>();
        }

        try {
        	List<String> asDesiredStatuses = new ArrayList<>(aeDesiredStatuses.size());
        	for (ProcessStatus eStatus : aeDesiredStatuses) {
        		if(null!=eStatus && !Utils.isNullOrEmpty(eStatus.name())) {
        			asDesiredStatuses.add(eStatus.name());
        		}
        	}
        	
        	if (asDesiredStatuses.isEmpty()) return new ArrayList<>();
        	
        	StringBuilder oSql = new StringBuilder("SELECT data FROM ").append(m_sThisCollection)
        			.append(" WHERE json_extract(data,'$.workspaceId') = ?");
        	
        	oSql.append(" AND json_extract(data,'$.status') IN (");
        	for (int i = 0; i < asDesiredStatuses.size(); i++) {
        		if (i > 0) oSql.append(",");
        		oSql.append("?");
        	}
        	oSql.append(") ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC");
        	
        	List<Object> aoParams = new ArrayList<>();
        	aoParams.add(sWorkspaceId);
        	aoParams.addAll(asDesiredStatuses);
        	
			return queryList(oSql.toString(), aoParams.toArray(), ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessByWorkspace: exception ", oEx);
        }

		return new ArrayList<>();
    }



    /**
     * Gets the list of processObjIds from a given workspace
     * @param sWorkspaceId the workspace id
     * @return the list of processObjIds in given workspace
     */
    public List<String> getProcessObjIdsFromWorkspaceId(String sWorkspaceId){
    	if(Utils.isNullOrEmpty(sWorkspaceId)) {
    		WasdiLog.debugLog("ProcessWorkspaceRepository.getProcessObjIdsFromWorkspaceId: workspace id null or empty, aborting");
    		return new ArrayList<String>(0);
    	}

    	try {
    		List<ProcessWorkspace> aoList = queryList(
    				"SELECT data FROM " + m_sThisCollection + " WHERE json_extract(data,'$.workspaceId') = ?" +
    				" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
    				new Object[]{sWorkspaceId}, ProcessWorkspace.class);
    		
    		List<String> asProcessObjIds = new ArrayList<String>(aoList.size());
    		for (ProcessWorkspace oPW : aoList) {
    			if (oPW != null && !Utils.isNullOrEmpty(oPW.getProcessObjId())) {
    				asProcessObjIds.add(oPW.getProcessObjId());
    			}
    		}
    		return asProcessObjIds;
    	} catch (Exception oEx) {
    		WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessObjIdsFromWorkspaceId: exception ", oEx);
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
     * @param oDateFrom Start Date
     * @param oDateTo End Date
     * @return Found processworkspaces with applied filters
     */
    public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, ProcessStatus eStatus, LauncherOperations eOperation, String sProductNameSubstring, Instant oDateFrom, Instant oDateTo) {

        try {
        	String sSql = buildFilterSql(sWorkspaceId, eStatus, eOperation, sProductNameSubstring, oDateFrom, oDateTo, false, 0, 0);
        	Object[] aoParams = buildFilterParams(sWorkspaceId, eStatus, eOperation, sProductNameSubstring, oDateFrom, oDateTo);
        	return queryList(sSql, aoParams, ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessByWorkspace: exception ", oEx);
        }

        return new ArrayList<>();
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
     * @param oDateFrom starting date (included)
     * @param oDateTo ending date (included)
     * @param iStartIndex start index for pagination
     * @param iEndIndex end index for pagination
     * @return list of results
     */
    public List<ProcessWorkspace> getProcessByWorkspace(String sWorkspaceId, ProcessStatus eStatus, LauncherOperations eOperation, String sProductNameSubstring, Instant oDateFrom, Instant oDateTo, int iStartIndex, int iEndIndex) {

        try {
        	String sSql = buildFilterSql(sWorkspaceId, eStatus, eOperation, sProductNameSubstring, oDateFrom, oDateTo, true, iStartIndex, iEndIndex);
        	List<Object> aoParamList = new ArrayList<>(java.util.Arrays.asList(buildFilterParams(sWorkspaceId, eStatus, eOperation, sProductNameSubstring, oDateFrom, oDateTo)));
        	int iLimit = iEndIndex - iStartIndex;
        	aoParamList.add(iLimit);
        	aoParamList.add(iStartIndex);
        	return queryList(sSql, aoParamList.toArray(), ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessByWorkspace: exception ", oEx);
        }

        return new ArrayList<>();
    }
    

	/**
	 * Build the SQL SELECT for workspace-filter queries
	 */
	private String buildFilterSql(String sWorkspaceId, ProcessStatus eStatus, LauncherOperations eOperation, String sProductNameSubstring, Instant oDateFrom, Instant oDateTo, boolean bPaginated, int iStartIndex, int iEndIndex) {
		StringBuilder oSql = new StringBuilder("SELECT data FROM ").append(m_sThisCollection);
		oSql.append(" WHERE json_extract(data,'$.workspaceId') = ?");
		if (eStatus != null) {
			oSql.append(" AND json_extract(data,'$.status') = ?");
		}
		if (eOperation != null) {
			oSql.append(" AND json_extract(data,'$.operationType') = ?");
		}
		if (!Utils.isNullOrEmpty(sProductNameSubstring)) {
			oSql.append(" AND json_extract(data,'$.productName') LIKE ?");
		}
		if (oDateFrom != null) {
			oSql.append(" AND json_extract(data,'$.operationDate') >= ?");
			oSql.append(" AND json_extract(data,'$.operationTimestamp') >= ?");
		}
		if (oDateTo != null) {
			oSql.append(" AND json_extract(data,'$.operationDate') <= ?");
			oSql.append(" AND json_extract(data,'$.operationTimestamp') <= ?");
		}
		oSql.append(" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC");
		if (bPaginated) {
			oSql.append(" LIMIT ? OFFSET ?");
		}
		return oSql.toString();
	}
	
	/**
	 * Build the params array for workspace-filter queries (without LIMIT/OFFSET)
	 */
	private Object[] buildFilterParams(String sWorkspaceId, ProcessStatus eStatus, LauncherOperations eOperation, String sProductNameSubstring, Instant oDateFrom, Instant oDateTo) {
		List<Object> aoParams = new ArrayList<>();
		aoParams.add(sWorkspaceId);
		if (eStatus != null) aoParams.add(eStatus.name());
		if (eOperation != null) aoParams.add(eOperation.name());
		if (!Utils.isNullOrEmpty(sProductNameSubstring)) aoParams.add("%" + sProductNameSubstring + "%");
		if (oDateFrom != null) {
			aoParams.add(oDateFrom.toString());
			aoParams.add(oDateFrom.toEpochMilli());
		}
		if (oDateTo != null) {
			aoParams.add(oDateTo.toString());
			aoParams.add(oDateTo.toEpochMilli());
		}
		return aoParams.toArray();
	}
	
	/**
	 * @param sProcessId the process id
	 * @return process status as a string, null if the process is not found
	 */
	public String getProcessStatusFromId(String sProcessId) {
		try {
			ProcessWorkspace oPW = findOneWhere("processObjId", sProcessId, ProcessWorkspace.class);
			if (oPW != null) return oPW.getStatus();
		} catch (Exception oE) {
			WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessStatusFromId( " + sProcessId + " ): ", oE );
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
    		return countWhere("workspaceId", sWorkspaceId);
    	} catch (Exception oEx) {
    		WasdiLog.errorLog("ProcessWorkspaceRepository.countByWorkspace: exception ", oEx);
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
    		return countWhere("productName", sProcessorName);
    	} catch (Exception oEx) {
    		WasdiLog.errorLog("ProcessWorkspaceRepository.countByProcessor: exception ", oEx);
		}
    	return 0;
    }
    
    
    /**
     * Get the list of process workspace for a user
     * @param sUserId
     * @return
     */
    public List<ProcessWorkspace> getProcessByUser(String sUserId) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection + " WHERE json_extract(data,'$.userId') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{sUserId}, ProcessWorkspace.class);

        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessByUser: exception ", oEx);
        }

        return new ArrayList<>();
    }

    /**
     * Get the list of the OGC Processes API triggered processworkspaces for a user
     * @param sUserId User Identifier
     * @return List of OGC Process Workspace of this user
     */
    public List<ProcessWorkspace> getOGCProcessByUser(String sUserId) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection + " WHERE json_extract(data,'$.userId') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{sUserId}, ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getOGCProcessByUser: exception ", oEx);
        }

        return new ArrayList<>();
    }

    
    /**
     * Get the list of created processes NOT Download or IDL
     * @return
     */
    public List<ProcessWorkspace> getCreatedProcesses() {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') != ?" +
        			" AND json_extract(data,'$.operationType') != ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{ProcessStatus.CREATED.name(), LauncherOperations.DOWNLOAD.name(), LauncherOperations.RUNIDL.name()},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getCreatedProcesses: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get the list of created processes NOT Download or IDL in a NODE
     * @param sComputingNodeCode
     * @return
     */
    public List<ProcessWorkspace> getCreatedProcessesByNode(String sComputingNodeCode) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') != ?" +
        			" AND json_extract(data,'$.operationType') != ?" +
        			" AND json_extract(data,'$.nodeCode') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{ProcessStatus.CREATED.name(), LauncherOperations.DOWNLOAD.name(), LauncherOperations.RUNIDL.name(), sComputingNodeCode},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getCreatedProcessesByNode: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get the list of processes not Download or IDL in ready state for a specific node
     * @param sComputingNodeCode
     * @return
     */
    public List<ProcessWorkspace> getReadyProcessesByNode(String sComputingNodeCode) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') != ?" +
        			" AND json_extract(data,'$.operationType') != ?" +
        			" AND json_extract(data,'$.nodeCode') = ?" +
        			" ORDER BY json_extract(data,'$.lastStateChangeTimestamp') DESC, json_extract(data,'$.lastStateChangeDate') DESC",
        			new Object[]{ProcessStatus.READY.name(), LauncherOperations.DOWNLOAD.name(), LauncherOperations.RUNIDL.name(), sComputingNodeCode},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getReadyProcessesByNode: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get the list of processes not download or idl in running state
     * @return
     */
    public List<ProcessWorkspace> getRunningProcesses() {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') != ?" +
        			" AND json_extract(data,'$.operationType') != ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{ProcessStatus.RUNNING.name(), LauncherOperations.DOWNLOAD.name(), LauncherOperations.RUNIDL.name()},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getRunningProcesses: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get the list of processes not download or idl in running state in a specific node
     * @param sComputingNodeCode
     * @return
     */
    public List<ProcessWorkspace> getRunningProcessesByNode(String sComputingNodeCode) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') != ?" +
        			" AND json_extract(data,'$.operationType') != ?" +
        			" AND json_extract(data,'$.nodeCode') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{ProcessStatus.RUNNING.name(), LauncherOperations.DOWNLOAD.name(), LauncherOperations.RUNIDL.name(), sComputingNodeCode},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getRunningProcessesByNode: exception ", oEx);
        }

        return new ArrayList<>();
    }    
    
    /**
     * Get the list of created download processes
     * @return
     */
    public List<ProcessWorkspace> getCreatedDownloads() {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{ProcessStatus.CREATED.name(), LauncherOperations.DOWNLOAD.name()},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getCreatedDownloads: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get the list of created download processes in specific node
     * @param sComputingNodeCode
     * @return
     */
    public List<ProcessWorkspace> getCreatedDownloadsByNode(String sComputingNodeCode) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') = ?" +
        			" AND json_extract(data,'$.nodeCode') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{ProcessStatus.CREATED.name(), LauncherOperations.DOWNLOAD.name(), sComputingNodeCode},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getCreatedDownloadsByNode: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get the list of created idl processes
     * @return
     */
    public List<ProcessWorkspace> getCreatedIDL() {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{ProcessStatus.CREATED.name(), LauncherOperations.RUNIDL.name()},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getCreatedIDL: exception ", oEx);
        }

        return new ArrayList<>();
    }

    /**
     * Get the list of created idl processes in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getCreatedIDLByNode(String sComputingNode) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') = ?" +
        			" AND json_extract(data,'$.nodeCode') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{ProcessStatus.CREATED.name(), LauncherOperations.RUNIDL.name(), sComputingNode},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getCreatedIDLByNode: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get the list of running download processes
     * @return
     */
    public List<ProcessWorkspace> getRunningDownloads() {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{ProcessStatus.RUNNING.name(), LauncherOperations.DOWNLOAD.name()},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getRunningDownloads: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get the list of running download processes in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getRunningDownloadsByNode(String sComputingNode) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') = ?" +
        			" AND json_extract(data,'$.nodeCode') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{ProcessStatus.RUNNING.name(), LauncherOperations.DOWNLOAD.name(), sComputingNode},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getRunningDownloadsByNode: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get the list of ready download processes
     * @return
     */
    public List<ProcessWorkspace> getReadyDownloads() {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') = ?" +
        			" ORDER BY json_extract(data,'$.lastStateChangeTimestamp') DESC, json_extract(data,'$.lastStateChangeDate') DESC",
        			new Object[]{ProcessStatus.READY.name(), LauncherOperations.DOWNLOAD.name()},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getReadyDownloads: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get the list of ready download processes in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getReadyDownloadsByNode(String sComputingNode) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') = ?" +
        			" AND json_extract(data,'$.nodeCode') = ?" +
        			" ORDER BY json_extract(data,'$.lastStateChangeTimestamp') DESC, json_extract(data,'$.lastStateChangeDate') DESC",
        			new Object[]{ProcessStatus.READY.name(), LauncherOperations.DOWNLOAD.name(), sComputingNode},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getReadyDownloadsByNode: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    
    /**
     * Get the list of running IDL
     * @return
     */
    public List<ProcessWorkspace> getRunningIDL() {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{ProcessStatus.RUNNING.name(), LauncherOperations.RUNIDL.name()},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getRunningIDL: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get the list of running IDL in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getRunningIDLByNode(String sComputingNode) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') = ?" +
        			" AND json_extract(data,'$.nodeCode') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{ProcessStatus.RUNNING.name(), LauncherOperations.RUNIDL.name(), sComputingNode},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getRunningIDLByNode: exception ", oEx);
        }

        return new ArrayList<>();
    }

    /**
     * Get the list of ready IDL
     * @return
     */
    public List<ProcessWorkspace> getReadyIDL() {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') = ?" +
        			" ORDER BY json_extract(data,'$.lastStateChangeTimestamp') DESC, json_extract(data,'$.lastStateChangeDate') DESC",
        			new Object[]{ProcessStatus.READY.name(), LauncherOperations.RUNIDL.name()},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getReadyIDL: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get the list of ready IDL in a specific node
     * @param sComputingNode
     * @return
     */
    public List<ProcessWorkspace> getReadyIDLByNode(String sComputingNode) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = ?" +
        			" AND json_extract(data,'$.operationType') = ?" +
        			" AND json_extract(data,'$.nodeCode') = ?" +
        			" ORDER BY json_extract(data,'$.lastStateChangeTimestamp') DESC, json_extract(data,'$.lastStateChangeDate') DESC",
        			new Object[]{ProcessStatus.READY.name(), LauncherOperations.RUNIDL.name(), sComputingNode},
        			ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getReadyIDLByNode: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    
    /**
     * Get the list of processes in a specific state in a specific node
     * @param sProcessStatus status of the process
     * @param sComputingNodeCode computing node
     * @return
     */    
    public List<ProcessWorkspace> getProcessesByStateNode(String sProcessStatus, String sComputingNodeCode) {
    	return getProcessesByStateNode(sProcessStatus, sComputingNodeCode, "operationTimestamp", "operationDate");
    }
    
    
    /**
     * Get the list of processes in a specific state in a specific node
     * @param sProcessStatus status of the process
     * @param sComputingNodeCode computing node
     * @param sOrderByTimestamp timestamp field to order by
     * @param sOrderByDate date field to order by (null for single sort)
     * @return
     */        
    public List<ProcessWorkspace> getProcessesByStateNode(String sProcessStatus, String sComputingNodeCode, String sOrderByTimestamp, String sOrderByDate) {

        try {
        	StringBuilder oSql = new StringBuilder("SELECT data FROM ").append(m_sThisCollection)
        			.append(" WHERE json_extract(data,'$.status') = ?")
        			.append(" AND json_extract(data,'$.nodeCode') = ?")
        			.append(" ORDER BY json_extract(data,'$.").append(sOrderByTimestamp).append("') DESC");
        	if (sOrderByDate != null) {
        		oSql.append(", json_extract(data,'$.").append(sOrderByDate).append("') DESC");
        	}
        	return queryList(oSql.toString(), new Object[]{sProcessStatus, sComputingNodeCode}, ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessesByStateNode: exception ", oEx);
        }

        return new ArrayList<>();
    }    
    
    
    /**
     * Get processes for the scheduler in a specific node (excludes DONE, ERROR, STOPPED)
     * @param sComputingNodeCode computing node
     * @param sOrderBy field to order by
     * @return
     */        
    public List<ProcessWorkspace> getProcessesForSchedulerNode(String sComputingNodeCode, String sOrderBy) {

        try {
        	String sSql = "SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') != 'DONE'" +
        			" AND json_extract(data,'$.status') != 'ERROR'" +
        			" AND json_extract(data,'$.status') != 'STOPPED'" +
        			" AND json_extract(data,'$.nodeCode') = ?" +
        			" ORDER BY json_extract(data,'$." + sOrderBy + "') DESC";
        	List<ProcessWorkspace> aoReturnList = queryList(sSql, new Object[]{sComputingNodeCode}, ProcessWorkspace.class);
            
            if (WasdiConfig.Current.scheduler.lastStateChangeDateOrderBy == 1) {
            	Collections.reverse(aoReturnList);
            }
            return aoReturnList;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessesForSchedulerNode: exception ", oEx);
        }
        
        return new ArrayList<>();
    }    
    
	/**
	 * Get the last 5 Process Workpsace
	 * @param sWorkspaceId
	 * @return
	 */
    public List<ProcessWorkspace> getLastProcessByWorkspace(String sWorkspaceId) {

        try {
        	List<ProcessWorkspace> aoReturnList = queryList(
        			"SELECT data FROM " + m_sThisCollection + " WHERE json_extract(data,'$.workspaceId') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC LIMIT 5",
        			new Object[]{sWorkspaceId}, ProcessWorkspace.class);

            // The client expects the most recent last
            Collections.reverse(aoReturnList);
            return aoReturnList;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getLastProcessByWorkspace: exception ", oEx);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Get the last 5 Process Workspace of a user
     * @param sUserId
     * @return
     */
    public List<ProcessWorkspace> getLastProcessByUser(String sUserId) {

        try {
        	List<ProcessWorkspace> aoReturnList = queryList(
        			"SELECT data FROM " + m_sThisCollection + " WHERE json_extract(data,'$.userId') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC LIMIT 5",
        			new Object[]{sUserId}, ProcessWorkspace.class);
        	
        	// The client expects the most recent last
        	Collections.reverse(aoReturnList);
        	return aoReturnList;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getLastProcessByUser: exception ", oEx);
        }
        
        return new ArrayList<>();
    }
    
    
    /**
     * Get the processes older that the timestamp specified
     * @param dTimestamp timestamp of the limit. It will return all the proc ws LATER than this timestamp
     * @return
     */
    public List<ProcessWorkspace> getProcessOlderThan(Long dTimestamp) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.operationTimestamp') > ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') ASC",
        			new Object[]{dTimestamp}, ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessOlderThan: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get a Process Workpsace from product name
     * @param sProductName
     * @return
     */
    public ArrayList<ProcessWorkspace> getProcessByProductName(String sProductName) {
        try {
        	return new ArrayList<>(queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.productName') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{sProductName}, ProcessWorkspace.class));
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessByProductName: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get a list of Process Workpsace by Product name in a Workpsace
     * @param sProductName Product Name to search
     * @param sWorkspace Workpsace
     * @return List of found proc ws
     */
    public List<ProcessWorkspace> getProcessByProductNameAndWorkspace(String sProductName, String sWorkspace) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.productName') = ?" +
        			" AND json_extract(data,'$.workspaceId') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{sProductName, sWorkspace}, ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessByProductNameAndWorkspace: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get a list of Process Workpsace by Product name and User Id
     * @param sProductName Product Name to search
     * @param sUserId User Id
     * @return List of found proc ws
     */
    public List<ProcessWorkspace> getProcessByProductNameAndUserId(String sProductName, String sUserId) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.productName') = ?" +
        			" AND json_extract(data,'$.userId') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{sProductName, sUserId}, ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessByProductNameAndUserId: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    
    /**
     * Get a Process Workpsace by the WASDI Id
     * @param sProcessObjId Process Workspace Id
     * @return Entity found or null
     */
    public ProcessWorkspace getProcessByProcessObjId(String sProcessObjId) {
        try {
        	return findOneWhere("processObjId", sProcessObjId, ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessByProcessObjId: exception ", oEx);
        }
        return null;
    }
    
    /**
     * Get the status of a list of process workspace
     * @param asProcessesObjId List of string each with a ProcessWorkspace Id to query
     * @return List of string, corresponding to the input one, with the status of the relative proc id
     */
    public ArrayList<String> getProcessesStatusByProcessObjId(ArrayList<String> asProcessesObjId) {
    	
    	ArrayList<String> asReturnStatus = new ArrayList<>();
        try {
        	if (asProcessesObjId == null || asProcessesObjId.isEmpty()) return asReturnStatus;

        	// Build IN query
        	StringBuilder oSql = new StringBuilder("SELECT data FROM ").append(m_sThisCollection).append(" WHERE json_extract(data,'$.processObjId') IN (");
        	for (int i = 0; i < asProcessesObjId.size(); i++) {
        		if (i > 0) oSql.append(',');
        		oSql.append('?');
        	}
        	oSql.append(')');
        	
        	List<ProcessWorkspace> aoProcessesList = queryList(oSql.toString(), asProcessesObjId.toArray(), ProcessWorkspace.class);
        	
        	// Build a lookup map
        	Map<String, String> oStatusMap = new HashMap<>();
        	for (ProcessWorkspace oPw : aoProcessesList) {
        		oStatusMap.put(oPw.getProcessObjId(), oPw.getStatus());
        	}
        	
        	// Return in the requested order
            for (String sId : asProcessesObjId) {
            	String sStatus = oStatusMap.get(sId);
            	asReturnStatus.add(sStatus != null ? sStatus : ProcessStatus.ERROR.name());
            }
        } 
        catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessesStatusByProcessObjId: exception ", oEx);
        }

        return asReturnStatus;
    }
    
    /**
     * Update a process Workspace. If there is a state change, the system will update also update lastStateChangeTimestamp 
     * @param oProcessWorkspace Process Workspace to update
     * @return
     */
    public boolean updateProcess(ProcessWorkspace oProcessWorkspace) {

        try {
        	ProcessWorkspace oOriginal = getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());
        	
        	if (oOriginal != null && !oOriginal.getStatus().equals(oProcessWorkspace.getStatus())) {
        		double dNowInMillis = Utils.nowInMillis();
        		oProcessWorkspace.setLastStateChangeTimestamp(dNowInMillis);

            	if (oOriginal.getStatus().equalsIgnoreCase("RUNNING")) {
            		double dLastChange = oOriginal.getLastStateChangeTimestamp();
            		long lDelta = (long) dNowInMillis - (long) dLastChange;
            		oProcessWorkspace.setRunningTime(oOriginal.getRunningTime() + lDelta);
            	}
        	}
        	
        	updateById(oProcessWorkspace.getProcessObjId(), oProcessWorkspace);
            return true;

        } catch (Exception oEx) {
            WasdiLog.errorLog("ProcessWorkspaceRepository.updateProcess: exception ", oEx);
        }

        return false;
    }
    
    /**
     * Clean the queue of Process Workpsace deleting all the created ones
     * @return True of False in case of exception
     */
    public boolean cleanQueue() {

        try {
        	WasdiLog.debugLog("Cleaning ProcessWorkspace Queue");
        	List<ProcessWorkspace> aoCreated = findAllWhere("status", ProcessStatus.CREATED.name(), ProcessWorkspace.class);
        	for (ProcessWorkspace oPw : aoCreated) {
        		oPw.setStatus(ProcessStatus.ERROR.name());
        		updateById(oPw.getProcessObjId(), oPw);
        	}
            return true;

        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.cleanQueue: exception ", oEx);
        }

        return false;
    }
    
    public boolean cleanPastProcessWorkspaces() {
        
        try {
            execute("DELETE FROM " + m_sThisCollection + " WHERE json_extract(data,'$.status') IN (?,?,?)",
                    new Object[]{ProcessStatus.ERROR.name(), ProcessStatus.STOPPED.name(), ProcessStatus.DONE.name()});
            return true;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.cleanPastProcessWorkspaces: exception ", oEx);
        }        

        return false;    	
    }

    /**
     * Check if a Process Workpsace with that pid exists
     * @param iPid
     * @return
     */
    public boolean existsPidProcessWorkspace(Integer iPid) {

        try (java.sql.Connection oConn = getConnection();
				 java.sql.PreparedStatement oPs = oConn.prepareStatement(
						"SELECT COUNT(*) FROM " + m_sThisCollection + " WHERE json_extract(data,'$.pid') = ?")) {
			oPs.setInt(1, iPid != null ? iPid : -1);
			try (java.sql.ResultSet oRs = oPs.executeQuery()) {
				if (oRs.next()) {
					return oRs.getLong(1) > 0;
				}
			}
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.existsPidProcessWorkspace: exception ", oEx);
        }

        return false;
    }

    /**
     * Get the count of all running (including Waiting and Ready) process worksapces
     * @param sUserId
     * @param bIncluded
     * @return
     */
    public long getRunningCountByUser(String sUserId, boolean bIncluded, String sWorkspaceId) {

        try {
        	StringBuilder oSql = new StringBuilder("SELECT COUNT(*) FROM ").append(m_sThisCollection)
        			.append(" WHERE json_extract(data,'$.status') IN ('RUNNING','WAITING','READY')")
        			.append(" AND json_extract(data,'$.userId') ").append(bIncluded ? "= ?" : "!= ?");
        	
        	List<Object> oParams = new ArrayList<>();
        	oParams.add(sUserId);

        	if (!Utils.isNullOrEmpty(sWorkspaceId)) {
        		oSql.append(" AND json_extract(data,'$.workspaceId') = ?");
        		oParams.add(sWorkspaceId);
        	}
        	
			try (java.sql.Connection oConn = getConnection();
					 java.sql.PreparedStatement oPs = oConn.prepareStatement(oSql.toString())) {
				for (int i = 0; i < oParams.size(); i++) {
					oPs.setObject(i + 1, oParams.get(i));
				}
				try (java.sql.ResultSet oRs = oPs.executeQuery()) {
					if (oRs.next()) {
						return oRs.getLong(1);
					}
				}
			}
			return 0L;
            
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getRunningCountByUser: exception ", oEx);
        }

        return 0L;
    }

    
    /**
     * Get the list of all running (including Waiting and Ready) process worksapces
     * @return List of RUNING, WAITING and READY Process Workspaces
     */
    public List<ProcessWorkspace> getRunningSummary() {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') IN ('RUNNING','WAITING','READY')" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{}, ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getRunningSummary: exception ", oEx);
        }

        return new ArrayList<>();
    }

    /**
     * Get the count of all created processes that are of or not of a User
     * @param sUserId User Filter
     * @param bIncluded True returns the total of this user, False returns the total not of this user
     * @return Long 
     */
    public long getCreatedCountByUser(String sUserId, boolean bIncluded, String sWorkspaceId) {

        try {
        	StringBuilder oSql = new StringBuilder("SELECT COUNT(*) FROM ").append(m_sThisCollection)
        			.append(" WHERE json_extract(data,'$.status') = 'CREATED'")
        			.append(" AND json_extract(data,'$.userId') ").append(bIncluded ? "= ?" : "!= ?");

        	List<Object> oParams = new ArrayList<>();
        	oParams.add(sUserId);

        	if (!Utils.isNullOrEmpty(sWorkspaceId)) {
        		oSql.append(" AND json_extract(data,'$.workspaceId') = ?");
        		oParams.add(sWorkspaceId);
        	}

			try (java.sql.Connection oConn = getConnection();
					 java.sql.PreparedStatement oPs = oConn.prepareStatement(oSql.toString())) {
				for (int i = 0; i < oParams.size(); i++) {
					oPs.setObject(i + 1, oParams.get(i));
				}
				try (java.sql.ResultSet oRs = oPs.executeQuery()) {
					if (oRs.next()) {
						return oRs.getLong(1);
					}
				}
			}
			return 0L;

        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getCreatedCountByUser: exception ", oEx);
        }

        return 0L;
    }    
    
    /**
     * Get a list of all the CREATED processes
     * @return
     */
    public List<ProcessWorkspace> getCreatedSummary() {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.status') = 'CREATED'" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{}, ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getCreatedSummary: exception ", oEx);
        }

        return new ArrayList<>();
    }
    
    /**
     * Get the list of all processes
     * @return
     */
    public List<ProcessWorkspace> getList() {

        try {
        	return findAll(ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getList: exception ", oEx);
        }

        return new ArrayList<>();
    }

	/**
	 * Check if the user is the owner of the process workspace
	 * @param sUserId a valid user id
	 * @param sProcessObjId a valid process obj id
	 * @return true if the user launched the process, false otherwise
	 */
	public boolean isProcessOwnedByUser(String sUserId, String sProcessObjId) {
		try {
			Map<String, Object> oFilter = new HashMap<>();
			oFilter.put("userId", sUserId);
			oFilter.put("processObjId", sProcessObjId);
			return countWhere(oFilter) > 0;
		} catch (Exception oE) {
			WasdiLog.errorLog("ProcessWorkspaceRepository.isProcessOwnedByUser( " + sUserId + ", " + sProcessObjId + " ): ", oE);
		}
		return false;
	}
	
	/**
	 * @param sProcessObjId a valid process obj id
	 * @return the corresponding workspace id, if found, null otherwise
	 */
	public String getWorkspaceByProcessObjId(String sProcessObjId) {
		try {
			ProcessWorkspace oPw = findOneWhere("processObjId", sProcessObjId, ProcessWorkspace.class);
			if (oPw == null) {
				WasdiLog.debugLog("ProcessWorkspaceRepository.getWorkspaceByProcessObjId: " + sProcessObjId + " is not a valid process obj id, aborting");
				return null;
			}
			return oPw.getWorkspaceId();
		} catch (Exception oE) {
			WasdiLog.errorLog("ProcessWorkspaceRepository.getWorkspaceByProcessObjId( " + sProcessObjId + " ): " , oE);
		}
		return null;
	}

	
	/**
	 * @param sProcessObjId a valid process obj id
	 * @return the corresponding payload
	 */
	public String getPayload(String sProcessObjId) {
		try {
			ProcessWorkspace oPw = findOneWhere("processObjId", sProcessObjId, ProcessWorkspace.class);
			if (oPw == null) {
				WasdiLog.debugLog("ProcessWorkspaceRepository.getPayload: " + sProcessObjId + " is not a valid process obj id, aborting");
				return null;
			}
			return oPw.getPayload();
		} catch (Exception oE) {
			WasdiLog.errorLog("ProcessWorkspaceRepository.getPayload( " + sProcessObjId + " ): ", oE);
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
		try {
			return queryList(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.nodeCode') = ?" +
					" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
					new Object[]{sComputingNodeCode}, ProcessWorkspace.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceRepository.getByNode: exception ", oEx);
		}

		return new ArrayList<>();
	}
	
	public List<ProcessWorkspace> getByNodeUnsorted(String sComputingNodeCode) {
		try {
			return findAllWhere("nodeCode", sComputingNodeCode, ProcessWorkspace.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceRepository.getByNodeUnsorted: exception ", oEx);
		}

		return new ArrayList<>();
	}

	public List<ProcessWorkspace> getUnfinishedByNode(String sComputingNodeCode) {
		try {
			return queryList(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.nodeCode') = ?" +
					" AND json_extract(data,'$.status') IN ('WAITING','READY','CREATED','RUNNING')" +
					" ORDER BY json_extract(data,'$.status') ASC, json_extract(data,'$.operationType') ASC, json_extract(data,'$.operationSubType') ASC",
					new Object[]{sComputingNodeCode}, ProcessWorkspace.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceRepository.getUnfinishedByNode: exception ", oEx);
		}

		return new ArrayList<>();
	}

	public List<ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult> getQueuesByNodeAndStatuses(String sNodeCode, String... asStatuses) {
		List<ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult> aoReturnList = new ArrayList<>();

		if (asStatuses == null || asStatuses.length == 0) {
			return aoReturnList;
		}

		try (java.sql.Connection oConn = getConnection();
			 java.sql.PreparedStatement oPs = buildInQueryStatement(oConn, sNodeCode, asStatuses)) {

			try (java.sql.ResultSet oRs = oPs.executeQuery()) {
				while (oRs.next()) {
					ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult oResult =
							new ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult();
					oResult.setOperationType(oRs.getString("operationType"));
					oResult.setOperationSubType(oRs.getString("operationSubType"));
					oResult.setStatus(oRs.getString("status"));
					oResult.setCount(oRs.getInt("cnt"));
					aoReturnList.add(oResult);
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceRepository.getQueuesByNodeAndStatuses: exception ", oEx);
		}

		return aoReturnList;
	}
	
	private java.sql.PreparedStatement buildInQueryStatement(java.sql.Connection oConn, String sNodeCode, String[] asStatuses) throws java.sql.SQLException {
		StringBuilder oSql = new StringBuilder(
				"SELECT json_extract(data,'$.operationType') AS operationType," +
				" json_extract(data,'$.operationSubType') AS operationSubType," +
				" json_extract(data,'$.status') AS status," +
				" COUNT(*) AS cnt FROM ").append(m_sThisCollection)
				.append(" WHERE json_extract(data,'$.nodeCode') = ?")
				.append(" AND json_extract(data,'$.status') IN (");
		for (int i = 0; i < asStatuses.length; i++) {
			if (i > 0) oSql.append(',');
			oSql.append('?');
		}
		oSql.append(") GROUP BY operationType, operationSubType, status ORDER BY operationType ASC, operationSubType ASC, status ASC");
		
		java.sql.PreparedStatement oPs = oConn.prepareStatement(oSql.toString());
		oPs.setString(1, sNodeCode);
		for (int i = 0; i < asStatuses.length; i++) {
			oPs.setString(i + 2, asStatuses[i]);
		}
		return oPs;
	}
	
	/**
     * Get the list of Process Workspaces by Parent Id
     * @param sParentId parent processObjId
     * @return Found processworkspaces
     */
    public List<ProcessWorkspace> getProcessByParentId(String sParentId) {

        try {
        	return queryList(
        			"SELECT data FROM " + m_sThisCollection +
        			" WHERE json_extract(data,'$.parentId') = ?" +
        			" ORDER BY json_extract(data,'$.operationTimestamp') DESC, json_extract(data,'$.operationDate') DESC",
        			new Object[]{sParentId}, ProcessWorkspace.class);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ProcessWorkspaceRepository.getProcessByParentId: exception ", oEx);
        }

        return new ArrayList<>();
    }

    public Long getRunningTime(String sUserId, Long lDateFrom, Long lDateTo) {
		if (Utils.isNullOrEmpty(sUserId) || lDateFrom == null || lDateTo == null) {
			return 0L;
		}
    	try {
			String sSql =
					"SELECT SUM(CAST(json_extract(data,'$.runningTime') AS INTEGER)) FROM " + m_sThisCollection +
					" WHERE LOWER(json_extract(data,'$.userId')) = LOWER(?)" +
					" AND json_extract(data,'$.operationTimestamp') >= ?" +
					" AND json_extract(data,'$.operationTimestamp') <= ?";

			try (java.sql.Connection oConn = getConnection();
					 java.sql.PreparedStatement oPs = oConn.prepareStatement(sSql)) {
				oPs.setString(1, sUserId);
				oPs.setLong(2, lDateFrom);
				oPs.setLong(3, lDateTo);
				try (java.sql.ResultSet oRs = oPs.executeQuery()) {
					if (oRs.next()) {
						long lValue = oRs.getLong(1);
						return oRs.wasNull() ? 0L : lValue;
					}
				}
			}
			return 0L;
    	} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceRepository.getRunningTime: exception ", oEx);
    	}
    	return 0L;
    }

	public List<ProcessWorkspaceAggregatorBySubscriptionAndProject> getRunningTimeInfo(Collection<String> asSubscriptionIds) {
		List<ProcessWorkspaceAggregatorBySubscriptionAndProject> aoResultList = new ArrayList<>();

		if (asSubscriptionIds == null || asSubscriptionIds.isEmpty()) return aoResultList;

		try (java.sql.Connection oConn = getConnection()) {
			StringBuilder oSql = new StringBuilder(
					"SELECT json_extract(data,'$.subscriptionId') AS subscriptionId," +
					" json_extract(data,'$.projectId') AS projectId," +
					" SUM(CAST(json_extract(data,'$.runningTime') AS INTEGER)) AS total FROM ")
					.append(m_sThisCollection)
					.append(" WHERE json_extract(data,'$.subscriptionId') IN (");
			for (int i = 0; i < asSubscriptionIds.size(); i++) {
				if (i > 0) oSql.append(',');
				oSql.append('?');
			}
			oSql.append(") AND json_extract(data,'$.projectId') IS NOT NULL GROUP BY subscriptionId, projectId");

			try (java.sql.PreparedStatement oPs = oConn.prepareStatement(oSql.toString())) {
				int i = 1;
				for (String sSub : asSubscriptionIds) oPs.setString(i++, sSub);
				try (java.sql.ResultSet oRs = oPs.executeQuery()) {
					while (oRs.next()) {
						ProcessWorkspaceAggregatorBySubscriptionAndProject oResult = new ProcessWorkspaceAggregatorBySubscriptionAndProject();
						oResult.setSubscriptionId(oRs.getString("subscriptionId"));
						oResult.setProjectId(oRs.getString("projectId"));
						oResult.setTotal(oRs.getLong("total"));
						aoResultList.add(oResult);
					}
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceRepository.getRunningTimeInfo: exception ", oEx);
		}

		return aoResultList;
    }
	
	public List<ProcessWorkspaceAggregatorBySubscriptionAndProject> getProjectRunningTime(String sUserId, Collection<String> asSubscriptionIds) {
		List<ProcessWorkspaceAggregatorBySubscriptionAndProject> aoResultList = new ArrayList<>();

		if (asSubscriptionIds == null || asSubscriptionIds.isEmpty()) return aoResultList;

		try (java.sql.Connection oConn = getConnection()) {
			StringBuilder oSql = new StringBuilder(
					"SELECT json_extract(data,'$.subscriptionId') AS subscriptionId," +
					" json_extract(data,'$.projectId') AS projectId," +
					" SUM(CAST(json_extract(data,'$.runningTime') AS INTEGER)) AS total FROM ")
					.append(m_sThisCollection)
					.append(" WHERE json_extract(data,'$.subscriptionId') IN (");
			for (int i = 0; i < asSubscriptionIds.size(); i++) {
				if (i > 0) oSql.append(',');
				oSql.append('?');
			}
			oSql.append(") AND json_extract(data,'$.projectId') IS NOT NULL")
			    .append(" AND json_extract(data,'$.userId') = ?")
			    .append(" GROUP BY subscriptionId, projectId");

			try (java.sql.PreparedStatement oPs = oConn.prepareStatement(oSql.toString())) {
				int i = 1;
				for (String sSub : asSubscriptionIds) oPs.setString(i++, sSub);
				oPs.setString(i, sUserId);
				try (java.sql.ResultSet oRs = oPs.executeQuery()) {
					while (oRs.next()) {
						ProcessWorkspaceAggregatorBySubscriptionAndProject oResult = new ProcessWorkspaceAggregatorBySubscriptionAndProject();
						oResult.setSubscriptionId(oRs.getString("subscriptionId"));
						oResult.setProjectId(oRs.getString("projectId"));
						oResult.setTotal(oRs.getLong("total"));
						aoResultList.add(oResult);
					}
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceRepository.getProjectRunningTime: exception ", oEx);
		}

		return aoResultList;
    }

}
