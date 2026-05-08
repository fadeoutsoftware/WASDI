package wasdi.operations;

import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Delete Processor Operation
 * 
 * Takes a ProcessorParameter.
 * 
 * This operation deletes a Processor on the local node.
 * It uses the Processors class hierarcy.
 * 
 * 
 * @author p.campanella
 *
 */
public class Deleteprocessor extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		WasdiLog.infoLog("Deleteprocessor.executeOperation");
		
		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}

		boolean bRet = false;
		
		try {		
	        // Delete User Processor
	        ProcessorParameter oParameter = (ProcessorParameter) oParam;
	        WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType());
	        oEngine.setSendToRabbit(m_oSendToRabbit);
	        oEngine.setParameter(oParameter);
	        oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
	        oEngine.setProcessWorkspace(oProcessWorkspace);
	        bRet = oEngine.delete(oParameter);
	                	
        	// Check if we are in the main node
        	if (WasdiConfig.Current.isMainNode()) {
				// Notify the user
	        	String sName = oParameter.getName();
	        	
	        	if (Utils.isNullOrEmpty(sName)) sName = "Your Processor";
	        	
	            String sInfo = "Delete App<br>" + sName + " has been deleted";
	            
	            if (!bRet) {
	            	sInfo = "GURU MEDITATION<br>There was an error deleting" + sName + " :(";
	            }
	            
	            m_oSendToRabbit.SendRabbitMessage(bRet, LauncherOperations.DELETEPROCESSOR.name(), oParam.getExchange(), sInfo, oParam.getExchange());	        				        		
        	}	        
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Deleteprocessor.executeOperation: exception", oEx);
		}
		
		return bRet;
		
	}

}
