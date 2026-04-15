package wasdi.shared.business;

import java.util.ArrayList;

import wasdi.shared.utils.Utils;

/**
 * JupyterNotebook Entity
 * Represents a JupyterNotebook associated to a specific user and to a specific workspace
 * 
 * @author PetruPetrescu
 *
 */
public class JupyterNotebook {

	/**
	 * Unique code of the Notebook, obtained by an hash of user and worspace
	 */
	private String code;
	
	/**
	 * Owner user id
	 */
	private String userId;
	
	/**
	 * Workpsace where the notebook is hosted
	 */
	private String workspaceId;
	
	/**
	 * Url to reach the notebook
	 */
	private String url;
	
	/**
	 * String that represents the list of users:ip allowed to access this notebook, concatenated with semicolon. user1:ip1;user2:ip2 ...
	 */
	private String allowedIpAddresses;
	
	/**
	 * Converts the allowedIpAddresses in a list of string representing allowed Ip
	 * @return
	 */
	public ArrayList<String> extractListOfWhiteIp() {
		
		ArrayList<String> asWhiteIps = new ArrayList<String>();
		
		if (!Utils.isNullOrEmpty(allowedIpAddresses)) {
			String [] asUserAndIp =  allowedIpAddresses.split(";");
			
			if (asUserAndIp != null) {
				
				for (String sUserAndIp : asUserAndIp) {
					
					String asUserIp[] = sUserAndIp.split(":");
					
					if (asUserIp != null) {
						if (asUserIp.length>1) {
							asWhiteIps.add(asUserIp[1]);
						}
					}
				}
			}
		}
			
		return asWhiteIps;
	}
	
	/**
	 * Removes one instance of user:ip in the allowedIpAddresses starting from the user name
	 * @param sUserId User to search and remove
	 * @return
	 */
	public String removeUserFromAllowedIp(String sUserId) {
		
		String sNewList = "";
		
		if (!Utils.isNullOrEmpty(allowedIpAddresses)) {
			
			String [] asUserAndIp =  allowedIpAddresses.split(";");
			
			if (asUserAndIp != null) {
				
				for (String sUserAndIp : asUserAndIp) {
					
					String asUserIp[] = sUserAndIp.split(":");
					
					if (asUserIp != null) {
						if (asUserIp.length>1) {
							
							if (!sUserId.equals(asUserIp[0])) {
								sNewList += sUserAndIp+";";
							}
						}
					}
				}
			}
			
		}
		
		if (sNewList.endsWith(";")) {
			sNewList =sNewList.substring(0, sNewList.length()-1);
		}
		
		allowedIpAddresses = sNewList;
		
		return sNewList;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getWorkspaceId() {
		return workspaceId;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getAllowedIpAddresses() {
		return allowedIpAddresses;
	}

	public void setAllowedIpAddresses(String allowedIpAddresses) {
		this.allowedIpAddresses = allowedIpAddresses;
	}
	
	

}
