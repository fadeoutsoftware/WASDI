package wasdi.shared.viewmodels.organizations;

public class OrganizationListViewModel {

	private String organizationId;
	private String ownerUserId;
	private String name;
//	private String description;
//	private String address;
//	private String email;
//	private String url;
//	private String logo;
	private boolean adminRole;
	public String getOrganizationId() {
		return organizationId;
	}
	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}
	public String getOwnerUserId() {
		return ownerUserId;
	}
	public void setOwnerUserId(String ownerUserId) {
		this.ownerUserId = ownerUserId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isAdminRole() {
		return adminRole;
	}
	public void setAdminRole(boolean adminRole) {
		this.adminRole = adminRole;
	}

//	private List<String> sharedUsers = new ArrayList<>();

}
