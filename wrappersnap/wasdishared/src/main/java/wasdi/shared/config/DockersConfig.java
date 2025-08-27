package wasdi.shared.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

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
	 * The amount of time (in millis) to wait between the attempts to check if the docker is started
	 */
	public Integer millisBetweenAttmpts = 5000;
	
	/**
	 * The amount of time (in millis) to wait between the calls to the docker engine api to request the status of a docker
	 */
	public Integer millisBetweenStatusPolling = 1000;
	
	/**
	 * Set the number of cycles that must be executed before the waitContainer function logs something
	 * to show that is alive
	 */
	public Integer numberOfPollStatusPollingCycleForLog = 30;
	
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
	
	/**
	 * Command to use to start docker compose
	 */
	public String dockerComposeCommand = "docker-compose";
	
	/**
	 * List of docker registries supported
	 */
	public ArrayList<DockerRegistryConfig> registers;
	
	/**
	 * Address of the API of the local Docker instance
	 */
	public String internalDockerAPIAddress = "http://127.0.0.1:2375/";
	
	/**
	 * Path of file with logs of the dockers build 
	 */
	public String dockersDeployLogFilePath = "/var/log/wasdi/dockers.log";
	
	/**
	 * Set to true to enable the log of the payload for the calls made to the Docker Engine API
	 */
	public boolean logDockerAPICallsPayload = false;
	/**
	 * Map the local shell exec commands in equivalent docker commands
	 */
	public Map<String, ShellExecItemConfig> shellExecCommands = new HashMap<>();
	
	/**
	 * If true, it removes the containers created for each shell execute
	 * If false, for debug, it will keep the containers for access the logs
	 */
	public boolean removeDockersAfterShellExec = true;
	
	/**
	 * All the dockers that makes a shell exec of a python script exchanges input and output files.
	 * When this flag is true these are automatically deleted after the operation is done.
	 * Keep true in production
	 * Can be set to false to save some of these files if needed for debug
	 */
	public boolean removeParameterFilesForPythonsShellExec = true;
	
	/**
	 * Docker Network mode. Note: can be overridden by the single shell Exec Items
	 */
	public String dockerNetworkMode ="net-wasdi";
	
	/**
	 * Standard processors internal port
	 */
	public int processorsInternalPort = 5000;
	
	/**
	 * Configuration of the processor types
	 */
	public List<ProcessorTypeConfig> processorTypes = new ArrayList<>();
	
	/**
	 * List of group id to add to the docker create command
	 */
	public List<String> groupAdd = new ArrayList<>();
	
	/**
	 * Get the list of registers ordered by priority
	 * @return Ordered list of registers
	 */
	public ArrayList<DockerRegistryConfig> getRegisters() {
		try {
			Collections.sort(registers, Comparator.comparing(DockerRegistryConfig::getPriority));
		}		
		catch (Exception oEx) {
			WasdiLog.errorLog("DockersConfig.getRegisters: Exception ordering the registers list");
		}
		
		return registers;
	}
	
	/**
	 * Get one docker register config from the address
	 * @param sAddress Address to search
	 * @return The config or null
	 */
	public DockerRegistryConfig getRegisterByAddress(String sAddress) {
		try {
			for (DockerRegistryConfig oDockerRegistryConfig : registers) {
				if (!sAddress.equals(oDockerRegistryConfig.address)) continue;
				return oDockerRegistryConfig;
			}
		}		
		catch (Exception oEx) {
			WasdiLog.errorLog("DockersConfig.getRegisters: Exception ordering the registers list");
		}
		
		return null;	
	}
	
	/**
	 * Safe get a ShellExecItemConfig
	 * @param sCommand Command we are searching for
	 * @return Equivalent ShellExecItemConfig or null in case of any problem
	 */
	public ShellExecItemConfig getShellExecItem(String sCommand) {
		
		// Check if we have the  command
		if (Utils.isNullOrEmpty(sCommand)) {
			WasdiLog.warnLog("DockersConfig.getShellExecItem: the command is null or empty");
			return null;
		}
		
		// Check if we have the maps of commands
		if (shellExecCommands == null) {
			WasdiLog.warnLog("DockersConfig.getShellExecItem: the map dictionary is null");
			return null;			
		}
		
		try {
			
			// Get just the command, without any path
			File oCommandAsFile = new File(sCommand);
			String sSimplifiedCommand = oCommandAsFile.getName();
			
			// We need to have a command!
			if (Utils.isNullOrEmpty(sSimplifiedCommand)) {
				WasdiLog.warnLog("DockersConfig.getShellExecItem: impossible to get the command without paths");
				return null;				
			}
			
			// Is this in the map?
			if (shellExecCommands.containsKey(sSimplifiedCommand)) {
				// Ok return the right ShellExecItemConfig
				return shellExecCommands.get(sSimplifiedCommand);
			}
			else {
				// We do not have it
				WasdiLog.warnLog("DockersConfig.getShellExecItem: command not found " + sCommand);
				return null;
			}
			
		}
		catch (Exception oEx) {
			// What happened?
			WasdiLog.errorLog("DockersConfig.getShellExecItem: Exception getting the command " + sCommand, oEx);
			return null;
		}
	}
	
	/**
	 * Utility method to quickly get a Processor Type Config
	 * @param sProcessorType
	 * @return
	 */
	public ProcessorTypeConfig getProcessorTypeConfig(String sProcessorType) {
		
		if (Utils.isNullOrEmpty(sProcessorType)) {
			WasdiLog.warnLog("DockersConfig.getProcessorTypeConfig: sProcessorType is null");
			return null;			
		}
		
		if (this.processorTypes == null) {
			WasdiLog.warnLog("DockersConfig.getProcessorTypeConfig: there are no processor Types configured");
			return null;						
		}
		
		try {
			
			for (ProcessorTypeConfig oProcessorTypeConfig : processorTypes) {
				if (Utils.isNullOrEmpty(oProcessorTypeConfig.processorType)) continue;
				if (oProcessorTypeConfig.processorType.equals(sProcessorType)) return oProcessorTypeConfig;
			}
		}
		catch (Exception oEx) {
			// What happened?
			WasdiLog.errorLog("DockersConfig.getShellExecItem: Exception getting the processor Type config " + sProcessorType, oEx);
		}	
		
		return null;
	}
	
}
