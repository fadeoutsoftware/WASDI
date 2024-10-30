package wasdi.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Preconditions;

import wasdi.processors.WasdiProcessorEngine;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ParametersRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.KillProcessTreeParameter;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.LauncherOperationsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.docker.DockerUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;

public class Killprocesstree extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		WasdiLog.debugLog("Killprocesstree.executeOperation");

		try {
			Preconditions.checkNotNull(oProcessWorkspace, "process workspace is null");
			Preconditions.checkNotNull(oParam, "parameter is null");
			
			KillProcessTreeParameter oKillProcessTreeParameter = (KillProcessTreeParameter) oParam;
			Preconditions.checkNotNull(oKillProcessTreeParameter, "parameter after casting is null");
			Preconditions.checkArgument(oKillProcessTreeParameter.getProcessesToBeKilledObjId().size()>0, "List of processes to be killed is empty");

			//now get the processWorkspaces from DB using their processObjId 
			LinkedList<ProcessWorkspace> aoProcessesToBeKilled = new LinkedList<>();
			
			for (String sProcessObjId: oKillProcessTreeParameter.getProcessesToBeKilledObjId()) {
				ProcessWorkspace oProcessToKill = m_oProcessWorkspaceRepository.getProcessByProcessObjId(sProcessObjId);
				if (null != oProcessToKill) {
					WasdiLog.infoLog("Killprocesstree.executeOperation: collecting and killing processes for " + oProcessToKill.getProcessObjId());
					aoProcessesToBeKilled.add(oProcessToKill);
				} else {
					WasdiLog.warnLog("Killprocesstree.executeOperation: no process found for processObjId " + sProcessObjId);
				}
			}

			List<String> asKilledProcessObjIds = new ArrayList<String>(aoProcessesToBeKilled.size());
			
			while (aoProcessesToBeKilled.size() > 0) {
				//breadth first
				ProcessWorkspace oProcess = aoProcessesToBeKilled.removeFirst();

				if (null == oProcess) {
					WasdiLog.errorLog("Killprocesstree.executeOperation: a null process was added, skipping");
					continue;
				}
				
				//a dead process cannot spawn any more children, add the remaining ones to the set of processes to be killed
				if (Utils.isNullOrEmpty(oProcess.getProcessObjId())) {
					WasdiLog.errorLog("Killprocesstree.executeOperation: process has null or empty ObjId, skipping");
					continue;
				}				
				
				WasdiLog.infoLog("Killprocesstree.executeOperation: killing " + oProcess.getProcessObjId());
				
				// Terminate the process
				terminate(oProcess);
				
				// Save processObjId for later DB cleanub
				asKilledProcessObjIds.add(oProcess.getProcessObjId());
				
				// Should we kill all the tree?
				if (!oKillProcessTreeParameter.getKillTree()) {
					continue;
				}
				
				//from here on collect children
				boolean bCanSpawnChildren = LauncherOperationsUtils.canOperationSpawnChildren(oProcess.getOperationType());
				
				if (!bCanSpawnChildren) {
					WasdiLog.debugLog("Killprocesstree.executeOperation: process " + oProcess.getProcessObjId() + " cannot spawn children, skipping");
					continue;
				}
				
				List<ProcessWorkspace> aoChildren = m_oProcessWorkspaceRepository.getProcessByParentId(oProcess.getProcessObjId());
				
				// Add the children
				if (null != aoChildren && aoChildren.size() > 0) {
					//append at the end
					for (ProcessWorkspace oChildren : aoChildren) {
						if(!aoProcessesToBeKilled.contains(oChildren)) {
							aoProcessesToBeKilled.add(oChildren);
						}
					}
				}
			}

			WasdiLog.infoLog("Killprocesstree.executeOperation: Kill loop done");
			
			if(oKillProcessTreeParameter.getCleanDb()) {
				cleanDB(asKilledProcessObjIds);
			}

			//mark complete and return
			updateProcessStatus(oProcessWorkspace, ProcessStatus.DONE, 100);
			
			return true;

		} catch (Exception oE) {
			WasdiLog.errorLog("Killprocesstree.executeOperation: " + oE);
		} 

		WasdiLog.infoLog("Killprocesstree.executeOperation: done");

		return false;
	}

	/**
	 * @param oProcessWorkspaceRepository
	 * @param asKilledProcessObjIds
	 */
	private void cleanDB(List<String> asKilledProcessObjIds) {
		WasdiLog.infoLog("Killprocesstree.executeOperation: cleaning DB");
		ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository(); 
		for (String sProcessObjId: asKilledProcessObjIds) {
			//delete processes
			oProcessWorkspaceRepository.deleteProcessWorkspaceByProcessObjId(sProcessObjId);
			//delete logs
			oProcessorLogRepository.deleteLogsByProcessWorkspaceId(sProcessObjId);
		}
	}

	/**
	 * @param oProcessToKill
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void terminate(ProcessWorkspace oProcessToKill) {
		try {

			if (LauncherOperationsUtils.doesOperationLaunchApplication(oProcessToKill.getOperationType())) {
				WasdiLog.infoLog("Killprocesstree.killProcessAndDocker: about to kill docker instance of process " + oProcessToKill.getProcessObjId());
				killApplication(oProcessToKill);
			}

			killProcess(oProcessToKill);
			setStatusAsStopped(oProcessToKill);
		} catch (Exception oE) {
			WasdiLog.errorLog("Killprocesstree.killProcessAndDocker: " + oE);
		}
	}

	/**
	 * kill the process
	 * 
	 * @param oProcessToKill the process to be killed
	 */
	private void killProcess(ProcessWorkspace oProcessToKill) {
		
		try {
			if (WasdiConfig.Current.shellExecLocally) {
				int iPid = oProcessToKill.getPid();
				
				if (iPid>1) {
					RunTimeUtils.killProcess(iPid);
				}
			}
			else { 
				String sContainerId = oProcessToKill.getContainerId();
				
				if (Utils.isNullOrEmpty(sContainerId)) {
					WasdiLog.warnLog("Killprocesstree.killProcess: we are dockerized but we cannot find the continer id for operation id " + oProcessToKill.getProcessObjId());
				}
				
				DockerUtils oDockerUtils = new DockerUtils();
				
				if (!oDockerUtils.stop(sContainerId)) {
					WasdiLog.warnLog("Killprocesstree.killProcess: dockerutils.stop returned false for process workspace " + oProcessToKill.getProcessObjId() + " ContainerId " + sContainerId);					
				}
			}
		} 
		catch (Exception oE) {
			WasdiLog.errorLog("Killprocesstree.killProcess( " + oProcessToKill.getProcessObjId() + " ): " + oE);
		}
	}

	/**
	 * set process state to STOPPED only if CREATED or RUNNING
	 * @param oKilledProcessWorkspace the ProcessWorkspace that has been killed and for which the status must be updated
	 */
	private void setStatusAsStopped(ProcessWorkspace oKilledProcessWorkspace) {
		
		//refresh the status
		ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
		oKilledProcessWorkspace = oRepository.getProcessByProcessObjId(oKilledProcessWorkspace.getProcessObjId());
		String sPrevSatus = oKilledProcessWorkspace.getStatus();

		if (
				!sPrevSatus.equalsIgnoreCase(ProcessStatus.DONE.name()) &&
				!sPrevSatus.equalsIgnoreCase(ProcessStatus.ERROR.name()) &&
				!sPrevSatus.equalsIgnoreCase(ProcessStatus.STOPPED.name())
		){
			oKilledProcessWorkspace.setStatus(ProcessStatus.STOPPED.name());
			oKilledProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());

			if (!oRepository.updateProcess(oKilledProcessWorkspace)) {
				WasdiLog.errorLog("Killprocesstree.setStatusAsStopped: Unable to update process status of process " +
				oKilledProcessWorkspace.getProcessObjId() + " from: " + sPrevSatus + " to: STOPPED");
			}
		} else {
			WasdiLog.infoLog("Killprocesstree.setStatusAsStopped: Process " + oKilledProcessWorkspace.getProcessObjId() +
					" was already over with status: " + sPrevSatus);
		}
	}


	/**
	 * @param oProcessToKill the process for which the corresponding docker must be killed
	 */
	private void killApplication(ProcessWorkspace oProcessToKill) {
		try {
			
            // Deserialize the parameter
			ParametersRepository oParametersRepository = new ParametersRepository();
			ProcessorParameter oParameter = (ProcessorParameter) oParametersRepository.getParameterByProcessObjId(oProcessToKill.getProcessObjId());
			
	        WasdiProcessorEngine oEngine = WasdiProcessorEngine.getProcessorEngine(oParameter.getProcessorType());
	        oEngine.setSendToRabbit(m_oSendToRabbit);
	        oEngine.setParameter(oParameter);
	        oEngine.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
	        oEngine.setProcessWorkspace(oProcessToKill);
	        
	        oEngine.stopApplication(oParameter);	        	
	        
		} catch (Exception oE) {
			WasdiLog.errorLog("Killprocesstree.killDocker( " + oProcessToKill.getProcessObjId() + " ): " + oE);
		}
	}

}
