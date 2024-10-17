package wasdi.operations;

import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.data.ProcessorRepository;
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
		
		ProcessorRepository oProcessorRepository = null;
		Processor oProcessor = null;
		
		try {		
	        // Update Lib
	        ProcessorParameter oParameter = (ProcessorParameter) oParam;
	        
            // First Check if processor exists
            String sProcessorId = oParameter.getProcessorID();

            oProcessorRepository = new ProcessorRepository();
            oProcessor = oProcessorRepository.getProcessor(sProcessorId);

            // Check processor
            if (oProcessor == null) {
                WasdiLog.errorLog("Libraryupdate.executeOperation: oProcessor is null [" + sProcessorId + "]");
                return false;
            }
	        
	        
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
		finally {
			// In any case, in the finally, we set the deployment on going flag to false
			if (oParam!=null) {
				ProcessorParameter oParameter = (ProcessorParameter) oParam;
	            oProcessorRepository = new ProcessorRepository();
	            oProcessor = oProcessorRepository.getProcessor(oParameter.getProcessorID());
	            oProcessor.setDeploymentOngoing(false);
				if (oProcessorRepository.updateProcessor(oProcessor)) {
		        	WasdiLog.debugLog("Libraryupdate.executeOperation: flag for tracking the in-progress deployment set back to false");
		        } else {
		        	WasdiLog.warnLog("Libraryupdate.executeOperation: could not set back to false the flag for tracking the in-progress deployment");
		        }	            
			}            
		}		
		
        return false;
	}

}
