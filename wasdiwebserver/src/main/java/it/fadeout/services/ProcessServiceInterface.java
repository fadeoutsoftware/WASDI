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

		public PrimitiveResult killProcessesInWorkspace(String sWorkspaceId, User oUser);
		public PrimitiveResult killProcesses(List<ProcessWorkspace> aoProcesses, Boolean bKillProcessTree, User oUser);
		public PrimitiveResult killProcessTree(Boolean bKillTheEntireTree, User oUser, ProcessWorkspace oProcessToKill);
}
