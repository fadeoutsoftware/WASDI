package it.fadeout.rest.resources;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import it.fadeout.Wasdi;
import wasdi.shared.business.Node;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WorkspacePolicy;
import wasdi.shared.viewmodels.WorkspaceListInfoViewModel;

@Path("/node")
public class NodeResource {
	private CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();
	private WorkspacePolicy m_oWorkspacePolicy = new WorkspacePolicy();
	
	@Context
	ServletConfig m_oServletConfig;
	
	@GET
	@Path("/allnodes")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<Node> getAllNodes(
			@HeaderParam("x-session-token") String sSessionId) {
		Utils.debugLog("NodeResource.getAllNodes( Session: " + sSessionId + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			Utils.debugLog("NodeResource.getAllNodes( Session: " + sSessionId + "): invalid session");
			return null;			
		}
		
		// get list of all nodes
		NodeRepository oNodeRepository = new NodeRepository();
		List<Node> asNodes = oNodeRepository.getNodesList();
				//oProductWorkspaceRepository.getWorkspaces(sProductName);

		if (asNodes == null) {
			Utils.debugLog("NodeResource.getAllNodes: Node list is null");
			return null;
		}

		return asNodes;
	}

	
	
}
