package it.fadeout.rest.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;

import it.fadeout.Wasdi;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.WorkspaceSharing;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.data.WorkspaceSharingRepository;
import wasdi.shared.geoserver.GeoserverMethods;
import wasdi.shared.rabbit.RabbitMethods;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.ProcessWorkspaceViewModel;
import wasdi.shared.viewmodels.WorkspaceEditorViewModel;
import wasdi.shared.viewmodels.WorkspaceListInfoViewModel;

@Path("/ws")
public class WorkspaceResource {

	@Context
	ServletConfig m_oServletConfig;


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

	@POST
	@Path("update")
	@Produces({"application/xml", "application/json", "text/xml"})	
	public WorkspaceEditorViewModel UpdateWorkspace(@HeaderParam("x-session-token") String sSessionId, WorkspaceEditorViewModel oViewModel) {

		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

		try
		{
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
		catch(Exception oEx){
			oEx.printStackTrace();
			System.out.println("WorkspaceResource.UpdateWorkspace: Error update workspace: " + oEx.getMessage());
		}

		return null;

	}

	@DELETE
	@Path("delete")
	@Produces({"application/xml", "application/json", "text/xml"})	
	public Response DeleteWorkspace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("sWorkspaceId") String sWorkspaceId, @QueryParam("bDeleteLayer") Boolean bDeleteLayer, @QueryParam("bDeleteFile") Boolean bDeleteFile) {

		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) return null;
		if (Utils.isNullOrEmpty(oUser.getUserId())) return null;

		try
		{
			//repositories
			ProductWorkspaceRepository oProductRepository = new ProductWorkspaceRepository();
			PublishedBandsRepository oPublishRepository = new PublishedBandsRepository();
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

			if (oWorkspaceRepository.DeleteWorkspace(sWorkspaceId))
			{
				//delete exchange on rabbit
				if (!RabbitMethods.ExchangeDelete(sWorkspaceId))
					System.out.println("AuthService.Logout: Error deleting session queue rabbit.");
				
				
				//get all product in workspace
				List<ProductWorkspace> aoProducts = oProductRepository.GetProductsByWorkspace(sWorkspaceId);

				if (bDeleteFile)
				{
					try
					{
						//get workspace path
						String sDownloadRootPath = "";
						if (m_oServletConfig.getInitParameter("DownloadRootPath") != null) {
							sDownloadRootPath = m_oServletConfig.getInitParameter("DownloadRootPath");
							if (!m_oServletConfig.getInitParameter("DownloadRootPath").endsWith("/"))
								sDownloadRootPath += "/";
						}

						String sDownloadPath = sDownloadRootPath + oUser.getUserId()+ "/" + sWorkspaceId + "/";
						System.out.println("WorkspaceResource.DeleteWorkspace: Delete workspace " + sDownloadPath);
						//delete directory
						FileUtils.deleteDirectory(new File(sDownloadPath));
						//delete download on data base
						for (ProductWorkspace oProductWorkspace : aoProducts) {
							try
							{
								String sFilePath = sDownloadPath + oProductWorkspace.getProductName();
								oDownloadedFilesRepository.DeleteByFilePath(sFilePath);
							}
							catch(Exception oEx)
							{
								System.out.println("WorkspaceResource.DeleteWorkspace: Error deleting download on data base: " + oEx.getMessage());
							}
						}

					}
					catch(Exception oEx)
					{
						System.out.println("WorkspaceResource.DeleteWorkspace: Error deleting workspace directory: " + oEx.getMessage());
					}
				}

				if (bDeleteLayer)
				{
					for (ProductWorkspace oProductWorkspace : aoProducts) {
						List<PublishedBand> aoPublishedBands = oPublishRepository.GetPublishedBandsByProductName(oProductWorkspace.getProductName());
						for (PublishedBand oPublishedBand : aoPublishedBands) {
							try
							{
								System.out.println("WorkspaceResource.DeleteWorkspace: LayerId to delete " + oPublishedBand.getLayerId());
								String sResult = GeoserverMethods.DeleteLayer(oPublishedBand.getLayerId(), "json");

								try
								{
									//delete published band on data base
									oPublishRepository.DeleteByProductNameLayerId(oProductWorkspace.getProductName(), oPublishedBand.getLayerId());
								}
								catch(Exception oEx)
								{
									System.out.println("WorkspaceResource.DeleteWorkspace: error deleting published band on data base " + oEx.toString());
								}

							}
							catch(Exception oEx)
							{
								System.out.println("WorkspaceResource.DeleteWorkspace: error deleting layer id " + oEx.toString());
							}

						}
					}

				}

				return Response.ok().build();
			}
			else
				System.out.println("WorkspaceResource.DeleteWorkspace: Error deleting workspace on data base");	


		}
		catch(Exception oEx){
			oEx.printStackTrace();
			System.out.println("WorkspaceResource.DeleteWorkspace: Error deleting workspace: " + oEx.getMessage());
		}

		return null;
	}

}
