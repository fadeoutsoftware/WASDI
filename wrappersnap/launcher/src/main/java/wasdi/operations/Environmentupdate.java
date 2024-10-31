package wasdi.operations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import org.json.JSONObject;

import wasdi.LauncherMain;
import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Environment Update Operation.
 * 
 * Takes in input a Processor Parameter
 * 
 * @author p.campanella
 *
 */
public class Environmentupdate extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {

		WasdiLog.infoLog("Environmentupdate.executeOperation");

		if (oParam == null) {
			WasdiLog.errorLog("Environmentupdate.executeOperation: Parameter is null");
			return false;
		}

		if (oProcessWorkspace == null) {
			WasdiLog.errorLog("Environmentupdate.executeOperation: Process Workspace is null");
			return false;
		}

		try {
			// Read the Parameter
			ProcessorParameter oParameter = (ProcessorParameter) oParam;

			// First Check if processor exists
			String sProcessorId = oParameter.getProcessorID();

			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

			// Check processor
			if (oProcessor == null) {
				WasdiLog.errorLog("Environmentupdate.executeOperation: oProcessor is null [" + sProcessorId + "]");
				return false;
			}
						
			// Get the right Processor Engine
			WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType());
			
			// If the application is not on this node there is nothing to do
	        if (!oEngine.isProcessorOnNode(oParameter)) {
                WasdiLog.infoLog("Environmentupdate.executeOperation: Processor [" + oProcessor.getName() + "] not installed in this node, return");
                return true;	        	
	        }
	        
			// Get the processor name
			String sProcessorName = oProcessor.getName();

			if (Utils.isNullOrEmpty(sProcessorName)) {
				// Should really not happen. But we love safe programming
				WasdiLog.errorLog("Environmentupdate.executeOperation: The processor does not have a name, we use Id " + sProcessorName);
				return false;
			}
	        
			oEngine.setSendToRabbit(m_oSendToRabbit);
			oEngine.setParameter(oParameter);
			oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
			oEngine.setProcessWorkspace(oProcessWorkspace);
			
			// Ok make the engine work
			boolean bRet = oEngine.environmentUpdate(oParameter);
			
			if (bRet) {
				WasdiLog.infoLog("Environmentupdate.executeOperation: update done with success");
				
				if (WasdiConfig.Current.isMainNode()) {
				
					// We need to update the history of actions done in this environment
					String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);
					sProcessorFolder = sProcessorFolder + "envActionsList.txt";
					
					// Get the actions file
					File oActionsLogFile = new File(sProcessorFolder);
					
					if (!oActionsLogFile.exists()) {
						boolean bIsFileCreated = oActionsLogFile.createNewFile();
						if (!bIsFileCreated) {
							WasdiLog.errorLog("Environmentupdate.executeOperation: the action file was not created");
						}
					}
					
					// Extract the command we just executed
					String sJson = oParameter.getJson();
					JSONObject oJsonItem = new JSONObject(sJson);
					Object oUpdateCommand = oJsonItem.get("updateCommand");

					if (oUpdateCommand == null || oUpdateCommand.equals(org.json.JSONObject.NULL)) {
						WasdiLog.debugLog("Environmentupdate.executeOperation: no actions to add to the envActionsList");
					} else {
						String sUpdateCommand = (String) oUpdateCommand;
						
						WasdiLog.infoLog("Environmentupdate.executeOperation: adding " + sUpdateCommand + " to the envActionsList");
					
						// we re-read all the actions line per line
						ArrayList<String> asActionLines = new ArrayList<>(); 

				        try (java.util.stream.Stream<String> oLinesStream = Files.lines(oActionsLogFile.toPath())) {
				        	oLinesStream.forEach(sLine -> {
				        		asActionLines.add(sLine);
				            });
				        }
				        
				        asActionLines.add(sUpdateCommand);
				        
				        String sLastLine = "";
					
						// Add this action to the list and re-write avoiding duplicates
						try (OutputStream oOutStream = new FileOutputStream(oActionsLogFile, false)) {
							for (String sActualLine : asActionLines) {
								if (sActualLine.equals(sLastLine)) {
									WasdiLog.debugLog("Environmentupdate.executeOperation: jump duplicate " + sActualLine + " in to envActionsList");
									continue;
								}
								
								String sWriteLine = sActualLine + "\n";
								byte[] ayBytes = sWriteLine.getBytes();
								oOutStream.write(ayBytes);
								
								sLastLine = sActualLine;
							}
						}
					}
				}
			}
			else {
				WasdiLog.errorLog("Environmentupdate.executeOperation: we got an error updating the environment");
			}

			try {
				// We need to refresh the package list if we are in the main node
				if (WasdiConfig.Current.isMainNode()) {
					
					oEngine.refreshPackagesInfo(oParameter);
					
					// Notify the user
					String sInfo = sProcessorName + " application<br>Environment Updated";

					if (!bRet) {
						sInfo = "GURU MEDITATION<br>Error updating " + sProcessorName + " Environment";
					}

					m_oSendToRabbit.SendRabbitMessage(bRet, LauncherOperations.ENVIRONMENTUPDATE.name(), oParam.getExchange(), sInfo, oParam.getExchange());
					
				}
			} 
			catch (Exception oRabbitException) {
				WasdiLog.errorLog("Environmentupdate.executeOperation: exception sending Rabbit Message", oRabbitException);
			}
			
			if (bRet) {
				LauncherMain.updateProcessStatus(m_oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
			}
			else {
				LauncherMain.updateProcessStatus(m_oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			}

			return bRet;
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("Environmentupdate.executeOperation: exception", oEx);
		}

		return false;
	}

}
