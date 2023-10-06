package wasdi.shared.config;

import java.util.ArrayList;

/**
 * Represent an external program or command that can be executed by WASDI
 * In the standard installation each command is executed with a Shell Execute
 * When wasdi is configured to be dockerized, some of these commands are replaced by Docker Commands.
 * 
 * The section is meant to be a dictionary whith the key representing the command and the value 
 * representing this Shell Exec Item Config that allows WASDI to understand how to map the command 
 * to the equivalent container
 * 
 * @author p.campanella
 *
 */
public class ShellExecItemConfig {
	
	/**
	 * Name of the docker images to use instead of the command
	 */
	public String dockerImage;
	
	/**
	 * Version of the docker image to use
	 */
	public String containerVersion;
	
	/**
	 * True by default, all the parts of the command line are passed as docker args
	 * If false, the first element of the shell execute will NOT be passed to the docker command line
	 */
	public boolean includeFirstCommand=true;
	
	/**
	 * False by default. If it is set to true, the command is executed locally also if WASDI 
	 * is configured to be dockerized.
	 */
	public boolean forceLocal = false;
	
	/**
	 * If true, WASDI will remove the path (if present) from the first command in the arg list
	 */
	public boolean removePathFromFirstArg = false;
	
	/**
	 * List of strings that are additional moung points for this specific docker
	 */
	public ArrayList<String> additionalMountPoints = new ArrayList<>();
}
