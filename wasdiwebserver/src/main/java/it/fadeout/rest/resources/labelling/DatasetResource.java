package it.fadeout.rest.resources.labelling;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.joda.time.DateTimeUtils;

import it.fadeout.Wasdi;
import it.fadeout.rest.resources.WorkspaceResource;
import wasdi.shared.business.labelling.DatasetProject;
import wasdi.shared.business.labelling.LabellingProjectRoles;
import wasdi.shared.business.users.User;
import wasdi.shared.data.labelling.DatasetProjectRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.labelling.datasets.DatasetCollaboratorViewModel;
import wasdi.shared.viewmodels.labelling.datasets.DatasetListViewModel;
import wasdi.shared.viewmodels.labelling.datasets.DatasetViewModel;

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
				WasdiLog.warnLog("DatasetResource.getList: aoTemplates is null");
				return Response.ok(aoDataasetsList).build();
			}			
			
			// For each
			for (DatasetProject oDataset : aoDatasets) {
				// Create View Model
				DatasetListViewModel oDatasetListViewModel = new DatasetListViewModel();
				
				oDatasetListViewModel.name = oDataset.getName();
				oDatasetListViewModel.id = oDataset.getId();
				
				if (oDataset.getOwner().equals(oUser.getUserId())) {
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
				WasdiLog.warnLog("DatasetResource.getById: oDataset is null");
				return Response.status(Status.BAD_REQUEST).build();
			}			
			
			if ( !oDataset.getOwner().equals(oUser.getUserId())) {
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
			oDatasetViewModel.owner = oDataset.getOwner();
			oDatasetViewModel.reviewRequired = oDataset.isReviewRequired();
			oDatasetViewModel.startDate = oDataset.getStartDate();
			oDatasetViewModel.templateId = oDataset.getTemplateId();
			oDatasetViewModel.tasks.addAll(oDataset.getTasks());
			oDatasetViewModel.workspaceId = oDataset.getWorkspaceId();
			
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
				WasdiLog.warnLog("DatasetResource.getCollaborators: oDataset is null");
				return Response.status(Status.BAD_REQUEST).build();
			}			
			
			if ( !oDataset.getOwner().equals(oUser.getUserId())) {
				if (!oDataset.isPublic()) {
					WasdiLog.warnLog("DatasetResource.getCollaborators: user cannot access the dataset");
					return Response.status(Status.UNAUTHORIZED).build();					
				}
			}
			
			for (String sAnnotator : oDataset.getAnnotators()) {
				DatasetCollaboratorViewModel oVM = new DatasetCollaboratorViewModel();
				oVM.userId = sAnnotator;
				oVM.userRole = LabellingProjectRoles.ANNOTATOR.name();
				aoCollaborators.add(oVM);
			}
			
			for (String sReviewer : oDataset.getReviewers()) {
				DatasetCollaboratorViewModel oVM = new DatasetCollaboratorViewModel();
				oVM.userId = sReviewer;
				oVM.userRole = LabellingProjectRoles.REVIEWER.name();
				aoCollaborators.add(oVM);
			}
			
			return Response.ok(aoCollaborators).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("DatasetResource.getCollaborators error: " + oEx);
			return Response.serverError().build();
		}
	}	
	
	@POST
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response create(@HeaderParam("x-session-token") String sSessionId, DatasetViewModel oDatasetViewModel) {
		WasdiLog.debugLog("DatasetResource.create");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("DatasetResource.create: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		if (oDatasetViewModel == null) {
			WasdiLog.warnLog("DatasetResource.create: invalid oDatasetViewModel");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			
			WasdiLog.debugLog("DatasetResource.create:");

			// Create repo
			DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
			
			DatasetProject oDataset = new DatasetProject();
			
			
			oDataset.setName(oDatasetViewModel.name);
			oDataset.setId(Utils.getRandomName());
			oDataset.setOwner(oUser.getUserId());			
			oDataset.setCreationDate(DateTimeUtils.currentTimeMillis());
			oDataset.setDescription(oDatasetViewModel.description) ;
			oDataset.setAnnotatorSeeAllLabels(oDatasetViewModel.annotatorSeeAllLabels);
			oDataset.setBbox(oDatasetViewModel.bbox);
			oDataset.setCreationDate(DateTimeUtils.currentTimeMillis());
			oDataset.setEndDate(oDatasetViewModel.endDate);
			oDataset.setGlobal(oDatasetViewModel.isGlobal);
			oDataset.setId(Utils.getRandomName());
			oDataset.setLink(oDatasetViewModel.link);
			oDataset.setMinReviewCount(oDatasetViewModel.minReviewCount);
			oDataset.setMissions(oDatasetViewModel.missions);
			oDataset.setPublic(oDatasetViewModel.isPublic);
			oDataset.setReviewRequired(oDatasetViewModel.reviewRequired);
			oDataset.setStartDate(oDatasetViewModel.startDate);
			oDataset.getTasks().addAll(oDatasetViewModel.tasks);
			oDataset.setTemplateId(oDatasetViewModel.templateId);
			WorkspaceResource oWorkspaceResource = new WorkspaceResource();
			PrimitiveResult oPrimitiveResult = oWorkspaceResource.createWorkspace(sSessionId, "labelling_"+oDatasetViewModel.name, "");
			if (oPrimitiveResult == null) {
				WasdiLog.errorLog("DatasetResource.create: we could not create the workspace associated to the dataset.. not good at all...");
				return Response.serverError().build();				
			}
			else {
				oDataset.setWorkspaceId(oPrimitiveResult.getStringValue());
			}
			
			oDatasetRepository.insertDataset(oDataset);
			
			return Response.ok(oDataset.getId()).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("DatasetResource.create error: " + oEx);
			return Response.serverError().build();
		}		
	}
	
	@PUT
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response update(@HeaderParam("x-session-token") String sSessionId, DatasetViewModel oDatasetViewModel) {
		WasdiLog.debugLog("DatasetResource.update");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("DatasetResource.update: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		if (oDatasetViewModel == null) {
			WasdiLog.warnLog("DatasetResource.update: invalid oDatasetViewModel");
			return Response.status(Status.BAD_REQUEST).build();
		}
		

		try {
			
			WasdiLog.debugLog("DatasetResource.update: " + oDatasetViewModel.id);

			// Create repo
			DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
			
			DatasetProject oDataset = oDatasetRepository.getDataset(oDatasetViewModel.id);
			
			if (oDataset == null) {
				WasdiLog.warnLog("DatasetResource.update: dataset not found");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (!oDataset.getOwner().equals(oUser.getUserId())) {
				WasdiLog.warnLog("DatasetResource.update: the user is not the creator for the dataset");
				return Response.status(Status.UNAUTHORIZED).build();				
			}
			
			oDataset.setName(oDatasetViewModel.name);
			oDataset.setDescription(oDatasetViewModel.description) ;
			oDataset.setAnnotatorSeeAllLabels(oDatasetViewModel.annotatorSeeAllLabels);
			oDataset.setBbox(oDatasetViewModel.bbox);
			oDataset.setCreationDate(DateTimeUtils.currentTimeMillis());
			oDataset.setEndDate(oDatasetViewModel.endDate);
			oDataset.setGlobal(oDatasetViewModel.isGlobal);
			oDataset.setLink(oDatasetViewModel.link);
			oDataset.setMinReviewCount(oDatasetViewModel.minReviewCount);
			oDataset.setMissions(oDatasetViewModel.missions);
			oDataset.setPublic(oDatasetViewModel.isPublic);
			oDataset.setReviewRequired(oDatasetViewModel.reviewRequired);
			oDataset.setStartDate(oDatasetViewModel.startDate);
			oDataset.getTasks().clear();
			oDataset.getTasks().addAll(oDatasetViewModel.tasks);
			oDataset.setTemplateId(oDatasetViewModel.templateId);
			
			oDatasetRepository.updateDataset(oDataset);
			
			return Response.ok().build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("DatasetResource.update error: " + oEx);
			return Response.serverError().build();
		}		
	}	
	
	@DELETE
	@Path("/")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response delete(@HeaderParam("x-session-token") String sSessionId, @QueryParam("datasetId") String sDatasetId) {
		WasdiLog.debugLog("DatasetResource.delete");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("DatasetResource.delete: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		try {
			
			WasdiLog.debugLog("DatasetResource.delete: " + sDatasetId);

			// Create repo
			DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
			
			DatasetProject oDataset = oDatasetRepository.getDataset(sDatasetId);
			
			if (oDataset == null) {
				WasdiLog.warnLog("DatasetResource.delete: dataset not found");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (!oDataset.getOwner().equals(oUser.getUserId())) {
				WasdiLog.warnLog("DatasetResource.delete: the user is not the creator for the dataset");
				return Response.status(Status.UNAUTHORIZED).build();				
			}
			
			WorkspaceResource oWorkspaceResource = new WorkspaceResource();
			oWorkspaceResource.deleteWorkspace(sSessionId, oDataset.getWorkspaceId(), true, true);
			
			oDatasetRepository.deleteDataset(sDatasetId);
			
			return Response.ok().build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("DatasetResource.delete error: " + oEx);
			return Response.serverError().build();
		}		
	}		
	
	
	@POST
	@Path("/collaborators")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response addCollaborator(@HeaderParam("x-session-token") String sSessionId, @QueryParam("datasetId") String sDatasetId, @QueryParam("userId") String sUserId, @QueryParam("roleId") String sRole) {
		
		WasdiLog.debugLog("DatasetResource.addCollaborator");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("DatasetResource.addCollaborator: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		try {
			
			WasdiLog.debugLog("DatasetResource.addCollaborator: dataset " + sUserId);

			// Create repo
			DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
			
			DatasetProject oDataset = oDatasetRepository.getDataset(sDatasetId);
			
			if (oDataset==null) {
				WasdiLog.warnLog("DatasetResource.addCollaborator: oDataset is null");
				return Response.status(Status.BAD_REQUEST).build();
			}			
			
			if ( !oDataset.getOwner().equals(oUser.getUserId())) {
				if (!oDataset.isPublic()) {
					WasdiLog.warnLog("DatasetResource.addCollaborator: user cannot access the dataset");
					return Response.status(Status.UNAUTHORIZED).build();					
				}
			}
			
			if (oUser.getUserId().equals(sUserId)) {
				WasdiLog.warnLog("DatasetResource.addCollaborator: target user is the owner, bad request");
				return Response.status(Status.BAD_REQUEST).build();		
			}
			
			boolean bExisting = false;
			
			for (String sAnnotator : oDataset.getAnnotators()) {
				if (sAnnotator.equals(sUserId)) {
					bExisting = true;
					break;
				}
			}
			
			if (!bExisting) {
				for (String sReviewer : oDataset.getReviewers()) {
					if (sReviewer.equals(sUserId)) {
						bExisting = true;
						break;
					}
				}				
			}
			
			if (bExisting) {
				WasdiLog.warnLog("DatasetResource.addCollaborator: user is already part of the Dataset as collaborator");
				return Response.status(Status.CONFLICT).build();				
			}

			if (!isValidLabellingProjectRole(sRole)) {
				WasdiLog.warnLog("DatasetResource.addCollaborator: invalid role");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (sRole.equals(LabellingProjectRoles.OWNER.name())) {
				WasdiLog.warnLog("DatasetResource.addCollaborator: the user cannot add another owner");
				return Response.status(Status.BAD_REQUEST).build();				
			}
			
			if (sRole.equals(LabellingProjectRoles.ANNOTATOR.name())) {
				oDataset.getAnnotators().add(sUserId);
			}
			else if (sRole.equals(LabellingProjectRoles.REVIEWER.name())) {
				oDataset.getReviewers().add(sUserId);
			}
			
			return Response.ok().build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("DatasetResource.addCollaborator error: " + oEx);
			return Response.serverError().build();
		}
	}		

	
	@DELETE
	@Path("/collaborators")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response deleteCollaborator(@HeaderParam("x-session-token") String sSessionId, @QueryParam("datasetId") String sDatasetId, @QueryParam("userId") String sUserId) {
		
		WasdiLog.debugLog("DatasetResource.deleteCollaborator");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("DatasetResource.deleteCollaborator: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		try {
			
			WasdiLog.debugLog("DatasetResource.deleteCollaborator: dataset " + sUserId);

			// Create repo
			DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
			
			DatasetProject oDataset = oDatasetRepository.getDataset(sDatasetId);
			
			if (oDataset==null) {
				WasdiLog.warnLog("DatasetResource.deleteCollaborator: oDataset is null");
				return Response.status(Status.BAD_REQUEST).build();
			}			
			
			if ( !oDataset.getOwner().equals(oUser.getUserId())) {
				if (!oDataset.isPublic()) {
					WasdiLog.warnLog("DatasetResource.deleteCollaborator: user cannot access the dataset");
					return Response.status(Status.UNAUTHORIZED).build();					
				}
			}
			
			if (oUser.getUserId().equals(sUserId)) {
				WasdiLog.warnLog("DatasetResource.deleteCollaborator: target user is the owner, bad request");
				return Response.status(Status.BAD_REQUEST).build();		
			}
			
			boolean bExisting = false;
			
			for (String sAnnotator : oDataset.getAnnotators()) {
				if (sAnnotator.equals(sUserId)) {
					bExisting = true;
					oDataset.getAnnotators().remove(sUserId);
					break;
				}
			}
			
			if (!bExisting) {
				for (String sReviewer : oDataset.getReviewers()) {
					if (sReviewer.equals(sUserId)) {
						bExisting = true;
						oDataset.getAnnotators().remove(sUserId);
						break;
					}
				}				
			}
			
			if (!bExisting) {
				WasdiLog.warnLog("DatasetResource.deleteCollaborator: user is not part of the Dataset as collaborator");
				return Response.status(Status.BAD_REQUEST).build();				
			}
			
			return Response.ok().build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("DatasetResource.addCollaborator error: " + oEx);
			return Response.serverError().build();
		}
	}		
	
	
	private boolean isValidLabellingProjectRole(String sRole) {
		if (Utils.isNullOrEmpty(sRole)) {
			return false;
		}

		for (LabellingProjectRoles oRole : LabellingProjectRoles.values()) {
			if (oRole.name().equals(sRole)) {
				return true;
			}
		}

		return false;
	}
	
	

}
