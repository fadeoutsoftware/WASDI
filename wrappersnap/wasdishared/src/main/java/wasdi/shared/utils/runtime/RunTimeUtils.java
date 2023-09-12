package wasdi.shared.utils.runtime;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class RunTimeUtils {

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
		shellExec(sCommand, asArgs, bWait, true);
	}

	/**
	 * Execute a system task
	 * @param sCommand Main command
	 * @param asArgs List of args to the command
	 * @param bWait True to wait the process to finish, false to not wait
	 * @param bLogCommandLine True to log the command line false to jump
	 */
	public static void shellExec(String sCommand, List<String> asArgs, boolean bWait, boolean bLogCommandLine) {
		try {
			if (asArgs==null) asArgs = new ArrayList<String>();
			asArgs.add(0, sCommand);
			
			if (bLogCommandLine) {
				String sCommandLine = "";
				
				for (String sArg : asArgs) {
					sCommandLine += sArg + " ";
				}			
			
				WasdiLog.debugLog("RunTimeUtils.shellExec CommandLine: " + sCommandLine);
			}
			
			ProcessBuilder oProcessBuilder = new ProcessBuilder(asArgs.toArray(new String[0]));
			Process oProcess = oProcessBuilder.start();
			
			if (bWait) {
				int iProcOuptut = oProcess.waitFor();				
				WasdiLog.debugLog("RunTimeUtils.shellExec CommandLine RETURNED: " + iProcOuptut);
			}
		}
		catch (Exception e) {
			WasdiLog.errorLog("RunTimeUtils.shellExec exception: " + e.getMessage());
		}
	}
	
	public static boolean shellExecWithLogs(String sCommand, List<String> asArgs) {
		return shellExecWithLogs(sCommand, asArgs, true);
	}

	/**
	 * Executes a command in the system registering the logs on a temp log file
	 * @param sCommand
	 * @param asArgs
	 * @return
	 */
	public static boolean shellExecWithLogs(String sCommand, List<String> asArgs, boolean bDelete) {

		try {
			if (asArgs == null) {
				asArgs = new ArrayList<>();
			}

			asArgs.add(0, sCommand);
			
			String sCommandLine = "";
			
			for (String sArg : asArgs) {
				sCommandLine += sArg + " ";
			}
			
			WasdiLog.debugLog("RunTimeUtils.shellExecWithLogs CommandLine: " + sCommandLine);

			File oLogFile = createLogFile();
			
			ProcessBuilder oProcessBuilder = new ProcessBuilder(asArgs.toArray(new String[0]));

			oProcessBuilder.redirectErrorStream(true);
			oProcessBuilder.redirectOutput(oLogFile);

			Process oProcess = oProcessBuilder.start();

			int iProcOuptut = oProcess.waitFor();				
			WasdiLog.debugLog("RunTimeUtils.shellExecWithLogs CommandLine RETURNED: " + iProcOuptut);

			if (iProcOuptut == 0) {

				String sOutputFileContent = readLogFile(oLogFile);
				
				WasdiLog.debugLog("RunTimeUtils.shellExecWithLogs sOutputFileContent: " + sOutputFileContent);
				
				if (bDelete) {
					deleteLogFile(oLogFile);
				}

				if (!Utils.isNullOrEmpty(sOutputFileContent)) {
					if (sOutputFileContent.trim().equalsIgnoreCase("false")) {
						return false;
					}
				}

				return true;
			} else {
				String sOutputFileContent = readLogFile(oLogFile);
				WasdiLog.debugLog("RunTimeUtils.shellExecWithLogs sOutputFileContent: " + sOutputFileContent);

				if (bDelete) {
					deleteLogFile(oLogFile);
				}

				return false;
			}
		} catch (Exception e) {			
			WasdiLog.errorLog("RunTimeUtils.shellExecWithLogs exception: " + e.getMessage());

			return false;
		}
	}
	
	/**
	 * Adds the run permission to a file.
	 * 
	 * @param sFile fill path of the file
	 */
	public static void addRunPermission(String sFile)  {
		try {
			// Make it executable
			Runtime.getRuntime().exec("chmod u+x " + sFile);

			// And wait a little bit to make the chmod done
			Thread.sleep(WasdiConfig.Current.msWaitAfterChmod);			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("RunTimeUtils.addRunPermission exception: " + oEx.getMessage());
		}
	}
	
	public static boolean runCommand(String sFolder, String sCommand) {
		return runCommand(sFolder, sCommand, false);
	}
	
	public static boolean runCommand(String sFolder, String sCommand, boolean bRunInShell) {
		return runCommand(sFolder, sCommand, false, true);
	}
	
	/**
	 * Run Linux command
	 * @param sCommand the command to run
	 * @return boolean in case of success, false otherwise
	 */
	public static boolean runCommand(String sFolder, String sCommand, boolean bRunInShell, boolean bDelete) {

		try {
			
			if (!sFolder.endsWith("/")) sFolder+="/";
			
			// Make a temp name
			String sTempScriptFile = Utils.getRandomName();
			
			// Generate shell script file
			String sBuildScriptFile = sFolder + sTempScriptFile + ".sh";

			File oBuildScriptFile = new File(sBuildScriptFile);

			try (BufferedWriter oBuildScriptWriter = new BufferedWriter(new FileWriter(oBuildScriptFile))) {
				// Fill the script file
				if (oBuildScriptWriter != null) {
					WasdiLog.debugLog("RunTimeUtils.runCommand: Creating " + sBuildScriptFile + " file");
					oBuildScriptWriter.write(sCommand);

					oBuildScriptWriter.flush();
					oBuildScriptWriter.close();
				}
			}

			// Wait a little bit to let the file be written
			Thread.sleep(100);

			// Make it executable
			addRunPermission(sBuildScriptFile);

			// Initialize Args
			List<String> asArgs = new ArrayList<>();
			
			// Run the script
			boolean bResult = false;
			
			if (bRunInShell) {
				asArgs.add("-c");
				asArgs.add(sBuildScriptFile);
				bResult = RunTimeUtils.shellExecWithLogs("sh", asArgs, bDelete);				
			}
			else {
				bResult = RunTimeUtils.shellExecWithLogs(sBuildScriptFile, asArgs, bDelete);
			}
			
			if (bDelete) {
				FileUtils.forceDelete(oBuildScriptFile);
			}

			if (bResult) {
				WasdiLog.debugLog("RunTimeUtils.runCommand: The shell command ran successfully.");
			} else {
				WasdiLog.debugLog("RunTimeUtils.runCommand: The shell command did not run successfully.");
			}

			return bResult;
		} catch (Exception oEx) {
			WasdiLog.errorLog("RunTimeUtils.runCommand: " + oEx.toString());
			return false;
		}
	}	

	/**
	 * Creates a temporary log file
	 * @return
	 */
	private static File createLogFile() {
		String sTmpFolder = WasdiConfig.Current.paths.wasdiTempFolder;
		if (!sTmpFolder.endsWith("/")) sTmpFolder += "/";

		File oLogFile = new File(sTmpFolder + Utils.getRandomName() + ".log");

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
			WasdiLog.errorLog("RunTimeUtils.readLogFile exception: " + oEx.getMessage());
		}

		return null;
	}
	
	/**
	 * Deletes a temporary log file
	 * @param oLogFile
	 */
	private static void deleteLogFile(File oLogFile) {
		try {
			FileUtils.forceDelete(oLogFile);
		} catch (IOException e) {
			WasdiLog.errorLog("RunTimeUtils.deleteLogFile exception: " + e.getMessage());
		}
	}
}
