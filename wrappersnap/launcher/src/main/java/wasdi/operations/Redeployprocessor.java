package wasdi.operations;

import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.log.WasdiLog;

public class Redeployprocessor extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		WasdiLog.debugLog("Redeployprocessor.executeOperation");
		
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
	        // redeploy User Processor
	        ProcessorParameter oParameter = (ProcessorParameter) oParam;			
			
            // First Check if processor exists
            String sProcessorName = oParameter.getName();
            String sProcessorId = oParameter.getProcessorID();

            oProcessorRepository = new ProcessorRepository();
            oProcessor = oProcessorRepository.getProcessor(sProcessorId);

            // Check processor
            if (oProcessor == null) {
                WasdiLog.errorLog("Redeployprocessor.executeOperation: oProcessor is null [" + sProcessorId + "]");
                return false;
            }

	        WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType());
	        
	        if (!oEngine.isProcessorOnNode(oParameter)) {
                WasdiLog.errorLog("Redeployprocessor.executeOperation: Processor [" + sProcessorName + "] not installed in this node, return");
                return true;
	        }
	        
	        oEngine.setSendToRabbit(m_oSendToRabbit);
	        oEngine.setParameter(oParameter);
	        oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
	        oEngine.setProcessWorkspace(oProcessWorkspace);
	        
	        boolean bRet =  oEngine.redeploy(oParameter);
	        
	        m_oSendToRabbit.sendRedeployDoneMessage(oParameter, bRet, oEngine.isLocalBuild());	     
	        
	        oProcessor.setDeploymentOngoing(false);
	        if (oProcessorRepository.updateProcessor(oProcessor)) {
	        	WasdiLog.debugLog("Redeployprocessor.executeOperation: flag for tracking the in-progress deployment set back to false");
	        } else {
	        	WasdiLog.warnLog("Redeployprocessor.executeOperation: could not set back to false the flag for tracking the in-progress deployment");
	        }

            return bRet;	        
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Redeployprocessor.executeOperation: exception", oEx);
		} 
		finally {
			if (oProcessorRepository != null && oProcessor != null) {
				if (oProcessor.isDeploymentOngoing())  {
					oProcessor.setDeploymentOngoing(false);
					if (oProcessorRepository.updateProcessor(oProcessor)) {
			        	WasdiLog.debugLog("Redeployprocessor.executeOperation: flag for tracking the in-progress deployment set back to false after after exception handling");
			        } else {
			        	WasdiLog.warnLog("Redeployprocessor.executeOperation: could not set back to false the flag for tracking the in-progress deployment after exception handling");
			        }
				}
				
			}
		}
		
		return false;        

	}

}
