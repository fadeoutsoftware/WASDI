/**
 * 
 */
package it.fadeout.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.parameters.KillProcessTreeParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;

/**
 * @author c.nattero
 *
 */
public class ProcessService implements ProcessServiceInterface {

	@Context
	ServletConfig m_oServletConfig;	


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

	
	//TODO don't use session, create one on the fly (assume not everybody can call this) 
	@Override
	public PrimitiveResult killProcessTree(String sSessionId, String sToKillProcessObjId, Boolean bKillTheEntireTree,
			User oUser, ProcessWorkspace oProcessToDelete) {

		//create the operation parameter
		String sDeleteObjId = Utils.GetRandomName();
		String sPath = m_oServletConfig.getInitParameter("SerializationPath");
		KillProcessTreeParameter oKillProcessParameter = new KillProcessTreeParameter();
		oKillProcessParameter.setProcessObjId(sDeleteObjId);
		oKillProcessParameter.setProcessToBeKilledObjId(sToKillProcessObjId);
		if(null!=bKillTheEntireTree) {
			oKillProcessParameter.setKillTree(bKillTheEntireTree);
		}

		String sWorkspaceId = oProcessToDelete.getWorkspaceId();

		//base parameter atttributes
		oKillProcessParameter.setWorkspace(sWorkspaceId);
		oKillProcessParameter.setUserId(oUser.getUserId());
		oKillProcessParameter.setExchange(sWorkspaceId);
		oKillProcessParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));

		//schedule the deletion
		PrimitiveResult oResult = null;
		try {
			oResult = Wasdi.runProcess(oUser.getUserId(), sSessionId, LauncherOperations.KILLPROCESSTREE.name(), oProcessToDelete.getProductName(), sPath, oKillProcessParameter, null);
			Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: kill scheduled with result: " + oResult.getBoolValue() + ", " + oResult.getIntValue() + ", " + oResult.getStringValue());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return oResult;
	}	
}
