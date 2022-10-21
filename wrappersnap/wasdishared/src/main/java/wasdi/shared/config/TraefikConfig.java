package wasdi.shared.config;

import java.util.ArrayList;

/**
 * Represents the options we need for the traefik proxy used to access Jupyter Notebooks
 * 
 * @author p.campanella
 *
 */
public class TraefikConfig {
	
	/**
	 * List of the local or external ip addresses white listed to access the Jupyter Lab 
	 */
	public ArrayList<String> firewallWhiteList;

}
