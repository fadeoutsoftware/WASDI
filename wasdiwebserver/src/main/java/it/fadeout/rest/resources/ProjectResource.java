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

import it.fadeout.Wasdi;
import wasdi.shared.business.Project;
import wasdi.shared.business.User;
import wasdi.shared.business.UserApplicationRole;
import wasdi.shared.data.ProjectRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.organizations.ProjectEditorViewModel;
import wasdi.shared.viewmodels.organizations.ProjectListViewModel;
import wasdi.shared.viewmodels.organizations.ProjectViewModel;
import wasdi.shared.viewmodels.organizations.SubscriptionListViewModel;

@Path("/projects")
public class ProjectResource {

	/**
	 * Get the list of projects associated to a user.
	 * @param sSessionId User Session Id
	 * @return a View Model with the Project Name and 
	 * 	a flag to know if the user is admin or not of the project
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

		System.out.println(oUser.getUserId() + "'s active project is :" + oUser.getActiveProjectId());

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
	 * Get an project by its Id.
	 * @param sSessionId User Session Id
	 * @param sProjectId the project Id
	 * @return the full view model of the project
	 */
	@GET
	@Path("/byId")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public ProjectViewModel getProjectViewModel(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("project") String sProjectId) {
		WasdiLog.debugLog("ProjectResource.getProjectViewModel( Project: " + sProjectId + ")");

		ProjectViewModel oVM = new ProjectViewModel();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("ProjectResource.getProjectViewModel: invalid session");
			return null;
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sProjectId)) {
				return oVM;
			}

			WasdiLog.debugLog("ProjectResource.getProjectViewModel: read projects " + sProjectId);

			// Create repo
			ProjectRepository oProjectRepository = new ProjectRepository();

			// Get requested project
			Project oProject = oProjectRepository.getProjectById(sProjectId);

			if (oProject.getSubscriptionId() == null) {
				WasdiLog.debugLog("ProjectResource.getProjectViewModel: the project is not connected with a subscription, aborting");
				return oVM;
			}

			if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), oProject.getSubscriptionId())) {
				WasdiLog.debugLog("ProjectResource.getProjectViewModel: user cannot access project info, aborting");
				return oVM;
			}

			oVM = convert(oProject, oUser.getActiveProjectId());

