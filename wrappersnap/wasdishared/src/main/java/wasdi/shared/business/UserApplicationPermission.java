package wasdi.shared.business;

public enum UserApplicationPermission {

	ADMIN_DASHBOARD("admin:dashboard"),
	NODE_READ("node:read"),
	NODE_WRITE("node:write"),
	USER_READ("user:read"),
	USER_WRITE("user:write"),
	WORKSPACE_READ("workspace:read"),
	WORKSPACE_WRITE("workspace:write"),
	PROCESSOR_PARAMETERS_TEMPLATE_READ("processorparameterstemplate:read"),
	PROCESSOR_PARAMETERS_TEMPLATE_WRITE("processorparameterstemplate:write");

	private final String permission;

	UserApplicationPermission(String permission) {
		this.permission = permission;
	}

	public String getPermission() {
		return permission;
	}

}
