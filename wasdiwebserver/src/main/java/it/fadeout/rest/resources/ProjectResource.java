package it.fadeout.rest.resources;

import static wasdi.shared.business.UserApplicationPermission.PROJECT_READ;

import java.util.ArrayList;
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
import wasdi.shared.business.User;
import wasdi.shared.business.UserApplicationRole;
import wasdi.shared.data.ProjectRepository;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.SuccessResponse;
import wasdi.shared.viewmodels.organizations.ProjectEditorViewModel;
import wasdi.shared.viewmodels.organizations.ProjectListViewModel;
import wasdi.shared.viewmodels.organizations.ProjectViewModel;
import wasdi.shared.viewmodels.organizations.SubscriptionListViewModel;

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
	public List<ProjectListViewModel> getListByUser(@HeaderParam("x-session-token") String sSessionId) {

		WasdiLog.debugLog("ProjectResource.getListByUser()");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		List<ProjectListViewModel> aoProjectList = new ArrayList<>();

		// Domain Check
		if (oUser == null) {
			WasdiLog.debugLog("ProjectResource.getListByUser: invalid session: " + sSessionId);
			return aoProjectList;
		}

		try {
			if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oUser.getRole(), PROJECT_READ)) {
				return aoProjectList;
			}

			WasdiLog.debugLog("ProjectResource.getListByUser: projects for " + oUser.getUserId());

			// Create repo
			ProjectRepository oProjectRepository = new ProjectRepository();


			List<SubscriptionListViewModel> aoSubscriptionLVMs = new SubscriptionResource().getListByUser(sSessionId);

			List<String> asSubscriptionIds = aoSubscriptionLVMs.stream()
					.map(SubscriptionListViewModel::getSubscriptionId)
					.collect(Collectors.toList());

			Map<String, String> aoSubscriptionNames = aoSubscriptionLVMs.stream()
				      .collect(Collectors.toMap(SubscriptionListViewModel::getSubscriptionId, SubscriptionListViewModel::getName));

			List<Project> aoProjects = oProjectRepository.getProjectsBySubscriptions(asSubscriptionIds);

			// For each
			for (Project oProject : aoProjects) {
				// Create View Model
				ProjectListViewModel oProjectViewModel = convert(oProject, aoSubscriptionNames.get(oProject.getSubscriptionId()), oUser.getActiveProjectId());

				aoProjectList.add(oProjectViewModel);
			}
		} catch (Exception oEx) {
			oEx.toString();
		}

		return aoProjectList;
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
	public List<ProjectListViewModel> getListBySubscription(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId) {
		WasdiLog.debugLog("ProjectResource.getListBySubscription(Subscription: " + sSubscriptionId + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		List<ProjectListViewModel> aoProjectList = new ArrayList<>();

		// Domain Check
		if (oUser == null) {
			WasdiLog.debugLog("ProjectResource.getListBySubscription: invalid session: " + sSessionId);
			return aoProjectList;
		}

		try {
			if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oUser.getRole(), PROJECT_READ)) {
				return aoProjectList;
			}

			WasdiLog.debugLog("ProjectResource.getListBySubscription: projects for " + oUser.getUserId());


			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

			Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);

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
		} catch (Exception oEx) {
			oEx.toString();
		}

		return aoProjectList;
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
			WasdiLog.debugLog("ProjectResource.getProjectViewModel: invalid session");
			return Response.status(400).entity(new ErrorResponse("Invalid session.")).build();
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sProjectId)) {
				return Response.status(400).entity(new ErrorResponse("Invalid projectId.")).build();
			}

			WasdiLog.debugLog("ProjectResource.getProjectViewModel: read projects " + sProjectId);

			// Create repo
			ProjectRepository oProjectRepository = new ProjectRepository();

			// Get requested project
			Project oProject = oProjectRepository.getProjectById(sProjectId);

			if (oProject.getSubscriptionId() == null) {
				WasdiLog.debugLog("ProjectResource.getProjectViewModel: the project is not connected with a subscription, aborting");
				return Response.status(400).entity(new ErrorResponse("The project is not connected with a subscription.")).build();
			}

			if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), oProject.getSubscriptionId())) {
				WasdiLog.debugLog("ProjectResource.getProjectViewModel: user cannot access project info, aborting");
				return Response.status(400).entity(new ErrorResponse("The user cannot access the project info.")).build();
			}

			oVM = convert(oProject, oUser.getActiveProjectId());

			return Response.ok(oVM).build();
		} catch (Exception oEx) {
			WasdiLog.debugLog( "ProjectResource.getProjectViewModel: " + oEx);
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
	public PrimitiveResult createProject(@HeaderParam("x-session-token") String sSessionId, ProjectEditorViewModel oProjectEditorViewModel) {
		WasdiLog.debugLog("ProjectResource.createProject( Project: " + oProjectEditorViewModel.toString() + ")");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("ProjectResource.createProject: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		ProjectRepository oProjectRepository = new ProjectRepository();

		Project oExistingProject = oProjectRepository.getByName(oProjectEditorViewModel.getName());

		if (oExistingProject != null) {
			WasdiLog.debugLog("ProjectResource.createProject: a different project with the same name already exists");
			oResult.setStringValue("An project with the same name already exists.");
			return oResult;
		}

		Project oProject = convert(oProjectEditorViewModel);
		oProject.setProjectId(Utils.getRandomName());

		if (oProjectRepository.insertProject(oProject)) {
			oResult.setBoolValue(true);
			oResult.setStringValue(oProject.getProjectId());

			if (oProjectEditorViewModel.isActiveProject()) {
				this.changeActiveProject(sSessionId, oProject.getProjectId());
			}
		} else {WasdiLog.debugLog("ProjectResource.createProject( " + oProjectEditorViewModel.getName() + " ): insertion failed");
			oResult.setStringValue("The creation of the project failed.");
		}

		return oResult;
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
	public PrimitiveResult upateProject(@HeaderParam("x-session-token") String sSessionId, ProjectEditorViewModel oProjectEditorViewModel) {
		WasdiLog.debugLog("ProjectResource.updateProject( Project: " + oProjectEditorViewModel.toString() + ")");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("ProjectResource.updateProject: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		ProjectRepository oProjectRepository = new ProjectRepository();

		Project oExistingProject = oProjectRepository.getProjectById(oProjectEditorViewModel.getProjectId());

		if (oExistingProject == null) {
			WasdiLog.debugLog("ProjectResource.updateProject: project does not exist");
			oResult.setStringValue("No project with the Id exists.");
			return oResult;
		}

		Project oExistingProjectWithTheSameName = oProjectRepository.getByName(oProjectEditorViewModel.getName());

		if (oExistingProjectWithTheSameName != null
				&& !oExistingProjectWithTheSameName.getProjectId().equalsIgnoreCase(oExistingProject.getProjectId())) {
			WasdiLog.debugLog("ProjectResource.updateProject: a different project with the same name already exists");
			oResult.setStringValue("An project with the same name already exists.");
			return oResult;
		}



		Project oProject = convert(oProjectEditorViewModel);

		if (oProjectRepository.updateProject(oProject)) {
			oResult.setBoolValue(true);
			oResult.setStringValue(oProject.getProjectId());

			if (oProjectEditorViewModel.isActiveProject()) {
				this.changeActiveProject(sSessionId, oProject.getProjectId());
			} else if (oProject.getProjectId().equals(oUser.getActiveProjectId())) {
				UserRepository oUserRepository = new UserRepository();

				oUser.setActiveProjectId(null);

				if (oUserRepository.updateUser(oUser)) {
					WasdiLog.debugLog("ProjectResource.updateProject( " + "changing the active project of the user to null failed");
					oResult.setStringValue("The removing of the active project failed.");
				}
			}
		} else {
			WasdiLog.debugLog("ProjectResource.updateProject( " + oProjectEditorViewModel.getName() + " ): update failed");
			oResult.setStringValue("The update of the project failed.");
		}

		return oResult;
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
	public PrimitiveResult changeActiveProject(@HeaderParam("x-session-token") String sSessionId, @QueryParam("project") String sProjectId) {
		WasdiLog.debugLog("ProjectResource.changeActiveProject( ProjectId: " + sProjectId + ")");

		if (sProjectId != null) {
			if (sProjectId.isEmpty() || sProjectId.equals("null")) {
				sProjectId = null;
			}
		}

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("ProjectResource.changeActiveProject: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		if (sProjectId != null) {
			ProjectRepository oProjectRepository = new ProjectRepository();

			Project oProject = oProjectRepository.getProjectById(sProjectId);

			if (oProject == null) {
				WasdiLog.debugLog("ProjectResource.changeActiveProject: project does not exist");
				oResult.setStringValue("No project with the Id " + sProjectId + " exists.");
				return oResult;
			}
		}

		UserRepository oUserRepository = new UserRepository();

		oUser.setActiveProjectId(sProjectId);

		if (oUserRepository.updateUser(oUser)) {
			oResult.setBoolValue(true);
			oResult.setStringValue(sProjectId);
		} else {
			WasdiLog.debugLog("ProjectResource.changeActiveProject( " + "changing the active project of the user to " + sProjectId + " failed");
			oResult.setStringValue("The changing of the active project failed.");
		}

		return oResult;
	}

	@DELETE
	@Path("/delete")
	public Response deleteProject(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("project") String sProjectId) {
		WasdiLog.debugLog("ProjectResource.deleteProject( Project: " + sProjectId + " )");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("ProjectResource.deleteProject: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse("Invalid session.")).build();
		}

		if (Utils.isNullOrEmpty(sProjectId)) {
			return Response.status(400).entity(new ErrorResponse("Invalid projectId.")).build();
		}

		ProjectRepository oProjectRepository = new ProjectRepository();

		Project oProject = oProjectRepository.getProjectById(sProjectId);

		if (oProject == null) {
			WasdiLog.debugLog("ProjectResource.deleteProject: project does not exist");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("No project with the name exists.")).build();
		}

		if (oProjectRepository.deleteProject(sProjectId)) {
			return Response.ok(new SuccessResponse(sProjectId)).build();
		} else {
			WasdiLog.debugLog("ProjectResource.deleteProject( " + sProjectId + " ): deletion failed");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The deletion of the project failed.")).build();
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
