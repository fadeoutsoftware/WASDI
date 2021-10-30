package wasdi.operations;

import wasdi.ConfigReader;
import wasdi.LauncherMain;
import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;

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
		
		m_oLocalLogger.debug("Deleteprocessor.executeOperation");
		
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}

		
		try {		
	        // Delete User Processor
	        ProcessorParameter oParameter = (ProcessorParameter) oParam;
	        WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType(),
	        		WasdiConfig.s_oConfig.paths.DownloadRootPath,
	        		WasdiConfig.s_oConfig.paths.DOCKER_TEMPLATE_PATH,
	        		WasdiConfig.s_oConfig.TOMCAT_USER);
	        oEngine.setParameter(oParameter);
	        oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
	        oEngine.setProcessWorkspace(oProcessWorkspace);
	        return oEngine.delete(oParameter);
		}
		catch (Exception oEx) {
			m_oLocalLogger.error("Deleteprocessor.executeOperation: exception", oEx);
		}
		
		return false;
		
	}

}
