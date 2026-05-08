package wasdi.operations;

import org.apache.commons.lang3.exception.ExceptionUtils;

import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.GraphParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.snapopearations.WasdiGraph;

public class Graph extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		
		WasdiLog.infoLog("Graph.executeOperation");
		
		if (oParam == null) {
			WasdiLog.errorLog("Parameter is null");
			return false;
		}
		
		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Process Workspace is null");
			return false;
		}
		
        try {
        	GraphParameter oGraphParams = (GraphParameter) oParam;
        	
            WasdiGraph oGraphManager = new WasdiGraph(oGraphParams, this, oProcessWorkspace);
            oGraphManager.execute();
            
            return true;
            
        } catch (Exception oEx) {
            WasdiLog.errorLog("Graph.executeOperation: Exception", oEx);
            String sError = ExceptionUtils.getMessage(oEx);
            
         // P.Campanella 2024/01/23: log the error also on the web user interface
            if (m_oProcessWorkspaceLogger != null) {
            	if (!Utils.isNullOrEmpty(sError)) {
            		m_oProcessWorkspaceLogger.log(sError);
            	}
            }

            // P.Campanella 2018/03/30: handle exception and close the process
            updateProcessStatus(oProcessWorkspace, ProcessStatus.ERROR, 100);

            m_oSendToRabbit.SendRabbitMessage(false, LauncherOperations.GRAPH.name(), oParam.getWorkspace(), sError, oParam.getExchange());
        }
        
		return false;
	}

}
