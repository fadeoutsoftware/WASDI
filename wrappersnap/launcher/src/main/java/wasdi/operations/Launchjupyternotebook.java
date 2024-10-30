package wasdi.operations;

import wasdi.processors.JupyterNotebookProcessorEngine;
import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.JupyterNotebookRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class Launchjupyternotebook extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		WasdiLog.infoLog("Launchjupyternotebook.executeOperation");

		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}

		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}

		try {
			ProcessorParameter oParameter = (ProcessorParameter) oParam;

			JupyterNotebookProcessorEngine oEngine = (JupyterNotebookProcessorEngine) WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType());
			oEngine.setSendToRabbit(m_oSendToRabbit);
			oEngine.setParameter(oParameter);
			oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
			oEngine.setProcessWorkspace(oProcessWorkspace);

			boolean bRet = oEngine.launchJupyterNotebook(oParameter);
			
			if (!bRet) {
				WasdiLog.warnLog("Launchjupyternotebook.executeOperation: operation was false, clean the db");
				String sWorkspaceId = oProcessWorkspace.getWorkspaceId();
				WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
				Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
				
				String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(oWorkspace.getUserId(), sWorkspaceId);

				JupyterNotebookRepository oJupyterNotebookRepository = new JupyterNotebookRepository();
				oJupyterNotebookRepository.deleteJupyterNotebook(sJupyterNotebookCode);
			}

			try {
				// In the exchange we should have the workspace on which the user requested
				// the launch of the Jupyter Notebook
				String sOriginalWorkspaceId = oParam.getExchange();

				// Check if it is valid
				if (!Utils.isNullOrEmpty(sOriginalWorkspaceId)) {
					// Read the workspace
					WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
					Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sOriginalWorkspaceId);

					if (oWorkspace != null) {
						String sNodeCode = "wasdi";

						if (!Utils.isNullOrEmpty(oWorkspace.getNodeCode())) {
							sNodeCode = oWorkspace.getNodeCode();
						}

						// This is the computing node where the request came from?
						if (sNodeCode.equals(WasdiConfig.Current.nodeCode)) {
							// Notify the user
							String sName = oParameter.getName();

							if (Utils.isNullOrEmpty(sName)) {
								sName = "Your Jupyter Notebook";
							}

							String sInfo = "JUPYTER NOTEBOOK STARTED<br>" + sName + " IS NOW AVAILABLE";

							if (!bRet) {
								sInfo = "GURU MEDITATION<br>ERROR STARTING JUPYTER NOTEBOOK " + sName + " :(";
							}

							m_oSendToRabbit.SendRabbitMessage(bRet, LauncherOperations.INFO.name(), oParam.getExchange(), sInfo, oParam.getExchange());
							m_oSendToRabbit.SendRabbitMessage(bRet, LauncherOperations.LAUNCHJUPYTERNOTEBOOK.name(), oParam.getExchange(), "", oParam.getExchange());
						}

					}
				}
			} 
			catch (Exception oRabbitException) {
				WasdiLog.errorLog("Launchjupyternotebook.executeOperation: exception sending Rabbit Message", oRabbitException);
			}
			
			return bRet;

		} catch (Exception oEx) {
			WasdiLog.errorLog("Launchjupyternotebook.executeOperation: exception", oEx);
		}

		return false;
	}

}
