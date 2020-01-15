package wasdi.shared.data;

import org.bson.Document;
import org.bson.types.ObjectId;

import wasdi.shared.business.Node;
import wasdi.shared.utils.Utils;

public class NodeRepository extends MongoRepository {
	
	   public String insertNode(Node oNode) {
	        try {
	        	if(null == oNode) {
	        		Utils.debugLog("NodeRepository.InsertNode: oNode is null");
	        		return null;
	        	}	        	
	            String sJSON = s_oMapper.writeValueAsString(oNode);
	            Document oDocument = Document.parse(sJSON);
	            
	            getCollection("node").insertOne(oDocument);
	            return oDocument.getObjectId("_id").toHexString();

	        } catch (Exception oEx) {
	            Utils.debugLog("NodeRepository.InsertNode: "+oEx);
	        }
	        return "";
	    }

	    public boolean deleteNode(String sId) {
	        try {
	            getCollection("node").deleteOne(new Document("_id", new ObjectId(sId)));

	            return true;

	        } catch (Exception oEx) {
	        	Utils.debugLog("NodeRepository.InsertNode( "+sId+" )" +oEx);
	        }

	        return false;
	    }

	    public Node getNodeByCode(String sCode) {

	        try {
	            Document oWSDocument = getCollection("node").find(new Document("nodeCode", sCode)).first();

	            String sJSON = oWSDocument.toJson();

	            Node oNode = s_oMapper.readValue(sJSON,Node.class);

	            return oNode;
	        } catch (Exception oEx) {
	            oEx.printStackTrace();
	        }

	        return  null;
	    }
}
