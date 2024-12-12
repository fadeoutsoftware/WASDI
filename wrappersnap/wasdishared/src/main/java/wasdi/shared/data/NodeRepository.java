package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.Node;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

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
				WasdiLog.debugLog("NodeRepository.InsertNode: oNode is null");
				return null;
			}	        	
			String sJSON = s_oMapper.writeValueAsString(oNode);
			Document oDocument = Document.parse(sJSON);

			getCollection(m_sThisCollection).insertOne(oDocument);
			return oDocument.getObjectId("_id").toHexString();

		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeRepository.InsertNode: ", oEx);
		}
		return "";
	}
	
	/**
	 * Updates a node
	 * @param oNode
	 * @return
	 */
	public boolean updateNode(Node oNode) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oNode);

			Bson oFilter = new Document("nodeCode", oNode.getNodeCode());
			Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));

			UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

			if (oResult.getModifiedCount() == 1) return true;
			else {
				WasdiLog.errorLog("NodeRepository.updateNode: indeed there was no change! ");
				return true;
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeRepository.updateNode: error ", oEx);
		}

		return false;
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
			WasdiLog.errorLog("NodeRepository.deleteNode( "+sId+" )", oEx);
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
			WasdiLog.errorLog("NodeRepository.getNodeByCode( "+sCode+" )", oEx);
		}

		return  null;
	}

	public List<Node> findNodeByPartialName(String sPartialName) {
		List<Node> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoReturnList;
		}

		Pattern regex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);

		Bson oFilterLikeNodeCode = Filters.eq("nodeCode", regex);

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(oFilterLikeNodeCode)
					.sort(new Document("nodeCode", 1));

			fillList(aoReturnList, oWSDocuments, Node.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeRepository.findNodeByPartialName", oEx);
		}

		return aoReturnList;
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
			WasdiLog.errorLog("NodeRepository.getNodesList(): ", oEx);
		}

		return aoReturnList;
	}
	

	/**
	 * Get the list of shared nodes
	 * @return List of all the nodes
	 */
	public List<Node> getSharedActiveNodesList() {

		final ArrayList<Node> aoReturnList = new ArrayList<Node>();
		try {
			
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(Filters.and(Filters.eq("shared", true), Filters.eq("active", true)));
			
			fillList(aoReturnList, oWSDocuments, Node.class);
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeRepository.getNodesList(): ", oEx);
		}

		return aoReturnList;
	}	

}
