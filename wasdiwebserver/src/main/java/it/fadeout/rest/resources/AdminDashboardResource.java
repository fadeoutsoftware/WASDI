package it.fadeout.rest.resources;

import javax.servlet.ServletConfig;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ErrorResponse;

@Path("/admin")
public class AdminDashboardResource {

	private static final String MSG_ERROR_INVALID_RESOURCE_TYPE = "MSG_ERROR_INVALID_RESOURCE_TYPE";

	@Context
	ServletConfig m_oServletConfig;

	@POST
	@Path("/resourcePermission")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response addResourcePermission(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("resourceType") String sResourceType,
			@QueryParam("resourceId") String sResourceId,
			@QueryParam("userId") String sDestinationUserId) {

		Utils.debugLog("AdminDashboardResource.addResourcePermission(" + " ResourceType: " + sResourceType
				+ ", ResourceId: " + sResourceId + ", User: " + sDestinationUserId + " )");

		if (sResourceType == null) {
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

		if (sResourceType == null) {
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

}
