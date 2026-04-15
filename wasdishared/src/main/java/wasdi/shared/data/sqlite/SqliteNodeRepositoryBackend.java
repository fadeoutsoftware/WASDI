package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import wasdi.shared.business.Node;
import wasdi.shared.data.interfaces.INodeRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class SqliteNodeRepositoryBackend extends SqliteRepository implements INodeRepositoryBackend {

	public SqliteNodeRepositoryBackend() {
		m_sThisCollection = "node";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public String insertNode(Node oNode) {
		try {
			if (null == oNode) {
				WasdiLog.debugLog("NodeRepository.InsertNode: oNode is null");
				return null;
			}

			insert(m_sThisCollection, oNode.getNodeCode(), oNode);
			return oNode.getNodeCode();

		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeRepository.InsertNode: ", oEx);
		}
		return "";
	}

	@Override
	public boolean updateNode(Node oNode) {
		try {
			return updateWhere(m_sThisCollection, "nodeCode", oNode.getNodeCode(), oNode);
		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeRepository.updateNode: error ", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteNode(String sId) {
		try {
			return deleteById(m_sThisCollection, sId);
		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeRepository.deleteNode( " + sId + " )", oEx);
		}

		return false;
	}

	@Override
	public Node getNodeByCode(String sCode) {
		try {
			return findOneWhere(m_sThisCollection, "nodeCode", sCode, Node.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeRepository.getNodeByCode( " + sCode + " )", oEx);
		}

		return null;
	}

	@Override
	public List<Node> findNodeByPartialName(String sPartialName) {
		List<Node> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoReturnList;
		}

		try {
			String sQuery = "SELECT data FROM " + m_sThisCollection
					+ " WHERE LOWER(json_extract(data,'$.nodeCode')) LIKE LOWER(?)"
					+ " ORDER BY json_extract(data,'$.nodeCode') ASC";

			return queryList(sQuery, Arrays.asList("%" + sPartialName + "%"), Node.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeRepository.findNodeByPartialName", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<Node> getNodesList() {
		try {
			return findAll(m_sThisCollection, Node.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeRepository.getNodesList(): ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<Node> getSharedActiveNodesList() {
		try {
			String sQuery = "SELECT data FROM " + m_sThisCollection
					+ " WHERE json_extract(data,'$.shared') = 1"
					+ " AND json_extract(data,'$.active') = 1";

			return queryList(sQuery, Arrays.asList(), Node.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("NodeRepository.getSharedActiveNodesList(): ", oEx);
		}

		return new ArrayList<>();
	}
}
