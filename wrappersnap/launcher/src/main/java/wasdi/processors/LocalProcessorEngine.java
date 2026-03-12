package wasdi.processors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import wasdi.LauncherMain;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;

/**
 * Local Processor Engine.
 * Runs user-uploaded Python applications directly on the host (or mini-WASDI container)
 * using a per-processor Python virtual environment managed by uv.
 * No Docker image is built or required.
 */
public class LocalProcessorEngine extends WasdiProcessorEngine {
	
	public LocalProcessorEngine() {
		super();
		
		// Set the folder
		if (!m_sDockerTemplatePath.endsWith("/")) m_sDockerTemplatePath += "/";
		m_sDockerTemplatePath += ProcessorTypes.getTemplateFolder(ProcessorTypes.LOCAL_PYTHON312);
	}	
	

	/**
	 * Name of the venv subfolder created inside the processor folder.
	 */
	private static final String VENV_FOLDER = "venv";

	/**
	 * Name of the pip requirements file expected in the processor zip.
	 */
	private static final String PIP_FILE = "pip.txt";

	/**
	 * Entry-point script expected in the processor zip.
	 */
	private static final String MAIN_SCRIPT = "wasdiProcessorExecutor.py";

	/**
	 * Derives the Python version string (e.g. "3.12") from the processor type constant.
	 * Add new mappings here when new LOCAL_PYTHON* types are introduced.
	 *
	 * @param sProcessorType the processor type string
	 * @return Python version string, or null if the type is not a known local-python type
	 */
	private String getLocalPythonVersion(String sProcessorType) {
		if (ProcessorTypes.LOCAL_PYTHON312.equals(sProcessorType)) return "3.12";
		return null;
	}

	/**
	 * Returns the path of the Python executable inside the venv, handling
	 * the Linux (bin/python) vs Windows (Scripts/python.exe) difference.
	 *
	 * @param sProcessorFolder processor root folder (trailing separator expected)
	 * @return absolute path to the venv Python executable
	 */
	private String getVenvPythonExecutable(String sProcessorFolder) {
		boolean bWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
		if (bWindows) {
			return sProcessorFolder + VENV_FOLDER + File.separator + "Scripts" + File.separator + "python.exe";
		}
		return sProcessorFolder + VENV_FOLDER + File.separator + "bin" + File.separator + "python";
	}

	/**
	 * Resolves the actual Python executable in the venv. Depending on platform
	 * and venv implementation, the interpreter can be named python, python3,
	 * or pythonX.Y.
	 */
	private String findVenvPythonExecutable(String sProcessorFolder, String sPythonVersion) {
		boolean bWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
		List<String> asCandidates = new ArrayList<>();

		if (bWindows) {
			asCandidates.add(sProcessorFolder + VENV_FOLDER + File.separator + "Scripts" + File.separator + "python.exe");
			asCandidates.add(sProcessorFolder + VENV_FOLDER + File.separator + "Scripts" + File.separator + "python3.exe");
		} else {
			String sBinFolder = sProcessorFolder + VENV_FOLDER + File.separator + "bin" + File.separator;
			asCandidates.add(sBinFolder + "python");
			asCandidates.add(sBinFolder + "python3");
			if (!Utils.isNullOrEmpty(sPythonVersion)) asCandidates.add(sBinFolder + "python" + sPythonVersion);
		}

		for (String sCandidate : asCandidates) {
			if (new File(sCandidate).exists()) return sCandidate;
		}

		return null;
	}

