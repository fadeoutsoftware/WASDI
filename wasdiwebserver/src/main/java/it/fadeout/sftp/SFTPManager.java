package it.fadeout.sftp;

import wasdi.shared.utils.log.WasdiLog;

/**
 * Class for sftp account management. In each WASDI Node a little web-socket server is installed.
 * This mini-server handles a simple protocol: 
 * COMMAND	PAYLOAD
 * Commands are:
 * exists_account	USERID			 : check if an sftp account for UserId exists on the server
 * create_account	USERID	PASSWORD : created a new sftp account for UserId with PASSWORD
 * update_password	USERID	PASSWORD : updates the USERID password
 * remove_account	USERID			 : removes the USERID sftp account
 * list				USERID			 : get a list of the files of the user
 * 
 * The server answers:
 * [OK|KO];[MESSAGE]
 * 
 * SFTP accounts are created on the server in the folder:
 * /data/sftpuser/[USERID]
 * 
 * 
 * @author doy
 *
 */
public class SFTPManager {
	
	/**
	 * Local server address
	 */
	String m_sAddress;
	
	/**
	 * Constructor with address of the local web-socket sftp manager server	
	 * @param m_sAddress web-socket sftp manager server address
	 */
	public SFTPManager(String sAddress) {
		this.m_sAddress = sAddress;
	}
	
	/**
	 * Internal 
	 * @param sCommand command to send
	 * @return Server response
	 */
	private String sendCommand(String sCommand) {
		try {
			WsClient oClient = new WsClient(m_sAddress, sCommand);
			if (oClient.isOk()) return oClient.getData();		
			WasdiLog.debugLog("ERROR: " + oClient.getData());		
		} catch (InterruptedException e) {
			WasdiLog.errorLog("SFTPManager.sendCommand:  error", e);
			Thread.currentThread().interrupt();
		}				
		return null;
	}
	
	/**
	 * Checks if a user sftp account exists
	 * @param sUser UserId to check
	 * @return true if sUser can connect to sftp server
	 */
	public boolean checkUser(String sUser) {
		return sendCommand("exists_account " + sUser) != null;
	}

	/**
	 * create a new account for sftp service
	 * @param sUser User Id
	 * @param sPassword Password to associate
	 * @return true if created, false otherwise
	 */
	public boolean createAccount(String sUser, String sPassword) {			
		return sendCommand("create_account " + sUser + " " + sPassword) != null;
	}

	/**
	 * update a user password in sftp service
	 * @param sUser user id
	 * @param sPassword new password
	 * @return True if changed
	 */
	public boolean updatePassword(String sUser, String sPassword) {			
		return sendCommand("update_password " + sUser + " " + sPassword) != null;
	}

	/**
	 * remove an account from sftp service
	 * @param sUser user id to delete
	 * @return true if deleted
	 */
	public boolean removeAccount(String sUser) {			
		return sendCommand("remove_account " + sUser) != null;
	}

	/**
	 * list the files in the sftp server user home 
	 * @param sUser user id
	 * @return Array of strings: each represent a file in the user sftp home in WASDI
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
