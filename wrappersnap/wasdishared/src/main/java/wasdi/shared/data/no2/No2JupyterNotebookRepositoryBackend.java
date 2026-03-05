package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.JupyterNotebook;
import wasdi.shared.data.interfaces.IJupyterNotebookRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for Jupyter notebook repository.
 */
public class No2JupyterNotebookRepositoryBackend extends No2Repository implements IJupyterNotebookRepositoryBackend {

	private static final String s_sCollectionName = "jupyternotebook";

	@Override
	public boolean insertJupyterNotebook(JupyterNotebook oJupyterNotebook) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oJupyterNotebook == null) {
				return false;
			}

			oCollection.insert(toDocument(oJupyterNotebook));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2JupyterNotebookRepositoryBackend.insertJupyterNotebook: error", oEx);
		}

		return false;
	}

	@Override
	public JupyterNotebook getJupyterNotebook(String sJupyterNotebookId) {
		return getJupyterNotebookByCode(sJupyterNotebookId);
	}

	@Override
	public JupyterNotebook getJupyterNotebookByCode(String sCode) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("code").eq(sCode))) {
				return fromDocument(oDocument, JupyterNotebook.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2JupyterNotebookRepositoryBackend.getJupyterNotebookByCode: error", oEx);
		}

		return null;
	}

	@Override
	public boolean updateJupyterNotebook(JupyterNotebook oJupyterNotebook) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oJupyterNotebook == null) {
				return false;
			}

			oCollection.update(where("code").eq(oJupyterNotebook.getCode()), toDocument(oJupyterNotebook));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2JupyterNotebookRepositoryBackend.updateJupyterNotebook: error", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteJupyterNotebook(String sJupyterNotebookId) {
		if (Utils.isNullOrEmpty(sJupyterNotebookId)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.remove(where("code").eq(sJupyterNotebookId));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2JupyterNotebookRepositoryBackend.deleteJupyterNotebook: error", oEx);
		}

		return false;
	}

	@Override
	public int deleteJupyterNotebookByUser(String sJupyterNotebookId) {
		if (Utils.isNullOrEmpty(sJupyterNotebookId)) {
			return 0;
		}

		int iDeleted = 0;
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return 0;
			}

			for (Document oDocument : oCollection.find(where("userId").eq(sJupyterNotebookId))) {
				if (oDocument != null) {
					iDeleted++;
				}
			}

			oCollection.remove(where("userId").eq(sJupyterNotebookId));
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2JupyterNotebookRepositoryBackend.deleteJupyterNotebookByUser: error", oEx);
		}

		return iDeleted;
	}
}
