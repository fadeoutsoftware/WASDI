package wasdi.shared.data;

import wasdi.shared.business.JupyterNotebook;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IJupyterNotebookRepositoryBackend;

public class JupyterNotebookRepository {

	private final IJupyterNotebookRepositoryBackend m_oBackend;

	public JupyterNotebookRepository() {
		m_oBackend = createBackend();
	}

	private IJupyterNotebookRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createJupyterNotebookRepository();
	}

	/**
	 * Create a new JupyterNotebook
	 * @param oJupyterNotebook JupyterNotebook Entity
	 * @return True or False in case of exception
	 */
	public boolean insertJupyterNotebook(JupyterNotebook oJupyterNotebook) {
		return m_oBackend.insertJupyterNotebook(oJupyterNotebook);
	}

	/**
	 * Get a JupyterNotebook by its WASDI Id
	 * @param sJupyterNotebookId WASDI id of the JupyterNotebook
	 * @return the entity
	 */
	public JupyterNotebook getJupyterNotebook(String sJupyterNotebookId) {
		return m_oBackend.getJupyterNotebook(sJupyterNotebookId);
	}

	/**
	 * Get a JupyterNotebook by code
	 * @param scode the code of the JupyterNotebook
	 * @return the JupyterNotebook Entity
	 */
	public JupyterNotebook getJupyterNotebookByCode(String sCode) {
		return m_oBackend.getJupyterNotebookByCode(sCode);
	}

	/**
	 * Update a JupyterNotebook
	 * @param oJupyterNotebook entity to update
	 * @return True or False in case of exception
	 */
	public boolean updateJupyterNotebook(JupyterNotebook oJupyterNotebook) {
		return m_oBackend.updateJupyterNotebook(oJupyterNotebook);
	}

	/**
	 * Delete a JupyterNotebook by WASDI Id
	 * @param sJupyterNotebookId JupyterNotebook Id
	 * @return True or false in case of exception
	 */
	public boolean deleteJupyterNotebook(String sJupyterNotebookId) {
		return m_oBackend.deleteJupyterNotebook(sJupyterNotebookId);
	}
	
	/**
	 * Delete a JupyterNotebook by User Id
	 * @param sJupyterNotebookId JupyterNotebook Id
	 * @return True or false in case of exception
	 */
	public int deleteJupyterNotebookByUser(String sJupyterNotebookId) {
		return m_oBackend.deleteJupyterNotebookByUser(sJupyterNotebookId);
	}	

}

