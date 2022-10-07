package wasdi.operations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.json.JSONObject;

import wasdi.LauncherMain;
import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.LauncherOperations;
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

public class Environmentupdate extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {

		m_oLocalLogger.debug("Environmentupdate.executeOperation");

		if (oParam == null) {
			m_oLocalLogger.error("Parameter is null");
			return false;
		}

		if (oProcessWorkspace == null) {
			m_oLocalLogger.error("Process Workspace is null");
			return false;
		}

		try {
			// redeploy User Processor
			ProcessorParameter oParameter = (ProcessorParameter) oParam;

			// First Check if processor exists
			String sProcessorId = oParameter.getProcessorID();

			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

			// Check processor
			if (oProcessor == null) {
				LauncherMain.s_oLogger.error("Environmentupdate.executeOperation: oProcessor is null [" + sProcessorId + "]");
				return false;
			}
						
			// Get the right Processor Engine
			WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType());
	        
			
			// If we are not on node, nothing to do
	        if (!oEngine.isProcessorOnNode(oParameter)) {
                LauncherMain.s_oLogger.error("Environmentupdate.executeOperation: Processor [" + oProcessor.getName() + "] not installed in this node, return");
                return true;	        	
	        }
	        
			// Get the processor name
			String sProcessorName = oProcessor.getName();

			if (Utils.isNullOrEmpty(sProcessorName)) {
				// Should really not happen. But we love safe programming
				sProcessorName = oProcessor.getProcessorId();
				
				LauncherMain.s_oLogger.error("Environmentupdate.executeOperation: The processor does not have a name, we use Id " + sProcessorName);
			}	        
	        
			oEngine.setSendToRabbit(m_oSendToRabbit);
			oEngine.setParameter(oParameter);
			oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
			oEngine.setProcessWorkspace(oProcessWorkspace);
			
			// Ok make the engine work
			boolean bRet = oEngine.environmentUpdate(oParameter);
			
			if (bRet) {
				LauncherMain.s_oLogger.info("Environmentupdate.executeOperation: update done with success");
				
				// We need to update the history of actions done in this environment
				String sProcessorFolder = oEngine.getProcessorFolder(sProcessorName);
				sProcessorFolder = sProcessorFolder + "envActionsList.txt";
				
				// Get the actions file
				File oActionsLogFile = new File(sProcessorFolder);
				
				if (!oActionsLogFile.exists()) {
					oActionsLogFile.createNewFile();
				}
				
				// Extract the command we just executed
				String sJson = oParameter.getJson();
				JSONObject oJsonItem = new JSONObject(sJson);
				String sUpdateCommand = (String) oJsonItem.get("updateCommand");
				
				// Add carriage return
				sUpdateCommand += "\n";
				
				// Add this action to the list
				try (OutputStream oOutStream = new FileOutputStream(oActionsLogFile, true)) {
					byte[] ayBytes = sUpdateCommand.getBytes();
					oOutStream.write(ayBytes);
				}				
			}
			else {
				LauncherMain.s_oLogger.error("Environmentupdate.executeOperation: we got an error updating the environment");
			}

			try {
				
				// We need to refresh the package list if we are in the main node
				if (WasdiConfig.Current.nodeCode.equals("wasdi")) {
					Thread.sleep(2000);
					oEngine.refreshPackagesInfo(oParameter);
				}

				// In the exchange we should have the workspace from there the user requested the environment update
				String sOriginalWorkspaceId = oParam.getExchange();				
				
				// Check if it is a valid workspace
				if (Utils.isNullOrEmpty(sOriginalWorkspaceId) == false) {

					// Read the workspace
					WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
					Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sOriginalWorkspaceId);

					if (oWorkspace != null) {

						String sNodeCode = "wasdi";

						if (!Utils.isNullOrEmpty(oWorkspace.getNodeCode())) {
							sNodeCode = oWorkspace.getNodeCode();
						}

						// This is the computing node where the request came from?
						if (sNodeCode.equals(WasdiConfig.Current.nodeCode)) {

							// Notify the user
							String sInfo = sProcessorName + " application<br>Environment Updated";

							if (!bRet) {
								sInfo = "GURU MEDITATION<br>Error updating " + sProcessorName + " Environment";
							}

							m_oSendToRabbit.SendRabbitMessage(bRet, LauncherOperations.INFO.name(),
									oParam.getExchange(), sInfo, oParam.getExchange());
						}
					}
				}				

			} catch (Exception oRabbitException) {
				m_oLocalLogger.error("Environmentupdate.executeOperation: exception sending Rabbit Message",
						oRabbitException);
			}

			return bRet;
		} catch (Exception oEx) {
			m_oLocalLogger.error("Environmentupdate.executeOperation: exception", oEx);
		}

		return false;
	}

}
