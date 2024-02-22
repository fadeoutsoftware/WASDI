package wasdi.operations;

import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
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
		
		try {
	        // redeploy User Processor
	        ProcessorParameter oParameter = (ProcessorParameter) oParam;			
			
            // First Check if processor exists
            String sProcessorName = oParameter.getName();
            String sProcessorId = oParameter.getProcessorID();

            ProcessorRepository oProcessorRepository = new ProcessorRepository();
            Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

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
	        
	        try {
	        	
	        	// In the exchange we should have the workspace from there the user requested the Redeploy
	        	String sOriginalWorkspaceId = oParam.getExchange();
	        	
	        	// Check if it is valid
	        	if (Utils.isNullOrEmpty(sOriginalWorkspaceId)==false) {
	        		
	        		// Read the workspace
	        		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
	        		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sOriginalWorkspaceId);
	        		
	        		if (oWorkspace != null) {
	        			
        				// Prepare the message
			        	String sName = oParameter.getName();
			        	
			        	if (Utils.isNullOrEmpty(sName)) sName = "Your Processor";
			        	
			            String sInfo = "Re Deploy Done<br>" + sName + " is now available";
			            
			            if (!bRet) {
			            	sInfo = "GURU MEDITATION<br>There was an error re-deploying " + sName + " :(";
			            }
	        			
	        			String sNodeCode = "wasdi";
	        			
	        			if (!Utils.isNullOrEmpty(oWorkspace.getNodeCode())) {
	        				sNodeCode = oWorkspace.getNodeCode();
	        			}	        			
	        			
	        			if (oEngine.isLocalBuild()) {
		        			// This is the computing node where the request came from?
		        			if (sNodeCode.equals(WasdiConfig.Current.nodeCode)) {
					            m_oSendToRabbit.SendRabbitMessage(bRet, LauncherOperations.INFO.name(), oParam.getExchange(), sInfo, oParam.getExchange());	        				
		        			}	        				
	        			}
	        			else {
	        				// This is the main node?
	        				if (WasdiConfig.Current.isMainNode()) {
	        					m_oSendToRabbit.SendRabbitMessage(bRet, LauncherOperations.INFO.name(), oParam.getExchange(), sInfo, oParam.getExchange());
	        				}
	        			}
	        		}	        		
	        	}
	        	
	        }
	        catch (Exception oRabbitException) {
				WasdiLog.errorLog("Redeployprocessor.executeOperation: exception sending Rabbit Message", oRabbitException);
			}
            
            return bRet;	        
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Redeployprocessor.executeOperation: exception", oEx);
		}
		
		return false;        

	}

}
