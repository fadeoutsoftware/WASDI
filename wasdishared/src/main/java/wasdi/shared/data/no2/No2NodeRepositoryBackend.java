package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.Node;
import wasdi.shared.data.interfaces.INodeRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for node repository.
 */
public class No2NodeRepositoryBackend extends No2Repository implements INodeRepositoryBackend {

	private static final String s_sCollectionName = "node";

	@Override
	public String insertNode(Node oNode) {
		try {
			if (oNode == null) {
				return "";
			}

			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return "";
			}

			oCollection.insert(toDocument(oNode));
			return oNode.getNodeCode();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2NodeRepositoryBackend.insertNode: error", oEx);
		}

		return "";
	}

	@Override
	public boolean updateNode(Node oNode) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oNode == null) {
				return false;
			}

			oCollection.update(where("nodeCode").eq(oNode.getNodeCode()), toDocument(oNode));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2NodeRepositoryBackend.updateNode: error", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteNode(String sId) {
		if (Utils.isNullOrEmpty(sId)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			// During migration, callers may pass either node code or legacy identifier.
			oCollection.remove(where("nodeCode").eq(sId));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2NodeRepositoryBackend.deleteNode: error", oEx);
		}

		return false;
	}

	@Override
	public Node getNodeByCode(String sCode) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("nodeCode").eq(sCode))) {
				return fromDocument(oDocument, Node.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2NodeRepositoryBackend.getNodeByCode: error", oEx);
		}

		return null;
	}

	@Override
	public List<Node> findNodeByPartialName(String sPartialName) {
		List<Node> aoResults = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoResults;
		}

		String sLookup = sPartialName.toLowerCase();

		for (Node oNode : getNodesList()) {
			if (oNode == null) {
				continue;
			}

			String sNodeCode = oNode.getNodeCode() != null ? oNode.getNodeCode().toLowerCase() : "";
			if (sNodeCode.contains(sLookup)) {
				aoResults.add(oNode);
			}
		}

		aoResults.sort(Comparator.comparing(Node::getNodeCode, Comparator.nullsLast(String::compareToIgnoreCase)));
		return aoResults;
	}

	@Override
	public List<Node> getNodesList() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find() : null;
			List<Node> aoNodes = toList(oCursor, Node.class);
			aoNodes.sort(Comparator.comparing(Node::getNodeCode, Comparator.nullsLast(String::compareToIgnoreCase)));
			return aoNodes;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2NodeRepositoryBackend.getNodesList: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<Node> getSharedActiveNodesList() {
		List<Node> aoResults = new ArrayList<>();

		for (Node oNode : getNodesList()) {
			if (oNode != null && oNode.getShared() && oNode.getActive()) {
				aoResults.add(oNode);
			}
		}

		return aoResults;
	}
}
