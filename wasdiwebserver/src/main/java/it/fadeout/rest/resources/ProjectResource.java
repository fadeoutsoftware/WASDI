package it.fadeout.rest.resources;

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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import wasdi.shared.business.Project;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.data.ProjectRepository;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.SuccessResponse;
import wasdi.shared.viewmodels.organizations.ProjectEditorViewModel;
import wasdi.shared.viewmodels.organizations.ProjectListViewModel;
import wasdi.shared.viewmodels.organizations.ProjectViewModel;
import wasdi.shared.viewmodels.organizations.SubscriptionListViewModel;


/**
 * Projects Resource.
 * 	Hosts the api to let the user create new projects and select the active one.
 * 		.create edit and delete projects
 * 		.set active projects
 * 		.read projects
 * 
 * @author p.campanella
 *
 */
@Path("/projects")
public class ProjectResource {
	/**
	 * Get the list of projects associated to a user.
	 * @param sSessionId User Session Id
	 * @return a list of Project View Models
	 */
	@GET
	@Path("/byuser")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getListByUser(@HeaderParam("x-session-token") String sSessionId, @QueryParam("valid") Boolean bValid) {
		
		if (bValid == null) bValid = false;

		WasdiLog.debugLog("ProjectResource.getListByUser");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		List<ProjectListViewModel> aoProjectList = new ArrayList<>();

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("ProjectResource.getListByUser: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		try {
			
			WasdiLog.debugLog("ProjectResource.getListByUser: projects for " + oUser.getUserId());

			// Create repo
			ProjectRepository oProjectRepository = new ProjectRepository();

			Response oResponse = new SubscriptionResource().getListByUser(sSessionId, bValid);

			@SuppressWarnings("unchecked")
			List<SubscriptionListViewModel> aoSubscriptionLVMs = (List<SubscriptionListViewModel>) oResponse.getEntity();
			
			if (aoSubscriptionLVMs==null) {
				WasdiLog.debugLog("ProjectResource.getListByUser: aoSubscriptionLVMs is null");
				return Response.ok(aoProjectList).build();
			}

			List<String> asSubscriptionIds = aoSubscriptionLVMs.stream()
					.map(SubscriptionListViewModel::getSubscriptionId)
					.collect(Collectors.toList());

			if (asSubscriptionIds==null) {
				WasdiLog.debugLog("ProjectResource.getListByUser: asSubscriptionIds is null");
				return Response.ok(aoProjectList).build();
			}
			
			Map<String, String> aoSubscriptionNames = new HashMap<String, String>();
			
			for (SubscriptionListViewModel oVMToAdd : aoSubscriptionLVMs) {
				if (!aoSubscriptionNames.containsKey(oVMToAdd.getSubscriptionId())) {
					aoSubscriptionNames.put(oVMToAdd.getSubscriptionId(), oVMToAdd.getName());
				}
			}

			List<Project> aoProjects = oProjectRepository.getProjectsBySubscriptions(asSubscriptionIds);
			
			if (aoProjects==null) {
				WasdiLog.debugLog("ProjectResource.getListByUser: aoProjects is null");
				return Response.ok(aoProjectList).build();
			}			
			
			boolean bFoundActiveProject = false;

			// For each
			for (Project oProject : aoProjects) {
				// Create View Model
				ProjectListViewModel oProjectViewModel = convert(oProject, aoSubscriptionNames.get(oProject.getSubscriptionId()), oUser.getActiveProjectId());
				aoProjectList.add(oProjectViewModel);
				
				if (oProjectViewModel.isActiveProject()) {
					bFoundActiveProject = true;
				}
			}
			
			// Force a default project if available and none is selected
			if (!bFoundActiveProject && aoProjectList.size()>0) {
				aoProjectList.get(0).setActiveProject(true);
			}

			return Response.ok(aoProjectList).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectResource.getListByUser error: " + oEx);
			return Response.serverError().build();
		}
	}

	/**
	 * Get the list of projects associated to a subscription.
	 * @param sSessionId User Session Id
	 * @param sSubscriptionId the subscription Id
	 * @return a list of Project View Models
	 */
	@GET
	@Path("/bysubscription")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getListBySubscription(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId) {
		WasdiLog.debugLog("ProjectResource.getListBySubscription(Subscription: " + sSubscriptionId + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("ProjectResource.getListBySubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
		Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);
		
		if (oSubscription == null) {
			WasdiLog.warnLog("ProjectResource.getListBySubscription: invalid subscription");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_CANNOT_ACCESS.name())).build();						
		}		
		
