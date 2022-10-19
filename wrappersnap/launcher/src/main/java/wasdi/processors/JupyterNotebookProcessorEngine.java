package wasdi.processors;

import java.io.File;
import java.util.Date;

import org.apache.commons.io.FileUtils;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.managers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;

public class JupyterNotebookProcessorEngine extends DockerProcessorEngine {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	public JupyterNotebookProcessorEngine() {
		super();
	}

	public JupyterNotebookProcessorEngine(String sWorkingRootPath, String sDockerTemplatePath, String sTomcatUser) {
		super(sWorkingRootPath,sDockerTemplatePath, sTomcatUser);
	}

	@Override
	protected IPackageManager getPackageManager(String sIp, int iPort) {
		throw new UnsupportedOperationException("The functionality is not yet implemented for this processor engine!");
	}

	public boolean launchJupyterNotebook(ProcessorParameter oParameter) {
		LauncherMain.s_oLogger.debug("JupyterNotebookProcessorEngine.launchJupyterNotebook: start");

		if (oParameter == null) {
			LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: oParameter is null");
			return false;
		}

		ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
		ProcessWorkspace oProcessWorkspace = null;

		String sProcessorName = oParameter.getName();

		try {
			processWorkspaceLog("Start launch of " + sProcessorName + " Type " + oParameter.getProcessorType());

			oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oProcessWorkspace = m_oProcessWorkspace;

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
			processWorkspaceLog("This is an attempt to launch of the Jupyter Notebook");


			String sProcessorFolder = getProcessorFolder(sProcessorName);
			String sProcessorTemplateFolder = getProcessorTemplateFolder(sProcessorName);

			// check if the processors/jupyter-notebook exists
			// make a copy of the template directory (in case it does not exist)
			// from: /data/wasdi/dockertemplate/jupyter-notebook/
			// to:   /data/wasdi/processors/jupyter-notebook

			boolean bProcessorTemplateFolderExists = WasdiFileUtils.fileExists(sProcessorTemplateFolder);

			if (!bProcessorTemplateFolderExists) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: the ProcessorTemplateFolder does not exist: " + sProcessorTemplateFolder);

				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;
			}

			processWorkspaceLog("copy template directory");

			boolean bProcessorFolderExists = WasdiFileUtils.fileExists(sProcessorFolder);

			if (bProcessorFolderExists) {
				String sGeneralCommonEnvTemplateFilePath = getProcessorTemplateGeneralCommonEnvFilePath(sProcessorName);
				LauncherMain.s_oLogger.info("JupyterNotebookProcessorEngine.launchJupyterNotebook: GeneralCommonEnvTemplate: " + sGeneralCommonEnvTemplateFilePath);
				File oGeneralCommonEnvTemplate = new File(sGeneralCommonEnvTemplateFilePath);

				String sGeneralCommonEnvProcessorFilePath = getProcessorGeneralCommonEnvFilePath(sProcessorName);
				LauncherMain.s_oLogger.info("JupyterNotebookProcessorEngine.launchJupyterNotebook: GeneralCommonEnvProcessor: " + sGeneralCommonEnvProcessorFilePath);
				File oGeneralCommonEnvProcessor = new File(sGeneralCommonEnvProcessorFilePath);


				if (WasdiFileUtils.fileExists(oGeneralCommonEnvTemplate)) {
					if (!WasdiFileUtils.filesAreTheSame(oGeneralCommonEnvTemplate, oGeneralCommonEnvProcessor)) {
						WasdiFileUtils.deleteFile(sProcessorFolder);
						bProcessorFolderExists = WasdiFileUtils.fileExists(sProcessorFolder);
					}
				}

			}

			if (!bProcessorFolderExists) {
				// Copy Docker template files in the processor folder
				File oDockerTemplateFolder = new File(sProcessorTemplateFolder);
				File oProcessorFolder = new File(sProcessorFolder);

				LauncherMain.s_oLogger.info("JupyterNotebookProcessorEngine.launchJupyterNotebook: creating the ProcessorFolder: " + sProcessorFolder);
				FileUtils.copyDirectory(oDockerTemplateFolder, oProcessorFolder);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 10);


			// Create Docker Util to be able to launch docker-compose commands
			DockerUtils oDockerUtils = new DockerUtils(null, sProcessorFolder, m_sWorkingRootPath, m_sTomcatUser);
			
			// Check if our load balancer is up and running
			processWorkspaceLog("verify that the Traefik container is up-and-running");
			
			String sTraefikDockerContainerInspectCommand = buildCommandDockerContainerInspect("traefik-notebook");
			boolean bTraefikDockerContainerInspectResult = oDockerUtils.runCommand(sTraefikDockerContainerInspectCommand);

			if (!bTraefikDockerContainerInspectResult) {
				
				// No!! Time to wake-up it
				
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: the Traefik container is down or missing:" + LINE_SEPARATOR + sTraefikDockerContainerInspectCommand);

				processWorkspaceLog("starting the Traefik container");

				String sDockerComposeUpTraefikCommand = buildCommandDockerComposeUpTraefik();
				boolean bDockerComposeUpTraefikResult = oDockerUtils.runCommand(sDockerComposeUpTraefikCommand);

				if (!bDockerComposeUpTraefikResult) {
					LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: start Traefik command failed:" + LINE_SEPARATOR + sDockerComposeUpTraefikCommand);

					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
					return false;
				}

				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 20);
			}
			
