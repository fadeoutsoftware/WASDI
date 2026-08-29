package wasdi.processors;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.LauncherMain;
import wasdi.io.WasdiProductReader;
import wasdi.io.WasdiProductReaderFactory;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.DownloadedFileCategory;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
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
import wasdi.shared.utils.stac.StacStageOutUtils;
import wasdi.shared.viewmodels.products.ProductViewModel;

/**
 * Processor Engine for the OGC Best Practice for Earth Observation Application Package
 * (see https://docs.ogc.org/bp/20-089r1.html).
 * 
 * Unlike {@link EoepcaProcessorEngine}, that pushes a WASDI-generated Application Package
 * to an external EOEPCA/ADES platform, this engine makes WASDI itself the "Platform": the
 * user uploads a ready-made Application Package (a CWL document, with either its own
 * Dockerfile/source or references to already published images), and WASDI runs it locally.
 * 
 * The actual CWL execution (steps wiring, scatter, expressions, subworkflows...) is delegated
 * to the real cwltool reference runner, invoked inside the dedicated "wasdi-cwl" image, which
 * in turn drives the host Docker Engine (via the bind-mounted docker.sock) to run each
 * CommandLineTool's own container - the same "sibling containers" pattern already used
 * everywhere else in WASDI, not nested Docker-in-Docker.
 * 
 * WASDI's own responsibilities stay: building/pushing the image for self-contained packages,
 * workspace mounting, STAC stage-in/out, and registering results as WASDI products.
 * 
 * @author p.campanella
 */
public class OgcAppPackageProcessorEngine extends DockerProcessorEngine {

	/**
	 * Name of the Dockerfile expected in the Application Package folder, when the package is self-contained
	 */
	protected static final String DOCKERFILE_NAME = "Dockerfile";

	/**
	 * Workspace subfolder where STAC Directory inputs are staged-in, one folder per input id
	 */
	protected static final String STAC_STAGE_IN_FOLDER_NAME = ".stac-in";

	/**
	 * Workspace subfolder used for a single cwltool run: the (copied) resolved CWL document,
	 * the job order, and cwltool's own --outdir all live there, so everything a run needs is
	 * inside the single mounted workspace folder
	 */
	protected static final String CWL_RUN_FOLDER_NAME = ".cwl-run";

	/**
	 * Name of the CWL document rewritten at deploy time (e.g. with WASDI's own built image
	 * reference), the one actually used at run time
	 */
	protected static final String RESOLVED_CWL_FILE_NAME = "resolved-app-package.cwl";

	protected static final String JOB_ORDER_FILE_NAME = "job-order.yml";

	protected static final String CWLTOOL_RESULT_FILE_NAME = "cwltool-result.json";

	/**
	 * Name/version of the WASDI-authored image that runs cwltool itself (built/published by a
	 * separate CI pipeline, not by this engine)
	 */
	protected static final String CWL_RUNNER_IMAGE_NAME = "wasdi-cwl";
	protected static final String CWL_RUNNER_IMAGE_VERSION = "latest";

	public OgcAppPackageProcessorEngine() {
		super();
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.OGC_APP_PACKAGE);

