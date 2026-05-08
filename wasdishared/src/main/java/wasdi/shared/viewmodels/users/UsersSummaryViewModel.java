package wasdi.shared.viewmodels.users;

/**
 * Represents the summary overview of the users registered
 */
public class UsersSummaryViewModel {
	
	private int totalUsers;
	private int noneUsers;
	private int freeUsers;
	private int standardUsers;
	private int proUsers;
	private int organizations;
	
	public int getTotalUsers() {
		return totalUsers;
	}
	public void setTotalUsers(int totalUsers) {
		this.totalUsers = totalUsers;
	}
	public int getFreeUsers() {
		return freeUsers;
	}
	public void setFreeUsers(int freeUsers) {
		this.freeUsers = freeUsers;
	}
	public int getStandardUsers() {
		return standardUsers;
	}
	public void setStandardUsers(int standardUsers) {
		this.standardUsers = standardUsers;
	}
	public int getProUsers() {
		return proUsers;
	}
	public void setProUsers(int proUsers) {
		this.proUsers = proUsers;
	}
	public int getOrganizations() {
		return organizations;
	}
	public void setOrganizations(int organizations) {
		this.organizations = organizations;
	}
	public int getNoneUsers() {
		return noneUsers;
	}
	public void setNoneUsers(int noneUsers) {
		this.noneUsers = noneUsers;
	}

}
