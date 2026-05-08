package wasdi.shared.viewmodels.nodes;

/**
 * Represents a WASDI Node.
 * 
 * Created by m.menapace on 11/02/2021
 */
public class NodeViewModel {

    private String nodeCode;
    private String cloudProvider;
    private String apiUrl;
    
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
}