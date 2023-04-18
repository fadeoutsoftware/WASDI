package wasdi.processors.dockerUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.business.Processor;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.HttpUtils;
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
    Processor m_oProcessor;

    /**
     * Folder of the processor
     */
    String m_sProcessorFolder;

    /**
     * Wasdi Working Path
     */
    String m_sWorkingRootPath;

    /**
     * Log file for docker operations
     */
    String m_sDockerLogFile = WasdiConfig.Current.dockers.dockersDeployLogFilePath;

    /**
     * User that run the docker
     */
    String m_sUser = "tomcat";    

    /**
     * Docker registry in use
     */
    String m_sDockerRegistry = "";
    
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
        m_sWorkingRootPath = WasdiConfig.Current.paths.downloadRootPath;
        if (!m_sWorkingRootPath.endsWith(File.separator)) m_sWorkingRootPath += File.separator; 
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
	 * Get the actual WASDI Working Path
	 * @return actual WASDI Working Path
	 */
	public String getWorkingRootPath() {
		return m_sWorkingRootPath;
	}
	
	/**
	 * Set the actual WASDI Working Path
	 * @param sWorkingRootPath actual WASDI Working Path
	 */
	public void setWorkingRootPath(String sWorkingRootPath) {
		this.m_sWorkingRootPath = sWorkingRootPath;
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
        return start(m_oProcessor.getPort());
    }

    /**
     * Run the docker at the specified port
     *
     * @param iProcessorPort Port to use
     */
    public boolean start(int iProcessorPort) {

        try {
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

            if (!oRunFile.exists() || oRunFile.length() == 0L) {

                // Command
                asArgs.add(sCommand);

                // Action
                asArgs.add("run");

                // User
                asArgs.add("-u$(id -u " + m_sUser + "):$(id -g " + m_sUser + ")");

                // Working Path
                asArgs.add("-v" + m_sWorkingRootPath + ":/data/wasdi");

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
            
            // Add the pull command
            if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
            	WasdiLog.debugLog("DockerUtils.start: log in in the registers");
            	
            	List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
            	
				// For each register: ordered by priority
				for (int iRegisters=0; iRegisters<aoRegisters.size(); iRegisters++) {
					
					DockerRegistryConfig oDockerRegistryConfig = aoRegisters.get(iRegisters);
					
					WasdiLog.debugLog("DockerUtils.start: logging in to " + oDockerRegistryConfig.id);
					
					// Try to login and push
					boolean bLogged = loginInRegistry(oDockerRegistryConfig.address, oDockerRegistryConfig.user, oDockerRegistryConfig.password, m_sProcessorFolder);
					
					if (!bLogged) {
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
    public boolean delete(String sProcessorName, String sVersion) {

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
            
            if (WasdiConfig.Current.nodeCode.equals("wasdi")) {            	
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
     * Log in docker on a specific Repository Server
     * @param sServer Server Address
     * @param sUser Server User
     * @param sPassword Server Password
     * @param sFolder 
     * @return True if logged false otherwise
     */
    public boolean loginInRegistry(String sServer, String sUser, String sPassword, String sFolder) {
    	try {
            // Create the docker command            
            String sCommand = "docker login --username " + sUser + " --password '" + sPassword + "' " + sServer;
            
            RunTimeUtils.runCommand(sFolder, sCommand, true, true);
            
    	} catch (Exception oEx) {
    		WasdiLog.errorLog("DockerWasdiLog.loginInRegistry: " + oEx.toString());
            return false;
        }
    	
    	return true;
    }
    
    /**
     * Push a docker image to a registry. Must be logged 
     * @param sImage Image name
     * @return True if pushed false if error 
     */
    public boolean push(String sImage) {	
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
    public boolean pull(String sImage, String sRegistry) {	
    	try {
    		
            // Create the docker command
            ArrayList<String> asArgs = new ArrayList<>();
            // Push
            asArgs.add("pull");
            
            String sServerImage = sRegistry + "/" + sImage;
            
            asArgs.add(sServerImage);
            
            String sCommand = "docker";
			
            RunTimeUtils.shellExec(sCommand, asArgs, true);    		
    		
    	} 
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.login: " + oEx.toString());
            return false;
        }
    	
    	return true;
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
       	try {
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		
    		sUrl += "containers/json";
    		
    		HttpCallResponse oResponse = HttpUtils.httpGet(sUrl);
    		
    		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
    			return false;
    		}
    		
    		List<Object> aoOutputJsonMap = null;

            try {
                ObjectMapper oMapper = new ObjectMapper();
                aoOutputJsonMap = oMapper.readValue(oResponse.getResponseBody(), new TypeReference<List<Object>>(){});
            } catch (Exception oEx) {
                WasdiLog.errorLog("DockerUtils.stop: exception converting API result " + oEx);
                return false;
            }
            
            String sMyImage = "wasdi/" + sProcessorName + ":" + sVersion;
            String sId = "";
            
            for (Object oContainer : aoOutputJsonMap) {
				try {
					LinkedHashMap<String, String> oContainerMap = (LinkedHashMap<String, String>) oContainer;
					String sImageName = oContainerMap.get("Image");
					
					if (sImageName.endsWith(sMyImage)) {
						WasdiLog.debugLog("DockerUtils.stop: found my container " + sMyImage + " Docker Image = " +sImageName);
						sId = oContainerMap.get("Id");
						break;
					}
					
				}
		    	catch (Exception oEx) {
		    		WasdiLog.errorLog("DockerUtils.stop: error parsing a container json entity " + oEx.toString());
		        }
			}
            
            if (!Utils.isNullOrEmpty(sId)) {
            	sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
        		if (!sUrl.endsWith("/")) sUrl += "/";
        		sUrl += "containers/" + sId + "/stop";
        		
        		oResponse = HttpUtils.httpPost(sUrl, "");
        		
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
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		
    		sUrl += "containers/json";
    		
    		HttpCallResponse oResponse = HttpUtils.httpGet(sUrl);
    		
    		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
    			return false;
    		}
    		
    		List<Object> aoOutputJsonMap = null;

            try {
                ObjectMapper oMapper = new ObjectMapper();
                aoOutputJsonMap = oMapper.readValue(oResponse.getResponseBody(), new TypeReference<List<Object>>(){});
            } catch (Exception oEx) {
                WasdiLog.errorLog("DockerUtils.isContainerStarted: exception converting API result " + oEx);
                return false;
            }
            
            String sMyImage = "wasdi/" + sProcessorName + ":" + sVersion;
            
            for (Object oContainer : aoOutputJsonMap) {
				try {
					LinkedHashMap<String, String> oContainerMap = (LinkedHashMap<String, String>) oContainer;
					String sImageName = oContainerMap.get("Image");
					
					if (sImageName.endsWith(sMyImage)) {
						WasdiLog.debugLog("DockerUtils.isContainerStarted: found my container " + sMyImage + " Docker Image = " +sImageName);
						return true;
					}
					
				}
		    	catch (Exception oEx) {
		    		WasdiLog.errorLog("DockerUtils.isContainerStarted: error parsing a container json entity " + oEx.toString());
		        }
			}
    		
    		return false;
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.isContainerStarted: " + oEx.toString());
            return false;
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
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		
    		sUrl += "images/json";
    		
    		HttpCallResponse oResponse = HttpUtils.httpGet(sUrl);
    		
    		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
    			return false;
    		}
    		
    		List<Object> aoOutputJsonMap = null;

            try {
                ObjectMapper oMapper = new ObjectMapper();
                aoOutputJsonMap = oMapper.readValue(oResponse.getResponseBody(), new TypeReference<List<Object>>(){});
            } catch (Exception oEx) {
                WasdiLog.errorLog("DockerUtils.isImageAvailable: exception converting API result " + oEx);
                return false;
            }
            
            String sMyImage = "";
            
            if (!Utils.isNullOrEmpty(m_sDockerRegistry)) sMyImage = m_sDockerRegistry + "/";
            sMyImage += "wasdi/" + sProcessorName + ":" + sVersion;
            
            for (Object oImage : aoOutputJsonMap) {
				try {
					LinkedHashMap<String, String> oImageMap = (LinkedHashMap<String, String>) oImage;
					
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
    		
    		HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders, aoOuputHeaders); 
    		String sManifest = oHttpCallResponse.getResponseBody();
    		//Manifest oManifest = MongoRepository.s_oMapper.readValue(sManifest, Manifest.class);
    		
    		//WasdiLog.debugLog("DockerUtils.removeImageFromRegistry: got manifest " + sManifest);
    		
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
        		
        		HttpUtils.httpDelete(sUrl, asHeaders);    			
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
