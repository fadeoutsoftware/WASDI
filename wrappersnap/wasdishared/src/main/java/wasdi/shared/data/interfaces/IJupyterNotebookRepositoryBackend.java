package wasdi.shared.data.interfaces;

import wasdi.shared.business.JupyterNotebook;

/**
 * Backend contract for Jupyter notebook repository.
 */
public interface IJupyterNotebookRepositoryBackend {

	boolean insertJupyterNotebook(JupyterNotebook oJupyterNotebook);

	JupyterNotebook getJupyterNotebook(String sJupyterNotebookId);

	JupyterNotebook getJupyterNotebookByCode(String sCode);

	boolean updateJupyterNotebook(JupyterNotebook oJupyterNotebook);

	boolean deleteJupyterNotebook(String sJupyterNotebookId);

	int deleteJupyterNotebookByUser(String sJupyterNotebookId);
}
