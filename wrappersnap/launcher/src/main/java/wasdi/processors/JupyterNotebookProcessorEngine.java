package wasdi.processors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import wasdi.LauncherMain;
import wasdi.shared.business.JupyterNotebook;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.JupyterNotebookRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.jinja.JinjaTemplateRenderer;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;

public class JupyterNotebookProcessorEngine extends DockerProcessorEngine {

	private static final String s_sLINE_SEPARATOR = System.getProperty("line.separator");

	public JupyterNotebookProcessorEngine() {
		super();
	}
	
	@Override
	protected IPackageManager getPackageManager(String sUrl) {
		throw new UnsupportedOperationException("The functionality is not yet implemented for this processor engine!");
	}

	/**
	 * Creates and starts a new Jupyter notebook
	 * @param oParameter
	 * @return
	 */
	public boolean launchJupyterNotebook(ProcessorParameter oParameter) {
		WasdiLog.debugLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: start");

		// Check the domain
		if (oParameter == null) {
			WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: oParameter is null");
			return false;
		}

		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		ProcessWorkspace oProcessWorkspace = m_oProcessWorkspace;

		String sProcessorName = "jupyter-notebook";

		try {
			processWorkspaceLog("Creating JupyterLab on this workspace");
			
			// Take reference to the folder of the notebooks "processor" where all the dockers are deployed
			String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);
			String sProcessorTemplateFolder = getProcessorTemplateFolder(sProcessorName);

			// check if the processors/jupyter-notebook template and processor exists
			boolean bProcessorTemplateFolderExists = WasdiFileUtils.fileExists(sProcessorTemplateFolder);

			if (!bProcessorTemplateFolderExists) {
				WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: the ProcessorTemplateFolder does not exist: " + sProcessorTemplateFolder);
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;
			}

			processWorkspaceLog("Copy template directory");

			boolean bProcessorFolderExists = WasdiFileUtils.fileExists(sProcessorFolder);

			if (bProcessorFolderExists) {
				
				// Check if nothing changed in the templates
				
				String sGeneralCommonEnvTemplateFilePath = getProcessorTemplateGeneralCommonEnvFilePath(sProcessorName);
				WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: GeneralCommonEnvTemplate: " + sGeneralCommonEnvTemplateFilePath);
				File oGeneralCommonEnvTemplate = new File(sGeneralCommonEnvTemplateFilePath);

				String sGeneralCommonEnvProcessorFilePath = getProcessorGeneralCommonEnvFilePath(sProcessorName);
				WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: GeneralCommonEnvProcessor: " + sGeneralCommonEnvProcessorFilePath);
				File oGeneralCommonEnvProcessor = new File(sGeneralCommonEnvProcessorFilePath);


				if (WasdiFileUtils.fileExists(oGeneralCommonEnvTemplate)) {
					if (!WasdiFileUtils.filesAreTheSame(oGeneralCommonEnvTemplate, oGeneralCommonEnvProcessor)) {
						// Files are different: we will need to re-create
						WasdiFileUtils.deleteFile(sProcessorFolder);
						bProcessorFolderExists = WasdiFileUtils.fileExists(sProcessorFolder);
					}
				}

			}

			if (!bProcessorFolderExists) {
				// Copy Docker template files in the processor folder
				File oDockerTemplateFolder = new File(sProcessorTemplateFolder);
				File oProcessorFolder = new File(sProcessorFolder);

				WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: creating the ProcessorFolder: " + sProcessorFolder);
				FileUtils.copyDirectory(oDockerTemplateFolder, oProcessorFolder);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 10);
			
			// Check if our load balancer is up and running
			processWorkspaceLog("verify that the Traefik container is up-and-running");
			
			String sTraefikDockerContainerInspectCommand = buildCommandDockerContainerInspect("traefik-notebook");
			boolean bTraefikDockerContainerInspectResult = RunTimeUtils.runCommand(sProcessorFolder, sTraefikDockerContainerInspectCommand);

			if (!bTraefikDockerContainerInspectResult) {
				
				// No!! Time to wake-up it
				
				WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: the Traefik container is down or missing:" + s_sLINE_SEPARATOR + sTraefikDockerContainerInspectCommand);

				processWorkspaceLog("starting the Traefik container");

				String sDockerComposeUpTraefikCommand = buildCommandDockerComposeUpTraefik();
				boolean bDockerComposeUpTraefikResult = RunTimeUtils.runCommand(sProcessorFolder, sDockerComposeUpTraefikCommand);

				if (!bDockerComposeUpTraefikResult) {
					WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: start Traefik command failed:" + s_sLINE_SEPARATOR + sDockerComposeUpTraefikCommand);

					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
					return false;
				}

				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 20);
			}
			
