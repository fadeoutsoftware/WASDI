package it.fadeout.rest.resources;

import static wasdi.shared.business.UserApplicationPermission.ADMIN_DASHBOARD;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.json.JSONArray;

import com.fasterxml.jackson.core.type.TypeReference;

import it.fadeout.Wasdi;
import it.fadeout.services.ProcessWorkspaceService;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.ProcessWorkspaceAgregatorByOperationTypeAndOperationSubtypeResult;
import wasdi.shared.business.User;
import wasdi.shared.business.UserApplicationRole;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.SchedulerQueueConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.processors.AppStatsViewModel;
import wasdi.shared.viewmodels.processors.ProcessHistoryViewModel;
import wasdi.shared.viewmodels.processworkspace.NodeScoreByProcessWorkspaceViewModel;
import wasdi.shared.viewmodels.processworkspace.ProcessWorkspaceAggregatedViewModel;
import wasdi.shared.viewmodels.processworkspace.ProcessWorkspaceSummaryViewModel;
import wasdi.shared.viewmodels.processworkspace.ProcessWorkspaceViewModel;

/**
 * Process Workspace Resource.
 *
 * Hosts API for:
 *	.get and set processworksapce status
 *	.get and set processworkspace payload
 *
 * Status of process workspace are listed in the ProcessStatus enum and are:
 *
 * CREATED	: process created, waiting to be started
 * RUNNING	: process started and running
 * WAITING	: process waiting for an another operation to finish
 * READY	: process ready to be remused in runnig, since the operation that was waiting for is finished
 * DONE		: process finished with success
 * ERROR	: process finished in error
 * STOPPED	: process stopped by the user or a timeout
 *
 * Processes can be of any type supported in the LauncherOperation enum.
 *
 * @author p.campanella
 *
 */
@Path("/process")
public class ProcessWorkspaceResource {
	
	protected static String s_sNoneSubtype = "none";

