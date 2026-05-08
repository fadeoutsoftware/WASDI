package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.Node;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.INodeRepositoryBackend;

/**
 * Repository of the WASDI Nodes entities
 * @author p.campanella
 *
 */
public class NodeRepository {

	private final INodeRepositoryBackend m_oBackend;

	public NodeRepository () {
		m_oBackend = createBackend();
	}

	private INodeRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createNodeRepository();
	}

	/**
	 * Create a new node
	 * @param oNode Entity
	 * @return Obj Id
	 */
	public String insertNode(Node oNode) {
		return m_oBackend.insertNode(oNode);
	}
	
	/**
	 * Updates a node
	 * @param oNode
	 * @return
	 */
	public boolean updateNode(Node oNode) {
		return m_oBackend.updateNode(oNode);
	}
	
	/**
	 * Delete a node by the mongo Id
	 * @param sId Id of the mongo object
	 * @return true or false
	 */
	public boolean deleteNode(String sId) {
		return m_oBackend.deleteNode(sId);
	}
	
	/**
	 * Get a node from the WASDI code
	 * @param sCode Code of the Node
	 * @return Entity or null
	 */
	public Node getNodeByCode(String sCode) {
		return m_oBackend.getNodeByCode(sCode);
	}

	public List<Node> findNodeByPartialName(String sPartialName) {
		return m_oBackend.findNodeByPartialName(sPartialName);
	}

	/**
	 * Get the full nodes list
	 * @return List of all the nodes
	 */
	public List<Node> getNodesList() {
		return m_oBackend.getNodesList();
	}
	

	/**
	 * Get the list of shared nodes
	 * @return List of all the nodes
	 */
	public List<Node> getSharedActiveNodesList() {
		return m_oBackend.getSharedActiveNodesList();
	}	

}

