package wasdi.processors;

import java.io.File;
import java.util.Date;

import org.apache.commons.io.FileUtils;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Processor;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.managers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.EndMessageProvider;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;

public class JupyterNotebookProcessorEngine extends DockerProcessorEngine {

	private static final String FILE_SEPARATOR = System.getProperty("file.separator");
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
		return launchJupyterNotebook(oParameter, true);
	}

	public boolean launchJupyterNotebook(ProcessorParameter oParameter, boolean bFirstDeploy) {
		LauncherMain.s_oLogger.debug("JupyterNotebookProcessorEngine.launchJupyterNotebook: start");

		if (oParameter == null) {
			LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: oParameter is null");
			return false;
		}

		ProcessWorkspaceRepository oProcessWorkspaceRepository = null;
		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		ProcessWorkspace oProcessWorkspace = null;

		String sProcessorName = oParameter.getName();
		String sProcessorId = oParameter.getProcessorID();

		try {
			processWorkspaceLog("Start launch of " + sProcessorName + " Type " + oParameter.getProcessorType());

			oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oProcessWorkspace = m_oProcessWorkspace;

			if (bFirstDeploy) {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);
				processWorkspaceLog("This is a first launch of the Jupyter Notebook");
			}

			// First Check if processor exists
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

			String sProcessorFolder = getProcessorFolder(sProcessorName);
			String sProcessorTemplateFolder = getProcessorTemplateFolder(sProcessorName);

			// check if the processors/jupyter-notebook exists
			// make a copy of the template directory (in case it does not exist)
			// from: /data/wasdi/dockertemplate/jupyter-notebook/
			// to:   /data/wasdi/processors/jupyter-notebook

			boolean bProcessorTemplateFolderExists = WasdiFileUtils.fileExists(sProcessorTemplateFolder);

			if (!bProcessorTemplateFolderExists) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: the ProcessorTemplateFolder does not exist: " + sProcessorTemplateFolder);

                if (bFirstDeploy) {
                    oProcessorRepository.deleteProcessor(sProcessorId);
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
			}

			processWorkspaceLog("copy template directory");

			boolean bProcessorFolderExists = WasdiFileUtils.fileExists(sProcessorFolder);

			if (!bProcessorFolderExists) {
	            // Copy Docker template files in the processor folder
	            File oDockerTemplateFolder = new File(sProcessorTemplateFolder);
	            File oProcessorFolder = new File(sProcessorFolder);

	            FileUtils.copyDirectory(oDockerTemplateFolder, oProcessorFolder);
			}

			if (bFirstDeploy)
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 10);


			// Create Docker Util and launch docker-compose command
			DockerUtils oDockerUtils = new DockerUtils(oProcessor, sProcessorFolder, m_sWorkingRootPath, m_sTomcatUser);

//			processWorkspaceLog("+++++++++++++++++++++++++++++++");
			oDockerUtils.runCommand("pwd");
//			processWorkspaceLog("*******************************");
			oDockerUtils.runCommand("ll");
