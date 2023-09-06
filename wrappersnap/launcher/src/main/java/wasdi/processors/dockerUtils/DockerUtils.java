package wasdi.processors.dockerUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.processors.dockerUtils.containersViewModels.AuthToken;
import wasdi.processors.dockerUtils.containersViewModels.ContainerInfo;
import wasdi.processors.dockerUtils.containersViewModels.CreateParams;
import wasdi.processors.dockerUtils.containersViewModels.LoginInfo;
import wasdi.processors.dockerUtils.containersViewModels.MountVolumeParam;
import wasdi.processors.dockerUtils.containersViewModels.constants.ContainerStates;
import wasdi.processors.dockerUtils.containersViewModels.constants.MountTypes;
import wasdi.shared.business.Processor;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.viewmodels.HttpCallResponse;

/**
 * Wrap main docker functionalities
 *
 * @author p.campanella
 */
public class DockerUtils {

    /**
     * Wasdi Processor
     */
    protected Processor m_oProcessor;

    /**
     * Folder of the processor
     */
    protected String m_sProcessorFolder;

    /**
     * Log file for docker operations
     */
    protected String m_sDockerLogFile = WasdiConfig.Current.dockers.dockersDeployLogFilePath;

    /**
     * User that run the docker
     */
    protected String m_sUser = "tomcat";    

    /**
     * Docker registry in use
     */
    protected String m_sDockerRegistry = "";
    
    /**
     * Create a new instance
     *
     * @param oProcessor       Processor
     * @param sProcessorFolder Processor Folder
     * @param sWorkingRootPath WASDI Working path
     * @param sTomcatUser      User
     */
    public DockerUtils(Processor oProcessor, String sProcessorFolder, String sTomcatUser) {
    	this(oProcessor, sProcessorFolder, sTomcatUser, "");
    }
    
    /**
     * Create a new instance
     * 
     * @param oProcessor       Processor
     * @param sProcessorFolder Processor Folder
     * @param sWorkingRootPath WASDI Working path
     * @param sTomcatUser      User
     * @param sDockerRegistry  Docker Registry to use. By default is "", means local registry
     */
    public DockerUtils(Processor oProcessor, String sProcessorFolder, String sTomcatUser, String sDockerRegistry) {
        m_oProcessor = oProcessor;
        m_sProcessorFolder = sProcessorFolder;
        m_sUser = sTomcatUser;
        m_sDockerRegistry = sDockerRegistry;
    }    
    
    /**
     * Get the processor entity
     * @return Processor
     */
	public Processor getProcessor() {
		return m_oProcessor;
	}
	
	/**
	 * Set the processor Entity
	 * @param oProcessor Processor Entity
	 */
	public void setProcessor(Processor oProcessor) {
		this.m_oProcessor = oProcessor;
	}
	
	/**
	 * Get the processor Folder
	 * @return processor Folder
	 */
	public String getProcessorFolder() {
		return m_sProcessorFolder;
	}

	/**
	 * Set the processor Folder
	 * @param sProcessorFolder Processor Folder
	 */
	public void setProcessorFolder(String sProcessorFolder) {
		this.m_sProcessorFolder = sProcessorFolder;
	}
	
	/**
	 * Get the path of the log file for docker
	 * @return
	 */
	public String getDockerLogFile() {
		return m_sDockerLogFile;
	}
	
	/**
	 * Set the path of the log file for docker
	 * @param sDockerLogFile
	 */
	public void setDockerLogFile(String sDockerLogFile) {
		this.m_sDockerLogFile = sDockerLogFile;
	}

	/**
	 * Get the name of user to pass to docker. Empty string to avoid.
	 * @return
	 */
	public String getUser() {
		return m_sUser;
	}
	
	/**
	 * Set the name of user to pass to docker. Empty string to avoid.
	 * @param sUser
	 */
	public void setUser(String sUser) {
		this.m_sUser = sUser;
	}    

	/**
	 * Address of the Docker Register in use. 
	 * @return id of th
	 */
	public String getDockerRegistry() {
		return m_sDockerRegistry;
	}

	/**
	 * Get the Address of the docker registry in use. "" to refer local registry
	 * @param sDockerRegistry
	 */
	public void setDockerRegistry(String sDockerRegistry) {
		this.m_sDockerRegistry = sDockerRegistry;
	}

