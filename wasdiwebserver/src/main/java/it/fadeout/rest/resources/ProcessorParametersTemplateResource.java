package it.fadeout.rest.resources;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorParametersTemplate;
import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserAccessRights;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessorParametersTemplateRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.utils.MailUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.processorParametersTemplates.ProcessorParametersTemplateSharingViewModel;
import wasdi.shared.viewmodels.processors.ProcessorParametersTemplateDetailViewModel;
import wasdi.shared.viewmodels.processors.ProcessorParametersTemplateListViewModel;

/**
 * ProcessorParametersTemplate Resource.
 * 
 * Hosts the API for: 
 * 	.create edit and delete ProcessorParametersTemplate
 * 	.Share processor parameters with other users
 * 
 * @author PetruPetrescu
 *
 */
@Path("processorParamTempl")
public class ProcessorParametersTemplateResource {

	/**
	 * Deletes a processor parameters template.
	 * 
	 * @param sSessionId the User Session Id
	 * @param sTemplateId the template Id
	 * @param sName the name of the template
	 * @return std http response
	 */
	@DELETE
	@Path("/delete")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response deleteProcessorParametersTemplate(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("templateId") String sTemplateId) {

		try {
			sTemplateId = java.net.URLDecoder.decode(sTemplateId, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			WasdiLog.errorLog("ProcessorParametersTemplateResource.deleteProcessorParametersTemplate excepion decoding the template Id");
		}

		WasdiLog.debugLog("ProcessorParametersTemplateResource.deleteProcessorParametersTemplate(sProcessorId: " + sTemplateId + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Check the user session
		if (oUser == null) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.deleteProcessorParametersTemplate: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		if (!PermissionsUtils.canUserAccessProcessorParametersTemplate(oUser.getUserId(), sTemplateId)) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.deleteProcessorParametersTemplate: user cannot access parameter template");
			return Response.status(Status.FORBIDDEN).build();
		}

		String sUserId = oUser.getUserId();

		ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();
		ProcessorParametersTemplate oTemplate = oProcessorParametersTemplateRepository.getProcessorParametersTemplateByTemplateId(sTemplateId);

		if (oTemplate == null) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		int iDeletedCount = 0;

		UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
		
		// Check if this is the owner or a shared user
		if (!oProcessorParametersTemplateRepository.isTheOwnerOfTheTemplate(sTemplateId, sUserId)) {
			// Shared User: we just delete the sharing
			iDeletedCount = oUserResourcePermissionRepository.deletePermissionsByUserIdAndProcessorParametersTemplateId(sUserId, sTemplateId);
		}
		else {
			// Owner: we delete the template
			iDeletedCount = oProcessorParametersTemplateRepository.deleteByTemplateId(sTemplateId);
			
			if (iDeletedCount>0) {
				// Clean also the sharings, it does not exists anymore
				oUserResourcePermissionRepository.deletePermissionsByProcessorParameterTemplateId(sTemplateId);
			}
		}

		if (iDeletedCount == 0) {
			return Response.status(Status.BAD_REQUEST).build();
		}

		return Response.status(Status.OK).build();
	}

	/**
	 * Updates a template
	 * 
	 * @param sSessionId the User Session Id
	 * @param oDetailViewModel the detail view model
	 * @return std http response
	 */
	@POST
	@Path("/update")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response updateProcessorParametersTemplate(@HeaderParam("x-session-token") String sSessionId, ProcessorParametersTemplateDetailViewModel oDetailViewModel) {
		WasdiLog.debugLog("ProcessorParametersTemplateResource.updateProcessorParametersTemplate");

		// Check the user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.updateProcessorParametersTemplate: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		String sUserId = oUser.getUserId();

		if (oDetailViewModel == null) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.updateProcessorParametersTemplate: body null");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		if (!PermissionsUtils.canUserWriteProcessorParametersTemplate(sUserId, oDetailViewModel.getTemplateId())) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.updateProcessorParametersTemplate: user cannot write this parameter");
			return Response.status(Status.FORBIDDEN).build();
		}

		ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();

