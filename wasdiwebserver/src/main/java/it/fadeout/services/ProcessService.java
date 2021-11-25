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
	public PrimitiveResult killProcesses(List<String> asProcesses, Boolean bKillProcessTree, User oUser) {
		if(null == asProcesses) {
			Utils.debugLog("ProcessWorkspaceService.killFathers: list of processes is null, aborting");
			return PrimitiveResult.getInvalid();
		}
		if(asProcesses.size() <= 0) {
			Utils.debugLog("ProcessWorkspaceService.killFathers: list of processes is empty, aborting");
			return PrimitiveResult.getInvalid();
		}

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
			if(!oSessionRepository.insertSession(oSession)) {
				Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: could not insert the session in the DB, kill not scheduled");
				oResult.setStringValue("Could not kill the requested process");
				return oResult;
			}
			KillProcessTreeParameter oKillProcessParameter = new KillProcessTreeParameter();
			if(null!=bKillProcessTree && bKillProcessTree) {
				oKillProcessParameter.setKillTree(true);
			} else {
				oKillProcessParameter.setKillTree(false);
			}
			
			oResult = Wasdi.runProcess(oUser.getUserId(), sSessionId, LauncherOperations.KILLPROCESSTREE.name(), asProcesses.get(0), m_oServletConfig.getInitParameter("SerializationPath"), oKillProcessParameter, null);
			Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: kill scheduled with result: " + oResult.getBoolValue() + ", " + oResult.getIntValue() + ", " + oResult.getStringValue());
			
		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: " + oE);
		}
		return oResult;
	}

	@Override
	public PrimitiveResult killProcessesInWorkspace(String sWorkspaceId, User oUser) {
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setIntValue(500);
		if(Utils.isNullOrEmpty(sWorkspaceId)) {
			Utils.debugLog("ProcessWorkspaceService.killProcessesInWorkspace: workspace is null or empty, aborting");
			return oResult;
		}
		
		List<ProcessWorkspace> aoProcesses = null;
		List<String> asProcesses = null;
		try {
			// Get all the process-workspaces of this workspace
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			//get list of processes that are not DONE / STOPPED / ERROR
			ProcessStatus[] aeNotTerminalStatusesArray = {ProcessStatus.CREATED, ProcessStatus.READY, ProcessStatus.RUNNING, ProcessStatus.WAITING};
			aoProcesses = oProcessWorkspaceRepository.getProcessByWorkspace(sWorkspaceId, new ArrayList<>(Arrays.asList(aeNotTerminalStatusesArray)));
			asProcesses = new LinkedList<String>();
			for (ProcessWorkspace oProcessWorkspace : aoProcesses) {
				if(null==oProcessWorkspace) {
					continue;
				}
				asProcesses.add(oProcessWorkspace.getProcessObjId());
			}
		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceService.killProcessesInWorkspace: " + oE);
		}
		return killProcesses(asProcesses, true, oUser);
	}


	//TODO don't use session, create one on the fly (assume not everybody can call this) 
	@Override
	public PrimitiveResult killProcessTree(Boolean bKillTheEntireTree, User oUser, ProcessWorkspace oProcessToKill) {

		//create the operation parameter
		String sDeleteObjId = Utils.getRandomName();
		KillProcessTreeParameter oKillProcessParameter = new KillProcessTreeParameter();
		oKillProcessParameter.setProcessObjId(sDeleteObjId);
		List<String> asProcessedToBeKilled = new LinkedList<String>();
		asProcessedToBeKilled.add(oProcessToKill.getProcessObjId());

		oKillProcessParameter.setProcessesToBeKilledObjId(asProcessedToBeKilled);
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
			List<String> asProcessesToBeKilled = new ArrayList<String>(1);
			asProcessesToBeKilled.add(oProcessToKill.getProductName());
			return killProcesses(asProcessedToBeKilled, bKillTheEntireTree, oUser);

		} catch (Exception oE) {
			Utils.debugLog("ProcessWorkspaceResource.DeleteProcess: " + oE);
		}
		return oResult;
	}	
}
