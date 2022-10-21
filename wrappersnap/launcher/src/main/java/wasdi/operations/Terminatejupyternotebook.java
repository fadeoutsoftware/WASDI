package wasdi.operations;

import wasdi.processors.JupyterNotebookProcessorEngine;
import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;

public class Terminatejupyternotebook extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		m_oLocalLogger.debug("Terminatejupyternotebook.executeOperation");

		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}

		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}

		try {
			ProcessorParameter oParameter = (ProcessorParameter) oParam;

			JupyterNotebookProcessorEngine oEngine = (JupyterNotebookProcessorEngine) WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType());
			oEngine.setSendToRabbit(m_oSendToRabbit);
			oEngine.setParameter(oParameter);
			oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
			oEngine.setProcessWorkspace(oProcessWorkspace);

			boolean bRet = oEngine.terminateJupyterNotebook(oParameter);
			
			m_oLocalLogger.error("Terminatejupyternotebook.executeOperation: delete result " + bRet);
			
			return true;
			
		} catch (Exception oEx) {
			m_oLocalLogger.error("Terminatejupyternotebook.executeOperation: exception", oEx);
		}

		return false;
	}

}
