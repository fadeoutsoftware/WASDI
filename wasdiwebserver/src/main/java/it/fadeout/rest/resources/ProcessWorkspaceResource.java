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
			if (oUser == null) {
				return Response.status(401).build();
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return Response.status(401).build();
			}

			// Create repo
			ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
			ProcessWorkspace oProcessToDelete = oRepository.GetProcessByProcessObjId(sProcessObjId);
			
			if (oProcessToDelete != null)
			{
				
				int iPid = oProcessToDelete.getPid();
				
				// Exists Pid, kill process
				String sShellExString = m_oServletConfig.getInitParameter("KillCommand") + iPid;
				
				System.out.println("ProcessWorkspaceResource.DeleteProcess: shell exec " + sShellExString);
				
				Process oProc = Runtime.getRuntime().exec(sShellExString);

				//delete process on database
				if (oRepository.DeleteProcessWorkspaceByPid(iPid))
				{
					return Response.ok().build();
				}
				else {
					return Response.status(400).build();
				}
				
			}
			else {
				return Response.status(400).build();
			}
						
		}
		catch (Exception oEx) {
			System.out.println("WorkspaceResource.DeleteProcess: error deleting process " + oEx.getMessage());
			oEx.printStackTrace();
		}

		return Response.status(500).build();
	}

	
	

}
