package wasdi.processors;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.packagemanagers.IPackageManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.cwl.CwlApplicationPackageUtils;
import wasdi.shared.utils.docker.DockerUtils;
import wasdi.shared.utils.docker.containersViewModels.ContainerInfo;
import wasdi.shared.utils.docker.containersViewModels.constants.ContainerStates;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.stac.StacStageInUtils;

/**
 * Processor Engine for the OGC Best Practice for Earth Observation Application Package
 * (see https://docs.ogc.org/bp/20-089r1.html).
 * 
 * Unlike {@link EoepcaProcessorEngine}, that pushes a WASDI-generated Application Package
 * to an external EOEPCA/ADES platform, this engine makes WASDI itself the "Platform": the
 * user uploads a ready-made Application Package (a CWL document, with either its own
 * Dockerfile/source or a reference to an already published image), and WASDI builds/pulls
 * the container and runs it locally as a one-shot CWL CommandLineTool.
 * 
 * v1 scope: single Workflow + single CommandLineTool, no multi-step workflows, no
 * File/Directory (STAC) stage-in/out yet.
 * 
 * @author p.campanella
 */
public class OgcAppPackageProcessorEngine extends DockerProcessorEngine {

	/**
	 * Name of the Dockerfile expected in the Application Package folder, when the package is self-contained
	 */
	protected static final String DOCKERFILE_NAME = "Dockerfile";

	/**
	 * Fixed container-side mount point of the workspace folder (matches DockerUtils' own convention).
	 * Reserved for the upcoming STAC stage-in/out host-to-container path translation.
	 */
	protected static final String CONTAINER_DATA_FOLDER = "/data/wasdi";

	/**
	 * Workspace subfolder where STAC Directory inputs are staged-in, one folder per input id
	 */
	protected static final String STAC_STAGE_IN_FOLDER_NAME = ".stac-in";

