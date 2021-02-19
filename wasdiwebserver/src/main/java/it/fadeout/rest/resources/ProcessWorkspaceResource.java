package it.fadeout.rest.resources;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.type.TypeReference;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.KillProcessTreeParameter;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.AppStatsViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProcessHistoryViewModel;
import wasdi.shared.viewmodels.ProcessWorkspaceSummaryViewModel;
import wasdi.shared.viewmodels.ProcessWorkspaceViewModel;

@Path("/process")
public class ProcessWorkspaceResource {
	
	@Context
	ServletConfig m_oServletConfig;	

	
	@GET
	@Path("/byws")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public ArrayList<ProcessWorkspaceViewModel> getProcessByWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sWorkspaceId") String sWorkspaceId, @QueryParam("status") String sStatus,
			@QueryParam("operationType") String sOperationType,
			@QueryParam("namePattern") String sNamePattern,
			@QueryParam("dateFrom") String sDateFrom, @QueryParam("dateTo") String sDateTo,
			@QueryParam("startindex") Integer iStartIndex, @QueryParam("endindex") Integer iEndIndex) {
		
		Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace( WS: " + sWorkspaceId +
				", status: " + sStatus + ", name pattern: " + sNamePattern +
				", Start: " + iStartIndex + ", End: " + iEndIndex);

		

		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: workspace id is null, aborting");
				return aoProcessList;
			}
			
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: invalid session");
				return aoProcessList;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: user id from session is null, aborting");
				return aoProcessList;
			}
			
			if(!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
				Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: user " + oUser.getUserId() + " is not allowed to access workspace " + sWorkspaceId +", aborting" );
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
					Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: could not convert " + sStatus + " to a valid process status, ignoring it");
				}
			}
			
			LauncherOperations eLauncherOperation = null;
			if(!Utils.isNullOrEmpty(sOperationType)) {
				try {
					eLauncherOperation = LauncherOperations.valueOf(sOperationType);
				} catch (Exception oE) {
					Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: could not convert " + sOperationType + " to a valid operation type, ignoring it");
				}
			}
			
			Instant oDateFrom = null;
			if(!Utils.isNullOrEmpty(sDateFrom)) {
				try {
					oDateFrom = Instant.parse(sDateFrom);
				} catch (Exception oE) {
					Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: could not convert start date " + sDateFrom + " to a valid date, ignoring it");
				}
			}
			
			Instant oDateTo = null;
			if(!Utils.isNullOrEmpty(sDateTo)){
				try {
					oDateTo = Instant.parse(sDateTo);
				} catch (Exception oE) {
					Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: could not convert end date " + sDateFrom + " to a valid date, ignoring it");
				}
			}
			
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
				aoProcessList.add(buildProcessWorkspaceViewModel(oProcess));
			}

		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: " + oEx);
		}

		return aoProcessList;
	}

	
	@GET
	@Path("/byusr")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<ProcessWorkspaceViewModel> getProcessByUser(@HeaderParam("x-session-token") String sSessionId) {
		
		Utils.debugLog("ProcessWorkspaceResource.GetProcessByUser()");

		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();
			

		try {
			// Domain Check
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if(null == oUser) {
				Utils.debugLog("ProcessWorkspaceResource.GetProcessByUser: invalid session");
				return aoProcessList;
			}
			
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return aoProcessList;
			}

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			List<ProcessWorkspace> aoProcess = oRepository.getProcessByUser(oUser.getUserId());

			// For each
			for (int iProcess=0; iProcess<aoProcess.size(); iProcess++) {
				// Create View Model
				ProcessWorkspace oProcess = aoProcess.get(iProcess);
				aoProcessList.add(buildProcessWorkspaceViewModel(oProcess));
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.GetProcessByUser: error retrieving process " + oEx);
		}

		return aoProcessList;
	}
	
	@GET
	@Path("/byapp")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<ProcessHistoryViewModel> getProcessByApplication(
			@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processorName") String sProcessorName) {
		Utils.debugLog("ProcessWorkspaceResource.getProcessByApplication " + sProcessorName);
		ArrayList<ProcessHistoryViewModel> aoProcessList = new ArrayList<ProcessHistoryViewModel>();
		try {			
			// Domain Check
			if(Utils.isNullOrEmpty(sProcessorName)) {
				Utils.debugLog("ProcessWorkspaceResource.getProcessByApplication: invalid processor name, aborting");
				return aoProcessList;
			}
			
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if(null == oUser) {
				Utils.debugLog("ProcessWorkspaceResource.getProcessByApplication: invalid session, aborting");
				return aoProcessList;
			}
			
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("ProcessWorkspaceResource.getProcessByApplication session invalid aborting");
				return aoProcessList;
			}
			
			// checks that processor is in db -> needed to avoid url injection from users 
			ProcessorRepository oProcessRepository = new ProcessorRepository();
			if (null == oProcessRepository.getProcessorByName(sProcessorName) ) {
				Utils.debugLog("ProcessWorkspaceResource.getProcessByApplication( Processor name: " + sProcessorName+ " ): Processor name not found in DB, aborting");
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
					oRun.setOperationDate(oProcess.getOperationDate() + " " + Utils.getLocalDateOffsetFromUTCForJS());
					
					// Set the start date: beeing introduced later, for compatibility, if not present use the Operation Date
					if (!Utils.isNullOrEmpty(oProcess.getOperationStartDate())) {
						oRun.setOperationStartDate(oProcess.getOperationStartDate() + Utils.getLocalDateOffsetFromUTCForJS());
					}
					else {
						oRun.setOperationStartDate(oProcess.getOperationDate() + " " + Utils.getLocalDateOffsetFromUTCForJS());
					}
					
					
					oRun.setOperationEndDate(oProcess.getOperationEndDate() + " " + Utils.getLocalDateOffsetFromUTCForJS());
					
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
					Utils.debugLog("ProcessWorkspaceResource.getProcessByApplication: exception generating History View Model " + oEx.toString());
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
						
						Utils.debugLog("ProcessWorkspaceResource.getProcessByApplication: calling url: " + sUrl);
						
						
						String sResponse = Wasdi.httpGet(sUrl, asHeaders);
						
						if (Utils.isNullOrEmpty(sResponse)==false) {
							ArrayList<ProcessHistoryViewModel> aoNodeHistory = MongoRepository.s_oMapper.readValue(sResponse, new TypeReference<ArrayList<ProcessHistoryViewModel>>(){});
							if (aoNodeHistory != null) {
								aoProcessList.addAll(aoNodeHistory);
							}
						}
						
					}
					catch (Exception e) {
						Utils.debugLog("ProcessWorkspaceResource.getProcessByApplication: exception contacting computing node: " + e.toString());
					}
				}
				
			}
			
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.getProcessByApplication: error retrieving process " + oEx);
		}

		return aoProcessList;
	}
	
	
	/**
	 * Get Application statistic
	 * @param sSessionId User Session 
	 * @param sProcessorName Application name
	 * @return AppStatsViewModel with the app statistics
	 */
	@GET
	@Path("/appstats")
	@Produces({"application/xml", "application/json", "text/xml"})
	public AppStatsViewModel getApplicationStatistics(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorName") String sProcessorName) {
		
		Utils.debugLog("ProcessWorkspaceResource.getProcessByApplication( Session: " + sSessionId + " )");
		
		AppStatsViewModel oReturnStats = new AppStatsViewModel();
		oReturnStats.setApplicationName(sProcessorName);
		
		try {			
			// Domain Check
			if(Utils.isNullOrEmpty(sProcessorName)) {
				Utils.debugLog("ProcessWorkspaceResource.getApplicationStatistics( " + sSessionId + ", " + sProcessorName + " ): invalid processor name, aborting");
				return oReturnStats;
			}
			
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if(null == oUser) {
				Utils.debugLog("ProcessWorkspaceResource.getApplicationStatistics( Session: " + sSessionId + " ): invalid session, aborting");
				return oReturnStats;
			}
			
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("ProcessWorkspaceResource.getApplicationStatistics( Session: " + sSessionId + " ): is valid, but userId is not (" + oUser.getUserId() + "), aborting");
				return oReturnStats;
			}
			
			// checks that processor is in db -> needed to avoid url injection from users 
			ProcessorRepository oProcessRepository = new ProcessorRepository();
			if (null == oProcessRepository.getProcessorByName(sProcessorName) ) {
				Utils.debugLog("ProcessWorkspaceResource.getApplicationStatistics( Processor name: " + sProcessorName+ " ): Processor name not found in DB, aborting");
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
					Utils.debugLog("ProcessWorkspaceResource.getApplicationStatistics: exception generating History View Model " + oEx.toString());
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
						
						Utils.debugLog("ProcessWorkspaceResource.getApplicationStatistics: calling url: " + sUrl);
						
						
						String sResponse = Wasdi.httpGet(sUrl, asHeaders);
						
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
						Utils.debugLog("ProcessWorkspaceResource.getApplicationStatistics: exception contacting computing node: " + e.toString());
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
			Utils.debugLog("ProcessWorkspaceResource.getApplicationStatistics: error retrieving process " + oEx);
		}

		return oReturnStats;
	}
	


	private ProcessWorkspaceViewModel buildProcessWorkspaceViewModel(ProcessWorkspace oProcess) {
		//Utils.debugLog("ProcessWorkspaceResource.buildProcessWorkspaceViewModel");
		ProcessWorkspaceViewModel oViewModel = new ProcessWorkspaceViewModel();
		try {
			// Set the start date: beeing introduced later, for compatibility, if not present use the Operation Date
			if (!Utils.isNullOrEmpty(oProcess.getOperationStartDate())) {
				oViewModel.setOperationStartDate(oProcess.getOperationStartDate() + Utils.getLocalDateOffsetFromUTCForJS());
			}
			else {
				oViewModel.setOperationStartDate(oProcess.getOperationDate() + " " + Utils.getLocalDateOffsetFromUTCForJS());
			}
			
			if (!Utils.isNullOrEmpty(oProcess.getLastStateChangeDate())) {
				oViewModel.setLastChangeDate(oProcess.getLastStateChangeDate() + " " + Utils.getLocalDateOffsetFromUTCForJS());
			}
			
			oViewModel.setOperationDate(oProcess.getOperationDate() + " " + Utils.getLocalDateOffsetFromUTCForJS());
			oViewModel.setOperationEndDate(oProcess.getOperationEndDate() + " " + Utils.getLocalDateOffsetFromUTCForJS());
			oViewModel.setOperationType(oProcess.getOperationType());
			if (!Utils.isNullOrEmpty(oProcess.getOperationSubType())) {
				oViewModel.setOperationSubType(oProcess.getOperationSubType());
			}
			else {
				oViewModel.setOperationSubType("");
			}
			
			oViewModel.setProductName(oProcess.getProductName());
			oViewModel.setUserId(oProcess.getUserId());
			oViewModel.setFileSize(oProcess.getFileSize());
			oViewModel.setPid(oProcess.getPid());
			oViewModel.setStatus(oProcess.getStatus());
			oViewModel.setProgressPerc(oProcess.getProgressPerc());
			oViewModel.setProcessObjId(oProcess.getProcessObjId());
			oViewModel.setPayload(oProcess.getPayload());
		} catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.buildProcessWorkspaceViewModel: " + oEx);
		}
		return oViewModel;
	}
	
	
	@GET
	@Path("/lastbyws")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<ProcessWorkspaceViewModel> getLastProcessByWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) {
		
		Utils.debugLog("ProcessWorkspaceRepository.getLastProcessByWorkspace( WS: " + sWorkspaceId + " )");
		User oUser = Wasdi.getUserFromSession(sSessionId);

		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();

		try {
			// Domain Check
			if (oUser == null) {
				Utils.debugLog("ProcessWorkspaceRepository.getLastProcessByWorkspace( WS: " + sWorkspaceId + " ): invalid session");
				return aoProcessList;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: user id is null");
				return aoProcessList;
			}
			
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: ws id is null");
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
				aoProcessList.add(buildProcessWorkspaceViewModel(oProcess));
			}

		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: " + oEx);
		}

		return aoProcessList;
	}
	
	@GET
	@Path("/lastbyusr")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<ProcessWorkspaceViewModel> getLastProcessByUser(@HeaderParam("x-session-token") String sSessionId) {
		
		Utils.debugLog("ProcessWorkspaceResource.GetLastProcessByUser()");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();

		try {
			// Domain Check
			if (oUser == null) {
				Utils.debugLog("ProcessWorkspaceResource.GetLastProcessByUser(): invalid session");
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
				aoProcessList.add(buildProcessWorkspaceViewModel(oProcess));
			}

		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.GetLastProcessByUser: " + oEx);
		}

		return aoProcessList;
	}
	
	@GET
	@Path("/summary")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceSummaryViewModel getSummary(@HeaderParam("x-session-token") String sSessionId) {
		
		Utils.debugLog("ProcessWorkspaceResource.GetSummary()");
		
		User oUser = Wasdi.getUserFromSession(sSessionId);
		ProcessWorkspaceSummaryViewModel oSummaryViewModel = new ProcessWorkspaceSummaryViewModel();

		try {
			// Domain Check
			if (oUser == null) {
				Utils.debugLog("ProcessWorkspaceResource.GetSummary: invalid session" + sSessionId);
				return oSummaryViewModel;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("ProcessWorkspaceResource.GetSummary: invalid session" + sSessionId);
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

/*			
			// Get Download Waiting Process List
			List<ProcessWorkspace> aoQueuedDownloads = oRepository.getCreatedDownloads();

			oSummaryViewModel.setAllDownloadWaiting(aoQueuedDownloads.size());
			
			int iUserDownloadWaiting = 0 ;
			// Count the user's ones
			for (int iProcess=0; iProcess<aoQueuedDownloads.size(); iProcess++) {
				// Create View Model
				ProcessWorkspace oProcess = aoQueuedDownloads.get(iProcess);
				if (oProcess.getUserId().equals(oUser.getUserId())) iUserDownloadWaiting ++;
			}
			
			oSummaryViewModel.setUserDownloadWaiting(iUserDownloadWaiting);
			
			
			// Get Processing Waiting Process List
			List<ProcessWorkspace> aoQueuedProcessing = oRepository.getCreatedProcesses();

			oSummaryViewModel.setAllProcessWaiting(aoQueuedProcessing.size());
			
			int iUserProcessWaiting = 0 ;
			// Count the user's ones
			for (int iProcess=0; iProcess<aoQueuedProcessing.size(); iProcess++) {
				// Create View Model
				ProcessWorkspace oProcess = aoQueuedProcessing.get(iProcess);
				if (oProcess.getUserId().equals(oUser.getUserId())) iUserProcessWaiting ++;
			}
			
			oSummaryViewModel.setUserProcessWaiting(iUserProcessWaiting);
			
			// Get IDL Waiting Process List
			List<ProcessWorkspace> aoQueuedIDL= oRepository.getCreatedIDL();

			oSummaryViewModel.setAllIDLWaiting(aoQueuedIDL.size());
			
			int iUserIDLWaiting = 0 ;
			// Count the user's ones
			for (int iProcess=0; iProcess<aoQueuedIDL.size(); iProcess++) {
				// Create View Model
				ProcessWorkspace oProcess = aoQueuedIDL.get(iProcess);
				if (oProcess.getUserId().equals(oUser.getUserId())) iUserIDLWaiting ++;
			}
			
			oSummaryViewModel.setUserIDLWaiting(iUserIDLWaiting);
			
			// Get Processing Running  List
			List<ProcessWorkspace> aoRunningProcessing = oRepository.getRunningProcesses();

			oSummaryViewModel.setAllProcessRunning(aoRunningProcessing.size());
			
			int iUserProcessRunning= 0 ;
			// Count the user's ones
			for (int iProcess=0; iProcess<aoRunningProcessing.size(); iProcess++) {
				// Create View Model
				ProcessWorkspace oProcess = aoRunningProcessing.get(iProcess);
				if (oProcess.getUserId().equals(oUser.getUserId())) iUserProcessRunning ++;
			}
			
			oSummaryViewModel.setUserProcessRunning(iUserProcessRunning);
			
			// Get Download Running Process List
			List<ProcessWorkspace> aoRunningDownload= oRepository.getRunningDownloads();

			oSummaryViewModel.setAllDownloadRunning(aoRunningDownload.size());
			
			int iUserDownloadRunning= 0 ;
			// Count the user's ones
			for (int iProcess=0; iProcess<aoRunningDownload.size(); iProcess++) {
				// Create View Model
				ProcessWorkspace oProcess = aoRunningDownload.get(iProcess);
				if (oProcess.getUserId().equals(oUser.getUserId())) iUserDownloadRunning ++;
			}
			
			oSummaryViewModel.setUserDownloadRunning(iUserDownloadRunning);

			// Get IDL Running  List
			List<ProcessWorkspace> aoRunningIDL= oRepository.getRunningIDL();

			oSummaryViewModel.setAllIDLRunning(aoRunningIDL.size());
			
			int iUserIDLRunning= 0 ;
			// Count the user's ones
			for (int iProcess=0; iProcess<aoRunningIDL.size(); iProcess++) {
				// Create View Model
				ProcessWorkspace oProcess = aoRunningIDL.get(iProcess);
				if (oProcess.getUserId().equals(oUser.getUserId())) iUserIDLRunning ++;
			}
			
			oSummaryViewModel.setUserIDLRunning(iUserIDLRunning);
*/			
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.GetSummary: " + oEx);
		}

		return oSummaryViewModel;
	}
	
	@GET
	@Path("/delete")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response deleteProcess(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProcessObjId") String sToKillProcessObjId, @QueryParam("treeKill") Boolean bKillTheEntireTree) {
		
		Utils.debugLog("ProcessWorkspaceResource.DeleteProcess( Process: " + sToKillProcessObjId + ", treeKill: " + bKillTheEntireTree + " )");

		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);
			// Domain Check
			if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("ProcessWorkspaceResource.DeleteProcess( Process: " + sToKillProcessObjId + ", treeKill: " + bKillTheEntireTree + " ): invalid session");
				return Response.status(401).build();
			}
			
			if(Utils.isNullOrEmpty(sToKillProcessObjId)) {
				Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: processObjId is null or empty, aborting");
				return Response.status(401).build();
			}
			
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessToDelete = oRepository.getProcessByProcessObjId(sToKillProcessObjId);
			
			//check that the process exists
			if(null==oProcessToDelete) {
				Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: process not found in DB, aborting");
				return Response.status(401).build();
			}
			
			// check that the user can access the processworkspace
			if(!PermissionsUtils.canUserAccessProcess(oUser.getUserId(), sToKillProcessObjId)) {
				Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: user cannot access requested process workspace");
				return Response.status(403).build();
			}
			
			
			//create the operation parameter
			String sDeleteObjId = Utils.GetRandomName();
			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			KillProcessTreeParameter oKillProcessParameter = new KillProcessTreeParameter();
			oKillProcessParameter.setProcessObjId(sDeleteObjId);
			oKillProcessParameter.setProcessToBeKilledObjId(sToKillProcessObjId);
			if(null!=bKillTheEntireTree) {
				oKillProcessParameter.setKillTree(bKillTheEntireTree);
			}
			
			
			String sWorkspaceId = oProcessToDelete.getWorkspaceId();
			
			//base parameter atttributes
			oKillProcessParameter.setWorkspace(sWorkspaceId);
			oKillProcessParameter.setUserId(oUser.getUserId());
			oKillProcessParameter.setExchange(sWorkspaceId);
			oKillProcessParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			
			//schedule the deletion
			PrimitiveResult oResult = Wasdi.runProcess(oUser.getUserId(), sSessionId, LauncherOperations.KILLPROCESSTREE.name(), oProcessToDelete.getProductName(), sPath, oKillProcessParameter, null);
			Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: kill scheduled with result: " + oResult.getBoolValue() + ", " + oResult.getIntValue() + ", " + oResult.getStringValue());
			
			return Response.status(oResult.getIntValue()).build();
						
		}
		catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.DeleteProcess: " + oEx);
		}

		return Response.status(500).build();
	}

	
	
	@GET
	@Path("/byid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceViewModel getProcessById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProcessId") String sProcessWorkspaceId) {
		
		Utils.debugLog("ProcessWorkspaceResource.GetProcessById( ProcWsId: " + sProcessWorkspaceId + " )");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();
		
		// Initialize status as ERROR
		oProcess.setStatus("ERROR");

		try {
			// Domain Check
			if (oUser == null) {
				Utils.debugLog("ProcessWorkspaceResource.GetProcessById( x-session-token: " + sSessionId + ", sProcessId: " + sProcessWorkspaceId + " ): invalid session");
				return oProcess;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return oProcess;
			}
			
			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			ProcessWorkspace oProcessWorkspace = oRepository.getProcessByProcessObjId(sProcessWorkspaceId);
			oProcess = buildProcessWorkspaceViewModel(oProcessWorkspace);

		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.GetProcessById: " + oEx);
		}

		return oProcess;
	}
	
	
	@POST
	@Path("/statusbyid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<String> getStatusProcessesById(@HeaderParam("x-session-token") String sSessionId, ArrayList<String> asProcessesWorkspaceId) {
		
		Utils.debugLog("ProcessWorkspaceResource.getStatusProcessesById");

		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		ArrayList<String> asReturnStatusList = new ArrayList<String>();

		try {
			// Domain Check
			if (oUser == null) {
				Utils.debugLog("ProcessWorkspaceResource.getStatusProcessesById: invalid session");
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
			Utils.debugLog("ProcessWorkspaceResource.getStatusProcessesById: " + oEx);
		}

		return asReturnStatusList;
	}
	
	@GET
	@Path("/getstatusbyid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public String getProcessStatusById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processObjId") String sProcessObjId) {
		Utils.debugLog("ProcessWorkspaceResource.getProcessStatusById(" + sProcessObjId + " )" );
		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);
			
			if(null == oUser) {
				Utils.debugLog("ProcessWorkspaceResource.getProcessStatusById: invalid session" );
				return null;
			}
			
			if(PermissionsUtils.canUserAccessProcess(oUser.getUserId(), sProcessObjId)) {
				ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
				return oProcessWorkspaceRepository.getProcessStatusFromId(sProcessObjId);
			}
		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceResource.getProcessStatusById: " + oE );
		}
		return null;
	}
	
	@GET
	@Path("/updatebyid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceViewModel updateProcessById(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sProcessId") String sProcessWorkspaceId, @QueryParam("status") String sNewStatus,
			@QueryParam("perc") int iPerc, @QueryParam("sendrabbit") String sSendToRabbit) {
		
		Utils.debugLog("ProcessWorkspaceResource.UpdateProcessById( ProcWsId: " + sProcessWorkspaceId + ", Status: " + sNewStatus + ", Perc: " + iPerc + ", SendRabbit:" + sSendToRabbit + " )" );

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();

		try {
			// Domain Check
			if (oUser == null) {
				Utils.debugLog("ProcessWorkspaceResource.UpdateProcessById: invalid session: " + sSessionId );
				return oProcess;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return oProcess;
			}

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			ProcessWorkspace oProcessWorkspace = oRepository.getProcessByProcessObjId(sProcessWorkspaceId);
			
			if (  (oProcessWorkspace.getStatus().equals(ProcessStatus.CREATED.name()) || oProcessWorkspace.getStatus().equals(ProcessStatus.RUNNING.name()) ) && (sNewStatus.equals(ProcessStatus.DONE.name()) || sNewStatus.equals(ProcessStatus.ERROR.name()) || sNewStatus.equals(ProcessStatus.STOPPED.name()) ) ) {
				// The process finished
				if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndDate())) {
					// No end-date set: put it here
					oProcessWorkspace.setOperationEndDate(Utils.getFormatDate(new Date()));
				}
			}
			
			oProcessWorkspace.setStatus(sNewStatus);
			
			if (iPerc>=0 && iPerc<=100) {
				oProcessWorkspace.setProgressPerc(iPerc);
			}
			

			oRepository.updateProcess(oProcessWorkspace);
			
			oProcess = buildProcessWorkspaceViewModel(oProcessWorkspace);

			// Check if we need to send the asynch rabbit message
			if (Utils.isNullOrEmpty(sSendToRabbit) == false) {
				// The param exists
				if (sSendToRabbit.equals("1") || sSendToRabbit.equals("true")) {
					
					// Search for exchange name
					String sExchange = m_oServletConfig.getInitParameter("RABBIT_EXCHANGE");
					
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
			Utils.debugLog("ProcessWorkspaceResource.UpdateProcessById: " + oEx);
		}

		return oProcess;
	}
	
	
	@GET
	@Path("/setpayload")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceViewModel setProcessPayload(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sProcessId") String sProcessWorkspaceId, @QueryParam("payload") String sPayload) {
		
		Utils.debugLog("ProcessWorkspaceResource.SetProcessPayload( ProcWsId: " + sProcessWorkspaceId +
				", Payload: " + sPayload + " )" );

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();

		try {
			// Domain Check
			if (oUser == null) {
				Utils.debugLog("ProcessWorkspaceResource.SetProcessPayload: invalid session" );
				return oProcess;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return oProcess;
			}

			Utils.debugLog("ProcessWorkspaceResource.SetProcessPayload: process id " + sProcessWorkspaceId);
			Utils.debugLog("ProcessWorkspaceResource.SetProcessPayload: PAYLOAD " + sPayload);

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			ProcessWorkspace oProcessWorkspace = oRepository.getProcessByProcessObjId(sProcessWorkspaceId);
			
			oProcessWorkspace.setPayload(sPayload);

			oRepository.updateProcess(oProcessWorkspace);
			
			oProcess = buildProcessWorkspaceViewModel(oProcessWorkspace);

		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.SetProcessPayload: " + oEx);
		}

		return oProcess;
	}
	
	
	@GET
	@Path("/setsubpid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceViewModel setSubProcessPid(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProcessId") String sProcessWorkspaceId, @QueryParam("subpid") int iSubPid) {
		
		Utils.debugLog("ProcessWorkspaceResource.setSubProcessPid( ProcWsId: " + sProcessWorkspaceId +", SubPid: " + iSubPid + " )" );

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();

		try {
			// Domain Check
			if (oUser == null) {
				Utils.debugLog("ProcessWorkspaceResource.setSubProcessPid( ProcWsId: " + sProcessWorkspaceId +", Payload: " + iSubPid + " ): invalid session" );
				return oProcess;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return oProcess;
			}
			
			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			ProcessWorkspace oProcessWorkspace = oRepository.getProcessByProcessObjId(sProcessWorkspaceId);
			
			if (oProcessWorkspace == null) {
				return oProcess;
			}
			
			oProcessWorkspace.setSubprocessPid(iSubPid);

			oRepository.updateProcess(oProcessWorkspace);
			
			oProcess = buildProcessWorkspaceViewModel(oProcessWorkspace);

		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.setSubProcessPid: " + oEx);
			oEx.printStackTrace();
		}

		return oProcess;
	}
	
	@GET
	@Path("/cleanqueue")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult cleanQueue(@HeaderParam("x-session-token") String sSessionId) {
		
		Utils.debugLog("ProcessWorkspaceResource.CleanQueue()");
		
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		try {
			// Domain Check
			if (oUser == null) {
				Utils.debugLog("ProcessWorkspaceResource.CleanQueue: invalid session");
				return oResult;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return oResult;
			}
			
			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			oRepository.cleanQueue();
			
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.CleanQueue: " + oEx);
		}
		
		return oResult;
	}

	@GET
	@Path("/payload")
	@Produces({"application/xml", "application/json", "text/xml"})
	public String getPayload(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processObjId") String sProcessObjId) {
		Utils.debugLog("ProcessWorkspaceResource.getPayload(" + sProcessObjId + " )" );
		try {
			if(Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessWorkspaceResource.getPayload: session is null or empty, aborting");
				return null;
			}
			User oUser = Wasdi.getUserFromSession(sSessionId);
			if(null == oUser) {
				Utils.debugLog("ProcessWorkspaceResource.getPayload: invalid session" );
				return null;
			}
			if(PermissionsUtils.canUserAccessProcess(oUser.getUserId(), sProcessObjId)) {
				ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
				return oProcessWorkspaceRepository.getPayload(sProcessObjId);
			} else {
				Utils.debugLog("ProcessWorkspaceResource.getPayload: user " + oUser.getUserId() + " cannot access process obj id " + sProcessObjId );
			}
		}catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceResource.getPayload: " + oE );
		}
		
		return null;
	}
}
