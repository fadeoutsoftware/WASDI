package wasdi.operations;

import java.util.Map;

import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.LauncherOperations;
import wasdi.shared.apiclients.pip.PipApiClient;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;

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

	            			if (sNodeCode.equals(WasdiConfig.Current.nodeCode)) {
	        	            	if (sNodeCode.equals("wasdi")) {

	        	            		Thread.sleep(1000);

	        		        		ProcessorRepository oProcessorRepository = new ProcessorRepository();
	        		        		Processor oProcessor = oProcessorRepository.getProcessorByName(sName);

	        		        		String sIp = "127.0.0.1";
	        		        		int iPort = oProcessor.getPort();
	        		        		m_oLocalLogger.debug("Deployprocessor.executeOperation | iPort: " + iPort);

	        		        		try {
	        		        			PipApiClient pipApiClient = new PipApiClient(sIp, iPort);

	        		        			Map<String, Object> aoPackagesInfo = pipApiClient.getPackagesInfo();

	        			        		String sProcessorFolder = oEngine.getProcessorFolder(sName);
	        			        		String sFileFullPath = sProcessorFolder + "packagesInfo.json";
	        			        		m_oLocalLogger.debug("Deployprocessor.executeOperation | sFileFullPath: " + sFileFullPath);

	        			        		boolean bResult = WasdiFileUtils.writeMapAsJsonFile(aoPackagesInfo, sFileFullPath);

						        		if (bResult) {
						        			m_oLocalLogger.debug("the file was created.");
						        		} else {
						        			m_oLocalLogger.debug("the file was not created.");
						        		}

	        		        		} catch (Exception oEx) {
	        		        			Utils.debugLog("Deployprocessor.executeOperation: " + oEx);
	        		        		}
	        		            }
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
