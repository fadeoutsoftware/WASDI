package it.fadeout.rest.resources;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import it.fadeout.Wasdi;
import wasdi.shared.business.Node;
import wasdi.shared.business.User;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.NodeViewModel;

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
		
		Utils.debugLog("NodeResource.getAllNodes( Session: " + sSessionId + ")");
		
		// Check the user
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			Utils.debugLog("NodeResource.getAllNodes( Session: " + sSessionId + "): invalid session");
			return null;			
		}
		
		// get list of all active nodes
		NodeRepository oNodeRepository = new NodeRepository();
		List<Node> asNodes = oNodeRepository.getNodesList();
	
		if (asNodes == null) {
			Utils.debugLog("NodeResource.getAllNodes: Node list is null");
			return null;
		}
		
		// returning list
		List<NodeViewModel> aoNodeViewModelList = new ArrayList<>();
		
		// For all the nodes
		for (Node node:asNodes) {
			
			// checks whether the node is active
			if (node.getActive()) {  
				
				// Create the view model and fill it
				NodeViewModel oNodeViewModel = new NodeViewModel();
				
				if (node.getCloudProvider()!=null) {
					oNodeViewModel.setCloudProvider(node.getCloudProvider());
				}
				else {
					oNodeViewModel.setCloudProvider(node.getNodeCode());
				}
				
				oNodeViewModel.setNodeCode(node.getNodeCode());
				
				// Add to the return list
				aoNodeViewModelList.add(oNodeViewModel);
			}
		}
		
		// done, return the list to the user
		return aoNodeViewModelList;
	}

	
	
}
