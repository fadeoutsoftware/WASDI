package wasdi.operations;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.GraphParameter;
import wasdi.snapopearations.WasdiGraph;

public class Graph extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		m_oLocalLogger.debug("Graph.executeOperation");
		
		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}
		
        try {
        	
        	GraphParameter oGraphParams = (GraphParameter) oParam;
        	
            WasdiGraph oGraphManager = new WasdiGraph(oGraphParams, m_oSendToRabbit, m_oProcessWorkspaceLogger, oProcessWorkspace);
            oGraphManager.execute();
            
            return true;
            
        } catch (Exception oEx) {
            m_oLocalLogger.error("Graph.executeOperation: Exception", oEx);
            String sError = org.apache.commons.lang.exception.ExceptionUtils.getMessage(oEx);

            // P.Campanella 2018/03/30: handle exception and close the process
            updateProcessStatus(m_oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);

            if (m_oSendToRabbit != null) {
            	m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.GRAPH.name(), oParam.getWorkspace(), sError, oParam.getExchange());
            }

        }
        
		return false;
	}

}