			// We build our new docker
			processWorkspaceLog("execute command: docker build");
			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: starting docker build");
			String sDockerBuildCommand = buildCommandDockerBuild(sProcessorFolder);
			boolean bDockerBuildResult = RunTimeUtils.runCommand(sProcessorFolder,sDockerBuildCommand);

			if (!bDockerBuildResult) {
				WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker build command failed:" + s_sLINE_SEPARATOR + sDockerBuildCommand);

				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 30);
			
			// This step will let WASDI push in a docker repository if (when) we will install one.
			processWorkspaceLog("execute command: docker push");
			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: docker push");

			String sDockerPushCommand = buildCommandDockerPush(sProcessorFolder);
			boolean bDockerPushResult = RunTimeUtils.runCommand(sProcessorFolder,sDockerPushCommand);

			if (!bDockerPushResult) {
				WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker push command failed:" + s_sLINE_SEPARATOR + sDockerPushCommand);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 40);


			// use the template to create a docker-compose file
			// from: docker-compose.yml.j2
			// to:   docker-compose.yml

			processWorkspaceLog("create docker-compose file");
			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: creating docker compose file");

			// Generate the Jupyter Code
			String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(oParameter.getWorkspaceOwnerId(), oParameter.getWorkspace());
			
			processWorkspaceLog("Notebook id: " + sJupyterNotebookCode);
			
			// Docker compose Template file
			String sDockerComposeYmlTemplateFileName = sProcessorFolder + "docker-compose.yml.j2";
			// Docker compose output file
			String sDockerComposeYmlFileName = sProcessorFolder + "docker-compose_" + sJupyterNotebookCode + ".yml";			
			
			// Utility to render Jinja templates
			JinjaTemplateRenderer oJinjaTemplateRenderer = new JinjaTemplateRenderer();

			// Fill the template params to create the 
			Map<String, Object> aoDockerComposeTemplateParams = new HashMap<String, Object>();
			
			// Add the paramters to the dictionary
			aoDockerComposeTemplateParams.put("wasdiNotebookId", sJupyterNotebookCode);
			aoDockerComposeTemplateParams.put("wasdiUserName", oParameter.getWorkspaceOwnerId());
			aoDockerComposeTemplateParams.put("wasdiWorkspaceId", oParameter.getWorkspace());
			
            if (WasdiConfig.Current.dockers.extraHosts != null) {            	
            	if (WasdiConfig.Current.dockers.extraHosts.size()>0) {
            		aoDockerComposeTemplateParams.put("extraHosts", WasdiConfig.Current.dockers.extraHosts);
            	}
            }            
            
            String sJsonParams = JsonUtils.stringify(aoDockerComposeTemplateParams);
            
            // Translate the template
			oJinjaTemplateRenderer.translate(sDockerComposeYmlTemplateFileName, sDockerComposeYmlFileName, sJsonParams);

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 60);

			// mkdir **notebook** in the Workspace folder: it will be mounted on the docker that hosts the users' notebook
			String sWorkspacePath = PathsConfig.getWorkspacePath(oParameter);

			String sNotebookPath = sWorkspacePath + "notebook";
			
			File oNotebookFolder = new File(sNotebookPath);

			if (!WasdiFileUtils.fileExists(oNotebookFolder)) {
				FileUtils.forceMkdir(oNotebookFolder);
			}

			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: the " + sNotebookPath + " folder was" + (WasdiFileUtils.fileExists(oNotebookFolder) ? " " : " not ") + "created");

			processWorkspaceLog("execute command: docker-compose up");

			// Launch docker-compose up command
			String sDockerComposeUpCommand = buildCommandDockerComposeUpJupyterNotebook(sProcessorFolder, sJupyterNotebookCode);
			boolean bDockerComposeUpResult = RunTimeUtils.runCommand(sProcessorFolder,sDockerComposeUpCommand);

			if (!bDockerComposeUpResult) {
				WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker compose up command failed:" + s_sLINE_SEPARATOR + sDockerComposeUpCommand);
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 80);

			processWorkspaceLog("adding file: notebook_config.cfg");

			String sContainerName = "nb_" + sJupyterNotebookCode;
			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: sContainerName: " + sContainerName);


			// create the JSON content of the waspy configuration file to let the lib know the workspace where it works
			String sJsonContent = "{\"WORKSPACEID\":\"" + oParameter.getWorkspace() + "\",\"UPLOADACTIVE\":false}";
			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: sJsonContent: " + sJsonContent);

