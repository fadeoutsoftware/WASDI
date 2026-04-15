package wasdi.shared.viewmodels.processors;

/**
 * Processor Sharing View Model
 * 
 * Represents a user that has a shared processor
 * 
 * @author p.campanella
 *
 */
public class ProcessorSharingViewModel {
	private String userId;
	private String permissions;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPermissions() {
		return permissions;
	}

	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}
}
