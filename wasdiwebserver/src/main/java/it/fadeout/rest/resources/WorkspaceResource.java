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
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.WorkspaceSharing;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.data.WorkspaceSharingRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;
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
	
}
