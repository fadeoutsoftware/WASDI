package wasdi.shared.viewmodels.organizations;

import java.util.ArrayList;
import java.util.List;

public class OrganizationViewModel {

	private String organizationId;
	private String userId;
	private String name;
	private String description;
	private String address;
	private String email;
	private String url;
	private boolean readOnly;

	private List<String> sharedUsers = new ArrayList<>();

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<String> getSharedUsers() {
		return sharedUsers;
	}

	public void setSharedUsers(List<String> sharedUsers) {
		this.sharedUsers = sharedUsers;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

}