//			processWorkspaceLog("===============================");


			processWorkspaceLog("execute command: docker build");

			String sDockerBuildCommand = buidCommandDockerBuild(sProcessorFolder);
			boolean bDockerBuildResult = oDockerUtils.runCommand(sDockerBuildCommand);

			if (!bDockerBuildResult) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker build command failed:" + LINE_SEPARATOR + sDockerBuildCommand);

                if (bFirstDeploy) {
                    oProcessorRepository.deleteProcessor(sProcessorId);
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
			}

			if (bFirstDeploy)
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 20);



			processWorkspaceLog("execute command: docker push");

			String sDockerPushCommand = buidCommandDockerPush(sProcessorFolder);
			boolean bDockerPushResult = oDockerUtils.runCommand(sDockerPushCommand);

			if (!bDockerPushResult) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker push command failed:" + LINE_SEPARATOR + sDockerPushCommand);

                if (bFirstDeploy) {
                    oProcessorRepository.deleteProcessor(sProcessorId);
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
			}

			if (bFirstDeploy)
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 40);



			processWorkspaceLog("create docker-compose file");

			// use the template to create a docker-compose file
			// from: docker-compose.yml.j2
			// to:   docker-compose.yml

			// Read the template file
			String sDockerComposeYmlTemplateFileName = sProcessorFolder + "docker-compose.yml.j2";
			File oDockerComposeYmlTemplateFile = new File(sDockerComposeYmlTemplateFileName);
			String sDockerComposeYmlTemplateFileContent = FileUtils.readFileToString(oDockerComposeYmlTemplateFile);

			String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(oParameter.getUserId(), oParameter.getWorkspace());

			sDockerComposeYmlTemplateFileContent = sDockerComposeYmlTemplateFileContent.replace("{{ wasdiNotebookId }}", sJupyterNotebookCode);
			sDockerComposeYmlTemplateFileContent = sDockerComposeYmlTemplateFileContent.replace("{{ wasdiUserName }}", oParameter.getUserId());
			sDockerComposeYmlTemplateFileContent = sDockerComposeYmlTemplateFileContent.replace("{{ wasdiWorkspaceId }}", oParameter.getWorkspace());

			String sDockerComposeYmlFileName = sProcessorFolder + "docker-compose_" + sJupyterNotebookCode + ".yml";
			File oDockerComposeYmlFile = new File(sDockerComposeYmlFileName);
			FileUtils.writeStringToFile(oDockerComposeYmlFile, sDockerComposeYmlTemplateFileContent);

			if (bFirstDeploy)
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 60);



			processWorkspaceLog("execute command: docker-compose");

			// Create Docker Util and launch docker-compose command
			String sDockerComposeCommand = buidCommandDockerCompose(sProcessorFolder, sJupyterNotebookCode);
			boolean bDockerComposeResult = oDockerUtils.runCommand(sDockerComposeCommand);

			if (!bDockerComposeResult) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: the docker compose command failed:" + LINE_SEPARATOR + sDockerComposeCommand);

                if (bFirstDeploy) {
                    oProcessorRepository.deleteProcessor(sProcessorId);
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
			}

			if (bFirstDeploy)
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 80);



			processWorkspaceLog("execute command: chown folder");

			// Create chown command
			String sChangeOwnerNotebookFolderCommand = buidCommandChangeOwnerNotebookFolder(oParameter.getUserId(), oParameter.getWorkspace());;
			boolean bChangeOwnerNotebookFolderResult = oDockerUtils.runCommand(sChangeOwnerNotebookFolderCommand);

			if (!bChangeOwnerNotebookFolderResult) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: the chown command failed:" + LINE_SEPARATOR + sChangeOwnerNotebookFolderCommand);

                if (bFirstDeploy) {
                    oProcessorRepository.deleteProcessor(sProcessorId);
                    LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
                }
			}

			if (bFirstDeploy)
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 90);



			processWorkspaceLog("execute command: create traefik configuration");

			// Create Docker Util and launch docker-compose command
			String sBuildTraefikConfigurationFileCommand = buidCommandTraefikConfigurationFile(sProcessorFolder, sJupyterNotebookCode);
			boolean bBuildTraefikConfigurationFileResult = oDockerUtils.runCommand(sBuildTraefikConfigurationFileCommand);

			if (!bBuildTraefikConfigurationFileResult) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook: the creation of the Traefik configuration-file failed:" + LINE_SEPARATOR + sBuildTraefikConfigurationFileCommand);
				return false;
			}


            if (bFirstDeploy)
                LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
            

            processWorkspaceLog(new EndMessageProvider().getGood());

    		return true;
		} catch (Exception oEx) {
			LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook Exception", oEx);

			try {
				if (oProcessWorkspace != null) {
					// Check and set the operation end-date
					if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndDate())) {
						oProcessWorkspace.setOperationEndDate(Utils.getFormatDate(new Date()));
					}

					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				}
			} catch (Exception e) {
				LauncherMain.s_oLogger.error("JupyterNotebookProcessorEngine.launchJupyterNotebook Exception", e);
			}

			return false;
		}
	}

	private static String buidCommandDockerBuild(String sProcessorFolder) {
		return "bash " + sProcessorFolder + "tool" + FILE_SEPARATOR + "dockerBuild.sh";
	}

	private static String buidCommandDockerPush(String sProcessorFolder) {
		return "bash " + sProcessorFolder + "tool" + FILE_SEPARATOR + "dockerPush.sh";
	}

	private static String buidCommandDockerCompose(String sProcessorFolder, String sJupyterNotebookCode) {
        StringBuilder oSB = new StringBuilder();

        oSB.append("docker-compose \\");
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

	private static String buidCommandTraefikConfigurationFile(String sProcessorFolder, String sJupyterNotebookCode) {
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

	private static String buidCommandChangeOwnerNotebookFolder(String sUserId, String sWorkspaceId) {
        StringBuilder oSB = new StringBuilder();

        String sWorkspacePath = WasdiConfig.Current.paths.downloadRootPath + FILE_SEPARATOR + sUserId + FILE_SEPARATOR + sWorkspaceId;

        oSB.append("sudo chown tomcat:tomcat ").append(sWorkspacePath).append(FILE_SEPARATOR).append("notebook");

        return oSB.toString();
	}

}
