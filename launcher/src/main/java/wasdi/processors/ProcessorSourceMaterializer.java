package wasdi.processors;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class ProcessorSourceMaterializer {

	private ProcessorSourceMaterializer() {
		// Private constructor to hide the public implicit one
	}

	/**
	 * Clones a processor repository into staging and replaces the live source only after a successful clone.
	 *
	 * @param oProcessor processor containing the Git source metadata
	 * @param sProcessorFolder live processor source folder
	 * @return an empty string on success, otherwise an error suitable for operation logs
	 */
	public static String materializeGitSource(Processor oProcessor, String sProcessorFolder) {
		if (oProcessor == null || Utils.isNullOrEmpty(oProcessor.getGitRepositoryUrl())) {
			return "Git processor source metadata is missing";
		}

		String sGitRepositoryUrl = oProcessor.getGitRepositoryUrl().trim();
		if (!isValidPublicGitRepositoryUrl(sGitRepositoryUrl)) {
			return "Git repository URL is not a valid public HTTPS URL";
		}

		File oLiveFolder = new File(sProcessorFolder).getAbsoluteFile();
		File oParentFolder = oLiveFolder.getParentFile();
		if (oParentFolder == null) {
			return "Cannot determine the processor source parent folder";
		}

		String sOperationId = UUID.randomUUID().toString();
		File oStagingFolder = new File(oParentFolder, oLiveFolder.getName() + ".git-staging-" + sOperationId);
		File oBackupFolder = new File(oParentFolder, oLiveFolder.getName() + ".git-backup-" + sOperationId);

		try {
			List<String> asCommand = new ArrayList<>();
			asCommand.add("git");
			asCommand.add("-c");
			asCommand.add("credential.helper=");
			asCommand.add("-c");
			asCommand.add("credential.interactive=false");
			asCommand.add("-c");
			asCommand.add("http.version=HTTP/1.1");
			asCommand.add("-c");
			asCommand.add("http.lowSpeedLimit=1");
			asCommand.add("-c");
			asCommand.add("http.lowSpeedTime=60");
			asCommand.add("clone");
			asCommand.add("--depth");
			asCommand.add("1");
			asCommand.add("--single-branch");
			asCommand.add(sGitRepositoryUrl);
			asCommand.add(oStagingFolder.getAbsolutePath());

			String sCloneError = cloneRepository(asCommand);
			if (!Utils.isNullOrEmpty(sCloneError)) {
				return sCloneError;
			}

			File oGitMetadataFolder = new File(oStagingFolder, ".git");
			if (oGitMetadataFolder.exists()) {
				FileUtils.deleteDirectory(oGitMetadataFolder);
			}

			if (oLiveFolder.exists()) {
				Files.move(oLiveFolder.toPath(), oBackupFolder.toPath());
			}

			try {
				Files.move(oStagingFolder.toPath(), oLiveFolder.toPath());
			}
			catch (IOException oMoveException) {
				restoreBackup(oLiveFolder.toPath(), oBackupFolder.toPath());
				throw oMoveException;
			}

			try {
				if (oBackupFolder.exists()) {
					FileUtils.deleteDirectory(oBackupFolder);
				}
			}
			catch (IOException oCleanupException) {
				WasdiLog.warnLog("ProcessorSourceMaterializer.materializeGitSource: cannot clean backup folder " + oBackupFolder.getAbsolutePath());
			}

			return "";
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorSourceMaterializer.materializeGitSource: cannot materialize processor source", oEx);
			return "Cannot materialize Git processor source: " + oEx.getMessage();
		}
		finally {
			try {
				if (oStagingFolder.exists()) {
					FileUtils.deleteDirectory(oStagingFolder);
				}
			}
			catch (IOException oCleanupException) {
				WasdiLog.warnLog("ProcessorSourceMaterializer.materializeGitSource: cannot clean staging folder " + oStagingFolder.getAbsolutePath());
			}
		}
	}

	private static String cloneRepository(List<String> asCommand) {
		Process oProcess = null;
		StringBuilder oProcessOutput = new StringBuilder();

		try {
			WasdiLog.debugLog("ProcessorSourceMaterializer.cloneRepository: cloning public Git repository");
			ProcessBuilder oProcessBuilder = new ProcessBuilder(asCommand);
			oProcessBuilder.redirectErrorStream(true);
			oProcessBuilder.environment().put("GIT_TERMINAL_PROMPT", "0");

			oProcess = oProcessBuilder.start();
			Process oRunningProcess = oProcess;
			Thread oOutputReader = Thread.ofVirtual().start(() -> readProcessOutput(oRunningProcess, oProcessOutput));

			if (!oProcess.waitFor(180L, TimeUnit.SECONDS)) {
				oProcess.descendants().forEach(ProcessHandle::destroyForcibly);
				oProcess.destroyForcibly();
				oProcess.waitFor(5L, TimeUnit.SECONDS);
				oOutputReader.join(5000L);
				return "Cannot clone Git processor source: operation timed out after 180 seconds" + formatProcessOutput(oProcessOutput);
			}

			oOutputReader.join(5000L);
			if (oProcess.exitValue() != 0) {
				return "Cannot clone Git processor source" + formatProcessOutput(oProcessOutput);
			}

			return "";
		}
		catch (InterruptedException oEx) {
			Thread.currentThread().interrupt();
			if (oProcess != null) {
				oProcess.descendants().forEach(ProcessHandle::destroyForcibly);
				oProcess.destroyForcibly();
			}
			return "Git processor source clone was interrupted";
		}
		catch (IOException oEx) {
			return "Cannot start Git processor source clone: " + oEx.getMessage();
		}
	}

	private static void readProcessOutput(Process oProcess, StringBuilder oProcessOutput) {
		try (BufferedReader oReader = new BufferedReader(new InputStreamReader(oProcess.getInputStream()))) {
			String sLine = null;
			while ((sLine = oReader.readLine()) != null) {
				oProcessOutput.append(sLine).append(System.lineSeparator());
			}
		}
		catch (IOException oEx) {
			WasdiLog.warnLog("ProcessorSourceMaterializer.readProcessOutput: cannot read Git output " + oEx.getMessage());
		}
	}

	private static String formatProcessOutput(StringBuilder oProcessOutput) {
		if (oProcessOutput.length() == 0) {
			return "";
		}
		return ": " + oProcessOutput.toString().trim();
	}

	private static void restoreBackup(Path oLivePath, Path oBackupPath) {
		try {
			if (Files.exists(oBackupPath) && !Files.exists(oLivePath)) {
				Files.move(oBackupPath, oLivePath);
			}
		}
		catch (IOException oRestoreException) {
			WasdiLog.errorLog("ProcessorSourceMaterializer.restoreBackup: cannot restore previous processor source", oRestoreException);
		}
	}

	private static boolean isValidPublicGitRepositoryUrl(String sGitRepositoryUrl) {
		try {
			URI oUri = new URI(sGitRepositoryUrl);
			return "https".equalsIgnoreCase(oUri.getScheme())
					&& oUri.getUserInfo() == null
					&& !Utils.isNullOrEmpty(oUri.getHost())
					&& !Utils.isNullOrEmpty(oUri.getPath())
					&& !"/".equals(oUri.getPath());
		}
		catch (URISyntaxException oEx) {
			return false;
		}
	}
}