	/**
	 * Get a filtered paginated list of processworkspaces.
	 *
	 * @param sSessionId User session id
	 * @param sWorkspaceId Workspace Id
	 * @param sStatus Status filter
	 * @param sOperationType Operation type filter
	 * @param sNamePattern Name filter
	 * @param sDateFrom Start update date filter
	 * @param sDateTo End update date filter
	 * @param iStartIndex start index
	 * @param iEndIndex end index
	 * @return List of Process Workspace View Models
	 */
	@GET
	@Path("/byws")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public ArrayList<ProcessWorkspaceViewModel> getProcessByWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("status") String sStatus,
			@QueryParam("operationType") String sOperationType,
			@QueryParam("namePattern") String sNamePattern,
			@QueryParam("dateFrom") String sDateFrom, @QueryParam("dateTo") String sDateTo,
			@QueryParam("startindex") Integer iStartIndex, @QueryParam("endindex") Integer iEndIndex) {
		
		WasdiLog.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace( WS: " + sWorkspaceId +
				", status: " + sStatus + ", name pattern: " + sNamePattern +
				", Start: " + iStartIndex + ", End: " + iEndIndex);

		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				WasdiLog.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: workspace id is null, aborting");
				return aoProcessList;
			}
			
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				WasdiLog.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: invalid session");
				return aoProcessList;
			}
			
			if(!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
				WasdiLog.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: user " + oUser.getUserId() + " is not allowed to access workspace " + sWorkspaceId +", aborting" );
				return aoProcessList;
			}
			
			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			List<ProcessWorkspace> aoProcess = null;
			
			ProcessStatus eStatus = null;
			if(!Utils.isNullOrEmpty(sStatus)) {
				try {
					eStatus = ProcessStatus.valueOf(sStatus);
				}catch (Exception oE) {
					WasdiLog.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: could not convert " + sStatus + " to a valid process status, ignoring it");
				}
			}
			
			LauncherOperations eLauncherOperation = null;
			if(!Utils.isNullOrEmpty(sOperationType)) {
				try {
					eLauncherOperation = LauncherOperations.valueOf(sOperationType);
				} catch (Exception oE) {
					WasdiLog.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: could not convert " + sOperationType + " to a valid operation type, ignoring it");
				}
			}

			Instant oDateFrom = TimeEpochUtils.fromDateStringToInstant(sDateFrom);
			Instant oDateTo = TimeEpochUtils.fromDateStringToInstant(sDateTo);

			if (iStartIndex != null && iEndIndex != null) {
				aoProcess = oRepository.getProcessByWorkspace(sWorkspaceId, eStatus, eLauncherOperation, sNamePattern, oDateFrom, oDateTo, iStartIndex, iEndIndex);
			}
			else {
				aoProcess = oRepository.getProcessByWorkspace(sWorkspaceId, eStatus, eLauncherOperation, sNamePattern, oDateFrom, oDateTo);
			}

			// For each
			for (int iProcess=0; iProcess<aoProcess.size(); iProcess++) {
				// Create View Model
				ProcessWorkspace oProcess = aoProcess.get(iProcess);
				aoProcessList.add(ProcessWorkspaceViewModel.buildProcessWorkspaceViewModel(oProcess));
			}

		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: " + oEx);
		}

		return aoProcessList;
	}

	
	/**
	 * Get a list of proc workspace for a user
	 *
	 * @param sSessionId User Session Id
	 * @return List of
	 */
	@GET
	@Path("/byusr")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<ProcessWorkspaceViewModel> getProcessByUser(@HeaderParam("x-session-token") String sSessionId, @QueryParam("ogc") Boolean bOgcOnly) {
		
		WasdiLog.debugLog("ProcessWorkspaceResource.GetProcessByUser()");

		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();
			

		try {
			// Domain Check
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if(null == oUser) {
				WasdiLog.debugLog("ProcessWorkspaceResource.GetProcessByUser: invalid session");
				return aoProcessList;
			}
			
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return aoProcessList;
			}

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			
			boolean bOgc = false;

			if (bOgcOnly!=null) bOgc = (boolean) bOgcOnly;

			// Get Process List
			List<ProcessWorkspace> aoProcess = null;

			if (bOgc) {
				// Get Process List
				aoProcess = oRepository.getOGCProcessByUser(oUser.getUserId());				
			}
			else {
				// Get Process List
				aoProcess = oRepository.getProcessByUser(oUser.getUserId());				
			}

			// For each
			for (int iProcess=0; iProcess<aoProcess.size(); iProcess++) {
				// Create View Model
				ProcessWorkspace oProcess = aoProcess.get(iProcess);
				aoProcessList.add(ProcessWorkspaceViewModel.buildProcessWorkspaceViewModel(oProcess));
			}
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.GetProcessByUser: error retrieving process " + oEx);
		}

		return aoProcessList;
	}
	
	/**
	 * Get all the process workspaces related to an application.
	 * This API is exposed by the main server that calls the same API on all computing node.
	 * Since processworkspace is in the distributed database, each node may have its own records.
	 * This API collects all these data to get back the full history.
	 *
	 * @param sSessionId User Session Id
	 * @param sProcessorName Processor Name
	 * @return Process History View Model
	 */
	@GET
	@Path("/byapp")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<ProcessHistoryViewModel> getProcessByApplication(
			@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processorName") String sProcessorName) {
		WasdiLog.debugLog("ProcessWorkspaceResource.getProcessByApplication " + sProcessorName);
		ArrayList<ProcessHistoryViewModel> aoProcessList = new ArrayList<ProcessHistoryViewModel>();
		try {			
			// Domain Check
			if(Utils.isNullOrEmpty(sProcessorName)) {
				WasdiLog.debugLog("ProcessWorkspaceResource.getProcessByApplication: invalid processor name, aborting");
				return aoProcessList;
			}
			
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if(null == oUser) {
				WasdiLog.debugLog("ProcessWorkspaceResource.getProcessByApplication: invalid session, aborting");
				return aoProcessList;
			}
			
			// checks that processor is in db -> needed to avoid url injection from users 
			ProcessorRepository oProcessRepository = new ProcessorRepository();
			if (null == oProcessRepository.getProcessorByName(sProcessorName) ) {
				WasdiLog.debugLog("ProcessWorkspaceResource.getProcessByApplication( Processor name: " + sProcessorName+ " ): Processor name not found in DB, aborting");
				return aoProcessList;
			}

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			List<ProcessWorkspace> aoProcess = oRepository.getProcessByProductNameAndUserId(sProcessorName, oUser.getUserId());
			
			HashMap<String, String> aoWorkspaceNames = new HashMap<String, String>();
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			

			// For each
			for (int iProcess=0; iProcess<aoProcess.size(); iProcess++) {
				// Create View Model
				ProcessWorkspace oProcess = aoProcess.get(iProcess);
				ProcessHistoryViewModel oRun = new ProcessHistoryViewModel();
				
				try {
					oRun.setOperationDate(Utils.getFormatDate(oProcess.getOperationTimestamp()));
					
					// Set the start date: beeing introduced later, for compatibility, if not present use the Operation Date
					if (!Utils.isNullOrEmpty(oProcess.getOperationStartTimestamp())) {
						oRun.setOperationStartDate(Utils.getFormatDate(oProcess.getOperationStartTimestamp()));
					}
					else {
						oRun.setOperationStartDate(Utils.getFormatDate(oProcess.getOperationTimestamp()));
					}
					
					
					oRun.setOperationEndDate(Utils.getFormatDate(oProcess.getOperationEndTimestamp()));
					
					oRun.setProcessorName(sProcessorName);
					oRun.setStatus(oProcess.getStatus());
					oRun.setWorkspaceId(oProcess.getWorkspaceId());
					
					if (!aoWorkspaceNames.containsKey(oProcess.getWorkspaceId())) {
						Workspace oWorkspace = oWorkspaceRepository.getWorkspace(oProcess.getWorkspaceId());
						
						if (oWorkspace != null) {
							aoWorkspaceNames.put(oProcess.getWorkspaceId(), oWorkspace.getName());
						}
						else {
							continue;
						}
					}
					
					oRun.setWorkspaceName(aoWorkspaceNames.get(oProcess.getWorkspaceId()));					
				}
				catch (Exception oEx) {
					WasdiLog.debugLog("ProcessWorkspaceResource.getProcessByApplication: exception generating History View Model " + oEx.toString());
				}
				
				if (oRun.getWorkspaceName().equals("wasdi_specific_node_ws_code")) continue;
				
				aoProcessList.add( oRun);
			}
			
			// The main node needs to query also the others
			if (Wasdi.s_sMyNodeCode.equals("wasdi") ) {
				
				NodeRepository oNodeRepo = new NodeRepository();
				List<Node> aoNodes = oNodeRepo.getNodesList();
				
				for (Node oNode : aoNodes) {
					
					if (oNode.getNodeCode().equals("wasdi")) continue;					
					if (oNode.getActive() == false) continue;
					
					try {
						String sUrl = oNode.getNodeBaseAddress();
						
						if (!sUrl.endsWith("/")) sUrl += "/";
						
						sUrl += "process/byapp?processorName="+sProcessorName;
						
						Map<String, String> asHeaders = new HashMap<String, String>();
						asHeaders.put("x-session-token", sSessionId);
						
						WasdiLog.debugLog("ProcessWorkspaceResource.getProcessByApplication: calling url: " + sUrl);
						
						
						HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders); 
						String sResponse = oHttpCallResponse.getResponseBody(); 
						
						if (Utils.isNullOrEmpty(sResponse)==false) {
							ArrayList<ProcessHistoryViewModel> aoNodeHistory = MongoRepository.s_oMapper.readValue(sResponse, new TypeReference<ArrayList<ProcessHistoryViewModel>>(){});
							if (aoNodeHistory != null) {
								aoProcessList.addAll(aoNodeHistory);
							}
						}
						
					}
					catch (Exception e) {
						WasdiLog.debugLog("ProcessWorkspaceResource.getProcessByApplication: exception contacting computing node: " + e.toString());
					}
				}
				
			}
			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.getProcessByApplication: error retrieving process " + oEx);
		}

		return aoProcessList;
	}
	
	
	/**
	 * Get Application statistic
	 *
	 * This API is exposed by the main server that calls the same API on all computing node.
	 * Since processworkspace is in the distributed database, each node may have its own records.
	 * This API collects all these data to get back the stats.
	 *
	 * @param sSessionId User Session
	 * @param sProcessorName Application name
	 * @return AppStatsViewModel with the app statistics
	 */
	@GET
	@Path("/appstats")
	@Produces({"application/xml", "application/json", "text/xml"})
	public AppStatsViewModel getApplicationStatistics(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorName") String sProcessorName) {
		
		WasdiLog.debugLog("ProcessWorkspaceResource.getApplicationStatistics( Session: " + sSessionId + " )");
		
		AppStatsViewModel oReturnStats = new AppStatsViewModel();
		oReturnStats.setApplicationName(sProcessorName);
		
		try {			
			// Domain Check
			if(Utils.isNullOrEmpty(sProcessorName)) {
				WasdiLog.debugLog("ProcessWorkspaceResource.getApplicationStatistics( " + sSessionId + ", " + sProcessorName + " ): invalid processor name, aborting");
				return oReturnStats;
			}
			
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if(null == oUser) {
				WasdiLog.debugLog("ProcessWorkspaceResource.getApplicationStatistics( Session: " + sSessionId + " ): invalid session, aborting");
				return oReturnStats;
			}
			
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				WasdiLog.debugLog("ProcessWorkspaceResource.getApplicationStatistics( Session: " + sSessionId + " ): is valid, but userId is not (" + oUser.getUserId() + "), aborting");
				return oReturnStats;
			}
			
			// checks that processor is in db -> needed to avoid url injection from users 
			ProcessorRepository oProcessRepository = new ProcessorRepository();
			if (null == oProcessRepository.getProcessorByName(sProcessorName) ) {
				WasdiLog.debugLog("ProcessWorkspaceResource.getApplicationStatistics( Processor name: " + sProcessorName+ " ): Processor name not found in DB, aborting");
				return oReturnStats;
			}

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			List<ProcessWorkspace> aoProcess = oRepository.getProcessByProductName(sProcessorName);
			
			oReturnStats.setRuns(aoProcess.size());
			
			long lTotalDoneTime = 0;

			// For each
			for (int iProcess=0; iProcess<aoProcess.size(); iProcess++) {
				
				// Create View Model
				ProcessWorkspace oProcess = aoProcess.get(iProcess);
				
				if (!oReturnStats.getUsers().contains(oProcess.getUserId())) {
					oReturnStats.getUsers().add(oProcess.getUserId());
				}
				
				try {
					
					if (oProcess.getStatus().equals(ProcessStatus.DONE.toString())) {
						oReturnStats.setDone(oReturnStats.getDone()+1);
						lTotalDoneTime += Utils.getProcessWorkspaceSecondsDuration(oProcess);
					}
					else if (oProcess.getStatus().equals(ProcessStatus.ERROR.toString())) {
						oReturnStats.setError(oReturnStats.getError()+1);
					}
					else if(oProcess.getStatus().equals(ProcessStatus.STOPPED.toString())) {
						oReturnStats.setStopped(oReturnStats.getStopped()+1);
					}
				}
				catch (Exception oEx) {
					WasdiLog.debugLog("ProcessWorkspaceResource.getApplicationStatistics: exception generating History View Model " + oEx.toString());
				}
				
			}
			
			
			// The main node needs to query also the others
			if (Wasdi.s_sMyNodeCode.equals("wasdi") ) {
				
				NodeRepository oNodeRepo = new NodeRepository();
				List<Node> aoNodes = oNodeRepo.getNodesList();
				
				for (Node oNode : aoNodes) {
					
					if (oNode.getNodeCode().equals("wasdi")) continue;					
					if (oNode.getActive() == false) continue;
					
					try {
						String sUrl = oNode.getNodeBaseAddress();
						
						if (!sUrl.endsWith("/")) sUrl += "/";
						
						sUrl += "process/appstats?processorName="+sProcessorName;
						
						Map<String, String> asHeaders = new HashMap<String, String>();
						asHeaders.put("x-session-token", sSessionId);
						
						WasdiLog.debugLog("ProcessWorkspaceResource.getApplicationStatistics: calling url: " + sUrl);
						
						
						HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders); 
						String sResponse = oHttpCallResponse.getResponseBody();
						
						if (Utils.isNullOrEmpty(sResponse)==false) {
							AppStatsViewModel oNodeStats = MongoRepository.s_oMapper.readValue(sResponse, new TypeReference<AppStatsViewModel>(){});
							oReturnStats.setRuns(oReturnStats.getRuns() + oNodeStats.getRuns());
							
							oReturnStats.setDone(oReturnStats.getDone() + oNodeStats.getDone());
							oReturnStats.setError(oReturnStats.getError() + oNodeStats.getError());
							oReturnStats.setStopped(oReturnStats.getStopped() + oNodeStats.getStopped());
							
							// Add to the time the total time for this node
							lTotalDoneTime += oNodeStats.getMediumTime()*oNodeStats.getDone()*60;
							
							for (String sUser : oNodeStats.getUsers()) {
								if (!oReturnStats.getUsers().contains(sUser)) {
									oReturnStats.getUsers().add(sUser);
								}
							}
						}
						
					}
					catch (Exception e) {
						WasdiLog.debugLog("ProcessWorkspaceResource.getApplicationStatistics: exception contacting computing node: " + e.toString());
					}
				}
				
				oReturnStats.setUniqueUsers(oReturnStats.getUsers().size());
				oReturnStats.getUsers().clear();
			}
			
			long lMediumTime = 0L;
			
			if (oReturnStats.getDone()>0) {
				lMediumTime = lTotalDoneTime/oReturnStats.getDone();
			}
			
			oReturnStats.setMediumTime((int) (lMediumTime/60));			
			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.getApplicationStatistics: error retrieving process " + oEx);
		}

		return oReturnStats;
	}
	
	
	/**
	 * Get the last 5 process workspaces of a workspace. The limit 5 is hard coded in the repository query.
	 * This is used mainly for the process bar in the client.
	 *
	 * @param sSessionId User Session Id
	 * @param sWorkspaceId Workspace Id
	 * @return List (max 5) of Process Workspace View Model
	 */
	@GET
	@Path("/lastbyws")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<ProcessWorkspaceViewModel> getLastProcessByWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspaceId) {
		
		WasdiLog.debugLog("ProcessWorkspaceRepository.getLastProcessByWorkspace( WS: " + sWorkspaceId + " )");
		User oUser = Wasdi.getUserFromSession(sSessionId);

		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();

		try {
			// Domain Check
			if (oUser == null) {
				WasdiLog.debugLog("ProcessWorkspaceRepository.getLastProcessByWorkspace( WS: " + sWorkspaceId + " ): invalid session");
				return aoProcessList;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				WasdiLog.debugLog("ProcessWorkspaceResource.getLastProcessByWorkspace: user id is null");
				return aoProcessList;
			}
			
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				WasdiLog.debugLog("ProcessWorkspaceResource.getLastProcessByWorkspace: ws id is null");
				return aoProcessList;
			}

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			List<ProcessWorkspace> aoProcess = oRepository.getLastProcessByWorkspace(sWorkspaceId);

			// For each
			for (int iProcess=0; iProcess<aoProcess.size(); iProcess++) {
				// Create View Model
				ProcessWorkspace oProcess = aoProcess.get(iProcess);
				aoProcessList.add(ProcessWorkspaceViewModel.buildProcessWorkspaceViewModel(oProcess));
			}

		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.getLastProcessByWorkspace: " + oEx);
		}

		return aoProcessList;
	}
	
	/**
	 * Get the last 5 process workspaces of a user.
	 * The limit 5 is hard coded in the repository query.
	 *
	 * @param sSessionId User Session Id
	 * @return List of Process Workspace View Models
	 */
	@GET
	@Path("/lastbyusr")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<ProcessWorkspaceViewModel> getLastProcessByUser(@HeaderParam("x-session-token") String sSessionId) {
		
		WasdiLog.debugLog("ProcessWorkspaceResource.GetLastProcessByUser()");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();

		try {
			// Domain Check
			if (oUser == null) {
				WasdiLog.debugLog("ProcessWorkspaceResource.GetLastProcessByUser(): invalid session");
				return aoProcessList;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return aoProcessList;
			}
			
			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			List<ProcessWorkspace> aoProcess = oRepository.getLastProcessByUser(oUser.getUserId());

			// For each
			for (int iProcess=0; iProcess<aoProcess.size(); iProcess++) {
				// Create View Model
				ProcessWorkspace oProcess = aoProcess.get(iProcess);
				aoProcessList.add(ProcessWorkspaceViewModel.buildProcessWorkspaceViewModel(oProcess));
			}

		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.GetLastProcessByUser: " + oEx);
		}

		return aoProcessList;
	}
	
	/**
	 * Get summary info about process workspaces of a user.
	 * The summary has the number of CREATED, RUNNING and WAITING processes for the user.
	 * This API works only on this node.
	 *
	 * @param sSessionId User Session Id
	 * @return Process Workspace Summary View Model
	 */
	@GET
	@Path("/summary")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceSummaryViewModel getSummary(@HeaderParam("x-session-token") String sSessionId) {
		
		WasdiLog.debugLog("ProcessWorkspaceResource.GetSummary()");
		ProcessWorkspaceSummaryViewModel oSummaryViewModel = new ProcessWorkspaceSummaryViewModel();

		try {
			
			User oUser = Wasdi.getUserFromSession(sSessionId);

			// Domain Check
			if (oUser == null) {
				WasdiLog.debugLog("ProcessWorkspaceResource.GetSummary: invalid session: " + sSessionId);
				return oSummaryViewModel;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				WasdiLog.debugLog("ProcessWorkspaceResource.GetSummary: invalid session: " + sSessionId);
				return oSummaryViewModel;
			}
			
			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			
			// Get all CREATED
			List<ProcessWorkspace> aoWaitingList = oRepository.getCreatedSummary();
			oSummaryViewModel.setAllProcessWaiting(aoWaitingList.size());
			
			int iUserWaiting = 0 ;
			// Count the user's ones
			for (int iProcess=0; iProcess<aoWaitingList.size(); iProcess++) {
				// Get the process
				ProcessWorkspace oProcess = aoWaitingList.get(iProcess);
				if (oProcess.getUserId().equals(oUser.getUserId())) iUserWaiting ++;
			}
			
			oSummaryViewModel.setUserProcessWaiting(iUserWaiting);
			
			// Get all RUNNING, WAITING, READY
			List<ProcessWorkspace> aoRunningList = oRepository.getRunningSummary();
			oSummaryViewModel.setAllProcessRunning(aoRunningList.size());
			
			int iUserRunning = 0;
			
			// Count the user's ones
			for (int iProcess=0; iProcess<aoRunningList.size(); iProcess++) {
				// Get the process
				ProcessWorkspace oProcess = aoRunningList.get(iProcess);
				if (oProcess.getUserId().equals(oUser.getUserId())) iUserRunning ++;
			}
			
			oSummaryViewModel.setUserProcessRunning(iUserRunning);
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.GetSummary: " + oEx);
		}

		return oSummaryViewModel;
	}
	
	/**
	 * Kills a running process workspace, and all the relative process tree.
	 *
	 * @param sSessionId User Session Id
	 * @param sToKillProcessObjId Proc id to kill
	 * @param bKillTheEntireTree Flag to delete all the tree or only the process
	 * @return Primitive Result with boolValue = true and intValue = 200 if ok, std http response with error code otherwise.
	 */
	@GET
	@Path("/delete")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response deleteProcess(@HeaderParam("x-session-token") String sSessionId, @QueryParam("procws") String sToKillProcessObjId, @QueryParam("treeKill") Boolean bKillTheEntireTree) {

		WasdiLog.debugLog("ProcessWorkspaceResource.DeleteProcess( Process: " + sToKillProcessObjId + ", treeKill: " + bKillTheEntireTree + " )");

		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);
			// Domain Check
			if (oUser == null) {
				WasdiLog.debugLog("ProcessWorkspaceResource.DeleteProcess( Process: " + sToKillProcessObjId + ", treeKill: " + bKillTheEntireTree + " ): invalid session");
				return Response.status(401).build();
			}

			if(Utils.isNullOrEmpty(sToKillProcessObjId)) {
				WasdiLog.debugLog("ProcessWorkspaceResource.DeleteProcess: processObjId is null or empty, aborting");
				return Response.status(401).build();
			}

			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessToKill = oRepository.getProcessByProcessObjId(sToKillProcessObjId);

			//check that the process exists
			if(null==oProcessToKill) {
				WasdiLog.debugLog("ProcessWorkspaceResource.DeleteProcess: process not found in DB, aborting");
				return Response.status(400).build();
			}

			// check that the user can access the processWorkspace
			if(!PermissionsUtils.canUserAccessProcessWorkspace(oUser.getUserId(), sToKillProcessObjId)) {
				WasdiLog.debugLog("ProcessWorkspaceResource.DeleteProcess: user cannot access requested process workspace");
				return Response.status(403).build();
			}
	
			ProcessWorkspaceService oProcessService = new ProcessWorkspaceService();
			List<ProcessWorkspace> aoProcessesToBeKilled = new ArrayList<ProcessWorkspace>(1);
			aoProcessesToBeKilled.add(oProcessToKill);
			boolean bResult = oProcessService.killProcesses(aoProcessesToBeKilled, bKillTheEntireTree, false, sSessionId);
			if(bResult) {
				return Response.status(Response.Status.OK).build();
			}
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("WorkspaceResource.DeleteProcess: " + oEx);
		}

		return Response.status(500).build();
	}


	
	/**
	 * Get a Process Workspace View Model by id
	 *
	 * @param sSessionId User Session Id
	 * @param sProcessWorkspaceId Process Workspace ID
	 * @return Process Workspace View Model
	 */
	@GET
	@Path("/byid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceViewModel getProcessById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("procws") String sProcessWorkspaceId) {
		
		WasdiLog.debugLog("ProcessWorkspaceResource.GetProcessById( ProcWsId: " + sProcessWorkspaceId + " )");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();
		
		// Initialize status as ERROR
		oProcess.setStatus("ERROR");

		try {
			// Domain Check
			if (oUser == null) {
				WasdiLog.debugLog("ProcessWorkspaceResource.GetProcessById( x-session-token: " + sSessionId + ", sProcessId: " + sProcessWorkspaceId + " ): invalid session");
				return oProcess;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return oProcess;
			}
			
			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			ProcessWorkspace oProcessWorkspace = oRepository.getProcessByProcessObjId(sProcessWorkspaceId);
			oProcess = ProcessWorkspaceViewModel.buildProcessWorkspaceViewModel(oProcessWorkspace);

		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.GetProcessById: " + oEx);
		}

		return oProcess;
	}
	
	
	/**
	 * Get the status of multiple processes in a single call
	 *
	 * @param sSessionId User Session Id
	 * @param asProcessesWorkspaceId List of string, each representing a ProcessWorkspace Id, receved in the body.
	 * @return List of strings: each is the status of the procws, corresponding to the same index used in input
	 */
	@POST
	@Path("/statusbyid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<String> getStatusProcessesById(@HeaderParam("x-session-token") String sSessionId, ArrayList<String> asProcessesWorkspaceId) {
		
		WasdiLog.debugLog("ProcessWorkspaceResource.getStatusProcessesById");

		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		ArrayList<String> asReturnStatusList = new ArrayList<String>();

		try {
			// Domain Check
			if (oUser == null) {
				WasdiLog.debugLog("ProcessWorkspaceResource.getStatusProcessesById: invalid session");
				return asReturnStatusList;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return asReturnStatusList;
			}
			
			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			asReturnStatusList = oRepository.getProcessesStatusByProcessObjId(asProcessesWorkspaceId);
			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.getStatusProcessesById: " + oEx);
		}

		return asReturnStatusList;
	}
	
	/**
	 * Get only the status of a single Process Workspace
	 * @param sSessionId User Session Id
	 * @param sProcessObjId Process Workspace Id
	 * @return string with the status.
	 */
	@GET
	@Path("/getstatusbyid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public String getProcessStatusById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("procws") String sProcessObjId) {
		WasdiLog.debugLog("ProcessWorkspaceResource.getProcessStatusById(" + sProcessObjId + " )" );
		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);
			
			if(null == oUser) {
				WasdiLog.debugLog("ProcessWorkspaceResource.getProcessStatusById: invalid session" );
				return null;
			}
			
			if(PermissionsUtils.canUserAccessProcessWorkspace(oUser.getUserId(), sProcessObjId)) {
				ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
				return oProcessWorkspaceRepository.getProcessStatusFromId(sProcessObjId);
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("ProcessWorkspaceResource.getProcessStatusById: " + oE );
		}
		return null;
	}
	
	/**
	 * Update Status and Progress Percentage of a process workspace.
	 *
	 * @param sSessionId User Session Id
	 * @param sProcessObjId Process Workspace Id
	 * @param sNewStatus New status
	 * @param iPerc New progress perc. If it is out of range [0-100], it is ignored
	 * @param sSendToRabbit flag to decide if this update has to be notified in rabbit or not. Can be "1" or "true" to activate the transmission
	 * @return updated Process Workspace View Model
	 */
	@GET
	@Path("/updatebyid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceViewModel updateProcessById(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("procws") String sProcessObjId, @QueryParam("status") String sNewStatus,
			@QueryParam("perc") int iPerc, @QueryParam("sendrabbit") String sSendToRabbit) {
		
		WasdiLog.debugLog("ProcessWorkspaceResource.UpdateProcessById( ProcWsId: " + sProcessObjId + ", Status: " + sNewStatus + ", Perc: " + iPerc + ", SendRabbit:" + sSendToRabbit + " )" );

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();

		try {
			// Domain Check
			if (oUser == null) {
				WasdiLog.debugLog("ProcessWorkspaceResource.UpdateProcessById: invalid session: " + sSessionId );
				return oProcess;
			}

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process
			ProcessWorkspace oProcessWorkspace = oRepository.getProcessByProcessObjId(sProcessObjId);
			
			if (  (oProcessWorkspace.getStatus().equals(ProcessStatus.CREATED.name()) || oProcessWorkspace.getStatus().equals(ProcessStatus.RUNNING.name()) || oProcessWorkspace.getStatus().equals(ProcessStatus.WAITING.name()) || oProcessWorkspace.getStatus().equals(ProcessStatus.READY.name())) 
					&& 
					(sNewStatus.equals(ProcessStatus.DONE.name()) || sNewStatus.equals(ProcessStatus.ERROR.name()) || sNewStatus.equals(ProcessStatus.STOPPED.name()) ) ) {
				// The process finished
				if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
					WasdiLog.debugLog("ProcessWorkspaceResource.UpdateProcessById( ProcWsId: " + sProcessObjId + ", update process end date" );
					// No end-date set: put it here
					oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
				}
			}
			
			oProcessWorkspace.setStatus(sNewStatus);
			
			if (iPerc>=0 && iPerc<=100) {
				oProcessWorkspace.setProgressPerc(iPerc);
			}

			oRepository.updateProcess(oProcessWorkspace);
			
			oProcess = ProcessWorkspaceViewModel.buildProcessWorkspaceViewModel(oProcessWorkspace);
			
			WasdiLog.debugLog("ProcessWorkspaceResource.UpdateProcessById( ProcWsId: " + sProcessObjId + ", Status updated : " +  oProcess.getStatus());

			// Check if we need to send the asynch rabbit message
			if (Utils.isNullOrEmpty(sSendToRabbit) == false) {
				// The param exists
				if (sSendToRabbit.equals("1") || sSendToRabbit.equals("true")) {
					
					// Search for exchange name
					String sExchange = WasdiConfig.Current.rabbit.exchange;
					
					// Set default if is empty
					if (Utils.isNullOrEmpty(sExchange)) {
						sExchange = "amq.topic";
					}
					
					// Send the Asynch Message to the clients
					Send oSendToRabbit = new Send(sExchange);
					oSendToRabbit.SendUpdateProcessMessage(oProcessWorkspace);
					oSendToRabbit.Free();
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.UpdateProcessById: " + oEx);
		}

		return oProcess;
	}
	
	/**
	 * Set the payload of a Process Workspace with a GET Method
	 *
	 * @param sSessionId User Session Id
	 * @param sProcessObjId Process Workspace Id
	 * @param sPayload Payload to save
	 * @return Updated Process Workspace View Model
	 */
	@GET
	@Path("/setpayload")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceViewModel setProcessPayloadGET(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("procws") String sProcessObjId, @QueryParam("payload") String sPayload) {
		return internalSetPaylod(sSessionId, sProcessObjId, sPayload);
	}
	
	/**
	 * Set the payload of a Process Workspace with a POST Method
	 * @param sSessionId User Session Id
	 * @param sProcessObjId Process Workspace Id
	 * @param sPayload Payload to save
	 * @return Updated Process Workspace View Model
	 */
	@POST
	@Path("/setpayload")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceViewModel setProcessPayloadPOST(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("procws") String sProcessObjId, String sPayload) {
		return internalSetPaylod(sSessionId, sProcessObjId, sPayload);
	}	
	
	protected ProcessWorkspaceViewModel internalSetPaylod(String sSessionId, String sProcessObjId, String sPayload) {
		
		WasdiLog.debugLog("ProcessWorkspaceResource.SetProcessPayload" );

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();

		try {
			// Domain Check
			if (oUser == null) {
				WasdiLog.debugLog("ProcessWorkspaceResource.SetProcessPayload: invalid session" );
				return oProcess;
			}

			WasdiLog.debugLog("ProcessWorkspaceResource.SetProcessPayload: process id " + sProcessObjId);
			WasdiLog.debugLog("ProcessWorkspaceResource.SetProcessPayload: PAYLOAD " + sPayload);

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process
			ProcessWorkspace oProcessWorkspace = oRepository.getProcessByProcessObjId(sProcessObjId);
			
			// Update payload
			oProcessWorkspace.setPayload(sPayload);

			oRepository.updateProcess(oProcessWorkspace);
			
			// Create return object
			oProcess = ProcessWorkspaceViewModel.buildProcessWorkspaceViewModel(oProcessWorkspace);

		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.SetProcessPayload: " + oEx);
		}

		return oProcess;
	}		
	
	/**
	 * Set the sub-pid for a running process workspace.
	 * Usually, pid is set by the launcher and is the pid of launcher process itself
	 * Sub-pid can be set via api and is the pid of the real running process in the application docker.
	 *
	 * @param sSessionId User Session Id
	 * @param sProcessObjId Process Workspace Id
	 * @param iSubPid Sub pid (in docker pid)
	 * @return Updated Process Workspace View Model
	 */
	@GET
	@Path("/setsubpid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceViewModel setSubProcessPid(@HeaderParam("x-session-token") String sSessionId, @QueryParam("procws") String sProcessObjId, @QueryParam("subpid") int iSubPid) {
		
		WasdiLog.debugLog("ProcessWorkspaceResource.setSubProcessPid( ProcWsId: " + sProcessObjId +", SubPid: " + iSubPid + " )" );

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();

		try {
			// Domain Check
			if (oUser == null) {
				WasdiLog.debugLog("ProcessWorkspaceResource.setSubProcessPid( ProcWsId: " + sProcessObjId +", Payload: " + iSubPid + " ): invalid session" );
				return oProcess;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return oProcess;
			}
			
			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			ProcessWorkspace oProcessWorkspace = oRepository.getProcessByProcessObjId(sProcessObjId);
			
			if (oProcessWorkspace == null) {
				return oProcess;
			}
			
			oProcessWorkspace.setSubprocessPid(iSubPid);

			oRepository.updateProcess(oProcessWorkspace);
			
			oProcess = ProcessWorkspaceViewModel.buildProcessWorkspaceViewModel(oProcessWorkspace);

		}
		catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.setSubProcessPid: " + oEx);
			oEx.printStackTrace();
		}

		return oProcess;
	}
	
	/**
	 * Get the payload of a Process Workspace
	 * @param sSessionId User Session Id
	 * @param sProcessObjId Process Workspace Id
	 * @return String containing the payload
	 */
	@GET
	@Path("/payload")
	@Produces({"application/xml", "application/json", "text/xml"})
	public String getPayload(@HeaderParam("x-session-token") String sSessionId, @QueryParam("procws") String sProcessObjId) {

		WasdiLog.debugLog("ProcessWorkspaceResource.getPayload(" + sProcessObjId + " )" );

		try {
			if(Utils.isNullOrEmpty(sSessionId)) {
				WasdiLog.debugLog("ProcessWorkspaceResource.getPayload: session is null or empty, aborting");
				return null;
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if(null == oUser) {
				WasdiLog.debugLog("ProcessWorkspaceResource.getPayload: invalid session" );
				return null;
			}
			if(PermissionsUtils.canUserAccessProcessWorkspace(oUser.getUserId(), sProcessObjId)) {
				ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
				return oProcessWorkspaceRepository.getPayload(sProcessObjId);
			} else {
				WasdiLog.debugLog("ProcessWorkspaceResource.getPayload: user " + oUser.getUserId() + " cannot access process obj id " + sProcessObjId );
			}
		}catch (Exception oE) {
			WasdiLog.debugLog("ProcessWorkspaceResource.getPayload: " + oE );
		}
		
		return null;
	}

	@GET
	@Path("/runningTime")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Long getRunningTime(@HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("userId") String sTargetUserId,
			@QueryParam("dateFrom") String sDateFrom, @QueryParam("dateTo") String sDateTo) {

		WasdiLog.debugLog("ProcessWorkspaceResource.getRunningTime");

		Long lRunningTime = null;

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("ProcessWorkspaceResource.getRunningTime: invalid session");
			return lRunningTime;
		}

		try {
			if (Utils.isNullOrEmpty(sTargetUserId)) {
				return lRunningTime;
			} else {
				UserRepository oUserRepository = new UserRepository();
				User oTargetUser = oUserRepository.getUser(sTargetUserId);

				if (oTargetUser == null) {
					return lRunningTime;
				}
			}

			// if the requesting-user is not super-user and if the requesting-user is not the target-user, then return null
			if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oUser.getRole(), ADMIN_DASHBOARD)
					&& !sTargetUserId.equalsIgnoreCase(oUser.getUserId())) {
				return lRunningTime;
			}



			if (Utils.isNullOrEmpty(sDateFrom)) {
				sDateFrom = "2020-01-01T00:00:00.000Z";
			}

			Date oDateFrom = Utils.getYyyyMMddTZDate(sDateFrom);

			if (oDateFrom == null) {
				return lRunningTime;
			}

			long lDateFrom = oDateFrom.getTime();



			if (Utils.isNullOrEmpty(sDateTo)) {
				sDateTo = "2099-12-31T23:59:59.999Z";
			}

			Date oDateTo = Utils.getYyyyMMddTZDate(sDateTo);

			if (oDateTo == null) {
				return lRunningTime;
			}

			long lDateTo = oDateTo.getTime();


			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			lRunningTime = oProcessWorkspaceRepository.getRunningTime(sTargetUserId, lDateFrom, lDateTo);

			// The main node needs to query also the others
			if (Wasdi.s_sMyNodeCode.equals("wasdi")) {
				NodeRepository oNodeRepo = new NodeRepository();
				List<Node> aoNodes = oNodeRepo.getNodesList();

				for (Node oNode : aoNodes) {
					if (oNode.getNodeCode().equals("wasdi")) continue;
					if (oNode.getActive() == false) continue;

					try {
						String sUrl = oNode.getNodeBaseAddress();
						if (!sUrl.endsWith("/")) sUrl += "/";
						sUrl += "process/runningTime?userId=" + sTargetUserId +"&dateFrom=" + sDateFrom + "&dateTo=" + sDateTo;

						Map<String, String> asHeaders = new HashMap<String, String>();
						asHeaders.put("x-session-token", sSessionId);

						WasdiLog.debugLog("ProcessWorkspaceResource.getRunningTime: calling url: " + sUrl);

						HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders); 
						String sResponse = oHttpCallResponse.getResponseBody();

						if (!Utils.isNullOrEmpty(sResponse)) {
							Long lRunningTimeOnNode = Long.valueOf(sResponse);

							lRunningTime += lRunningTimeOnNode;
						}
					} catch (Exception e) {
						WasdiLog.debugLog("ProcessWorkspaceResource.getRunningTime: exception contacting computing node: " + e.toString());
					}
				}
			}
		} catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.getRunningTime: " + oEx);
		}

		return lRunningTime;
	}
	
	/**
	 * Get a list of 
	 * @param sSessionId
	 * @param sNodeCode
	 * @param sStatuses
	 * @return
	 */
	public static List<ProcessWorkspaceAggregatedViewModel> getNodeQueuesStatus(String sSessionId, String sNodeCode, String sStatuses) {
		
		List<ProcessWorkspaceAggregatedViewModel> aoViewModel = new ArrayList<>();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("ProcessWorkspaceResource.getQueuesStatus: invalid session");
			return aoViewModel;
		}

		try {
			
			if (WasdiConfig.Current.nodeCode.equals("wasdi")) {
				if (Utils.isNullOrEmpty(sNodeCode)) {
					sNodeCode = "wasdi";
				}				
			}
			else {
				if (WasdiConfig.Current.nodeCode.equals(sNodeCode) == false) {
					WasdiLog.debugLog("ProcessWorkspaceResource.getQueuesStatus: distributed node answer only for itself");
					return aoViewModel;					
				}
			}


			NodeRepository oNodeRepo = new NodeRepository();
			Node oNode = oNodeRepo.getNodeByCode(sNodeCode);

			if (!sNodeCode.equals("wasdi")) {
				if (oNode == null) {
					WasdiLog.debugLog("ProcessWorkspaceResource.getQueuesStatus: Impossible to find node " + sNodeCode);
					return aoViewModel;
				}

				if (!oNode.getActive())  {
					WasdiLog.debugLog("ProcessWorkspaceResource.getQueuesStatus: node " + sNodeCode + " is not active");
					return aoViewModel;				
				}
			}
			
			if (Utils.isNullOrEmpty(sStatuses)) {
				sStatuses = "" + ProcessStatus.WAITING.name() + "," + ProcessStatus.READY.name() + "," + ProcessStatus.CREATED + "," + ProcessStatus.RUNNING; 
			}			
			
			if (WasdiConfig.Current.nodeCode.equals(sNodeCode)) {
				
				WasdiLog.debugLog("ProcessWorkspaceResource.getQueuesStatus: working on my node, read proc status from db");
				
				// Split the states
				String[] asStatuses = sStatuses.split(",");

				ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();

				// Retrive operations from the Database
				List<ProcessWorkspaceAgregatorByOperationTypeAndOperationSubtypeResult> aoResultsList =
						oProcessWorkspaceRepository.getQueuesByNodeAndStatuses(sNodeCode, asStatuses);

				// Create an aggregated map: Map<OperationType, Map<OperationSubType, Map<Status, Count>>>
				// For each operation type, for each subtype, map state - number of processes
				Map<String, Map<String, Map<String, Integer>>> aoPWAggregatedMap = new HashMap<>();

				for (ProcessWorkspaceAgregatorByOperationTypeAndOperationSubtypeResult oResult : aoResultsList) {
					
					// Get the operation type
					String sOperationType = oResult.getOperationType();
					
					// Check if we already have a map for this op-type, otherwise create it
					Map<String, Map<String, Integer>> aoOperationTypeMap = aoPWAggregatedMap.get(sOperationType);

					if (aoOperationTypeMap == null) {
						aoOperationTypeMap = new HashMap<>();
						aoPWAggregatedMap.put(sOperationType, aoOperationTypeMap);
					}

					// Get the operation subtype
					String sOperationSubType = oResult.getOperationSubType();
					if (Utils.isNullOrEmpty(sOperationSubType)) {
						sOperationSubType = s_sNoneSubtype;
					}

					// Check if we already have a map for this op-Sub-type, otherwise create it
					Map<String, Integer> aoOperationSubTypeMap = aoOperationTypeMap.get(sOperationSubType);

					if (aoOperationSubTypeMap == null) {
						aoOperationSubTypeMap = new HashMap<>();
						aoOperationTypeMap.put(sOperationSubType, aoOperationSubTypeMap);
					}
					
					// Add the count for this status
					String sStatus = oResult.getStatus();
					Integer iCount = oResult.getCount();
					aoOperationSubTypeMap.put(sStatus, iCount);
				}
				
				// Take the scheduler config to re-create queues structure
				List<SchedulerQueueConfig> aoQueueConfigs = WasdiConfig.Current.scheduler.schedulers;


				LauncherOperations [] aoOperations = LauncherOperations.class.getEnumConstants();
				List<String> asWasdiOperationTypes = Arrays.stream(aoOperations)
						.map(LauncherOperations::name)
						.collect(Collectors.toList());


				for (SchedulerQueueConfig oQueueConfig: aoQueueConfigs) {
					ProcessWorkspaceAggregatedViewModel oViewModel = new ProcessWorkspaceAggregatedViewModel();

					asWasdiOperationTypes.remove(oQueueConfig.opTypes);

					oViewModel.setSchedulerName(oQueueConfig.name);
					oViewModel.setOperationType(oQueueConfig.opTypes);
					oViewModel.setOperationSubType(oQueueConfig.opSubType);

					Map<String, Map<String, Integer>> aoOperationTypeMap = aoPWAggregatedMap.get(oQueueConfig.opTypes);
					if (aoOperationTypeMap != null) {
						
						String sSubType = oQueueConfig.opSubType;
						if (Utils.isNullOrEmpty(sSubType)) {
							sSubType = s_sNoneSubtype;
						}
						
						Map<String, Integer> aoOperationSubTypeMap = aoOperationTypeMap.get(sSubType);
						
						Integer iCreated = 0;
						Integer iRunning = 0;
						Integer iWaiting = 0;
						Integer iReady = 0;
						
						if (aoOperationSubTypeMap != null) {
							iCreated = aoOperationSubTypeMap.get(ProcessStatus.CREATED.name());
							iRunning = aoOperationSubTypeMap.get(ProcessStatus.RUNNING.name());
							iWaiting = aoOperationSubTypeMap.get(ProcessStatus.WAITING.name());
							iReady = aoOperationSubTypeMap.get(ProcessStatus.READY.name());
						}
						
						oViewModel.setProcCreated(iCreated);
						oViewModel.setProcRunning(iRunning);
						oViewModel.setProcWaiting(iWaiting);
						oViewModel.setProcReady(iReady);
						
					}

					aoViewModel.add(oViewModel);
				}


				// create the Default case
				ProcessWorkspaceAggregatedViewModel oViewModelDefaultCase = new ProcessWorkspaceAggregatedViewModel();
				
				// Initialize the sceheduler name and ops
				oViewModelDefaultCase.setSchedulerName("DEFAULT");
				oViewModelDefaultCase.setOperationType("OTHERS");
				oViewModelDefaultCase.setOperationSubType("");
				
				
				// For all the ops not present in other schedulers
				for (String sDefaultOperation : asWasdiOperationTypes) {
					// Take the summary of this operation
					Map<String, Map<String, Integer>> aoOperationTypeMap = aoPWAggregatedMap.get(sDefaultOperation);
					
					if (aoOperationTypeMap != null) {
						// Get the generic subtype
						Map<String, Integer> aoOperationSubTypeMap = aoOperationTypeMap.get(s_sNoneSubtype);
						if (aoOperationSubTypeMap != null) {
							
							Integer iCreated = aoOperationSubTypeMap.get(ProcessStatus.CREATED.name());
							Integer iRunning = aoOperationSubTypeMap.get(ProcessStatus.RUNNING.name());
							Integer iWaiting = aoOperationSubTypeMap.get(ProcessStatus.WAITING.name());
							Integer iReady = aoOperationSubTypeMap.get(ProcessStatus.READY.name());
							
							oViewModelDefaultCase.setProcCreated(oViewModelDefaultCase.getProcCreated()+iCreated);
							oViewModelDefaultCase.setProcRunning(oViewModelDefaultCase.getProcRunning()+iRunning);
							oViewModelDefaultCase.setProcWaiting(oViewModelDefaultCase.getProcWaiting() + iWaiting);
							oViewModelDefaultCase.setProcReady(oViewModelDefaultCase.getProcReady() + iReady);
						}
					}				
				}
				
				aoViewModel.add(oViewModelDefaultCase);				
			}
			else {
				
				WasdiLog.debugLog("ProcessWorkspaceResource.getQueuesStatus: working on remote node, call API to " + sNodeCode);
				
				// Ask to the node!!
				try {
					String sUrl = oNode.getNodeBaseAddress();
					if (sUrl.endsWith("/") == false) sUrl += "/";
					sUrl += "process/queuesStatus?nodeCode=" + sNodeCode + "&statuses=" + sStatuses;
					
					HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
					String sNodeResponse = oHttpCallResponse.getResponseBody();
					
					// Create an array of answers
					JSONArray oResults = new JSONArray(sNodeResponse);
					
					// Convert the View Models
					for (int iViewModels = 0; iViewModels<oResults.length(); iViewModels++) {
						Object oNodeQueueStatus = oResults.get(iViewModels);
						
						ProcessWorkspaceAggregatedViewModel oVM = (ProcessWorkspaceAggregatedViewModel) MongoRepository.s_oMapper.readValue(oNodeQueueStatus.toString(), ProcessWorkspaceAggregatedViewModel.class);
						aoViewModel.add(oVM);
					}
					
				}
				catch (Exception oNodeEx) {
					WasdiLog.debugLog("ProcessWorkspaceResource.getQueuesStatus: Exception contacting the remote node: " + oNodeEx);
				}
			}


		} catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.getQueuesStatus: " + oEx);
		}

		return aoViewModel;		
	}

	@GET
	@Path("/queuesStatus")
	@Produces({"application/xml", "application/json", "text/xml"})
	public List<ProcessWorkspaceAggregatedViewModel> getQueuesStatus(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("nodeCode") String sNodeCode,
			@QueryParam("statuses") String sStatuses) {
		WasdiLog.debugLog("ProcessWorkspaceResource.getQueuesStatus");
		
		return ProcessWorkspaceResource.getNodeQueuesStatus(sSessionId, sNodeCode, sStatuses);
	}

	@GET
	@Path("/nodesByScore")
	@Produces({"application/xml", "application/json", "text/xml"})
	public List<NodeScoreByProcessWorkspaceViewModel> getNodesSortedByScore(@HeaderParam("x-session-token") String sSessionId) {
		WasdiLog.debugLog("ProcessWorkspaceResource.getNodesSortedByScore");

		List<NodeScoreByProcessWorkspaceViewModel> aoViewModels = new ArrayList<>();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("ProcessWorkspaceResource.getNodesSortedByScore: invalid session");
			return aoViewModels;
		}

		try {
			if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oUser.getRole(), ADMIN_DASHBOARD)) {
				return aoViewModels;
			}
			
			return Wasdi.getNodesSortedByScore(sSessionId, null);
			

		} catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.getNodesSortedByScore: " + oEx);
		}

		return aoViewModels;
	}

}
