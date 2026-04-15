/**
 * Created by Cristiano Nattero on 2019-01-11
 * 
 * Fadeout software
 *
 */
package wasdi;

import wasdi.shared.business.ProcessWorkspace;

/**
 * @author c.nattero
 *
 */
public interface ProcessWorkspaceUpdateSubscriber {
	public void notify(ProcessWorkspace oProcessWorkspace);
}
