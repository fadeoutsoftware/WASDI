package it.fadeout.rest.resources.labelling;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import wasdi.shared.business.labelling.Attribute;
import wasdi.shared.business.labelling.DatasetProject;
import wasdi.shared.business.labelling.Template;
import wasdi.shared.business.users.User;
import wasdi.shared.data.labelling.DatasetProjectRepository;
import wasdi.shared.data.labelling.LabellingTemplateRepository;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.labelling.attributes.AttributeViewModel;
import wasdi.shared.viewmodels.labelling.datasets.DatasetCollaboratorViewModel;
import wasdi.shared.viewmodels.labelling.datasets.DatasetListViewModel;
import wasdi.shared.viewmodels.labelling.datasets.DatasetViewModel;
import wasdi.shared.viewmodels.labelling.templates.TemplateListViewModel;
import wasdi.shared.viewmodels.labelling.templates.TemplateViewModel;

@Path("/labelling/datasets")
public class DatasetResource {
	
	@GET
	@Path("/list")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getList(@HeaderParam("x-session-token") String sSessionId) {
		
		WasdiLog.debugLog("DatasetResource.getPublicList");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		List<DatasetListViewModel> aoDataasetsList = new ArrayList<>();

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("DatasetResource.getList: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		try {
			
			WasdiLog.debugLog("DatasetResource.getList: datasets for " + oUser.getUserId());

			// Create repo
			DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
			
			List<DatasetProject> aoDatasets = oDatasetRepository.getDatasetsForUser(oUser.getUserId());
			
			if (aoDatasets==null) {
				WasdiLog.debugLog("DatasetResource.getList: aoTemplates is null");
				return Response.ok(aoDataasetsList).build();
			}			
			
			// For each
			for (DatasetProject oDataset : aoDatasets) {
				// Create View Model
				DatasetListViewModel oDatasetListViewModel = new DatasetListViewModel();
				
				oDatasetListViewModel.name = oDataset.getName();
				oDatasetListViewModel.id = oDataset.getId();
				
				if (oDataset.getOwnersIds().equals(oUser.getUserId())) {
					oDatasetListViewModel.userRole = "OWNER";	
				}
				else {
					oDatasetListViewModel.userRole = "GUEST";
				}
				oDatasetListViewModel.bbox = oDataset.getBbox();
				oDatasetListViewModel.description = oDataset.getDescription();
				oDatasetListViewModel.isGlobal = oDataset.isGlobal();
				oDatasetListViewModel.mission = oDataset.getMissions();
				for (String sTask : oDataset.getTasks()) {
					oDatasetListViewModel.tasks.add(sTask);
				}
				
				aoDataasetsList.add(oDatasetListViewModel);
			}
			
			return Response.ok(aoDataasetsList).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("DatasetResource.getList error: " + oEx);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("datasetId") String sDatasetId) {
		
		WasdiLog.debugLog("DatasetResource.getById");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("DatasetResource.getById: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		try {
			
			WasdiLog.debugLog("DatasetResource.getById: dataset " + sDatasetId);

			// Create repo
			DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
			
			DatasetProject oDataset = oDatasetRepository.getDataset(sDatasetId);
			
			if (oDataset==null) {
				WasdiLog.debugLog("DatasetResource.getById: oDataset is null");
				return Response.status(Status.BAD_REQUEST).build();
			}			
			
			if ( !oDataset.getOwnersIds().equals(oUser.getUserId())) {
				if (!oDataset.isPublic()) {
					WasdiLog.warnLog("DatasetResource.getById: user cannot access the dataset");
					return Response.status(Status.UNAUTHORIZED).build();					
				}
			}
			
			DatasetViewModel oDatasetViewModel = new DatasetViewModel();
			
			
			oDatasetViewModel.name = oDataset.getName();
			oDatasetViewModel.id = oDataset.getId();
			oDatasetViewModel.description = oDataset.getDescription();
			oDatasetViewModel.annotatorSeeAllLabels = oDataset.isAnnotatorSeeAllLabels();
			oDatasetViewModel.bbox = oDataset.getBbox();
			oDatasetViewModel.creationDate = oDataset.getCreationDate();
			oDatasetViewModel.endDate = oDataset.getEndDate();
			oDatasetViewModel.isGlobal = oDataset.isGlobal();
			oDatasetViewModel.isPublic = oDataset.isPublic();
			oDatasetViewModel.link = oDataset.getLink();
			oDatasetViewModel.minReviewCount = oDataset.getMinReviewCount();
			oDatasetViewModel.missions = oDataset.getMissions();
			oDatasetViewModel.ownersIds = oDataset.getOwnersIds();
			oDatasetViewModel.reviewRequired = oDataset.isReviewRequired();
			oDatasetViewModel.startDate = oDataset.getStartDate();
			oDatasetViewModel.tasks.addAll(oDataset.getTasks());
			
			
			return Response.ok(oDatasetViewModel).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("DatasetResource.getById error: " + oEx);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("/collaborators")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getCollaborators(@HeaderParam("x-session-token") String sSessionId, @QueryParam("datasetId") String sDatasetId) {
		
		WasdiLog.debugLog("DatasetResource.getCollaborators");

		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		ArrayList<DatasetCollaboratorViewModel> aoCollaborators = new ArrayList<>();

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("DatasetResource.getCollaborators: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		try {
			
			WasdiLog.debugLog("DatasetResource.getCollaborators: dataset " + sDatasetId);

			// Create repo
			DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
			
			DatasetProject oDataset = oDatasetRepository.getDataset(sDatasetId);
			
			if (oDataset==null) {
				WasdiLog.debugLog("DatasetResource.getCollaborators: oDataset is null");
				return Response.status(Status.BAD_REQUEST).build();
			}			
			
			if ( !oDataset.getOwnersIds().equals(oUser.getUserId())) {
				if (!oDataset.isPublic()) {
					WasdiLog.warnLog("DatasetResource.getCollaborators: user cannot access the dataset");
					return Response.status(Status.UNAUTHORIZED).build();					
				}
			}
			
			for (String sAnnotator : oDataset.getAnnotators()) {
				DatasetCollaboratorViewModel oVM = new DatasetCollaboratorViewModel();
				oVM.userId = sAnnotator;
				oVM.userRole = "ANNOTATOR";
				aoCollaborators.add(oVM);
			}
			
			for (String sReviewer : oDataset.getReviewers()) {
				DatasetCollaboratorViewModel oVM = new DatasetCollaboratorViewModel();
				oVM.userId = sReviewer;
				oVM.userRole = "REVIEWER";
				aoCollaborators.add(oVM);
			}
			
			return Response.ok(aoCollaborators).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("DatasetResource.getCollaborators error: " + oEx);
			return Response.serverError().build();
		}
	}	
	

}
