/**
 * 
 */
package it.fadeout.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class ProcessService implements ProcessServiceInterface {



	@Override
	public List<ProcessWorkspace> killFathers(List<ProcessWorkspace> aoProcesses, String sWorkspaceId) {
		if(null == aoProcesses) {
			Utils.debugLog("ProcessWorkspaceService.killFathers: list of processes is null, aborting");
			return new LinkedList<ProcessWorkspace>();
		}
		if(aoProcesses.size() <= 0) {
			Utils.debugLog("ProcessWorkspaceService.killFathers: list of processes is empty, aborting");
			return new LinkedList<ProcessWorkspace>();
		}
		if(Utils.isNullOrEmpty(sWorkspaceId)) {
			Utils.debugLog("ProcessWorkspaceService.killFathers: workspaceId null or empty, aborting");
			return new LinkedList<ProcessWorkspace>();
		}
		try {
			//search for the fathers of these processes
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			List<ProcessWorkspace> aoFathers = oProcessWorkspaceRepository.getRoots(aoProcesses);
			
			//TODO killProcessTree on roots
			
			
		} catch (Exception oE) {
			// TODO: handle exception
			oE.printStackTrace();
		}
		return null;
	}

	@Override
	public List<ProcessWorkspace> killProcessesInWorkspace(String sWorkspaceId) {
		if(Utils.isNullOrEmpty(sWorkspaceId)) {
			Utils.debugLog("ProcessWorkspaceService.killProcessesInWorkspace: workspace is null or empty, aborting");
			return new LinkedList<ProcessWorkspace>();
		}
		List<ProcessWorkspace> aoProcesses = null;
		try {
			// Get all the process-workspaces of this workspace
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			//get list of processes that are not DONE / STOPPED / ERROR
			ProcessStatus[] aeNotTerminalStatusesArray = {ProcessStatus.CREATED, ProcessStatus.READY, ProcessStatus.RUNNING, ProcessStatus.WAITING};
			aoProcesses = oProcessWorkspaceRepository.getProcessByWorkspace(sWorkspaceId, new ArrayList<>(Arrays.asList(aeNotTerminalStatusesArray)));
		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceService.killProcessesInWorkspace: " + oE);
		}
		return killFathers(aoProcesses, sWorkspaceId);
	}

}
