package wasdi.shared.config;

import java.util.ArrayList;

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
	public ArrayList<String> extraHosts;
	
	/**
	 * Address to use to access pyPi to install waspy
	 */
	public String pipInstallWasdiAddress;
	
	/**
	 * Address to use to reach the internal dockers
	 */
	public String internalDockersBaseAddress = "localhost";

	/**
	 * Number of attempt to try to ping the server before deciding that the server is down
	 */
	public Integer numberOfAttemptsToPingTheServer = 4;

	/**
	 * The amount of time (in millis) to wait between the attempts
	 */
	public Integer millisBetweenAttmpts = 5000;

}
