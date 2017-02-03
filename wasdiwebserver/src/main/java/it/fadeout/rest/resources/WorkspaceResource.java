package it.fadeout.rest.resources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import it.fadeout.Wasdi;
import it.fadeout.viewmodels.OrbitSearchViewModel;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.WorkspaceSharing;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.data.WorkspaceSharingRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProcessWorkspaceViewModel;
import wasdi.shared.viewmodels.WorkspaceEditorViewModel;
import wasdi.shared.viewmodels.WorkspaceListInfoViewModel;

@Path("/ws")
public class WorkspaceResource {
	@GET
	@Path("/byuser")
	@Produces({"application/xml", "application/json", "text/xml"})
	public ArrayList<WorkspaceListInfoViewModel> GetListByUser(@HeaderParam("x-session-token") String sSessionId) {
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		ArrayList<WorkspaceListInfoViewModel> aoWSList = new ArrayList<>();
		
		try {
			// Domain Check
			if (oUser == null) {
				return aoWSList;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return aoWSList;
			}
			
			System.out.println("WorkspaceResource.GetListByUser: workspaces for " + oUser.getUserId());
			
			// Create repo
			WorkspaceRepository oWSRepository = new WorkspaceRepository();
			WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
			
			// Get Workspace List
			List<Workspace> aoWorkspaces = oWSRepository.GetWorkspaceByUser(oUser.getUserId());
			
			// For each
			for (int iWorkspaces=0; iWorkspaces<aoWorkspaces.size(); iWorkspaces++) {
				// Create View Model
				WorkspaceListInfoViewModel oWSViewModel = new WorkspaceListInfoViewModel();
				Workspace oWorkspace = aoWorkspaces.get(iWorkspaces);
				
				oWSViewModel.setOwnerUserId(oUser.getUserId());
				oWSViewModel.setWorkspaceId(oWorkspace.getWorkspaceId());
				oWSViewModel.setWorkspaceName(oWorkspace.getName());
				
				// Get Sharings
				List<WorkspaceSharing> aoSharings = oWorkspaceSharingRepository.GetWorkspaceSharingByWorkspace(oWorkspace.getWorkspaceId());
				
				// Add Sharings to View Model
				if (aoSharings != null) {
					for (int iSharings=0; iSharings<aoSharings.size(); iSharings++) {
						if (oWSViewModel.getSharedUsers() == null) {
							oWSViewModel.setSharedUsers(new ArrayList<String>());
						}
						
						oWSViewModel.getSharedUsers().add(aoSharings.get(iSharings).getUserId());
					}
				}
				
				aoWSList.add(oWSViewModel);
				
			}
				
		}
		catch (Exception oEx) {
			oEx.toString();
		}
		

		
		return aoWSList;
	}
	
	@GET
	@Path("/processbyws")
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
			
			System.out.println("WorkspaceResource.GetProcessByWorkspace: process for ws " + sWorkspaceId);
			
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
				
				aoProcessList.add(oViewModel);
				
			}
				
		}
		catch (Exception oEx) {
			System.out.println("WorkspaceResource.GetProcessByWorkspace: error retrieving process " + oEx.getMessage());
			oEx.toString();
		}
				
		return aoProcessList;
	}
	
	
	@GET
	@Path("")
	@Produces({"application/xml", "application/json", "text/xml"})
	public WorkspaceEditorViewModel GetWorkspaceEditorViewModel(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId) {
				
		WorkspaceEditorViewModel oVM = new WorkspaceEditorViewModel();
		
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		
		if (oUser==null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		
		try {
			// Domain Check
			if (sWorkspaceId == null) {
				return oVM;
			}
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				return oVM;
			}
			
			System.out.println("WorkspaceResource.GetWorkspaceEditorViewModel: read workspaces " + sWorkspaceId);
			
			// Create repo
			WorkspaceRepository oWSRepository = new WorkspaceRepository();
			WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
			
			// Get Workspace List
			Workspace oWorkspace = oWSRepository.GetWorkspace(sWorkspaceId);
							
			oVM.setUserId(oWorkspace.getUserId());
			oVM.setWorkspaceId(oWorkspace.getWorkspaceId());
			oVM.setName(oWorkspace.getName());
			oVM.setCreationDate(Utils.getDate(oWorkspace.getCreationDate()) );
			oVM.setLastEditDate(Utils.getDate(oWorkspace.getLastEditDate()));
				
			// Get Sharings
			List<WorkspaceSharing> aoSharings = oWorkspaceSharingRepository.GetWorkspaceSharingByWorkspace(oWorkspace.getWorkspaceId());
				
			// Add Sharings to View Model
			if (aoSharings != null) {
				for (int iSharings=0; iSharings<aoSharings.size(); iSharings++) {
					if (oVM.getSharedUsers() == null) {
						oVM.setSharedUsers(new ArrayList<String>());
					}
					
					oVM.getSharedUsers().add(aoSharings.get(iSharings).getUserId());
				}
			}
		}
		catch (Exception oEx) {
			oEx.toString();
		}
		
		return oVM;
	}
	
	@GET
	@Path("create")
	@Produces({"application/xml", "application/json", "text/xml"})	
	public PrimitiveResult CreateWorkspace(@HeaderParam("x-session-token") String sSessionId) {
		
		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		// Create New Workspace
		Workspace oWorkspace = new Workspace();
		
		// Default values
		oWorkspace.setCreationDate((double)new Date().getTime());
		oWorkspace.setLastEditDate((double)new Date().getTime());
		oWorkspace.setName("Untitled Workspace");
		oWorkspace.setUserId(oUser.getUserId());
		oWorkspace.setWorkspaceId(Utils.GetRandomName());
		
		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		if (oWorkspaceRepository.InsertWorkspace(oWorkspace)) {
			
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setStringValue(oWorkspace.getWorkspaceId());
			
			return oResult;			
		}
		else {
			return null;
		}
		
	}
	
	@GET
	@Path("update")
	@Produces({"application/xml", "application/json", "text/xml"})	
	public WorkspaceEditorViewModel UpdateWorkspace(@HeaderParam("x-session-token") String sSessionId, WorkspaceEditorViewModel oViewModel) {
		
		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;
		
		// Create New Workspace
		Workspace oWorkspace = new Workspace();
		
		// Default values
		oWorkspace.setCreationDate((double)oViewModel.getCreationDate().getTime());
		oWorkspace.setLastEditDate((double)oViewModel.getLastEditDate().getTime());
		oWorkspace.setName(oViewModel.getName());
		oWorkspace.setUserId(oViewModel.getUserId());
		oWorkspace.setWorkspaceId(oViewModel.getWorkspaceId());
		
		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		if (oWorkspaceRepository.UpdateWorkspace(oWorkspace)) {
			
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setStringValue(oWorkspace.getWorkspaceId());
			
			return oViewModel;			
		}
		else {
			return null;
		}
		
	}
	
}
