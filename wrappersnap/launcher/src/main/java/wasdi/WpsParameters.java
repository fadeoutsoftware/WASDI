/**
 * Created by Cristiano Nattero on 2019-02-20
 * 
 * Fadeout software
 *
 */
package wasdi;

import wasdi.shared.parameters.BaseParameter;

/**
 * @author c.nattero
 *
 */
public class WpsParameters extends BaseParameter {

	private String sWpsProvider;
	private String sXmlPayload;
	private String sWpsOperationName;
	private String sWpsProcessIdOnRemoteService;
	private String sUsername;
	private String sPassword;
	
	public String getsWpsProvider() {
		return sWpsProvider;
	}
	public void setsWpsProvider(String sWpsProvider) {
		this.sWpsProvider = sWpsProvider;
	}
	public String getsXmlPayload() {
		return sXmlPayload;
	}
	public void setsXmlPayload(String sXmlPayload) {
		this.sXmlPayload = sXmlPayload;
	}
	public String getsWpsOperationName() {
		return sWpsOperationName;
	}
	public void setsWpsOperationName(String sWpsOperationName) {
		this.sWpsOperationName = sWpsOperationName;
	}
	public String getsWpsProcessIdOnRemoteService() {
		return sWpsProcessIdOnRemoteService;
	}
	public void setsWpsProcessIdOnRemoteService(String sWpsProcessIdOnRemoteService) {
		this.sWpsProcessIdOnRemoteService = sWpsProcessIdOnRemoteService;
	}
	public String getsUsername() {
		return sUsername;
	}
	public void setsUsername(String sUsername) {
		this.sUsername = sUsername;
	}
	public String getsPassword() {
		return sPassword;
	}
	public void setsPassword(String sPassword) {
		this.sPassword = sPassword;
	}
}
