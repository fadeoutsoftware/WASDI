package wasdi.shared.viewmodels.organizations;

public class ProjectListViewModel {

	private String projectId;
//	private String subscriptionId;
	private String subscriptionName;
	private String name;
	private String description;
	private boolean activeProject;
	
	public String getProjectId() {
		return projectId;
	}
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}
	public String getSubscriptionName() {
		return subscriptionName;
	}
	public void setSubscriptionName(String subscriptionName) {
		this.subscriptionName = subscriptionName;
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

}
