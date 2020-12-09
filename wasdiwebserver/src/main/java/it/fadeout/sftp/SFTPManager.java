package it.fadeout.sftp;

import wasdi.shared.utils.Utils;

/**
 * class for sftp account management
 * @author doy
 *
 */
public class SFTPManager {
	
	String m_sAddress;
	
	public SFTPManager(String m_sAddress) {
		this.m_sAddress = m_sAddress;
	}

	private String sendCommand(String sCommand) {
		try {
			WsClient oClient = new WsClient(m_sAddress, sCommand);
			if (oClient.isOk()) return oClient.getData();		
			Utils.debugLog("ERROR: " + oClient.getData());		
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}				
		return null;
	}
	
	/**
	 * @param sUser
	 * @return true if sUser can connect to sftp server
	 */
	public boolean checkUser(String sUser) {
		return sendCommand("exists_account " + sUser) != null;
	}

	/**
	 * create a new account for sftp service
	 * @param sUser
	 * @param sPassword
	 * @return 
	 */
	public boolean createAccount(String sUser, String sPassword) {			
		return sendCommand("create_account " + sUser + " " + sPassword) != null;
	}

	/**
	 * update a user password in sftp service
	 * @param sUser
	 * @param sPassword
	 * @return 
	 */
	public boolean updatePassword(String sUser, String sPassword) {			
		return sendCommand("update_password " + sUser + " " + sPassword) != null;
	}

	/**
	 * remove an account from sftp service
	 * @param sUser
	 * @return 
	 */
	public boolean removeAccount(String sUser) {			
		return sendCommand("remove_account " + sUser) != null;
	}

	/**
	 * list the files in the sftp server user home 
	 * @param sUser
	 * @return
	 */
	public String[] list(String sUser) {
		String sCommand = sendCommand("list " + sUser);
		if (sCommand != null) {
			return sCommand.split("__WASDI_SEPARATOR__");
		}
		else {
			return null;
		}
	}
}
