package wasdi.operations;

import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.log.WasdiLog;

public class Libraryupdate extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		WasdiLog.infoLog("Libraryupdate.executeOperation");
		
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
		boolean bRet = false;
		WasdiProcessorEngine oEngine = null;
		
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
	        
	        oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType());
	        
	        if (!oEngine.isProcessorOnNode(oParameter)) {
                WasdiLog.errorLog("Libraryupdate.executeOperation: Processor [" + oParameter.getName() + "] not installed in this node, return");
                return true;	        	
	        }
	        	        
	        oEngine.setParameter(oParameter);
	        oEngine.setSendToRabbit(m_oSendToRabbit);
	        oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
	        oEngine.setProcessWorkspace(oProcessWorkspace);
	        bRet = oEngine.libraryUpdate(oParameter);
	        
	        return bRet;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Libraryupdate.executeOperation: exception", oEx);
		}
		finally {
			// In any case, in the finally, we set the deployment on going flag to false
			if (oParam!=null) {
				if (WasdiConfig.Current.isMainNode()) {
					ProcessorParameter oParameter = (ProcessorParameter) oParam;
		            oProcessorRepository = new ProcessorRepository();
		            oProcessor = oProcessorRepository.getProcessor(oParameter.getProcessorID());
		            oProcessor.setDeploymentOngoing(false);
					if (oProcessorRepository.updateProcessor(oProcessor)) {
			        	WasdiLog.debugLog("Libraryupdate.executeOperation: flag for tracking the in-progress deployment set back to false");
			        } else {
			        	WasdiLog.warnLog("Libraryupdate.executeOperation: could not set back to false the flag for tracking the in-progress deployment");
			        }
					
					boolean bIsLocalBuild = false;
					if (oEngine != null) {
						bIsLocalBuild = oEngine.isLocalBuild();
					}
					
					m_oSendToRabbit.sendRedeployDoneMessage(oParameter, bRet, bIsLocalBuild);
					
				}
			}            
		}		
		
        return false;
	}

}