		// cwltool drives its own containers one-shot: this engine never runs a long-lived REST server
		m_bRunAfterDeploy = false;
	}

	/**
	 * Deploys the Application Package: unzips it, parses the CWL document, derives the WASDI
	 * parameter sample from the Workflow inputs, and either builds the user provided Dockerfile
	 * (rewriting every CommandLineTool's dockerPull to point at it) or validates that every
	 * CommandLineTool already references an external, already published image.
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
			List<Map<String, Object>> aoCommandLineToolNodes = CwlApplicationPackageUtils.getAllCommandLineToolNodes(oCwlDocument);

			if (oWorkflowNode == null || aoCommandLineToolNodes.isEmpty()) {
				return logDeployErrorAndClean("the CWL document must contain a Workflow class and at least one CommandLineTool class (OGC BP Requirement 7)", bFirstDeploy);
			}

			for (Map<String, Object> oCommandLineToolNode : aoCommandLineToolNodes) {
				if (Utils.isNullOrEmpty(String.valueOf(oCommandLineToolNode.get("id"))) || oCommandLineToolNode.get("baseCommand") == null) {
					return logDeployErrorAndClean("every CommandLineTool must declare an id and a baseCommand (OGC BP Requirement 8)", bFirstDeploy);
				}
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

				// A single embedded Dockerfile is used for every CommandLineTool of this package (v1 limitation)
				for (Map<String, Object> oCommandLineToolNode : aoCommandLineToolNodes) {
					CwlApplicationPackageUtils.setDockerPull(oCommandLineToolNode, sPushedImageAddress);
				}
			}
			else {
				for (Map<String, Object> oCommandLineToolNode : aoCommandLineToolNodes) {
					if (Utils.isNullOrEmpty(CwlApplicationPackageUtils.getDockerPull(oCommandLineToolNode))) {
						return logDeployErrorAndClean("no Dockerfile in the package and a CommandLineTool has no DockerRequirement.dockerPull: impossible to get a container image", bFirstDeploy);
					}
				}

				processWorkspaceLog("Application Package references external image(s): cwltool will pull them at run time");
			}

			if (!CwlApplicationPackageUtils.writeYamlDocument(oCwlDocument, new File(sProcessorFolder, RESOLVED_CWL_FILE_NAME))) {
				return logDeployErrorAndClean("impossible to write the resolved CWL document", bFirstDeploy);
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
	 * Runs the Application Package: stages-in the STAC Directory inputs, builds the CWL job
	 * order, and invokes cwltool (inside the "wasdi-cwl" image) once for the whole Workflow -
	 * cwltool itself resolves steps, scatter, expressions and subworkflows. Stages-out whatever
	 * cwltool reports as the Workflow's actual outputs.
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

		File oHostRunFolder = null;

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

			File oResolvedCwlFile = new File(sProcessorFolder, RESOLVED_CWL_FILE_NAME);

			if (!oResolvedCwlFile.exists()) {
				// Backward compatibility: a processor deployed before this engine started writing a resolved copy
				oResolvedCwlFile = CwlApplicationPackageUtils.findCwlFile(sProcessorFolder);
			}

			if (oResolvedCwlFile == null) {
				processWorkspaceLog("No CWL file found for this Application Package");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			Map<String, Object> oCwlDocument = CwlApplicationPackageUtils.loadCwlDocument(oResolvedCwlFile);
			Map<String, Object> oWorkflowNode = CwlApplicationPackageUtils.getWorkflowNode(oCwlDocument);
			String sWorkflowId = CwlApplicationPackageUtils.getWorkflowId(oWorkflowNode);

			if (oWorkflowNode == null || Utils.isNullOrEmpty(sWorkflowId)) {
				processWorkspaceLog("Impossible to parse the Workflow from the CWL document");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			// The job values are keyed by the Workflow's own input ids: cwltool resolves the rest (steps, scatter, ...) itself
			Map<String, Object> oJobInputValues = decodeJobInputValues(oParameter.getJson());

			String sHostWorkspacePath = PathsConfig.getWorkspacePath(oParameter);

			if (!stageInDirectoryInputs(oWorkflowNode, oJobInputValues, sHostWorkspacePath)) {
				processWorkspaceLog("Impossible to stage-in one of the STAC inputs");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			Map<String, Object> oJobOrder = CwlApplicationPackageUtils.buildJobOrder(oWorkflowNode, oJobInputValues);

			// Everything a run needs (CWL document, job order, cwltool's own --outdir) lives in a single per-run workspace subfolder
			String sHostRunFolder = sHostWorkspacePath + CWL_RUN_FOLDER_NAME + "/" + oParameter.getProcessObjId() + "/";
			oHostRunFolder = new File(sHostRunFolder);
			oHostRunFolder.mkdirs();
			// The wasdi-cwl container runs as a fixed numeric uid (see DockerUtils.run), not necessarily the launcher's own user
			oHostRunFolder.setWritable(true, false);
			oHostRunFolder.setExecutable(true, false);

			File oRunCwlFile = new File(oHostRunFolder, oResolvedCwlFile.getName());
			FileUtils.copyFile(oResolvedCwlFile, oRunCwlFile);

			File oJobOrderFile = new File(oHostRunFolder, JOB_ORDER_FILE_NAME);

			if (!CwlApplicationPackageUtils.writeYamlDocument(oJobOrder, oJobOrderFile)) {
				processWorkspaceLog("Impossible to write the CWL job order");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			String sContainerRunFolder = sHostRunFolder.replaceAll("/+$", "");
			String sContainerCwlFile = sContainerRunFolder + "/" + oRunCwlFile.getName();
			String sContainerJobOrderFile = sContainerRunFolder + "/" + JOB_ORDER_FILE_NAME;
			String sContainerResultFile = sContainerRunFolder + "/" + CWLTOOL_RESULT_FILE_NAME;

			// cwltool creates scratch/staging folders of its own (by default under /tmp) and, using the shared
			// docker.sock, asks the HOST Docker daemon to bind-mount them into the sibling CommandLineTool
			// containers it starts. The daemon resolves those paths against the real host filesystem, so any
			// path cwltool computes (its own tmp dirs, and every staged Directory input) must be a real host
			// path, not a WASDI-internal container remap. So the workspace is mounted here at the SAME
			// absolute path as on the host (identity mount), and cwltool's own scratch dirs are pinned inside it.
			String sHostTmpFolder = sHostRunFolder + "tmp/";
			File oHostTmpFolder = new File(sHostTmpFolder);
			oHostTmpFolder.mkdirs();
			oHostTmpFolder.setWritable(true, false);
			oHostTmpFolder.setExecutable(true, false);

			ArrayList<String> asAdditionalMountPoints = new ArrayList<>();
			String sIdentityWorkspacePath = sHostWorkspacePath.replaceAll("/+$", "");
			asAdditionalMountPoints.add(sIdentityWorkspacePath + ":" + sIdentityWorkspacePath);

			String sDockerApiAddress = WasdiConfig.Current.dockers.internalDockerAPIAddress;

			if (!Utils.isNullOrEmpty(sDockerApiAddress) && sDockerApiAddress.startsWith("unix://")) {
				String sDockerSocketPath = sDockerApiAddress.substring("unix://".length());
				// The wasdi-cwl container needs the same "sibling containers" access to the host Docker Engine the launcher itself already has
				asAdditionalMountPoints.add(sDockerSocketPath + ":" + sDockerSocketPath);
			}
			else {
				WasdiLog.warnLog("OgcAppPackageProcessorEngine.run: the Docker API address is not a unix socket, cwltool may not be able to reach the Docker Engine");
			}

			// wasdi-cwl is a host-provisioned "system image" (like wasdi-gdal, wasdi-sen2cor, ...), never pulled from a registry
			DockerUtils oDockerUtils = new DockerUtils(oProcessor, m_oParameter, sProcessorFolder, "", m_oProcessWorkspaceLogger);

			String sCwltoolCommand = "cwltool --tmpdir-prefix " + sHostTmpFolder + " --tmp-outdir-prefix " + sHostTmpFolder
					+ " --outdir " + sContainerRunFolder + " " + sContainerCwlFile + "#" + sWorkflowId + " " + sContainerJobOrderFile + " > " + sContainerResultFile;
			List<String> asCommand = List.of("sh", "-c", sCwltoolCommand);

			processWorkspaceLog("Starting the Application (cwltool)");
			String sDecodedParams = URLDecoder.decode(oParameter.getJson(), java.nio.charset.StandardCharsets.UTF_8.toString());
			processWorkspaceLog("Input Params " + sDecodedParams );
			WasdiLog.debugLog("OgcAppPackageProcessorEngine.run: cwltool command " + sCwltoolCommand);

			String sContainerId = oDockerUtils.run(CWL_RUNNER_IMAGE_NAME, CWL_RUNNER_IMAGE_VERSION, asCommand, true, asAdditionalMountPoints, false, sHostWorkspacePath, sContainerRunFolder);

			if (Utils.isNullOrEmpty(sContainerId)) {
				processWorkspaceLog("Impossible to start the cwltool container");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			ProcessStatus oFinalStatus = waitForContainerCompletion(oDockerUtils, sContainerId, oProcessor, oProcessWorkspaceRepository, oProcessWorkspace);

			// Surface cwltool's own progress/log output to the user, same as the docker build logs do
			String sContainerLogs = oDockerUtils.getContainerLogsByContainerId(sContainerId);

			if (!Utils.isNullOrEmpty(sContainerLogs)) {
				processWorkspaceLog("cwltool output:");

				// One process log row per call: split so each container log line shows up as its own row, not one giant blob
				for (String sLogLine : sContainerLogs.split("\\r?\\n")) {
					if (!Utils.isNullOrEmpty(sLogLine)) {
						processWorkspaceLog(sLogLine);
					}
				}
			}

			WasdiLog.debugLog("OgcAppPackageProcessorEngine.run: cwltool output " + sContainerLogs);

			if (oFinalStatus == ProcessStatus.DONE) {

				Map<String, Object> oCwltoolResult = StacStageOutUtils.parseCwltoolResult(new File(oHostRunFolder, CWLTOOL_RESULT_FILE_NAME));

				if (oCwltoolResult == null) {
					processWorkspaceLog("Impossible to read the cwltool result, cannot stage-out the outputs");
					oFinalStatus = ProcessStatus.ERROR;
				}
				else {
					stageOutOutputs(oHostRunFolder, sHostWorkspacePath, oParameter, oCwltoolResult);
				}
			}

			try {
				oDockerUtils.removeContainer(sContainerId, true);
			}
			catch (Exception oRemoveEx) {
				WasdiLog.warnLog("OgcAppPackageProcessorEngine.run: impossible to remove the container " + sContainerId);
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, oFinalStatus, 100);

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
			// This is just cwltool's scratch working dir: its content was already copied into the workspace above
			if (oHostRunFolder != null) {
				try {
					FileUtils.deleteDirectory(oHostRunFolder);
				}
				catch (Exception oCleanupEx) {
					WasdiLog.warnLog("OgcAppPackageProcessorEngine.run: impossible to clean up the run folder " + oHostRunFolder.getAbsolutePath());
				}
			}

			if (oProcessWorkspace != null) {
				m_oProcessWorkspace.setStatus(oProcessWorkspace.getStatus());
			}
		}
	}

	/**
	 * Resolves every Directory-typed Workflow input by staging-in the STAC Item(s) it references
	 * (see {@link StacStageInUtils}), and replaces its raw job value (a STAC Item url, or a list
	 * of them for a Directory[] input) with the resulting local path(s).
	 */
	@SuppressWarnings("unchecked")
	protected boolean stageInDirectoryInputs(Map<String, Object> oWorkflowNode, Map<String, Object> oJobInputValues, String sHostWorkspacePath) {

		List<String> asDirectoryInputIds = CwlApplicationPackageUtils.getInputIdsOfType(oWorkflowNode, "Directory");

		for (String sInputId : asDirectoryInputIds) {

			Object oRawValue = oJobInputValues.get(sInputId);

			if (oRawValue == null) {
				WasdiLog.warnLog("OgcAppPackageProcessorEngine.stageInDirectoryInputs: no value provided for Directory input [" + sInputId + "], skipping it");
				continue;
			}

			if (oRawValue instanceof List) {

				// A Directory[] input: one STAC Item url per array element, staged into its own indexed subfolder.
				// Duplicate urls within the same array are staged only once and their folder reused, to avoid re-downloading the same assets.
				List<Object> aoRawValues = (List<Object>) oRawValue;
				List<String> asStagedPaths = new ArrayList<>();
				Map<String, String> oAlreadyStagedByUrl = new HashMap<>();

				for (int iIndex = 0; iIndex < aoRawValues.size(); iIndex++) {

					String sItemUrl = extractStacItemUrl(aoRawValues.get(iIndex));

					String sAlreadyStagedPath = oAlreadyStagedByUrl.get(sItemUrl);

					if (sAlreadyStagedPath != null) {
						asStagedPaths.add(sAlreadyStagedPath);
						continue;
					}

					String sHostStagingFolder = sHostWorkspacePath + STAC_STAGE_IN_FOLDER_NAME + "/" + sInputId + "/" + iIndex;

					File oStagedFolder = StacStageInUtils.stageInStacItem(sItemUrl, sHostStagingFolder, m_oProcessWorkspaceLogger);

					if (oStagedFolder == null) {
						WasdiLog.errorLog("OgcAppPackageProcessorEngine.stageInDirectoryInputs: impossible to stage-in input [" + sInputId + "][" + iIndex + "]");
						return false;
					}

					String sStagedPath = oStagedFolder.getAbsolutePath();
					oAlreadyStagedByUrl.put(sItemUrl, sStagedPath);
					asStagedPaths.add(sStagedPath);
				}

				oJobInputValues.put(sInputId, asStagedPaths);
				continue;
			}

			String sHostStagingFolder = sHostWorkspacePath + STAC_STAGE_IN_FOLDER_NAME + "/" + sInputId;

			File oStagedFolder = StacStageInUtils.stageInStacItem(extractStacItemUrl(oRawValue), sHostStagingFolder, m_oProcessWorkspaceLogger);

			if (oStagedFolder == null) {
				WasdiLog.errorLog("OgcAppPackageProcessorEngine.stageInDirectoryInputs: impossible to stage-in input [" + sInputId + "]");
				return false;
			}

			// The workspace is mounted at an identity path for the wasdi-cwl container (see run()), so the host path is also the container path
			oJobInputValues.put(sInputId, oStagedFolder.getAbsolutePath());
		}

		return true;
	}

	/**
	 * Extracts the STAC Item url out of a raw Directory-input value: accepts a plain string, or an OGC
	 * API Processes "complex value" object carrying it under "href" or "value" (possibly nested).
	 */
	@SuppressWarnings("unchecked")
	protected static String extractStacItemUrl(Object oRawValue) {

		if (oRawValue instanceof Map) {
			Map<String, Object> oValueMap = (Map<String, Object>) oRawValue;

			if (oValueMap.containsKey("href")) return extractStacItemUrl(oValueMap.get("href"));
			if (oValueMap.containsKey("value")) return extractStacItemUrl(oValueMap.get("value"));
		}

		return String.valueOf(oRawValue);
	}

	/**
	 * Stages-out the run's outputs, using cwltool's own result document as the authoritative
	 * list of produced files (see {@link StacStageOutUtils#collectCwltoolOutputFiles}). If the
	 * Application also wrote its own local STAC catalog (OGC BP Req 5), its bbox metadata is
	 * reused for the matching files. Every resolved file is copied into the workspace and
	 * registered as a WASDI product.
	 */
	protected void stageOutOutputs(File oHostRunFolder, String sHostWorkspacePath, ProcessorParameter oParameter, Map<String, Object> oCwltoolResult) {

		try {
			Set<File> aoOutputFiles = StacStageOutUtils.collectCwltoolOutputFiles(oCwltoolResult);

			if (aoOutputFiles.isEmpty()) {
				processWorkspaceLog("cwltool reported no output files to stage-out");
				return;
			}

			// Best effort: reuse the bbox metadata from the Application's own local STAC catalog, if any
			Map<String, String> oBboxByFileName = new HashMap<>();
			List<StacStageOutUtils.StagedOutFile> aoStacStagedOutFiles = StacStageOutUtils.parseLocalOutputCatalog(oHostRunFolder, m_oProcessWorkspaceLogger);

			if (aoStacStagedOutFiles != null) {
				for (StacStageOutUtils.StagedOutFile oStagedOutFile : aoStacStagedOutFiles) {
					oBboxByFileName.put(oStagedOutFile.file.getName(), oStagedOutFile.bbox);
				}
			}

			for (File oOutputFile : aoOutputFiles) {

				// This is WASDI's own stage-in/out bookkeeping, not an application result
				if (oOutputFile.getName().equals("catalog.json")) continue;
				if (oOutputFile.getName().equals(JOB_ORDER_FILE_NAME)) continue;
				if (oOutputFile.getName().equals(CWLTOOL_RESULT_FILE_NAME)) continue;

				try {
					File oWorkspaceFile = new File(sHostWorkspacePath, oOutputFile.getName());
					FileUtils.copyFile(oOutputFile, oWorkspaceFile);

					String sBbox = oBboxByFileName.getOrDefault(oOutputFile.getName(), "");
					addOutputProductToWorkspace(oWorkspaceFile, oParameter, sBbox);

					processWorkspaceLog("Output added to the workspace: " + oWorkspaceFile.getName());
				}
				catch (Exception oEx) {
					WasdiLog.errorLog("OgcAppPackageProcessorEngine.stageOutOutputs: impossible to stage-out " + oOutputFile.getAbsolutePath(), oEx);
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcAppPackageProcessorEngine.stageOutOutputs: exception", oEx);
		}
	}

	/**
	 * Registers a staged-out file as a WASDI product (DownloadedFile + ProductWorkspace),
	 * same DB pattern used by the Ingest operation, without going through its REST/queue flow.
	 * 
	 * @param oFile Staged-out file, already copied into the workspace
	 * @param oParameter Processor Parameter of the run
	 * @param sBbox Bounding box in WASDI's own "north,west,south,east" format, or empty if unknown
	 */
	protected void addOutputProductToWorkspace(File oFile, ProcessorParameter oParameter, String sBbox) {
		try {
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			DownloadedFile oExistingDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(oFile.getAbsolutePath());

			ProductViewModel oProductViewModel = null;

			try {
				WasdiProductReader oReader = WasdiProductReaderFactory.getProductReader(oFile);
				oProductViewModel = oReader.getProductViewModel();
			}
			catch (Exception oReadEx) {
				WasdiLog.warnLog("OgcAppPackageProcessorEngine.addOutputProductToWorkspace: impossible to read the product metadata for " + oFile.getName() + ", registering it with minimal info");
			}

			if (oProductViewModel == null) {
				oProductViewModel = new ProductViewModel();
				oProductViewModel.setName(oFile.getName());
				oProductViewModel.setFileName(oFile.getName());
			}

			if (oExistingDownloadedFile == null) {
				DownloadedFile oDownloadedFile = new DownloadedFile();
				oDownloadedFile.setFileName(oFile.getName());
				oDownloadedFile.setFilePath(oFile.getAbsolutePath());
				oDownloadedFile.setProductViewModel(oProductViewModel);
				oDownloadedFile.setRefDate(new Date());
				oDownloadedFile.setCategory(DownloadedFileCategory.COMPUTED.name());

				if (!oDownloadedFilesRepository.insertDownloadedFile(oDownloadedFile)) {
					WasdiLog.errorLog("OgcAppPackageProcessorEngine.addOutputProductToWorkspace: impossible to insert the downloaded file entry for " + oFile.getName());
				}
			}

			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();

			if (!oProductWorkspaceRepository.existsProductWorkspace(oFile.getAbsolutePath(), oParameter.getWorkspace())) {
				ProductWorkspace oProductWorkspace = new ProductWorkspace();
				oProductWorkspace.setProductName(oFile.getAbsolutePath());
				oProductWorkspace.setWorkspaceId(oParameter.getWorkspace());

				if (!Utils.isNullOrEmpty(sBbox)) {
					oProductWorkspace.setBbox(sBbox);
				}

				if (!oProductWorkspaceRepository.insertProductWorkspace(oProductWorkspace)) {
					WasdiLog.errorLog("OgcAppPackageProcessorEngine.addOutputProductToWorkspace: impossible to link " + oFile.getName() + " to the workspace");
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcAppPackageProcessorEngine.addOutputProductToWorkspace: exception", oEx);
		}
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
	 * Dockerfile (rewriting every CommandLineTool's dockerPull to point at it), otherwise just
	 * validates every CommandLineTool still references an external image. Either way, refreshes
	 * the parameter sample and the resolved CWL document.
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
		List<Map<String, Object>> aoCommandLineToolNodes = CwlApplicationPackageUtils.getAllCommandLineToolNodes(oCwlDocument);

		if (oWorkflowNode == null || aoCommandLineToolNodes.isEmpty()) {
			WasdiLog.errorLog("OgcAppPackageProcessorEngine.redeploy: impossible to parse the Workflow/CommandLineTool from the CWL document");
			return false;
		}

		// Refresh the parameter sample in case the user replaced the CWL document
		oProcessor.setParameterSample(CwlApplicationPackageUtils.buildParameterSampleJson(oWorkflowNode));

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

			m_sDockerImageName = oDockerUtils.build();

			if (Utils.isNullOrEmpty(m_sDockerImageName)) {
				WasdiLog.errorLog("OgcAppPackageProcessorEngine.redeploy: the docker build returned an empty image name, something went wrong");
				return false;
			}

			String sPushedImageAddress = pushImageInRegisters(oProcessor);

			if (Utils.isNullOrEmpty(sPushedImageAddress)) {
				WasdiLog.errorLog("OgcAppPackageProcessorEngine.redeploy: impossible to push the built image");
				return false;
			}

			for (Map<String, Object> oCommandLineToolNode : aoCommandLineToolNodes) {
				CwlApplicationPackageUtils.setDockerPull(oCommandLineToolNode, sPushedImageAddress);
			}
		}
		else {
			for (Map<String, Object> oCommandLineToolNode : aoCommandLineToolNodes) {
				if (Utils.isNullOrEmpty(CwlApplicationPackageUtils.getDockerPull(oCommandLineToolNode))) {
					WasdiLog.errorLog("OgcAppPackageProcessorEngine.redeploy: a CommandLineTool has no DockerRequirement.dockerPull found in the CWL document");
					return false;
				}
			}

			String sNewVersion = StringUtils.incrementIntegerString(oProcessor.getVersion());
			oProcessor.setVersion(sNewVersion);
		}

		oProcessorRepository.updateProcessor(oProcessor);

		return CwlApplicationPackageUtils.writeYamlDocument(oCwlDocument, new File(sProcessorFolder, RESOLVED_CWL_FILE_NAME));
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
	 * A CWL Application Package has no notion of "add a library": redeploy is the way to update it
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
