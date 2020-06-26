package wasdi.shared.business;

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
	private String m_sDefaultProvider;
	
	
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
		return m_sDefaultProvider;
	}

	public void setDefaultProvider(String sValue) {
		this.m_sDefaultProvider = sValue;
	}
}
