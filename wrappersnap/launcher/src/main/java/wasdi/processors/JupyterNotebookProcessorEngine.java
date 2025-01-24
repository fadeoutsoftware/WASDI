package wasdi.processors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import wasdi.LauncherMain;
import wasdi.shared.business.JupyterNotebook;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.S3Volume;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.config.DockerRegistryConfig;
import wasdi.shared.config.EnvironmentVariableConfig;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.ProcessorTypeConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.JupyterNotebookRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.docker.DockerUtils;
import wasdi.shared.utils.docker.containersViewModels.ContainerInfo;
import wasdi.shared.utils.jinja.JinjaTemplateRenderer;
import wasdi.shared.utils.log.WasdiLog;

public class JupyterNotebookProcessorEngine extends DockerProcessorEngine {

	private static final String s_sLINE_SEPARATOR = System.getProperty("line.separator");

	public JupyterNotebookProcessorEngine() {
		super();
	}
	
	@Override
	protected IPackageManager getPackageManager(String sUrl) {
		WasdiLog.warnLog("JupyterNotebookProcessorEngine.getPackageManager: Package Manager Not available for Jupyter Notebook Engine");
		return null;
	}

	/**
	 * Creates and starts a new Jupyter notebook
	 * @param oParameter
	 * @return
	 */
	public boolean launchJupyterNotebook(ProcessorParameter oParameter) {
		
		WasdiLog.debugLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: start");

		// Check the parameter
		if (oParameter == null) {
			WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: oParameter is null");
			return false;
		}

		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		ProcessWorkspace oProcessWorkspace = m_oProcessWorkspace;
		
		// Fixed Processor Name in this case
		String sProcessorName = ProcessorTypes.JUPYTER_NOTEBOOK;
		
		// Take reference to the folder of the local processor
		String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);
		// And of the template folder
		String sProcessorTemplateFolder = getProcessorTemplateFolder(sProcessorName);		

