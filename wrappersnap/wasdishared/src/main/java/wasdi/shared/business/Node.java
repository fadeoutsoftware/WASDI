package wasdi.shared.business;

/**
 * WASDI Computing Node entity.
 * Nodes has a code and are identified by an IP Address.
 * 
 * Nodes can be disabled when are no longer avaiable.
 * 
 * @author p.campanella
 *
 */
public class Node {
	/**
	 * Unique code of the node
	 */
	private String nodeCode;
	/**
	 * Base URL
	 */
	private String nodeBaseAddress;	
	/**
	 * Node Description
	 */
	private String nodeDescription;
	/**
	 * GeoServer URL
	 */
	private String nodeGeoserverAddress;

	/**
	 * Default provider for the node
	 */
	private String defaultProvider;
	
	/**
	 * Flag to know if the node is active or not
	 */
	private boolean active = true;
	
	/**
	 * String that represents the cloud provider
	 */
	private String cloudProvider;
	
	

	public String getCloudProvider() {
		return cloudProvider;
	}
	public void setCloudProvider(String cloudProvider) {
		this.cloudProvider = cloudProvider;
	}
	public String getNodeGeoserverAddress() {
		return nodeGeoserverAddress;
	}
	public void setNodeGeoserverAddress(String nodeGeoserverAddress) {
		this.nodeGeoserverAddress = nodeGeoserverAddress;
	}
	
	public String getNodeCode() {
		return nodeCode;
	}
	public void setNodeCode(String nodeCode) {
		this.nodeCode = nodeCode;
	}
	public String getNodeBaseAddress() {
		return nodeBaseAddress;
	}
	public void setNodeBaseAddress(String nodeBaseAddress) {
		this.nodeBaseAddress = nodeBaseAddress;
	}
	public String getNodeDescription() {
		return nodeDescription;
	}
	public void setNodeDescription(String nodeDescription) {
		this.nodeDescription = nodeDescription;
	}

	public String getDefaultProvider() {
		return defaultProvider;
	}

	public void setDefaultProvider(String sValue) {
		this.defaultProvider = sValue;
	}
	public String getM_sDefaultProvider() {
		return defaultProvider;
	}
	public void setM_sDefaultProvider(String m_sDefaultProvider) {
		this.defaultProvider = m_sDefaultProvider;
	}
	public boolean getActive() {
		return active;
	}
	public void setActive(boolean bActive) {
		this.active = bActive;
	}

}
