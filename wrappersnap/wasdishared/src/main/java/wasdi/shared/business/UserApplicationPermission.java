package wasdi.shared.business;

public enum UserApplicationPermission {

	ADMIN_DASHBOARD("admin:dashboard"),
	NODE_READ("node:read"),
	ORGANIZATION_READ("organization:read"),
	ORGANIZATION_WRITE("organization:write"),
	NODE_WRITE("node:write"),
	PROJECT_READ("project:read"),
	PROJECT_WRITE("project:write"),
	STYLE_READ("style:read"),
	STYLE_WRITE("style:write"),
	SUBSCRIPTION_READ("subscription:read"),
	SUBSCRIPTION_WRITE("subscription:write"),
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
