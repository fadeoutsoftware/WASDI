package wasdi.shared.viewmodels.organizations;

public class ProjectEditorViewModel {

	private String projectId;
	private String subscriptionId;
	private String name;
	private String description;
	private String targetUser;
	private boolean activeProject;
	public String getProjectId() {
		return projectId;
	}
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}
	public String getSubscriptionId() {
		return subscriptionId;
	}
	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
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
	public boolean isActiveProject() {
		return activeProject;
	}
	public void setActiveProject(boolean activeProject) {
		this.activeProject = activeProject;
	}
	public String getTargetUser() {
		return targetUser;
	}
	public void setTargetUser(String targetUser) {
		this.targetUser = targetUser;
	}

}