//			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
//
//			// Get Sharings
//			List<UserResourcePermission> aoSharings = oUserResourcePermissionRepository
//					.getProjectSharingsByProjectId(oProject.getProjectId());
//			// Add Sharings to View Model
//			if (aoSharings != null) {
//				if (oVM.getSharedUsers() == null) {
//					oVM.setSharedUsers(new ArrayList<String>());
//				}
//
//				for (UserResourcePermission oSharing : aoSharings) {
//					oVM.getSharedUsers().add(oSharing.getUserId());
//				}
//			}
		} catch (Exception oEx) {
			WasdiLog.debugLog( "ProjectResource.getProjectViewModel: " + oEx);
		}

		return oVM;
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

			if (oProjectEditorViewModel.isDefaultProject()) {
				this.changeDefaultProject(sSessionId, oProject.getProjectId());
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

			if (oProjectEditorViewModel.isDefaultProject()) {
				this.changeDefaultProject(sSessionId, oProject.getProjectId());
			} else if (oProject.getProjectId().equals(oUser.getActiveProjectId())) {
				UserRepository oUserRepository = new UserRepository();

				oUser.setActiveProjectId(null);

				if (oUserRepository.updateUser(oUser)) {
					WasdiLog.debugLog("ProjectResource.updateProject( " + "changing the default project of the user to null failed");
					oResult.setStringValue("The removing of the default project failed.");
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
	@Path("/default")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult changeDefaultProject(@HeaderParam("x-session-token") String sSessionId, @QueryParam("project") String sProjectId) {
		WasdiLog.debugLog("ProjectResource.changeDefaultProject( ProjectId: " + sProjectId + ")");

		if (sProjectId != null) {
			if (sProjectId.isEmpty() || sProjectId.equals("null")) {
				sProjectId = null;
			}
		}

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("ProjectResource.changeDefaultProject: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		if (sProjectId != null) {
			ProjectRepository oProjectRepository = new ProjectRepository();

			Project oProject = oProjectRepository.getProjectById(sProjectId);

			if (oProject == null) {
				WasdiLog.debugLog("ProjectResource.changeDefaultProject: project does not exist");
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
			WasdiLog.debugLog("ProjectResource.changeDefaultProject( " + "changing the default project of the user to " + sProjectId + " failed");
			oResult.setStringValue("The changing of the default project failed.");
		}

		return oResult;
	}

	@DELETE
	@Path("/delete")
	public PrimitiveResult deleteProject(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("project") String sProjectId) {
		WasdiLog.debugLog("ProjectResource.deleteProject( Project: " + sProjectId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("ProjectResource.deleteProject: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		ProjectRepository oProjectRepository = new ProjectRepository();

		Project oProject = oProjectRepository.getProjectById(sProjectId);

		if (oProject == null) {
			WasdiLog.debugLog("ProjectResource.deleteProject: project does not exist");
			oResult.setStringValue("No project with the name already exists.");
			return oResult;
		}

//		UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
//
//		String sProjectOwner = oProject.getUserId();
//
//		if (!sProjectOwner.equals(oUser.getUserId())) {
//			// The current uses is not the owner of the project
//			WasdiLog.debugLog("ProjectResource.deleteProject: user " + oUser.getUserId() + " is not the owner [" + sProjectOwner + "]: delete the sharing, not the project");
//			oUserResourcePermissionRepository.deletePermissionsByUserIdAndProjectId(oUser.getUserId(), sProjectId);
//
//			oResult.setBoolValue(true);
//			oResult.setStringValue(sProjectId);
//
//			return oResult;
//		}
//
//		if (oUserResourcePermissionRepository.isProjectShared(sProjectId)) {
//			WasdiLog.debugLog("ProjectResource.deleteProject: the project is shared with users");
//			oResult.setStringValue("The project cannot be removed as it has users. Before deleting the project, please remove the users.");
//			return oResult;
//		}
//
//		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
//
//		if (oSubscriptionRepository.projectHasSubscriptions(sProjectId)) {
//			WasdiLog.debugLog("ProjectResource.deleteProject: the project shares subscriptions");
//			oResult.setStringValue("The project cannot be removed as it shares subscriptions. Before deleting the project, please remove the sharings.");
//			return oResult;
//		}

		if (oProjectRepository.deleteProject(sProjectId)) {
			oResult.setBoolValue(true);
			oResult.setStringValue(sProjectId);
		} else {
			WasdiLog.debugLog("ProjectResource.deleteProject( " + sProjectId + " ): deletion failed");
			oResult.setStringValue("The deletion of the project failed.");
		}

		return oResult;
	}

	private static Project convert(ProjectEditorViewModel oProjectEVM) {
		Project oProject = new Project();
		oProject.setProjectId(oProjectEVM.getProjectId());
		oProject.setSubscriptionId(oProjectEVM.getSubscriptionId());
		oProject.setName(oProjectEVM.getName());
		oProject.setDescription(oProjectEVM.getDescription());

		return oProject;
	}

	private static ProjectListViewModel convert(Project oProject, String sSubscriptionName, String sDefaultProjectId) {
		ProjectListViewModel oProjectListViewModel = new ProjectListViewModel();
		oProjectListViewModel.setProjectId(oProject.getProjectId());
//		oProjectListViewModel.setSubscriptionId(oProject.getSubscriptionId());
		oProjectListViewModel.setSubscriptionName(sSubscriptionName);
		oProjectListViewModel.setName(oProject.getName());
		oProjectListViewModel.setDescription(oProject.getDescription());

		if (oProjectListViewModel.getProjectId() != null && sDefaultProjectId != null && oProjectListViewModel.getProjectId().equals(sDefaultProjectId)) {
			oProjectListViewModel.setDefaultProject(true);
		}

		return oProjectListViewModel;
	}

	private static ProjectViewModel convert(Project oProject, String sDefaultProjectId) {
		ProjectViewModel oProjectViewModel = new ProjectViewModel();
		oProjectViewModel.setProjectId(oProject.getProjectId());
		oProjectViewModel.setSubscriptionId(oProject.getSubscriptionId());
		oProjectViewModel.setName(oProject.getName());
		oProjectViewModel.setDescription(oProject.getDescription());

		if (oProjectViewModel.getProjectId() != null && sDefaultProjectId != null && oProjectViewModel.getProjectId().equals(sDefaultProjectId)) {
			oProjectViewModel.setDefaultProject(true);
		}

		return oProjectViewModel;
	}

}