		ProcessorParametersTemplate oTemplate = getTemplateFromDetailViewModel(oDetailViewModel, sUserId, oDetailViewModel.getTemplateId());

		Date oDate = new Date();
		oTemplate.setUpdateDate((double) oDate.getTime());

		boolean isUpdated = oProcessorParametersTemplateRepository.updateProcessorParametersTemplate(oTemplate);
		if (isUpdated) {
			return Response.status(Status.OK).build();
		} else {
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	/**
	 * Add a template
	 * 
	 * @param sSessionId the User Session Id
	 * @param oDetailViewModel the detail view model
	 * @return std http reponse
	 */
	@POST
	@Path("/add")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response addProcessorParametersTemplate(@HeaderParam("x-session-token") String sSessionId, ProcessorParametersTemplateDetailViewModel oDetailViewModel) {
		WasdiLog.debugLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate");

		// Check the user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate: invalid user");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		if (oDetailViewModel == null) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate: invalid view model");
			return Response.status(Status.BAD_REQUEST).build();
		}

		String sProcessorId = oDetailViewModel.getProcessorId();

		if (Utils.isNullOrEmpty(sProcessorId)) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate: invalid processorId");
			return Response.status(Status.BAD_REQUEST).build();
		}

		try {
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

			if (oProcessor == null) {
				WasdiLog.warnLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate: processor null " + sProcessorId);
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), oProcessor)) {
				WasdiLog.warnLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate: user canno access the processor");
				return Response.status(Status.FORBIDDEN).build();
			}		
			
			String sUserId = oUser.getUserId();

			ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();

			ProcessorParametersTemplate oTemplate = getTemplateFromDetailViewModel(oDetailViewModel, sUserId, Utils.getRandomName());

			Date oDate = new Date();
			oTemplate.setCreationDate((double) oDate.getTime());
			oTemplate.setUpdateDate((double) oDate.getTime());

			oProcessorParametersTemplateRepository.insertProcessorParametersTemplate(oTemplate);

			return Response.status(Status.OK).build();			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateResource.addProcessorParametersTemplate: error ", oEx);
			return Response.serverError().build();
		}
	}

	/**
	 * Get the ProcessorParametersTemplate by Id.
	 * 
	 * @param sSessionId the User Session Id
	 * @param sTemplateId the template Id
	 * @return the detail view model
	 */
	@GET
	@Path("/get")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response getProcessorParameterTemplateById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("templateId") String sTemplateId) {
		WasdiLog.debugLog("ProcessorParametersTemplateResource.getProcessorParametersTemplateById");

		// Check the user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.getProcessorParametersTemplateById: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		String sUserId = oUser.getUserId();

		if (!PermissionsUtils.canUserAccessProcessorParametersTemplate(sUserId, sTemplateId)) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.getProcessorParameterTemplateById: user cannot access the parameter");
			return Response.status(Status.FORBIDDEN).build();
		}
		
		try {
			// Get all the ProcessorParametersTemplates
			ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();
			ProcessorParametersTemplate oTemplate = oProcessorParametersTemplateRepository.getProcessorParametersTemplateByTemplateId(sTemplateId);

			ProcessorParametersTemplateDetailViewModel oDetailViewModel = getDetailViewModel(oTemplate);
			
			if (oDetailViewModel!=null) {
				oDetailViewModel.setReadOnly(!PermissionsUtils.canUserWriteProcessorParametersTemplate(sUserId, sTemplateId));
				return Response.ok(oDetailViewModel).build();
			}
			else {
				return Response.serverError().build();
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateResource.getProcessorParameterTemplateById: error ", oEx);
			return Response.serverError().build();
		}
	}

	/**
	 * Get the list of ProcessorParametersTemplate associated to a processor.
	 * 
	 * @param sSessionId   User Session Id
	 * @param sProcessorId Processor Id
	 * @return a list of list view model
	 */
	@GET
	@Path("/getlist")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response getProcessorParametersTemplatesListByProcessor(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorId") String sProcessorId) {
		WasdiLog.debugLog("ProcessorParametersTemplateResource.getProcessorParametersTemplatesListByProcessor");

		// Check the user session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.getProcessorParametersTemplatesListByProcessor: invalid session");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

		if (oProcessor == null) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.getProcessorParametersTemplatesListByProcessor: invalid processor");
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		if (!PermissionsUtils.canUserAccessProcessor(oUser.getUserId(), oProcessor)) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.getProcessorParametersTemplatesListByProcessor: user canno access the processor");
			return Response.status(Status.FORBIDDEN).build();
		}
		
		try {
			String sUserId = oUser.getUserId();

			// Get all the ProcessorParametersTemplates
			ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();

			List<ProcessorParametersTemplate> aoUnfilteredTemplates = oProcessorParametersTemplateRepository.getProcessorParametersTemplatesByProcessor(sProcessorId);

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			List<UserResourcePermission> aoSharings = oUserResourcePermissionRepository.getProcessorParametersTemplateSharingsByUserId(sUserId);

			Map<String, UserResourcePermission> aoSharingsMap = aoSharings.stream().collect(Collectors.toMap(UserResourcePermission::getResourceId, Function.identity()));

			List<ProcessorParametersTemplate> aoTemplates = new ArrayList<>();

			for (ProcessorParametersTemplate oTemplate : aoUnfilteredTemplates) {
				if (oTemplate.getUserId().equals(sUserId) || aoSharingsMap.containsKey(oTemplate.getTemplateId())) {
					aoTemplates.add(oTemplate);
				}
			}

			// Cast in a list
			List<ProcessorParametersTemplateListViewModel> aoListViewModel = getListViewModels(aoTemplates);
			
			for (ProcessorParametersTemplateListViewModel oActualParameterViewModel : aoListViewModel) {
				oActualParameterViewModel.setReadOnly(!PermissionsUtils.canUserWriteProcessorParametersTemplate(sUserId, oActualParameterViewModel.getTemplateId()));
			}

			return Response.ok(aoListViewModel).build();			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateResource.getProcessorParametersTemplatesListByProcessor: error ", oEx);
			return Response.serverError().build();
		}		
	}

	/**
	 * Share a processorParametersTemplate with another user.
	 *
	 * @param sSessionId User Session Id
	 * @param sProcessorParametersTemplateId ProcessorParametersTemplate Id
	 * @param sDestinationUserId User id that will receive the processorParametersTemplate in sharing.
	 * @return Primitive Result with boolValue = true and stringValue = Done if ok. False and error description otherwise
	 */
	@PUT
	@Path("share/add")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult shareProcessorParametersTemplate(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processorParametersTemplate") String sProcessorParametersTemplateId, @QueryParam("userId") String sDestinationUserId, @QueryParam("rights") String sRights) {

		WasdiLog.debugLog("ProcessorParametersTemplateResource.shareProcessorParametersTemplate( Template Id: " + sProcessorParametersTemplateId + ", User: " + sDestinationUserId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.shareProcessorParametersTemplate: invalid session");

			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name());

			return oResult;
		}
		
		// Use Read By default
		if (!UserAccessRights.isValidAccessRight(sRights)) {
			sRights = UserAccessRights.READ.getAccessRight();
		}
		
		// Check if the processorParametersTemplate exists
		ProcessorParametersTemplateRepository oProcessorParametersTemplateRepository = new ProcessorParametersTemplateRepository();
		ProcessorParametersTemplate oProcessorParametersTemplate = oProcessorParametersTemplateRepository.getProcessorParametersTemplateByTemplateId(sProcessorParametersTemplateId);
		
		if (oProcessorParametersTemplate == null) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.shareProcessorParametersTemplate: invalid processorParametersTemplate");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_INVALID_PROCESSOR_PARAMETERS_TEMPLATE.name());

			return oResult;
		}
		
		// Can the user access this resource?
		if (!PermissionsUtils.canUserWriteProcessorParametersTemplate(oRequesterUser.getUserId(), sProcessorParametersTemplateId)
				&& !UserApplicationRole.isAdmin(oRequesterUser)) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.shareProcessorParametersTemplate: " + sProcessorParametersTemplateId + " cannot be accessed by " + oRequesterUser.getUserId() + ", aborting");

			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_PROCESSOR_PARAMETERS_TEMPLATE.name());

			return oResult;
		}		
		
		// Cannot Autoshare
		if (oRequesterUser.getUserId().equals(sDestinationUserId)) {
			if (UserApplicationRole.isAdmin(oRequesterUser)) {
				// A user that has Admin rights should be able to auto-share the resource.
			} else {
				WasdiLog.warnLog("ProcessorParametersTemplateResource.shareProcessorParametersTemplate: auto sharing not so smart");

				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
				oResult.setStringValue(ClientMessageCodes.MSG_ERROR_SHARING_WITH_ONESELF.name());

				return oResult;
			}
		}
		
		// Cannot share with the owner
		if (oProcessorParametersTemplate.getUserId().equals(sDestinationUserId)) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.shareProcessorParametersTemplate: sharing with the owner not so smart");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_SHARING_WITH_OWNER.name());

			return oResult;
		}

		UserRepository oUserRepository = new UserRepository();
		User oDestinationUser = oUserRepository.getUser(sDestinationUserId);

		if (oDestinationUser == null) {
			//No. So it is neither the owner or a shared one
			WasdiLog.warnLog("ProcessorParametersTemplateResource.shareProcessorParametersTemplate: Destination user does not exists");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER.name());

			return oResult;
		}


		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(oProcessorParametersTemplate.getProcessorId());

		String sProcessorName = "unknown";

		if (oProcessor != null) {
			sProcessorName = oProcessor.getName();
		}

		try {
            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			if (!oUserResourcePermissionRepository.isProcessorParametersTemplateSharedWithUser(sDestinationUserId, sProcessorParametersTemplateId)) {
				UserResourcePermission oProcessorParametersTemplateSharing =
						new UserResourcePermission(ResourceTypes.PARAMETER.getResourceType(), sProcessorParametersTemplateId, sDestinationUserId, oProcessorParametersTemplate.getUserId(), oRequesterUser.getUserId(), sRights);

				oUserResourcePermissionRepository.insertPermission(oProcessorParametersTemplateSharing);				
			} else {
				WasdiLog.debugLog("ProcessorParametersTemplateResource.shareProcessorParametersTemplate: already shared!");
				oResult.setStringValue("Already Shared.");
				oResult.setBoolValue(true);
				oResult.setIntValue(Status.OK.getStatusCode());

				return oResult;
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateResource.shareProcessorParametersTemplate: " + oEx);

			oResult.setIntValue(Status.INTERNAL_SERVER_ERROR.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_IN_INSERT_PROCESS.name());

			return oResult;
		}

		try {
			String sTitle = "WASDI " + sProcessorName + " Parameter " + oProcessorParametersTemplate.getName() + " shared with you";
			
			String sMessage = "The user " + oRequesterUser.getUserId() + " shared with you the parameters " + oProcessorParametersTemplate.getName() + " of " + sProcessorName + " WASDI Application.";
			
			MailUtils.sendEmail(WasdiConfig.Current.notifications.sftpManagementMailSender, sDestinationUserId, sTitle, sMessage);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateResource.sendNotificationEmail: notification exception " + oEx.toString());
		}
		
		oResult.setStringValue("Done");
		oResult.setBoolValue(true);

		return oResult;
	}

	/**
	 * Get the list of users that has a ProcessorParametersTemplate in sharing.
	 *
	 * @param sSessionId User Session
	 * @param sProcessorParametersTemplateId ProcessorParametersTemplate Id
	 * @return list of ProcessorParametersTemplate Sharing View Models
	 */
	@GET
	@Path("share/byprocessorParametersTemplate")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<ProcessorParametersTemplateSharingViewModel> getEnabledUsersSharedProcTemplates(@HeaderParam("x-session-token") String sSessionId, @QueryParam("processorParametersTemplate") String sProcessorParametersTemplateId) {

		WasdiLog.debugLog("ProcessorParametersTemplateResource.getEnabledUsersSharedProcTemplates( WS: " + sProcessorParametersTemplateId + " )");

	
		List<UserResourcePermission> aoProcessorParametersTemplateSharing = null;
		List<ProcessorParametersTemplateSharingViewModel> aoProcessorParametersTemplateSharingViewModels = new ArrayList<>();

		User oOwnerUser = Wasdi.getUserFromSession(sSessionId);
		if (oOwnerUser == null) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.getEnabledUsersSharedProcTemplates: invalid session");
			return aoProcessorParametersTemplateSharingViewModels;
		}
		
		if (!PermissionsUtils.canUserAccessProcessorParametersTemplate(oOwnerUser.getUserId(), sProcessorParametersTemplateId)) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.getEnabledUsersSharedProcTemplates: user cannot access this parameter");
			return aoProcessorParametersTemplateSharingViewModels;			
		}

		try {
            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
            aoProcessorParametersTemplateSharing = oUserResourcePermissionRepository.getProcessorParametersTemplateSharingsByProcessorParametersTemplateId(sProcessorParametersTemplateId);

			if (aoProcessorParametersTemplateSharing != null) {
				for (UserResourcePermission oProcessorParametersTemplateSharing : aoProcessorParametersTemplateSharing) {
					ProcessorParametersTemplateSharingViewModel oProcessorParametersTemplateSharingViewModel = new ProcessorParametersTemplateSharingViewModel();
					oProcessorParametersTemplateSharingViewModel.setOwnerId(oProcessorParametersTemplateSharing.getUserId());
					oProcessorParametersTemplateSharingViewModel.setUserId(oProcessorParametersTemplateSharing.getUserId());
					oProcessorParametersTemplateSharingViewModel.setProcessorParametersTemplateId(oProcessorParametersTemplateSharing.getResourceId());
					oProcessorParametersTemplateSharingViewModel.setPermissions(oProcessorParametersTemplateSharing.getPermissions());

					aoProcessorParametersTemplateSharingViewModels.add(oProcessorParametersTemplateSharingViewModel);
				}

			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateResource.getEnabledUsersSharedProcTemplates: " + oEx);
			return aoProcessorParametersTemplateSharingViewModels;
		}

		return aoProcessorParametersTemplateSharingViewModels;

	}

	/**
	 * Removes one user from the sharings of a parameter
	 * @param sSessionId Session Id
	 * @param sProcessorParametersTemplateId Parameter Template Id
	 * @param sUserId User Id to remove
	 * @return Primitive Result
	 */
	@DELETE
	@Path("share/delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult deleteUserSharedProcessorParametersTemplate(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("processorParametersTemplate") String sProcessorParametersTemplateId, @QueryParam("userId") String sUserId) {

		WasdiLog.debugLog("ProcessorParametersTemplateResource.deleteUserSharedProcessorParametersTemplate( WS: " + sProcessorParametersTemplateId + ", User:" + sUserId + " )");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		// Validate Session
		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);

		if (oRequestingUser == null) {
			WasdiLog.warnLog("ProcessorParametersTemplateResource.deleteUserSharedProcessorParametersTemplate: invalid session");

			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name());

			return oResult;
		}

		try {

			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);

			if (oDestinationUser == null) {
				WasdiLog.warnLog("ProcessorParametersTemplateResource.deleteUserSharedProcessorParametersTemplate: invalid destination user");

				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
				oResult.setStringValue(ClientMessageCodes.MSG_ERROR_INVALID_DESTINATION_USER.name());

				return oResult;
			}

            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
            oUserResourcePermissionRepository.deletePermissionsByUserIdAndProcessorParametersTemplateId(sUserId, sProcessorParametersTemplateId);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateResource.deleteUserSharedProcessorParametersTemplate: " + oEx);

			oResult.setIntValue(Status.INTERNAL_SERVER_ERROR.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_IN_DELETE_PROCESS.name());

			return oResult;
		}

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);
		oResult.setIntValue(Status.OK.getStatusCode());

		return oResult;

	}
	

	/**
	 * Fill the list of ProcessorParametersTemplateListViewModel result from a list of templates.
	 * 
	 * @param aoTemplates the list of templates
	 * @return the list of ListViewModel
	 */
	private static List<ProcessorParametersTemplateListViewModel> getListViewModels(List<ProcessorParametersTemplate> aoTemplates) {
		if (aoTemplates == null) {
			return null;
		}

		return aoTemplates.stream().map(ProcessorParametersTemplateResource::getListViewModel).collect(Collectors.toList());
	}

	/**
	 * Fill the ProcessorParametersTemplateListViewModel result from a {@link ProcessorParametersTemplate} object.
	 * 
	 * @param oTemplate the template object
	 * @return a new list view model object
	 */
	private static ProcessorParametersTemplateListViewModel getListViewModel(ProcessorParametersTemplate oTemplate) {
		if (oTemplate == null) {
			return null;
		}

		ProcessorParametersTemplateListViewModel oListViewModel = new ProcessorParametersTemplateListViewModel();
		oListViewModel.setTemplateId(oTemplate.getTemplateId());
		oListViewModel.setUserId(oTemplate.getUserId());
		oListViewModel.setProcessorId(oTemplate.getProcessorId());
		oListViewModel.setName(oTemplate.getName());

		if (oTemplate.getUpdateDate() != null) {
			oListViewModel.setUpdateDate(Utils.getFormatDate(new Date(oTemplate.getUpdateDate().longValue())));
		}

		return oListViewModel;
	}

	/**
	 * Fill the {@link ProcessorParametersTemplateDetailViewModel} result from a {@link ProcessorParametersTemplate} object.
	 * 
	 * @param oTemplate the template object
	 * @return a new detail view model object
	 */
	private static ProcessorParametersTemplateDetailViewModel getDetailViewModel(ProcessorParametersTemplate oTemplate) {
		if (oTemplate == null) {
			return null;
		}

		ProcessorParametersTemplateDetailViewModel oDetailViewModel = new ProcessorParametersTemplateDetailViewModel();
		oDetailViewModel.setTemplateId(oTemplate.getTemplateId());
		oDetailViewModel.setUserId(oTemplate.getUserId());
		oDetailViewModel.setProcessorId(oTemplate.getProcessorId());
		oDetailViewModel.setName(oTemplate.getName());
		oDetailViewModel.setDescription(oTemplate.getDescription());
		oDetailViewModel.setJsonParameters(oTemplate.getJsonParameters());

		if (oTemplate.getCreationDate() != null) {
			oDetailViewModel.setCreationDate(Utils.getFormatDate(new Date(oTemplate.getCreationDate().longValue())));
		}

		if (oTemplate.getUpdateDate() != null) {
			oDetailViewModel.setUpdateDate(Utils.getFormatDate(new Date(oTemplate.getUpdateDate().longValue())));
		}

		return oDetailViewModel;
	}

	/**
	 * Converts a {@link ProcessorParametersTemplateDetailViewModel} in a {@link ProcessorParametersTemplate} object.
	 * 
	 * @param oDetailViewModel the view model object
	 * @param sUserId the user Id
	 * @param sId the id of the template object
	 * @return a new template object
	 */
	private static ProcessorParametersTemplate getTemplateFromDetailViewModel(ProcessorParametersTemplateDetailViewModel oDetailViewModel, String sUserId, String sId) {
		if (oDetailViewModel == null) {
			return null;
		}

		ProcessorParametersTemplate oTemplate = new ProcessorParametersTemplate();
		oTemplate.setTemplateId(sId);
		oTemplate.setUserId(sUserId);
		oTemplate.setProcessorId(oDetailViewModel.getProcessorId());
		oTemplate.setName(oDetailViewModel.getName());
		oTemplate.setDescription(oDetailViewModel.getDescription());
		oTemplate.setJsonParameters(oDetailViewModel.getJsonParameters());
		oTemplate.setCreationDate(Utils.getWasdiDateAsDouble(oDetailViewModel.getCreationDate()));

		return oTemplate;
	}	

}