			// We build our new docker
			processWorkspaceLog("execute command: docker build");

			String sDockerBuildCommand = buildCommandDockerBuild(sProcessorFolder);
			boolean bDockerBuildResult = oDockerUtils.runCommand(sDockerBuildCommand);

			if (!bDockerBuildResult) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker build command failed:" + LINE_SEPARATOR + sDockerBuildCommand);

				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 30);
			
			// This step will let WASDI push in a docker repository if (when) we will install one.
			processWorkspaceLog("execute command: docker push");

			String sDockerPushCommand = buildCommandDockerPush(sProcessorFolder);
			boolean bDockerPushResult = oDockerUtils.runCommand(sDockerPushCommand);

			if (!bDockerPushResult) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker push command failed:" + LINE_SEPARATOR + sDockerPushCommand);

				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 40);


			// use the template to create a docker-compose file
			// from: docker-compose.yml.j2
			// to:   docker-compose.yml

			processWorkspaceLog("create docker-compose file");

			// Read the template file
			String sDockerComposeYmlTemplateFileName = sProcessorFolder + "docker-compose.yml.j2";
			File oDockerComposeYmlTemplateFile = new File(sDockerComposeYmlTemplateFileName);
			String sDockerComposeYmlTemplateFileContent = FileUtils.readFileToString(oDockerComposeYmlTemplateFile);

			String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(oParameter.getUserId(), oParameter.getWorkspace());

			sDockerComposeYmlTemplateFileContent = sDockerComposeYmlTemplateFileContent.replace("{{ wasdiNotebookId }}", sJupyterNotebookCode);
			sDockerComposeYmlTemplateFileContent = sDockerComposeYmlTemplateFileContent.replace("{{ wasdiUserName }}", oParameter.getUserId());
			sDockerComposeYmlTemplateFileContent = sDockerComposeYmlTemplateFileContent.replace("{{ wasdiWorkspaceId }}", oParameter.getWorkspace());
			
			
            // Extra hosts mapping, useful for some instances when the server host can't be resolved
            // The symptoms of such problem is that the POST call from the Docker container timeouts
			
			String sExtraHosts = "";
            if (WasdiConfig.Current.dockers.extraHosts != null) {
            	
            	if (WasdiConfig.Current.dockers.extraHosts.size()>0) {
            		
            		LauncherMain.s_oLogger.debug("DockerUtils.run adding configured extra host mapping to the run arguments");
            		
            		sExtraHosts += "    extra_hosts:\n";
            		
                	for (int iExtraHost = 0; iExtraHost<WasdiConfig.Current.dockers.extraHosts.size(); iExtraHost ++) {
                		
                		String sExtraHost = WasdiConfig.Current.dockers.extraHosts.get(iExtraHost);
                		sExtraHosts += "      - " + sExtraHost + "\n";
                	}
            	}
            }
            
            if (!Utils.isNullOrEmpty(sDockerComposeYmlTemplateFileContent)) {
            	sDockerComposeYmlTemplateFileContent += sExtraHosts;
            }

			String sDockerComposeYmlFileName = sProcessorFolder + "docker-compose_" + sJupyterNotebookCode + ".yml";
			File oDockerComposeYmlFile = new File(sDockerComposeYmlFileName);
			FileUtils.writeStringToFile(oDockerComposeYmlFile, sDockerComposeYmlTemplateFileContent);

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 60);



			// mkdir **notebook**

			String sWorkspacePath = WasdiConfig.Current.paths.downloadRootPath + FILE_SEPARATOR + oParameter.getUserId() + FILE_SEPARATOR + oParameter.getWorkspace();

			String sNotebookPath = sWorkspacePath + FILE_SEPARATOR + "notebook";
			File oNotebookFolder = new File(sNotebookPath);

			if (!WasdiFileUtils.fileExists(oNotebookFolder)) {
				FileUtils.forceMkdir(oNotebookFolder);
			}

			LauncherMain.s_oLogger.info("JupyterNotebookProcessorEngine.launchJupyterNotebook: the " + sNotebookPath + " folder was" + (WasdiFileUtils.fileExists(oNotebookFolder) ? " " : " not ") + "created");



			processWorkspaceLog("execute command: docker-compose up");

			// Launch docker-compose up command
			String sDockerComposeUpCommand = buildCommandDockerComposeUpJupyterNotebook(sProcessorFolder, sJupyterNotebookCode);
			boolean bDockerComposeUpResult = oDockerUtils.runCommand(sDockerComposeUpCommand);

			if (!bDockerComposeUpResult) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker compose up command failed:" + LINE_SEPARATOR + sDockerComposeUpCommand);

				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 80);



			processWorkspaceLog("adding file: notebook_config.cfg");

			String sContainerName = "nb_" + sJupyterNotebookCode;
			LauncherMain.s_oLogger.info("JupyterNotebookProcessorEngine.launchJupyterNotebook: sContainerName: " + sContainerName);


			// create the JSON content of the temporary file
			String sJsonContent = "{\"WORKSPACEID\":\"" + oParameter.getWorkspace() + "\"}";
			LauncherMain.s_oLogger.info("JupyterNotebookProcessorEngine.launchJupyterNotebook: sJsonContent: " + sJsonContent);

			// create a temporary file on the host file-system
			String sTemporaryFileFullPath = sProcessorFolder + sContainerName + "_notebook_config.cfg";
			LauncherMain.s_oLogger.info("JupyterNotebookProcessorEngine.launchJupyterNotebook: sTemporaryFileFullPath: " + sTemporaryFileFullPath);

			WasdiFileUtils.writeFile(sJsonContent, sTemporaryFileFullPath);


			// copy the temporary config file to the container
			String sDockerCpFileCommand = buildCommandDockerCpFile(sContainerName, sTemporaryFileFullPath);
			boolean bDockerCpFileResult = oDockerUtils.runCommand(sDockerCpFileCommand);

			if (!bDockerCpFileResult) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: unable to copy config file on the container:" + LINE_SEPARATOR + sDockerCpFileCommand);

				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			}

			// remove the temporary file
			if (WasdiFileUtils.fileExists(sTemporaryFileFullPath)) {
				WasdiFileUtils.deleteFile(sTemporaryFileFullPath);
			}


			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 90);



			processWorkspaceLog("execute command: create traefik configuration");

			// Launch create traefik configuration command
			String sBuildTraefikConfigurationFileCommand = buildCommandTraefikConfigurationFile(sProcessorFolder, sJupyterNotebookCode);
			boolean bBuildTraefikConfigurationFileResult = oDockerUtils.runCommand(sBuildTraefikConfigurationFileCommand);

			if (!bBuildTraefikConfigurationFileResult) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: the creation of the Traefik configuration-file failed:" + LINE_SEPARATOR + sBuildTraefikConfigurationFileCommand);
				return false;
			}



			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);


			processWorkspaceLog(new EndMessageProvider().getGood());

			return true;
		} catch (Exception oEx) {
			LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook Exception", oEx);

			try {
				if (oProcessWorkspace != null) {
					// Check and set the operation end-date
					if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
						oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
					}

					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				}
			} catch (Exception e) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook Exception", e);
			}

			return false;
		}
	}

	public boolean terminateJupyterNotebook(ProcessorParameter oParameter) {
		LauncherMain.s_oLogger.debug("JupyterNotebookProcessorEngine.terminateJupyterNotebook: start");

		if (oParameter == null) {
			LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.terminateJupyterNotebook: oParameter is null");
			return false;
		}

		ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
		ProcessWorkspace oProcessWorkspace = null;

		String sProcessorName = oParameter.getName();

		try {
			processWorkspaceLog("Start termination of " + sProcessorName + " Type " + oParameter.getProcessorType());

			oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oProcessWorkspace = m_oProcessWorkspace;

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
			processWorkspaceLog("This is an attempt to terminate the Jupyter Notebook");


			String sProcessorFolder = getProcessorFolder(sProcessorName);

			String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(oParameter.getUserId(), oParameter.getWorkspace());


			// delete the Traefik configuration file
			// rm -f /data/wasdi/container/volume/traefik-notebook/etc_traefik/conf.d/nb_<notebook ID>
			
			String sTraefikConfigurationFilePath = "/data/wasdi/container/volume/traefik-notebook/etc_traefik/conf.d/nb_" + sJupyterNotebookCode + ".yml";
			File oTraefikConfigurationFile = new File(sTraefikConfigurationFilePath);

			if (WasdiFileUtils.fileExists(oTraefikConfigurationFile)) {
				FileUtils.deleteQuietly(oTraefikConfigurationFile);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 25);



			// Create Docker Util to be able to launch docker-compose commands
			DockerUtils oDockerUtils = new DockerUtils(null, sProcessorFolder, m_sWorkingRootPath, m_sTomcatUser);


			// stop the container instance
			// docker-compose [--file </path/to/the/docker-compose/file.yml] stop


			processWorkspaceLog("execute command: docker-compose stop");

			// Launch docker-compose stop command
			String sDockerComposeStopCommand = buildCommandDockerComposeStopJupyterNotebook(sProcessorFolder, sJupyterNotebookCode);
			boolean bDockerComposeStopResult = oDockerUtils.runCommand(sDockerComposeStopCommand);

			if (!bDockerComposeStopResult) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker compose stop command failed:" + LINE_SEPARATOR + sDockerComposeStopCommand);

				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 50);



			// delete the container instance
			// docker-compose [--file </path/to/the/docker-compose/file.yml] rm
			

			processWorkspaceLog("execute command: docker-compose rm");

			// Launch docker-compose rm command
			String sDockerComposeRmCommand = buildCommandDockerComposeRmJupyterNotebook(sProcessorFolder, sJupyterNotebookCode);
			boolean bDockerComposeRmResult = oDockerUtils.runCommand(sDockerComposeRmCommand);

			if (!bDockerComposeRmResult) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker compose rm command failed:" + LINE_SEPARATOR + sDockerComposeRmCommand);

				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 75);



			// Delete the Jupyter Notebook docker-compose file
			// rm -f /data/wasdi/processors/jupyter-notebook/docker-compose_<notebook ID>.yml

			String sDockerComposeFilePath = "/data/wasdi/processors/jupyter-notebook/docker-compose_" + sJupyterNotebookCode + ".yml";
			File oDockerComposeFile = new File(sDockerComposeFilePath);

			if (WasdiFileUtils.fileExists(oDockerComposeFile)) {
				FileUtils.deleteQuietly(oDockerComposeFile);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);



			processWorkspaceLog(new EndMessageProvider().getGood());

			return true;

		} catch (Exception oEx) {
			LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.terminateJupyterNotebook Exception", oEx);

			try {
				if (oProcessWorkspace != null) {
					// Check and set the operation end-date
					if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
						oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
					}

					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				}
			} catch (Exception e) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.terminateJupyterNotebook Exception", e);
			}

			return false;
		}

	}

	private static String buildCommandDockerBuild(String sProcessorFolder) {
		return "bash " + sProcessorFolder + "tool" + FILE_SEPARATOR + "dockerBuild.sh";
	}

	private static String buildCommandDockerPush(String sProcessorFolder) {
		return "bash " + sProcessorFolder + "tool" + FILE_SEPARATOR + "dockerPush.sh";
	}

	private static String buildCommandDockerComposeUpJupyterNotebook(String sProcessorFolder, String sJupyterNotebookCode) {
		StringBuilder oSB = new StringBuilder();

		oSB.append("docker-compose \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    --project-name " + sJupyterNotebookCode + "\\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    --file " + sProcessorFolder + "docker-compose_" + sJupyterNotebookCode + ".yml \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    --env-file " + sProcessorFolder + "var" + FILE_SEPARATOR + "general_common.env \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    up \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    --detach");

		return oSB.toString();
	}

	// docker-compose [--file </path/to/the/docker-compose/file.yml] stop
	private static String buildCommandDockerComposeStopJupyterNotebook(String sProcessorFolder, String sJupyterNotebookCode) {
		StringBuilder oSB = new StringBuilder();

		oSB.append("docker-compose \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    --project-name " + sJupyterNotebookCode + "\\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    --file " + sProcessorFolder + "docker-compose_" + sJupyterNotebookCode + ".yml \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    --env-file " + sProcessorFolder + "var" + FILE_SEPARATOR + "general_common.env \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    stop");

		return oSB.toString();
	}

	// docker-compose [--file </path/to/the/docker-compose/file.yml] rm
	private static String buildCommandDockerComposeRmJupyterNotebook(String sProcessorFolder, String sJupyterNotebookCode) {
		StringBuilder oSB = new StringBuilder();

		oSB.append("docker-compose \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    --project-name " + sJupyterNotebookCode + "\\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    --file " + sProcessorFolder + "docker-compose_" + sJupyterNotebookCode + ".yml \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    --env-file " + sProcessorFolder + "var" + FILE_SEPARATOR + "general_common.env \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    rm \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    --stop \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    --force");

		return oSB.toString();
	}

	private static String buildCommandTraefikConfigurationFile(String sProcessorFolder, String sJupyterNotebookCode) {
		StringBuilder oSB = new StringBuilder();

		oSB.append("/usr/bin/python3 \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("  -B \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("  /opt/companyExploitation/common/tool/toolbox/sysadmin/code/toolbox.py \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("  --configuration-directory /opt/companyExploitation/common/tool/toolbox/sysadmin/configuration/ \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("  --module jinja2 \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("  --submodule renderTemplate \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("  --template /data/wasdi/container/volume/traefik-notebook/etc_traefik/template/conf.d_notebook.yml.j2 \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("  --rendered-file /data/wasdi/container/volume/traefik-notebook/etc_traefik/conf.d/nb_" + sJupyterNotebookCode + ".yml \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("  --json-inline '{\"wasdiNotebookId\": \"" + sJupyterNotebookCode + "\"}' \\");
		oSB.append(LINE_SEPARATOR);
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

		oSB.append("docker-compose \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    --file /data/wasdi/processors/traefik-notebook/docker-compose.yml \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    up \\");
		oSB.append(LINE_SEPARATOR);
		oSB.append("    --detach");

		return oSB.toString();
	}

}
