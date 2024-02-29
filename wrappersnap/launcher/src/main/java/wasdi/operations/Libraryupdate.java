package wasdi.operations;

import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.log.WasdiLog;

public class Libraryupdate extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		WasdiLog.debugLog("Libraryupdate.executeOperation");
		
		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}

		
		try {		
	        // Update Lib
	        ProcessorParameter oParameter = (ProcessorParameter) oParam;
	        
	        WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType());
	        
	        if (!oEngine.isProcessorOnNode(oParameter)) {
                WasdiLog.errorLog("Libraryupdate.executeOperation: Processor [" + oParameter.getName() + "] not installed in this node, return");
                return true;	        	
	        }
	        	        
	        oEngine.setParameter(oParameter);
	        oEngine.setSendToRabbit(m_oSendToRabbit);
	        oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
	        oEngine.setProcessWorkspace(oProcessWorkspace);
	        boolean bRet = oEngine.libraryUpdate(oParameter);
	        
	        m_oSendToRabbit.sendRedeployDoneMessage(oParameter, bRet, oEngine.isLocalBuild());
	        
	        return bRet;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Libraryupdate.executeOperation: exception", oEx);
		}
		
        return false;
	}

}
