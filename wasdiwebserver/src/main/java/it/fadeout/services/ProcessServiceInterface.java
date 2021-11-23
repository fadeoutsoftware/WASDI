/**
 * 
 */
package it.fadeout.services;

import java.util.List;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.viewmodels.PrimitiveResult;

/**
 * @author c.nattero
 *
 */
public interface ProcessServiceInterface {

		public List<ProcessWorkspace> killProcessesInWorkspace(String sWorkspaceId);
		public List<ProcessWorkspace> killFathers(List<ProcessWorkspace> aoProcesses, String sWorkspaceId);
		public PrimitiveResult killProcessTree(Boolean bKillTheEntireTree, User oUser, ProcessWorkspace oProcessToKill);
}
