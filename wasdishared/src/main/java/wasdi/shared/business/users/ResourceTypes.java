package wasdi.shared.business.users;

public enum ResourceTypes {
	ORGANIZATION("organization"),
	SUBSCRIPTION("subscription"),
	WORKSPACE("workspace"),
	STYLE("style"),
	WORKFLOW("workflow"),
	PROCESSOR("processor"),
	PARAMETER("processorparameterstemplate"),
	NODE("node"),
	VOLUME("volume"),
	MISSION("mission");

	private final String resourceType;

	ResourceTypes(String sResourceType) {
		this.resourceType = sResourceType;
	}

	public String getResourceType() {
		return resourceType;
	}	
}
