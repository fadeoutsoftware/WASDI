package it.fadeout.rest.resources;

import static wasdi.shared.business.UserApplicationPermission.ADMIN_DASHBOARD;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;

import it.fadeout.Wasdi;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.business.User;
import wasdi.shared.business.UserApplicationRole;
import wasdi.shared.business.UserResourcePermission;
import wasdi.shared.business.Workspace;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.permissions.UserResourcePermissionViewModel;
import wasdi.shared.viewmodels.users.UserViewModel;
import wasdi.shared.viewmodels.workspaces.WorkspaceListInfoViewModel;

@Path("/admin")
public class AdminDashboardResource {

	private static final String MSG_ERROR_INVALID_SESSION = "MSG_ERROR_INVALID_SESSION";

	private static final String MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD = "MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD";

	private static final String MSG_ERROR_INVALID_RESOURCE_TYPE = "MSG_ERROR_INVALID_RESOURCE_TYPE";
	private static final String MSG_ERROR_INVALID_PARTIAL_NAME = "MSG_ERROR_INVALID_PARTIAL_NAME";
	private static final String MSG_ERROR_INSUFFICIENT_SEARCH_CRITERIA = "MSG_ERROR_INSUFFICIENT_SEARCH_CRITERIA";

	@Context
	ServletConfig m_oServletConfig;

