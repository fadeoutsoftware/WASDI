package it.fadeout.rest.resources;

import static wasdi.shared.business.UserApplicationPermission.ADMIN_DASHBOARD;
import static wasdi.shared.business.UserApplicationPermission.NODE_READ;

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
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import wasdi.shared.business.Node;
import wasdi.shared.business.User;
import wasdi.shared.business.UserApplicationRole;
import wasdi.shared.business.UserResourcePermission;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
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

	private static final String MSG_ERROR_INVALID_SESSION = "MSG_ERROR_INVALID_SESSION";

	private static final String MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_NODE = "MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_NODE";
	private static final String MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_NODE = "MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_NODE";

	private static final String MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER = "MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER";

	private static final String MSG_ERROR_INVALID_NODE = "MSG_ERROR_INVALID_NODE";
	private static final String MSG_ERROR_INVALID_DESTINATION_USER = "MSG_ERROR_INVALID_DESTINATION_USER";
	private static final String MSG_ERROR_IN_DELETE_PROCESS = "MSG_ERROR_IN_DELETE_PROCESS";
	private static final String MSG_ERROR_IN_INSERT_PROCESS = "MSG_ERROR_IN_INSERT_PROCESS";
	
	
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
			WasdiLog.debugLog("NodeResource.getAllNodes: invalid session");
			return null;			
		}
		
		// get list of all active nodes
		NodeRepository oNodeRepository = new NodeRepository();
		List<Node> aoNodes = oNodeRepository.getNodesList();
	
		if (aoNodes == null) {
			WasdiLog.debugLog("NodeResource.getAllNodes: Node list is null");
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
			@QueryParam("node") String sNodeCode, @QueryParam("userId") String sDestinationUserId) {

		WasdiLog.debugLog("NodeResource.ShareNode( Node: " + sNodeCode + ", User: " + sDestinationUserId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.debugLog("NodeResource.shareNode: invalid session");

			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
			oResult.setStringValue(MSG_ERROR_INVALID_SESSION);

			return oResult;
		}

		// Check if the node exists
		NodeRepository oNodeRepository = new NodeRepository();
		Node oNode = oNodeRepository.getNodeByCode(sNodeCode);

		if (oNode == null) {
			WasdiLog.debugLog("NodeResource.ShareNode: invalid node");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(MSG_ERROR_INVALID_NODE);

			return oResult;
		}

		// Can the user access this section?
		if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), NODE_READ)) {
			WasdiLog.debugLog("NodeResource.shareNode: " + oRequesterUser.getUserId() + " cannot access the section " + ", aborting");

			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
			oResult.setStringValue(MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_NODE);

			return oResult;
		}

		// Can the user access this resource?
		if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ADMIN_DASHBOARD)) {
			WasdiLog.debugLog("NodeResource.shareNode: " + sNodeCode + " cannot be accessed by " + oRequesterUser.getUserId() + ", aborting");

			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
			oResult.setStringValue(MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_NODE);

			return oResult;
		}

		UserRepository oUserRepository = new UserRepository();
		User oDestinationUser = oUserRepository.getUser(sDestinationUserId);

		if (oDestinationUser == null) {
			//No. So it is neither the owner or a shared one
			WasdiLog.debugLog("NodeResource.shareNode: Destination user does not exists");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER);

			return oResult;
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			if (!oUserResourcePermissionRepository.isNodeSharedWithUser(sDestinationUserId, sNodeCode)) {
				UserResourcePermission oNodeSharing =
						new UserResourcePermission("node", sNodeCode, sDestinationUserId, null, oRequesterUser.getUserId(), "write");

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
			oResult.setStringValue(MSG_ERROR_IN_INSERT_PROCESS);

			return oResult;
		}

		sendNotificationEmail(oRequesterUser.getUserId(), sDestinationUserId, oNode.getNodeCode());

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);

		return oResult;
	}

	private static void sendNotificationEmail(String sRequesterUserId, String sDestinationUserId, String sNodeName) {
		try {
			String sMercuriusAPIAddress = WasdiConfig.Current.notifications.mercuriusAPIAddress;

			if(Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
				WasdiLog.debugLog("NodeResource.sendNotificationEmail: sMercuriusAPIAddress is null");
			}
			else {

				WasdiLog.debugLog("NodeResource.sendNotificationEmail: send notification");

				MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);	
				Message oMessage = new Message();

				String sTitle = "Node " + sNodeName + " Shared";

				oMessage.setTilte(sTitle);
				
				String sSender = WasdiConfig.Current.notifications.sftpManagementMailSender;
				if (sSender==null) {
					sSender = "wasdi@wasdi.net";
				}

				oMessage.setSender(sSender);

				String sMessage = "The user " + sRequesterUserId +  " shared with you the node: " + sNodeName;

				oMessage.setMessage(sMessage);

				Integer iPositiveSucceded = 0;

				iPositiveSucceded = oAPI.sendMailDirect(sDestinationUserId, oMessage);

				WasdiLog.debugLog("NodeResource.sendNotificationEmail: notification sent with result " + iPositiveSucceded);
			}

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
			WasdiLog.debugLog("NodeResource.getEnableUsersSharedWorksace: invalid session");
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
			WasdiLog.debugLog("NodeResource.deleteUserSharedNode: invalid session");

			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
			oResult.setStringValue(MSG_ERROR_INVALID_SESSION);

			return oResult;
		}

		try {

			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);

			if (oDestinationUser == null) {
				WasdiLog.debugLog("NodeResource.deleteUserSharedNode: invalid destination user");

				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
				oResult.setStringValue(MSG_ERROR_INVALID_DESTINATION_USER);

				return oResult;
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			oUserResourcePermissionRepository.deletePermissionsByUserIdAndNodeCode(sUserId, sNodeCode);
		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeResource.deleteUserSharedNode: " + oEx);

			oResult.setIntValue(Status.INTERNAL_SERVER_ERROR.getStatusCode());
			oResult.setStringValue(MSG_ERROR_IN_DELETE_PROCESS);

			return oResult;
		}

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);
		oResult.setIntValue(Status.OK.getStatusCode());

		return oResult;

	}
	
	
}
