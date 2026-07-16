package it.fadeout.rest.resources.labelling;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.joda.time.DateTimeUtils;

import it.fadeout.Wasdi;
import it.fadeout.rest.resources.WorkspaceResource;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.labelling.Attribute;
import wasdi.shared.business.labelling.DatasetProject;
import wasdi.shared.business.labelling.LabellingProjectRoles;
import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserAccessRights;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.business.labelling.Label;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.data.labelling.DatasetProjectRepository;
import wasdi.shared.data.labelling.LabelRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.labelling.datasets.DatasetCollaboratorViewModel;
import wasdi.shared.viewmodels.labelling.datasets.DatasetListViewModel;
import wasdi.shared.viewmodels.labelling.datasets.DatasetViewModel;
import wasdi.shared.viewmodels.labelling.datasets.ExportDatasetViewModel;

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
				} else if (oDataset.getReviewers() != null && oDataset.getReviewers().contains(oUser.getUserId())) {
					oDatasetListViewModel.userRole = "REVIEWER";
				} else if (oDataset.getAnnotators() != null && oDataset.getAnnotators().contains(oUser.getUserId())) {
					oDatasetListViewModel.userRole = "ANNOTATOR";
				} else {
					oDatasetListViewModel.userRole = "GUEST";
				}
				oDatasetListViewModel.bbox = oDataset.getBbox();
				oDatasetListViewModel.description = oDataset.getDescription();
				oDatasetListViewModel.isGlobal = oDataset.isGlobal();
				oDatasetListViewModel.mission = oDataset.getMissions();
				oDatasetListViewModel.workspaceId = oDataset.getWorkspaceId();
				oDatasetListViewModel.templateId = oDataset.getTemplateId();
				
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
	
	@DELETE
	@Path("/leave")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response leaveProject(@HeaderParam("x-session-token") String sSessionId, @QueryParam("datasetId") String sDatasetId) {
		
		WasdiLog.debugLog("DatasetResource.leaveProject");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// 1. Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("DatasetResource.leaveProject: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		try {
			DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
			DatasetProject oDataset = oDatasetRepository.getDataset(sDatasetId);
			
			if (oDataset == null) {
				WasdiLog.warnLog("DatasetResource.leaveProject: dataset not found");
				return Response.status(Status.BAD_REQUEST).build();
			}			
			
			// 2. Prevent the Owner from leaving (they must delete the project instead)
			if (oDataset.getOwner().equals(oUser.getUserId())) {
				WasdiLog.warnLog("DatasetResource.leaveProject: Owner cannot leave the project.");
				return Response.status(Status.BAD_REQUEST).build();		
			}
			
			boolean bRemoved = false;
			
			// 3. Find them and remove them!
			if (oDataset.getAnnotators() != null && oDataset.getAnnotators().contains(oUser.getUserId())) {
				oDataset.getAnnotators().remove(oUser.getUserId());
				bRemoved = true;
			}
			
			if (oDataset.getReviewers() != null && oDataset.getReviewers().contains(oUser.getUserId())) {
				oDataset.getReviewers().remove(oUser.getUserId());
				bRemoved = true;
			}
			
			if (!bRemoved) {
				WasdiLog.warnLog("DatasetResource.leaveProject: user is not part of the Dataset");
				return Response.status(Status.BAD_REQUEST).build();				
			}
			
			// 4. Save the changes to MongoDB
			oDatasetRepository.updateDataset(oDataset);
			
			return Response.ok().build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("DatasetResource.leaveProject error: " + oEx);
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

			// Validate Destination User exists
			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);
			if (oDestinationUser == null) {
				WasdiLog.warnLog("DatasetResource.addCollaborator: Destination user does not exist");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Target user does not exist")).build();
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

			// ── THE FIX: Add the user to the linked workspace ──
			String sWorkspaceId = oDataset.getWorkspaceId();
			
			if (!Utils.isNullOrEmpty(sWorkspaceId)) {
				WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
				Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
				
				if (oWorkspace != null) {
					UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

					// Check if the workspace is already shared with this user
					if (!oUserResourcePermissionRepository.isWorkspaceSharedWithUser(sUserId, sWorkspaceId)) {
						
						// Grant READ access to the underlying imagery
						String sRights = UserAccessRights.READ.getAccessRight();
						
						UserResourcePermission oWorkspaceSharing = new UserResourcePermission(
								ResourceTypes.WORKSPACE.getResourceType(), 
								sWorkspaceId, 
								sUserId, 
								oWorkspace.getUserId(), 
								oUser.getUserId(), 
								sRights
						);

						oUserResourcePermissionRepository.insertPermission(oWorkspaceSharing);	
						WasdiLog.debugLog("DatasetResource.addCollaborator: Automatically shared workspace " + sWorkspaceId + " with user " + sUserId);
					} else {
						WasdiLog.debugLog("DatasetResource.addCollaborator: Workspace already shared with user " + sUserId);
					}
				}
			}
			
			oDatasetRepository.updateDataset(oDataset);
			
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
						oDataset.getReviewers().remove(sUserId);
						break;
					}
				}				
			}
			
			if (!bExisting) {
				WasdiLog.warnLog("DatasetResource.deleteCollaborator: user is not part of the Dataset as collaborator");
				return Response.status(Status.BAD_REQUEST).build();				
			}
			oDatasetRepository.updateDataset(oDataset);
			
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
	
	@POST
	@Path("/export")
	@Produces("application/zip")
	public Response exportDataset(@HeaderParam("x-session-token") String sSessionId, ExportDatasetViewModel oExportViewModel) {
		
		WasdiLog.debugLog("DatasetResource.exportDataset");
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		try {
			DatasetProjectRepository oDatasetRepository = new DatasetProjectRepository();
			DatasetProject oDataset = oDatasetRepository.getDataset(oExportViewModel.projectId);
			
			if (oDataset == null) {
				return Response.status(Status.NOT_FOUND).build();
			}			
			
			LabelRepository oLabelRepo = new LabelRepository();
			List<Label> aoAllLabels = oLabelRepo.getLabelsByDataset(oDataset.getId());
			
			if (aoAllLabels == null || aoAllLabels.isEmpty()) {
				return Response.status(Status.NOT_FOUND).entity("No labels found").build();
			}

			// ── THE MISSING PIECE: Filter out unvalidated labels! ──
			if ("validated".equalsIgnoreCase(oExportViewModel.labelFilter)) {
				aoAllLabels = aoAllLabels.stream()
						.filter(Label::isValidated) // Or .getIsValidated() depending on your getter
						.collect(Collectors.toList());
				
				if (aoAllLabels.isEmpty()) {
					return Response.status(Status.NOT_FOUND).entity("No validated labels found for this project.").build();
				}
			}

			// 1. Group labels by Geometry Type (Just like your Python script!)
			Map<String, List<Label>> dictGDFs = new HashMap<>();
			dictGDFs.put("Points", new ArrayList<>());
			dictGDFs.put("Lines", new ArrayList<>());
			dictGDFs.put("Polygons", new ArrayList<>());

			for (Label lbl : aoAllLabels) {
				if (lbl.isPoint()) dictGDFs.get("Points").add(lbl);
				else if (lbl.isLine()) dictGDFs.get("Lines").add(lbl);
				else if (lbl.isPolygon() || lbl.isMultiPolygon()) dictGDFs.get("Polygons").add(lbl);
			}

			// 2. Create the Streaming ZIP Output
			StreamingOutput oStream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException, WebApplicationException {
					try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(output)) {
						
						// ── A. EXPORT SHAPEFILES ──
						java.nio.file.Path oTempDirPath = java.nio.file.Files.createTempDirectory("wasdi_shape_export_");
						java.io.File oTempDir = oTempDirPath.toFile();

						try {
							org.geotools.data.shapefile.ShapefileDataStoreFactory dataStoreFactory = new org.geotools.data.shapefile.ShapefileDataStoreFactory();
							org.geotools.geojson.geom.GeometryJSON gjson = new org.geotools.geojson.geom.GeometryJSON();

							for (Map.Entry<String, List<Label>> entry : dictGDFs.entrySet()) {
								String sGeomType = entry.getKey();
								List<Label> aoTypeLabels = entry.getValue();
								
								if (aoTypeLabels.isEmpty()) continue;

								// 1. Dynamically build the GeoTools Schema String
								String sGeomClass = sGeomType.equals("Points") ? "Point" : (sGeomType.equals("Lines") ? "LineString" : "Polygon");
								StringBuilder sSchemaBuilder = new StringBuilder("the_geom:" + sGeomClass + ":srid=4326,lblId:String,annotator:String,validated:Boolean");
								
								// Check the first label to extract dynamic template attributes safely!
								Label oFirstLabel = aoTypeLabels.get(0);
								List<String> aoSafeDynamicKeys = new ArrayList<>();
								
								if (oFirstLabel.getAttributes() != null) {
									for (Attribute attr : oFirstLabel.getAttributes()) {
										// YOUR PYTHON FIX TRANSLATED TO JAVA: Truncate to 10 chars!
										String sSafeKey = attr.getName().length() > 10 ? attr.getName().substring(0, 10) : attr.getName();
										
										// GeoTools needs to know the data type
										// Convert the type enum to a string safely
										String sTypeString = (attr.getType() != null) ? attr.getType().name() : "STRING";
										
										// GeoTools needs to know the data type
										String sDataType = sTypeString.equalsIgnoreCase("INTEGER") ? "Integer" : 
														  (sTypeString.equalsIgnoreCase("FLOAT") ? "Double" : "String"); 
										sSchemaBuilder.append(",").append(sSafeKey).append(":").append(sDataType);
										aoSafeDynamicKeys.add(attr.getName()); // Keep track of original name to fetch value later
									}
								}

								// 2. Create the GeoTools Feature Builder
								String sFileName = oDataset.getName().replaceAll(" ", "_") + "_" + sGeomType;
								org.opengis.feature.simple.SimpleFeatureType sFeatureType = org.geotools.data.DataUtilities.createType(sFileName, sSchemaBuilder.toString());
								org.geotools.feature.simple.SimpleFeatureBuilder featureBuilder = new org.geotools.feature.simple.SimpleFeatureBuilder(sFeatureType);
								org.geotools.feature.DefaultFeatureCollection collection = new org.geotools.feature.DefaultFeatureCollection();

								// 3. Populate Features
								// 3. Populate Features
								for (Label oLabel : aoTypeLabels) {
									try {
										// ── THE FIX: Wrap the String in a StringReader ──
										java.io.Reader stringReader = new java.io.StringReader(oLabel.getGeometry());
										org.locationtech.jts.geom.Geometry geometry = gjson.read(stringReader);
										
										featureBuilder.add(geometry);
										featureBuilder.add(oLabel.getId());
										featureBuilder.add(oLabel.getAnnotator());
										featureBuilder.add(oLabel.isValidated());
										
										// Append dynamic attributes in the exact order we defined them
										if (oLabel.getAttributes() != null) {
											for (String originalKey : aoSafeDynamicKeys) {
												Object val = oLabel.getAttributes().stream()
													.filter(a -> a.getName().equals(originalKey))
													.map(Attribute::getValue).findFirst().orElse("");
												featureBuilder.add(val);
											}
										}
										
										collection.add(featureBuilder.buildFeature(null));
									} catch (Exception ex) {
										WasdiLog.warnLog("Skipping invalid geometry: " + ex.getMessage());
									}
								}


								// 4. Write to temp Shapefile
								java.io.File oShapefile = new java.io.File(oTempDir, sFileName + ".shp");
								Map<String, java.io.Serializable> params = new java.util.HashMap<>();
								params.put("url", oShapefile.toURI().toURL());
								params.put("create spatial index", Boolean.TRUE);

								org.geotools.data.shapefile.ShapefileDataStore newDataStore = (org.geotools.data.shapefile.ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
								newDataStore.createSchema(sFeatureType);

								// ── THE FIX: Pull featureStore OUTSIDE the try() parentheses ──
								org.geotools.data.simple.SimpleFeatureStore featureStore = 
									(org.geotools.data.simple.SimpleFeatureStore) newDataStore.getFeatureSource(newDataStore.getTypeNames()[0]);

								// ── ONLY the transaction goes INSIDE the try() parentheses ──
								try (org.geotools.data.Transaction transaction = new org.geotools.data.DefaultTransaction("create")) {
									featureStore.setTransaction(transaction);
									featureStore.addFeatures(collection);
									transaction.commit();
								}
								
								newDataStore.dispose();

								// 5. Copy all 4 shapefile parts (.shp, .shx, .dbf, .prj) into the ZIP
								java.io.File[] aoGeneratedFiles = oTempDir.listFiles((dir, name) -> name.startsWith(sFileName));
								if (aoGeneratedFiles != null) {
									for (java.io.File oGenFile : aoGeneratedFiles) {
										zos.putNextEntry(new java.util.zip.ZipEntry("labels/" + sGeomType + "/" + oGenFile.getName()));
										java.nio.file.Files.copy(oGenFile.toPath(), zos);
										zos.closeEntry();
									}
								}
							}
						} finally {
							// CLEANUP TEMP DIR
							java.io.File[] aoFilesToDelete = oTempDir.listFiles();
							if (aoFilesToDelete != null) {
								for (java.io.File f : aoFilesToDelete) f.delete();
							}
							oTempDir.delete();
						}

						// ── B. EXPORT RAW IMAGERY ──
						if (oExportViewModel.includeRawData) {
							/*ProductRepository oProductRepo = new ProductRepository();
							List<Product> aoImages = oProductRepo.getProductsByWorkspace(oDataset.getWorkspaceId());
							
							for (Product oImg : aoImages) {
								java.io.File oPhysicalFile = new java.io.File(oImg.getFilePath()); // Or however your Java backend resolves file paths
								if (oPhysicalFile.exists()) {
									zos.putNextEntry(new java.util.zip.ZipEntry("raw_images/" + oImg.getFileName()));
									java.nio.file.Files.copy(oPhysicalFile.toPath(), zos);
									zos.closeEntry();
								}
							}*/
						}

					} catch (Exception e) {
						WasdiLog.errorLog("Error writing zip stream: " + e.getMessage());
						throw new WebApplicationException("Error generating zip", e);
					}
				}
			};

			String sSafeName = oDataset.getName().replaceAll("[^a-zA-Z0-9_-]", "_");
			return Response.ok(oStream)
					.type("application/zip")
					.header("Content-Disposition", "attachment; filename=\"ComapVeda_Export_" + sSafeName + ".zip\"")
					.build();

		} catch (Exception oEx) {
			WasdiLog.errorLog("DatasetResource.exportDataset error: " + oEx);
			return Response.serverError().build();
		}
	}
	
	

}
