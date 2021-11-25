/**
 * Created by Cristiano Nattero on 2020-05-25
 * 
 * Fadeout software
 *
 */
package wasdi.shared.parameters;

import java.util.List;

/**
 * Parameter of the KILL Operation
 * 
 * @author c.nattero
 *
 */
public class KillProcessTreeParameter extends BaseParameter {
	
	/**
	 * Process Workspace Id of the process to kill
	 */
	private List<String> processesToBeKilledObjId;
	
	/**
	 * by default, kill the entire process tree
	 */
	private boolean killTree = true;
	
	/**
	 * @return the sProcessObjId
	 */
	public List<String> getProcessesToBeKilledObjId() {
		return processesToBeKilledObjId;
	}
	/**
	 * @param sProcessObjId the sProcessObjId to set
	 */
	public void setProcessesToBeKilledObjId(List<String> sProcessObjId) {
		this.processesToBeKilledObjId = sProcessObjId;
	}
	/**
	 * @return the bKillTree
	 */
	public boolean getKillTree() {
		return killTree;
	}
	/**
	 * @param killTree the bKillTree to set
	 */
	public void setKillTree(boolean killTree) {
		this.killTree = killTree;
	}
}
