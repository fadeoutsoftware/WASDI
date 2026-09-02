package wasdi.processors;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;

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
			asCommand.add("clone");
			asCommand.add("--depth");
			asCommand.add("1");
			asCommand.add("--single-branch");
			asCommand.add(sGitRepositoryUrl);
			asCommand.add(oStagingFolder.getAbsolutePath());

			Map<String, String> aoEnvironment = new HashMap<>();
			aoEnvironment.put("GIT_TERMINAL_PROMPT", "0");

			ShellExecReturn oCloneResult = RunTimeUtils.shellExec(asCommand, true, true, true, true, aoEnvironment);
			if (!oCloneResult.isOperationOk() || oCloneResult.getOperationReturn() != 0) {
				return "Cannot clone Git processor source: " + oCloneResult.getOperationLogs();
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