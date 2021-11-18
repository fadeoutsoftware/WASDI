package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.FindIterable;

import wasdi.shared.business.Node;
import wasdi.shared.utils.Utils;

/**
 * Repository of the WASDI Nodes entities
 * @author p.campanella
 *
 */
public class NodeRepository extends MongoRepository {

	public NodeRepository () {
		m_sThisCollection = "node";
	}

	/**
	 * Create a new node
	 * @param oNode Entity
	 * @return Obj Id
	 */
	public String insertNode(Node oNode) {
		try {
			if(null == oNode) {
				Utils.debugLog("NodeRepository.InsertNode: oNode is null");
				return null;
			}	        	
			String sJSON = s_oMapper.writeValueAsString(oNode);
			Document oDocument = Document.parse(sJSON);

			getCollection(m_sThisCollection).insertOne(oDocument);
			return oDocument.getObjectId("_id").toHexString();

		} catch (Exception oEx) {
			Utils.debugLog("NodeRepository.InsertNode: "+oEx);
		}
		return "";
	}
	
	/**
	 * Delete a node by the mongo Id
	 * @param sId Id of the mongo object
	 * @return true or false
	 */
	public boolean deleteNode(String sId) {
		try {
			getCollection(m_sThisCollection).deleteOne(new Document("_id", new ObjectId(sId)));

			return true;

		} catch (Exception oEx) {
			Utils.debugLog("NodeRepository.deleteNode( "+sId+" )" +oEx);
		}

		return false;
	}
	
	/**
	 * Get a node from the WASDI code
	 * @param sCode Code of the Node
	 * @return Entity or null
	 */
	public Node getNodeByCode(String sCode) {

		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("nodeCode", sCode)).first();
			
			if (oWSDocument!=null) {
				String sJSON = oWSDocument.toJson();

				Node oNode = s_oMapper.readValue(sJSON,Node.class);

				return oNode;				
			}

		} catch (Exception oEx) {
			Utils.debugLog("NodeRepository.getNodeByCode( "+sCode+" )" +oEx.toString());
		}

		return  null;
	}
	
	/**
	 * Get the full nodes list
	 * @return List of all the nodes
	 */
	public List<Node> getNodesList() {

		final ArrayList<Node> aoReturnList = new ArrayList<Node>();
		try {

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();
			
			fillList(aoReturnList, oWSDocuments, Node.class);
			
		} catch (Exception oEx) {
			Utils.debugLog("NodeRepository.getNodesList(): " + oEx.toString());
		}

		return aoReturnList;
	}

}
