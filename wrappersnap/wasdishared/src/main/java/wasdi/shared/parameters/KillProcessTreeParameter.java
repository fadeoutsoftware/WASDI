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
	public String getsProcessObjId() {
		return sProcessObjId;
	}
	/**
	 * @param sProcessObjId the sProcessObjId to set
	 */
	public void setsProcessObjId(String sProcessObjId) {
		this.sProcessObjId = sProcessObjId;
	}
	/**
	 * @return the bKillTree
	 */
	public boolean isbKillTree() {
		return bKillTree;
	}
	/**
	 * @param bKillTree the bKillTree to set
	 */
	public void setbKillTree(boolean bKillTree) {
		this.bKillTree = bKillTree;
	}
}
