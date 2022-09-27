package wasdi.operations;

import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;

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
	        oEngine.setSendToRabbit(m_oSendToRabbit);
	        oEngine.setParameter(oParameter);
	        oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
	        oEngine.setProcessWorkspace(oProcessWorkspace);
	        
	        boolean bRet = oEngine.deploy(oParameter);
	        
	        try {
	        	
	        	String sName = oParameter.getName();
	        	
	        	if (Utils.isNullOrEmpty(sName)) sName = "Your Processor";
	        	
	            String sInfo = "Deploy Done<br>" + sName + " is now available";
	            
	            if (!bRet) {
	            	sInfo = "GURU MEDITATION<br>There was an error deploying " + sName + " :(";
	            } else {
	            	
	            	String sOriginalWorkspaceId = oParam.getExchange();
	            	if (!Utils.isNullOrEmpty(sOriginalWorkspaceId)) {
	            		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
	            		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sOriginalWorkspaceId);

	            		if (oWorkspace != null) {
	            			String sNodeCode = "wasdi";

	            			if (!Utils.isNullOrEmpty(oWorkspace.getNodeCode())) {
	            				sNodeCode = oWorkspace.getNodeCode();
	            			}

	            			m_oLocalLogger.debug("Deployprocessor.executeOperation | sNodeCode: " + sNodeCode);
	            			m_oLocalLogger.debug("Deployprocessor.executeOperation | WasdiConfig.Current.nodeCode: " + WasdiConfig.Current.nodeCode);

            				if (sNodeCode.equals("wasdi")) {
            					Thread.sleep(2000);

            					oEngine.refreshPackagesInfo(oParameter);
            				}
	            		}
	            	}

	            }
	            
	            m_oSendToRabbit.SendRabbitMessage(bRet, LauncherOperations.INFO.name(), oParam.getExchange(), sInfo, oParam.getExchange());	        	
	        }
	        catch (Exception oRabbitException) {
				m_oLocalLogger.error("Deployprocessor.executeOperation: exception sending Rabbit Message", oRabbitException);
			}
            
            return bRet;
	        
		}
		catch (Exception oEx) {
			m_oLocalLogger.error("Deployprocessor.executeOperation: exception", oEx);
		}
		
		return false;
	}

}
