package it.fadeout.rest.resources;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import javax.ws.rs.core.Response.Status;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;

import it.fadeout.Wasdi;
import it.fadeout.services.ProcessWorkspaceService;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.aggregators.ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult;
import wasdi.shared.business.aggregators.ProcessWorkspaceAggregatorBySubscriptionAndProject;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.config.SchedulerQueueConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ParametersRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.wasdiAPI.ProcessWorkspaceAPIClient;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.processors.AppStatsViewModel;
import wasdi.shared.viewmodels.processors.ProcessHistoryViewModel;
import wasdi.shared.viewmodels.processworkspace.ComputingTimeViewModel;
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
		
		WasdiLog.debugLog("ProcessWorkspaceResource.getProcessByWorkspace( WS: " + sWorkspaceId +
				", status: " + sStatus + ", name pattern: " + sNamePattern +
				", Start: " + iStartIndex + ", End: " + iEndIndex);

		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessByWorkspace: workspace id is null, aborting");
				return aoProcessList;
			}
			
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessByWorkspace: invalid session");
				return aoProcessList;
			}
			
			if(!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessByWorkspace: user not allowed to access workspace" );
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
					WasdiLog.errorLog("ProcessWorkspaceResource.getProcessByWorkspace: could not convert " + sStatus + " to a valid process status, ignoring it");
				}
			}
			
			LauncherOperations eLauncherOperation = null;
			if(!Utils.isNullOrEmpty(sOperationType)) {
				try {
					eLauncherOperation = LauncherOperations.valueOf(sOperationType);
				} catch (Exception oE) {
					WasdiLog.errorLog("ProcessWorkspaceResource.getProcessByWorkspace: could not convert " + sOperationType + " to a valid operation type, ignoring it");
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
			WasdiLog.errorLog("ProcessWorkspaceResource.getProcessByWorkspace: " + oEx);
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
		
		WasdiLog.debugLog("ProcessWorkspaceResource.getProcessByUser");
		
		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();

		try {
			// Domain Check
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if(null == oUser) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessByUser: invalid session");
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
			WasdiLog.errorLog("ProcessWorkspaceResource.getProcessByUser: error retrieving process " + oEx);
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
		
		ArrayList<ProcessHistoryViewModel> aoProcessList = new ArrayList<ProcessHistoryViewModel>();
		
		try {
			WasdiLog.debugLog("ProcessWorkspaceResource.getProcessByApplication  sProcessorName=" + sProcessorName);
			
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if(null == oUser) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessByApplication: invalid session");
				return aoProcessList;
			}			
			
			// Domain Check
			if(Utils.isNullOrEmpty(sProcessorName)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessByApplication: invalid processor name");
				return aoProcessList;
			}
			
			// checks that processor is in db -> needed to avoid url injection from users 
			ProcessorRepository oProcessRepository = new ProcessorRepository();
			
			if (null == oProcessRepository.getProcessorByName(sProcessorName) ) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessByApplication Processor name not found in DB, aborting");
				return aoProcessList;
			}
			
			if (!PermissionsUtils.canUserAccessProcessorByName(oUser.getUserId(), sProcessorName)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessByApplication: user cannot access processor");
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
					WasdiLog.errorLog("ProcessWorkspaceResource.getProcessByApplication: exception generating History View Model " + oEx.toString());
				}
				
				if (oRun.getWorkspaceName().equals("wasdi_specific_node_ws_code")) continue;
				
				aoProcessList.add( oRun);
			}
			
			// The main node needs to query also the others
			if (WasdiConfig.Current.isMainNode()) {
				
				NodeRepository oNodeRepo = new NodeRepository();
				List<Node> aoNodes = oNodeRepo.getNodesList();
				
				for (Node oNode : aoNodes) {
					
					if (oNode.getNodeCode().equals("wasdi")) continue;					
					if (oNode.getActive() == false) continue;
					
					try {
						HttpCallResponse oHttpCallResponse = ProcessWorkspaceAPIClient.getByApplication(oNode, sSessionId, sProcessorName); 
						String sResponse = oHttpCallResponse.getResponseBody(); 
						
						if (Utils.isNullOrEmpty(sResponse)==false) {
							ArrayList<ProcessHistoryViewModel> aoNodeHistory = MongoRepository.s_oMapper.readValue(sResponse, new TypeReference<ArrayList<ProcessHistoryViewModel>>(){});
							if (aoNodeHistory != null) {
								aoProcessList.addAll(aoNodeHistory);
							}
						}
						
					}
					catch (Exception e) {
						WasdiLog.errorLog("ProcessWorkspaceResource.getProcessByApplication: exception contacting computing node: " + e.toString());
					}
				}
				
			}
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceResource.getProcessByApplication: error retrieving process " + oEx);
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
		
		AppStatsViewModel oReturnStats = new AppStatsViewModel();
		oReturnStats.setApplicationName(sProcessorName);
		
		try {			
			
			WasdiLog.debugLog("ProcessWorkspaceResource.getApplicationStatistics( Session: " + sSessionId + ", processorName: " + sProcessorName + " )");
			
			User oUser = Wasdi.getUserFromSession(sSessionId);
			
			if(oUser == null) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getApplicationStatistics: invalid session");
				return oReturnStats;
			}			
			
			// Domain Check
			if(Utils.isNullOrEmpty(sProcessorName)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getApplicationStatistics: invalid processor name, aborting");
				return oReturnStats;
			}
			
			// checks that processor is in db -> needed to avoid url injection from users 
			ProcessorRepository oProcessRepository = new ProcessorRepository();
			if (null == oProcessRepository.getProcessorByName(sProcessorName) ) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getApplicationStatistics: Processor name not found in DB, aborting");
				return oReturnStats;
			}
			
			if (!PermissionsUtils.canUserAccessProcessorByName(oUser.getUserId(), sProcessorName)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getApplicationStatistics: user cannot access the processor");
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
					WasdiLog.errorLog("ProcessWorkspaceResource.getApplicationStatistics: exception generating History View Model " + oEx.toString());
				}
				
			}
			
			
			// The main node needs to query also the others
			if (WasdiConfig.Current.isMainNode()) {
				
				NodeRepository oNodeRepo = new NodeRepository();
				List<Node> aoNodes = oNodeRepo.getNodesList();
				
				for (Node oNode : aoNodes) {
					
					if (oNode.getNodeCode().equals("wasdi")) continue;					
					if (oNode.getActive() == false) continue;
					
					try {						
						HttpCallResponse oHttpCallResponse = ProcessWorkspaceAPIClient.getAppStats(oNode, sSessionId, sProcessorName);
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
						WasdiLog.errorLog("ProcessWorkspaceResource.getApplicationStatistics: exception contacting computing node: " + e.toString());
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
			WasdiLog.errorLog("ProcessWorkspaceResource.getApplicationStatistics: error retrieving process " + oEx);
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
				WasdiLog.warnLog("ProcessWorkspaceRepository.getLastProcessByWorkspace: invalid session");
				return aoProcessList;
			}
			
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getLastProcessByWorkspace: ws id is null");
				return aoProcessList;
			}
			
			if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
				WasdiLog.warnLog("ProcessWorkspaceRepository.getLastProcessByWorkspace: user cannot access the workspace");
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
			WasdiLog.errorLog("ProcessWorkspaceResource.getLastProcessByWorkspace: " + oEx);
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
		
		WasdiLog.debugLog("ProcessWorkspaceResource.getLastProcessByUser");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();

		try {
			// Domain Check
			if (oUser == null) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getLastProcessByUser: invalid session");
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
			WasdiLog.errorLog("ProcessWorkspaceResource.getLastProcessByUser: " + oEx);
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
	public ProcessWorkspaceSummaryViewModel getSummary(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspaceId) {
		
		WasdiLog.debugLog("ProcessWorkspaceResource.getSummary");
		ProcessWorkspaceSummaryViewModel oSummaryViewModel = new ProcessWorkspaceSummaryViewModel();

		try {
			
			User oUser = Wasdi.getUserFromSession(sSessionId);

			// Domain Check
			if (oUser == null) {
				WasdiLog.warnLog("ProcessWorkspaceResource.GetSummary: invalid session");
				return oSummaryViewModel;
			}
						
			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			
			long lUserWaiting = oRepository.getCreatedCountByUser(oUser.getUserId(), true, sWorkspaceId);
			long lOthersWaiting = oRepository.getCreatedCountByUser(oUser.getUserId(), false, sWorkspaceId);
			
			long lUserRunning = oRepository.getRunningCountByUser(oUser.getUserId(), true, sWorkspaceId);
			long lOthersRunning = oRepository.getRunningCountByUser(oUser.getUserId(), false, sWorkspaceId);
			
			oSummaryViewModel.setUserProcessRunning((int) lUserRunning);
			oSummaryViewModel.setUserProcessWaiting((int) lUserWaiting);
			oSummaryViewModel.setAllProcessRunning((int)(lUserRunning+lOthersRunning));
			oSummaryViewModel.setAllProcessWaiting((int)(lUserWaiting+lOthersWaiting));			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceResource.getSummary error: " + oEx);
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

		WasdiLog.debugLog("ProcessWorkspaceResource.deleteProcess( Process: " + sToKillProcessObjId + ", treeKill: " + bKillTheEntireTree + " )");

		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);
			// Domain Check
			if (oUser == null) {
				WasdiLog.warnLog("ProcessWorkspaceResource.deleteProcess: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if(Utils.isNullOrEmpty(sToKillProcessObjId)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.deleteProcess: processObjId is null or empty, aborting");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessToKill = oRepository.getProcessByProcessObjId(sToKillProcessObjId);

			//check that the process exists
			if(null==oProcessToKill) {
				WasdiLog.warnLog("ProcessWorkspaceResource.deleteProcess: process not found in DB, aborting");
				return Response.status(Status.BAD_REQUEST).build();
			}

			// check that the user can access the processWorkspace
			if(!PermissionsUtils.canUserWriteProcessWorkspace(oUser.getUserId(), sToKillProcessObjId)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.deleteProcess: user cannot access requested process workspace");
				return Response.status(Status.FORBIDDEN).build();
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
			WasdiLog.errorLog("WorkspaceResource.deleteProcess: " + oEx);
		}

		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
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
		
		WasdiLog.debugLog("ProcessWorkspaceResource.getProcessById( ProcWsId: " + sProcessWorkspaceId + " )");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();
		
		// Initialize status as ERROR
		oProcess.setStatus("ERROR");

		try {
			// Domain Check
			if (oUser == null) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessById: invalid session");
				return oProcess;
			}
			
			if (!PermissionsUtils.canUserAccessProcessWorkspace(oUser.getUserId(), sProcessWorkspaceId)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessById: user cannot access the process workspace");
				return oProcess;				
			}
			
			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			ProcessWorkspace oProcessWorkspace = oRepository.getProcessByProcessObjId(sProcessWorkspaceId);
			oProcess = ProcessWorkspaceViewModel.buildProcessWorkspaceViewModel(oProcessWorkspace);

		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceResource.getProcessById: " + oEx);
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
		ArrayList<String> asFullReturnStatusList = new ArrayList<String>();

		try {
			// Domain Check
			if (oUser == null) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getStatusProcessesById: invalid session");
				return asReturnStatusList;
			}
			
			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			
			ArrayList<String> asFiltered = new ArrayList<>();
			ArrayList<Integer> aiFilteredIndexes = new ArrayList<>();
			 
			for (int iIndex = 0; iIndex<asProcessesWorkspaceId.size(); iIndex++) {
				
				String sProcId = asProcessesWorkspaceId.get(iIndex);
				
				if (Utils.isNullOrEmpty(sProcId)) {
					WasdiLog.warnLog("ProcessWorkspaceResource.getStatusProcessesById: requesting proc id is null or empty index "+ iIndex);
					asProcessesWorkspaceId.set(iIndex, ProcessStatus.ERROR.name());
					continue;
				}
				
				if (sProcId.equals(ProcessStatus.DONE.name()) ||  sProcId.equals(ProcessStatus.ERROR.name()) || sProcId.equals(ProcessStatus.STOPPED.name()) ) {
					WasdiLog.warnLog("ProcessWorkspaceResource.getStatusProcessesById: requesting proc id is already a status jump it index " + iIndex);
					continue;					
				}
				
				if (!PermissionsUtils.canUserAccessProcessWorkspace(oUser.getUserId(), sProcId)) {
					WasdiLog.warnLog("ProcessWorkspaceResource.getStatusProcessesById: requesting proc id that cannot be accessed index " + iIndex);
					asProcessesWorkspaceId.set(iIndex, ProcessStatus.ERROR.name());
					continue;
				}				
				
				aiFilteredIndexes.add(iIndex);
				asFiltered.add(sProcId);				 
			}
			
			if (asFiltered.size()==0) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getStatusProcessesById: all the proc id were Empty or statuses, return the fixed list");
				return asProcessesWorkspaceId;
			}

			// Get Process Status List
			asReturnStatusList = oRepository.getProcessesStatusByProcessObjId(asFiltered);
			
			for (int iIndex = 0; iIndex<asProcessesWorkspaceId.size(); iIndex++) {
				if (aiFilteredIndexes.contains(iIndex)) {
					int iFilteredOldIndex = aiFilteredIndexes.indexOf(iIndex);
					asFullReturnStatusList.add(asReturnStatusList.get(iFilteredOldIndex));
				}
				else {
					asFullReturnStatusList.add(asProcessesWorkspaceId.get(iIndex));
				}
			}
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceResource.getStatusProcessesById error: " + oEx);
		}

		return asFullReturnStatusList;
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
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessStatusById: invalid session" );
				return ProcessStatus.ERROR.name();
			}
			
			if (sProcessObjId.equals(ProcessStatus.DONE.name()) ||  sProcessObjId.equals(ProcessStatus.ERROR.name()) || sProcessObjId.equals(ProcessStatus.STOPPED.name()) ) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessStatusById: proc id is already a status" );
				return sProcessObjId;				
			}			
			
			if(!PermissionsUtils.canUserAccessProcessWorkspace(oUser.getUserId(), sProcessObjId)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessStatusById: user cannot access process workspace" );
				return ProcessStatus.ERROR.name();
			}
			
			if (Utils.isNullOrEmpty(sProcessObjId)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessStatusById: proc id is null or empty" );
				return ProcessStatus.ERROR.name();				
			}

			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			return oProcessWorkspaceRepository.getProcessStatusFromId(sProcessObjId);

		} catch (Exception oE) {
			WasdiLog.errorLog("ProcessWorkspaceResource.getProcessStatusById error: " + oE );
		}
		return ProcessStatus.ERROR.name();
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
		
		WasdiLog.debugLog("ProcessWorkspaceResource.updateProcessById( ProcWsId: " + sProcessObjId + ", Status: " + sNewStatus + ", Perc: " + iPerc + ", SendRabbit:" + sSendToRabbit + " )" );

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();

		try {
			// Domain Check
			if (oUser == null) {
				WasdiLog.warnLog("ProcessWorkspaceResource.updateProcessById: invalid session");
				return oProcess;
			}
			
			if (!PermissionsUtils.canUserWriteProcessWorkspace(oUser.getUserId(), sProcessObjId)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.updateProcessById: user cannot write Process Workspace");
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
					WasdiLog.debugLog("ProcessWorkspaceResource.updateProcessById - update process end date" );
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
			
			WasdiLog.debugLog("ProcessWorkspaceResource.updateProcessById( ProcWsId: " + sProcessObjId + ", Status updated : " +  oProcess.getStatus());

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
			WasdiLog.errorLog("ProcessWorkspaceResource.updateProcessById: " + oEx);
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
		
		WasdiLog.debugLog("ProcessWorkspaceResource.internalSetPaylod" );

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();

		try {
			// Domain Check
			if (oUser == null) {
				WasdiLog.warnLog("ProcessWorkspaceResource.internalSetPaylod: invalid session" );
				return oProcess;
			}

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			// Get Process
			ProcessWorkspace oProcessWorkspace = oRepository.getProcessByProcessObjId(sProcessObjId);
			
			if (oProcessWorkspace == null) {
				WasdiLog.warnLog("ProcessWorkspaceResource.internalSetPaylod: invalidprocess workspace id" );
				return oProcess;				
			}

			if (!PermissionsUtils.canUserWriteProcessWorkspace(oUser.getUserId(), sProcessObjId)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.internalSetPaylod: user cannot write process workspace id" );
				return oProcess;				
			}
			
			// Update payload
			oProcessWorkspace.setPayload(sPayload);

			oRepository.updateProcess(oProcessWorkspace);
			
			// Create return object
			oProcess = ProcessWorkspaceViewModel.buildProcessWorkspaceViewModel(oProcessWorkspace);

		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceResource.internalSetPaylod error: " + oEx);
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
				WasdiLog.warnLog("ProcessWorkspaceResource.setSubProcessPid: invalid session" );
				return oProcess;
			}
			
			if (!PermissionsUtils.canUserWriteProcessWorkspace(oUser.getUserId(), sProcessObjId)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.setSubProcessPid: user cannot access process workspace" );
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
			WasdiLog.errorLog("ProcessWorkspaceResource.setSubProcessPid: " + oEx);
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
			User oUser = Wasdi.getUserFromSession(sSessionId);
			
			if(null == oUser) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getPayload: invalid session" );
				return null;
			}
			
			if(!PermissionsUtils.canUserAccessProcessWorkspace(oUser.getUserId(), sProcessObjId)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getPayload: user cannot access process workspace");
				return null;
			} 
			
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			return oProcessWorkspaceRepository.getPayload(sProcessObjId);
			
		}catch (Exception oE) {
			WasdiLog.errorLog("ProcessWorkspaceResource.getPayload error: " + oE );
		}
		
		return null;
	}

	@GET
	@Path("/runningTime/UI")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Long getRunningTimeByUserAndInterval(@HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("userId") String sTargetUserId,
			@QueryParam("dateFrom") String sDateFrom, @QueryParam("dateTo") String sDateTo) {

		WasdiLog.debugLog("ProcessWorkspaceResource.getRunningTimeByUserAndInterval");

		Long lRunningTime = null;

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("ProcessWorkspaceResource.getRunningTimeByUserAndInterval: invalid session");
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
			if (!UserApplicationRole.isAdmin(oUser)
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
			if (WasdiConfig.Current.isMainNode()) {
				NodeRepository oNodeRepo = new NodeRepository();
				List<Node> aoNodes = oNodeRepo.getNodesList();

				for (Node oNode : aoNodes) {
					if (oNode.getNodeCode().equals("wasdi")) continue;
					if (oNode.getActive() == false) continue;

					try {
						HttpCallResponse oHttpCallResponse = ProcessWorkspaceAPIClient.getRunningTimeUI(oNode, sSessionId, sTargetUserId, sDateFrom, sDateTo); 
						String sResponse = oHttpCallResponse.getResponseBody();

						if (!Utils.isNullOrEmpty(sResponse)) {
							Long lRunningTimeOnNode = Long.valueOf(sResponse);

							lRunningTime += lRunningTimeOnNode;
						}
					} catch (Exception oEx) {
						WasdiLog.errorLog("ProcessWorkspaceResource.getRunningTimeByUserAndInterval: exception contacting computing node: " + oEx.toString());
					}
				}
			}
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceResource.getRunningTimeByUserAndInterval error: " + oEx);
		}

		return lRunningTime;
	}
	
	@GET
	@Path("/runningTime/SP")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Map<String, Map<String, Long>> getRunningTimeBySubscriptionAndProject(@HeaderParam("x-session-token") String sSessionId) {

		WasdiLog.debugLog("ProcessWorkspaceResource.getRunningTimeBySubscriptionAndProject");

		Map<String, Map<String, Long>> aoRunningTimeBySubscriptionByProject = new HashMap<String, Map<String,Long>>();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("ProcessWorkspaceResource.getRunningTimeBySubscriptionAndProject: invalid session");
			return aoRunningTimeBySubscriptionByProject;
		}

		try {
			Collection<String> asIdsOfSubscriptionsAssociatedWithUser =
					new SubscriptionResource().getIdsOfSubscriptionsAssociatedWithUser(oUser.getUserId());

			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			List<ProcessWorkspaceAggregatorBySubscriptionAndProject> aoResultList =
					oProcessWorkspaceRepository.getRunningTimeInfo(asIdsOfSubscriptionsAssociatedWithUser);

			for (ProcessWorkspaceAggregatorBySubscriptionAndProject oResult : aoResultList) {
				Map<String, Long> aoRunningTimeByProject = aoRunningTimeBySubscriptionByProject.get(oResult.getSubscriptionId());

				if (aoRunningTimeByProject == null) {
					aoRunningTimeByProject = new HashMap<>();
					aoRunningTimeBySubscriptionByProject.put(oResult.getSubscriptionId(), aoRunningTimeByProject);
				}

				Long lTotalRunningTime = aoRunningTimeByProject.get(oResult.getProjectId());

				if (lTotalRunningTime == null) {
					lTotalRunningTime = Long.valueOf(0);
					aoRunningTimeByProject.put(oResult.getProjectId(), lTotalRunningTime);
				}

				aoRunningTimeByProject.put(oResult.getProjectId(), aoRunningTimeByProject.get(oResult.getProjectId()) + oResult.getTotal());
			}

			// The main node needs to query also the others
			if (WasdiConfig.Current.isMainNode()) {
				NodeRepository oNodeRepo = new NodeRepository();
				List<Node> aoNodes = oNodeRepo.getNodesList();

				for (Node oNode : aoNodes) {
					if (oNode.getNodeCode().equals("wasdi")) continue;
					if (oNode.getActive() == false) continue;

					try {

						HttpCallResponse oHttpCallResponse = ProcessWorkspaceAPIClient.getRunningTimeBySubscriptionAndProject(oNode, sSessionId); 
						String sResponse = oHttpCallResponse.getResponseBody();

						if (!Utils.isNullOrEmpty(sResponse)) {
							Map<String, Map<String, Long>> aoRunningTimeBySubscriptionByProjectFromNode = MongoRepository.s_oMapper.readValue(sResponse, new TypeReference<Map<String, Map<String, Long>>>(){});
							if (aoRunningTimeBySubscriptionByProjectFromNode != null) {
								for (Map.Entry<String, Map<String, Long>> oEntrySubscription : aoRunningTimeBySubscriptionByProjectFromNode.entrySet()) {
									String sSubscriptionId = oEntrySubscription.getKey();
									Map<String, Long> aoRunningTimeByProjectFromNode = oEntrySubscription.getValue();

									Map<String, Long> aoRunningTimeByProject = aoRunningTimeBySubscriptionByProject.get(sSubscriptionId);

									if (aoRunningTimeByProject == null) {
										aoRunningTimeBySubscriptionByProject.put(sSubscriptionId, aoRunningTimeByProjectFromNode);
									} else {
										for (Map.Entry<String, Long> oEntryProject : aoRunningTimeByProjectFromNode.entrySet()) {
											String sProjectId = oEntryProject.getKey();
											Long lRunningTimeFromNode = oEntryProject.getValue();

											Long lRunningTime = aoRunningTimeByProject.get(sProjectId);

											if (lRunningTime == null) {
												aoRunningTimeByProject.put(sProjectId, lRunningTimeFromNode);
											} else {
												aoRunningTimeByProject.put(sProjectId, lRunningTime + lRunningTimeFromNode);
											}
										}
									}
								}
							}
						}
					} catch (Exception oEx) {
						WasdiLog.errorLog("ProcessWorkspaceResource.getRunningTimeBySubscriptionAndProject: exception contacting computing node: " + oEx.toString());
					}
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceResource.getRunningTimeBySubscriptionAndProject error: " + oEx);
		}

		return aoRunningTimeBySubscriptionByProject;
	}
	
	/**
	 * Get the overall computing time of projects associated to user. The overall computing time takes into account the computing times of ALL the users
	 * who worked on the project, and not only the computing time of the user in the corresponding session
	 * @param sSessionId user session id
	 * @return the list of view models 
	 */
	@GET
	@Path("/runningtimeproject")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response getOverallRunningTimeProject(@HeaderParam("x-session-token") String sSessionId) {

		WasdiLog.debugLog("ProcessWorkspaceResource.getOverallRunningTimeProject");

		// OUTER MAP. Keys: subscription-id, Values: dictionary
		// INNER MAPS: Keys: project-id, Values: computing time
		Map<String, Map<String, Long>> aoRunningTimeBySubscription = new HashMap<String, Map<String,Long>>();
		

		try {
			
			User oUser = Wasdi.getUserFromSession(sSessionId);
			

			if (oUser == null) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getOverallRunningTimeProject: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			WasdiLog.debugLog("ProcessWorkspaceResource.getOverallRunningTimeProject: user id: " + oUser.getUserId());
			
			// ids of the subscriptions associated to the user
			Collection<String> asSubscriptionIds =new SubscriptionResource().getIdsOfSubscriptionsAssociatedWithUser(oUser.getUserId());
			List<ProcessWorkspaceAggregatorBySubscriptionAndProject> aoRunningTimes = new ProcessWorkspaceRepository().getRunningTimeInfo(asSubscriptionIds);
			aoRunningTimes.forEach(oRunningTimeInfo -> updateRunningTimesMap(aoRunningTimeBySubscription, oRunningTimeInfo));
			
			
			if (WasdiConfig.Current.isMainNode()) {
				NodeRepository oNodeRepo = new NodeRepository();
				List<Node> aoNodes = oNodeRepo.getNodesList();

				for (Node oNode : aoNodes) {
					if (oNode.getNodeCode().equals("wasdi")) continue;
					if (oNode.getActive() == false) continue;

					try {
						WasdiLog.debugLog("ProcessWorkspaceResource.getOverallRunningTimeProject: send request to computing nodes");
						HttpCallResponse oHttpCallResponse = ProcessWorkspaceAPIClient.getRunningTimePerProject(oNode, sSessionId); 
						String sResponseBody = oHttpCallResponse.getResponseBody();
						
						if (!Utils.isNullOrEmpty(sResponseBody)) {
							WasdiLog.debugLog("ProcessWorkspaceResource.getOverallRunningTimeProject: response code " + oHttpCallResponse.getResponseCode());
							WasdiLog.debugLog("ProcessWorkspaceResource.getOverallRunningTimeProject: response body " + sResponseBody);
							
							// Create an array of answers
							JSONArray oResults = new JSONArray(sResponseBody);
							
							// Convert the View Models
							for (int iViewModel = 0; iViewModel < oResults.length(); iViewModel++) {
								JSONObject oViewModel = oResults.getJSONObject(iViewModel);
								String sSubscriptionId = oViewModel.optString("subscriptionId");
								String sProjectId = oViewModel.optString("projectId");
								String sUserId = oViewModel.optString("userId");
								Long lComputingTime = oViewModel.optLong("computingTime");
								
								WasdiLog.debugLog("ProcessWorkspaceResource.getOverallRunningTimeProject: subscription: " + sSubscriptionId + ", project: " + sProjectId + ", user: " + sUserId);
								
	//							if (!oUser.getUserId().equals(sUserId)) {
	//								WasdiLog.errorLog("ProcessWorkspaceResource.getOverallRunningTimeProject: mismatch between the user in the main node and the user in the computing node");
	//								return Response.serverError().build();
	//							}
								
								if (Utils.isNullOrEmpty(sSubscriptionId) || Utils.isNullOrEmpty(sProjectId)) {
									WasdiLog.errorLog("ProcessWorkspaceResource.getOverallRunningTimeProject: subscription id or project id not available.");
									return Response.serverError().build();
								}
								
								updateRunningTimesMap(aoRunningTimeBySubscription, sSubscriptionId, sProjectId, lComputingTime);
								WasdiLog.debugLog("ProcessWorkspaceResource.getOverallRunningTimeProject: general statistics should be updated with the statistics from node");
								
							}
						}
						else {
							WasdiLog.debugLog("ProcessWorkspaceResource.getOverallRunningTimeProject: response body from computing node is empty");
						}
					} catch (Exception oEx) {
						WasdiLog.errorLog("ProcessWorkspaceResource.getOverallRunningTimeProject: exception contacting computing node", oEx);
						return Response.serverError().build();
					}
				}
				
			}
			
			List<ComputingTimeViewModel> aoComputingTimesList = buildComputingTimeViewModel(aoRunningTimeBySubscription, oUser.getUserId());
			return Response.ok(aoComputingTimesList).build();
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceResource.getRunningTimeBySubscriptionAndProject error: " + oEx);
			return Response.serverError().build();
		}
					
	}
	
	/**
	 * Get the user computing time for the projects associated to them. The computing time refers only to the computing time of the user on the projects, and not
	 * the computing time of other users on the same projects.
	 * @param sSessionId user session id
	 * @return the list of view models 
	 */
	@GET
	@Path("/runningtimeproject/byuser")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response getProjectRunningTimeByUser(@HeaderParam("x-session-token") String sSessionId) {

		WasdiLog.debugLog("ProcessWorkspaceResource.getProjectRunningTimeByUser");

		Map<String, Map<String, Long>> aoRunningTimesBySubscription = new HashMap<String, Map<String,Long>>();
		
		
		try {
			
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProjectRunningTimeByUser: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			String sUserId = oUser.getUserId();
			Collection<String> asUserSubscriptionsIds = new SubscriptionResource().getIdsOfSubscriptionsAssociatedWithUser(sUserId);
			List<ProcessWorkspaceAggregatorBySubscriptionAndProject> aoRunningTimes = new ProcessWorkspaceRepository().getProjectRunningTime(sUserId, asUserSubscriptionsIds);
			aoRunningTimes.forEach(oRunningTimeInfo -> updateRunningTimesMap(aoRunningTimesBySubscription, oRunningTimeInfo));
			
			if (WasdiConfig.Current.isMainNode()) {
				NodeRepository oNodeRepo = new NodeRepository();
				List<Node> aoNodes = oNodeRepo.getNodesList();

				for (Node oNode : aoNodes) {
					if (oNode.getNodeCode().equals("wasdi")) continue;
					if (oNode.getActive() == false) continue;

					try {
						WasdiLog.debugLog("ProcessWorkspaceResource.getProjectRunningTimeByUser: response body from computing node is empty");
						HttpCallResponse oHttpCallResponse = ProcessWorkspaceAPIClient.getRunningTimePerProjectPerUser(oNode, sSessionId); 
						String sResponse = oHttpCallResponse.getResponseBody();

						if (!Utils.isNullOrEmpty(sResponse)) {
							WasdiLog.debugLog("ProcessWorkspaceResource.getOverallRunningTimeProject: response code " + oHttpCallResponse.getResponseCode());
							WasdiLog.debugLog("ProcessWorkspaceResource.getOverallRunningTimeProject: response body " + sResponse);
							
							
							// Create an array of answers
							JSONArray oResults = new JSONArray(sResponse);
							
							// Convert the View Models
							for (int iViewModel = 0; iViewModel < oResults.length(); iViewModel++) {
								JSONObject oViewModel = oResults.getJSONObject(iViewModel);
								String sSubscriptionId = oViewModel.optString("subscriptionId");
								String sProjectId = oViewModel.optString("projectId");
								
								Long lComputingTime = oViewModel.optLong("computingTime");
								
								WasdiLog.debugLog("ProcessWorkspaceResource.getOverallRunningTimeProject: subscription: " + sSubscriptionId + ", project: " + sProjectId + ", user: " + sUserId);
								
								if (Utils.isNullOrEmpty(sSubscriptionId) || Utils.isNullOrEmpty(sProjectId)) {
									WasdiLog.errorLog("ProcessWorkspaceResource.getOverallRunningTimeProject: subscription id or project id not available.");
									return Response.serverError().build();
								}
								
								updateRunningTimesMap(aoRunningTimesBySubscription, sSubscriptionId, sProjectId, lComputingTime);
								WasdiLog.debugLog("ProcessWorkspaceResource.getOverallRunningTimeProject: general statistics should be updated with the statistics from node");

							}
//							WasdiLog.debugLog("ProcessWorkspaceResource.getOverallRunningTimeProject: got a not-empty answer from node. Updating the computing statistics");
//							Map<String, Map<String, Long>> aoSubscriptionRunningTimesFromNode = MongoRepository.s_oMapper.readValue(sResponse, new TypeReference<Map<String, Map<String, Long>>>(){});
//							updateRunningTimesMap(aoRunningTimesBySubscription, aoSubscriptionRunningTimesFromNode);
						}
						else {
							WasdiLog.debugLog("ProcessWorkspaceResource.getProjectRunningTimeByUser: response body from computing node is empty");
						}
					} catch (Exception oEx) {
						WasdiLog.errorLog("ProcessWorkspaceResource.getProjectRunningTimeByUser: exception contacting computing node: " + oEx.toString());
						return Response.serverError().build();
					}
				}
			} 
			
			List<ComputingTimeViewModel>  aoComputingTimesList = buildComputingTimeViewModel(aoRunningTimesBySubscription, oUser.getUserId());
			return Response.ok(aoComputingTimesList).build();
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceResource.getRunningTimeBySubscriptionAndProject error: " + oEx);
			return Response.serverError().build();
		}			
	}
	
	
	/**
	 * Get a Process Workspace View Model by id
	 *
	 * @param sSessionId User Session Id
	 * @param sProcessWorkspaceId Process Workspace ID
	 * @return Process Workspace View Model
	 */
	@GET
	@Path("/paramsbyid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response getProcessParameters(@HeaderParam("x-session-token") String sSessionId, @QueryParam("procws") String sProcessWorkspaceId) {
		
		WasdiLog.debugLog("ProcessWorkspaceResource.getProcessParameters( ProcWsId: " + sProcessWorkspaceId + " )");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();
		
		// Initialize status as ERROR
		oProcess.setStatus("ERROR");

		try {
			// Domain Check
			if (oUser == null) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessParameters: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			if (!PermissionsUtils.canUserAccessProcessWorkspace(oUser.getUserId(), sProcessWorkspaceId)) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessParameters: user cannot access the process workspace");
				return Response.status(Status.FORBIDDEN).build();
			}
			
			// Create repo
			ParametersRepository oParametersRepository = new ParametersRepository();
			BaseParameter oBaseParameter = oParametersRepository.getParameterByProcessObjId(sProcessWorkspaceId);
			
			if (oBaseParameter == null) {
				WasdiLog.warnLog("ProcessWorkspaceResource.getProcessParameters: user cannot access the process workspace");
				return Response.status(Status.BAD_REQUEST).build();			
			}
			
			if (oBaseParameter.getClass()==ProcessorParameter.class) {
				ProcessorParameter oProcessorParameter = (ProcessorParameter) oBaseParameter;
				String sJson = oProcessorParameter.getJson();
				return Response.ok(sJson).build();
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceResource.getProcessParameters: " + oEx);
		}

		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
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
			WasdiLog.warnLog("ProcessWorkspaceResource.getNodeQueuesStatus: invalid session");
			return aoViewModel;
		}

		try {
			
			if (WasdiConfig.Current.isMainNode()) {
				if (Utils.isNullOrEmpty(sNodeCode)) {
					sNodeCode = "wasdi";
				}				
			}
			else {
				if (WasdiConfig.Current.nodeCode.equals(sNodeCode) == false) {
					WasdiLog.debugLog("ProcessWorkspaceResource.getNodeQueuesStatus: distributed node answer only for itself");
					return aoViewModel;					
				}
			}

			NodeRepository oNodeRepo = new NodeRepository();
			Node oNode = oNodeRepo.getNodeByCode(sNodeCode);

			if (!sNodeCode.equals("wasdi")) {
				if (oNode == null) {
					WasdiLog.debugLog("ProcessWorkspaceResource.getNodeQueuesStatus: Impossible to find node " + sNodeCode);
					return aoViewModel;
				}

				if (!oNode.getActive())  {
					WasdiLog.debugLog("ProcessWorkspaceResource.getNodeQueuesStatus: node " + sNodeCode + " is not active");
					return aoViewModel;				
				}
			}
			
			if (Utils.isNullOrEmpty(sStatuses)) {
				sStatuses = "" + ProcessStatus.WAITING.name() + "," + ProcessStatus.READY.name() + "," + ProcessStatus.CREATED + "," + ProcessStatus.RUNNING; 
			}			
			
			if (WasdiConfig.Current.nodeCode.equals(sNodeCode)) {
				
				WasdiLog.debugLog("ProcessWorkspaceResource.getNodeQueuesStatus: working on my node, read proc status from db");
				
				// Split the states
				String[] asStatuses = sStatuses.split(",");

				ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();

				// Retrive operations from the Database
				List<ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult> aoResultsList =
						oProcessWorkspaceRepository.getQueuesByNodeAndStatuses(sNodeCode, asStatuses);

				// Create an aggregated map: Map<OperationType, Map<OperationSubType, Map<Status, Count>>>
				// For each operation type, for each subtype, map state - number of processes
				Map<String, Map<String, Map<String, Integer>>> aoPWAggregatedMap = new HashMap<>();

				for (ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult oResult : aoResultsList) {
					
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
				
				WasdiLog.debugLog("ProcessWorkspaceResource.getNodeQueuesStatus: working on remote node, call API to " + sNodeCode);
				
				// Ask to the node!!
				try {					
					HttpCallResponse oHttpCallResponse = ProcessWorkspaceAPIClient.getQueueStatus(oNode, sSessionId, sStatuses); 
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
					WasdiLog.errorLog("ProcessWorkspaceResource.getNodeQueuesStatus: Exception contacting the remote node: " + oNodeEx);
				}
			}


		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceResource.getNodeQueuesStatus: " + oEx);
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
			WasdiLog.warnLog("ProcessWorkspaceResource.getNodesSortedByScore: invalid session");
			return aoViewModels;
		}

		try {
			if (!UserApplicationRole.isAdmin(oUser)) {
				WasdiLog.debugLog("ProcessWorkspaceResource.getNodesSortedByScore: user not admin");
				return aoViewModels;
			}
			
			return Wasdi.getNodesSortedByScore(sSessionId, null);
			

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceResource.getNodesSortedByScore error: " + oEx);
		}

		return aoViewModels;
	}
	
	/**
	 * Converts a map summarising the statistics about the the computing times per subscription and project into the corresponding view model
	 * @param aoComputingTimes a map where: keys are the ids of the subscriptions and values are maps projectId-computingTime
	 * @return the computing time view model
	 */
	private List<ComputingTimeViewModel> buildComputingTimeViewModel(Map<String, Map<String, Long>> aoComputingTimes, String sUserId) {
		
		List<ComputingTimeViewModel> aoResultList = new ArrayList<>();
		
		for (String sSubscriptionId : aoComputingTimes.keySet()) {
			Map<String, Long> asProjectsMap = aoComputingTimes.get(sSubscriptionId);
			List<ComputingTimeViewModel> aoViewModels = asProjectsMap.entrySet().stream()
					.map(oEntry -> buildComputingTimeViewModel(sSubscriptionId, sUserId, oEntry.getKey(), oEntry.getValue()))
					.collect(Collectors.toList());
			aoResultList.addAll(aoViewModels);
		}
		return aoResultList;
	}
	
	/**
	 * Build the computing time view model for a given subscription, user and project
	 * @param sSubscriptionId the id or the subscription
	 * @param sUserId the id of the user 
	 * @param sProjectId the id of the project
	 * @param sComputingTime the computing time for the specified project and subscription ids
	 * @return the computing time view model the corresponding view model
	 */
	private ComputingTimeViewModel buildComputingTimeViewModel(String sSubscriptionId, String sUserId, String sProjectId, Long sComputingTime) {
		ComputingTimeViewModel oViewModel = new ComputingTimeViewModel();
		oViewModel.setSubscriptionId(sSubscriptionId);
		oViewModel.setProjectId(sProjectId);
		oViewModel.setUserId(sUserId);
		oViewModel.setComputingTime(sComputingTime);
		return oViewModel;
	}
	
	/**
	 * Updates the map containing the computing times by subscription and projects
	 * @param aoSubscriptionsComputingTimeMap the map that needs to be updated, where the keys are subscription ids and the values are maps <project-id, computing time>
	 * @param oComputingTime an object representing the computing time associated to a specific project and subscription id
	 */
	private void updateRunningTimesMap(Map<String, Map<String, Long>> aoSubscriptionsComputingTimeMap, ProcessWorkspaceAggregatorBySubscriptionAndProject oComputingTime) {
		String sSubscriptionId = oComputingTime.getSubscriptionId();
		String sProjectId = oComputingTime.getProjectId();
		Long lComputingTime = oComputingTime.getTotal();
		
		Map<String, Long> aoProjectsComputingTimeMap = aoSubscriptionsComputingTimeMap.getOrDefault(sSubscriptionId, new HashMap<String, Long>());
		Long lRunningTime = aoProjectsComputingTimeMap.getOrDefault(sProjectId, Long.valueOf(0)) + lComputingTime;
		aoProjectsComputingTimeMap.put(sProjectId, lRunningTime);
		aoSubscriptionsComputingTimeMap.put(sSubscriptionId, aoProjectsComputingTimeMap);
	}
	
	/**
	 * Updates the map containing the computing times by subscription and projects
	 * @param aoGeneralStatisticsMap the map that contains the overall count of the computing times and that needs to be updated. The keys are subscription ids and the values are maps <project-id, computing time> 
	 * @param aoPartialStatisticsMap the map that contains the computing times to be added in the map with the overall computing times. The keys are subscription ids and the values are maps <project-id, computing time> 
	 */
	private void updateRunningTimesMap(Map<String, Map<String, Long>> aoGeneralStatisticsMap, Map<String, Map<String, Long>> aoPartialStatisticsMap) {
		
		if (aoPartialStatisticsMap == null || aoPartialStatisticsMap.isEmpty()) {
			WasdiLog.debugLog("ProcessWorkspaceResource.updateComputingTimesMap: no computing times from node. ");
			return;
		}
		
		for (Map.Entry<String, Map<String, Long>> oEntrySubscription : aoPartialStatisticsMap.entrySet()) {
			String sSubscriptionId = oEntrySubscription.getKey();
			Map<String, Long> aoPartialRunningTimeByProject = oEntrySubscription.getValue();
			
			Map<String, Long> aoRunningTimeByProject = aoGeneralStatisticsMap.getOrDefault(sSubscriptionId, new HashMap<String, Long>());

			for (Map.Entry<String, Long> oEntryProject : aoPartialRunningTimeByProject.entrySet()) {
					String sProjectId = oEntryProject.getKey();
					Long lPartialRunningTime = oEntryProject.getValue();
					Long lGeneralRunningTime = aoRunningTimeByProject.getOrDefault(sProjectId, Long.valueOf(0));
					aoRunningTimeByProject.put(sProjectId, lGeneralRunningTime + lPartialRunningTime);
			}
			aoGeneralStatisticsMap.put(sSubscriptionId, aoRunningTimeByProject);
		}
			
	}
	
	/**
	 */
	private void updateRunningTimesMap(Map<String, Map<String, Long>> aoGeneralStatisticsMap, String sSbuscriptionId, String sProjectId, Long lComputingTime) {
		Map<String, Long> aoProjectStatisticsMap = null;	
		if (aoGeneralStatisticsMap.containsKey(sSbuscriptionId)) {
			aoProjectStatisticsMap = aoGeneralStatisticsMap.get(sSbuscriptionId);
			Long lGeneralRunningTime = aoProjectStatisticsMap.getOrDefault(sProjectId, Long.valueOf(0));
			aoProjectStatisticsMap.put(sProjectId, lGeneralRunningTime + lComputingTime);
		} else {
			aoProjectStatisticsMap = new HashMap<>();
			aoProjectStatisticsMap.put(sProjectId, lComputingTime);
			aoGeneralStatisticsMap.put(sSbuscriptionId, aoProjectStatisticsMap);
		}	
	}


}
