package it.fadeout.rest.resources;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import wasdi.shared.business.Node;
import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserAccessRights;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.utils.MailUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.NodeSharingViewModel;
import wasdi.shared.viewmodels.NodeViewModel;
import wasdi.shared.viewmodels.PrimitiveResult;

/**
 * Node Resource.
 * Hosts API for:
 * 	.get the list of WASDI nodes
 * @author p.campanella
 *
 */
@Path("/node")
public class NodeResource {	
	
	/**
	 * Get the list of WASDI Nodes
	 * @param sSessionId User Session
	 * @return List of Node View Models
	 */
	@GET
	@Path("/allnodes")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<NodeViewModel> getAllNodes(@HeaderParam("x-session-token") String sSessionId) {
		
		WasdiLog.debugLog("NodeResource.getAllNodes( Session: " + sSessionId + ")");
		
		// Check the user
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			WasdiLog.warnLog("NodeResource.getAllNodes: invalid session");
			return null;			
		}
		
		// get list of all active nodes
		NodeRepository oNodeRepository = new NodeRepository();
		List<Node> aoNodes = oNodeRepository.getNodesList();
	
		if (aoNodes == null) {
			WasdiLog.warnLog("NodeResource.getAllNodes: Node list is null");
			return null;
		}
		
		// returning list
		List<NodeViewModel> aoNodeViewModelList = new ArrayList<>();
		
		// For all the nodes
		for (Node oNode:aoNodes) {
			try {
				
				// checks whether the node is active
				if (oNode.getActive()) {
					
					// Create the view model and fill it
					NodeViewModel oNodeViewModel = new NodeViewModel();
					
					if (oNode.getCloudProvider()!=null) {
						oNodeViewModel.setCloudProvider(oNode.getCloudProvider());
					}
					else {
						oNodeViewModel.setCloudProvider(oNode.getNodeCode());
					}
					
					oNodeViewModel.setNodeCode(oNode.getNodeCode());

					oNodeViewModel.setApiUrl(oNode.getNodeBaseAddress());
					
					// Add to the return list
					aoNodeViewModelList.add(oNodeViewModel);
					
				}
			}
			catch (Throwable oEx) {
				WasdiLog.errorLog("NodeResource.getAllNodes: Exception " + oEx.toString());
			}
		}
		
		WasdiLog.debugLog("NodeResource.getAllNodes: end cycle");
		
		// done, return the list to the user
		return aoNodeViewModelList;
	}


	/**
	 * Share a node with another user.
	 *
	 * @param sSessionId User Session Id
	 * @param sNodeCode Node Id
	 * @param sDestinationUserId User id that will receive the node in sharing.
	 * @return Primitive Result with boolValue = true and stringValue = Done if ok. False and error description otherwise
	 */
	@PUT
	@Path("share/add")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult shareNode(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("node") String sNodeCode, @QueryParam("userId") String sDestinationUserId, @QueryParam("rights") String sRights) {

		WasdiLog.debugLog("NodeResource.ShareNode( Node: " + sNodeCode + ", User: " + sDestinationUserId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.warnLog("NodeResource.shareNode: invalid session");

			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name());

			return oResult;
		}
		
		// Use Read By default
		if (!UserAccessRights.isValidAccessRight(sRights)) {
			sRights = UserAccessRights.READ.getAccessRight();
		}

		// Check if the node exists
		NodeRepository oNodeRepository = new NodeRepository();
		Node oNode = oNodeRepository.getNodeByCode(sNodeCode);

		if (oNode == null) {
			WasdiLog.warnLog("NodeResource.ShareNode: invalid node");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_INVALID_NODE.name());

			return oResult;
		}

