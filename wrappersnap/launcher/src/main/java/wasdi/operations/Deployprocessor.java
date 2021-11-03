package wasdi.operations;

import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;

/**
 * Deploy Processor Operation
 * 
 * Takes a ProcessorParameter.
 * 
 * This operation deploys a Processor on the local node.
 * It uses the Processors class hierarcy.
 * 
 * @author p.campanella
 *
 */
public class Deployprocessor extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		m_oLocalLogger.debug("Deployprocessor.executeOperation");
		
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}

		
		try {
	        // Deploy new user processor
	        ProcessorParameter oParameter = (ProcessorParameter) oParam;
	        WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType());
	        oEngine.setParameter(oParameter);
	        oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
	        oEngine.setProcessWorkspace(oProcessWorkspace);
	        return oEngine.deploy(oParameter);			
		}
		catch (Exception oEx) {
			m_oLocalLogger.error("Deployprocessor.executeOperation: exception", oEx);
		}
		
		return false;
	}

}
