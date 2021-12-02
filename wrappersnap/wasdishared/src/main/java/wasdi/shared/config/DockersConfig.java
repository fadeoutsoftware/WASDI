package wasdi.shared.config;

/**
 * Dockers configuration
 * @author p.campanella
 *
 */
public class DockersConfig {
	
	/**
	 * Extra hosts to add to the containers
	 * May be needed in some cloud for network reasons.
	 */
	public String extraHosts;
	
	/**
	 * Address to use to access pyPi to install waspy
	 */
	public String pipInstallWasdiAddress;
}
