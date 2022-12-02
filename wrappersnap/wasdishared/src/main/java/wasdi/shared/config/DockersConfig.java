package wasdi.shared.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import wasdi.shared.utils.Utils;

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
	
	/**
	 * The amout of time  (in millis) to wait for docker to complete delete operation
	 */
	public Integer millisWaitAfterDelete = 15000;
	
	/**
	 * The amout of time  (in millis) to wait after the deploy.sh file is created before proceeding (to be safe that the file is written completly)
	 */
	public Integer millisWaitAfterDeployScriptCreated = 2000;
	
	/**
	 * The amout of time  (in millis) to wait for the docker login operation to finish
	 */
	public Integer millisWaitForLogin= 4000;
	
	/**
	 * Configuration of the EoEpca related docker parameters
	 */
	public EOEPCAConfig eoepca;
}
