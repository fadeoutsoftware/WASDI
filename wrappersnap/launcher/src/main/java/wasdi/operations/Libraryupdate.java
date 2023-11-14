package wasdi.operations;

import wasdi.LauncherMain;
import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;

public class Libraryupdate extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		m_oLocalLogger.debug("Libraryupdate.executeOperation");
		
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}

		
		try {		
	        // Update Lib
	        ProcessorParameter oParameter = (ProcessorParameter) oParam;
	        
	        WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType());
	        
	        
	        if (!oEngine.isProcessorOnNode(oParameter)) {
                LauncherMain.s_oLogger.error("Libraryupdate.executeOperation: Processor [" + oParameter.getName() + "] not installed in this node, return");
                return true;	        	
	        }
	        	        
	        oEngine.setParameter(oParameter);
	        oEngine.setSendToRabbit(m_oSendToRabbit);
	        oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
	        oEngine.setProcessWorkspace(oProcessWorkspace);
	        return oEngine.libraryUpdate(oParameter);
		}
		catch (Exception oEx) {
			m_oLocalLogger.error("Libraryupdate.executeOperation: exception", oEx);
		}
		
        return false;
	}

}
