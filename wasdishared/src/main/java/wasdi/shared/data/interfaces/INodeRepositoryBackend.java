package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.Node;

/**
 * Backend contract for node repository.
 */
public interface INodeRepositoryBackend {

	String insertNode(Node oNode);

	boolean updateNode(Node oNode);

	boolean deleteNode(String sId);

	Node getNodeByCode(String sCode);

	List<Node> findNodeByPartialName(String sPartialName);

	List<Node> getNodesList();

	List<Node> getSharedActiveNodesList();
}
