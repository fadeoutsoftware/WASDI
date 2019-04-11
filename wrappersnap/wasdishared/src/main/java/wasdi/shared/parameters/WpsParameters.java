/**
 * Created by Cristiano Nattero on 2019-02-20
 * 
 * Fadeout software
 *
 */
package wasdi.shared.parameters;

/**
 * @author c.nattero
 *
 */
public class WpsParameters extends BaseParameter {

	//the provider, e.g., WASDI, 
	private String wpsProvider;
	private String wpsProviderUrl;
	private String xmlPayload;
	private String wpsOperationName;
	private String wpsProcessIdOnRemoteService;
	private String username;
	private String password;
	
	public String getWpsProvider() {
		return wpsProvider;
	}
	public void setWpsProvider(String wpsProvider) {
		this.wpsProvider = wpsProvider;
	}
	public String getWpsProviderUrl() {
		return wpsProviderUrl;
	}
	public void setWpsProviderUrl(String wpsProviderUrl) {
		this.wpsProviderUrl = wpsProviderUrl;
	}
	public String getXmlPayload() {
		return xmlPayload;
	}
	public void setXmlPayload(String xmlPayload) {
		this.xmlPayload = xmlPayload;
	}
	public String getWpsOperationName() {
		return wpsOperationName;
	}
	public void setWpsOperationName(String wpsOperationName) {
		this.wpsOperationName = wpsOperationName;
	}
	public String getWpsProcessIdOnRemoteService() {
		return wpsProcessIdOnRemoteService;
	}
	public void setWpsProcessIdOnRemoteService(String wpsProcessIdOnRemoteService) {
		this.wpsProcessIdOnRemoteService = wpsProcessIdOnRemoteService;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	
}