    /**
     * Deploy a docker
     * @return the docker image tag if ok, empty string in case of problems
     */
    public String build() {
    	
    	String sDockerName = "";

        try {

            // Generate Docker Name
            String sProcessorName = m_oProcessor.getName();
            
            // Prepare the name of the docker
            String sDockerBaseName = "wasdi/" + sProcessorName + ":" + m_oProcessor.getVersion();
            
            sDockerName = sDockerBaseName;
            
            // Do we have a registry?
            if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
            	// Yes, add it to the docker name
            	WasdiLog.debugLog("DockerUtils.build: using registry " + m_sDockerRegistry);
            	sDockerName = m_sDockerRegistry + "/" + sDockerBaseName;
            }
            
            // Initialize Args
            ArrayList<String> asArgs = new ArrayList<>();

            // Generate shell script file
            String sBuildScriptFile = m_sProcessorFolder + "deploywasdidocker.sh";
            File oBuildScriptFile = new File(sBuildScriptFile);
            
            try (BufferedWriter oBuildScriptWriter = new BufferedWriter(new FileWriter(oBuildScriptFile))) {
                // Fill the script file
                if (oBuildScriptWriter != null) {
                    WasdiLog.debugLog("DockerUtils.build: Creating " + sBuildScriptFile + " file");

                    oBuildScriptWriter.write("#!/bin/bash");
                    oBuildScriptWriter.newLine();
                    oBuildScriptWriter.write("echo Deploy Docker Started >> " + m_sDockerLogFile);
                    oBuildScriptWriter.newLine();
                    
                    // Are we building locally or for a register?
                    if (Utils.isNullOrEmpty(m_sDockerRegistry)) {
                    	// Local build
                    	oBuildScriptWriter.write("docker build -t" + sDockerName + " " + m_sProcessorFolder);
                    }
                    else {
                    	WasdiLog.debugLog("DockerUtils.build: multi tag build ");
                    	
                    	// Register: the first tag is already in the Docker Name
                    	String sMultiTagBuild = "docker build -t" + sDockerName;
                    	
                    	// Take all our registers
                    	List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
                    	
                    	// For each register
                    	for (DockerRegistryConfig oDockerRegistryConfig : aoRegisters) {
                    		// Is this the main one? So jump, already added
							if (oDockerRegistryConfig.address.equals(m_sDockerRegistry)) continue;
							
							// No, ok, we can add a new tag
							sMultiTagBuild += " -t" +  oDockerRegistryConfig.address + "/"  + sDockerBaseName;
							
							WasdiLog.debugLog("DockerUtils.build: added tag  for " + oDockerRegistryConfig.id);
						}
                    	
                    	// Last the space and the folder to build
                    	sMultiTagBuild += " " + m_sProcessorFolder;
                    	
                    	// We can write our script
                    	oBuildScriptWriter.write(sMultiTagBuild);
                    }
                    
                    if (!Utils.isNullOrEmpty(m_sUser)) {
                        oBuildScriptWriter.write(" --build-arg USR_NAME=" + m_sUser + " --build-arg USR_ID=$(id -u " + m_sUser + ")" +
                                " --build-arg GRP_NAME=" + m_sUser + " --build-arg GRP_ID=$(id -g " + m_sUser + ")");                    	
                    }
                    
                    if (!Utils.isNullOrEmpty(WasdiConfig.Current.dockers.pipInstallWasdiAddress)) {
                    	oBuildScriptWriter.write(" --build-arg PIP_INSTALL_WASDI_ARGUMENTS=\"" + WasdiConfig.Current.dockers.pipInstallWasdiAddress+"\""); 
                    }
                      
                    oBuildScriptWriter.write(" $1 >> " + m_sDockerLogFile + " 2>&1");
                    
                    oBuildScriptWriter.newLine();
                    oBuildScriptWriter.write("echo Deploy Docker Done >> " + m_sDockerLogFile);
                    oBuildScriptWriter.flush();
                    oBuildScriptWriter.close();
                }
            }

            // Wait a little bit to let the file be written
            Thread.sleep(WasdiConfig.Current.dockers.millisWaitAfterDeployScriptCreated);

            // Make it executable
            RunTimeUtils.addRunPermission(sBuildScriptFile);

            // Run the script
            RunTimeUtils.shellExec(sBuildScriptFile, asArgs);

            WasdiLog.debugLog("DockerUtils.build: created image " + sDockerName);
        } catch (Exception oEx) {
        	WasdiLog.errorLog("DockerUtils.build: " + oEx.toString());
            return "";
        }