		if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), sSubscriptionId)) {
			WasdiLog.warnLog("ProjectResource.getListBySubscription: user cannot access the subscription");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_CANNOT_ACCESS.name())).build();			
		}

		try {
			
			List<ProjectListViewModel> aoProjectList = new ArrayList<>();

			String sSubscriptionName = null;

			if (oSubscription != null && oSubscription.getOrganizationId() != null) {
				sSubscriptionName = oSubscription.getName();
			}

			// Create repo
			ProjectRepository oProjectRepository = new ProjectRepository();

			List<Project> aoProjects = oProjectRepository.getProjectsBySubscription(sSubscriptionId);

			// For each
			for (Project oProject : aoProjects) {
				// Create View Model
				ProjectListViewModel oProjectViewModel = convert(oProject, sSubscriptionName, oUser.getActiveProjectId());

				aoProjectList.add(oProjectViewModel);
			}

			return Response.ok(aoProjectList).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProjectResource.getListBySubscription error: " + oEx);
			return Response.serverError().build();
		}
	}

	/**
	 * Get an project by its Id.
	 * @param sSessionId User Session Id
	 * @param sProjectId the project Id
	 * @return the full view model of the project
	 */
	@GET
	@Path("/byId")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getProjectViewModel(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("project") String sProjectId) {
		WasdiLog.debugLog("ProjectResource.getProjectViewModel( Project: " + sProjectId + ")");

		ProjectViewModel oVM = new ProjectViewModel();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("ProjectResource.getProjectViewModel: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		// Domain Check
		if (Utils.isNullOrEmpty(sProjectId)) {
			WasdiLog.warnLog("ProjectResource.getProjectViewModel: invalid project id");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid projectId.")).build();
		}

		try {
			// Create repo
			ProjectRepository oProjectRepository = new ProjectRepository();

			// Get requested project
			Project oProject = oProjectRepository.getProjectById(sProjectId);

			if (oProject.getSubscriptionId() == null) {
				WasdiLog.debugLog("ProjectResource.getProjectViewModel: the project is not connected with a subscription, aborting");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The project is not connected with a subscription.")).build();
			}

			if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), oProject.getSubscriptionId())) {
				WasdiLog.debugLog("ProjectResource.getProjectViewModel: user cannot access project info, aborting");
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse("The user cannot access the project info.")).build();
			}

			oVM = convert(oProject, oUser.getActiveProjectId());

			return Response.ok(oVM).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog( "ProjectResource.getProjectViewModel error: " + oEx);
			return Response.serverError().build();
		}
	}

	/**
	 * Create a new Project.
	 * @param sSessionId User Session Id
	 * @param oProjectViewModel the project to be created
	 * @return a primitive result containing the outcome of the operation
	 */
	@POST
	@Path("/add")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response createProject(@HeaderParam("x-session-token") String sSessionId, ProjectEditorViewModel oProjectEditorViewModel) {
		
		WasdiLog.debugLog("ProjectResource.createProject( Project: " + oProjectEditorViewModel.toString() + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("ProjectResource.createProject: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
	
		try {
			
			if (!PermissionsUtils.canUserWriteSubscription(oUser.getUserId(), oProjectEditorViewModel.getSubscriptionId())) {
				WasdiLog.warnLog("ProjectResource.createProject: user cannot write the subscription");
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_CANNOT_ACCESS.name())).build();				
			}
			
			ProjectRepository oProjectRepository = new ProjectRepository();

			String sName = oProjectEditorViewModel.getName();
			
			while (oProjectRepository.getByName(sName) != null) {
				sName = Utils.cloneName(sName);
				WasdiLog.debugLog("ProjectResource.createProject: a Project with the same name already exists. Changing the name to " + sName);
			}

			Project oProject = convert(oProjectEditorViewModel);
			oProject.setProjectId(Utils.getRandomName());

			if (oProjectRepository.insertProject(oProject)) {
				if (oProjectEditorViewModel.isActiveProject()) {
					this.changeActiveProject(sSessionId, oProject.getProjectId(), oProjectEditorViewModel.getTargetUser());
				}

				return Response.ok(new SuccessResponse(oProject.getProjectId())).build();
			} 
			else 
			{
				WasdiLog.debugLog("ProjectResource.createProject( " + oProjectEditorViewModel.getName() + " ): insertion failed");
				return Response.serverError().build();
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProjectResource.createProject: error ", oEx);
			return Response.serverError().build();
		}
	}

	/**
	 * Update an Project.
	 * @param sSessionId User Session Id
	 * @param oProjectViewModel the project to be updated
	 * @return a primitive result containing the outcome of the operation
	 */
	@PUT
	@Path("/update")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response upateProject(@HeaderParam("x-session-token") String sSessionId, ProjectEditorViewModel oProjectEditorViewModel) {
		WasdiLog.debugLog("ProjectResource.updateProject( Project: " + oProjectEditorViewModel.toString() + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("ProjectResource.updateProject: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		try {
			ProjectRepository oProjectRepository = new ProjectRepository();

			Project oExistingProject = oProjectRepository.getProjectById(oProjectEditorViewModel.getProjectId());

			if (oExistingProject == null) {
				WasdiLog.warnLog("ProjectResource.updateProject: project does not exist");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("No project with the Id exists.")).build();
			}
			
			if (!PermissionsUtils.canUserWriteSubscription(oUser.getUserId(), oExistingProject.getSubscriptionId())) {
				WasdiLog.warnLog("ProjectResource.updateProject: user cannot write subscription ");
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_CANNOT_ACCESS.name())).build();			
			}

			Project oExistingProjectWithTheSameName = oProjectRepository.getByName(oProjectEditorViewModel.getName());

			if (oExistingProjectWithTheSameName != null
					&& !oExistingProjectWithTheSameName.getProjectId().equalsIgnoreCase(oExistingProject.getProjectId())) {
				WasdiLog.warnLog("ProjectResource.updateProject: a different project with the same name already exists");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("An project with the same name already exists.")).build();
			}

			Project oProject = convert(oProjectEditorViewModel);
			
			SubscriptionRepository oSubRepo = new SubscriptionRepository();
			Subscription oSubscription = oSubRepo.getSubscriptionById(oProject.getSubscriptionId());
			
			if (oSubscription == null) {
				WasdiLog.warnLog("ProjectResource.updateProject: related subscription does not exist");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("No parent subscirption exists.")).build();				
			}
			
			if (!oSubscription.getUserId().equals(oUser.getUserId())) {
				// The request come from a user that is not the owner: but the owner is the target of the change project!
				oProjectEditorViewModel.setTargetUser(oSubscription.getUserId());
			}

			if (oProjectRepository.updateProject(oProject)) {

				if (oProjectEditorViewModel.isActiveProject()) {
					this.changeActiveProject(sSessionId, oProject.getProjectId(), oProjectEditorViewModel.getTargetUser());
				} 
				else if (oProject.getProjectId().equals(oUser.getActiveProjectId())) {
					UserRepository oUserRepository = new UserRepository();

					oUser.setActiveProjectId(null);
					oUser.setActiveSubscriptionId(null);

					if (!oUserRepository.updateUser(oUser)) {
						WasdiLog.warnLog("ProjectResource.updateProject( " + "changing the active project of the user to null failed");
						return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The removing of the active project failed.")).build();
					}
				}

				return Response.ok(new SuccessResponse(oProject.getProjectId())).build();
			} else {
				WasdiLog.debugLog("ProjectResource.updateProject( " + oProjectEditorViewModel.getName() + " ): update failed");
				return Response.serverError().build();
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProjectResource.updateProject: error ", oEx);
			return Response.serverError().build();
		}		
	}

	/**
	 * Update an Project.
	 * @param sSessionId User Session Id
	 * @param oProjectViewModel the project to be updated
	 * @return a primitive result containing the outcome of the operation
	 */
	@PUT
	@Path("/active")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response changeActiveProject(@HeaderParam("x-session-token") String sSessionId, @QueryParam("project") String sProjectId, @QueryParam("target") String sTargetUserId) {
		WasdiLog.debugLog("ProjectResource.changeActiveProject( ProjectId: " + sProjectId + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("ProjectResource.changeActiveProject: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		try {
			String sSubscriptionId = null;
			UserRepository oUserRepository = new UserRepository();
			
			if (Utils.isNullOrEmpty(sTargetUserId)) sTargetUserId = oUser.getUserId();
			
			if (!oUser.getUserId().equals(sTargetUserId)) {
				if (!UserApplicationRole.isAdmin(oUser.getUserId())) {
					WasdiLog.warnLog("ProjectResource.changeActiveProject: requested a change for a target user by a not-admin user. We stop here");
					return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();					
				}
				
				oUser = oUserRepository.getUser(sTargetUserId);
				WasdiLog.debugLog("ProjectResource.changeActiveProject: changed the user to the target user");
			}
			

			if (sProjectId != null) {
				ProjectRepository oProjectRepository = new ProjectRepository();

				Project oProject = oProjectRepository.getProjectById(sProjectId);

				if (oProject == null) {
					WasdiLog.warnLog("ProjectResource.changeActiveProject: project does not exist");
					return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("No project with the Id " + sProjectId + " exists.")).build();
				}

				sSubscriptionId = oProject.getSubscriptionId();

				if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), sSubscriptionId)) {
					WasdiLog.warnLog("ProjectResource.changeActiveProject: user cannot access the subscription");
					return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_CANNOT_ACCESS.name())).build();				
				}
			}

			oUser.setActiveSubscriptionId(sSubscriptionId);
			oUser.setActiveProjectId(sProjectId);

			if (oUserRepository.updateUser(oUser)) {
				WasdiLog.warnLog("ProjectResource.changeActiveProject: active project changed to " + sProjectId + " for user " + oUser.getUserId());
				return Response.ok(new SuccessResponse(sProjectId)).build();
			} 
			else {
				WasdiLog.debugLog("ProjectResource.changeActiveProject( " + "changing the active project of the user to " + sProjectId + " failed");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The changing of the active project failed.")).build();
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProjectResource.changeActiveProject: error ", oEx);
			return Response.serverError().build();
		}		
	}

	@DELETE
	@Path("/delete")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response deleteProject(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("project") String sProjectId) {
		WasdiLog.debugLog("ProjectResource.deleteProject( Project: " + sProjectId + " )");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("ProjectResource.deleteProject: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (Utils.isNullOrEmpty(sProjectId)) {
			WasdiLog.warnLog("ProjectResource.deleteProject: invalid project id");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid projectId.")).build();
		}
		
		try {
			ProjectRepository oProjectRepository = new ProjectRepository();

			Project oProject = oProjectRepository.getProjectById(sProjectId);

			if (oProject == null) {
				WasdiLog.warnLog("ProjectResource.deleteProject: project does not exist");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("No project with the name exists.")).build();
			}
			
			if (!PermissionsUtils.canUserWriteSubscription(oUser.getUserId(), sProjectId)) {
				WasdiLog.warnLog("ProjectResource.deleteProject: user cannot write subscription");
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_CANNOT_ACCESS.name())).build();				
			}

			if (oProjectRepository.deleteProject(sProjectId)) {
				return Response.ok(new SuccessResponse(sProjectId)).build();
			} 
			else {
				WasdiLog.debugLog("ProjectResource.deleteProject: deletion failed");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The deletion of the project failed.")).build();
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProjectResource.deleteProject: error ", oEx);
			return Response.serverError().build();
		}	
	}

	private static Project convert(ProjectEditorViewModel oProjectEVM) {
		Project oProject = new Project();
		oProject.setProjectId(oProjectEVM.getProjectId());
		oProject.setSubscriptionId(oProjectEVM.getSubscriptionId());
		oProject.setName(oProjectEVM.getName());
		oProject.setDescription(oProjectEVM.getDescription());

		return oProject;
	}

	private static ProjectListViewModel convert(Project oProject, String sSubscriptionName, String sActiveProjectId) {
		ProjectListViewModel oProjectListViewModel = new ProjectListViewModel();
		oProjectListViewModel.setProjectId(oProject.getProjectId());
//		oProjectListViewModel.setSubscriptionId(oProject.getSubscriptionId());
		oProjectListViewModel.setSubscriptionName(sSubscriptionName);
		oProjectListViewModel.setName(oProject.getName());
		oProjectListViewModel.setDescription(oProject.getDescription());

		if (oProjectListViewModel.getProjectId() != null && sActiveProjectId != null && oProjectListViewModel.getProjectId().equals(sActiveProjectId)) {
			oProjectListViewModel.setActiveProject(true);
		}

		return oProjectListViewModel;
	}

	private static ProjectViewModel convert(Project oProject, String sActiveProjectId) {
		ProjectViewModel oProjectViewModel = new ProjectViewModel();
		oProjectViewModel.setProjectId(oProject.getProjectId());
		oProjectViewModel.setSubscriptionId(oProject.getSubscriptionId());
		oProjectViewModel.setName(oProject.getName());
		oProjectViewModel.setDescription(oProject.getDescription());

		if (oProjectViewModel.getProjectId() != null && sActiveProjectId != null && oProjectViewModel.getProjectId().equals(sActiveProjectId)) {
			oProjectViewModel.setActiveProject(true);
		}

		return oProjectViewModel;
	}

}
