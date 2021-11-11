/**
 * Created by Cristiano Nattero on 2020-05-25
 * 
 * Fadeout software
 *
 */
package wasdi.shared.parameters;

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
	private String processToBeKilledObjId;
	
	/**
	 * by default, kill the entire process tree
	 */
	private boolean killTree = true;
	
	/**
	 * @return the sProcessObjId
	 */
	public String getProcessToBeKilledObjId() {
		return processToBeKilledObjId;
	}
	/**
	 * @param sProcessObjId the sProcessObjId to set
	 */
	public void setProcessToBeKilledObjId(String sProcessObjId) {
		this.processToBeKilledObjId = sProcessObjId;
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