        return sDockerName;
    }
    
    

    /**
     * Run the docker
     */
    public boolean start() {
        return start("");
    }
    
    /**
     * Run the docker
     * 
     * @param sMountWorkspaceFolder workspace folder to mount
     * 
     */
    public boolean start(String sMountWorkspaceFolder) {
        return start2(sMountWorkspaceFolder, m_oProcessor.getPort());
    }    

    /**
     * Run the docker at the specified port
     *
     * @param sMountWorkspaceFolder workspace folder to mount
     * @param iProcessorPort Port to use
     */
    public boolean start(String sMountWorkspaceFolder, int iProcessorPort) {

        try {
        	
        	if (Utils.isNullOrEmpty(sMountWorkspaceFolder)) {
        		sMountWorkspaceFolder = WasdiConfig.Current.paths.downloadRootPath;
        		if (!sMountWorkspaceFolder.endsWith(File.separator)) sMountWorkspaceFolder += File.separator;
        	}
        	
            // Get the docker name
            String sDockerName = "wasdi/" + m_oProcessor.getName() + ":" + m_oProcessor.getVersion();
            
            if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
            	sDockerName = m_sDockerRegistry + "/" + sDockerName;
            }
            
            String sCommand = "docker";

            // Initialize Args
            ArrayList<String> asArgs = new ArrayList<>();

            // Processor Script File
            String sRunFile = m_sProcessorFolder + "runwasdidocker.sh";
            File oRunFile = new File(sRunFile);

            // Check if it is already done:

            //if (!oRunFile.exists() || oRunFile.length() == 0L) {
            if (true) {

                // Command
                asArgs.add(sCommand);

                // Action
                asArgs.add("run");

                // User
                asArgs.add("-u$(id -u " + m_sUser + "):$(id -g " + m_sUser + ")");

                // Working Path
                asArgs.add("-v" + sMountWorkspaceFolder + ":/data/wasdi");

                // Processor folder
                asArgs.add("--mount");
                asArgs.add("type=bind,src=" + m_sProcessorFolder + ",dst=/wasdi");

                // Port
                asArgs.add("-p127.0.0.1:" + iProcessorPort + ":5000");

                // Extra hosts mapping, useful for some instances when the server host can't be resolved
                // The symptoms of such problem is that the POST call from the Docker container timeouts
                if (WasdiConfig.Current.dockers.extraHosts != null) {
                	
                	if (WasdiConfig.Current.dockers.extraHosts.size()>0) {
                		WasdiLog.debugLog("DockerUtils.start adding configured extra host mapping to the run arguments");
                    	for (int iExtraHost = 0; iExtraHost<WasdiConfig.Current.dockers.extraHosts.size(); iExtraHost ++) {
                    		
                    		String sExtraHost = WasdiConfig.Current.dockers.extraHosts.get(iExtraHost);
                    		
                    		asArgs.add("--add-host=" + sExtraHost);
                    	}
                	}
                }
				
                // Add the pull command
                if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {	                	
                	asArgs.add("--pull always");
                }				
                
                if (!Utils.isNullOrEmpty(WasdiConfig.Current.dockers.additionalDockerRunParameter)) {
                	asArgs.add(WasdiConfig.Current.dockers.additionalDockerRunParameter);
                }

                // Docker name
                asArgs.add(sDockerName);

                // Generate the command line
                String sCommandLine = "";


                for (String sArg : asArgs) {
                    sCommandLine += sArg + " ";
                }

                WasdiLog.debugLog("DockerUtils.start CommandLine: " + sCommandLine);

                try (BufferedWriter oRunWriter = new BufferedWriter(new FileWriter(oRunFile))) {
                    if (null != oRunWriter) {
                        WasdiLog.debugLog("DockerUtils.start: Creating " + sRunFile + " file");

                        oRunWriter.write("#!/bin/bash");
                        oRunWriter.newLine();
                        oRunWriter.write("echo Run Docker Started ");
                        oRunWriter.newLine();
                        oRunWriter.write(sCommandLine);
                        oRunWriter.newLine();
                        oRunWriter.write("echo Run Docker Done");
                        oRunWriter.flush();
                        oRunWriter.close();
                    }
                }

                RunTimeUtils.addRunPermission(sRunFile);

                asArgs.clear();
            }
            
            // Check if we need to login in the registry
            if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
            	WasdiLog.debugLog("DockerUtils.start: login in the registers");
            	
            	List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
            	
				// For each register: ordered by priority
				for (int iRegisters=0; iRegisters<aoRegisters.size(); iRegisters++) {
					
					DockerRegistryConfig oDockerRegistryConfig = aoRegisters.get(iRegisters);
					
					WasdiLog.debugLog("DockerUtils.start: logging in to " + oDockerRegistryConfig.id);
					
					// Try to login
					String sToken = loginInRegistry(oDockerRegistryConfig);
					
					if (Utils.isNullOrEmpty(sToken)) {
						WasdiLog.debugLog("DockerUtils.start: error in the login");
					}
				}
            }
            

            try {
                if (!oRunFile.canExecute()) {
                	RunTimeUtils.addRunPermission(sRunFile);
                }
            } catch (Exception oInnerEx) {
            	RunTimeUtils.addRunPermission(sRunFile);
            }

            // Execute the command to start the docker
            RunTimeUtils.shellExec(sRunFile, asArgs, false);

            WasdiLog.debugLog("DockerUtils.start " + sDockerName + " started");
        } catch (Exception oEx) {
        	WasdiLog.errorLog("DockerUtils.start error: " + oEx.toString());
            return false;
        }

        return true;
    }
    
    /**
     * Run the docker at the specified port
     *
     * @param sMountWorkspaceFolder workspace folder to mount
     * @param iProcessorPort Port to use
     */
    public boolean start2(String sMountWorkspaceFolder, int iProcessorPort) {

        try {
        	
        	if (Utils.isNullOrEmpty(sMountWorkspaceFolder)) {
        		sMountWorkspaceFolder = WasdiConfig.Current.paths.downloadRootPath;
        		if (!sMountWorkspaceFolder.endsWith(File.separator)) sMountWorkspaceFolder += File.separator;
        	}        	
        	
            // Get the Image name
            String sImageName = "wasdi/" + m_oProcessor.getName() + ":" + m_oProcessor.getVersion();
            if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
            	sImageName = m_sDockerRegistry + "/" + sImageName;
            }
            
            WasdiLog.debugLog("DockerUtils.start: try to start a container from image " + sImageName);
            
            // Authentication Token for the registry (if needed)
            String sToken = "";
            // Name of the container
            String sContainerName = "";
            
            // Check if we need to login in the registry
            if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
            	
            	WasdiLog.debugLog("DockerUtils.start: login in the register");
            	
            	List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
            	
				// For each register: ordered by priority
				for (int iRegisters=0; iRegisters<aoRegisters.size(); iRegisters++) {
					
					DockerRegistryConfig oDockerRegistryConfig = aoRegisters.get(iRegisters);
					
					if (!m_sDockerRegistry.equals(oDockerRegistryConfig.address)) continue;
					
					WasdiLog.debugLog("DockerUtils.start: logging in to " + oDockerRegistryConfig.id);
					
					// Try to login
					sToken = loginInRegistry(oDockerRegistryConfig);
					
					if (Utils.isNullOrEmpty(sToken)) {
						WasdiLog.errorLog("DockerUtils.start: error in the login this will be a problem!!");
					}
					
					break;
				}
            }
        	
            // Search first of all if the container is already here
        	ContainerInfo oContainerInfo = getContainerInfo(m_oProcessor.getName(), m_oProcessor.getVersion());
        	
        	if (oContainerInfo == null) {
        		
        		// No we do not have it
        		WasdiLog.debugLog("DockerUtils.start: the container is not available");
        		
        		// Do we have at least the image?
        		if (isImageAvailable(m_oProcessor) == false) {
        			
        			// No, we need to pull the image
        			WasdiLog.debugLog("DockerUtils.start: also the image is not available: pull it");
        			boolean bPullResult = pull(sImageName, sToken);
        			
        			if (!bPullResult) {
        				// Impossible to pull, is a big problem
        				WasdiLog.errorLog("DockerUtils.start: Error pulling the image, we cannot proceed");
        				return false;
        			}
        		}
        		
        		
        		// Since we are creating the Container, we need to set up our name
        		sContainerName = m_oProcessor.getName() + "_" + m_oProcessor.getVersion() + "_" + Utils.getRandomNumber(1, 5000);
        		WasdiLog.debugLog("DockerUtils.start: ok is image pulled create the container named " + sContainerName);
        		
        		// Create the container
        		
            	try {
            		// API URL
            		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
            		if (!sUrl.endsWith("/")) sUrl += "/";
            		sUrl += "containers/create?name=" + sContainerName;
            		
            		// Create the Payload to send to create the container
            		CreateParams oContainerCreateParams = new CreateParams();
            		
            		// Set the user
            		oContainerCreateParams.User = "appwasdi";
            		
            		// Set the image
            		oContainerCreateParams.Image = sImageName;
            		
            		// Add the volume with workspace data
//            		MountVolumeParam oWorkingPath = new MountVolumeParam();
//            		oWorkingPath.Source = sMountWorkspaceFolder;
//            		oWorkingPath.Target = "/data/wasdi";
//            		oWorkingPath.ReadOnly = false;
//            		oWorkingPath.Type= MountTypes.BIND;
//            		
//            		oContainerCreateParams.HostConfig.Mounts.add(oWorkingPath);
            		
            		oContainerCreateParams.HostConfig.Binds.add(sMountWorkspaceFolder+":"+"/data/wasdi");
            		
            		// Add the volume with the Processor Code
            		MountVolumeParam oProcessorPath = new MountVolumeParam();
            		oProcessorPath.Source = m_sProcessorFolder;
            		oProcessorPath.Target = "/wasdi";
            		oProcessorPath.ReadOnly = false;
            		oProcessorPath.Type= MountTypes.BIND;
            		
            		oContainerCreateParams.HostConfig.Mounts.add(oProcessorPath);
            		
            		oContainerCreateParams.HostConfig.RestartPolicy.put("Name", "no");
            		oContainerCreateParams.HostConfig.RestartPolicy.put("MaximumRetryCount", 0);
            		
            		// Expose the TCP Port
            		oContainerCreateParams.ExposedPorts.add("5000/tcp");
            		oContainerCreateParams.HostConfig.PortBindings.put("5000/tcp", ""+iProcessorPort);
            		
                    // Extra hosts mapping, useful for some instances when the server host can't be resolved
                    // The symptoms of such problem is that the POST call from the Docker container timeouts
                    if (WasdiConfig.Current.dockers.extraHosts != null) {
                    	
                    	if (WasdiConfig.Current.dockers.extraHosts.size()>0) {
                    		WasdiLog.debugLog("DockerUtils.start adding configured extra host mapping to the run arguments");
                        	for (int iExtraHost = 0; iExtraHost<WasdiConfig.Current.dockers.extraHosts.size(); iExtraHost ++) {
                        		String sExtraHost = WasdiConfig.Current.dockers.extraHosts.get(iExtraHost);
                        		oContainerCreateParams.HostConfig.ExtraHosts.add(sExtraHost);
                        	}
                    	}
                    }
            		   
                    // This is no longer supported!!
//                    if (!Utils.isNullOrEmpty(WasdiConfig.Current.dockers.additionalDockerRunParameter)) {
//                    	asArgs.add(WasdiConfig.Current.dockers.additionalDockerRunParameter);
//                    }
            		
            		String sContainerCreateParams = oContainerCreateParams.toJson();
            		
            		if (Utils.isNullOrEmpty(sContainerCreateParams)) {
            			WasdiLog.errorLog("DockerUtils.start: impossible to get the payload to create the container. We cannot proceed :(");
            			return false;
            		}
            		
            		HashMap<String, String> asHeaders = new HashMap<>();
            		asHeaders.put("Content-Type", "application/json");
            		
            		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, sContainerCreateParams, asHeaders);
            		
            		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
            			WasdiLog.errorLog("DockerUtils.start: impossible to create the container. We cannot proceed :(. ERROR = " + oResponse.getResponseBody());
            			return false;
            		}
            		
            		WasdiLog.debugLog("DockerUtils.start: Container created");
            		
            	}
            	catch (Exception oEx) {
            		
            		WasdiLog.errorLog("DockerUtils.start: " + oEx.toString());
                    return false;
                }        		
        	}
        	else {        		
        		WasdiLog.debugLog("DockerUtils.start: Container already found, we will use the id");
        		sContainerName = oContainerInfo.Id;
        	}
            
            WasdiLog.debugLog("DockerUtils.start: Starting Container Named" + sContainerName + " created");
            
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		sUrl+="containers/" + sContainerName + "/start";
    		
    		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, "");
    		
    		if (oResponse.getResponseCode() == 204) {
    			WasdiLog.debugLog("DockerUtils.start: Container " + sContainerName + " started");
    			return true;
    		}
    		else if (oResponse.getResponseCode() == 204) {
    			WasdiLog.debugLog("DockerUtils.start: Container " + sContainerName + " wasd already started");
    			return true;
    		}
    		else {
    			WasdiLog.errorLog("DockerUtils.start: Impossible to start Container " + sContainerName);
    			return false;
    		}
            
        } catch (Exception oEx) {
        	WasdiLog.errorLog("DockerUtils.start error creating the container: " + oEx.toString());
            return false;
        }
    }    

    /**
     * Delete using Processor member variable
     *
     * @return
     */
    public boolean delete() {
        if (m_oProcessor != null) {
            return delete(m_oProcessor.getName(), m_oProcessor.getVersion());
        } else return false;
    }

    /**
     * Delete using processor name and default "1" version
     *
     * @param sProcessorName
     * @return
     */
    public boolean delete(String sProcessorName) {
        return delete(sProcessorName, "1");
    }

    /**
     * Delete using processor name and processor version
     *
     * @param sProcessorName
     * @param sVersion
     * @return
     */
    public boolean delete_old(String sProcessorName, String sVersion) {

        try {

            // docker ps -a | awk '{ print $1,$2 }' | grep <imagename> | awk '{print $1 }' | xargs -I {} docker rm -f {}
            // docker rmi -f <imagename>

            String sBaseDockerName = "wasdi/" + sProcessorName + ":" + sVersion;
            String sDockerName = sBaseDockerName;
            
            if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
            	sDockerName = m_sDockerRegistry + "/" + sDockerName;
            }

            String sDeleteScriptFile = m_sProcessorFolder + "cleanwasdidocker.sh";
            File oDeleteScriptFile = new File(sDeleteScriptFile);

            if (!oDeleteScriptFile.exists() || oDeleteScriptFile.length() == 0L) {
                try (BufferedWriter oDeleteScriptWriter = new BufferedWriter(new FileWriter(oDeleteScriptFile))) {
                    if (oDeleteScriptWriter != null) {
                        WasdiLog.debugLog("DockerUtils.delete: Creating " + sDeleteScriptFile + " file");

                        oDeleteScriptWriter.write("#!/bin/bash");
                        oDeleteScriptWriter.newLine();
                        oDeleteScriptWriter.write("docker ps -a | awk '{ print $1,$2 }' | grep " + sDockerName + " | awk '{print $1 }' | xargs -I {} docker rm -f {}");
                        oDeleteScriptWriter.newLine();
                        oDeleteScriptWriter.flush();
                        oDeleteScriptWriter.close();
                    }
                }

                RunTimeUtils.addRunPermission(sDeleteScriptFile);
                
                
            } else {
                RunTimeUtils.addRunPermission(sDeleteScriptFile);
            }

            Runtime.getRuntime().exec(sDeleteScriptFile);

            // Wait for docker to finish
            Thread.sleep(WasdiConfig.Current.dockers.millisWaitAfterDelete);
            
            WasdiLog.debugLog("DockerUtils.delete: remove image");

            // Delete this image
            ArrayList<String> asArgs = new ArrayList<>();
            // Remove the container image
            asArgs.add("rmi");
            asArgs.add("-f");
            asArgs.add(sDockerName);
            
            String sCommand = "docker";

            RunTimeUtils.shellExec(sCommand, asArgs, false);
            
            // Wait for docker to finish
            Thread.sleep(WasdiConfig.Current.dockers.millisWaitAfterDelete);
            
            
            String sVersionCopy = sVersion;
            int iVersion = StringUtils.getAsInteger(sVersionCopy);
            
        	while (iVersion>1)  {
        		sVersionCopy = StringUtils.decrementIntegerString(sVersionCopy);
        		
        		WasdiLog.debugLog("DockerUtils.delete: remove also image version " + sVersionCopy);
        		
                // Delete this image
                asArgs = new ArrayList<>();
                // Remove the container image
                asArgs.add("rmi");
                asArgs.add("-f");
                asArgs.add(sDockerName.replace(":"+sVersion, ":"+sVersionCopy));
        		
        		RunTimeUtils.shellExec(sCommand, asArgs, false);
        		
                // Wait for docker to finish
                Thread.sleep(WasdiConfig.Current.dockers.millisWaitAfterDelete);    		
                
                iVersion = StringUtils.getAsInteger(sVersionCopy);
        	}
       
            
            if (WasdiConfig.Current.isMainNode()) {            	
                if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
                	WasdiLog.infoLog("This is a registry stored docker: clean all our registers");
                	for (DockerRegistryConfig oRegistryConfig : WasdiConfig.Current.dockers.registers) {
                		this.removeImageFromRegistry(sBaseDockerName, sVersion, oRegistryConfig);					
    				}
                }
            }
            
            
        } catch (Exception oEx) {
        	WasdiLog.errorLog("DockerUtils.delete: " + oEx.toString());
            return false;
        }

        return true;
    }
    
    /**
     * Delete using processor name and processor version
     * It deletes also all the previous versions
     * 
     * @param sProcessorName Name of the processor to delete
     * @param sVersion Version
     * @return
     */
    public boolean delete(String sProcessorName, String sVersion) {

        try {            
            
            int iVersion = StringUtils.getAsInteger(sVersion);
            
        	while (iVersion>0)  {
        		
        		WasdiLog.debugLog("DockerUtils.delete2: Removing " + sProcessorName + " version "  + sVersion);
        		
                String sBaseDockerName = "wasdi/" + sProcessorName + ":" + sVersion;
                String sDockerName = sBaseDockerName;
                
                if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
                	sDockerName = m_sDockerRegistry + "/" + sDockerName;
                }
                
                String sId = getContainerIdFromWasdiAppName(sProcessorName, sVersion);
                boolean bContainersRemoved = removeContainer(sId, true);
                
                if (!bContainersRemoved) {
                	WasdiLog.errorLog("DockerUtils.delete2: Impossible to remove the container (maybe it was not present!!) for " + sProcessorName + " Version: " +  sVersion  + " Found Id: " + sId);
                }
                
                WasdiLog.debugLog("DockerUtils.delete2: Removing image for " + sProcessorName + " version "  + sVersion);
                
                boolean bImageRemoved = removeImage(sDockerName, true);
                
                if (!bImageRemoved) {
                	WasdiLog.errorLog("DockerUtils.delete2: error removing the image for " + sProcessorName + " Version: " +  sVersion  + " Found Id: " + sId);
                }
                
                if (WasdiConfig.Current.isMainNode()) {            	
                    if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
                    	WasdiLog.infoLog("DockerUtils.delete2: This is a registry stored docker: clean all our registers");
                    	for (DockerRegistryConfig oRegistryConfig : WasdiConfig.Current.dockers.registers) {
                    		this.removeImageFromRegistry(sBaseDockerName, sVersion, oRegistryConfig);					
        				}
                    }
                }                
        		
                sVersion = StringUtils.decrementIntegerString(sVersion);
                iVersion = StringUtils.getAsInteger(sVersion);
        	}
            
            
        } catch (Exception oEx) {
        	WasdiLog.errorLog("DockerUtils.delete: " + oEx.toString());
            return false;
        }

        return true;
    }
    
    public String loginInRegistry(DockerRegistryConfig oDockerRegistryConfig) {
    	return loginInRegistry(oDockerRegistryConfig.address, oDockerRegistryConfig.user, oDockerRegistryConfig.password);
    }

    /**
     * Log in docker on a specific Repository Server
     * @param sServer Server Address
     * @param sUser Server User
     * @param sPassword Server Password
     * @param sFolder 
     * @return True if logged false otherwise
     */
    public String loginInRegistry(String sServer, String sUser, String sPassword) {
    	
    	try {
    		// Get the API Address
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		
    		// Auth end-point
    		sUrl += "auth";
    		
    		// Prepare the login info
    		LoginInfo oLoginInfo = new LoginInfo();
    		oLoginInfo.username = sUser;
    		oLoginInfo.password = sPassword;
    		oLoginInfo.serveraddress = sServer;
    		
    		if (!oLoginInfo.serveraddress.startsWith("https://")) oLoginInfo.serveraddress = "https://" + oLoginInfo.serveraddress; 
    		
    		// Convert in string
    		String sLoginInfo = JsonUtils.stringify(oLoginInfo);
    		
    		// Make the post
    		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, sLoginInfo);
    		
    		if (oResponse.getResponseCode() == 200) {
    			// Here we should had our Token
    			String sAuthToken = oResponse.getResponseBody();
    			AuthToken oToken = JsonUtils.s_oMapper.readValue(sAuthToken,AuthToken.class);
    			
    			if (Utils.isNullOrEmpty(oToken.IdentityToken)) {
    				String sEncodedAuth = Base64.getEncoder().encodeToString(sLoginInfo.getBytes(StandardCharsets.UTF_8));
    				return sEncodedAuth;
    			}
    			else {
    				
    				String sTokenJson = "\"identitytoken\":\""+oToken.IdentityToken+"\"}";
    				String sEncodedAuth = Base64.getEncoder().encodeToString(sTokenJson.getBytes(StandardCharsets.UTF_8));
    				return sEncodedAuth;
    			}
    		}
    		else {
    			// There is some problem
    			WasdiLog.errorLog("DockerUtils.loginInRegistry: auth api for register " + sServer + " returned " + oResponse.getResponseCode() + " and we are without token!");
    			return "";
    		}
    	} catch (Exception oEx) {
    		WasdiLog.errorLog("DockerWasdiLog.loginInRegistry: " + oEx.toString());
            return "";
        }
    }
    
    /**
     * Push a docker image to a registry. Must be logged 
     * @param sImage Image name
     * @return True if pushed false if error 
     */
    public boolean push(String sImage, String sToken) {	
    	try {
    		
    		if (sImage.contains(":")) {
    			WasdiLog.debugLog("DockerUtils.push image contains a tag: remove it");
    			sImage = sImage.split(":")[0];
    		}
    		
            // Create the docker command
            ArrayList<String> asArgs = new ArrayList<>();
            // Push
            asArgs.add("push");
            // Option
            asArgs.add("--all-tags");
            
            String sServerImage = sImage;
            
            asArgs.add(sServerImage);
            
            String sCommand = "docker";
			
            RunTimeUtils.shellExec(sCommand, asArgs, true);    		
    		
    	} catch (Exception oEx) {
    		WasdiLog.errorLog("DockerWasdiLog.login: " + oEx.toString());
            return false;
        }
    	
    	return true;
    }
    
    /**
     * Pull a docker image from a registry. Must be logged 
     * @param sImage Image name
     * @return True if pushed false if error 
     */
    public boolean pull(String sImage, String sToken) {	
    	try {
    		
    		// Get the docker address
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		
    		// Url Encode
    		String sEncodedServerImage = URLEncoder.encode(sImage);
    		
    		// Add the query parameter
    		sUrl+="images/create?fromImage="+sEncodedServerImage;
    		
    		// We need a couple of headers
    		HashMap<String, String> asHeaders = new HashMap<>();
    		
    		// The content is taken from the sample on line
    		asHeaders.put("Content-Type", "application/tar");
    		asHeaders.put("X-Registry-Auth", sToken);    		
    		
    		// Is a post!
    		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, "", asHeaders);
    		
    		if (oResponse.getResponseCode() == 200) {
    			return true;
    		}
    		else {
    			WasdiLog.errorLog("Error pulling image " + sImage + " got answer http " + oResponse.getResponseCode());
    			return false;
    		}
    	} 
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.pull: " + oEx.toString());
            return false;
        }
    }
        
    /**
     * Gets the container id starting from processor name and version
     * @param sProcessorName
     * @param sVersion
     * @return
     */
    protected String getContainerIdFromWasdiAppName(String sProcessorName, String sVersion) {
    	try {
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		
    		sUrl += "containers/json";
    		
    		HttpCallResponse oResponse = HttpUtils.httpGet(sUrl);
    		
    		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
    			return "";
    		}
    		
    		List<Object> aoOutputJsonMap = null;

            try {
                aoOutputJsonMap = JsonUtils.s_oMapper.readValue(oResponse.getResponseBody(), new TypeReference<List<Object>>(){});
            } catch (Exception oEx) {
                WasdiLog.errorLog("DockerUtils.getContainerIdFromWasdiAppName: exception converting API result " + oEx);
                return "";
            }
            
            String sMyImage = "wasdi/" + sProcessorName + ":" + sVersion;
            String sId = "";
            
            for (Object oContainer : aoOutputJsonMap) {
				try {
					LinkedHashMap<String, String> oContainerMap = (LinkedHashMap<String, String>) oContainer;
					String sImageName = oContainerMap.get("Image");
					
					if (sImageName.endsWith(sMyImage)) {
						WasdiLog.debugLog("DockerUtils.DockerUtils.getContainerIdFromWasdiAppName: found my container " + sMyImage + " Docker Image = " +sImageName);
						sId = oContainerMap.get("Id");
						return sId;
					}
					
				}
		    	catch (Exception oEx) {
		    		WasdiLog.errorLog("DockerUtils.DockerUtils.getContainerIdFromWasdiAppName: error parsing a container json entity " + oEx.toString());
		        }
			}    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.getContainerIdFromWasdiAppName: " + oEx.toString());
		}
    	return "";
    }
    
    /**
     * Stop a running container
     * @param oProcessor Processor associated to the container
     * @return true if stopped
     */
    public boolean stop(Processor oProcessor) {
    	if (oProcessor == null) {
    		WasdiLog.errorLog("DockerUtils.stop: processor is null");
    		return false;
    	}
    	
    	return stop(oProcessor.getName(), oProcessor.getVersion());    	
    }

    
    /**
     * Stop a running container
     * @param sProcessorName Processor Name
     * @param sVersion Version
     * @return true if stopped
     */
    public boolean stop(String sProcessorName, String sVersion) {
        String sId = getContainerIdFromWasdiAppName(sProcessorName, sVersion);
        return stop(sId);            
    }
    
    /**
     * Stop a running container
     * @param sProcessorName Processor Name
     * @param sVersion Version
     * @return true if stopped
     */
    public boolean stop(String sId) {
       	try {
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
                        
            if (!Utils.isNullOrEmpty(sId)) {
        		if (!sUrl.endsWith("/")) sUrl += "/";
        		sUrl += "containers/" + sId + "/stop";
        		
        		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, "");
        		
        		if (oResponse.getResponseCode()==204||oResponse.getResponseCode()==304) {
        			return true;
        		}
        		
        		// In theory here we should just return false. But still if the answer was not
        		// 204 (stopped) or 304 (already stopped) we handle the un-expected case of 
        		// 2xx returned codes.
        		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
        			return false;
        		}
        		else {
        			return true;
        		}
            }
    		
    		return false;
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.stop: " + oEx.toString());
            return false;
        }
    }    
    
    /**
     * Remove a container
     * @param sId Container Id
     * @return true if was removed
     */
    public boolean removeContainer(String sId) {
    	return removeContainer(sId, false);
    }
    
    /**
     * Remove a container
     * @param sId Container Id
     * @param bForce True to force to kill running containers 
     * @return true if was removed
     */
    public boolean removeContainer(String sId, boolean bForce) {
       	try {
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
                        
            if (!Utils.isNullOrEmpty(sId)) {
        		if (!sUrl.endsWith("/")) sUrl += "/";
        		sUrl += "containers/" + sId;
        		
        		if (bForce) {
        			sUrl+="?force=true";
        		}
        		
        		HttpCallResponse oResponse = HttpUtils.httpDelete(sUrl);
        		
        		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
        			return false;
        		}
        		else {
        			return true;
        		}
            }
    		
    		return false;
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.removeContainer: " + oEx.toString());
            return false;
        }    	
    }
    
    /**
     * Removes a Docker Image
     * @param sImageName Image Name
     * @return True if deleted
     */
    boolean removeImage(String sImageName) {
    	return removeImage(sImageName, true);
    	
    }
    
    /**
     * Removes a Docker Image
     * @param sImageName Image Name
     * @param bForce True to force to delete the image even if there are stopped containers
     * @return True if deleted
     */
    boolean removeImage(String sImageName, boolean bForce) {
       	try {
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
                        
            if (!Utils.isNullOrEmpty(sImageName)) {
        		if (!sUrl.endsWith("/")) sUrl += "/";
        		sUrl += "images/" + sImageName;
        		
        		if (bForce) {
        			sUrl+="?force=true";
        		}
        		
        		HttpCallResponse oResponse = HttpUtils.httpDelete(sUrl);
        		
        		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
        			return false;
        		}
        		else {
        			return true;
        		}
            }
    		
    		return false;
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.removeImage: " + oEx.toString());
            return false;
        }    	    	
    }
    
    /**
     * Check if a container is started
     * @param oProcessor Processor Entity
     * @return true if it is running
     */
    public boolean isContainerStarted(Processor oProcessor) {
    	if (oProcessor == null) {
    		WasdiLog.errorLog("DockerUtils.isContainerStarted: processor is null");
    		return false;
    	}
    	
    	return isContainerStarted(oProcessor.getName(), oProcessor.getVersion());
    }
    
    /**
     * Check if a container is started
     * @param sProcessorName
     * @param sVersion
     * @return
     */
    public boolean isContainerStarted(String sProcessorName, String sVersion) {
    	
    	try {
    		
    		WasdiLog.debugLog("DockerUtils.isContainerStarted: search the container");
    		ContainerInfo oContainer = getContainerInfo(sProcessorName, sVersion);
    		
    		if (oContainer == null) {
    			WasdiLog.debugLog("DockerUtils.isContainerStarted: container not found, so for sure not started");
    			return false;
    		}
    		
    		if (oContainer.State.equals(ContainerStates.RUNNING)) return true;
    		else return false;    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.isContainerStarted: " + oEx.toString());
            return false;
        }
    }
    
    /**
     * Check if a container is started
     * @param sProcessorName
     * @param sVersion
     * @return
     */
    public ContainerInfo getContainerInfo(String sProcessorName, String sVersion) {
    	
    	try {
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		sUrl += "containers/json";
    		
    		HttpCallResponse oResponse = HttpUtils.httpGet(sUrl);
    		
    		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
    			return null;
    		}
    		
    		List<Object> aoOutputJsonMap = null;

            try {
                ObjectMapper oMapper = new ObjectMapper();
                aoOutputJsonMap = oMapper.readValue(oResponse.getResponseBody(), new TypeReference<List<Object>>(){});
            } catch (Exception oEx) {
                WasdiLog.errorLog("DockerUtils.getContainerInfo: exception converting API result " + oEx);
                return null;
            }
            
            String sMyImage = "wasdi/" + sProcessorName + ":" + sVersion;
            
            if (!Utils.isNullOrEmpty(m_sDockerRegistry))   {
            	sMyImage = m_sDockerRegistry + "/" + sMyImage;
            }
            
            for (Object oContainer : aoOutputJsonMap) {
				try {
					LinkedHashMap<String, Object> oContainerMap = (LinkedHashMap<String, Object>) oContainer;
					String sImageName = (String) oContainerMap.get("Image");
					
					if (sImageName.endsWith(sMyImage)) {
						WasdiLog.debugLog("DockerUtils.getContainerInfo: found my container " + sMyImage + " Docker Image = " +sImageName);
						
						ContainerInfo oContainerInfo = new ContainerInfo();
						oContainerInfo.Id = (String) oContainerMap.get("Id");
						oContainerInfo.Image = (String) oContainerMap.get("Image");
						oContainerInfo.ImageId = (String) oContainerMap.get("ImageId");
						oContainerInfo.State = (String) oContainerMap.get("State");
						oContainerInfo.Status = (String) oContainerMap.get("Status");
						oContainerInfo.Names = (List<String>) oContainerMap.get("Names");
						
						return oContainerInfo;
					}
					
				}
		    	catch (Exception oEx) {
		    		WasdiLog.errorLog("DockerUtils.getContainerInfo: error parsing a container json entity " + oEx.toString());
		        }
			}
    		
    		return null;
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.getContainerInfo: " + oEx.toString());
            return null;
        }
    }
    
    
    /**
     * Check if an image is available on the local registry
     * @param oProcessor Processor entity
     * @return true if the image is available locally
     */
    public boolean isImageAvailable(Processor oProcessor) {
    	if (oProcessor == null) {
    		WasdiLog.errorLog("DockerUtils.isImageAvailable: processor is null");
    		return false;
    	}
    	
    	return isImageAvailable(oProcessor.getName(), oProcessor.getVersion());
    }
    
    /**
     * Check if an image is available on the local registry
     * @param sProcessorName Processor Name
     * @param sVersion Processor Version
     * @return true if the image is available locally
     */
    @SuppressWarnings("unchecked")
	public boolean isImageAvailable(String sProcessorName, String sVersion) {
    	
    	try {
    		// Get the internal API address
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		
    		// End point to get the list of images
    		sUrl += "images/json";
    		
    		// Get the list
    		HttpCallResponse oResponse = HttpUtils.httpGet(sUrl);
    		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
    			return false;
    		}
    		
    		// Here we put the results
    		List<Object> aoOutputJsonMap = null;

            try {
                aoOutputJsonMap = JsonUtils.s_oMapper.readValue(oResponse.getResponseBody(), new TypeReference<List<Object>>(){});
            } catch (Exception oEx) {
                WasdiLog.errorLog("DockerUtils.isImageAvailable: exception converting API result " + oEx);
                return false;
            }
            
            // Define the name of our image
            String sMyImage = "";
            
            if (!Utils.isNullOrEmpty(m_sDockerRegistry)) sMyImage = m_sDockerRegistry + "/";
            sMyImage += "wasdi/" + sProcessorName + ":" + sVersion;
            
            // Search all the available images
            for (Object oImage : aoOutputJsonMap) {
				try {
					
					LinkedHashMap<String, String> oImageMap = (LinkedHashMap<String, String>) oImage;
					
					// Search a Repo Tag that equals to our name
					Object oRepoTags = oImageMap.get("RepoTags");
					ArrayList<String> asRepoTags = (ArrayList<String>) oRepoTags;
					
					for (String sTag : asRepoTags) {
						if (sTag.equals(sMyImage)) {
							WasdiLog.debugLog("DockerUtils.isImageAvailable: found my image");
							return true;							
						}
					}
				}
		    	catch (Exception oEx) {
		    		WasdiLog.errorLog("DockerUtils.isImageAvailable: error parsing a container json entity " + oEx.toString());
		        }
			}
    		
            // No we did not found the image
    		return false;
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.isImageAvailable: " + oEx.toString());
            return false;
        }
    }    
    
    /**
     * Remove an image from a Registry
     * @param sImageName
     * @param sVersion
     * @param oRegistry
     */
    public void removeImageFromRegistry(String sImageName, String sVersion, DockerRegistryConfig oRegistry) {
    	try {
    		
    		if (sImageName.contains(":")) {
    			sImageName = sImageName.split(":")[0];
    			WasdiLog.debugLog("DockerUtils.removeImageFromRegistry: Image Name contains version, cleaned to " + sImageName);
    		}
    		
    		// Get the layer manifest
    		Map<String, String> asHeaders = HttpUtils.getBasicAuthorizationHeaders(oRegistry.user, oRegistry.password);
    		
    		String sUrl = oRegistry.apiAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		
    		sUrl += "repository/docker-wasdi-processor/v2/";
    		sUrl += sImageName;
    		sUrl += "/manifests/" + sVersion;
    		
    		asHeaders.put("Accept", "application/vnd.docker.distribution.manifest.v2+json");
    		
    		WasdiLog.debugLog("DockerUtils.removeImageFromRegistry: Get Manifest with registry url " + sUrl);
    		
    		Map<String, List<String>> aoOuputHeaders = new HashMap<>(); 
    		
    		HttpUtils.httpGet(sUrl, asHeaders, aoOuputHeaders); 
    		
    		String sDigest = "";
    		
    		if (aoOuputHeaders != null) {
    			if (aoOuputHeaders.containsKey("Docker-Content-Digest")) {
    				if (aoOuputHeaders.get("Docker-Content-Digest").size()>0) {
    					sDigest = aoOuputHeaders.get("Docker-Content-Digest").get(0);
    					WasdiLog.debugLog("DockerUtils.removeImageFromRegistry: found digest " + sDigest);
    				}
    			}
    		}
    		
    		if (!Utils.isNullOrEmpty(sDigest)) {
        		sUrl = oRegistry.apiAddress;
        		
        		if (!sUrl.endsWith("/")) sUrl += "/";
        		
        		sUrl += "repository/docker-wasdi-processor/v2/";
        		sUrl += sImageName;
        		sUrl += "/manifests/" + sDigest;
        		
        		WasdiLog.debugLog("DockerUtils.removeImageFromRegistry: Delete Layer with Digest " + sUrl);
        		
        		HttpCallResponse oResponse = HttpUtils.httpDelete(sUrl, asHeaders);
        		
        		if (oResponse.getResponseCode() == 202) {
        			WasdiLog.debugLog("DockerUtils.removeImageFromRegistry: Image Deleted ");
        		}
        		else {
        			WasdiLog.debugLog("DockerUtils.removeImageFromRegistry: Problem deleting image: output code " + oResponse.getResponseCode().toString() + " Message: " + oResponse.getResponseBody());
        		}
    		}
    		else {
    			WasdiLog.debugLog("DockerUtils.removeImageFromRegistry: Delete Layer digest is null, nothing to do ");
    		}
    		
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.removeImageFromRegistry: error " + oEx.toString());
		}
    }

}
