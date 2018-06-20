package it.fadeout.rest.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
	public ArrayList<ProcessWorkspaceViewModel> GetProcessByWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) {
		
		Wasdi.DebugLog("ProcessWorkspaceResource.GetProcessByWorkspace");

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

			System.out.println("ProcessWorkspaceResource.GetProcessByWorkspace: process for ws " + sWorkspaceId);

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			List<ProcessWorkspace> aoProcess = oRepository.GetProcessByWorkspace(sWorkspaceId);

			// For each
			for (int iProcess=0; iProcess<aoProcess.size(); iProcess++) {
				// Create View Model
				ProcessWorkspace oProcess = aoProcess.get(iProcess);
				aoProcessList.add(buildProcessWorkspaceViewModel(oProcess));
			}

		}
		catch (Exception oEx) {
			System.out.println("ProcessWorkspaceResource.GetProcessByWorkspace: error retrieving process " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return aoProcessList;
	}

	
	@GET
	@Path("/byusr")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<ProcessWorkspaceViewModel> GetProcessByUser(@HeaderParam("x-session-token") String sSessionId) {
		
		Wasdi.DebugLog("ProcessWorkspaceResource.GetProcessByUser");

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

			System.out.println("ProcessWorkspaceResource.GetProcessByUser: process for User " + oUser.getUserId());

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
			System.out.println("ProcessWorkspaceResource.GetProcessByUser: error retrieving process " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return aoProcessList;
	}


	private ProcessWorkspaceViewModel buildProcessWorkspaceViewModel(ProcessWorkspace oProcess) {
		ProcessWorkspaceViewModel oViewModel = new ProcessWorkspaceViewModel();
		oViewModel.setOperationDate(oProcess.getOperationDate());
		oViewModel.setOperationEndDate(oProcess.getOperationEndDate());
		oViewModel.setOperationType(oProcess.getOperationType());
		oViewModel.setProductName(oProcess.getProductName());
		oViewModel.setUserId(oProcess.getUserId());
		oViewModel.setFileSize(oProcess.getFileSize());
		oViewModel.setPid(oProcess.getPid());
		oViewModel.setStatus(oProcess.getStatus());
		oViewModel.setProgressPerc(oProcess.getProgressPerc());
		oViewModel.setProcessObjId(oProcess.getProcessObjId());
		oViewModel.setPayload(oProcess.getPayload());
		return oViewModel;
	}
	
	
	@GET
	@Path("/lastbyws")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<ProcessWorkspaceViewModel> GetLastProcessByWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) {
		
		Wasdi.DebugLog("ProcessWorkspaceResource.GetLastProcessByWS");

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


			System.out.println("ProcessWorkspaceResource.GetProcessByWorkspace: process for ws " + sWorkspaceId);

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
			System.out.println("ProcessWorkspaceResource.GetProcessByWorkspace: error retrieving process " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return aoProcessList;
	}
	
	@GET
	@Path("/lastbyusr")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<ProcessWorkspaceViewModel> GetLastProcessByUser(@HeaderParam("x-session-token") String sSessionId) {
		
		Wasdi.DebugLog("ProcessWorkspaceResource.GetLastProcessByUser");

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

			System.out.println("ProcessWorkspaceResource.GetLastProcessByUser: process for user " + oUser.getUserId());

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
			System.out.println("ProcessWorkspaceResource.GetLastProcessByUser: error retrieving process " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return aoProcessList;
	}
	
	@GET
	@Path("/summary")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceSummaryViewModel GetSummary(@HeaderParam("x-session-token") String sSessionId) {
		
		Wasdi.DebugLog("ProcessWorkspaceResource.GetSummary");

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

			System.out.println("ProcessWorkspaceResource.GetSummary: process for user " + oUser.getUserId());

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

		}
		catch (Exception oEx) {
			System.out.println("ProcessWorkspaceResource.GetSummary: error retrieving process " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return oSummaryViewModel;
	}
	
	@GET
	@Path("/delete")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response DeleteProcess(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProcessObjId") String sProcessObjId) {
		
		Wasdi.DebugLog("ProcessWorkspaceResource.DeleteProcess");

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
					
					System.out.println("ProcessWorkspaceResource.DeleteProcess: shell exec " + sShellExString);
					
					Process oProc = Runtime.getRuntime().exec(sShellExString);
					
					System.out.println("ProcessWorkspaceResource.DeleteProcess: kill result: " + oProc.waitFor());

				} else {
					
					System.out.println("ProcessWorkspaceResource. Process pid not in data");
					
				}
								
				// set process state to STOPPED only if CREATED or RUNNING
				String sPrecSatus = oProcessToDelete.getStatus();
				
				if (sPrecSatus.equalsIgnoreCase(ProcessStatus.CREATED.name()) || sPrecSatus.equalsIgnoreCase(ProcessStatus.RUNNING.name())) {
					
					oProcessToDelete.setStatus(ProcessStatus.STOPPED.name());
					oProcessToDelete.setOperationEndDate(Utils.GetFormatDate(new Date()));
					
					if (!oRepository.UpdateProcess(oProcessToDelete)) {
						System.out.println("ProcessWorkspaceResource.DeleteProcess: Unable to update process status");
					}
					
				} else {
					System.out.println("ProcessWorkspaceResource.DeleteProcess: Process already terminated: " + sPrecSatus);
				}
				
				
				return Response.ok().build();
				
			} else {
				
				System.out.println("ProcessWorkspaceResource. Process not found in DB");
				return Response.status(404).build();
				
			}
						
		}
		catch (Exception oEx) {
			System.out.println("WorkspaceResource.DeleteProcess: error deleting process " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return Response.status(500).build();
	}

	
	
	@GET
	@Path("/byid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceViewModel GetProcessById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProcessId") String sProcessWorkspaceId) {
		
		Wasdi.DebugLog("ProcessWorkspaceResource.GetProcessById");

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

			System.out.println("ProcessWorkspaceResource.GetProcessByUser: process id " + sProcessWorkspaceId);

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			ProcessWorkspace oProcessWorkspace = oRepository.GetProcessByProcessObjId(sProcessWorkspaceId);
			oProcess = buildProcessWorkspaceViewModel(oProcessWorkspace);

		}
		catch (Exception oEx) {
			System.out.println("ProcessWorkspaceResource.GetProcessById: error retrieving process " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return oProcess;
	}

	
	@GET
	@Path("/updatebyid")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceViewModel UpdateProcessById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProcessId") String sProcessWorkspaceId, @QueryParam("status") String sStatus, @QueryParam("perc") int iPerc) {
		
		Wasdi.DebugLog("ProcessWorkspaceResource.UpdateProcessById");

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

			System.out.println("ProcessWorkspaceResource.UpdateProcessById: process id " + sProcessWorkspaceId);

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			ProcessWorkspace oProcessWorkspace = oRepository.GetProcessByProcessObjId(sProcessWorkspaceId);
			
			oProcessWorkspace.setStatus(sStatus);
			oProcessWorkspace.setProgressPerc(iPerc);

			oRepository.UpdateProcess(oProcessWorkspace);
			
			oProcess = buildProcessWorkspaceViewModel(oProcessWorkspace);

		}
		catch (Exception oEx) {
			System.out.println("ProcessWorkspaceResource.UpdateProcessById: error retrieving process " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return oProcess;
	}
	
	
	@GET
	@Path("/setpayload")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ProcessWorkspaceViewModel SetProcessPayload(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProcessId") String sProcessWorkspaceId, @QueryParam("payload") String sPayload) {
		
		Wasdi.DebugLog("ProcessWorkspaceResource.SetProcessPayload");

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

			System.out.println("ProcessWorkspaceResource.SetProcessPayload: process id " + sProcessWorkspaceId);
			System.out.println("ProcessWorkspaceResource.SetProcessPayload: PAYLOAD " + sPayload);

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			ProcessWorkspace oProcessWorkspace = oRepository.GetProcessByProcessObjId(sProcessWorkspaceId);
			
			oProcessWorkspace.setPayload(sPayload);

			oRepository.UpdateProcess(oProcessWorkspace);
			
			oProcess = buildProcessWorkspaceViewModel(oProcessWorkspace);

		}
		catch (Exception oEx) {
			System.out.println("ProcessWorkspaceResource.SetProcessPayload: error retrieving process " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return oProcess;
	}
	
	@GET
	@Path("/cleanqueue")
	@Produces({"application/xml", "application/json", "text/xml"})
	public PrimitiveResult CleanQueue(@HeaderParam("x-session-token") String sSessionId) {
		
		Wasdi.DebugLog("ProcessWorkspaceResource.CleanQueue");
		
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
			System.out.println("ProcessWorkspaceResource.CleanQueue: error " + oEx.getMessage());
			oEx.printStackTrace();
			
		}
		
		return oResult;
	}

}
