package it.fadeout.rest.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;

import it.fadeout.Wasdi;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserAccessRights;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.data.MetricsEntryRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.SuccessResponse;
import wasdi.shared.viewmodels.monitoring.MetricsEntry;
import wasdi.shared.viewmodels.permissions.UserResourcePermissionViewModel;
import wasdi.shared.viewmodels.processors.DeployedProcessorViewModel;
import wasdi.shared.viewmodels.users.UserViewModel;
import wasdi.shared.viewmodels.workspaces.WorkspaceListInfoViewModel;

/**
 * Admin Dashboard Resource
 * 
 * Host the API for the Admin backend:
 * 	.find users, workspace and processors also with partial names
 * 	.Store and read Metrics Entries
 * 
 * The metrics are pushed by each node to the main one to let WASDI 
 * decide the best node at runtime.
 * 
 * @author p.campanella
 *
 */
@Path("/admin")
public class AdminDashboardResource {

	@Context
	ServletConfig m_oServletConfig;

	@GET
	@Path("/usersByPartialName")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response findUsersByPartialName(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("partialName") String sPartialName) {

		WasdiLog.debugLog("AdminDashboardResource.findUsersByPartialName(" + " Partial name: " + sPartialName + " )");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.debugLog("AdminDashboardResource.findUsersByPartialName: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.isAdmin(oRequesterUser)) {
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
		}
		
		// Do we have at least 3 chars to make our search?
		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			WasdiLog.debugLog("AdminDashboardResource.findUsersByPartialName: invalid partialName");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_PARTIAL_NAME.name())).build();
		}
		
		// Create the repo and get the list
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

		WasdiLog.debugLog("AdminDashboardResource.findWorkspacesByPartialName(" + " Partial name: " + sPartialName + " )");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.debugLog("AdminDashboardResource.findWorkspacesByPartialName: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.isAdmin(oRequesterUser)) {
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
		}

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			WasdiLog.debugLog("AdminDashboardResource.findWorkspacesByPartialName: invalid partialName");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_PARTIAL_NAME.name())).build();
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
	@Path("/processorsByPartialName")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response findProcessorsByPartialName(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("partialName") String sPartialName) {

		WasdiLog.debugLog("AdminDashboardResource.findProcessorsByPartialName(" + " Partial name: " + sPartialName + " )");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.debugLog("WorkspaceResource.findProcessorsByPartialName: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.isAdmin(oRequesterUser)) {
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
		}

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			WasdiLog.debugLog("AdminDashboardResource.findProcessorsByPartialName: invalid partialName");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_PARTIAL_NAME.name())).build();
		}

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		List<Processor> aoProcessors = oProcessorRepository.findProcessorsByPartialName(sPartialName);

		List<DeployedProcessorViewModel> aoProcessorVMs = new ArrayList<>();

		if (aoProcessors != null) {
			aoProcessorVMs = aoProcessors.stream()
					.map(AdminDashboardResource::convert)
					.collect(Collectors.toList());
		}

		GenericEntity<List<DeployedProcessorViewModel>> entity = new GenericEntity<List<DeployedProcessorViewModel>>(aoProcessorVMs, List.class);

		return Response.ok(entity).build();
	}

	@GET
	@Path("/resourcePermissions")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response findResourcePermissions(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("resourceType") String sResourceType,
			@QueryParam("resourceId") String sResourceId,
			@QueryParam("userId") String sUserId) {

		WasdiLog.debugLog("AdminDashboardResource.findResourcePermissions(" + " ResourceType: " + sResourceType
				+ ", ResourceId: " + sResourceId + ", User: " + sUserId + " )");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.debugLog("AdminDashboardResource.findResourcePermissions: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.isAdmin(oRequesterUser)) {
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
		}

		if (Utils.isNullOrEmpty(sResourceType) && Utils.isNullOrEmpty(sResourceId) && Utils.isNullOrEmpty(sUserId)) {
			WasdiLog.debugLog("AdminDashboardResource.findResourcePermissions: insufficient search criteria");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INSUFFICIENT_SEARCH_CRITERIA.name())).build();
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
			@QueryParam("userId") String sDestinationUserId, @QueryParam("rights") String sRights) {

		WasdiLog.debugLog("AdminDashboardResource.addResourcePermission(" + " ResourceType: " + sResourceType
				+ ", ResourceId: " + sResourceId + ", User: " + sDestinationUserId + " )");
		
		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.debugLog("AdminDashboardResource.addResourcePermission: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}		
		
		// Use Read By default
		if (!UserAccessRights.isValidAccessRight(sRights)) {
			sRights = UserAccessRights.READ.getAccessRight();
		}

		if (Utils.isNullOrEmpty(sResourceType)) {
			WasdiLog.debugLog("AdminDashboardResource.addResourcePermission: invalid resource type");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_RESOURCE_TYPE.name())).build();
		}

		if (sResourceType.equalsIgnoreCase(ResourceTypes.NODE.getResourceType())) {
			NodeResource oNodeResource = new NodeResource();
			PrimitiveResult oResult = oNodeResource.shareNode(sSessionId, sResourceId, sDestinationUserId, sRights);

			if (oResult.getBoolValue()) {
				return Response.ok().build();
			} else {
				return Response.status(oResult.getIntValue()).entity(new ErrorResponse(oResult.getStringValue())).build();
			}
		} 
		else if (sResourceType.equalsIgnoreCase(ResourceTypes.PARAMETER.getResourceType())) {
			ProcessorParametersTemplateResource oProcessorParametersTemplateResource = new ProcessorParametersTemplateResource();
			PrimitiveResult oResult = oProcessorParametersTemplateResource.shareProcessorParametersTemplate(sSessionId, sResourceId, sDestinationUserId, sRights);

			if (oResult.getBoolValue()) {
				return Response.ok().build();
			} else {
				return Response.status(oResult.getIntValue()).entity(new ErrorResponse(oResult.getStringValue())).build();
			}
		} 
		else if (sResourceType.equalsIgnoreCase(ResourceTypes.PROCESSOR.getResourceType())) {
			ProcessorsResource oProcessorResource = new ProcessorsResource();
			PrimitiveResult oResult = oProcessorResource.shareProcessor(sSessionId, sResourceId, sDestinationUserId, sRights);

			if (oResult.getBoolValue()) {
				return Response.ok().build();
			} else {
				return Response.status(oResult.getIntValue()).entity(new ErrorResponse(oResult.getStringValue())).build();
			}
		} 
		else if (sResourceType.equalsIgnoreCase(ResourceTypes.STYLE.getResourceType())) {
			StyleResource oStyleResource = new StyleResource();
			PrimitiveResult oResult = oStyleResource.shareStyle(sSessionId, sResourceId, sDestinationUserId, sRights);

			if (oResult.getBoolValue()) {
				return Response.ok().build();
			} else {
				return Response.status(oResult.getIntValue()).entity(new ErrorResponse(oResult.getStringValue())).build();
			}
		} 
		else if (sResourceType.equalsIgnoreCase(ResourceTypes.WORKFLOW.getResourceType())) {
			WorkflowsResource oWorkflowResource = new WorkflowsResource();
			PrimitiveResult oResult = oWorkflowResource.shareWorkflow(sSessionId, sResourceId, sDestinationUserId, sRights);

			if (oResult.getBoolValue()) {
				return Response.ok().build();
			} else {
				return Response.status(oResult.getIntValue()).entity(new ErrorResponse(oResult.getStringValue())).build();
			}
		} 
		else if (sResourceType.equalsIgnoreCase(ResourceTypes.WORKSPACE.getResourceType())) {
			WorkspaceResource oWorkspaceResource = new WorkspaceResource();
			PrimitiveResult oResult = oWorkspaceResource.shareWorkspace(sSessionId, sResourceId, sDestinationUserId, sRights);

			if (oResult.getBoolValue()) {
				return Response.ok().build();
			} else {
				return Response.status(oResult.getIntValue()).entity(new ErrorResponse(oResult.getStringValue())).build();
			}
		} 
		else {
			WasdiLog.debugLog("AdminDashboardResource.addResourcePermission: invalid resource type");

			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_RESOURCE_TYPE.name())).build();
		}
	}

	@DELETE
	@Path("/resourcePermissions")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response removeResourcePermission(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("resourceType") String sResourceType,
			@QueryParam("resourceId") String sResourceId,
			@QueryParam("userId") String sUserId) {

		WasdiLog.debugLog("AdminDashboardResource.removeResourcePermission(" + " ResourceType: " + sResourceType
				+ ", ResourceId: " + sResourceId + ", User: " + sUserId + " )");
		
		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.debugLog("AdminDashboardResource.removeResourcePermission: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}		

		if (Utils.isNullOrEmpty(sResourceType)) {
			WasdiLog.debugLog("AdminDashboardResource.removeResourcePermission: invalid resource type");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_RESOURCE_TYPE.name())).build();
		}

		if (sResourceType.equalsIgnoreCase(ResourceTypes.NODE.getResourceType())) {
			NodeResource oNodeResource = new NodeResource();
			PrimitiveResult oResult = oNodeResource.deleteUserSharedNode(sSessionId, sResourceId, sUserId);

			if (oResult.getBoolValue()) {
				return Response.ok().build();
			} 
			else {
				return Response.status(oResult.getIntValue()).entity(new ErrorResponse(oResult.getStringValue())).build();
			}
		} 
		if (sResourceType.equalsIgnoreCase(ResourceTypes.PARAMETER.getResourceType())) {
			ProcessorParametersTemplateResource oProcessorParametersTemplateResource = new ProcessorParametersTemplateResource();
			PrimitiveResult oResult = oProcessorParametersTemplateResource.deleteUserSharedProcessorParametersTemplate(sSessionId, sResourceId, sUserId);

			if (oResult.getBoolValue()) {
				return Response.ok().build();
			} 
			else {
				return Response.status(oResult.getIntValue()).entity(new ErrorResponse(oResult.getStringValue())).build();
			}
		} 
		else if (sResourceType.equalsIgnoreCase(ResourceTypes.PROCESSOR.getResourceType())) {
			ProcessorsResource oProcessorResource = new ProcessorsResource();
			PrimitiveResult oResult = oProcessorResource.deleteUserSharingProcessor(sSessionId, sResourceId, sUserId);

			if (oResult.getBoolValue()) {
				return Response.ok().build();
			} 
			else {
				return Response.status(oResult.getIntValue()).entity(new ErrorResponse(oResult.getStringValue())).build();
			}
		} 
		else if (sResourceType.equalsIgnoreCase(ResourceTypes.STYLE.getResourceType())) {
			StyleResource oStyleResource = new StyleResource();
			PrimitiveResult oResult = oStyleResource.deleteUserSharingStyle(sSessionId, sResourceId, sUserId);

			if (oResult.getBoolValue()) {
				return Response.ok().build();
			} 
			else {
				return Response.status(oResult.getIntValue()).entity(new ErrorResponse(oResult.getStringValue())).build();
			}
		} 
		else if (sResourceType.equalsIgnoreCase(ResourceTypes.WORKFLOW.getResourceType())) {
			WorkflowsResource oWorkflowResource = new WorkflowsResource();
			PrimitiveResult oResult = oWorkflowResource.deleteUserSharingWorkflow(sSessionId, sResourceId, sUserId);

			if (oResult.getBoolValue()) {
				return Response.ok().build();
			} 
			else {
				return Response.status(oResult.getIntValue()).entity(new ErrorResponse(oResult.getStringValue())).build();
			}
		} 
		else if (sResourceType.equalsIgnoreCase(ResourceTypes.WORKSPACE.getResourceType())) {
			WorkspaceResource oWorkspaceResource = new WorkspaceResource();
			PrimitiveResult oResult = oWorkspaceResource.deleteUserSharedWorkspace(sSessionId, sResourceId, sUserId);

			if (oResult.getBoolValue()) {
				return Response.ok().build();
			} 
			else {
				return Response.status(oResult.getIntValue()).entity(new ErrorResponse(oResult.getStringValue())).build();
			}
		} 
		else {
			WasdiLog.debugLog("AdminDashboardResource.removeResourcePermission: invalid resource type");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_RESOURCE_TYPE.name())).build();
		}
	}

	@PUT
	@Path("/metrics")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response updateMetricsEntry(@HeaderParam("x-session-token") String sSessionId, MetricsEntry oMetricsEntry) {

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.debugLog("AdminDashboardResource.updateMetricsEntry: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.isAdmin(oRequesterUser)) {
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
		}

		if (oMetricsEntry == null) {
			WasdiLog.debugLog("AdminDashboardResource.updateMetricsEntry: invalid payload");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_METRICS_ENTRY.name())).build();
		}


		try {
			MetricsEntryRepository oMetricsEntryRepository = new MetricsEntryRepository();
			oMetricsEntryRepository.updateMetricsEntry(oMetricsEntry);
		} catch (Exception oEx) {
			WasdiLog.errorLog("AdminDashboardResource.updateMetricsEntry: Error inserting metricsEntry: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_IN_INSERT_PROCESS.name())).build();
		}

		return Response.ok(new SuccessResponse(ClientMessageCodes.MSG_SUCCESS_METRICS_ENTRY_INSERT.name())).build();
	}

	@GET
	@Path("/metrics/latest")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getLatestMetricsEntry(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("nodeCode") String sNodeCode) {

		WasdiLog.debugLog("AdminDashboardResource.getLatestMetricsEntry()");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.debugLog("AdminDashboardResource.getLatestMetricsEntry: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.isAdmin(oRequesterUser)) {
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
		}


		try {
			MetricsEntryRepository oMetricsEntryRepository = new MetricsEntryRepository();
			MetricsEntry oMetricsEntry = oMetricsEntryRepository.getLatestMetricsEntryByNode(sNodeCode);

			GenericEntity<MetricsEntry> oGenericEntity = new GenericEntity<MetricsEntry>(oMetricsEntry, MetricsEntry.class);

			return Response.ok(oGenericEntity).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("AdminDashboardResource.getLatestMetricsEntry: Error searching metricsEntry: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_IN_SEARCH_PROCESS.name())).build();
		}
	}
	
	/**
	 * Converts a User in a User View Model
	 * @param oUser User Entity to convert
	 * @return Corresponding View Model
	 */
	public static UserViewModel convert(User oUser) {
		UserViewModel oUserVM = new UserViewModel();
		oUserVM.setName(oUser.getName());
		oUserVM.setSurname(oUser.getSurname());
		oUserVM.setUserId(oUser.getUserId());
		oUserVM.setType(PermissionsUtils.getUserType(oUser));

		if (oUser.getRole() != null) {
			oUserVM.setRole(StringUtils.capitalize(oUser.getRole().toLowerCase()));
		}

		return oUserVM;
	}
	
	/**
	 * Converts a Workspace Entity in the WorkspaceListInfoViewModel
	 * @param oWorkspace Workspace Entity to convert
	 * @return Corresponding View Model
	 */
	public static WorkspaceListInfoViewModel convert(Workspace oWorkspace) {
		WorkspaceListInfoViewModel oWSViewModel = new WorkspaceListInfoViewModel();

		oWSViewModel.setOwnerUserId(oWorkspace.getUserId());
		oWSViewModel.setWorkspaceId(oWorkspace.getWorkspaceId());
		oWSViewModel.setWorkspaceName(oWorkspace.getName());
		oWSViewModel.setNodeCode(oWorkspace.getNodeCode());

		return oWSViewModel;
	}

	/**
	 * Convert a Processor Entity in the DeployedProcessorViewModel
	 * @param oProcessor Processor Entity to convert
	 * @return Corresponding View Model
	 */
	public static DeployedProcessorViewModel convert(Processor oProcessor) {
		DeployedProcessorViewModel oProcessorViewModel = new DeployedProcessorViewModel();

		oProcessorViewModel.setPublisher(oProcessor.getUserId());
		oProcessorViewModel.setProcessorId(oProcessor.getProcessorId());
		oProcessorViewModel.setProcessorName(oProcessor.getName());
		oProcessorViewModel.setProcessorDescription(oProcessor.getDescription());
		oProcessorViewModel.setType(oProcessor.getType());
		oProcessorViewModel.setProcessorVersion(oProcessor.getVersion());
		oProcessorViewModel.setIsPublic(oProcessor.getIsPublic());

		return oProcessorViewModel;
	}

	/**
	 * Converts a UserResourcePermission Entity in to the UserResourcePermissionViewModel
	 * @param oPermission UserResourcePermission entity to convert
	 * @return Corresponding View Model
	 */
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
