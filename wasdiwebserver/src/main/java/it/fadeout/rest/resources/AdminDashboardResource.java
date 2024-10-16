package it.fadeout.rest.resources;

import java.util.ArrayList;
import java.util.Arrays;
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
import wasdi.shared.business.Organization;
import wasdi.shared.business.SnapWorkflow;
import wasdi.shared.business.Style;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.missions.Mission;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserAccessRights;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.business.users.UserType;
import wasdi.shared.data.MetricsEntryRepository;
import wasdi.shared.data.OrganizationRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.StyleRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.data.SnapWorkflowRepository;
import wasdi.shared.data.missions.MissionsRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.GenericResourceViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.SuccessResponse;
import wasdi.shared.viewmodels.monitoring.MetricsEntry;
import wasdi.shared.viewmodels.permissions.UserResourcePermissionViewModel;
import wasdi.shared.viewmodels.processors.DeployedProcessorViewModel;
import wasdi.shared.viewmodels.users.FullUserViewModel;
import wasdi.shared.viewmodels.users.UserListViewModel;
import wasdi.shared.viewmodels.users.UserViewModel;
import wasdi.shared.viewmodels.users.UsersSummaryViewModel;
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
	public Response findUsersByPartialName(@HeaderParam("x-session-token") String sSessionId, @QueryParam("partialName") String sPartialName) {
		
		try {
			WasdiLog.debugLog("AdminDashboardResource.findUsersByPartialName(" + " Partial name: " + sPartialName + " )");

			// Validate Session
			User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
			if (oRequesterUser == null) {
				WasdiLog.warnLog("AdminDashboardResource.findUsersByPartialName: invalid session");
				return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
			}

			// Can the user access this section?
			if (!UserApplicationRole.isAdmin(oRequesterUser)) {
				WasdiLog.warnLog("AdminDashboardResource.findUsersByPartialName: user is not an admin");
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
			}
			
			// Do we have at least 3 chars to make our search?
			if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
				WasdiLog.warnLog("AdminDashboardResource.findUsersByPartialName: invalid partialName");
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
		catch (Exception oEx) {
			WasdiLog.errorLog("AdminDashboardResource.findUsersByPartialName: invalid partialName");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

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
			WasdiLog.warnLog("AdminDashboardResource.findWorkspacesByPartialName: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.isAdmin(oRequesterUser)) {
			WasdiLog.warnLog("AdminDashboardResource.findWorkspacesByPartialName: user is not an admin");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
		}

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			WasdiLog.warnLog("AdminDashboardResource.findWorkspacesByPartialName: invalid partialName");
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

		GenericEntity<List<WorkspaceListInfoViewModel>> aoWorkspaceViewModels = new GenericEntity<List<WorkspaceListInfoViewModel>>(aoWorkspaceVMs, List.class);

		return Response.ok(aoWorkspaceViewModels).build();
	}
	
	@GET
	@Path("/resourceByPartialName")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response findResourceByPartialName(@HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("resourceType") String sResourceType,
			@QueryParam("partialName") String sPartialName,
			@QueryParam("offset") Integer iOffset, 
			@QueryParam("limit") Integer iLimit) {
		
		WasdiLog.debugLog("AdminDashboardResource.findResourceByPartialName( Resource type: " + sResourceType + ", Partial name: " + sPartialName + " )");
		
		try {

			// Validate Session
			User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
			if (oRequesterUser == null) {
				WasdiLog.debugLog("AdminDashboardResource.findResourceByPartialName: invalid session");
				return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
			}
	
			// Can the user access this section?
			if (!UserApplicationRole.isAdmin(oRequesterUser)) {
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
			}
	
			if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
				WasdiLog.debugLog("AdminDashboardResource.findResourceByPartialName: invalid partialName");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_PARTIAL_NAME.name())).build();
			}
			
			if (Utils.isNullOrEmpty(sResourceType)) {
				WasdiLog.debugLog("AdminDashboardResource.findResourceByPartialName: no resource type specified");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INSUFFICIENT_SEARCH_CRITERIA.name())).build();
			}
			
			if (iOffset == null) iOffset = 0;
			
			if (iLimit == null) iLimit = 10;
			
			List<GenericResourceViewModel> aoResultsVM = null;
			
			for (ResourceTypes oType : ResourceTypes.values()) {
				
				if (!sResourceType.equalsIgnoreCase(oType.name())) {
					continue;
				}
				
				if (oType.equals(ResourceTypes.WORKSPACE)) {
					aoResultsVM = retrieveWorkspacesByPartialName(sPartialName);
				} 
				else if (oType.equals(ResourceTypes.PROCESSOR)) {
					aoResultsVM = retrieveProcessorsByPartialName(sPartialName);
				} 
				else if (oType.equals(ResourceTypes.ORGANIZATION)) {
					aoResultsVM = retrieveOrganizationsByPartialName(sPartialName);
				} 
				else if (oType.equals(ResourceTypes.WORKFLOW)) {
					aoResultsVM = retrieveWorkflowsByPartialName(sPartialName);
				} 
				else if (oType.equals(ResourceTypes.STYLE)) {
					aoResultsVM = retrieveStylesByPartialName(sPartialName);
				} 
			}
			
			if (aoResultsVM == null) {
				WasdiLog.warnLog("AdminDashboard.findResourceByPartialName: looks like there was an error while retrieving the resource type " + sResourceType + " with partial name " + sPartialName);
				return Response.serverError().build();
			}
			
			
			List<GenericResourceViewModel> aoPaginatedResultsViewModel = new ArrayList<>();
			
			for (int i = 0; i < aoResultsVM.size(); i++) {
				
				if (i < iOffset) continue;
				if (i >= iOffset+iLimit) break;
				
				aoPaginatedResultsViewModel.add(aoResultsVM.get(i));
			}
			
			GenericEntity<List<GenericResourceViewModel>> aoGenericResourcesViewModels = new GenericEntity<List<GenericResourceViewModel>>(aoPaginatedResultsViewModel, List.class);
			return Response.ok(aoGenericResourcesViewModels).build();
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("AdminDashboard.findResourceByPartialName: exception while trying to retrieve the resource ", oEx);
			return Response.serverError().build();
		}

		
	}
	
	/**
	 * Retrieve the list of workspaces matching a partial name
	 * @param sPartialName the partial name that the workspace's name should match
	 * @return the list of view model representing the resource
	 */
	private List<GenericResourceViewModel> retrieveWorkspacesByPartialName(String sPartialName) {
		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		
		List<Workspace> aoWorkspaces = oWorkspaceRepository.findWorkspacesByPartialName(sPartialName);
		
		if (aoWorkspaces == null) {
			WasdiLog.warnLog("AdminDashboard.retrieveWorkspacesByPartialName. List of retrieved workspaces is null");
			return null;
		}

		List<GenericResourceViewModel> aoResultsVM = new ArrayList<>();
		
		for (Workspace oWorkspace : aoWorkspaces) {
			aoResultsVM.add(new GenericResourceViewModel(ResourceTypes.WORKSPACE.name(), oWorkspace.getWorkspaceId(), oWorkspace.getName(), oWorkspace.getUserId()));
		}
		
		WasdiLog.debugLog("AdminDashboard.retrieveWorkspacesByPartialName. Retrieved " + aoResultsVM.size() + " workspaces matching the partial name " + sPartialName);
		
		return aoResultsVM;
	}
	
	
	/**
	 * Retrieve the list of processors matching a partial name
	 * @param sPartialName the partial name that the pocessor's name should match
	 * @return the list of view model representing the resource
	 */
	private List<GenericResourceViewModel> retrieveProcessorsByPartialName(String sPartialName) {
		
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		
		List<Processor> aoProcessors = oProcessorRepository.findProcessorsByPartialName(sPartialName);
		
		if (aoProcessors == null) {
			WasdiLog.warnLog("AdminDashboard.retrieveProcessorsByPartialName. List of retrieved processors is null");
			return null;
		}

		List<GenericResourceViewModel> aoResultsVM = new ArrayList<>();
		
		for (Processor oProcessor : aoProcessors) {
			aoResultsVM.add(new GenericResourceViewModel(ResourceTypes.PROCESSOR.name(), oProcessor.getProcessorId(), oProcessor.getName(), oProcessor.getUserId()));
		}
		
		WasdiLog.debugLog("AdminDashboard.retrieveProcessorsByPartialName. Retrieved " + aoResultsVM.size() + " processors matching the partial name " + sPartialName);
	
		return aoResultsVM;
	}
	
	
	/**
	 * Retrieve the list of organisations matching a partial name
	 * @param sPartialName the partial name that the organisation's name should match
	 * @return the list of view model representing the resource
	 */
	private List<GenericResourceViewModel> retrieveOrganizationsByPartialName(String sPartialName) {
		
		OrganizationRepository oOrganizationRepository = new OrganizationRepository();
		
		List<Organization> aoOrganization = oOrganizationRepository.findOrganizationsByPartialName(sPartialName);
		
		if (aoOrganization == null) {
			WasdiLog.warnLog("AdminDashboard.retrieveOrganizationsByPartialName. List of retrieved organizations is null");
			return null;
		}
		
		List<GenericResourceViewModel> aoResultsVM = new ArrayList<>();
		
		for (Organization oOrganization : aoOrganization) {
			aoResultsVM.add(new GenericResourceViewModel(ResourceTypes.ORGANIZATION.name(), oOrganization.getOrganizationId(), oOrganization.getName(), oOrganization.getUserId()));
		}
		
		WasdiLog.debugLog("AdminDashboard.retrieveOrganizationsByPartialName. Retrieved " + aoResultsVM.size() + " organizations matching the partial name " + sPartialName);
	
		return aoResultsVM;
	}

	/**
	 * Retrieve the list of SNAP workflows matching a partial name
	 * @param sPartialName the partial name that the organisation's name should match
	 * @return the list of view model representing the resource
	 */
	private List<GenericResourceViewModel> retrieveWorkflowsByPartialName(String sPartialName) {
		
		SnapWorkflowRepository oWorkflowRepository = new SnapWorkflowRepository();
		
		List<SnapWorkflow> aoWorkflows = oWorkflowRepository.findWorkflowByPartialName(sPartialName);
		
		if (aoWorkflows == null) {
			WasdiLog.warnLog("AdminDashboard.retrieveWorkflowsByPartialName. List of retrieved workflows is null");
			return null;
		}

		List<GenericResourceViewModel> aoResultsVM = new ArrayList<>();
		
		for (SnapWorkflow oWorkflow : aoWorkflows) {
			aoResultsVM.add(new GenericResourceViewModel(ResourceTypes.WORKFLOW.name(), oWorkflow.getWorkflowId(), oWorkflow.getName(), oWorkflow.getUserId()));
		}
		
		WasdiLog.debugLog("AdminDashboard.retrieveWorkflowsByPartialName. Retrieved " + aoResultsVM.size() + " workflows matching the partial name " + sPartialName);

		return aoResultsVM;
	}
	
	/**
	 * Retrieve the list of styles matching a partial name
	 * @param sPartialName the partial name that the style's name should match
	 * @return the list of view model representing the resource
	 */
	private List<GenericResourceViewModel> retrieveStylesByPartialName(String sPartialName) {
		
		StyleRepository oStyleRepository = new StyleRepository();
		
		List<Style> aoStyles = oStyleRepository.findStylesByPartialName(sPartialName);
		
		List<GenericResourceViewModel> aoResultsVM = new ArrayList<>();
		
		if (aoStyles == null) {
			return null;
		}
		
		for (Style oStyle : aoStyles) {
			aoResultsVM.add(new GenericResourceViewModel(ResourceTypes.STYLE.name(), oStyle.getStyleId(), oStyle.getName(), oStyle.getUserId()));
		}
	
		return aoResultsVM;
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
		else if (sResourceType.equalsIgnoreCase(ResourceTypes.MISSION.getResourceType())) {
			MissionsRepository oMissionsRepository = new MissionsRepository();
			Mission oMission = oMissionsRepository.getMissionsByIndexValue(sResourceId);
			
			if (oMission == null) {
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid Resource Id")).build();
			}
			
			UserResourcePermission oUserResourcePermission = new UserResourcePermission(sResourceType,sResourceId, sDestinationUserId,oMission.getUserid(),oRequesterUser.getUserId(), sRights);
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			if (oUserResourcePermissionRepository.insertPermission(oUserResourcePermission)) {
				return Response.ok().build();
			}
			else {
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Error inserting the sharing")).build();
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
			
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			UserResourcePermission oUserResourcePermission = oUserResourcePermissionRepository.getPermissionByTypeAndUserIdAndResourceId(sResourceType, sUserId, sResourceId);
			
			if (oUserResourcePermission != null) {
				oUserResourcePermissionRepository.deletePermissionsByTypeAndUserIdAndResourceId(sResourceType, sUserId, sResourceId);
				return Response.ok().build();
			}
			
			WasdiLog.debugLog("AdminDashboardResource.removeResourcePermission: invalid resource type");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_RESOURCE_TYPE.name())).build();
		}
	}
	
	/**
	 * Get the list of available Resource Types
	 * 
	 * @param sSessionId Session Id
	 * @return Array of strings with the names of the resource types
	 */
	@GET
	@Path("resourcePermissions/types")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getResourceTypes(@HeaderParam("x-session-token") String sSessionId) {
		WasdiLog.debugLog("SubscriptionResource.getSubscriptionTypes");
		try {
			ArrayList<String> asResourceTypes = new ArrayList<>();
			
			List<ResourceTypes> aoOriginalList = Arrays.asList(ResourceTypes.values());
			
			for (ResourceTypes oResType : aoOriginalList) {
				asResourceTypes.add(oResType.name());
			}
			
			return Response.ok(asResourceTypes).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.getSubscriptionTypes: error ", oEx);
			return Response.serverError().build();			
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
	

	@GET
	@Path("/users/list")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getUsersList(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("partialName") String sPartialName,
			@QueryParam("offset") Integer iOffset, @QueryParam("limit") Integer iLimit,
			@QueryParam("sortedby") String sSortedBy, @QueryParam("order") String sOrder) {
		
		
		WasdiLog.debugLog("AdminDashboardResource.getUsersList(" + " Partial name: " + sPartialName + " )");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.infoLog("AdminDashboardResource.getUsersList: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.isAdmin(oRequesterUser)) {
			WasdiLog.infoLog("AdminDashboardResource.getUsersList: requesting user is not an admin ");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
		}
		
		// Clean our inputs
		if (Utils.isNullOrEmpty(sPartialName)) sPartialName = "";
		if (Utils.isNullOrEmpty(sSortedBy)) sSortedBy = "userId";
		
		if (! (sSortedBy.equals("name") || sSortedBy.equals("surname") || sSortedBy.equals("userId") || sSortedBy.equals(""))) sSortedBy = "userId"; 
		
		if (Utils.isNullOrEmpty(sOrder)) sOrder = "asc";
		if (iOffset == null) iOffset = 0;
		if (iLimit == null) iLimit = 10;
		
		int iOrder = 1;
		
		if (sOrder.equals("desc") || sOrder.equals("des") || sOrder.equals("0") || sOrder.equals("-1")) {
			WasdiLog.debugLog("AdminDashboardResource.getUsersList: setting iOrder to -1 due to order= " + sOrder);
			iOrder = -1;
		}
		
		try {
			// Create the repo and get the list
			UserRepository oUserRepository = new UserRepository();
			
			
			List<User> aoUsers = oUserRepository.findUsersByPartialName(sPartialName, sSortedBy, iOrder);

			List<UserListViewModel> aoUserVMs = new ArrayList<>();
			
			for (int iUsers = 0; iUsers < aoUsers.size(); iUsers++) {
				if (iUsers<iOffset) continue;
				if (iUsers>=iOffset+iLimit) break;
				
				try {
					User oActualUser = aoUsers.get(iUsers);
					
					UserListViewModel oUserListViewModel = new UserListViewModel();
					oUserListViewModel.setUserId(oActualUser.getUserId());
					
					Boolean bValid = oActualUser.getValidAfterFirstAccess();
					if (bValid == null) bValid = false;
					oUserListViewModel.setActive(bValid);
					oUserListViewModel.setLastLogin(oActualUser.getLastLogin());
					oUserListViewModel.setType(PermissionsUtils.getUserType(oActualUser.getUserId()));
					oUserListViewModel.setName(oActualUser.getName());
					oUserListViewModel.setSurname(oActualUser.getSurname());
					
					aoUserVMs.add(oUserListViewModel);					
				}
				catch (Exception oEx) {
					WasdiLog.errorLog("AdminDashboardResource.getUsersList: exception reading a user ", oEx);
				}				
			}

			return Response.ok(aoUserVMs).build();			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("AdminDashboardResource.getUsersList: exception ", oEx);
			return Response.serverError().build();
		}
	}	
	
	
	@GET
	@Path("/users/summary")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getUsersSummary(@HeaderParam("x-session-token") String sSessionId) {
		
		WasdiLog.debugLog("AdminDashboardResource.getUsersSummary");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.infoLog("AdminDashboardResource.getUsersSummary: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.isAdmin(oRequesterUser)) {
			WasdiLog.infoLog("AdminDashboardResource.getUsersSummary: requesting user is not an admin ");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
		}
		
		try {
			// Create the repo and get the list
			UserRepository oUserRepository = new UserRepository();
			
			UsersSummaryViewModel oUsersSummaryViewModel = new UsersSummaryViewModel();
			
			
			// NOTE: This can become critical with a very long users list
			List<User> aoUsers = oUserRepository.getAllUsers();
			
			oUsersSummaryViewModel.setTotalUsers(aoUsers.size());
			
			int iNone = 0;
			int iFree = 0;
			int iStd = 0;
			int iPro = 0;
			
			for (User oUser : aoUsers) {
				String sType = PermissionsUtils.getUserType(oUser.getUserId());
				
				if (sType.equals(UserType.NONE.name())) {
					iNone ++;
				}
				else if (sType.equals(UserType.FREE.name())) {
					iFree ++;
				}
				else if (sType.equals(UserType.STANDARD.name())) {
					iStd ++;
				}
				else if (sType.equals(UserType.PROFESSIONAL.name())) {
					iPro ++;
				}
			}
			
			oUsersSummaryViewModel.setNoneUsers(iNone);
			oUsersSummaryViewModel.setFreeUsers(iFree);
			oUsersSummaryViewModel.setProUsers(iPro);
			oUsersSummaryViewModel.setStandardUsers(iStd);
			
			OrganizationRepository oOrganizationRepository = new OrganizationRepository();
			long lOrgs = oOrganizationRepository.getOrganizationsList().size();
			
			oUsersSummaryViewModel.setOrganizations((int)lOrgs);			

			return Response.ok(oUsersSummaryViewModel).build();			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("AdminDashboardResource.getUsersSummary: exeception ", oEx);
			return Response.serverError().build();
		}
	}		
	
	@GET
	@Path("/users")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getUsersDetails(@HeaderParam("x-session-token") String sSessionId, @QueryParam("userId") String sTargetUser) {
		
		WasdiLog.debugLog("AdminDashboardResource.getUsersDetails for " + sTargetUser);

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.infoLog("AdminDashboardResource.getUsersDetails: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.isAdmin(oRequesterUser)) {
			WasdiLog.infoLog("AdminDashboardResource.getUsersDetails: requesting user is not an admin ");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
		}
		
		if (Utils.isNullOrEmpty(sTargetUser)) {
			WasdiLog.infoLog("AdminDashboardResource.getUsersDetails: target user is empty");
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		UserRepository oUserRepository = new UserRepository();
		User oTargetUser = oUserRepository.getUser(sTargetUser);
		
		if (oTargetUser == null){
			WasdiLog.infoLog("AdminDashboardResource.getUsersDetails: target user not found");
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		try {
			// Convert the Target User to the view model
			FullUserViewModel oFullUserViewModel = FullUserViewModel.fromUser(oTargetUser);
			return Response.ok(oFullUserViewModel).build();			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("AdminDashboardResource.getUsersDetails: exeception ", oEx);
			return Response.serverError().build();
		}
	}			
	
	@PUT
	@Path("/users")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response updateUsersDetails(@HeaderParam("x-session-token") String sSessionId, FullUserViewModel oUserViewModel) {
		
		WasdiLog.debugLog("AdminDashboardResource.updateUsersDetails");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.infoLog("AdminDashboardResource.updateUsersDetails: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.isAdmin(oRequesterUser)) {
			WasdiLog.infoLog("AdminDashboardResource.updateUsersDetails: requesting user is not an admin ");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
		}
		
		if (oUserViewModel == null) {
			WasdiLog.infoLog("AdminDashboardResource.updateUsersDetails: user view model is empty");
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		UserRepository oUserRepository = new UserRepository();
		User oTargetUser = oUserRepository.getUser(oUserViewModel.getUserId());
		
		if (oTargetUser == null){
			WasdiLog.infoLog("AdminDashboardResource.updateUsersDetails: target user not found");
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		try {
			// Convert the view model to the entity
			
			oTargetUser.setConfirmationDate(oTargetUser.getConfirmationDate());
			oTargetUser.setDefaultNode(oUserViewModel.getDefaultNode());
			oTargetUser.setDescription(oUserViewModel.getDescription());
			oTargetUser.setLink(oUserViewModel.getLink());
			oTargetUser.setName(oUserViewModel.getName());
			oTargetUser.setRegistrationDate(oUserViewModel.getRegistrationDate());
			oTargetUser.setRole(oUserViewModel.getRole());
			oTargetUser.setSurname(oUserViewModel.getSurname());
			
			if (!oUserRepository.updateUser(oTargetUser)) {
				WasdiLog.errorLog("AdminDashboardResource.updateUsersDetails: the update user returned false!");
				return Response.serverError().build();
			}
			
			return Response.ok().build();			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("AdminDashboardResource.updateUsersDetails: exeception ", oEx);
			return Response.serverError().build();
		}
	}
	
	@DELETE
	@Path("/users")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response deleteUser(@HeaderParam("x-session-token") String sSessionId, @QueryParam("userId") String sTargetUser) {
		
		WasdiLog.debugLog("AdminDashboardResource.deleteUser for " + sTargetUser);

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.infoLog("AdminDashboardResource.deleteUser: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.isAdmin(oRequesterUser)) {
			WasdiLog.infoLog("AdminDashboardResource.deleteUser: requesting user is not an admin ");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
		}
		
		if (Utils.isNullOrEmpty(sTargetUser)) {
			WasdiLog.infoLog("AdminDashboardResource.deleteUser: target user is empty");
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		UserRepository oUserRepository = new UserRepository();
		User oTargetUser = oUserRepository.getUser(sTargetUser);
		
		if (oTargetUser == null){
			WasdiLog.infoLog("AdminDashboardResource.deleteUser: target user not found");
			return Response.status(Status.BAD_REQUEST).build();			
		}
		
		try {
			if (PermissionsUtils.deleteUser(oTargetUser, sSessionId)) {
				return Response.ok().build();				
			}
			else {
				WasdiLog.errorLog("AdminDashboardResource.deleteUser: delete user returned false!!");
				return Response.serverError().build();
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("AdminDashboardResource.deleteUser: exeception ", oEx);
			return Response.serverError().build();
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
