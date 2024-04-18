package wasdi.shared.viewmodels.nodes;

import wasdi.shared.business.Node;

public class NodeFullViewModel {
    private String nodeCode;
    private String cloudProvider;
    private String apiUrl;
    private String nodeDescription;
    private String nodeGeoserverAddress;
    private boolean active;
    private boolean shared;
    
	public String getNodeCode() {
		return nodeCode;
	}
	public void setNodeCode(String nodeCode) {
		this.nodeCode = nodeCode;
	}
	public String getCloudProvider() {
		return cloudProvider;
	}
	public void setCloudProvider(String cloudProvider) {
		this.cloudProvider = cloudProvider;
	}
	
	public String getApiUrl() { return apiUrl; }
	
	public void setApiUrl(String apiUrl) {this.apiUrl = apiUrl; }
	
	public String getNodeDescription() {
		return nodeDescription;
	}
	public void setNodeDescription(String nodeDescription) {
		this.nodeDescription = nodeDescription;
	}
	public String getNodeGeoserverAddress() {
		return nodeGeoserverAddress;
	}
	public void setNodeGeoserverAddress(String nodeGeoserverAddress) {
		this.nodeGeoserverAddress = nodeGeoserverAddress;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public boolean isShared() {
		return shared;
	}
	public void setShared(boolean shared) {
		this.shared = shared;
	}
	
	public static NodeFullViewModel fromEntity(Node oNode) {
		if (oNode == null) return null;
		
		NodeFullViewModel oNodeFullViewModel = new NodeFullViewModel();
		oNodeFullViewModel.setActive(oNode.getActive());
		oNodeFullViewModel.setApiUrl(oNode.getNodeBaseAddress());
		oNodeFullViewModel.setCloudProvider(oNode.getCloudProvider());
		oNodeFullViewModel.setNodeCode(oNode.getNodeCode());
		oNodeFullViewModel.setNodeDescription(oNode.getNodeDescription());
		oNodeFullViewModel.setNodeGeoserverAddress(oNode.getNodeGeoserverAddress());
		oNodeFullViewModel.setShared(oNode.getShared());
		
		return oNodeFullViewModel;
	}
	
	public static Node toEntity(NodeFullViewModel oNodeFullViewModel) {
		if (oNodeFullViewModel == null) return null;
		
		Node oNode = new Node();
		oNode.setActive(oNodeFullViewModel.isActive());
		oNode.setCloudProvider(oNodeFullViewModel.getCloudProvider());
		oNode.setNodeBaseAddress(oNodeFullViewModel.getApiUrl());
		oNode.setNodeCode(oNodeFullViewModel.getNodeCode());
		oNode.setNodeDescription(oNodeFullViewModel.getNodeDescription());
		oNode.setNodeGeoserverAddress(oNodeFullViewModel.getNodeGeoserverAddress());
		oNode.setShared(oNodeFullViewModel.isShared());
		
		return oNode;
	}
}
