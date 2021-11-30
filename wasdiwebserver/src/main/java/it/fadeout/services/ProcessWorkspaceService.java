/**
 * 
 */
package it.fadeout.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.KillProcessTreeParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;

/**
 * @author c.nattero
 *
 */
public class ProcessWorkspaceService {

	public boolean killProcesses(List<ProcessWorkspace> aoProcesses, Boolean bKillProcessTree, Boolean bCleanDB, String sSessionId) {
		if (null == aoProcesses) {
			Utils.debugLog("ProcessWorkspaceService.killFathers: list of processes is null, aborting");
			return false;
		}
		if (aoProcesses.size() <= 0) {
			Utils.debugLog("ProcessWorkspaceService.killFathers: list of processes is empty, aborting");
			return true;
		}


		try {

			KillProcessTreeParameter oKillProcessParameter = new KillProcessTreeParameter();
			if (null != bKillProcessTree && bKillProcessTree) {
				oKillProcessParameter.setKillTree(true);
			} else {
				oKillProcessParameter.setKillTree(false);
			}
			oKillProcessParameter.setSessionID(sSessionId);

			String sWorkspaceId = null;
			List<String> asProcesses = null;
			asProcesses = new LinkedList<String>();
			for (ProcessWorkspace oProcessWorkspace : aoProcesses) {
				if (null == oProcessWorkspace) {
					Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: found a null process, skipping");
					continue;
				}
				asProcesses.add(oProcessWorkspace.getProcessObjId());
				if (Utils.isNullOrEmpty(sWorkspaceId)) {
					sWorkspaceId = oProcessWorkspace.getWorkspaceId();
				}
			}
			oKillProcessParameter.setWorkspace(sWorkspaceId);
			oKillProcessParameter.setExchange(sWorkspaceId);
			oKillProcessParameter.setProcessesToBeKilledObjId(asProcesses);
			oKillProcessParameter.setSessionID(sSessionId);
			oKillProcessParameter.setProcessObjId(UUID.randomUUID().toString());
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
			String sOwner = oWorkspace.getUserId();
			oKillProcessParameter.setWorkspaceOwnerId(sOwner);
			if(null!=bCleanDB) {
				oKillProcessParameter.setCleanDb(true);
			}
			
			User oUser = Wasdi.getUserFromSession(sSessionId);
			oKillProcessParameter.setUserId(oUser.getUserId());
			PrimitiveResult oResult = Wasdi.runProcess(oUser.getUserId(), sSessionId, LauncherOperations.KILLPROCESSTREE.name(),
					asProcesses.get(0), WasdiConfig.Current.paths.serializationPath, oKillProcessParameter, null);
			Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: kill scheduled with result: "
					+ oResult.getBoolValue() + ", " + oResult.getIntValue() + ", " + oResult.getStringValue());
			return oResult.getBoolValue();

		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: " + oE);
		}
		return false;
	}

	public boolean killProcessesInWorkspace(String sWorkspaceId, String sSessionId, boolean bCleanDB) {
		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			Utils.debugLog("ProcessWorkspaceService.killProcessesInWorkspace: workspace is null or empty, aborting");
			return false;
		}

		List<ProcessWorkspace> aoProcesses = null;
		try {
			// Get all the process-workspaces of this workspace
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			// get list of processes that are not DONE / STOPPED / ERROR
			ProcessStatus[] aeNotTerminalStatusesArray = { ProcessStatus.CREATED, ProcessStatus.READY,
					ProcessStatus.RUNNING, ProcessStatus.WAITING };
			aoProcesses = oProcessWorkspaceRepository.getProcessByWorkspace(sWorkspaceId,
					new ArrayList<>(Arrays.asList(aeNotTerminalStatusesArray)));
		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceService.killProcessesInWorkspace: " + oE);
		}
		return killProcesses(aoProcesses, true, true, sSessionId);
	}

}
