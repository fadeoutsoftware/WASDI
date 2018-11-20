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
	//MAYBE add a name field, should it become necessary 
	private String address;

	public String getAddress() {
		return address;
	}

	public void setAddress(String wPS) {
		address = wPS;
	}
}