	@GET
	@Path("/usersByPartialName")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response findUsersByPartialName(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("partialName") String sPartialName) {

		Utils.debugLog("AdminDashboardResource.findUsersByPartialName(" + " Partial name: " + sPartialName + " )");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			Utils.debugLog("AdminDashboardResource.findUsersByPartialName: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ADMIN_DASHBOARD)) {
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD)).build();
		}

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			Utils.debugLog("AdminDashboardResource.findUsersByPartialName: invalid partialName");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_PARTIAL_NAME)).build();
		}

		UserRepository oUserRepository = new UserRepository();
		List<User> aoUsers = oUserRepository.findUsersByPartialName(sPartialName);

		List<UserViewModel> aoUserVMs = new ArrayList<>();

		if (aoUsers != null) {
			aoUserVMs = aoUsers.stream()
					.map(AdminDashboardResource::convert)
					.collect(Collectors.toList());
		}

		GenericEntity<List<UserViewModel>> entity = new GenericEntity<List<UserViewModel>>(aoUserVMs, List.class);

		return Response.ok(entity).build();
	}

	@GET
	@Path("/workspacesByPartialName")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response findWorkspacesByPartialName(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("partialName") String sPartialName) {

		Utils.debugLog("AdminDashboardResource.findWorkspacesByPartialName(" + " Partial name: " + sPartialName + " )");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			Utils.debugLog("WorkspaceResource.shareWorkspace: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ADMIN_DASHBOARD)) {
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD)).build();
		}

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			Utils.debugLog("AdminDashboardResource.findUsersByPartialName: invalid partialName");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_PARTIAL_NAME)).build();
		}

		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		List<Workspace> aoWorkspaces = oWorkspaceRepository.findWorkspacesByPartialName(sPartialName);

		List<WorkspaceListInfoViewModel> aoWorkspaceVMs = new ArrayList<>();

		if (aoWorkspaces != null) {
			aoWorkspaceVMs = aoWorkspaces.stream()
					.map(AdminDashboardResource::convert)
					.collect(Collectors.toList());
		}

		GenericEntity<List<WorkspaceListInfoViewModel>> entity = new GenericEntity<List<WorkspaceListInfoViewModel>>(aoWorkspaceVMs, List.class);

		return Response.ok(entity).build();
	}

	@GET
	@Path("/resourcePermissions")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response findResourcePermissions(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("resourceType") String sResourceType,
			@QueryParam("resourceId") String sResourceId,
			@QueryParam("userId") String sUserId) {

		Utils.debugLog("AdminDashboardResource.findResourcePermissions(" + " ResourceType: " + sResourceType
				+ ", ResourceId: " + sResourceId + ", User: " + sUserId + " )");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			Utils.debugLog("WorkspaceResource.findResourcePermissions: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ADMIN_DASHBOARD)) {
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD)).build();
		}

		if (Utils.isNullOrEmpty(sResourceType) && Utils.isNullOrEmpty(sResourceId) && Utils.isNullOrEmpty(sUserId)) {
			Utils.debugLog("AdminDashboardResource.findResourcePermissions: insufficient search criteria");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INSUFFICIENT_SEARCH_CRITERIA)).build();
		}

		UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
		List<UserResourcePermission> aoPermissions = oUserResourcePermissionRepository
				.getPermissionsByTypeAndUserIdAndResourceId(sResourceType, sUserId, sResourceId);

		List<UserResourcePermissionViewModel> aoPermissionVMs = new ArrayList<>();

		if (aoPermissions != null) {
			aoPermissionVMs = aoPermissions.stream()
					.map(AdminDashboardResource::convert)
					.collect(Collectors.toList());
		}

		GenericEntity<List<UserResourcePermissionViewModel>> entity = new GenericEntity<List<UserResourcePermissionViewModel>>(aoPermissionVMs, List.class);

		return Response.ok(entity).build();
	}

	@POST
	@Path("/resourcePermissions")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response addResourcePermission(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("resourceType") String sResourceType,
			@QueryParam("resourceId") String sResourceId,
			@QueryParam("userId") String sDestinationUserId) {

		Utils.debugLog("AdminDashboardResource.addResourcePermission(" + " ResourceType: " + sResourceType
				+ ", ResourceId: " + sResourceId + ", User: " + sDestinationUserId + " )");

		if (Utils.isNullOrEmpty(sResourceType)) {
			Utils.debugLog("AdminDashboardResource.addResourcePermission: invalid resource type");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_RESOURCE_TYPE)).build();
		}

		if (sResourceType.equalsIgnoreCase("workspace")) {
			WorkspaceResource oWorkspaceResource = new WorkspaceResource();
			PrimitiveResult oResult = oWorkspaceResource.shareWorkspace(sSessionId, sResourceId, sDestinationUserId);

			if (oResult.getBoolValue()) {
				return Response.ok().build();
			} else {
				return Response.status(oResult.getIntValue()).entity(new ErrorResponse(oResult.getStringValue())).build();
			}
		} else {
			Utils.debugLog("AdminDashboardResource.addResourcePermission: invalid resource type");

			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_RESOURCE_TYPE)).build();
		}
	}

	@DELETE
	@Path("/resourcePermissions")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response removeResourcePermission(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("resourceType") String sResourceType,
			@QueryParam("resourceId") String sResourceId,
			@QueryParam("userId") String sUserId) {

		Utils.debugLog("AdminDashboardResource.removeResourcePermission(" + " ResourceType: " + sResourceType
				+ ", ResourceId: " + sResourceId + ", User: " + sUserId + " )");

		if (Utils.isNullOrEmpty(sResourceType)) {
			Utils.debugLog("AdminDashboardResource.removeResourcePermission: invalid resource type");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_RESOURCE_TYPE)).build();
		}

		if (sResourceType.equalsIgnoreCase("workspace")) {
			WorkspaceResource oWorkspaceResource = new WorkspaceResource();
			PrimitiveResult oResult = oWorkspaceResource.deleteUserSharedWorkspace(sSessionId, sResourceId, sUserId);

			if (oResult.getBoolValue()) {
				return Response.ok().build();
			} else {
				return Response.status(oResult.getIntValue()).entity(new ErrorResponse(oResult.getStringValue())).build();
			}
		} else {
			Utils.debugLog("AdminDashboardResource.removeResourcePermission: invalid resource type");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_RESOURCE_TYPE)).build();
		}
	}

	public static UserViewModel convert(User oUser) {
		UserViewModel oUserVM = new UserViewModel();
		oUserVM.setName(oUser.getName());
		oUserVM.setSurname(oUser.getSurname());
		oUserVM.setUserId(oUser.getUserId());

		if (oUser.getRole() != null) {
			oUserVM.setRole(StringUtils.capitalize(oUser.getRole().toLowerCase()));
		}

		return oUserVM;
	}

	public static WorkspaceListInfoViewModel convert(Workspace oWorkspace) {
		WorkspaceListInfoViewModel oWSViewModel = new WorkspaceListInfoViewModel();

		oWSViewModel.setOwnerUserId(oWorkspace.getUserId());
		oWSViewModel.setWorkspaceId(oWorkspace.getWorkspaceId());
		oWSViewModel.setWorkspaceName(oWorkspace.getName());
		oWSViewModel.setNodeCode(oWorkspace.getNodeCode());

		return oWSViewModel;
	}

	public static UserResourcePermissionViewModel convert(UserResourcePermission oPermission) {
		UserResourcePermissionViewModel oPermissionViewModel = new UserResourcePermissionViewModel();

		oPermissionViewModel.setResourceId(oPermission.getResourceId());
		oPermissionViewModel.setResourceType(oPermission.getResourceType());
		oPermissionViewModel.setUserId(oPermission.getUserId());
		oPermissionViewModel.setOwnerId(oPermission.getOwnerId());
		oPermissionViewModel.setPermissions(oPermission.getPermissions());
		oPermissionViewModel.setCreatedBy(oPermission.getCreatedBy());
		oPermissionViewModel.setCreatedDate(oPermission.getCreatedDate());

		return oPermissionViewModel;
	}

}