//		// Can the user access this section?
//		if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), NODE_READ)) {
//			WasdiLog.warnLog("NodeResource.shareNode: " + oRequesterUser.getUserId() + " cannot access the section " + ", aborting");
//
//			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
//			oResult.setStringValue(MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_NODE);
//
//			return oResult;
//		}

		// Can the user access this resource?
		if (!UserApplicationRole.isAdmin(oRequesterUser)) {
			WasdiLog.warnLog("NodeResource.shareNode: " + sNodeCode + " cannot be accessed by " + oRequesterUser.getUserId() + ", aborting");

			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_NODE.name());

			return oResult;
		}

		UserRepository oUserRepository = new UserRepository();
		User oDestinationUser = oUserRepository.getUser(sDestinationUserId);

		if (oDestinationUser == null) {
			//No. So it is neither the owner or a shared one
			WasdiLog.warnLog("NodeResource.shareNode: Destination user does not exists");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER.name());

			return oResult;
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			if (!oUserResourcePermissionRepository.isNodeSharedWithUser(sDestinationUserId, sNodeCode)) {
				UserResourcePermission oNodeSharing =
						new UserResourcePermission(ResourceTypes.NODE.getResourceType(), sNodeCode, sDestinationUserId, null, oRequesterUser.getUserId(), sRights);

				oUserResourcePermissionRepository.insertPermission(oNodeSharing);				
			} else {
				WasdiLog.debugLog("NodeResource.shareNode: already shared!");
				oResult.setStringValue("Already Shared.");
				oResult.setBoolValue(true);
				oResult.setIntValue(Status.OK.getStatusCode());

				return oResult;
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeResource.shareNode: " + oEx);

			oResult.setIntValue(Status.INTERNAL_SERVER_ERROR.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_IN_INSERT_PROCESS.name());

			return oResult;
		}

		sendNotificationEmail(oRequesterUser.getUserId(), sDestinationUserId, oNode.getNodeCode());

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);

		return oResult;
	}

	private static void sendNotificationEmail(String sRequesterUserId, String sDestinationUserId, String sNodeName) {
		try {
			WasdiLog.debugLog("NodeResource.sendNotificationEmail: send notification");

			String sTitle = "Node " + sNodeName + " Shared";

			String sMessage = "The user " + sRequesterUserId +  " shared with you the node: " + sNodeName;

			MailUtils.sendEmail(WasdiConfig.Current.notifications.sftpManagementMailSender, sDestinationUserId, sTitle, sMessage);

		}
		catch (Exception oEx) {
			WasdiLog.errorLog("NodeResource.sendNotificationEmail: notification exception " + oEx.toString());
		}
	}

	/**
	 * Get the list of users that has a Node in sharing.
	 *
	 * @param sSessionId User Session
	 * @param sNodeCode Node Id
	 * @return list of Node Sharing View Models
	 */
	@GET
	@Path("share/bynode")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<NodeSharingViewModel> getEnableUsersSharedWorksace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("node") String sNodeCode) {

		WasdiLog.debugLog("NodeResource.getEnableUsersSharedWorksace( WS: " + sNodeCode + " )");

	
		List<UserResourcePermission> aoNodeSharing = null;
		List<NodeSharingViewModel> aoNodeSharingViewModels = new ArrayList<NodeSharingViewModel>();

		User oOwnerUser = Wasdi.getUserFromSession(sSessionId);
		if (oOwnerUser == null) {
			WasdiLog.warnLog("NodeResource.getEnableUsersSharedWorksace: invalid session");
			return aoNodeSharingViewModels;
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			aoNodeSharing = oUserResourcePermissionRepository.getNodeSharingsByNodeCode(sNodeCode);

			if (aoNodeSharing != null) {
				for (UserResourcePermission oNodeSharing : aoNodeSharing) {
					NodeSharingViewModel oNodeSharingViewModel = new NodeSharingViewModel();
					oNodeSharingViewModel.setUserId(oNodeSharing.getUserId());
					oNodeSharingViewModel.setNodeCode(oNodeSharing.getResourceId());

					aoNodeSharingViewModels.add(oNodeSharingViewModel);
				}

			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeResource.getEnableUsersSharedWorksace: " + oEx);
			return aoNodeSharingViewModels;
		}

		return aoNodeSharingViewModels;

	}

	@DELETE
	@Path("share/delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult deleteUserSharedNode(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("node") String sNodeCode, @QueryParam("userId") String sUserId) {

		WasdiLog.debugLog("NodeResource.deleteUserSharedNode( WS: " + sNodeCode + ", User:" + sUserId + " )");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		// Validate Session
		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);

		if (oRequestingUser == null) {
			WasdiLog.warnLog("NodeResource.deleteUserSharedNode: invalid session");

			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name());

			return oResult;
		}

		try {

			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);

			if (oDestinationUser == null) {
				WasdiLog.warnLog("NodeResource.deleteUserSharedNode: invalid destination user");

				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
				oResult.setStringValue(ClientMessageCodes.MSG_ERROR_INVALID_DESTINATION_USER.name());

				return oResult;
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			oUserResourcePermissionRepository.deletePermissionsByUserIdAndNodeCode(sUserId, sNodeCode);
		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeResource.deleteUserSharedNode: " + oEx);

			oResult.setIntValue(Status.INTERNAL_SERVER_ERROR.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_IN_DELETE_PROCESS.name());

			return oResult;
		}

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);
		oResult.setIntValue(Status.OK.getStatusCode());

		return oResult;

	}
	
	
}
