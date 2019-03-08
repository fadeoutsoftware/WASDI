/*
 * WPS providers entity
 * 
 * Created by Cristiano Nattero on 2018.11.16
 * 
 * Fadeout software
 * 
 */

package wasdi.shared.business;

public class WpsProvider {
 
	//WASDI address to call
	private String address;
	
	//label for the provider
	private String provider;
	
	//real provider to be called by our proxy
	private String providerUrl;

	public String getAddress() {
		return address;
	}

	public void setAddress(String wPS) {
		address = wPS;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getProviderUrl() {
		return providerUrl;
	}

	public void setProviderUrl(String providerUrl) {
		this.providerUrl = providerUrl;
	}
}
