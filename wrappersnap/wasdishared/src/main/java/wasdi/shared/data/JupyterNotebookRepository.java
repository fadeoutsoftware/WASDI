package wasdi.shared.data;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.JupyterNotebook;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class JupyterNotebookRepository extends MongoRepository {

	public JupyterNotebookRepository() {
		m_sThisCollection = "jupyternotebook";
	}

	/**
	 * Create a new JupyterNotebook
	 * @param oJupyterNotebook JupyterNotebook Entity
	 * @return True or False in case of exception
	 */
	public boolean insertJupyterNotebook(JupyterNotebook oJupyterNotebook) {
		try {
			String sJSON = s_oMapper.writeValueAsString(oJupyterNotebook);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("JupyterNotebookRepository.insertJupyterNotebook: error", oEx);
		}

		return false;
	}

	/**
	 * Get a JupyterNotebook by its WASDI Id
	 * @param sJupyterNotebookId WASDI id of the JupyterNotebook
	 * @return the entity
	 */
	public JupyterNotebook getJupyterNotebook(String sJupyterNotebookId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("code", sJupyterNotebookId)).first();

			if (oWSDocument != null) {
				String sJSON = oWSDocument.toJson();
				return s_oMapper.readValue(sJSON, JupyterNotebook.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("JupyterNotebookRepository.getJupyterNotebook: error", oEx);
		}

		return  null;
	}

	/**
	 * Get a JupyterNotebook by code
	 * @param scode the code of the JupyterNotebook
	 * @return the JupyterNotebook Entity
	 */
	public JupyterNotebook getJupyterNotebookByCode(String sCode) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("code", sCode)).first();

			if (oWSDocument != null) {
				String sJSON = oWSDocument.toJson();
				return s_oMapper.readValue(sJSON, JupyterNotebook.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("JupyterNotebookRepository.getJupyterNotebookByCode: error", oEx);
		}

		return  null;
	}

	/**
	 * Update a JupyterNotebook
	 * @param oJupyterNotebook entity to update
	 * @return True or False in case of exception
	 */
	public boolean updateJupyterNotebook(JupyterNotebook oJupyterNotebook) {
		try {
			String sJSON = s_oMapper.writeValueAsString(oJupyterNotebook);

			Bson oFilter = new Document("code", oJupyterNotebook.getCode());
			Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));

			UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

			if (oResult.getModifiedCount() == 1) return  true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("JupyterNotebookRepository.updateJupyterNotebook: error", oEx);
		}

		return  false;
	}

	/**
	 * Delete a JupyterNotebook by WASDI Id
	 * @param sJupyterNotebookId JupyterNotebook Id
	 * @return True or false in case of exception
	 */
	public boolean deleteJupyterNotebook(String sJupyterNotebookId) {
		if (Utils.isNullOrEmpty(sJupyterNotebookId)) return false;

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("code", sJupyterNotebookId));

			if (oDeleteResult != null) {
				if (oDeleteResult.getDeletedCount() == 1) {
					return  true;
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("JupyterNotebookRepository.deleteJupyterNotebook: error", oEx);
		}

		return  false;
	}
	
	/**
	 * Delete a JupyterNotebook by User Id
	 * @param sJupyterNotebookId JupyterNotebook Id
	 * @return True or false in case of exception
	 */
	public int deleteJupyterNotebookByUser(String sJupyterNotebookId) {
		if (Utils.isNullOrEmpty(sJupyterNotebookId)) return 0;

		try {
			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("userId", sJupyterNotebookId));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("JupyterNotebookRepository.deleteJupyterNotebook: error", oEx);
		}

		return 0;
	}	

}
