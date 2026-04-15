package wasdi.shared.business;

/**
 * Reprents a cloud provider where a WASDI node can be installed
 * 
 * @author p.campanella
 *
 */
public class CloudProvider {
	
	public CloudProvider() {
	
	}
	
	/**
	 * Unique Cloud Provider Guid 
	 */
	private String cloudProviderId;
	
	/**
	 * Cloud Provider Code as declared in the Node entity
	 */
	private String code;
	
	/**
	 * main link to the cloud provider site
	 */
	private String mainLink;
	
	/**
	 * Mail to contact the cloud provider
	 */
	private String contact;
	
	/**
	 * Direct link to the the SLA of the cloud provider
	 */
	private String slaLink;
	
	/**
	 * Brief description
	 */
	private String description;

	public String getCloudProviderId() {
		return cloudProviderId;
	}

	public void setCloudProviderId(String cloudProviderId) {
		this.cloudProviderId = cloudProviderId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMainLink() {
		return mainLink;
	}

	public void setMainLink(String mainLink) {
		this.mainLink = mainLink;
	}

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public String getSlaLink() {
		return slaLink;
	}

	public void setSlaLink(String slaLink) {
		this.slaLink = slaLink;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
