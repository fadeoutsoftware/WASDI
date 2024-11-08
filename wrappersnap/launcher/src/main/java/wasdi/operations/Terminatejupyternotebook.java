package wasdi.operations;

import wasdi.processors.JupyterNotebookProcessorEngine;
import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.log.WasdiLog;

public class Terminatejupyternotebook extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		WasdiLog.infoLog("Terminatejupyternotebook.executeOperation");

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

			boolean bRet = oEngine.terminateJupyterNotebook(oParameter);
			
			WasdiLog.debugLog("Terminatejupyternotebook.executeOperation: delete result " + bRet);
			
			return true;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("Terminatejupyternotebook.executeOperation: exception", oEx);
		}

		return false;
	}

}
