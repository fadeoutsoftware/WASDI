package it.fadeout.rest.resources;

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

import wasdi.shared.data.UserRepository;
import wasdi.shared.business.User;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.users.UserViewModel;

@Path("/admin")
public class AdminDashboardResource {

	private static final String MSG_ERROR_INVALID_RESOURCE_TYPE = "MSG_ERROR_INVALID_RESOURCE_TYPE";
	private static final String MSG_ERROR_INVALID_PARTIAL_NAME = "MSG_ERROR_INVALID_PARTIAL_NAME";

	@Context
	ServletConfig m_oServletConfig;

	@GET
	@Path("/usersByPartialName")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response findUsersByPartialName(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("partialName") String sPartialName) {

		Utils.debugLog("AdminDashboardResource.findUsersByPartialName(" + " Partial name: " + sPartialName + " )");

		if (Utils.isNullOrEmpty(sPartialName)) {
			Utils.debugLog("AdminDashboardResource.findUsersByPartialName: invalid partialName");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_PARTIAL_NAME)).build();
		}

		UserRepository oUserRepository = new UserRepository();
		List<User> aoUsers = oUserRepository.findUsersByPartialName(sPartialName);

		List<UserViewModel> aoUserVMs = new ArrayList<>();

		if (aoUsers != null) {
			aoUserVMs = aoUsers.parallelStream()
					.map(AdminDashboardResource::converToBasic)
					.collect(Collectors.toList());
		}

		GenericEntity<List<UserViewModel>> entity = new GenericEntity<List<UserViewModel>>(aoUserVMs, List.class);

		return Response.ok(entity).build();
	}

	@POST
	@Path("/resourcePermission")
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
			return oWorkspaceResource.shareWorkspace(sSessionId, sResourceId, sDestinationUserId);
		} else {
			Utils.debugLog("AdminDashboardResource.addResourcePermission: invalid resource type");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_RESOURCE_TYPE)).build();
		}
	}

	@DELETE
	@Path("/resourcePermission")
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
			return oWorkspaceResource.deleteUserSharedWorkspace(sSessionId, sResourceId, sUserId);
		} else {
			Utils.debugLog("AdminDashboardResource.removeResourcePermission: invalid resource type");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_RESOURCE_TYPE)).build();
		}
	}

	public static UserViewModel converToBasic(User oUser) {
		UserViewModel oUserVM = new UserViewModel();
		oUserVM.setName(oUser.getName());
		oUserVM.setSurname(oUser.getSurname());
		oUserVM.setUserId(oUser.getUserId());

		return oUserVM;
	}

}
