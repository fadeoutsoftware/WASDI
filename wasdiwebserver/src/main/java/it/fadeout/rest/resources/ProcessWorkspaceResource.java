package it.fadeout.rest.resources;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.parameters.KillProcessTreeParameter;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;
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
		
		Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace( Session: " + sSessionId + ", WS: " + sWorkspaceId +
				", status: " + sStatus + ", name pattern: " + sNamePattern +
				", Start: " + iStartIndex + ", End: " + iEndIndex);

		

		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: workspace id is null, aborting");
				return aoProcessList;
			}
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			if (oUser == null) {
				Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: user from session is null, aborting");
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
		
		Utils.debugLog("ProcessWorkspaceResource.GetProcessByUser( Session: " + sSessionId + " )");

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();

		try {
			// Domain Check
			if (oUser == null) {
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


	private ProcessWorkspaceViewModel buildProcessWorkspaceViewModel(ProcessWorkspace oProcess) {
		//Utils.debugLog("ProcessWorkspaceResource.buildProcessWorkspaceViewModel");
		ProcessWorkspaceViewModel oViewModel = new ProcessWorkspaceViewModel();
		try {
			// Set the start date: beeing introduced later, for compatibility, if not present use the Operation Date
			if (!Utils.isNullOrEmpty(oProcess.getOperationStartDate())) {
				//oViewModel.setOperationStartDate(oProcess.getOperationStartDate() + " " + TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT));
				oViewModel.setOperationStartDate(oProcess.getOperationStartDate() + Utils.getLocalDateOffsetFromUTCForJS());
			}
			else {
				//oViewModel.setOperationStartDate(oProcess.getOperationDate() + " " + TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT));
				oViewModel.setOperationStartDate(oProcess.getOperationDate() + " " + Utils.getLocalDateOffsetFromUTCForJS());
			}
			
			if (!Utils.isNullOrEmpty(oProcess.getLastStateChangeDate())) {
				//oViewModel.setLastChangeDate(oProcess.getLastStateChangeDate() + " " + TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT));
				oViewModel.setLastChangeDate(oProcess.getLastStateChangeDate() + " " + Utils.getLocalDateOffsetFromUTCForJS());
			}
			
			//oViewModel.setOperationDate(oProcess.getOperationDate() + " " + TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT));
			oViewModel.setOperationDate(oProcess.getOperationDate() + " " + Utils.getLocalDateOffsetFromUTCForJS());
			//oViewModel.setOperationEndDate(oProcess.getOperationEndDate() + " " + TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT));
			oViewModel.setOperationEndDate(oProcess.getOperationEndDate() + " " + Utils.getLocalDateOffsetFromUTCForJS());
			oViewModel.setOperationType(oProcess.getOperationType());
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
		
		Utils.debugLog("ProcessWorkspaceRepository.getLastProcessByWorkspace( Session: " + sSessionId + ", WS: " + sWorkspaceId + " )");
		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();

		try {
			// Domain Check
			if (oUser == null) {
				Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace: user not found from session");
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
		
		Utils.debugLog("ProcessWorkspaceResource.GetLastProcessByUser( Session: " + sSessionId + " )");

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ArrayList<ProcessWorkspaceViewModel> aoProcessList = new ArrayList<ProcessWorkspaceViewModel>();

		try {
			// Domain Check
			if (oUser == null) {
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
		
		Utils.debugLog("ProcessWorkspaceResource.GetSummary( Session: " + sSessionId + " )");
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		ProcessWorkspaceSummaryViewModel oSummaryViewModel = new ProcessWorkspaceSummaryViewModel();

		try {
			// Domain Check
			if (oUser == null) {
				return oSummaryViewModel;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
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
	public Response deleteProcess(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProcessObjId") String sProcessObjId, @QueryParam("treeKill") Boolean bKillTheEntireTree) {
		
		Utils.debugLog("ProcessWorkspaceResource.DeleteProcess( Session: " + sSessionId + ", Process: " + sProcessObjId + ", treeKill: " + bKillTheEntireTree + " )");

		try {
			User oUser = Wasdi.GetUserFromSession(sSessionId);
			// Domain Check
			if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: user (or userId) null, aborting");
				return Response.status(401).build();
			}
			
			if(Utils.isNullOrEmpty(sProcessObjId)) {
				Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: processObjId is null or empty, aborting");
				return Response.status(401).build();
			}
			
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessToDelete = oRepository.getProcessByProcessObjId(sProcessObjId);
			
			//check that the process exists
			if(null==oProcessToDelete) {
				Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: process not found in DB, aborting");
				return Response.status(401).build();
			}
			
			// check that the user can access the processworkspace
			if(!PermissionsUtils.canUserAccessProcess(oUser.getUserId(), sProcessObjId)) {
				Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: user cannot access requested process workspace");
				return Response.status(403).build();
			}
			
			
			//create the operation parameter
			String sDeleteObjId = Utils.GetRandomName();
			String sPath = m_oServletConfig.getInitParameter("SerializationPath");
			KillProcessTreeParameter oProcessorParameter = new KillProcessTreeParameter();
			oProcessorParameter.setProcessObjId(sProcessObjId);
			if(null!=bKillTheEntireTree) {
				oProcessorParameter.setKillTree(bKillTheEntireTree);
			}
			
			
			String sWorkspaceId = oProcessToDelete.getWorkspaceId();
			
			//base parameter atttributes
			oProcessorParameter.setWorkspace(sWorkspaceId);
			oProcessorParameter.setUserId(oUser.getUserId());
			oProcessorParameter.setExchange(sWorkspaceId);
			oProcessorParameter.setProcessObjId(sProcessObjId);
			oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));
			
			//schedule the deletion
			PrimitiveResult oResult = Wasdi.runProcess(oUser.getUserId(), sSessionId, sDeleteObjId, LauncherOperations.KILLPROCESSTREE.name(), sPath, oProcessorParameter, oProcessToDelete.getParentId());
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
		
		Utils.debugLog("ProcessWorkspaceResource.GetProcessById( Session: " + sSessionId + ", ProcWsId: " + sProcessWorkspaceId + " )");

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();
		
		// Initialize status as ERROR
		oProcess.setStatus("ERROR");

		try {
			// Domain Check
			if (oUser == null) {
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
		
		Utils.debugLog("ProcessWorkspaceResource.getStatusProcessesById( Session: " + sSessionId + " )");

		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		ArrayList<String> asReturnStatusList = new ArrayList<String>();

		try {
			// Domain Check
			if (oUser == null) {
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
		Utils.debugLog("ProcessWorkspaceResource.getProcessStatusById( " + sSessionId + ", " + sProcessObjId + " )" );
		try {
			User oUser = Wasdi.GetUserFromSession(sSessionId);
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
		
		Utils.debugLog("ProcessWorkspaceResource.UpdateProcessById( Session: " + sSessionId + ", ProcWsId: " +
				sProcessWorkspaceId + ", Status: " + sNewStatus + ", Perc: " + iPerc + ", SendRabbit:" + sSendToRabbit + " )" );

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();

		try {
			// Domain Check
			if (oUser == null) {
				return oProcess;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return oProcess;
			}

			Utils.debugLog("ProcessWorkspaceResource.UpdateProcessById: process id " + sProcessWorkspaceId);

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			ProcessWorkspace oProcessWorkspace = oRepository.getProcessByProcessObjId(sProcessWorkspaceId);
			
			if (  (oProcessWorkspace.getStatus().equals(ProcessStatus.CREATED.name()) || oProcessWorkspace.getStatus().equals(ProcessStatus.RUNNING.name()) ) && (sNewStatus.equals(ProcessStatus.DONE.name()) || sNewStatus.equals(ProcessStatus.ERROR.name()) || sNewStatus.equals(ProcessStatus.STOPPED.name()) ) ) {
				// The process finished
				if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndDate())) {
					// No end-date set: put it here
					oProcessWorkspace.setOperationEndDate(Utils.GetFormatDate(new Date()));
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
		
		Utils.debugLog("ProcessWorkspaceResource.SetProcessPayload( Session: " + sSessionId + ", ProcWsId: " + sProcessWorkspaceId +
				", Payload: " + sPayload + " )" );

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();

		try {
			// Domain Check
			if (oUser == null) {
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
		
		Utils.debugLog("ProcessWorkspaceResource.setSubProcessPid( Session: " + sSessionId + ", ProcWsId: " + sProcessWorkspaceId +", Payload: " + iSubPid + " )" );

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ProcessWorkspaceViewModel oProcess = new ProcessWorkspaceViewModel();

		try {
			// Domain Check
			if (oUser == null) {
				return oProcess;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return oProcess;
			}

			Utils.debugLog("ProcessWorkspaceResource.setSubProcessPid: process id " + sProcessWorkspaceId);
			Utils.debugLog("ProcessWorkspaceResource.setSubProcessPid: SubPid " + iSubPid);

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
		
		Utils.debugLog("ProcessWorkspaceResource.CleanQueue( Session: " + sSessionId + " )");
		
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		try {
			// Domain Check
			if (oUser == null) {
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
		Utils.debugLog("ProcessWorkspaceResource.getPayload( " + sSessionId + ", " + sProcessObjId + " )" );
		try {
			if(Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("ProcessWorkspaceResource.getPayload: session is null or empty, aborting");
				return null;
			}
			User oUser = Wasdi.GetUserFromSession(sSessionId);
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
