package wasdi.shared.config;

/**
 * Configuration of the notifications mail sent by WASDI to users.
 * @author p.campanella
 *
 */
public class NotificationsConfig {
	
	/**
	 * Address of the Mercurius service. 
	 * Mercurius is a CIMA service API to send e-mails.
	 */
	public String mercuriusAPIAddress;
	
	/**
	 * Title of the mail for password recovery
	 */
	public String pwRecoveryMailTitle;
	
	/**
	 * Sender of the mail for password recovery
	 */
	public String pwRecoveryMailSender;
	
	/**
	 * Text of the mail for password recovery
	 */
	public String pwRecoveryMailText;
	
	/**
	 * Title of the mail for a new sftp account 
	 */
	public String sftpMailTitle;
	/**
	 * Sender of the mail for a new sftp account
	 */
	public String sftpManagementMailSender;
	/**
	 * Text of the mail for a new sftp account
	 */
	public String sftpMailText;
	/**
	 * Declared WASDI admin mail
	 */
	public String wasdiAdminMail;
}
