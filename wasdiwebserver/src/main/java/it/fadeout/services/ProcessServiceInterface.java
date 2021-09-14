/**
 * 
 */
package it.fadeout.services;

import java.util.List;

import wasdi.shared.business.ProcessWorkspace;

/**
 * @author c.nattero
 *
 */
public interface ProcessServiceInterface {

		public List<ProcessWorkspace> killProcessesInWorkspace(String sWorkspaceId);
		public List<ProcessWorkspace> killFathers(List<ProcessWorkspace> aoProcesses, String sWorkspaceId);
}