	public OgcAppPackageProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.OGC_APP_PACKAGE);

		// A CWL CommandLineTool is a one-shot CLI application, not a long running REST server
		m_bRunAfterDeploy = false;
	}

	/**
	 * Deploys the Application Package: unzips it, parses the CWL document, derives the WASDI
	 * parameter sample from the Workflow inputs, and either builds the user provided Dockerfile
	 * or pulls the image referenced in the CWL DockerRequirement.
	 */
	@Override
	public boolean deploy(ProcessorParameter oParameter, boolean bFirstDeploy) {

		WasdiLog.debugLog("OgcAppPackageProcessorEngine.deploy: start");

		if (oParameter == null) {
			return logDeployErrorAndClean("parameter is null, return false", bFirstDeploy);
		}

		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		ProcessorRepository oProcessorRepository = new ProcessorRepository();

		String sProcessorName = oParameter.getName();
		String sProcessorId = oParameter.getProcessorID();

		try {
			processWorkspaceLog("Start Deploy of OGC Application Package " + sProcessorName);

			if (bFirstDeploy) {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.RUNNING, 0);
			}

			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);
			String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);

			File oProcessorZipFile = new File(sProcessorFolder + sProcessorId + ".zip");

			if (!oProcessorZipFile.exists()) {
				return logDeployErrorAndClean("DeployProcessor Cannot find the processor Zip file, something went wrong", bFirstDeploy);
			}

			if (bFirstDeploy) {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.RUNNING, 5);
			}

			if (!unzipProcessor(sProcessorFolder, sProcessorId + ".zip", oParameter.getProcessObjId())) {
				return logDeployErrorAndClean("error unzipping the Processor [" + sProcessorName + "]", bFirstDeploy);
			}

			if (bFirstDeploy) {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.RUNNING, 20);
			}

			// Copy the (currently empty) template folder, kept for consistency with the other Docker-based engines
			File oDockerTemplateFolder = new File(m_sDockerTemplatePath);

			if (oDockerTemplateFolder.exists()) {
				FileUtils.copyDirectory(oDockerTemplateFolder, new File(sProcessorFolder));
			}

			processWorkspaceLog("Parsing the OGC Application Package (CWL document)");

			File oCwlFile = CwlApplicationPackageUtils.findCwlFile(sProcessorFolder);

			if (oCwlFile == null) {
				return logDeployErrorAndClean("no CWL file found in the uploaded Application Package", bFirstDeploy);
			}

			Map<String, Object> oCwlDocument = CwlApplicationPackageUtils.loadCwlDocument(oCwlFile);

			if (oCwlDocument == null) {
				return logDeployErrorAndClean("impossible to parse the CWL document " + oCwlFile.getName(), bFirstDeploy);
			}

			Map<String, Object> oWorkflowNode = CwlApplicationPackageUtils.getWorkflowNode(oCwlDocument);
			Map<String, Object> oCommandLineToolNode = CwlApplicationPackageUtils.getCommandLineToolNode(oCwlDocument);

			if (oWorkflowNode == null || oCommandLineToolNode == null) {
				return logDeployErrorAndClean("the CWL document must contain a Workflow class and a CommandLineTool class (OGC BP Requirement 7)", bFirstDeploy);
			}

			if (Utils.isNullOrEmpty(String.valueOf(oCommandLineToolNode.get("id"))) || oCommandLineToolNode.get("baseCommand") == null) {
				return logDeployErrorAndClean("the CommandLineTool must declare an id and a baseCommand (OGC BP Requirement 8)", bFirstDeploy);
			}

			// Derive and store the WASDI parameter sample from the Workflow inputs
			String sParameterSample = CwlApplicationPackageUtils.buildParameterSampleJson(oWorkflowNode);
			oProcessor.setParameterSample(sParameterSample);
			oProcessorRepository.updateProcessor(oProcessor);

			if (bFirstDeploy) {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.RUNNING, 30);
			}

			boolean bHasOwnDockerfile = new File(sProcessorFolder, DOCKERFILE_NAME).exists();

			m_sDockerRegistry = getDockerRegisterAddress();

			if (bHasOwnDockerfile) {

				processWorkspaceLog("Application Package includes its own Dockerfile: building the image");

				if (Utils.isNullOrEmpty(m_sDockerRegistry)) {
					return logDeployErrorAndClean("register address not found, return false.", bFirstDeploy);
				}

				DockerUtils oDockerUtils = new DockerUtils(oProcessor, m_oParameter, sProcessorFolder, m_sDockerRegistry, m_oProcessWorkspaceLogger);
				m_sDockerImageName = oDockerUtils.build();

				if (Utils.isNullOrEmpty(m_sDockerImageName)) {
					return logDeployErrorAndClean("the docker build returned an empty image name, something went wrong", bFirstDeploy);
				}

				if (bFirstDeploy) {
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.RUNNING, 70);
				}

				String sPushedImageAddress = pushImageInRegisters(oProcessor);

				if (Utils.isNullOrEmpty(sPushedImageAddress)) {
					return logDeployErrorAndClean("impossible to push the built image", bFirstDeploy);
				}
			}
			else {

				String sDockerPull = CwlApplicationPackageUtils.getDockerPull(oCommandLineToolNode);

				if (Utils.isNullOrEmpty(sDockerPull)) {
					return logDeployErrorAndClean("no Dockerfile in the package and no DockerRequirement.dockerPull in the CWL: impossible to get a container image", bFirstDeploy);
				}

				processWorkspaceLog("Application Package references the external image " + sDockerPull + ": pulling it");

				DockerUtils oDockerUtils = new DockerUtils(oProcessor, m_oParameter, sProcessorFolder);

				if (!oDockerUtils.pull(sDockerPull, "")) {
					WasdiLog.warnLog("OgcAppPackageProcessorEngine.deploy: impossible to pull " + sDockerPull + ", it may already be available locally: we will try again when the app is run");
				}
			}

			if (bFirstDeploy) {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, m_oProcessWorkspace, ProcessStatus.DONE, 100);
			}

			processWorkspaceLog("Deploy done");

			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcAppPackageProcessorEngine.deploy Exception", oEx);
			return logDeployErrorAndClean("Exception deploying the Application Package: " + oEx.toString(), bFirstDeploy);
		}
	}

	/**
	 * Runs the Application Package: builds the container command line from the CWL
	 * CommandLineTool and the WASDI job parameters, starts the container once, and waits
	 * for it to exit.
	 */
	@Override
	public boolean run(ProcessorParameter oParameter) {

		WasdiLog.debugLog("OgcAppPackageProcessorEngine.run: start");

		if (oParameter == null) {
			WasdiLog.errorLog("OgcAppPackageProcessorEngine.run: parameter is null");
			return false;
		}

		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		ProcessWorkspace oProcessWorkspace = m_oProcessWorkspace;

		try {
			checkAndCreateWorkspaceFolder(oParameter);

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			String sProcessorName = oParameter.getName();
			String sProcessorId = oParameter.getProcessorID();

			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

			if (oProcessor == null) {
				WasdiLog.errorLog("OgcAppPackageProcessorEngine.run: Impossible to find processor " + sProcessorId);
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);

			File oCwlFile = CwlApplicationPackageUtils.findCwlFile(sProcessorFolder);

			if (oCwlFile == null) {
				processWorkspaceLog("No CWL file found for this Application Package");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			Map<String, Object> oCwlDocument = CwlApplicationPackageUtils.loadCwlDocument(oCwlFile);
			Map<String, Object> oWorkflowNode = CwlApplicationPackageUtils.getWorkflowNode(oCwlDocument);
			Map<String, Object> oCommandLineToolNode = CwlApplicationPackageUtils.getCommandLineToolNode(oCwlDocument);

			if (oCommandLineToolNode == null) {
				processWorkspaceLog("Impossible to parse the CommandLineTool from the CWL document");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			// The job values are keyed by Workflow input ids: resolve them to the CommandLineTool's own input ids
			Map<String, Object> oWorkflowInputValues = decodeJobInputValues(oParameter.getJson());
			Map<String, Object> oJobInputValues = CwlApplicationPackageUtils.mapWorkflowValuesToToolInputs(oWorkflowNode, oWorkflowInputValues);

			String sHostWorkspacePath = PathsConfig.getWorkspacePath(oParameter);

			if (!stageInDirectoryInputs(oCommandLineToolNode, oJobInputValues, sHostWorkspacePath)) {
				processWorkspaceLog("Impossible to stage-in one of the STAC inputs");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			List<String> asCommand = CwlApplicationPackageUtils.buildCommandLine(oCommandLineToolNode, oJobInputValues);

			boolean bHasOwnDockerfile = new File(sProcessorFolder, DOCKERFILE_NAME).exists();

			m_sDockerRegistry = getDockerRegisterAddress();

			String sImageName;
			String sImageVersion;
			DockerUtils oDockerUtils;

			if (bHasOwnDockerfile) {
				// Self-built image: follows the usual wasdi/<name>:<version> naming convention
				oDockerUtils = new DockerUtils(oProcessor, m_oParameter, sProcessorFolder, m_sDockerRegistry, m_oProcessWorkspaceLogger);
				sImageName = sProcessorName;
				sImageVersion = oProcessor.getVersion();
			}
			else {
				// Externally referenced image: use the dockerPull reference as-is, no registry prefix
				oDockerUtils = new DockerUtils(oProcessor, m_oParameter, sProcessorFolder);
				sImageName = CwlApplicationPackageUtils.getDockerPull(oCommandLineToolNode);
				sImageVersion = null;

				if (Utils.isNullOrEmpty(sImageName)) {
					processWorkspaceLog("No DockerRequirement.dockerPull found in the CWL document");
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
					return false;
				}

				// Best effort: the image may already be available locally from the deploy step
				oDockerUtils.pull(sImageName, "");
			}

			processWorkspaceLog("Starting the Application container");
			WasdiLog.debugLog("OgcAppPackageProcessorEngine.run: command line " + asCommand);

			// Unlike generic shell-exec containers, an OGC app only needs (and should only see) its own workspace
			String sContainerId = oDockerUtils.run(sImageName, sImageVersion, asCommand, true, null, false, sHostWorkspacePath);

			if (Utils.isNullOrEmpty(sContainerId)) {
				processWorkspaceLog("Impossible to start the Application container");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			ProcessStatus oFinalStatus = waitForContainerCompletion(oDockerUtils, sContainerId, oProcessor, oProcessWorkspaceRepository, oProcessWorkspace);

			// Surface the container stdout/stderr to the user, same as the docker build logs do
			String sContainerLogs = oDockerUtils.getContainerLogsByContainerId(sContainerId);

			if (!Utils.isNullOrEmpty(sContainerLogs)) {
				processWorkspaceLog("Application output:");
				processWorkspaceLog(sContainerLogs);
			}

			WasdiLog.debugLog("OgcAppPackageProcessorEngine.run: container output " + sContainerLogs);

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, oFinalStatus, 100);

			try {
				oDockerUtils.removeContainer(sContainerId, true);
			}
			catch (Exception oRemoveEx) {
				WasdiLog.warnLog("OgcAppPackageProcessorEngine.run: impossible to remove the container " + sContainerId);
			}

			if (Utils.isNullOrEmpty(oProcessWorkspace.getOperationEndTimestamp())) {
				oProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
			}

			return oFinalStatus != ProcessStatus.ERROR;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcAppPackageProcessorEngine.run Exception", oEx);
			try {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			}
			catch (Exception oInnerEx) {
				WasdiLog.errorLog("OgcAppPackageProcessorEngine.run Exception", oInnerEx);
			}
			return false;
		}
		finally {
			if (oProcessWorkspace != null) {
				m_oProcessWorkspace.setStatus(oProcessWorkspace.getStatus());
			}
		}
	}

	/**
	 * Resolves every Directory-typed CommandLineTool input by staging-in the STAC Item it
	 * references (see {@link StacStageInUtils}), and replaces its raw job value (the STAC Item
	 * url) with the resulting container-visible local path.
	 */
	protected boolean stageInDirectoryInputs(Map<String, Object> oCommandLineTool, Map<String, Object> oJobInputValues, String sHostWorkspacePath) {

		List<String> asDirectoryInputIds = CwlApplicationPackageUtils.getInputIdsOfType(oCommandLineTool, "Directory");

		for (String sInputId : asDirectoryInputIds) {

			Object oRawValue = oJobInputValues.get(sInputId);

			if (oRawValue == null) {
				WasdiLog.warnLog("OgcAppPackageProcessorEngine.stageInDirectoryInputs: no value provided for Directory input [" + sInputId + "], skipping it");
				continue;
			}

			String sHostStagingFolder = sHostWorkspacePath + STAC_STAGE_IN_FOLDER_NAME + "/" + sInputId;

			processWorkspaceLog("Staging-in STAC input [" + sInputId + "]");

			File oStagedFolder = StacStageInUtils.stageInStacItem(oRawValue.toString(), sHostStagingFolder);

			if (oStagedFolder == null) {
				WasdiLog.errorLog("OgcAppPackageProcessorEngine.stageInDirectoryInputs: impossible to stage-in input [" + sInputId + "]");
				return false;
			}

			oJobInputValues.put(sInputId, translateHostPathToContainerPath(sHostWorkspacePath, oStagedFolder.getAbsolutePath()));
		}

		return true;
	}

	/**
	 * Translates a host path inside the current job's workspace folder into the equivalent
	 * container path (the workspace folder is bind-mounted at CONTAINER_DATA_FOLDER, see run())
	 */
	protected String translateHostPathToContainerPath(String sHostWorkspacePath, String sHostPath) {
		String sNormalizedWorkspacePath = sHostWorkspacePath.endsWith("/") ? sHostWorkspacePath : sHostWorkspacePath + "/";
		String sRelativePath = sHostPath.startsWith(sNormalizedWorkspacePath) ? sHostPath.substring(sNormalizedWorkspacePath.length()) : sHostPath;
		return CONTAINER_DATA_FOLDER + "/" + sRelativePath;
	}

	/**
	 * Polls the container State until it exits (or the user stops the process, or the
	 * processor timeout is reached), and returns the corresponding final status.
	 */
	protected ProcessStatus waitForContainerCompletion(DockerUtils oDockerUtils, String sContainerId, Processor oProcessor,
			ProcessWorkspaceRepository oProcessWorkspaceRepository, ProcessWorkspace oProcessWorkspace) {

		long lTimeSpentMs = 0;
		long lSleepMs = 2000;

		while (true) {

			ContainerInfo oContainer = oDockerUtils.getContainerInfoByContainerId(sContainerId);

			if (oContainer == null) {
				WasdiLog.errorLog("OgcAppPackageProcessorEngine.waitForContainerCompletion: container not found anymore, consider it failed");
				return ProcessStatus.ERROR;
			}

			if (ContainerStates.EXITED.equals(oContainer.State) || ContainerStates.DEAD.equals(oContainer.State)) {
				return isSuccessfulExit(oContainer) ? ProcessStatus.DONE : ProcessStatus.ERROR;
			}

			// Did the user ask to stop the process in the meantime?
			ProcessWorkspace oRefreshedProcessWorkspace = oProcessWorkspaceRepository.getProcessByProcessObjId(oProcessWorkspace.getProcessObjId());

			if (oRefreshedProcessWorkspace != null && ProcessStatus.STOPPED.name().equals(oRefreshedProcessWorkspace.getStatus())) {
				WasdiLog.infoLog("OgcAppPackageProcessorEngine.waitForContainerCompletion: user stopped the process, stopping the container");
				oDockerUtils.stop(sContainerId);
				return ProcessStatus.STOPPED;
			}

			try {
				Thread.sleep(lSleepMs);
			}
			catch (InterruptedException oEx) {
				Thread.currentThread().interrupt();
				WasdiLog.errorLog("OgcAppPackageProcessorEngine.waitForContainerCompletion: interrupted", oEx);
			}

			lTimeSpentMs += lSleepMs;

			if (oProcessor.getTimeoutMs() > 0 && lTimeSpentMs > oProcessor.getTimeoutMs()) {
				WasdiLog.debugLog("OgcAppPackageProcessorEngine.waitForContainerCompletion: Timeout reached, stopping the container");
				oDockerUtils.stop(sContainerId);
				return ProcessStatus.ERROR;
			}
		}
	}

	/**
	 * Best effort parsing of the Docker "Status" field (e.g. "Exited (0) 5 seconds ago")
	 * to detect if the container process exited with a 0 return code.
	 */
	protected boolean isSuccessfulExit(ContainerInfo oContainer) {
		try {
			if (oContainer == null || oContainer.Status == null) return false;

			Matcher oMatcher = Pattern.compile("Exited \\((-?\\d+)\\)").matcher(oContainer.Status);

			if (oMatcher.find()) {
				return "0".equals(oMatcher.group(1));
			}

			// No exit code found in the status: fall back on the container State
			return ContainerStates.EXITED.equals(oContainer.State);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcAppPackageProcessorEngine.isSuccessfulExit: exception", oEx);
			return false;
		}
	}

	/**
	 * Decodes the WASDI job input values (ProcessorParameter json) in a generic Map
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> decodeJobInputValues(String sJson) {
		try {
			if (Utils.isNullOrEmpty(sJson)) return new HashMap<>();

			ObjectMapper oMapper = new ObjectMapper();

			try {
				return oMapper.readValue(sJson, Map.class);
			}
			catch (Exception oEx) {
				String sDecodedJson = java.net.URLDecoder.decode(sJson, "UTF-8");
				return oMapper.readValue(sDecodedJson, Map.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcAppPackageProcessorEngine.decodeJobInputValues: impossible to decode the job parameters", oEx);
			return new HashMap<>();
		}
	}

	/**
	 * Force the redeploy of the Application: rebuilds the image if the package has its own
	 * Dockerfile, otherwise just re-pulls the referenced external image.
	 */
	@Override
	public boolean redeploy(ProcessorParameter oParameter) {

		if (oParameter == null) {
			WasdiLog.errorLog("OgcAppPackageProcessorEngine.redeploy: oParameter is null");
			return false;
		}

		String sProcessorId = oParameter.getProcessorID();
		String sProcessorName = oParameter.getName();

		ProcessorRepository oProcessorRepository = new ProcessorRepository();
		Processor oProcessor = oProcessorRepository.getProcessor(sProcessorId);

		if (oProcessor == null) {
			WasdiLog.errorLog("OgcAppPackageProcessorEngine.redeploy: oProcessor is null [" + sProcessorId + "]");
			return false;
		}

		String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);

		File oCwlFile = CwlApplicationPackageUtils.findCwlFile(sProcessorFolder);

		if (oCwlFile == null) {
			WasdiLog.errorLog("OgcAppPackageProcessorEngine.redeploy: no CWL file found, cannot redeploy");
			return false;
		}

		Map<String, Object> oCwlDocument = CwlApplicationPackageUtils.loadCwlDocument(oCwlFile);
		Map<String, Object> oWorkflowNode = CwlApplicationPackageUtils.getWorkflowNode(oCwlDocument);
		Map<String, Object> oCommandLineToolNode = CwlApplicationPackageUtils.getCommandLineToolNode(oCwlDocument);

		// Refresh the parameter sample in case the user replaced the CWL document
		if (oWorkflowNode != null) {
			oProcessor.setParameterSample(CwlApplicationPackageUtils.buildParameterSampleJson(oWorkflowNode));
		}

		boolean bHasOwnDockerfile = new File(sProcessorFolder, DOCKERFILE_NAME).exists();

		m_sDockerRegistry = getDockerRegisterAddress();

		if (bHasOwnDockerfile) {

			if (Utils.isNullOrEmpty(m_sDockerRegistry)) {
				WasdiLog.errorLog("OgcAppPackageProcessorEngine.redeploy: register address not found, return false.");
				return false;
			}

			DockerUtils oDockerUtils = new DockerUtils(oProcessor, m_oParameter, sProcessorFolder, m_sDockerRegistry, m_oProcessWorkspaceLogger);

			if (oDockerUtils.isContainerStarted(oProcessor.getName(), oProcessor.getVersion())) {
				oDockerUtils.stop(oProcessor);
			}

			String sNewVersion = StringUtils.incrementIntegerString(oProcessor.getVersion());
			oProcessor.setVersion(sNewVersion);
			oProcessorRepository.updateProcessor(oProcessor);

			m_sDockerImageName = oDockerUtils.build();

			if (Utils.isNullOrEmpty(m_sDockerImageName)) {
				WasdiLog.errorLog("OgcAppPackageProcessorEngine.redeploy: the docker build returned an empty image name, something went wrong");
				return false;
			}

			String sPushedImageAddress = pushImageInRegisters(oProcessor);

			return !Utils.isNullOrEmpty(sPushedImageAddress);
		}
		else {

			if (oCommandLineToolNode == null) {
				WasdiLog.errorLog("OgcAppPackageProcessorEngine.redeploy: impossible to parse the CommandLineTool from the CWL document");
				return false;
			}

			String sDockerPull = CwlApplicationPackageUtils.getDockerPull(oCommandLineToolNode);

			if (Utils.isNullOrEmpty(sDockerPull)) {
				WasdiLog.errorLog("OgcAppPackageProcessorEngine.redeploy: no DockerRequirement.dockerPull found in the CWL document");
				return false;
			}

			DockerUtils oDockerUtils = new DockerUtils(oProcessor, m_oParameter, sProcessorFolder);

			String sNewVersion = StringUtils.incrementIntegerString(oProcessor.getVersion());
			oProcessor.setVersion(sNewVersion);
			oProcessorRepository.updateProcessor(oProcessor);

			return oDockerUtils.pull(sDockerPull, "");
		}
	}

	/**
	 * Stops the running Application container, if any
	 */
	@Override
	public boolean stopApplication(ProcessorParameter oParameter) {
		try {
			String sProcessorName = m_oProcessWorkspace.getProductName();
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			Processor oProcessor = oProcessorRepository.getProcessorByName(sProcessorName);

			if (oProcessor == null) {
				WasdiLog.warnLog("OgcAppPackageProcessorEngine.stopApplication: impossible to find the processor " + sProcessorName);
				return false;
			}

			DockerUtils oDockerUtils = new DockerUtils(oProcessor, PathsConfig.getProcessorFolder(oProcessor));
			return oDockerUtils.stop(oProcessor);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcAppPackageProcessorEngine.stopApplication: error", oEx);
			return false;
		}
	}

	/**
	 * A CWL CommandLineTool has no notion of "add a library": redeploy is the way to update it
	 */
	@Override
	public boolean libraryUpdate(ProcessorParameter oParameter) {
		WasdiLog.debugLog("OgcAppPackageProcessorEngine.libraryUpdate: not applicable for OGC Application Packages, nothing to do");
		return true;
	}

	/**
	 * Not supported for this Processor Engine
	 */
	@Override
	public boolean environmentUpdate(ProcessorParameter oParameter) {
		return false;
	}

	/**
	 * Not supported for this Processor Engine
	 */
	@Override
	public boolean refreshPackagesInfo(ProcessorParameter oParameter) {
		return false;
	}

	/**
	 * The container image is not built from an internal package manager
	 */
	@Override
	protected IPackageManager getPackageManager(String sUrl) {
		return null;
	}
}