		try {
			processWorkspaceLog("Creating JupyterLab on this workspace");
			
			// Get the processor Type Config
			ProcessorTypeConfig oProcessorTypeConfig = WasdiConfig.Current.dockers.getProcessorTypeConfig(sProcessorName);
			
			if (oProcessorTypeConfig == null) {
				WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: impossible to find the " + sProcessorName + " config");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;		
			}
			
			WasdiLog.debugLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: check if the jupyter image exists: name = " + oProcessorTypeConfig.image + " version = " + oProcessorTypeConfig.version);
			
			// Create the Docker Utils Object that will be used to build and create containers
			DockerUtils oDockerUtils = new DockerUtils();
			
			// We do not need to start after the build
			m_bRunAfterDeploy = false;
			// And we work with our main register
			m_sDockerRegistry = getDockerRegisterAddress();
			
			// Create the "fake" processor Entity
			Processor oNotebookFakeProcessor = new Processor();
			oNotebookFakeProcessor.setName(oProcessorTypeConfig.image);
			oNotebookFakeProcessor.setVersion(oProcessorTypeConfig.version);			
			
			// Initialize Docker Utils
			oDockerUtils.setProcessor(oNotebookFakeProcessor);
			oDockerUtils.setWasdiSystemUserName(WasdiConfig.Current.systemUserName);
			oDockerUtils.setWasdiSystemGroupName(WasdiConfig.Current.systemGroupName);
			oDockerUtils.setWasdiSystemGroupId(WasdiConfig.Current.systemGroupId);
			oDockerUtils.setWasdiSystemUserId(WasdiConfig.Current.systemUserId);
			oDockerUtils.setProcessorFolder(sProcessorFolder);
			oDockerUtils.setDockerRegistry(m_sDockerRegistry);
			oDockerUtils.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
			
			// Get back the full Docker Register Config entity
			DockerRegistryConfig oDockerRegistryConfig = WasdiConfig.Current.dockers.getRegisterByAddress(m_sDockerRegistry);
			
			// That must exists
			if (oDockerRegistryConfig == null) {
				WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: impossible to find the registry config for " + m_sDockerRegistry);
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;						
			}
			
			// Login in the registry
			String sRegistryToken = oDockerUtils.loginInRegistry(oDockerRegistryConfig);
			
			if (Utils.isNullOrEmpty(sRegistryToken)) {
				WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: impossible to login in the registry");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;										
			}
			
			WasdiLog.debugLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: logged in the registry, check the processor folder");
		
			processWorkspaceLog("Copy template directory");

			// We will copy in any case so we are sure to use always the most updated one
			// Copy Docker template files in the processor folder
			File oDockerTemplateFolder = new File(sProcessorTemplateFolder);
			File oProcessorFolder = new File(sProcessorFolder);
			
			// Check and or create the processor folder
			if (!oProcessorFolder.exists()) {
				oProcessorFolder.mkdirs();
			}			

			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: Updating the ProcessorFolder: " + sProcessorFolder + " with the last template");
			FileUtils.copyDirectory(oDockerTemplateFolder, oProcessorFolder);
			
			// mkdir **notebook** in the Workspace folder: it will be mounted on the docker that hosts the users' notebook
			String sWorkspacePath = PathsConfig.getWorkspacePath(oParameter);
			String sNotebookPath = sWorkspacePath + "notebook";
			
			File oNotebookFolder = new File(sNotebookPath);

			if (!WasdiFileUtils.fileExists(oNotebookFolder)) {
				FileUtils.forceMkdir(oNotebookFolder);
			}

			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: the " + sNotebookPath + " folder was" + (WasdiFileUtils.fileExists(oNotebookFolder) ? " " : " not ") + "created");			
			
			WasdiLog.debugLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: search if the image is available");
			
			// is the image available?
			boolean bImageAvailable = oDockerUtils.isImageAvailable(oProcessorTypeConfig.image, oProcessorTypeConfig.version);
			
			if (!bImageAvailable) {
				// We have no local image. But maybe is available on the registry
				WasdiLog.debugLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: image not available, can we pull it?");
				
				// Prepare the full name of the image
				String sFullImageName = m_sDockerRegistry + "/wasdi/" + oProcessorTypeConfig.image + ":" + oProcessorTypeConfig.version;
				
				// Try to pull
				boolean bPullDone = oDockerUtils.pull(sFullImageName, sRegistryToken);
				
				if (!bPullDone) {
					
					WasdiLog.debugLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: Impossible to pull the image. We need to build");
					
					// check if the jupyter-notebook template folder exists
					boolean bProcessorTemplateFolderExists = WasdiFileUtils.fileExists(sProcessorTemplateFolder);

					if (!bProcessorTemplateFolderExists) {
						WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: the ProcessorTemplateFolder does not exist: " + sProcessorTemplateFolder);
						LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
						return false;
					}

					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 10);
					
					// We build our new docker
					processWorkspaceLog("Building Notebook Docker");
					WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: starting docker build");
													
					// Trigger the build
					String sDockerImageName = oDockerUtils.build();
					
					if (Utils.isNullOrEmpty(sDockerImageName)) {
						WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker build command failed");
						LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
						return false;
					}

					WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: got built image name " + sDockerImageName);
					
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 30);
					
					// This step will let WASDI push in a docker repository
					processWorkspaceLog("Push new image to the registers");
					WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: pushing image in registers");
					
					// Save the name of the image to push
					m_sDockerImageName = sDockerImageName;
					
					// Here we save the address of the image
					String sPushedImageAddress = pushImageInRegisters(oNotebookFakeProcessor);
					
					if (Utils.isNullOrEmpty(sPushedImageAddress)) {
						WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: Impossible to push the image.");
						return false;
					}
					
					WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: pushed image in registers = " + sPushedImageAddress);					
				}
				else {
					WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: image pulled = " + sFullImageName);
					m_sDockerImageName = sFullImageName;					
				}
			}
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 40);
			
			// Generate the Jupyter Code
			String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(oParameter.getWorkspaceOwnerId(), oParameter.getWorkspace());
			
			processWorkspaceLog("Creating the container for Notebook id: " + sJupyterNotebookCode);
			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: Creating the container for Notebook id: " + sJupyterNotebookCode);
			
			// Set the container name
			String sContainerName = "nb_" + sJupyterNotebookCode;
			
			// Create a copy of the mount points
			ArrayList<String> asAdditionalMountPointsCopy = new ArrayList<>();
			
			for (String sMountPoint : oProcessorTypeConfig.additionalMountPoints) {
				asAdditionalMountPointsCopy.add(sMountPoint);
			}
			
			// Add the folder fo the workspace
			String sWorkspaceFolderMount = PathsConfig.getWorkspacePath(oParameter);
			String sContainerWorkspacePath = PathsConfig.getWorkspacePath(oParameter, "/home/" + WasdiConfig.Current.systemUserName + "/.wasdi/");
			sWorkspaceFolderMount = sWorkspaceFolderMount + ":" + sContainerWorkspacePath;
			
			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: adding mount folder " + sWorkspaceFolderMount);
			asAdditionalMountPointsCopy.add(sWorkspaceFolderMount);
			
    		List<S3Volume> aoVolumesToMount = PermissionsUtils.getVolumesToMount(oParameter);
    		
    		if (aoVolumesToMount != null) {
    			if (aoVolumesToMount.size()>0) {
    				WasdiLog.debugLog("DockerUtils.start: Found " + aoVolumesToMount.size() + " S3 Volumes to mount");
    				
    				for (S3Volume oS3Volume : aoVolumesToMount) {
    					
    					String sRemoteVolume = sContainerWorkspacePath;
    					
    					if (!sRemoteVolume.endsWith( "/")) sRemoteVolume+= "/";
    					
						String sMountingVolume = PathsConfig.getS3VolumesBasePath()+oS3Volume.getMountingFolderName() + ":" + sRemoteVolume + oS3Volume.getMountingFolderName();
						
						if (oS3Volume.isReadOnly()) {
							sMountingVolume += ":ro";
						}
						
						WasdiLog.debugLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: Adding Volume " + sMountingVolume);
						asAdditionalMountPointsCopy.add(sMountingVolume);
					}
    			}
    		}
			// Add the folder of the notebook
			String sWorkspaceNotebookFolderMount = PathsConfig.getWorkspacePath(oParameter) + "notebook";
			String sContainerNotebookFolderPath = "/home/"  + WasdiConfig.Current.systemUserName + "/notebook";
			sWorkspaceNotebookFolderMount = sWorkspaceNotebookFolderMount +":" + sContainerNotebookFolderPath;
			
			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: adding mount folder " + sWorkspaceNotebookFolderMount);
			asAdditionalMountPointsCopy.add(sWorkspaceNotebookFolderMount);
			
			// Create a copy of the environment variables
			ArrayList<EnvironmentVariableConfig> aoEnvironmentVariablesCopy = new ArrayList<>();
			
			for (EnvironmentVariableConfig oEnv : oProcessorTypeConfig.environmentVariables) {
				EnvironmentVariableConfig oCopy = new EnvironmentVariableConfig();
				oCopy.key = oEnv.key;
				oCopy.value = oEnv.value;
				aoEnvironmentVariablesCopy.add(oCopy);
			}
			
			// Add the notebook id variable
			EnvironmentVariableConfig oEnvNotebookId = new EnvironmentVariableConfig();
			oEnvNotebookId.key = "sWasdiNotebookId";
			oEnvNotebookId.value = sJupyterNotebookCode;
			aoEnvironmentVariablesCopy.add(oEnvNotebookId);
			
			// Create and start the container
			String sContainerId = oDockerUtils.createAndStartContainer(sContainerName, "wasdi/" + oProcessorTypeConfig.image, oProcessorTypeConfig.version, oProcessorTypeConfig.commands, asAdditionalMountPointsCopy, oProcessorTypeConfig.extraHosts, aoEnvironmentVariablesCopy);
			
			if (Utils.isNullOrEmpty(sContainerId)) {
				WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker createAndStartContainer command failed");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;
			}
			
			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: obtained container id " + sContainerId);			

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 60);
			
			processWorkspaceLog("Adding notebook configuration file");
			
			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: adding notebook config");
			
			// create the JSON content of the waspy configuration file to let the lib know the workspace where it works
			String sJsonContent = "{\"WORKSPACEID\":\"" + oParameter.getWorkspace() + "\",\"UPLOADACTIVE\":false}";
			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: sJsonContent: " + sJsonContent);

			// create the file on the workspace folder that will be mounted by docker
			String sWaspyNotebookConfigFileFullPath = PathsConfig.getWorkspacePath(oParameter) + "notebook/notebook_config.cfg";
			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: sWaspyNotebookConfigFileFullPath: " + sWaspyNotebookConfigFileFullPath);
			WasdiFileUtils.writeFile(sJsonContent, sWaspyNotebookConfigFileFullPath);
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 90);

			processWorkspaceLog("Creating traefik configuration");
			
			String sVolumeFolder = WasdiConfig.Current.paths.traefikMountedVolume;
			if (!sVolumeFolder.endsWith("/")) sVolumeFolder += "/";
			
			String sTemplateFile = sProcessorFolder + "traefik_notebook.yml.j2";
			String sOutputFile = sVolumeFolder + "nb_" + sJupyterNotebookCode + ".yml";
			
			Map<String, Object> aoTraefikTemplateParams = new HashMap<>();
			
			aoTraefikTemplateParams.put("sWasdiJupyterNotebookId", sJupyterNotebookCode);
			
			JupyterNotebookRepository oJupyterNotebookRepository = new JupyterNotebookRepository();
			JupyterNotebook oNotebook = oJupyterNotebookRepository.getJupyterNotebook(sJupyterNotebookCode);
			
			if (oNotebook!=null) {
				ArrayList<String> asWhiteIp = oNotebook.extractListOfWhiteIp();
				
				if (WasdiConfig.Current.traefik.firewallWhiteList!=null) {
					asWhiteIp.addAll(WasdiConfig.Current.traefik.firewallWhiteList);
				}
				
				aoTraefikTemplateParams.put("aWasdiJupyterNotebookFirewallAllowedIps", asWhiteIp);
			}
			
			String sJSON = JsonUtils.stringify(aoTraefikTemplateParams);
			
			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: creating traefik config ");
			
			JinjaTemplateRenderer oJinjaTemplateRenderer = new JinjaTemplateRenderer(); 
			oJinjaTemplateRenderer.translate(sTemplateFile, sOutputFile, sJSON);
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

			processWorkspaceLog(new EndMessageProvider().getGood());

			return true;
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook Exception", oEx);

			return false;
		}
	}

	public boolean terminateJupyterNotebook(ProcessorParameter oParameter) {
		WasdiLog.debugLog("JupyterNotebookProcessorEngine.terminateJupyterNotebook: start");

		if (oParameter == null) {
			WasdiLog.errorLog("JupyterNotebookProcessorEngine.terminateJupyterNotebook: oParameter is null");
			return false;
		}

		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		ProcessWorkspace oProcessWorkspace = m_oProcessWorkspace;

		try {
			processWorkspaceLog("Start termination of the notebook in the workspace " + oParameter.getWorkspace());

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(oParameter.getWorkspaceOwnerId(), oParameter.getWorkspace());

			WasdiLog.debugLog("JupyterNotebookProcessorEngine.terminateJupyterNotebook: terminate notebook with code " + sJupyterNotebookCode);

			// Delete the Traefik configuration file
			String sVolumeFolder = WasdiConfig.Current.paths.traefikMountedVolume;
			if (!sVolumeFolder.endsWith("/")) sVolumeFolder += "/";
			
			String sTraefikConfigurationFilePath = sVolumeFolder + "nb_" + sJupyterNotebookCode + ".yml";
			
			File oTraefikConfigurationFile = new File(sTraefikConfigurationFilePath);

			if (WasdiFileUtils.fileExists(oTraefikConfigurationFile)) {
				FileUtils.deleteQuietly(oTraefikConfigurationFile);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 25);


			// stop the container instance
			// docker-compose [--file </path/to/the/docker-compose/file.yml] stop
			processWorkspaceLog("Stop docker");
			DockerUtils oDockerUtils = new DockerUtils();
			oDockerUtils.setProcessWorkspaceLogger(m_oProcessWorkspaceLogger);
			
			ContainerInfo oContainerInfo = oDockerUtils.getContainerInfoByContainerName("nb_" + sJupyterNotebookCode);
			
			if (oContainerInfo != null) {
				WasdiLog.debugLog("JupyterNotebookProcessorEngine.terminateJupyterNotebook: stopping container ");
				oDockerUtils.stop(oContainerInfo.Id);
				oDockerUtils.removeContainer(oContainerInfo.Id);
			}
			else {
				WasdiLog.debugLog("JupyterNotebookProcessorEngine.terminateJupyterNotebook: container not found");
			}
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

			processWorkspaceLog(new EndMessageProvider().getGood());

			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("JupyterNotebookProcessorEngine.terminateJupyterNotebook Exception", oEx);

			try {
				if (oProcessWorkspace != null) {
					// Check and set the operation end-date
					if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
						oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
					}

					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				}
			} catch (Exception e) {
				WasdiLog.errorLog("JupyterNotebookProcessorEngine.terminateJupyterNotebook Exception", e);
			}

			return false;
		}

	}


}