			// create the file on the workspace folder that will be mounted by docker
			String sWaspyNotebookConfigFileFullPath = PathsConfig.getWorkspacePath(oParameter) + "notebook/notebook_config.cfg";
			WasdiLog.infoLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: sWaspyNotebookConfigFileFullPath: " + sWaspyNotebookConfigFileFullPath);
			WasdiFileUtils.writeFile(sJsonContent, sWaspyNotebookConfigFileFullPath);
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 90);

			processWorkspaceLog("execute command: create traefik configuration");
			
			String sVolumeFolder = WasdiConfig.Current.paths.traefikMountedVolume;
			if (!sVolumeFolder.endsWith("/")) sVolumeFolder += "/";
			
			String sTemplateFile = sVolumeFolder + "template/conf.d_notebook.yml.j2";
			String sOutputFile = sVolumeFolder + "conf.d/nb_" + sJupyterNotebookCode + ".yml";
			
			Map<String, Object> aoTraefikTemplateParams = new HashMap<>();
			
			aoTraefikTemplateParams.put("wasdiNotebookId", sJupyterNotebookCode);
			
			
			JupyterNotebookRepository oJupyterNotebookRepository = new JupyterNotebookRepository();
			JupyterNotebook oNotebook = oJupyterNotebookRepository.getJupyterNotebook(sJupyterNotebookCode);
			
			if (oNotebook!=null) {
				ArrayList<String> asWhiteIp = oNotebook.extractListOfWhiteIp();
				
				if (WasdiConfig.Current.traefik.firewallWhiteList!=null) {
					asWhiteIp.addAll(WasdiConfig.Current.traefik.firewallWhiteList);
				}
				
				aoTraefikTemplateParams.put("sourceRangeList", asWhiteIp);
			}
			
			String sJSON = JsonUtils.stringify(aoTraefikTemplateParams);
			
			oJinjaTemplateRenderer.translate(sTemplateFile, sOutputFile, sJSON);
			
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);

			processWorkspaceLog(new EndMessageProvider().getGood());

			return true;
		} catch (Exception oEx) {
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

		String sProcessorName = "jupyter-notebook";

		try {
			processWorkspaceLog("Start termination of the notebook in the workspace " + oParameter.getWorkspace());

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);

			String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(oParameter.getWorkspaceOwnerId(), oParameter.getWorkspace());


			// delete the Traefik configuration file
			// rm -f /data/wasdi/container/volume/traefik-notebook/etc_traefik/conf.d/nb_<notebook ID>
			
			String sTraefikConfigurationFilePath = "/data/wasdi/container/volume/traefik-notebook/etc_traefik/conf.d/nb_" + sJupyterNotebookCode + ".yml";
			File oTraefikConfigurationFile = new File(sTraefikConfigurationFilePath);

			if (WasdiFileUtils.fileExists(oTraefikConfigurationFile)) {
				FileUtils.deleteQuietly(oTraefikConfigurationFile);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 25);


			// stop the container instance
			// docker-compose [--file </path/to/the/docker-compose/file.yml] stop
			processWorkspaceLog("Stop docker");

			// Launch docker-compose stop command
			String sDockerComposeStopCommand = buildCommandDockerComposeStopJupyterNotebook(sProcessorFolder, sJupyterNotebookCode);
			boolean bDockerComposeStopResult = RunTimeUtils.runCommand(sProcessorFolder, sDockerComposeStopCommand);

			if (!bDockerComposeStopResult) {
				WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker compose stop command failed:" + s_sLINE_SEPARATOR + sDockerComposeStopCommand);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 50);

			// delete the container instance
			// docker-compose [--file </path/to/the/docker-compose/file.yml] rm
			

			processWorkspaceLog("Remove docker");

			// Launch docker-compose rm command
			String sDockerComposeRmCommand = buildCommandDockerComposeRmJupyterNotebook(sProcessorFolder, sJupyterNotebookCode);
			boolean bDockerComposeRmResult = RunTimeUtils.runCommand(sProcessorFolder, sDockerComposeRmCommand);

			if (!bDockerComposeRmResult) {
				WasdiLog.errorLog("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker compose rm command failed:" + s_sLINE_SEPARATOR + sDockerComposeRmCommand);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 75);

			// Delete the Jupyter Notebook docker-compose file
			// rm -f /data/wasdi/processors/jupyter-notebook/docker-compose_<notebook ID>.yml

			String sDockerComposeFilePath = PathsConfig.getProcessorFolder(sProcessorName) + "docker-compose_" + sJupyterNotebookCode + ".yml";
			File oDockerComposeFile = new File(sDockerComposeFilePath);

			if (WasdiFileUtils.fileExists(oDockerComposeFile)) {
				FileUtils.deleteQuietly(oDockerComposeFile);
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

	private static String buildCommandDockerBuild(String sProcessorFolder) {
		return "bash " + sProcessorFolder + "tool" + s_sFILE_SEPARATOR + "dockerBuild.sh";
	}

	private static String buildCommandDockerPush(String sProcessorFolder) {
		return "bash " + sProcessorFolder + "tool" + s_sFILE_SEPARATOR + "dockerPush.sh";
	}

	private static String buildCommandDockerComposeUpJupyterNotebook(String sProcessorFolder, String sJupyterNotebookCode) {
		StringBuilder oSB = new StringBuilder();

		
		oSB.append(WasdiConfig.Current.dockers.dockerComposeCommand + " \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    --project-name " + sJupyterNotebookCode + "\\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    --file " + sProcessorFolder + "docker-compose_" + sJupyterNotebookCode + ".yml \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    --env-file " + sProcessorFolder + "var" + s_sFILE_SEPARATOR + "general_common.env \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    up \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    --detach");

		return oSB.toString();
	}

	// docker-compose [--file </path/to/the/docker-compose/file.yml] stop
	private static String buildCommandDockerComposeStopJupyterNotebook(String sProcessorFolder, String sJupyterNotebookCode) {
		StringBuilder oSB = new StringBuilder();

		oSB.append(WasdiConfig.Current.dockers.dockerComposeCommand + " \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    --project-name " + sJupyterNotebookCode + "\\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    --file " + sProcessorFolder + "docker-compose_" + sJupyterNotebookCode + ".yml \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    --env-file " + sProcessorFolder + "var" + s_sFILE_SEPARATOR + "general_common.env \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    stop");

		return oSB.toString();
	}

	// docker-compose [--file </path/to/the/docker-compose/file.yml] rm
	private static String buildCommandDockerComposeRmJupyterNotebook(String sProcessorFolder, String sJupyterNotebookCode) {
		StringBuilder oSB = new StringBuilder();

		oSB.append(WasdiConfig.Current.dockers.dockerComposeCommand + " \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    --project-name " + sJupyterNotebookCode + "\\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    --file " + sProcessorFolder + "docker-compose_" + sJupyterNotebookCode + ".yml \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    --env-file " + sProcessorFolder + "var" + s_sFILE_SEPARATOR + "general_common.env \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    rm \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    --stop \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    --force");

		return oSB.toString();
	}

	private static String buildCommandTraefikConfigurationFile(String sProcessorFolder, String sJupyterNotebookCode) {
		StringBuilder oSB = new StringBuilder();

		oSB.append("/usr/bin/python3 \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("  -B \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("  /opt/companyExploitation/common/tool/toolbox/sysadmin/code/toolbox.py \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("  --configuration-directory /opt/companyExploitation/common/tool/toolbox/sysadmin/configuration/ \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("  --module jinja2 \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("  --submodule renderTemplate \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("  --template /data/wasdi/container/volume/traefik-notebook/etc_traefik/template/conf.d_notebook.yml.j2 \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("  --rendered-file /data/wasdi/container/volume/traefik-notebook/etc_traefik/conf.d/nb_" + sJupyterNotebookCode + ".yml \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("  --json-inline '{\"wasdiNotebookId\": \"" + sJupyterNotebookCode + "\"}' \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("  --strict");

		return oSB.toString();
	}

	private static String buildCommandDockerContainerInspect(String sContainerName) {
		StringBuilder oSB = new StringBuilder();

		oSB.append("docker container inspect --format '{{.State.Running}}' ");
		oSB.append(sContainerName);

		return oSB.toString();
	}

	private static String buildCommandDockerCpFile(String sContainerName, String sTemporaryFileFullPath) {
		StringBuilder oSB = new StringBuilder();

		oSB.append("docker cp ");
		oSB.append(sTemporaryFileFullPath);
		oSB.append(" ");
		oSB.append(sContainerName);
		oSB.append(":/home/wasdi/notebook/notebook_config.cfg");

		return oSB.toString();
	}

	// docker-compose --file /data/wasdi/processors/traefik-notebook/docker-compose.yml up --detach
	private static String buildCommandDockerComposeUpTraefik() {
		StringBuilder oSB = new StringBuilder();

		oSB.append(WasdiConfig.Current.dockers.dockerComposeCommand + " \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    --file /data/wasdi/processors/traefik-notebook/docker-compose.yml \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    up \\");
		oSB.append(s_sLINE_SEPARATOR);
		oSB.append("    --detach");

		return oSB.toString();
	}

}
