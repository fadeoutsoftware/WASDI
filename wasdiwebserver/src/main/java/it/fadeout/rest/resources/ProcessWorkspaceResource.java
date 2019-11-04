package it.fadeout.rest.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import it.fadeout.Wasdi;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.rabbit.Send;
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
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<ProcessWorkspaceViewModel> getProcessByWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sWorkspaceId") String sWorkspaceId, @QueryParam("startindex") Integer iStartIndex, @QueryParam("endindex") Integer iEndIndex) {
		Utils.debugLog("ProcessWorkspaceResource.GetProcessByWorkspace( " + sSessionId + ", " + sWorkspaceId + ", " +
				iStartIndex + ", " + iEndIndex);

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
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				return aoProcessList;
			}

//			Wasdi.DebugLog("ProcessWorkspaceResource.GetProcessByWorkspace: process for ws " + sWorkspaceId);

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			List<ProcessWorkspace> aoProcess = null;
			
			if (iStartIndex != null && iEndIndex != null) {
				aoProcess = oRepository.GetProcessByWorkspace(sWorkspaceId, iStartIndex, iEndIndex);
			}
			else {
				aoProcess = oRepository.GetProcessByWorkspace(sWorkspaceId);
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
		
		Utils.debugLog("ProcessWorkspaceResource.GetProcessByUser( " + sSessionId + " )");

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

			//Wasdi.DebugLog("ProcessWorkspaceResource.GetProcessByUser: process for User " + oUser.getUserId());

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			List<ProcessWorkspace> aoProcess = oRepository.GetProcessByUser(oUser.getUserId());

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
				oViewModel.setOperationStartDate(oProcess.getOperationStartDate() + " " + TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT));
			}
			else {
				oViewModel.setOperationStartDate(oProcess.getOperationDate() + " " + TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT));
			}
			
			oViewModel.setOperationDate(oProcess.getOperationDate() + " " + TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT));
			oViewModel.setOperationEndDate(oProcess.getOperationEndDate() + " " + TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT));
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
	public ArrayList<ProcessWorkspaceViewModel> getLastProcessByWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sWorkspaceId") String sWorkspaceId) {
		Utils.debugLog("ProcessWorkspaceRepository.getLastProcessByWorkspace( " + sSessionId + ", " + sWorkspaceId + " )");
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


			//Wasdi.DebugLog("ProcessWorkspaceResource.GetProcessByWorkspace: process for ws " + sWorkspaceId);

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			List<ProcessWorkspace> aoProcess = oRepository.GetLastProcessByWorkspace(sWorkspaceId);

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
		
		Utils.debugLog("ProcessWorkspaceResource.GetLastProcessByUser( " + sSessionId + " )");

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

			//Wasdi.DebugLog("ProcessWorkspaceResource.GetLastProcessByUser: process for user " + oUser.getUserId());

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			List<ProcessWorkspace> aoProcess = oRepository.GetLastProcessByUser(oUser.getUserId());

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
		Utils.debugLog("ProcessWorkspaceResource.GetSummary( " + sSessionId + " )");
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

			//Wasdi.DebugLog("ProcessWorkspaceResource.GetSummary: process for user " + oUser.getUserId());

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Download Waiting Process List
			List<ProcessWorkspace> aoQueuedDownloads = oRepository.GetQueuedDownloads();

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
			List<ProcessWorkspace> aoQueuedProcessing = oRepository.GetQueuedProcess();

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
			List<ProcessWorkspace> aoQueuedIDL= oRepository.GetQueuedIDL();

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
			List<ProcessWorkspace> aoRunningProcessing = oRepository.GetRunningProcess();

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
			List<ProcessWorkspace> aoRunningDownload= oRepository.GetRunningDownloads();

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
			List<ProcessWorkspace> aoRunningIDL= oRepository.GetRunningIDL();

			oSummaryViewModel.setAllIDLRunning(aoRunningIDL.size());
			
			int iUserIDLRunning= 0 ;
			// Count the user's ones
			for (int iProcess=0; iProcess<aoRunningIDL.size(); iProcess++) {
				// Create View Model
				ProcessWorkspace oProcess = aoRunningIDL.get(iProcess);
				if (oProcess.getUserId().equals(oUser.getUserId())) iUserIDLRunning ++;
			}
			
			oSummaryViewModel.setUserIDLRunning(iUserIDLRunning);
			
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.GetSummary: " + oEx);
		}

		return oSummaryViewModel;
	}
	
	@GET
	@Path("/delete")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response deleteProcess(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProcessObjId") String sProcessObjId) {
		
		Utils.debugLog("ProcessWorkspaceResource.DeleteProcess( " + sSessionId + ", " + sProcessObjId + " )");

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		try {
			// Domain Check
			if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
				return Response.status(401).build();
			}

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessToDelete = oRepository.GetProcessByProcessObjId(sProcessObjId);
			
			if (oProcessToDelete != null)
			{
				
				int iPid = oProcessToDelete.getPid();
				
				if (iPid>0) {
					// Exists Pid, kill process
					String sShellExString = m_oServletConfig.getInitParameter("KillCommand") + " " + iPid;
					
					Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: shell exec " + sShellExString);
					
					Process oProc = Runtime.getRuntime().exec(sShellExString);
					
					Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: kill result: " + oProc.waitFor());

				} else {
					
					Utils.debugLog("ProcessWorkspaceResource. Process pid not in data");
					
				}
								
				// set process state to STOPPED only if CREATED or RUNNING
				String sPrecSatus = oProcessToDelete.getStatus();
				
				if (sPrecSatus.equalsIgnoreCase(ProcessStatus.CREATED.name()) || sPrecSatus.equalsIgnoreCase(ProcessStatus.RUNNING.name())) {
					
					oProcessToDelete.setStatus(ProcessStatus.STOPPED.name());
					oProcessToDelete.setOperationEndDate(Utils.GetFormatDate(new Date()));
					
					if (!oRepository.UpdateProcess(oProcessToDelete)) {
						Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: Unable to update process status");
					}
					
				} else {
					Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: Process already terminated: " + sPrecSatus);
				}
				
				
				return Response.ok().build();
				
			} else {
				
				Utils.debugLog("ProcessWorkspaceResource. Process not found in DB");
				return Response.status(404).build();
				
			}
						
		}
		catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.DeleteProcess: " + oEx);
		}

		return Response.status(500).build();
	}

	
	
	@GET
	@Path("/byid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceViewModel getProcessById(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sProcessId") String sProcessWorkspaceId) {
		Utils.debugLog("ProcessWorkspaceResource.GetProcessById( " + sSessionId + ", " + sProcessWorkspaceId + " )");

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

			//Wasdi.DebugLog("ProcessWorkspaceResource.getProcessById: process id " + sProcessWorkspaceId);

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			ProcessWorkspace oProcessWorkspace = oRepository.GetProcessByProcessObjId(sProcessWorkspaceId);
			oProcess = buildProcessWorkspaceViewModel(oProcessWorkspace);

		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.GetProcessById: " + oEx);
		}

		return oProcess;
	}

	
	@GET
	@Path("/updatebyid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceViewModel updateProcessById(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sProcessId") String sProcessWorkspaceId, @QueryParam("status") String sStatus,
			@QueryParam("perc") int iPerc, @QueryParam("sendrabbit") String sSendToRabbit) {
		
		Utils.debugLog("ProcessWorkspaceResource.UpdateProcessById( " + sSessionId + ", " +
				sProcessWorkspaceId + ", " + sStatus + ", " + iPerc + ", " + sSendToRabbit + " )" );

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
			ProcessWorkspace oProcessWorkspace = oRepository.GetProcessByProcessObjId(sProcessWorkspaceId);
			
			if (  (oProcessWorkspace.getStatus().equals(ProcessStatus.CREATED.name()) || oProcessWorkspace.getStatus().equals(ProcessStatus.RUNNING.name()) ) && (sStatus.equals(ProcessStatus.DONE.name()) || sStatus.equals(ProcessStatus.ERROR.name()) || sStatus.equals(ProcessStatus.STOPPED.name()) ) ) {
				// The process finished
				if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndDate())) {
					// No end-date set: put it here
					oProcessWorkspace.setOperationDate(Utils.GetFormatDate(new Date()));
				}
			}
			
			oProcessWorkspace.setStatus(sStatus);
			oProcessWorkspace.setProgressPerc(iPerc);

			oRepository.UpdateProcess(oProcessWorkspace);
			
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
		
		Utils.debugLog("ProcessWorkspaceResource.SetProcessPayload( " + sSessionId + ", " + sProcessWorkspaceId +
				", " + sPayload + " )" );

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
			ProcessWorkspace oProcessWorkspace = oRepository.GetProcessByProcessObjId(sProcessWorkspaceId);
			
			oProcessWorkspace.setPayload(sPayload);

			oRepository.UpdateProcess(oProcessWorkspace);
			
			oProcess = buildProcessWorkspaceViewModel(oProcessWorkspace);

		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.SetProcessPayload: " + oEx);
		}

		return oProcess;
	}
	
	@GET
	@Path("/cleanqueue")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult cleanQueue(@HeaderParam("x-session-token") String sSessionId) {
		
		Utils.debugLog("ProcessWorkspaceResource.CleanQueue( " + sSessionId + " )");
		
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
			oRepository.CleanQueue();
			
		}
		catch (Exception oEx) {
			Utils.debugLog("ProcessWorkspaceResource.CleanQueue: " + oEx);
		}
		
		return oResult;
	}

}
