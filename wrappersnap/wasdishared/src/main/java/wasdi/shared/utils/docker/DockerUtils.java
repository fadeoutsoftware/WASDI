package wasdi.shared.utils.docker;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.business.S3Volume;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.EnvironmentVariableConfig;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.ProcessorTypeConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.ProcessWorkspaceLogger;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.TarUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.docker.containersViewModels.AuthToken;
import wasdi.shared.utils.docker.containersViewModels.ContainerInfo;
import wasdi.shared.utils.docker.containersViewModels.CreateContainerResponse;
import wasdi.shared.utils.docker.containersViewModels.CreateParams;
import wasdi.shared.utils.docker.containersViewModels.LoginInfo;
import wasdi.shared.utils.docker.containersViewModels.MountVolumeParam;
import wasdi.shared.utils.docker.containersViewModels.constants.ContainerStates;
import wasdi.shared.utils.docker.containersViewModels.constants.MountTypes;
import wasdi.shared.utils.log.WasdiLog;
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
     * Processor Parameter
     */
    protected ProcessorParameter m_oProcessorParameter;

    /**
     * Folder of the processor
     */
    protected String m_sProcessorFolder;

    /**
     * User that run the docker
     */
    protected String m_sWasdiSystemUserName = "appwasdi";
    
    /**
     * User that run the docker
     */
    protected String m_sWasdiSystemGroupName = "appwasdi";
    
    /**
     * User that run the docker
     */
    protected int m_iWasdiSystemUserId = 2042;
    
    /**
     * User that run the docker
     */
    protected int m_iWasdiSystemGroupId = 2042;

    /**
     * Network mode
     */
    protected String m_sDockerNetworkMode = "net-wasdi";
    
    /**
     * Docker registry in use
     */
    protected String m_sDockerRegistry = "";
    
    /**
     * Process Workspace Logger, if available
     */
    protected ProcessWorkspaceLogger m_oProcessWorkspaceLogger = null;
    
    /**
     * Basic Constructor. Set all the members to default value.
     */
    public DockerUtils() {
    	m_oProcessor = null;
    	m_sProcessorFolder = "";
    	m_sDockerRegistry = "";
    	m_sWasdiSystemUserName = WasdiConfig.Current.systemUserName;
    	m_sWasdiSystemGroupName = WasdiConfig.Current.systemGroupName;
    	m_iWasdiSystemUserId = WasdiConfig.Current.systemUserId;
    	m_iWasdiSystemGroupId = WasdiConfig.Current.systemGroupId;
    	if (WasdiConfig.Current != null) {
    		m_sDockerNetworkMode = WasdiConfig.Current.dockers.dockerNetworkMode;
    	}
    }
    
    
    /**
     * Create a new instance
     * 
     * @param oProcessor       Processor
     * @param sProcessorFolder Processor Folder
     */
    public DockerUtils(Processor oProcessor, String sProcessorFolder) {
    	// Initialize the members
    	this();
    	// and override with the ones provided
        m_oProcessor = oProcessor;
        m_sProcessorFolder = sProcessorFolder; 
    }    
    
    
    public DockerUtils(Processor oProcessor, ProcessorParameter oProcessorParameter, String sProcessorFolder) {
    	// Initialize the members
    	this();
    	// and override with the ones provided
        m_oProcessor = oProcessor;
        m_oProcessorParameter = oProcessorParameter;
        m_sProcessorFolder = sProcessorFolder; 
    }    
    
    /**
     * Create a new instance
     * 
     * @param oProcessor       Processor
     * @param sProcessorFolder Processor Folder
     * @param sDockerRegistry  Docker Registry to use. By default is "", means local registry
     */
    public DockerUtils(Processor oProcessor, String sProcessorFolder, String sDockerRegistry) {
    	// Initialize the members
    	this();
    	// and override with the ones provided
        m_oProcessor = oProcessor;
        m_sProcessorFolder = sProcessorFolder;
        m_sDockerRegistry = sDockerRegistry;
    }    
    
    public DockerUtils(Processor oProcessor, ProcessorParameter oProcessorParameter, String sProcessorFolder, String sDockerRegistry) {
    	// Initialize the members
    	this();
    	// and override with the ones provided
        m_oProcessor = oProcessor;
        m_oProcessorParameter = oProcessorParameter;
        m_sProcessorFolder = sProcessorFolder;
        m_sDockerRegistry = sDockerRegistry;	
    }
    
    public DockerUtils(Processor oProcessor, ProcessorParameter oProcessorParameter, String sProcessorFolder, String sDockerRegistry, ProcessWorkspaceLogger oProcessWorkspaceLogger) {
    	// Initialize the members
    	this();
    	// and override with the ones provided
        m_oProcessor = oProcessor;
        m_oProcessorParameter = oProcessorParameter;
        m_sProcessorFolder = sProcessorFolder;
        m_sDockerRegistry = sDockerRegistry;	
    	m_oProcessWorkspaceLogger = oProcessWorkspaceLogger;
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
	 * Get the name of user to pass to docker. Empty string to avoid.
	 * @return
	 */
	public String getWasdiSystemUserName() {
		return m_sWasdiSystemUserName;
	}
	
	/**
	 * Set the name of user to pass to docker. Empty string to avoid.
	 * @param sWasdiSystemUserName
	 */
	public void setWasdiSystemUserName(String sWasdiSystemUserName) {
		this.m_sWasdiSystemUserName = sWasdiSystemUserName;
	}    

	/**
	 * get the wasdi system group name
	 * @return
	 */
	public String getWasdiSystemGroupName() {
		return m_sWasdiSystemGroupName;
	}

	/**
	 * set the wasdi system group name
	 * @param sWasdiSystemGroupName
	 */
	public void setWasdiSystemGroupName(String sWasdiSystemGroupName) {
		this.m_sWasdiSystemGroupName = sWasdiSystemGroupName;
	}

	/**
	 * Get the wasdi system user id
	 * @return
	 */
	public int getWasdiSystemUserId() {
		return m_iWasdiSystemUserId;
	}
	
	/**
	 * Set the wasdi system user id
	 * @param iWasdiSystemUserId
	 */
	public void setWasdiSystemUserId(int iWasdiSystemUserId) {
		this.m_iWasdiSystemUserId = iWasdiSystemUserId;
	}

	/**
	 * Get the wasdi system group id
	 * @return
	 */
	public int getWasdiSystemGroupId() {
		return m_iWasdiSystemGroupId;
	}

	/**
	 * Get the wasdi system group id
	 * @param iWasdiSystemGroupId
	 */
	public void setWasdiSystemGroupId(int iWasdiSystemGroupId) {
		this.m_iWasdiSystemGroupId = iWasdiSystemGroupId;
	}    
	
	/**
	 * Get the network mode
	 * @return
	 */
	public String getDockerNetworkMode() {
		return m_sDockerNetworkMode;
	}

	/**
	 * Set the network node
	 * @param sDockerNetworkMode
	 */
	public void setDockerNetworkMode(String sDockerNetworkMode) {
		this.m_sDockerNetworkMode = sDockerNetworkMode;
	} 
	
	/**
	 * Address of the Docker Register in use. 
	 * @return id of the registry
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
	 * Get the process workspace logger to send messages to wasdi client log view
	 * @return
	 */
	public ProcessWorkspaceLogger getProcessWorkspaceLogger() {
		return m_oProcessWorkspaceLogger;
	}

	/**
	 * Set the process workspace logger to send messages to wasdi client log view
	 * @param oProcessWorkspaceLogger
	 */
	public void setProcessWorkspaceLogger(ProcessWorkspaceLogger oProcessWorkspaceLogger) {
		this.m_oProcessWorkspaceLogger = oProcessWorkspaceLogger;
	}
	
	/**
	 * Safe log on the process workspace
	 * @param sLog
	 */
	protected void processWorkspaceLog(String sLog) {
		if (m_oProcessWorkspaceLogger!=null) {
			m_oProcessWorkspaceLogger.log(sLog);
		}
	}

    /**
     * Deploy a docker
     * @return the docker image tag if ok, empty string in case of problems
     */
    public String build() {
    	
    	String sImageName = "";

        try {
        	// Docker API Url
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";

            // Generate Docker Name
            String sProcessorName = m_oProcessor.getName();
            
            // Prepare the name of the image
            String sImageBaseName = "wasdi/" + sProcessorName + ":" + m_oProcessor.getVersion();
            sImageName = sImageBaseName;
            
            sUrl += "build?t=" + StringUtils.encodeUrl(sImageName);
            
            // Do we have a registry?
            if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
            	// Yes, add it to the docker name
            	WasdiLog.debugLog("DockerUtils.build: using registry " + m_sDockerRegistry);
            	sImageName = m_sDockerRegistry + "/" + sImageBaseName;
            	sUrl += "&t=" + StringUtils.encodeUrl(sImageName);

            	// Take all our registers
            	List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
            	
            	// For each register
            	for (DockerRegistryConfig oDockerRegistryConfig : aoRegisters) {
            		// Is this the main one? So jump, already added
					if (oDockerRegistryConfig.address.equals(m_sDockerRegistry)) continue;
					
					String sAddedName = oDockerRegistryConfig.address + "/"  + sImageBaseName; 
					
					// No, ok, we can add a new tag
					sUrl += "&t=" +  StringUtils.encodeUrl(sAddedName);
					
					WasdiLog.debugLog("DockerUtils.build: added tag  for " + oDockerRegistryConfig.id);
				}            	
            }
    		            	
        	// If we have user and or pip params, add the buildargs parameter
        	if (!Utils.isNullOrEmpty(m_sWasdiSystemUserName) || !Utils.isNullOrEmpty(WasdiConfig.Current.dockers.pipInstallWasdiAddress)) {
        		
        		WasdiLog.debugLog("DockerUtils.build: adding build args to url");
        		
        		String sBuildArgs = "{";
        		
        		boolean bAdded = false;
        		
                if (!Utils.isNullOrEmpty(m_sWasdiSystemUserName)) {
                	sBuildArgs += "\"USR_NAME\":\""+m_sWasdiSystemUserName+"\",\"USR_ID\":\"" + m_iWasdiSystemUserId + "\",\"GRP_NAME\":\""+m_sWasdiSystemGroupName+"\",\"GRP_ID\":\"" + m_iWasdiSystemGroupId +"\"";                    	
                	bAdded = true;
                }
                
                if (!Utils.isNullOrEmpty(WasdiConfig.Current.dockers.pipInstallWasdiAddress)) {
                	if (bAdded) sBuildArgs+=",";
                	sBuildArgs+="\"PIP_INSTALL_WASDI_ARGUMENTS\":\""+ WasdiConfig.Current.dockers.pipInstallWasdiAddress + "\"";
                }            	
        		
                sBuildArgs += "}";
                
                String sEncodedBuildArgs = StringUtils.encodeUrl(sBuildArgs);
                
                sUrl+="&buildargs=" + sEncodedBuildArgs;
        	}
        	
        	// We need to make a Tar File
        	final ArrayList<String> asTarFiles = new ArrayList<>();
    		
        	// Get the list of all files in the processor folder
			Path oPath = Paths.get(m_sProcessorFolder);
			
			try (Stream<Path> oPathStream = Files.walk(oPath)) { 
				oPathStream.filter(oFilteredPath -> !Files.isDirectory(oFilteredPath)).forEach(oFilePath -> {
					asTarFiles.add(oFilePath.toString());
				});
			}
			
			WasdiLog.debugLog("DockerUtils.build: creating tar file");
        	
			// Name of the ouput tar
        	String sTarFileOuput = m_sProcessorFolder + m_oProcessor.getProcessorId() + ".tar";
        	
        	// Tar the file
        	if (!TarUtils.tarFiles(asTarFiles, m_sProcessorFolder, sTarFileOuput)) {
        		WasdiLog.errorLog("Impossible to create the tar file to upload to docker, this is a problem!");
        		return "";
        	}
        	
        	// Set the Content Type Header
        	HashMap<String, String> asHeaders = new HashMap<>();
        	asHeaders.put("Content-type","application/x-tar");
        	
    		if (WasdiConfig.Current.dockers.logDockerAPICallsPayload) {
        		WasdiLog.debugLog("DOCKER URL DUMP");
        		WasdiLog.debugLog(sUrl);
        		WasdiLog.debugLog("------------------");            			
    		}
        	
    		WasdiLog.debugLog("DockerUtils.build: start build");
    		
        	// Finally make the call
        	HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, new File(sTarFileOuput), asHeaders);
        	
        	WasdiLog.debugLog("DockerUtils.build: build done");
        	
        	String sBuildOutput = oResponse.getResponseBody();
        	sBuildOutput = cleanDockerLogsString(sBuildOutput);
        	
        	String sBuildDate = Utils.getFormatDate(new Date());
        	sBuildOutput = sBuildDate + " - v." + m_oProcessor.getVersion() + "|START_BUILD_LOG|"+sBuildOutput;
        	
        	// Save the build output
        	m_oProcessor.getBuildLogs().add(sBuildOutput);
        	ProcessorRepository oProcessorRepository = new ProcessorRepository();
        	oProcessorRepository.updateProcessor(m_oProcessor);
        	
        	if (WasdiConfig.Current.dockers.logDockerAPICallsPayload) {
        		WasdiLog.debugLog("DockerUtils.build: build output = " + sBuildOutput);
        	}
        	
        	// Delete the tar
        	WasdiFileUtils.deleteFile(sTarFileOuput);
        	
        	if (oResponse.getResponseCode() != 200) {
        		WasdiLog.errorLog("There was an error in the post: message " + oResponse.getResponseBody());
        		return "";
        	}

            WasdiLog.debugLog("DockerUtils.build: created image " + sImageName);
            
        } catch (Exception oEx) {
        	WasdiLog.errorLog("DockerUtils.build: " + oEx.toString());
            return "";
        }

        return sImageName;
    }
    
    /**
     * Clean the string representing the log of a docker build, replacing
     * different special chars obtained by dockers api.
     * 
     * @param sInputString Logs returned from docker
     * @return Logs cleaned from special chars
     */
    protected String cleanDockerLogsString(String sInputString) {
    	String sOutputString = "";
    	
    	sOutputString = sInputString.replace("{\"stream\":", "");
    	sOutputString = sOutputString.replace("\"\\n\"}", "\n");
    	sOutputString = sOutputString.replace("\\n\"}", "\n");
    	sOutputString = sOutputString.replace("\"}", "");
    	sOutputString = sOutputString.replace("---\\u003e", "");
    	sOutputString = sOutputString.replace("\\u0026", "");
    	sOutputString = sOutputString.replace("\\u0026", "");
    	sOutputString = sOutputString.replace("\\u001b", "");
    	
    	sOutputString = sOutputString.replace("\\n", "\n");
    	
    	
    	return sOutputString;
    }

    /**
     * Run the docker
     */
    public String start() {
        return start("");
    }
    
    /**
     * Run the docker
     * 
     * @param sMountWorkspaceFolder workspace folder to mount
     * 
     */
    public String start(String sMountWorkspaceFolder) {
        return start(sMountWorkspaceFolder, m_oProcessor.getPort());
    }    
    
    /**
     * Run the docker at the specified port
     *
     * @param sMountWorkspaceFolder workspace folder to mount
     * @param iProcessorPort Port to use
     */
    public String start(String sMountWorkspaceFolder, int iProcessorPort) {
    	return start(sMountWorkspaceFolder, iProcessorPort, false);
    }
    
    
    /**
     * Run the docker at the specified port
     *
     * @param sMountWorkspaceFolder workspace folder to mount
     * @param iProcessorPort Port to use
     * @param bAutoRemove true to autoremove the container once is done
     */
    public String start(String sMountWorkspaceFolder, int iProcessorPort, boolean bAutoRemove) {
    	return start(sMountWorkspaceFolder, iProcessorPort, bAutoRemove, true);
    }
    
    /**
     * Run the docker at the specified port
     *
     * @param sMountWorkspaceFolder workspace folder to mount
     * @param iProcessorPort Port to use
     * @param bAutoRemove true to autoremove the container once is done
     */
    public String start(String sMountWorkspaceFolder, int iProcessorPort, boolean bAutoRemove, boolean bReuseExistingContainer) {

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
        	ContainerInfo oContainerInfo = null;
        	
        	if (bReuseExistingContainer) {
        		oContainerInfo = getContainerInfoByImageName(m_oProcessor.getName(), m_oProcessor.getVersion());
        	}
        	else {
        		WasdiLog.warnLog("DockerUtils.start: reuse existing container is false, force the creation of a new one");
        	}
        	
        	boolean bNameIsDefined = true;
        	
        	if (oContainerInfo == null) {
        		
        		// No we do not have it
        		WasdiLog.debugLog("DockerUtils.start: the container is not available");
        		
        		// Do we have at least the image?
        		if (isImageAvailable(m_oProcessor) == false) {
        			
        			// No, we need to pull the image
        			WasdiLog.debugLog("DockerUtils.start: also the image is not available: pull it");
        			
        			processWorkspaceLog("WASDI is pulling the last version of your application, please wait");
        			
        			boolean bPullResult = pull(sImageName, sToken);
        			
        			if (!bPullResult) {
        				// Impossible to pull, is a big problem
        				WasdiLog.errorLog("DockerUtils.start: Error pulling the image, we cannot proceed");
        				processWorkspaceLog("There was an error pulling the image, we cannot proceed");
        				processWorkspaceLog(new EndMessageProvider().getBad());
        				return "";
        			}
        			
        			processWorkspaceLog("Application updated!");
        		}
        		
        		// Since we are creating the Container, we need to set up our name
        		sContainerName = m_oProcessor.getName() + "_" + m_oProcessor.getVersion() + "_" + Utils.getRandomName();
        		WasdiLog.debugLog("DockerUtils.start: image pulled, create the container named " + sContainerName);
        		
        		if (sContainerName.length()>62) {
        			sContainerName = sContainerName.substring(0, 62);
        			WasdiLog.debugLog("DockerUtils.start: container name too long, cut it to " + sContainerName);
        		}
        		
        		// Create the container
            	try {
            		// API URL
            		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
            		if (!sUrl.endsWith("/")) sUrl += "/";
            		sUrl += "containers/create?name=" + sContainerName;
            		
            		// Create the Payload to send to create the container
            		CreateParams oContainerCreateParams = new CreateParams();
            		
                    String sProcessorType = m_oProcessor.getType();
                    ProcessorTypeConfig oProcessorTypeConfig = WasdiConfig.Current.dockers.getProcessorTypeConfig(sProcessorType);
                    
                    if (oProcessorTypeConfig==null) {
                    	WasdiLog.errorLog("DockerUtils.start: impossible to find the configuration for " + sProcessorType + " we need to stop here.");
                    	return "";
                    }
            		
            		// Set the user
            		oContainerCreateParams.User = m_sWasdiSystemUserName+":"+m_sWasdiSystemGroupName;
            		
            		// Set the image
            		oContainerCreateParams.Image = sImageName;
            		
            		// Default Container base data folder
            		String sOnContainerDataFolder = "/data/wasdi";
            		
            		// Do we need to mount all data or only the workspace?
            		if (oProcessorTypeConfig.mountOnlyWorkspaceFolder) {
            			sMountWorkspaceFolder = PathsConfig.getWorkspacePath(m_oProcessorParameter);
            			if (sOnContainerDataFolder.endsWith("/")) sOnContainerDataFolder = sOnContainerDataFolder.substring(0,sOnContainerDataFolder.length()-1);
            			WasdiLog.debugLog("DockerUtils.start: mounting only workspace folder in " + sOnContainerDataFolder);
            		}
            		
            		// Mount the data folder
            		oContainerCreateParams.HostConfig.Binds.add(sMountWorkspaceFolder+":"+ sOnContainerDataFolder);            		
            		
            		// Do we have additional mount points configured?
            		if (oProcessorTypeConfig.additionalMountPoints!=null) {
            			for (String sMountPoint : oProcessorTypeConfig.additionalMountPoints) {
            				oContainerCreateParams.HostConfig.Binds.add(sMountPoint);
						}
            		}
            		
            		List<S3Volume> aoVolumesToMount = PermissionsUtils.getVolumesToMount(m_oProcessorParameter);
            		
            		if (aoVolumesToMount != null) {
            			if (aoVolumesToMount.size()>0) {
            				WasdiLog.debugLog("DockerUtils.start: Found " + aoVolumesToMount.size() + " S3 Volumes to mount");
            				
            				for (S3Volume oS3Volume : aoVolumesToMount) {
            					
            					String sRemoteVolume = sOnContainerDataFolder;
            					
            					if (!oProcessorTypeConfig.mountOnlyWorkspaceFolder) {
            						sRemoteVolume = PathsConfig.getWorkspacePath(m_oProcessorParameter, sOnContainerDataFolder);
            					}
            					
            					if (!sRemoteVolume.endsWith( "/")) sRemoteVolume+= "/";
            					
								String sMountingVolume = PathsConfig.getS3VolumesBasePath()+oS3Volume.getMountingFolderName() + ":" + sRemoteVolume + oS3Volume.getMountingFolderName();
								
								if (oS3Volume.isReadOnly()) {
									sMountingVolume += ":ro";
								}
								
								WasdiLog.debugLog("DockerUtils.start: Adding Volume " + sMountingVolume);
								oContainerCreateParams.HostConfig.Binds.add(sMountingVolume);
								
								// When we mount, the docker creates a folder, and it becomes of root. 
								// So we need to create it here, at least is of the right user..
								String sHostWorkspacePath = PathsConfig.getWorkspacePath(m_oProcessorParameter);
								sHostWorkspacePath += oS3Volume.getMountingFolderName();
								File oHostS3Folder = new File(sHostWorkspacePath);
								if (!oHostS3Folder.exists()) {
									oHostS3Folder.mkdirs();
								}
							}
            			}
            		}
            		
            		// Set the network mode
            		oContainerCreateParams.HostConfig.NetworkMode = m_sDockerNetworkMode;
            		
            		// Mount the processor folder only if it exists
            		File oProcessorFolder = new File(m_sProcessorFolder);
            		
            		if (oProcessorFolder.exists() && oProcessorFolder.isDirectory()) {
                		// Add the volume with the Processor Code
                		MountVolumeParam oProcessorPath = new MountVolumeParam();
                		oProcessorPath.Source = m_sProcessorFolder;
                		oProcessorPath.Target = "/wasdi";
                		oProcessorPath.ReadOnly = false;
                		oProcessorPath.Type= MountTypes.BIND;
                		
                		oContainerCreateParams.HostConfig.Mounts.add(oProcessorPath);            			
            		}
            		    
            		
            		if (bAutoRemove) {
            			oContainerCreateParams.HostConfig.AutoRemove = true;
            		}
            		else {
                		oContainerCreateParams.HostConfig.RestartPolicy.put("Name", "no");
                		oContainerCreateParams.HostConfig.RestartPolicy.put("MaximumRetryCount", 0);            			
            		}
            		
            		// Expose the TCP Port
            		oContainerCreateParams.ExposedPorts.add("" + WasdiConfig.Current.dockers.processorsInternalPort + "/tcp");
            		
            		// If we are dockerized, no need to add the ip and port
            		if (WasdiConfig.Current.shellExecLocally) {
            			oContainerCreateParams.HostConfig.PortBindings.put("" + WasdiConfig.Current.dockers.processorsInternalPort + "/tcp", ""+iProcessorPort);
            		}
            		
                    // Extra hosts mapping, useful for some instances when the server host can't be resolved
                    // The symptoms of such problem is that the POST call from the Docker container timeouts
                    if (WasdiConfig.Current.dockers.extraHosts != null) {
                    	
                    	if (WasdiConfig.Current.dockers.extraHosts.size()>0) {
                    		WasdiLog.debugLog("DockerUtils.start: adding configured extra host mapping to the run arguments");
                        	for (int iExtraHost = 0; iExtraHost<WasdiConfig.Current.dockers.extraHosts.size(); iExtraHost ++) {
                        		String sExtraHost = WasdiConfig.Current.dockers.extraHosts.get(iExtraHost);
                        		oContainerCreateParams.HostConfig.ExtraHosts.add(sExtraHost);
                        	}
                    	}
                    }
                                        
                    if (oProcessorTypeConfig != null) {
                    	if (oProcessorTypeConfig.environmentVariables != null) {
                    		
                    		for (EnvironmentVariableConfig oEnvironmentVariable : oProcessorTypeConfig.environmentVariables) {
                    			
                    			String sKey = oEnvironmentVariable.key;
                    			String sValue = oEnvironmentVariable.value;
                    			
                    			if (Utils.isNullOrEmpty(sKey)) {
                    				WasdiLog.warnLog("DockerUtils.start: found the ProcessorTypeConfig but there is an env variable with key null. Jump it");
                    				continue;
                    			}
                    			
                    			if (sValue == null) sValue="";
                    			
                    			String sVariable = sKey + "=" + sValue;
                    			
                    			WasdiLog.debugLog("DockerUtils.start: adding env variable: " + sVariable);
                    			
                    			oContainerCreateParams.Env.add(sVariable);
							}
                    	}
                    }
                    
                    if (WasdiConfig.Current.nvidiaGPUAvailable) {
                    	WasdiLog.warnLog("DockerUtils.start: Node has a GPU, enabling NVIDIA in the Container");
                    	oContainerCreateParams.HostConfig.EnableGpu = true;
                    }
                    else {
                    	oContainerCreateParams.HostConfig.EnableGpu = false;
                    }
                    
                    // Convert the payload in JSON (NOTE: is hand-made !!)
            		String sContainerCreateParams = oContainerCreateParams.toJson();
            		
            		if (Utils.isNullOrEmpty(sContainerCreateParams)) {
            			WasdiLog.errorLog("DockerUtils.start: impossible to get the payload to create the container. We cannot proceed :(");
            			return "";
            		}
            		
            		if (WasdiConfig.Current.dockers.logDockerAPICallsPayload) {
                		WasdiLog.debugLog("DOCKER PAYLOAD DUMP");
                		WasdiLog.debugLog(sContainerCreateParams);
                		WasdiLog.debugLog("------------------");            			
            		}              		
            		
            		// We need to set the json Content-Type
            		HashMap<String, String> asHeaders = new HashMap<>();
            		asHeaders.put("Content-Type", "application/json");
            		
            		// Is a post!
            		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, sContainerCreateParams, asHeaders);
            		
            		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
            			// Here is not good
            			WasdiLog.errorLog("DockerUtils.start: impossible to create the container. We cannot proceed :(. ERROR = " + oResponse.getResponseBody());
            			return "";
            		}
            		
            		WasdiLog.debugLog("DockerUtils.start: Container created");
            		
            	}
            	catch (Exception oEx) {
            		WasdiLog.errorLog("DockerUtils.start exception creating the container: " + oEx.toString());
                    return "";
                }        		
        	}
        	else {        		
        		// If the container exists, we can take the ID
        		WasdiLog.debugLog("DockerUtils.start: Container already found, we will use the id");
        		
        		// Now we use the id to be sure, but we need to return the name after.
        		sContainerName = oContainerInfo.Id;
        		// So here we remember this
        		bNameIsDefined = false;
        	}
            
            WasdiLog.debugLog("DockerUtils.start: Starting Container Named " + sContainerName);
            
            // Prepare the url to start it
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		sUrl +="containers/" + sContainerName + "/start";
    		
    		// Make the call
    		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, "");
    		
    		// Did we used the real name or the id?
    		if (!bNameIsDefined) {
    			// The id: check if we can saefly find the name
    			if (oContainerInfo!=null) {
    				if (oContainerInfo.Names != null) {
    					if (oContainerInfo.Names.size()>0) {
    						sContainerName = oContainerInfo.Names.get(0);
    					}
    				}
    			}
    		}
    		
    		if (oResponse.getResponseCode() == 204) {
    			// Started Well
    			WasdiLog.debugLog("DockerUtils.start: Container " + sContainerName + " started");
    			return sContainerName;
    		}
    		else if (oResponse.getResponseCode() == 304) {
    			// ALready Started (but so why we did not detected this before?!?)
    			WasdiLog.debugLog("DockerUtils.start: Container " + sContainerName + " was already started");
    			return sContainerName;
    		}
    		else {
    			// Error!
    			WasdiLog.errorLog("DockerUtils.start: Impossible to start Container " + sContainerName + " Message: " + oResponse.getResponseBody());
    			return "";
    		}
            
        } catch (Exception oEx) {
        	WasdiLog.errorLog("DockerUtils.start error creating the container: " + oEx.toString());
            return "";
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
     * It deletes also all the previous versions
     * 
     * @param sProcessorName Name of the processor to delete
     * @param sVersion Version
     * @return
     */
    public boolean delete(String sProcessorName, String sVersion) {

        try {            
            
        	// Get the version of the Docker
            int iVersion = StringUtils.getAsInteger(sVersion);
            
            List<Object> aoOutputJsonMap = getContainersInfo(true);
            
            // If it is a WASDI Valid one
        	while (iVersion>0)  {
        		
        		// We start removing the requested version
        		WasdiLog.debugLog("DockerUtils.delete: Removing " + sProcessorName + " version "  + sVersion);
        		
                String sBaseDockerName = "wasdi/" + sProcessorName + ":" + sVersion;
                String sDockerName = sBaseDockerName;
                
                if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
                	sDockerName = m_sDockerRegistry + "/" + sDockerName;
                }
                
                // Check if we have a container
                String sId = getContainerIdFromWasdiAppName(sProcessorName, sVersion, aoOutputJsonMap);
                
                if (!Utils.isNullOrEmpty(sId)) {
                	// Clean the container
                    WasdiLog.debugLog("DockerUtils.delete: call Remove Container " + sId);
                    
                    boolean bContainersRemoved = removeContainer(sId, true);
                    
                    if (!bContainersRemoved) {
                    	WasdiLog.errorLog("DockerUtils.delete: Impossible to remove the container (maybe it was not present!!) for " + sProcessorName + " Version: " +  sVersion  + " Found Id: " + sId);
                    }                	
                }
                else {
                	WasdiLog.debugLog("DockerUtils.delete: no container found for " + sProcessorName + " version "  + sVersion);
                }
                
                List<Object> aoImagesAvailable = getImagesList();
                
                // Check the image
                if (isImageAvailable(sProcessorName,sVersion, m_sDockerRegistry, aoImagesAvailable)) {

                	// Remove the image
                    WasdiLog.debugLog("DockerUtils.delete: Removing image for " + sProcessorName + " version "  + sVersion + " Docker Image: " + sDockerName);
                    boolean bImageRemoved = removeImage(sDockerName, true);
                    
                    if (!bImageRemoved) {
                    	WasdiLog.errorLog("DockerUtils.delete: error removing the image for " + sProcessorName + " Version: " +  sVersion  + " Found Id: " + sId);
                    }
                    
                    // Try to delete local version
                    if (!sBaseDockerName.equals(sDockerName)) {
                        WasdiLog.debugLog("DockerUtils.delete: Removing also local image for " + sProcessorName + " version "  + sVersion + " Docker Image: " + sBaseDockerName);
                        bImageRemoved = removeImage(sBaseDockerName, true);
                        if (!bImageRemoved) {
                        	WasdiLog.warnLog("DockerUtils.delete: error removing the local image for " + sProcessorName + " Version: " +  sVersion  + " Docker Image: " + sBaseDockerName);
                        }                    
                    }                
                    
                    // And if we are in the main node
                    if (WasdiConfig.Current.isMainNode()) {           
                    	// With registers..
                        if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
                        	// Check if we have to clean also other registries
                        	WasdiLog.infoLog("DockerUtils.delete: This is a registry stored docker: clean all our registers");
                        	for (DockerRegistryConfig oRegistryConfig : WasdiConfig.Current.dockers.registers) {
                        		
                        		if (!m_sDockerRegistry.equals(oRegistryConfig.address)) {
                        			sDockerName = oRegistryConfig.address + "/" + sBaseDockerName;
                        			WasdiLog.infoLog("DockerUtils.delete: this is a backup registry, clean also the image " + sDockerName);
                        			this.removeImage(sDockerName);
                        		}
                        		
                        		this.removeImageFromRegistry(sBaseDockerName, sVersion, oRegistryConfig);
            				}
                        }
                    }
                }
                else {
                	WasdiLog.debugLog("DockerUtils.delete: no image found for " + sProcessorName + " version "  + sVersion );
                	
                	// But what about the other registries?
                    if (WasdiConfig.Current.isMainNode()) {            	
                        if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
                        	WasdiLog.debugLog("DockerUtils.delete:  Search back up registries");
                        	
                        	// For all the registers
                        	for (DockerRegistryConfig oRegistryConfig : WasdiConfig.Current.dockers.registers) {
                        		
                        		// If this is not the main one
                        		if (!m_sDockerRegistry.equals(oRegistryConfig.address)) {
                        			
                        			// Do we have a backup?
                        			WasdiLog.debugLog("DockerUtils.delete: delete " + sProcessorName + " version "  + sVersion + " in registry " + oRegistryConfig.address);
                        			if (isImageAvailable(sProcessorName, sVersion, oRegistryConfig.address, aoImagesAvailable)) {
                        				
                        				// Yes! Clean also it!!
                            			sDockerName = oRegistryConfig.address + "/" + sBaseDockerName;
                            			this.removeImage(sDockerName);
                            			this.removeImageFromRegistry(sBaseDockerName, sVersion, oRegistryConfig);                				
                        			}
                        		}
            				}   	
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
    
    /**
     * Log in a Docker Registry
     * @param oDockerRegistryConfig
     * @return
     */
    public String loginInRegistry(DockerRegistryConfig oDockerRegistryConfig) {
    	return loginInRegistry(oDockerRegistryConfig.address, oDockerRegistryConfig.user, oDockerRegistryConfig.password);
    }

    /**
     * Log in docker on a specific Repository Server
     * @param sServer Server Address
     * @param sUser Server User
     * @param sPassword Server Password
     * @param sFolder 
     * @return The encoded auth header for the registry or an empty string
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
    		
    		// Get the docker address
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		
    		if (sImage.contains(":")) {
    			WasdiLog.debugLog("DockerUtils.push image contains a tag: remove it");
    			sImage = sImage.split(":")[0];
    		}    		
    		
    		// Url Encode
    		String sEncodedServerImage = StringUtils.encodeUrl(sImage);
    		
    		// Add the query parameter
    		sUrl += "images/"+sEncodedServerImage + "/push";
    		
    		// We need a couple of headers
    		HashMap<String, String> asHeaders = new HashMap<>();
    		
    		// The content is taken from the sample on line
    		asHeaders.put("X-Registry-Auth", sToken);
    		
    		// P.Campanella 2025-02-19: since we experience sometimes bad answers from the registry, add the retry here
    		int iAttemp = 0;
    		int iResponseCode = -1;
    		
    		while (iAttemp<WasdiConfig.Current.dockers.numberOfAttemptsToPingTheServer) {
        		// Is a post!
        		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, "", asHeaders);
        		iResponseCode = oResponse.getResponseCode();
        		
        		if (iResponseCode==200) break;
        		
        		iAttemp= iAttemp+1;
        		
        		WasdiLog.debugLog("DockerUtils.push: the registry returned " + iResponseCode + " make another try (n. " + iAttemp + ") after a sleep");
        		
    			try {
    				Thread.sleep(WasdiConfig.Current.dockers.millisBetweenAttmpts);
    			}
    			catch (InterruptedException oEx) {
					Thread.currentThread().interrupt();
					WasdiLog.errorLog("DockerUtils.push: Current thread was interrupted", oEx);
				}        		
    		}
    		
    		if (iResponseCode == 200) {
    			return true;
    		}
    		else {
    			WasdiLog.errorLog("DockerUtils.push: Error pushing image " + sImage + " got answer http " + iResponseCode);
    			return false;
    		}
    	} 
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.push: " + oEx.toString());
            return false;
        }
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
    		String sEncodedServerImage = StringUtils.encodeUrl(sImage);
    		
    		// Add the query parameter
    		sUrl += "images/create?fromImage="+sEncodedServerImage;
    		
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
    		List<Object> aoOutputJsonMap = getContainersInfo(true);
    		return getContainerIdFromWasdiAppName(sProcessorName, sVersion, aoOutputJsonMap);            
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.getContainerIdFromWasdiAppName: " + oEx.toString());
		}
    	return "";
    }
	
    /**
     * Gets the container id starting from processor name and version
     * @param sProcessorName Name of the processor
     * @param sVersion Version as string
     * @param aoOutputJsonMap List of the containers info objects
     * @return
     */    
    @SuppressWarnings("unchecked")
    protected String getContainerIdFromWasdiAppName(String sProcessorName, String sVersion, List<Object> aoOutputJsonMap) {
    	try {
            String sMyImage = "wasdi/" + sProcessorName + ":" + sVersion;
            String sId = "";
            
            for (Object oContainer : aoOutputJsonMap) {
				try {
					LinkedHashMap<String, String> oContainerMap = (LinkedHashMap<String, String>) oContainer;
					String sImageName = oContainerMap.get("Image");
					
					if (sImageName.endsWith(sMyImage)) {
						WasdiLog.debugLog("DockerUtils.getContainerIdFromWasdiAppName: found my container " + sMyImage + " Docker Image = " +sImageName);
						sId = oContainerMap.get("Id");
						return sId;
					}
					
				}
		    	catch (Exception oEx) {
		    		WasdiLog.errorLog("DockerUtils.getContainerIdFromWasdiAppName: error parsing a container json entity " + oEx.toString());
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
            	
            	WasdiLog.debugLog("DockerUtils.removeContainer: search id " + sId);
            	
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
            else {
            	WasdiLog.debugLog("DockerUtils.removeContainer: sId is null or empty, nothing to do");
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
    public boolean removeImage(String sImageName, boolean bForce) {
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
        			WasdiLog.errorLog("DockerUtils.removeImage: Bad answer..: " + oResponse.getResponseCode() + " Message:" + oResponse.getResponseBody());
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
    		
    		//WasdiLog.debugLog("DockerUtils.isContainerStarted: search the container");
    		ContainerInfo oContainer = getContainerInfoByImageName(sProcessorName, sVersion);
    		
    		if (oContainer == null) {
    			WasdiLog.debugLog("DockerUtils.isContainerStarted: container not found, so for sure not started " + sProcessorName + " V: " + sVersion);
    			return false;
    		}
    		
    		if (oContainer.State.equals(ContainerStates.RUNNING)) {
    			WasdiLog.debugLog("DockerUtils.isContainerStarted: found running container for " + sProcessorName + " V: " + sVersion);
    			return true;
    		}
    		else {
    			WasdiLog.debugLog("DockerUtils.isContainerStarted: found NOT RUNNING container for " + sProcessorName + " V: " + sVersion);
    			return false;    		
    		}
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.isContainerStarted: " + oEx.toString());
            return false;
        }
    }
    
    
    /**
     * Check if a container is started with a specific timeout
     * @param sContainerId Container Id
     * @return true when done. False in case of errors
     */
    public boolean waitForContainerToFinish(String sContainerId) {
    	return waitForContainerToFinish(sContainerId, -1);
    }
    
    /**
     * Check if a container is started with a specific timeout
     * @param sContainerId  Container Id
     * @param iMsTimeout Max Milliseconds to wait. -1 for infinite
     * @return true when done. False in case of errors
     */
    public boolean waitForContainerToFinish(String sContainerId, int iMsTimeout) {
    	
    	try {
    		
    		boolean bFinished = false;
    		
    		int iLoopCount = 0;
    		int iLogMaxCount = WasdiConfig.Current.dockers.numberOfPollStatusPollingCycleForLog;
    		
    		if (iLogMaxCount<=1) iLogMaxCount = 1;
    		
    		while (!bFinished) {
    			
        		ContainerInfo oContainer = getContainerInfoByContainerId(sContainerId);
        		
        		if (oContainer == null) {
        			WasdiLog.debugLog("DockerUtils.waitForContainerToFinish: container not found, so for sure not started");
        			return false;
        		}
        		
        		if (oContainer.State.equals(ContainerStates.EXITED) || oContainer.State.equals(ContainerStates.DEAD)) return true;
        		else {
        			
        			if (iLoopCount%iLogMaxCount == 0) {
        				WasdiLog.debugLog("DockerUtils.waitForContainerToFinish: still waiting");
        			}
        			
        			try {
        				Thread.sleep(WasdiConfig.Current.dockers.millisBetweenStatusPolling);
        			}
        			catch (InterruptedException oEx) {
						Thread.currentThread().interrupt();
						WasdiLog.errorLog("DockerUtils.waitForContainerToFinish. Current thread was interrupted", oEx);
					}
        		}
        		
        		iLoopCount++;
    		}
    		
    		return true;
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.waitForContainerToFinish: " + oEx.toString());
            return false;
        }
    }    
    
    /**
     * Check if a container is started
     * @param sProcessorName
     * @param sVersion
     * @return
     */
    @SuppressWarnings("unchecked")
	public ContainerInfo getContainerInfoByImageName(String sProcessorName, String sVersion) {
    	
    	try {
    		List<Object> aoOutputJsonMap = getContainersInfo(true);
            
            String sMyImage = "wasdi/" + sProcessorName + ":" + sVersion;
            
            if (!Utils.isNullOrEmpty(m_sDockerRegistry))   {
            	sMyImage = m_sDockerRegistry + "/" + sMyImage;
            }
            
            for (Object oContainer : aoOutputJsonMap) {
				try {
					LinkedHashMap<String, Object> oContainerMap = (LinkedHashMap<String, Object>) oContainer;
					String sImageName = (String) oContainerMap.get("Image");
					
					if (sImageName.endsWith(sMyImage)) {
						WasdiLog.debugLog("DockerUtils.getContainerInfoByImageName: found my container " + sMyImage + " Docker Image = " +sImageName);
						
						return convertContainerMapToContainerInfo(oContainerMap);
					}
					
				}
		    	catch (Exception oEx) {
		    		WasdiLog.errorLog("DockerUtils.getContainerInfoByImageName: error parsing a container json entity " + oEx.toString());
		        }
			}
    		
    		return null;
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.getContainerInfoByImageName: error ", oEx);
            return null;
        }
    }
    
    
    /**
     * Get the info of a container starting from the name
     * @param sContainerName Name of the container
     * @return ContainerInfo or null
     */
    @SuppressWarnings("unchecked")
	public ContainerInfo getContainerInfoByContainerName(String sContainerName) {
    	
    	try {
    		//WasdiLog.debugLog("DockerUtils.getContainerInfoByContainerName: Searching for container named: " + sContainerName );
    		
    		List<Object> aoOutputJsonMap = getContainersInfo(true);
    		
    		if (!sContainerName.startsWith("/")) sContainerName = "/" + sContainerName;
                        
            for (Object oContainer : aoOutputJsonMap) {
				try {
					LinkedHashMap<String, Object> oContainerMap = (LinkedHashMap<String, Object>) oContainer;
					
					boolean bFound = false;
					List<String> asNames = (List<String>) oContainerMap.get("Names");
					
					if (asNames == null) continue;
					
					for (String sName : asNames) {
						if (sName.equals(sContainerName)) {
							bFound = true;
							break;
						}
					}
					
					if (bFound) {						
						return convertContainerMapToContainerInfo(oContainerMap);
					}
					
				}
		    	catch (Exception oEx) {
		    		WasdiLog.errorLog("DockerUtils.getContainerInfoByContainerName: error parsing a container json entity " + oEx.toString());
		        }
			}

            WasdiLog.debugLog("DockerUtils.getContainerInfoByContainerName: container " + sContainerName + " NOT found" );
    		return null;
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.getContainerInfoByContainerName: " + oEx.toString());
            return null;
        }
    }
    
    
    /**
     * Get the info of a container starting from the name
     * @param sContainerId Name of the container
     * @return ContainerInfo or null
     */
    @SuppressWarnings("unchecked")
	public ContainerInfo getContainerInfoByContainerId(String sContainerId) {
    	
    	try {
    		//WasdiLog.debugLog("DockerUtils.getContainerInfoByContainerId: Searching for container id: " + sContainerId );
    		
    		List<Object> aoOutputJsonMap = getContainersInfo(true);
    		
            for (Object oContainer : aoOutputJsonMap) {
				try {
					LinkedHashMap<String, Object> oContainerMap = (LinkedHashMap<String, Object>) oContainer;
					
					String sId = (String) oContainerMap.get("Id");
					
					if (Utils.isNullOrEmpty(sId)) continue;
					
					if (sId.equals(sContainerId)) {
						//WasdiLog.debugLog("DockerUtils.getContainerInfoByContainerId: found my container " + sContainerId );						
						return convertContainerMapToContainerInfo(oContainerMap);
					}					
					
				}
		    	catch (Exception oEx) {
		    		WasdiLog.errorLog("DockerUtils.getContainerInfoByContainerId: error parsing a container json entity " + oEx.toString());
		        }
			}
    		
    		return null;
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.getContainerInfoByContainerId: " + oEx.toString());
            return null;
        }
    }
    
    /**
     * Get the list of running containers from docker
     * @return
     */
    protected List<Object> getContainersInfo() {
    	return getContainersInfo(false);
    }
    
    /**
     * Get the list of containers from docker
     * @return List of objects as returned by Docker API
     */
    protected List<Object> getContainersInfo(boolean bAll) {
    	try {
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		sUrl += "containers/json";
    		
    		if (bAll) {
    			sUrl += "?all=true";
    		}
    		
    		HttpCallResponse oResponse = HttpUtils.httpGet(sUrl);
    		
    		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
    			return null;
    		}
    		
    		List<Object> aoOutputJsonMap = null;

            try {
                ObjectMapper oMapper = new ObjectMapper();
                aoOutputJsonMap = oMapper.readValue(oResponse.getResponseBody(), new TypeReference<List<Object>>(){});
            } catch (Exception oEx) {
                WasdiLog.errorLog("DockerUtils.getContainerInfoByProcessor: exception converting API result " + oEx);
                return null;
            }
            
            return aoOutputJsonMap;
    	}
    	catch (Exception oEx) {
            WasdiLog.errorLog("DockerUtils.getContainerInfoByProcessor: exception converting API result " + oEx);
            return null;
        }    	
    }
    
    /**
     * Converts a String,Object dictionary, return of the docker api for containers info
     * into a ContainerInfo object
     * @param oContainerMap Map object as returned by docker api
     * @return ContainerInfo entity
     */
    @SuppressWarnings("unchecked")
	protected ContainerInfo convertContainerMapToContainerInfo(LinkedHashMap<String, Object> oContainerMap) {
		ContainerInfo oContainerInfo = new ContainerInfo();
		
		try {
			oContainerInfo.Id = (String) oContainerMap.get("Id");
			oContainerInfo.Image = (String) oContainerMap.get("Image");
			oContainerInfo.ImageId = (String) oContainerMap.get("ImageId");
			oContainerInfo.State = (String) oContainerMap.get("State");
			oContainerInfo.Status = (String) oContainerMap.get("Status");
			oContainerInfo.Names = (List<String>) oContainerMap.get("Names");			
		}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.convertContainerMapToContainerInfo: ", oEx);
        }
		
		return oContainerInfo;
    	
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
     * Search if an image is availalbe using the registry set as member property (m_sDockerRegistry)
     * @param sProcessorName Processor Name
     * @param sVersion Processor Version
     * @return true if the image is available
     */
    public boolean isImageAvailable(String sProcessorName, String sVersion) {
    	return isImageAvailable(sProcessorName, sVersion, m_sDockerRegistry);
    }
    
    /**
     * Check if an image is available on the local registry
     * @param sProcessorName Processor Name
     * @param sVersion Processor Version
     * @param sRegistry optional Docker registry
     * @return true if the image is available
     */
	public boolean isImageAvailable(String sProcessorName, String sVersion, String sRegistry) {
    	
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
    
            return isImageAvailable(sProcessorName, sVersion, sRegistry, aoOutputJsonMap);
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.isImageAvailable: " + oEx.toString());
            return false;
        }
    }    
    
    /**
     * Check if an image is available on the local registry
     * @param sProcessorName Processor Name
     * @param sVersion Processor Version
     * @param sRegistry optional Docker registry
     * @param aoOutputJsonMap list of images as returned by registry api
     * @return true if the image is available
     */
    @SuppressWarnings("unchecked")
	public boolean isImageAvailable(String sProcessorName, String sVersion, String sRegistry, List<Object> aoOutputJsonMap) {
    	
    	try {
            // Define the name of our image
            String sMyImage = "";
            
            if (!Utils.isNullOrEmpty(sRegistry)) sMyImage = sRegistry + "/";
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
							WasdiLog.debugLog("DockerUtils.isImageAvailable: found my image " + sMyImage);
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
     * Get a list of images available in docker
     * @return
     */
	public List<Object> getImagesList() {
		List<Object> aoReturn = new ArrayList<>();
    	try {
    		
    		
    		// Get the internal API address
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		
    		// End point to get the list of images
    		sUrl += "images/json";
    		
    		// Get the list
    		HttpCallResponse oResponse = HttpUtils.httpGet(sUrl);
    		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
    			return aoReturn;
    		}
    		
    		// Here we put the results
    		List<Object> aoOutputJsonMap = null;

            try {
                aoOutputJsonMap = JsonUtils.s_oMapper.readValue(oResponse.getResponseBody(), new TypeReference<List<Object>>(){});
            } catch (Exception oEx) {
                WasdiLog.errorLog("DockerUtils.getImagesList: exception converting API result " + oEx);
                return aoReturn;
            }
    
            return aoOutputJsonMap;
    	}
    	catch (Exception oEx) {
    		WasdiLog.errorLog("DockerUtils.getImagesList: " + oEx.toString());
            return aoReturn;
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
    		
    		sUrl += "repository/"  +  oRegistry.repositoryName + "/v2/";
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
        		
        		sUrl += "repository/"+  oRegistry.repositoryName +"/v2/";
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

    

    /**
     * Create and Run a container
     * @param sImageName Name of the image
     * @param sImageVersion Version
     * @param asArg Args to be passed as CMD parameter
     * @return The Id of the container if created, empty string in case of problems
     */
    public String run(String sImageName, String sImageVersion, List<String> asArg, boolean bAlwaysRecreateContainer,  ArrayList<String> asAdditionalMountPoints, boolean bAutoRemove) {

        try {
        	
        	// We need the name of the image
        	if (Utils.isNullOrEmpty(sImageName)) {
        		WasdiLog.warnLog("DockerUtils.run: image is null");
        		return "";
        	}        	
        	
            // Attach the version, if available
        	if (!Utils.isNullOrEmpty(sImageVersion))  sImageName += ":" + sImageVersion;
        	
        	// And the registry
            if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
            	sImageName = m_sDockerRegistry + "/" + sImageName;
            }
            
            WasdiLog.debugLog("DockerUtils.run: try to start a container from image " + sImageName);
            
            // Authentication Token for the registry (if needed)
            String sToken = "";
            // Name of the container
            String sContainerName = "";
            // Id of the container
            String sContainerId = "";            
            
            // Check if we need to login in the registry
            if (!Utils.isNullOrEmpty(m_sDockerRegistry)) {
            	
            	WasdiLog.debugLog("DockerUtils.run: login in the register");
            	
            	List<DockerRegistryConfig> aoRegisters = WasdiConfig.Current.dockers.getRegisters();
            	
				// For each register: ordered by priority
				for (int iRegisters=0; iRegisters<aoRegisters.size(); iRegisters++) {
					
					DockerRegistryConfig oDockerRegistryConfig = aoRegisters.get(iRegisters);
					
					if (!m_sDockerRegistry.equals(oDockerRegistryConfig.address)) continue;
					
					WasdiLog.debugLog("DockerUtils.run: logging in to " + oDockerRegistryConfig.id);
					
					// Try to login
					sToken = loginInRegistry(oDockerRegistryConfig);
					
					if (Utils.isNullOrEmpty(sToken)) {
						WasdiLog.errorLog("DockerUtils.run: error in the login this will be a problem!!");
					}
					
					break;
				}
            }
        	
            // Search first of all if the container is already here
        	ContainerInfo oContainerInfo = null;
        	
        	// Shall we try to re-use a contanier?
        	if (!bAlwaysRecreateContainer) {
        		// Yes! Maybe I'll find it?
        		oContainerInfo = getContainerInfoByImageName(sImageName, sImageVersion); 
        	}
        	
        	if (oContainerInfo == null) {
        		
        		// No we do not have it
        		WasdiLog.debugLog("DockerUtils.run: the container is not available");
        		
        		// Since we are creating the Container, we need to set up our name
        		sContainerName = sImageName.replace(":", "_").replace("/", "_") + "_" + Utils.getRandomName();
        		WasdiLog.debugLog("DockerUtils.run: try to create a container named " + sContainerName);
        		
        		if (sContainerName.length()>62) {
        			sContainerName = sContainerName.substring(0, 62);
        			WasdiLog.debugLog("DockerUtils.run: container name too long, cut it to " + sContainerName);
        		}
        		
        		// Create the container
            	try {
            		// API URL
            		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
            		if (!sUrl.endsWith("/")) sUrl += "/";
            		sUrl += "containers/create?name=" + sContainerName;
            		
            		// Create the Payload to send to create the container
            		CreateParams oContainerCreateParams = new CreateParams();
            		
            		// Set the user
            		oContainerCreateParams.User = m_sWasdiSystemUserName+":"+m_sWasdiSystemGroupName;
            		
            		// Set the image
            		oContainerCreateParams.Image = sImageName;            		
            		
            		// Mount the /data/wasdi/ folder
            		oContainerCreateParams.HostConfig.Binds.add(PathsConfig.getWasdiBasePath()+":"+"/data/wasdi");
            		
            		// Set the network mode
            		oContainerCreateParams.HostConfig.NetworkMode = m_sDockerNetworkMode;
            		
            		if (asAdditionalMountPoints!=null) {
            			for (String sMountPoint : asAdditionalMountPoints) {
            				oContainerCreateParams.HostConfig.Binds.add(sMountPoint);
						}
            		}
            		            		
            		if (bAutoRemove) {
            			oContainerCreateParams.HostConfig.AutoRemove = true;
            		}
            		else {
                		oContainerCreateParams.HostConfig.RestartPolicy.put("Name", "no");
                		oContainerCreateParams.HostConfig.RestartPolicy.put("MaximumRetryCount", 0);            			
            		}
            		
            		if (asArg!=null) oContainerCreateParams.Cmd.addAll(asArg);
            		
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
                    
                    if (WasdiConfig.Current.dockers.groupAdd != null) {
                    	if (WasdiConfig.Current.dockers.groupAdd.size()>0) {
                    		WasdiLog.debugLog("Adding Group Add with " + WasdiConfig.Current.dockers.groupAdd.size() + " entries");
                    		for (String sGroupId : WasdiConfig.Current.dockers.groupAdd) {
								oContainerCreateParams.HostConfig.GroupAdd.add(sGroupId);
							}
                    	}
                    }
            		
                    // Convert the payload in JSON (NOTE: is hand-made !!)
            		String sContainerCreateParams = oContainerCreateParams.toJson();
            		
            		if (Utils.isNullOrEmpty(sContainerCreateParams)) {
            			WasdiLog.errorLog("DockerUtils.run: impossible to get the payload to create the container. We cannot proceed :(");
            			return "";
            		}
            		
            		if (WasdiConfig.Current.dockers.logDockerAPICallsPayload) {
                		WasdiLog.debugLog("DOCKER JSON PAYLOAD DUMP");
                		WasdiLog.debugLog(sContainerCreateParams);
                		WasdiLog.debugLog("------------------");            			
            		}
            		
            		// We need to set the json Content-Type
            		HashMap<String, String> asHeaders = new HashMap<>();
            		asHeaders.put("Content-Type", "application/json");
            		
            		// Is a post!
            		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, sContainerCreateParams, asHeaders);
            		
            		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
            			// Here is not good
            			WasdiLog.errorLog("DockerUtils.run: impossible to create the container. We cannot proceed :(. ERROR = " + oResponse.getResponseBody());
            			return "";
            		}
            		
            		WasdiLog.debugLog("DockerUtils.run: Container created. Message = " + oResponse.getResponseBody());
            		
            		String sCreateResult = oResponse.getResponseBody();
            		CreateContainerResponse oCreateContainerResponse = JsonUtils.s_oMapper.readValue(sCreateResult,CreateContainerResponse.class);
            		
            		if (oCreateContainerResponse != null) {
            			WasdiLog.debugLog("DockerUtils.run: got the response, ContainerId = " + oCreateContainerResponse.Id);
            			sContainerId = oCreateContainerResponse.Id;
            		}
            		else {
            			WasdiLog.warnLog("DockerUtils.run: impossible to get or transalte the api return, so not container id available");
            		}            		
            	}
            	catch (Exception oEx) {
            		WasdiLog.errorLog("DockerUtils.run exception creating the container: " + oEx.toString());
                    return "";
                }        		
        	}
        	else {        		
        		// If the container exists, we can take the ID
        		WasdiLog.debugLog("DockerUtils.run: Container already found, we will use the id");
        		sContainerId = oContainerInfo.Id;
        	}
            
            WasdiLog.debugLog("DockerUtils.run: Starting Container Named " + sContainerName + " Id " + sContainerId);
            
            // Prepare the url to start it
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		sUrl +="containers/" + sContainerId + "/start";
    		
    		// Make the call
    		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, "");
    		
    		if (oResponse.getResponseCode() == 204) {
    			// Started Well
    			WasdiLog.debugLog("DockerUtils.run: Container " + sContainerName + " started");
    			return sContainerId;
    		}
    		else if (oResponse.getResponseCode() == 304) {
    			// ALready Started (but so why we did not detected this before?!?)
    			WasdiLog.debugLog("DockerUtils.run: Container " + sContainerName + " wasd already started");
    			return sContainerId;
    		}
    		else {
    			// Error!
    			WasdiLog.errorLog("DockerUtils.run: Impossible to start Container " + sContainerName);
    			WasdiLog.errorLog("DockerUtils.run: Message Received " + oResponse.getResponseBody());
    			return "";
    		}
            
        } catch (Exception oEx) {
        	WasdiLog.errorLog("DockerUtils.run error creating the container: " + oEx.toString());
            return "";
        }
    }    
    
    /**
     * Get the logs of a container from the Id
     * @return List of objects as returned by Docker API
     */
    public String getContainerLogsByContainerId(String sContainerId) {
    	try {
    		String sUrl =  WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		sUrl += "containers/"+sContainerId+"/logs?stdout=true&stderr=true";
    		
    		HttpCallResponse oResponse = HttpUtils.httpGet(sUrl);
    		
    		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
    			return "";
    		}
    		else {
    			String sResponseBody = oResponse.getResponseBody();
    			sResponseBody = sResponseBody.replaceAll("[\\x00-\\x09\\x11\\x12\\x14-\\x1F\\x7F]", "");
    			sResponseBody = sResponseBody.replaceAll("[^\\p{ASCII}]", "");
    			
    			return sResponseBody;
    		}    		
    	}
    	catch (Exception oEx) {
            WasdiLog.errorLog("DockerUtils.getContainerLogsByContainerName: exception converting API result " + oEx);
            return "";
        }    	
    }

    
    
    /**
     * Create and Start a container
     * @param sImageName Name of the image
     * @param sImageVersion Version
     * @param asArg Args to be passed as CMD parameter
     * @return The Id of the container if created, empty string in case of problems
     */
    public String createAndStartContainer(String sContainerName, String sImageName, String sImageVersion, List<String> asArg,  List<String> asAdditionalMountPoints, List<String> asExtraHosts,List<EnvironmentVariableConfig> asEnvironmentVariables) {

        try {
        	
        	// We need the name of the image
        	if (Utils.isNullOrEmpty(sImageName)) {
        		WasdiLog.warnLog("DockerUtils.createAndStartContainer: image is null");
        		return "";
        	}        	
        	
            // Attach the version, if available
        	if (!Utils.isNullOrEmpty(sImageVersion))  sImageName += ":" + sImageVersion;
        	if (!Utils.isNullOrEmpty(m_sDockerRegistry)) sImageName = m_sDockerRegistry+"/" + sImageName;
        	
            WasdiLog.debugLog("DockerUtils.createAndStartContainer: try to start a container from image " + sImageName);
            
            // Id of the container
            String sContainerId = "";
        	
            // Search first of all if the container is already here
        	ContainerInfo oContainerInfo = getContainerInfoByContainerName(sContainerName);        	
        	
        	if (oContainerInfo == null) {
        		
        		// No we do not have it
        		WasdiLog.debugLog("DockerUtils.createAndStartContainer: the container is not available, try to create a container named " + sContainerName);
        		
        		// Create the container
            	try {
            		// API URL
            		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
            		if (!sUrl.endsWith("/")) sUrl += "/";
            		sUrl += "containers/create?name=" + sContainerName;
            		
            		// Create the Payload to send to create the container
            		CreateParams oContainerCreateParams = new CreateParams();
            		            		
            		// Set the image
            		oContainerCreateParams.Image = sImageName;            		
            		            		
            		// Set the network mode
            		oContainerCreateParams.HostConfig.NetworkMode = m_sDockerNetworkMode;
            		
            		// Activate tty
            		oContainerCreateParams.Tty = true;
            		
            		if (asAdditionalMountPoints!=null) {
            			for (String sMountPoint : asAdditionalMountPoints) {
            				oContainerCreateParams.HostConfig.Binds.add(sMountPoint);
						}
            		}
            		
            		oContainerCreateParams.HostConfig.RestartPolicy.put("Name", "unless-stopped");
            		
            		if (asArg!=null) oContainerCreateParams.Cmd.addAll(asArg);
            		
                    // Extra hosts mapping, useful for some instances when the server host can't be resolved
                    // The symptoms of such problem is that the POST call from the Docker container timeouts
                    if (asExtraHosts != null) {
                    	if (asExtraHosts.size()>0) {
                    		WasdiLog.debugLog("DockerUtils.createAndStartContainer adding configured extra host mapping to the run arguments");
                        	for (int iExtraHost = 0; iExtraHost<asExtraHosts.size(); iExtraHost ++) {
                        		String sExtraHost = asExtraHosts.get(iExtraHost);
                        		oContainerCreateParams.HostConfig.ExtraHosts.add(sExtraHost);
                        	}
                    	}
                    }
                    
                    if (asEnvironmentVariables != null) {
                		
                		for (EnvironmentVariableConfig oEnvironmentVariable : asEnvironmentVariables) {
                			
                			String sKey = oEnvironmentVariable.key;
                			String sValue = oEnvironmentVariable.value;
                			
                			if (Utils.isNullOrEmpty(sKey)) {
                				WasdiLog.warnLog("DockerUtils.createAndStartContainer: there is an env variable with key null. Jump it");
                				continue;
                			}
                			
                			if (sValue == null) sValue="";
                			
                			String sVariable = sKey + "=" + sValue;
                			
                			WasdiLog.warnLog("DockerUtils.createAndStartContainer: adding env variable: " + sVariable);
                			
                			oContainerCreateParams.Env.add(sVariable);
						}
                	}                    
            		
                    // Convert the payload in JSON (NOTE: is hand-made !!)
            		String sContainerCreateParams = oContainerCreateParams.toJson();
            		
            		if (Utils.isNullOrEmpty(sContainerCreateParams)) {
            			WasdiLog.errorLog("DockerUtils.createAndStartContainer: impossible to get the payload to create the container. We cannot proceed :(");
            			return "";
            		}
            		
            		if (WasdiConfig.Current.dockers.logDockerAPICallsPayload) {
                		WasdiLog.debugLog("DOCKER JSON PAYLOAD DUMP");
                		WasdiLog.debugLog(sContainerCreateParams);
                		WasdiLog.debugLog("------------------");            			
            		}
            		
            		// We need to set the json Content-Type
            		HashMap<String, String> asHeaders = new HashMap<>();
            		asHeaders.put("Content-Type", "application/json");
            		
            		// Is a post!
            		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, sContainerCreateParams, asHeaders);
            		
            		if (oResponse.getResponseCode()<200||oResponse.getResponseCode()>299) {
            			// Here is not good
            			WasdiLog.errorLog("DockerUtils.createAndStartContainer: impossible to create the container. We cannot proceed :(. ERROR = " + oResponse.getResponseBody());
            			return "";
            		}
            		
            		WasdiLog.debugLog("DockerUtils.createAndStartContainer: Container created. Message = " + oResponse.getResponseBody());
            		
            		String sCreateResult = oResponse.getResponseBody();
            		CreateContainerResponse oCreateContainerResponse = JsonUtils.s_oMapper.readValue(sCreateResult,CreateContainerResponse.class);
            		
            		if (oCreateContainerResponse != null) {
            			WasdiLog.debugLog("DockerUtils.createAndStartContainer: got the response, ContainerId = " + oCreateContainerResponse.Id);
            			sContainerId = oCreateContainerResponse.Id;
            		}
            		else {
            			WasdiLog.warnLog("DockerUtils.createAndStartContainer: impossible to get or transalte the api return, so not container id available");
            		}            		
            	}
            	catch (Exception oEx) {
            		WasdiLog.errorLog("DockerUtils.createAndStartContainer exception creating the container: " + oEx.toString());
                    return "";
                }        		
        	}
        	else {        		
        		// If the container exists, we can take the ID
        		WasdiLog.debugLog("DockerUtils.createAndStartContainer: Container already found, we will use the id");
        		sContainerId = oContainerInfo.Id;
        	}
            
            WasdiLog.debugLog("DockerUtils.createAndStartContainer: Starting Container Named " + sContainerName + " Id " + sContainerId);
            
            // Prepare the url to start it
    		String sUrl = WasdiConfig.Current.dockers.internalDockerAPIAddress;
    		if (!sUrl.endsWith("/")) sUrl += "/";
    		sUrl +="containers/" + sContainerId + "/start";
    		
    		// Make the call
    		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, "");
    		
    		if (oResponse.getResponseCode() == 204) {
    			// Started Well
    			WasdiLog.debugLog("DockerUtils.createAndStartContainer: Container " + sContainerName + " started");
    			return sContainerId;
    		}
    		else if (oResponse.getResponseCode() == 304) {
    			// ALready Started (but so why we did not detected this before?!?)
    			WasdiLog.debugLog("DockerUtils.createAndStartContainer: Container " + sContainerName + " wasd already started");
    			return sContainerId;
    		}
    		else {
    			// Error!
    			WasdiLog.errorLog("DockerUtils.createAndStartContainer: Impossible to start Container " + sContainerName);
    			WasdiLog.errorLog("DockerUtils.createAndStartContainer: Message Received " + oResponse.getResponseBody());
    			return "";
    		}
            
        } catch (Exception oEx) {
        	WasdiLog.errorLog("DockerUtils.createAndStartContainer error creating the container: " + oEx.toString());
            return "";
        }
    }
        
}
