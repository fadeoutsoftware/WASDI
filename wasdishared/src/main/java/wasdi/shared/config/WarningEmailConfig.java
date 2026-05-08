package wasdi.shared.config;

public class WarningEmailConfig {
	
	/**
	 * Object of the email
	 */
	public String title = "Storage exceeded in WASDI";
	
	/**
	 * Body of the email
	 */
	public String message = "Dear <user>\n, your storage space in WASDI is <storage_size>, which exceeds the maximum limit of <storage_limit> allowed by your subscription. "
			+ "Please, proceed to free up some storage space, otherwise some of your workspaces will be automatically deleted in <warning_delay> days.\n\n"
			+ "Kind regards,\n"
			+ "the WASDI team";
}
