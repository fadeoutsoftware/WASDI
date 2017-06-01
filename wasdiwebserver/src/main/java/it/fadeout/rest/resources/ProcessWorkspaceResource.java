package it.fadeout.rest.resources;

import java.util.ArrayList;
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
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ProcessWorkspaceViewModel;

@Path("/process")
public class ProcessWorkspaceResource {
	
	@Context
	ServletConfig m_oServletConfig;	
	
	@GET
	@Path("/byws")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<ProcessWorkspaceViewModel> GetProcessByWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) {

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

			System.out.println("ProcessWorkspaceResource.GetProcessByWorkspace: process for ws " + sWorkspaceId);

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();

			// Get Process List
			List<ProcessWorkspace> aoProcess = oRepository.GetProcessByWorkspace(sWorkspaceId);

			// For each
			for (int iProcess=0; iProcess<aoProcess.size(); iProcess++) {
				// Create View Model
				ProcessWorkspaceViewModel oViewModel = new ProcessWorkspaceViewModel();
				ProcessWorkspace oProcess = aoProcess.get(iProcess);

				oViewModel.setOperationDate(oProcess.getOperationDate());
				oViewModel.setOperationType(oProcess.getOperationType());
				oViewModel.setProductName(oProcess.getProductName());
				oViewModel.setUserId(oProcess.getUserId());
				oViewModel.setFileSize(oProcess.getFileSize());
				oViewModel.setPid(oProcess.getPid());
				oViewModel.setStatus(oProcess.getStatus());
				oViewModel.setProgressPerc(oProcess.getProgressPerc());
				oViewModel.setProcessObjId(oProcess.getProcessObjId());

				aoProcessList.add(oViewModel);

			}

		}
		catch (Exception oEx) {
			System.out.println("ProcessWorkspaceResource.GetProcessByWorkspace: error retrieving process " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return aoProcessList;
	}
	
	@GET
	@Path("/delete")
	@Produces({"application/xml", "application/json", "text/xml"})
	public Response DeleteProcess(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sProcessObjId") String sProcessObjId) {

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
				if (sPrecSatus.equalsIgnoreCase(ProcessStatus.CREATED.name()) || 
						sPrecSatus.equalsIgnoreCase(ProcessStatus.RUNNING.name())) {
					
					oProcessToDelete.setStatus(ProcessStatus.STOPPED.name());
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

	
	

}
