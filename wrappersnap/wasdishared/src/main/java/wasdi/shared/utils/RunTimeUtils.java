package wasdi.shared.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class RunTimeUtils {
	
	protected static final String FILE_SEPARATOR = System.getProperty("file.separator");

	/**
	 * Execute a system task waiting the process to finish
	 * @param sCommand main command to use 
	 * @param asArgs List of args to pass to the command
	 */
	public static void shellExec(String sCommand, List<String> asArgs) {
		shellExec(sCommand,asArgs,true);
	}
	
	/**
	 * Execute a system task
	 * @param sCommand Main command
	 * @param asArgs List of args to the command
	 * @param bWait True to wait the process to finish, false to not wait
	 */	
	public static void shellExec(String sCommand, List<String> asArgs, boolean bWait) {
		try {
			if (asArgs==null) asArgs = new ArrayList<String>();
			asArgs.add(0, sCommand);
			
			String sCommandLine = "";
			
			for (String sArg : asArgs) {
				sCommandLine += sArg + " ";
			}
			
			Utils.debugLog("ShellExec CommandLine: " + sCommandLine);
			
			ProcessBuilder oProcessBuilder = new ProcessBuilder(asArgs.toArray(new String[0]));
			Process oProcess = oProcessBuilder.start();
			
			if (bWait) {
				int iProcOuptut = oProcess.waitFor();				
				Utils.debugLog("ShellExec CommandLine RETURNED: " + iProcOuptut);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Executes a command in the system registering the logs on a temp log file
	 * @param sCommand
	 * @param asArgs
	 * @return
	 */
	public static boolean shellExecWithLogs(String sCommand, List<String> asArgs) {
		Utils.debugLog("shellExecWithLogs sCommand: " + sCommand);

		try {
			if (asArgs == null) {
				asArgs = new ArrayList<>();
			}

			asArgs.add(0, sCommand);
			
			String sCommandLine = "";
			
			for (String sArg : asArgs) {
				sCommandLine += sArg + " ";
			}
			
			Utils.debugLog("shellExecWithLogs CommandLine: " + sCommandLine);

			File logFile = createLogFile();
			
			ProcessBuilder oProcessBuilder = new ProcessBuilder(asArgs.toArray(new String[0]));

			oProcessBuilder.redirectErrorStream(true);
			oProcessBuilder.redirectOutput(logFile);

			Process oProcess = oProcessBuilder.start();

			int iProcOuptut = oProcess.waitFor();				
			Utils.debugLog("shellExecWithLogs CommandLine RETURNED: " + iProcOuptut);

			if (iProcOuptut == 0) {

				String sOutputFileContent = readLogFile(logFile);
				deleteLogFile(logFile);

				if (!Utils.isNullOrEmpty(sOutputFileContent)) {
					if (sOutputFileContent.trim().equalsIgnoreCase("false")) {
						return false;
					}
				}

				return true;
			} else {
				String sOutputFileContent = readLogFile(logFile);
				Utils.debugLog("shellExecWithLogs sOutputFileContent: " + sOutputFileContent);

				deleteLogFile(logFile);

				return false;
			}
		} catch (Exception e) {			
			Utils.debugLog("shellExecWithLogs exception: " + e.getMessage());

			return false;
		}
	}
	
	/**
	 * Run Linux command
	 * @param sCommand the command to run
	 * @return boolean in case of success, false otherwise
	 */
	public static boolean runCommand(String sFolder, String sCommand) {

		try {
			
			// Make a temp name
			String sTempScriptFile = Utils.getRandomName();
			
			// Generate shell script file
			String sBuildScriptFile = sFolder + sTempScriptFile + ".sh";

			File oBuildScriptFile = new File(sBuildScriptFile);

			try (BufferedWriter oBuildScriptWriter = new BufferedWriter(new FileWriter(oBuildScriptFile))) {
				// Fill the script file
				if (oBuildScriptWriter != null) {
					Utils.debugLog("DockerProcessorEngine.runCommand: Creating " + sBuildScriptFile + " file");
					oBuildScriptWriter.write(sCommand);

					oBuildScriptWriter.flush();
					oBuildScriptWriter.close();
				}
			}

			// Wait a little bit to let the file be written
			Thread.sleep(100);

			// Make it executable
			Runtime.getRuntime().exec("chmod u+x " + sBuildScriptFile);

			// And wait a little bit to make the chmod done
			Thread.sleep(100);

			// Initialize Args
			List<String> asArgs = new ArrayList<>();


			// Generate shell command
			Utils.debugLog("DockerProcessorEngine.runCommand: Running the shell command");
			Utils.debugLog(sCommand);

			// Run the script
			boolean bResult = RunTimeUtils.shellExecWithLogs(sBuildScriptFile, asArgs);

			FileUtils.forceDelete(oBuildScriptFile);

			if (bResult) {
				Utils.debugLog("DockerUtils.runCommand: The shell command ran successfully.");
			} else {
				Utils.debugLog("DockerUtils.runCommand: The shell command did not run successfully.");
			}

			return bResult;
		} catch (Exception oEx) {
			Utils.debugLog("DockerUtils.runCommand: " + oEx.toString());
			return false;
		}
	}	

	/**
	 * Creates a temporary log file
	 * @return
	 */
	private static File createLogFile() {
		Utils.debugLog("createLogFile Working Directory = " + System.getProperty("user.dir"));

		File oUserDir = new File(System.getProperty("user.dir"));
		File oLogFile = new File(oUserDir.getAbsolutePath() + FILE_SEPARATOR + "temporary_log_file.log");

		return oLogFile;
	}
	
	/**
	 * Reads a temporary log file
	 * @param oLogFile
	 * @return
	 */
	private static String readLogFile(File oLogFile) {
		try {
			return FileUtils.readFileToString(oLogFile);
		} catch (IOException oEx) {
			Utils.debugLog("readLogFile exception: " + oEx.getMessage());
		}

		return null;
	}

	private static void deleteLogFile(File logFile) {
		try {
			FileUtils.forceDelete(logFile);
		} catch (IOException e) {
			Utils.debugLog("deleteLogFile exception: " + e.getMessage());
		}
	}
}