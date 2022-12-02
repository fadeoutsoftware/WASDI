package wasdi.operations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Preconditions;

import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorLogRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.parameters.KillProcessTreeParameter;
import wasdi.shared.utils.LauncherOperationsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class Killprocesstree extends Operation {

	@Override
	public boolean executeOperation(BaseParameter oParam, ProcessWorkspace oProcessWorkspace) {
		WasdiLog.debugLog("Killprocesstree.executeOperation");

		try {
			Preconditions.checkNotNull(oParam, "process workspace is null");
			Preconditions.checkNotNull(oParam, "parameter is null");
			
			KillProcessTreeParameter oKillProcessTreeParameter = (KillProcessTreeParameter) oParam;
			Preconditions.checkNotNull(oKillProcessTreeParameter, "parameter after casting is null");
			Preconditions.checkArgument(oKillProcessTreeParameter.getProcessesToBeKilledObjId().size()>0, "List of processes to be killed is empty");

			//now get the processWorkspaces from DB using their processObjId 
			LinkedList<ProcessWorkspace> aoProcessesToBeKilled = new LinkedList<>();
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			for (String sProcessObjId: oKillProcessTreeParameter.getProcessesToBeKilledObjId()) {
				ProcessWorkspace oProcessToKill = oProcessWorkspaceRepository.getProcessByProcessObjId(sProcessObjId);
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
				
				WasdiLog.infoLog("Killprocesstree.executeOperation: killing " + oProcess.getProcessObjId());

				terminate(oProcess);
				
				//save processObjId for later DB cleanub
				asKilledProcessObjIds.add(oProcess.getProcessObjId());
				
				//from here on collect children
				if (!oKillProcessTreeParameter.getKillTree()) {
					continue;
				}

				LauncherOperationsUtils oLauncherOperationsUtils = new LauncherOperationsUtils();
				boolean bCanSpawnChildren = oLauncherOperationsUtils.canOperationSpawnChildren(oProcess.getOperationType());
				if (!bCanSpawnChildren) {
					WasdiLog.debugLog("Killprocesstree.executeOperation: process " + oProcess.getProcessObjId() + " cannot spawn children, skipping");
					continue;
				}

				//a dead process cannot spawn any more children, add the remaining ones to the set of processes to be killed
				if (Utils.isNullOrEmpty(oProcess.getProcessObjId())) {
					WasdiLog.errorLog("Killprocesstree.executeOperation: process has null or empty ObjId, skipping");
					continue;
				}
				List<ProcessWorkspace> aoChildren = oProcessWorkspaceRepository.getProcessByParentId(oProcess.getProcessObjId());
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
			ProcessWorkspace oMyProcess = oProcessWorkspaceRepository.getProcessByProcessObjId(oKillProcessTreeParameter.getProcessObjId());			
			updateProcessStatus(oMyProcess, ProcessStatus.DONE, 100);
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
			LauncherOperationsUtils oLauncherOperationsUtils = new LauncherOperationsUtils();

			if (oLauncherOperationsUtils.doesOperationLaunchDocker(oProcessToKill.getOperationType())) {
				WasdiLog.infoLog("Killprocesstree.killProcessAndDocker: about to kill docker instance of process " + oProcessToKill.getProcessObjId());
				killDocker(oProcessToKill);
			}

			killProcess(oProcessToKill);
			setStatusAsStopped(oProcessToKill);
		} catch (Exception oE) {
			WasdiLog.errorLog("Killprocesstree.killProcessAndDocker: " + oE);
		}
	}

	/**
	 * @param oProcessToKill the process to be killed
	 */
	private void killProcess(ProcessWorkspace oProcessToKill) {
		//kill the process
		//(code ported from webserver)

		try {
			int iPid = oProcessToKill.getPid();

			if (iPid > 0) {
				// Pid exists, kill the process
				String sShellExString = WasdiConfig.Current.scheduler.killCommand;
				if (Utils.isNullOrEmpty(sShellExString)) sShellExString = "kill -9";
				sShellExString += " " + iPid;

				WasdiLog.infoLog("Killprocesstree.killProcess: shell exec " + sShellExString);
				Process oProc = Runtime.getRuntime().exec(sShellExString);
				WasdiLog.infoLog("Killprocesstree.killProcess: kill result: " + oProc.waitFor());

			} else {
				WasdiLog.errorLog("Killprocesstree.killProcess: Process pid not in data");
			}
		} catch (Exception oE) {
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
	private void killDocker(ProcessWorkspace oProcessToKill) {
		try {
			String sProcessorName = oProcessToKill.getProductName();
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessorToKill = oProcessorRepository.getProcessorByName(sProcessorName);

			// Call localhost:port
			String sUrl = "http://" + WasdiConfig.Current.dockers.internalDockersBaseAddress + ":" + oProcessorToKill.getPort() + "/run/--kill" + "_" + oProcessToKill.getSubprocessPid();

			URL oProcessorUrl = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oProcessorUrl.openConnection();
			oConnection.setDoOutput(true);
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Content-Type", "application/json");
			OutputStream oOutputStream = oConnection.getOutputStream();
			oOutputStream.write("{}".getBytes());
			oOutputStream.flush();

			if (!(oConnection.getResponseCode() == HttpURLConnection.HTTP_OK || oConnection.getResponseCode() == HttpURLConnection.HTTP_CREATED)) {
				throw new RuntimeException("Failed : HTTP error code : " + oConnection.getResponseCode());
			}
			BufferedReader oBufferedReader = new BufferedReader(new InputStreamReader((oConnection.getInputStream())));
			String sOutputResult;
			String sOutputCumulativeResult = "";

			while ((sOutputResult = oBufferedReader.readLine()) != null) {
				WasdiLog.debugLog("killDocker: " + sOutputResult);
				if (!Utils.isNullOrEmpty(sOutputResult)) sOutputCumulativeResult += sOutputResult;
			}
			oConnection.disconnect();

			WasdiLog.infoLog("Killprocesstree.killDocker: " + sOutputCumulativeResult);
			WasdiLog.infoLog("Killprocesstree.killDocker: Kill docker done for " + oProcessToKill.getProcessObjId() + " SubPid: " + oProcessToKill.getSubprocessPid());
		} catch (Exception oE) {
			WasdiLog.errorLog("Killprocesstree.killDocker( " + oProcessToKill.getProcessObjId() + " ): " + oE);
		}
	}

}
