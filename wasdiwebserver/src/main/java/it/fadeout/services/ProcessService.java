/**
 * 
 */
package it.fadeout.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.SessionRepository;
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
	public PrimitiveResult killProcessTree(Boolean bKillTheEntireTree, User oUser, ProcessWorkspace oProcessToKill) {

		//create the operation parameter
		String sDeleteObjId = Utils.getRandomName();
		String sPath = m_oServletConfig.getInitParameter("SerializationPath");
		KillProcessTreeParameter oKillProcessParameter = new KillProcessTreeParameter();
		oKillProcessParameter.setProcessObjId(sDeleteObjId);
		oKillProcessParameter.setProcessToBeKilledObjId(oProcessToKill.getProcessObjId());
		if(null!=bKillTheEntireTree) {
			oKillProcessParameter.setKillTree(bKillTheEntireTree);
		}

		String sWorkspaceId = oProcessToKill.getWorkspaceId();

		//base parameter atttributes
		oKillProcessParameter.setWorkspace(sWorkspaceId);
		oKillProcessParameter.setUserId(oUser.getUserId());
		oKillProcessParameter.setExchange(sWorkspaceId);
		oKillProcessParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));

		//schedule the deletion
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setIntValue(500);
		try {
			//create new session
			String sSessionId = Utils.getRandomName();
			UserSession oSession = new UserSession();
			oSession.setSessionId(sSessionId);
			oSession.setLoginDate((double) new Date().getTime());
			oSession.setLastTouch((double) new Date().getTime());
			SessionRepository oSessionRepository = new SessionRepository();
			
			if(oSessionRepository.insertSession(oSession)) {
				oResult = Wasdi.runProcess(oUser.getUserId(), sSessionId, LauncherOperations.KILLPROCESSTREE.name(), oProcessToKill.getProductName(), sPath, oKillProcessParameter, null);
				Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: kill scheduled with result: " + oResult.getBoolValue() + ", " + oResult.getIntValue() + ", " + oResult.getStringValue());
			} else {
				Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: could not insert the session in the DB, kill not scheduled");
				oResult.setStringValue("Could not kill the requested process");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return oResult;
	}	
}