	/**
	 * Fallback path: create the venv with stdlib `venv` using available Python
	 * launchers, forcing copies so the environment is self-contained.
	 */
	private boolean createVenvWithStdlib(String sVenvPath, String sPythonVersion) {
		boolean bWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
		List<List<String>> aoBaseCommands = new ArrayList<>();

		if (bWindows) {
			if (!Utils.isNullOrEmpty(sPythonVersion)) {
				List<String> asPyLauncher = new ArrayList<>();
				asPyLauncher.add("py");
				asPyLauncher.add("-" + sPythonVersion);
				aoBaseCommands.add(asPyLauncher);
			}
			List<String> asPython = new ArrayList<>();
			asPython.add("python");
			aoBaseCommands.add(asPython);
		} else {
			if (!Utils.isNullOrEmpty(sPythonVersion)) {
				List<String> asVersioned = new ArrayList<>();
				asVersioned.add("python" + sPythonVersion);
				aoBaseCommands.add(asVersioned);
			}
			List<String> asPython3 = new ArrayList<>();
			asPython3.add("python3");
			aoBaseCommands.add(asPython3);
			List<String> asPython = new ArrayList<>();
			asPython.add("python");
			aoBaseCommands.add(asPython);
		}

		for (List<String> asBaseCmd : aoBaseCommands) {
			List<String> asCmd = new ArrayList<>(asBaseCmd);
			asCmd.add("-m");
			asCmd.add("venv");
			asCmd.add("--clear");
			asCmd.add("--copies");
			asCmd.add(sVenvPath);

			ShellExecReturn oResult = RunTimeUtils.shellExec(asCmd, true, true, true);
			if (oResult.isOperationOk() && oResult.getOperationReturn() == 0) {
				WasdiLog.debugLog("LocalProcessorEngine.createVenvWithStdlib: created venv using command " + asBaseCmd.get(0));
				return true;
			}
		}

		return false;
	}

