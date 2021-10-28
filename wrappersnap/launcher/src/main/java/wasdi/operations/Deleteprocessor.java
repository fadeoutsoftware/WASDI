package wasdi.operations;

import wasdi.ConfigReader;
import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;

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
	                ConfigReader.getPropValue("DOWNLOAD_ROOT_PATH"),
	                ConfigReader.getPropValue("DOCKER_TEMPLATE_PATH"),
	                ConfigReader.getPropValue("TOMCAT_USER", "tomcat8"));
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
