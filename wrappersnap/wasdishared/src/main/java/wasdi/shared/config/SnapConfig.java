package wasdi.shared.config;

/**
 * Snap configuration
 * @author p.campanella
 *
 */
public class SnapConfig {
	/**
	 * Snap Aux Properties file full path
	 */
	public String auxPropertiesFile = "/usr/lib/wasdi/launcher/snap.auxdata.properties";
	/**
	 * Flag to activate snap logs in the launcher
	 */
	public String launcherLogActive = "1";
	/**
	 * Flag to activate snap logs in the web server
	 */
	public String webLogActive = "1";
	/**
	 * Snap Launcher log file
	 */
	public String launcherLogFile = "";
	/**
	 * Snap web server log file
	 */
	public String webLogFile = "";
	/**
	 * SNAP Launcher log level
	 */
	public String launcherLogLevel="SEVERE";
	/**
	 * SNAP web server log level
	 */
	public String webLogLevel="SEVERE";
}