	/**
	 * Deploy: unzips the processor, creates a uv-managed venv for the requested
	 * Python version, and installs packages from pip.txt.
	 * The zip is assumed to already be present in the processor folder.
	 */
	@Override
	public boolean deploy(ProcessorParameter oParameter) {

		if (oParameter == null) {
			WasdiLog.errorLog("LocalProcessorEngine.deploy: oParameter is null");
			return false;
		}

		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		ProcessWorkspace oProcessWorkspace = m_oProcessWorkspace;

		try {
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			String sProcessorName = oParameter.getName();
			String sProcessorId = oParameter.getProcessorID();
			String sProcessorType = oParameter.getProcessorType();

			String sPythonVersion = getLocalPythonVersion(sProcessorType);
			if (sPythonVersion == null) {
				WasdiLog.errorLog("LocalProcessorEngine.deploy: unsupported processor type " + sProcessorType);
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);

			processWorkspaceLog("LocalProcessorEngine: deploying " + sProcessorName + " (Python " + sPythonVersion + ")");

			// The zip file is named <processorId>.zip and is already in the processor folder
			String sZipFileName = sProcessorId + ".zip";

			WasdiLog.debugLog("LocalProcessorEngine.deploy: unzipping " + sZipFileName);
			processWorkspaceLog("Unzipping processor archive...");

			if (!unzipProcessor(sProcessorFolder, sZipFileName, sProcessorId)) {
				WasdiLog.errorLog("LocalProcessorEngine.deploy: unzip failed");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 30);
			
            // Copy Docker template files in the processor folder
            File oDockerTemplateFolder = new File(m_sDockerTemplatePath);
            File oProcessorFolder = new File(sProcessorFolder);

            FileUtils.copyDirectory(oDockerTemplateFolder, oProcessorFolder);			

			// Create the venv with uv: uv venv --python <version> <processorFolder>/venv
			String sVenvPath = sProcessorFolder + VENV_FOLDER;
			WasdiLog.debugLog("LocalProcessorEngine.deploy: creating venv at " + sVenvPath);
			processWorkspaceLog("Creating Python " + sPythonVersion + " virtual environment...");

			List<String> asVenvCmd = new ArrayList<>();
			asVenvCmd.add("uv");
			asVenvCmd.add("venv");
			asVenvCmd.add("--seed");
			asVenvCmd.add("--python");
			asVenvCmd.add(sPythonVersion);
			asVenvCmd.add(sVenvPath);

			ShellExecReturn oVenvResult = RunTimeUtils.shellExec(asVenvCmd, true, true, true);

			if (!oVenvResult.isOperationOk() || oVenvResult.getOperationReturn() != 0) {
				WasdiLog.errorLog("LocalProcessorEngine.deploy: uv venv failed. Output: " + oVenvResult.getOperationLogs());
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			String sVenvPython = findVenvPythonExecutable(sProcessorFolder, sPythonVersion);
			if (Utils.isNullOrEmpty(sVenvPython)) {
				WasdiLog.warnLog("LocalProcessorEngine.deploy: uv created venv without a visible interpreter. Trying stdlib venv fallback with --copies...");
				processWorkspaceLog("Retrying virtual environment creation with stdlib venv...");

				if (!createVenvWithStdlib(sVenvPath, sPythonVersion)) {
					WasdiLog.errorLog("LocalProcessorEngine.deploy: stdlib venv fallback failed at " + sVenvPath);
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
					return false;
				}

				sVenvPython = findVenvPythonExecutable(sProcessorFolder, sPythonVersion);
				if (Utils.isNullOrEmpty(sVenvPython)) {
					WasdiLog.errorLog("LocalProcessorEngine.deploy: no Python interpreter found in venv even after stdlib fallback at " + sVenvPath);
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
					return false;
				}
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 60);

			// Install packages if pip.txt exists
			String sPipFile = sProcessorFolder + PIP_FILE;
			if (new File(sPipFile).exists()) {
				WasdiLog.debugLog("LocalProcessorEngine.deploy: installing packages from " + sPipFile);
				processWorkspaceLog("Installing packages from pip.txt...");

				List<String> asPipCmd = new ArrayList<>();
				asPipCmd.add(sVenvPython);
				asPipCmd.add("-m");
				asPipCmd.add("pip");
				asPipCmd.add("install");
				asPipCmd.add("-r");
				asPipCmd.add(sPipFile);

				ShellExecReturn oPipResult = RunTimeUtils.shellExec(asPipCmd, true, true, true);

				if (!oPipResult.isOperationOk() || oPipResult.getOperationReturn() != 0) {
					WasdiLog.errorLog("LocalProcessorEngine.deploy: pip install failed. Output: " + oPipResult.getOperationLogs());
					LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
					return false;
				}
			} else {
				WasdiLog.debugLog("LocalProcessorEngine.deploy: no pip.txt found, skipping package installation");
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
			processWorkspaceLog("Deploy completed successfully.");

			WasdiLog.debugLog("LocalProcessorEngine.deploy: done for " + sProcessorName);
			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("LocalProcessorEngine.deploy: exception", oEx);
			try {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
			} catch (Exception oInnerEx) {
				WasdiLog.errorLog("LocalProcessorEngine.deploy: exception updating status", oInnerEx);
			}
			return false;
		}
	}

	/**
	 * Run: executes myProcessor.py inside the processor venv, passing the
	 * encoded JSON parameters as a command-line argument (same convention
	 * used by the wasdi Python library).
	 */
	@Override
	public boolean run(ProcessorParameter oParameter) {

		if (oParameter == null) {
			WasdiLog.errorLog("LocalProcessorEngine.run: oParameter is null");
			return false;
		}

		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		ProcessWorkspace oProcessWorkspace = m_oProcessWorkspace;

		try {
			// Ensure the workspace folder exists
			String sWorkspacePath = PathsConfig.getWorkspacePath(oParameter);
			File oWorkspacePath = new File(sWorkspacePath);
			if (!oWorkspacePath.exists()) oWorkspacePath.mkdirs();

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			String sProcessorName = oParameter.getName();
			String sProcessorFolder = PathsConfig.getProcessorFolder(sProcessorName);

			String sMainScript = sProcessorFolder + MAIN_SCRIPT;
			if (!new File(sMainScript).exists()) {
				WasdiLog.errorLog("LocalProcessorEngine.run: entry point not found: " + sMainScript);
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			String sPythonVersion = getLocalPythonVersion(oParameter.getProcessorType());
			String sVenvPython = findVenvPythonExecutable(sProcessorFolder, sPythonVersion);
			if (Utils.isNullOrEmpty(sVenvPython) || !new File(sVenvPython).exists()) {
				WasdiLog.errorLog("LocalProcessorEngine.run: venv python not found in " + sProcessorFolder + VENV_FOLDER + " - was the processor deployed?");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			// Pass encoded JSON params as first argument, matching wasdi Python lib convention
			String sEncodedJson = oParameter.getJson();
			if (Utils.isNullOrEmpty(sEncodedJson)) sEncodedJson = "{}";

			WasdiLog.debugLog("LocalProcessorEngine.run: executing " + sMainScript);
			processWorkspaceLog("Starting " + sProcessorName + "...");

			List<String> asRunCmd = new ArrayList<>();
			asRunCmd.add(sVenvPython);
			asRunCmd.add(sMainScript);
			
			Map<String, String> aoEnv = new HashMap<>();
			aoEnv.put("WASDI_ONESHOT_ENCODED_PARAMS", sEncodedJson);
			aoEnv.put("WASDI_USER", oParameter.getUserId());
			aoEnv.put("WASDI_SESSION_ID", oParameter.getSessionID());
			aoEnv.put("WASDI_PROCESS_WORKSPACE_ID", oParameter.getProcessObjId());
			aoEnv.put("WASDI_WORKSPACE_ID", oParameter.getWorkspace());
			aoEnv.put("WASDI_WEBSERVER_URL", WasdiConfig.Current.baseUrl);
			aoEnv.put("WASDI_BASE_PATH", WasdiConfig.Current.paths.downloadRootPath);

			ShellExecReturn oRunResult = RunTimeUtils.shellExec(asRunCmd, true, true, true, aoEnv);

			WasdiLog.debugLog("LocalProcessorEngine.run: process exited with code " + oRunResult.getOperationReturn());
			if (!Utils.isNullOrEmpty(oRunResult.getOperationLogs())) {
				WasdiLog.debugLog("LocalProcessorEngine.run output:\n" + oRunResult.getOperationLogs());
			}

			if (!oRunResult.isOperationOk() || oRunResult.getOperationReturn() != 0) {
				WasdiLog.errorLog("LocalProcessorEngine.run: processor exited with non-zero code " + oRunResult.getOperationReturn());
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
				return false;
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("LocalProcessorEngine.run: exception", oEx);
			try {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 100);
			} catch (Exception oInnerEx) {
				WasdiLog.errorLog("LocalProcessorEngine.run: exception updating status", oInnerEx);
			}
			return false;
		}
	}

	/**
	 * Delete: removes the entire processor folder (venv + code).
	 */
	@Override
	public boolean delete(ProcessorParameter oParameter) {

		if (oParameter == null) {
			WasdiLog.errorLog("LocalProcessorEngine.delete: oParameter is null");
			return false;
		}

		try {
			String sProcessorFolder = PathsConfig.getProcessorFolder(oParameter.getName());
			File oProcessorFolder = new File(sProcessorFolder);

			if (oProcessorFolder.exists()) {
				org.apache.commons.io.FileUtils.deleteDirectory(oProcessorFolder);
				WasdiLog.debugLog("LocalProcessorEngine.delete: deleted " + sProcessorFolder);
			} else {
				WasdiLog.debugLog("LocalProcessorEngine.delete: folder not found, nothing to delete");
			}
			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("LocalProcessorEngine.delete: exception", oEx);
			return false;
		}
	}

	/**
	 * Redeploy: wipe the existing venv and re-run deploy.
	 */
	@Override
	public boolean redeploy(ProcessorParameter oParameter) {

		if (oParameter == null) {
			WasdiLog.errorLog("LocalProcessorEngine.redeploy: oParameter is null");
			return false;
		}

		try {
			String sProcessorFolder = PathsConfig.getProcessorFolder(oParameter.getName());
			File oVenvFolder = new File(sProcessorFolder + VENV_FOLDER);

			if (oVenvFolder.exists()) {
				WasdiLog.debugLog("LocalProcessorEngine.redeploy: removing existing venv");
				org.apache.commons.io.FileUtils.deleteDirectory(oVenvFolder);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("LocalProcessorEngine.redeploy: exception removing venv", oEx);
			return false;
		}

		return deploy(oParameter);
	}

	/**
	 * Library update: re-runs pip install from pip.txt inside the existing venv.
	 */
	@Override
	public boolean libraryUpdate(ProcessorParameter oParameter) {

		if (oParameter == null) {
			WasdiLog.errorLog("LocalProcessorEngine.libraryUpdate: oParameter is null");
			return false;
		}

		ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		ProcessWorkspace oProcessWorkspace = m_oProcessWorkspace;

		try {
			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.RUNNING, 0);

			String sProcessorFolder = PathsConfig.getProcessorFolder(oParameter.getName());
			String sPipFile = sProcessorFolder + PIP_FILE;

			if (!new File(sPipFile).exists()) {
				WasdiLog.warnLog("LocalProcessorEngine.libraryUpdate: no pip.txt found, nothing to update");
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
				return true;
			}

			String sVenvPath = sProcessorFolder + VENV_FOLDER;
			String sPythonVersion = getLocalPythonVersion(oParameter.getProcessorType());
			String sVenvPython = findVenvPythonExecutable(sProcessorFolder, sPythonVersion);
			if (Utils.isNullOrEmpty(sVenvPython)) {
				WasdiLog.errorLog("LocalProcessorEngine.libraryUpdate: no Python interpreter found in venv at " + sVenvPath);
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			processWorkspaceLog("Updating packages from pip.txt...");

			List<String> asPipCmd = new ArrayList<>();
			asPipCmd.add(sVenvPython);
			asPipCmd.add("-m");
			asPipCmd.add("pip");
			asPipCmd.add("install");
			asPipCmd.add("-r");
			asPipCmd.add(sPipFile);

			ShellExecReturn oPipResult = RunTimeUtils.shellExec(asPipCmd, true, true, true);

			if (!oPipResult.isOperationOk() || oPipResult.getOperationReturn() != 0) {
				WasdiLog.errorLog("LocalProcessorEngine.libraryUpdate: pip install failed. Output: " + oPipResult.getOperationLogs());
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
				return false;
			}

			LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.DONE, 100);
			processWorkspaceLog("Library update completed.");
			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("LocalProcessorEngine.libraryUpdate: exception", oEx);
			try {
				LauncherMain.updateProcessStatus(oProcessWorkspaceRepository, oProcessWorkspace, ProcessStatus.ERROR, 0);
			} catch (Exception oInnerEx) {
				WasdiLog.errorLog("LocalProcessorEngine.libraryUpdate: exception updating status", oInnerEx);
			}
			return false;
		}
	}

	/**
	 * Environment update: same as library update for local processors.
	 */
	@Override
	public boolean environmentUpdate(ProcessorParameter oParameter) {
		return libraryUpdate(oParameter);
	}

	/**
	 * Refresh packages info: not applicable for local processors.
	 */
	@Override
	public boolean refreshPackagesInfo(ProcessorParameter oParameter) {
		WasdiLog.debugLog("LocalProcessorEngine.refreshPackagesInfo: not implemented for local processors");
		return true;
	}

	/**
	 * Stop application: not applicable — local processors run synchronously.
	 */
	@Override
	public boolean stopApplication(ProcessorParameter oParameter) {
		WasdiLog.debugLog("LocalProcessorEngine.stopApplication: local processors run synchronously, nothing to stop");
		return true;
	}

}
