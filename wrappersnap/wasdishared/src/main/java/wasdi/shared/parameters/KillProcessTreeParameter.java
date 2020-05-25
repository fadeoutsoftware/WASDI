/**
 * Created by Cristiano Nattero on 2020-05-25
 * 
 * Fadeout software
 *
 */
package wasdi.shared.parameters;

/**
 * @author c.nattero
 *
 */
public class KillProcessTreeParameter extends BaseParameter {
	private String sProcessObjId;
	private boolean bKillTree;
	
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
