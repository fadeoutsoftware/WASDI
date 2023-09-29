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
	 * Execute a system task waiting the process to finish without collecting the logs
	 * It logs the command line executed on the launcher logs
	 *  
	 * @param asArgs List of strings that represents the command and the list of arguments
	 * @return True if the process is executed
	 */
	public static ShellExecReturn shellExec(List<String> asArgs) {
		return shellExec(asArgs,true);
	}	
	
	/**
	 * Execute a system task without collecting the logs
	 * It logs the command line executed on the launcher logs
	 * 
	 * @param asArgs List of strings that represents the command and the list of arguments 
	 * @param bWait True if the method should wait the shell exec to finish, false otherwise
	 * @return 
	 */
	public static ShellExecReturn shellExec(List<String> asArgs, boolean bWait) {
		return shellExec(asArgs, bWait, false);
	}
	
	/**
	 * Execute a system task 
	 * It does not redirect the error streamn to std out
	 * It logs the command line executed on the launcher logs
	 * 
	 * @param asArgs
	 * @param bWait
	 * @param bReadOutput
	 * @return
	 */
	public static ShellExecReturn shellExec(List<String> asArgs, boolean bWait, boolean bReadOutput) {
		return shellExec(asArgs, bWait, bReadOutput, false);
	}
	
	/**
	 * Execute a system task
	 * It logs the command line executed on the launcher logs
	 * 
	 * @param asArgs
	 * @param bWait
	 * @param bReadOutput
	 * @param bRedirectError
	 * @return
	 */
	public static ShellExecReturn shellExec(List<String> asArgs, boolean bWait, boolean bReadOutput, boolean bRedirectError) {
		return shellExec(asArgs, bWait, bReadOutput, bRedirectError, false);
	}

	/**
	 * Execute a system task
	 * 
	 * @param asArgs List of arguments
	 * @param bWait True to wait the process to finish, false to not wait
	 * @param bLogCommandLine  True to log the command line false to jump
	 * @return True if the process is executed
	 */
	public static ShellExecReturn shellExec(List<String> asArgs, boolean bWait, boolean bReadOutput, boolean bRedirectError, boolean bLogCommandLine) {
		if (WasdiConfig.Current.shellExecLocally) {
			return localShellExec(asArgs, bWait, bReadOutput, bRedirectError, bLogCommandLine);
		}
		else {
			return dockerShellExec(asArgs, bWait, bReadOutput, bRedirectError, bLogCommandLine);
		}
	}

	/**
	 * Execute a system task using the local (hosting) system
	 * 
	 * @param asArgs List of arguments
	 * @param bWait True to wait the process to finish, false to not wait
	 * @param bLogCommandLine  True to log the command line false to jump
	 * @return True if the process is executed
	 */
	private static ShellExecReturn localShellExec(List<String> asArgs, boolean bWait, boolean bReadOutput, boolean bRedirectError, boolean bLogCommandLine) {
		
		// Shell exec return object
		ShellExecReturn oReturn = new ShellExecReturn();
		
		// Set the asynch flag
		oReturn.setAsynchOperation(!bWait);
		
		try {
			// We need args!
			if (asArgs==null) {
				WasdiLog.errorLog("RunTimeUtils.shellExec: Args are null");
				return oReturn;
			}
			if (asArgs.size() == 0) {
				WasdiLog.errorLog("RunTimeUtils.shellExec: Args are empty");
				return oReturn;
			}
			
			// If this is asynch, we cannot collect logs
			if (bWait == false) { 
				if (bReadOutput || bRedirectError) {
					bReadOutput = false;
					bRedirectError = false;
					WasdiLog.warnLog("RunTimeUtils.shellExec: running with bWait = false, we cannot collect logs. Forcing ReadOutput and RedirectError to false (at least once was true!)");
				}
			}
			
			if (bRedirectError==true && bReadOutput==false) {
				bRedirectError = false;
				WasdiLog.warnLog("RunTimeUtils.shellExec: RedirectError makes no sense if we do not read output. Forcing to false");
			}
			
			// Check if we need to log the command line or not
			if (bLogCommandLine) {
				// Initialize the command line
				String sCommandLine = "";
				
				for (String sArg : asArgs) {
					sCommandLine += sArg + " ";
				}			
				
				// and log it
				WasdiLog.debugLog("RunTimeUtils.shellExec CommandLine: " + sCommandLine);
			}
			
			// If we want to collect the logs, create the temp file
			File oLogFile = null;
			if (bReadOutput) oLogFile = createLogFile();
			
			// Create the process builder
			ProcessBuilder oProcessBuilder = new ProcessBuilder(asArgs.toArray(new String[0]));
			
			if (bRedirectError) oProcessBuilder.redirectErrorStream(true);
			if (bReadOutput) oProcessBuilder.redirectOutput(oLogFile);
			
			Process oProcess = oProcessBuilder.start();
			
			if (bWait) {
				int iProcOuptut = oProcess.waitFor();
				oReturn.setOperationReturn(iProcOuptut);
				
				if (bReadOutput) {
					String sOutputFileContent = readLogFile(oLogFile);
					oReturn.setOperationLogs(sOutputFileContent);
					deleteLogFile(oLogFile);					
				}
				
				WasdiLog.debugLog("RunTimeUtils.shellExec CommandLine RETURNED: " + iProcOuptut);
			}
			
			oReturn.setOperationOk(true);
			
			return oReturn;
		}
		catch (Exception e) {
			WasdiLog.errorLog("RunTimeUtils.shellExec exception: " + e.getMessage());
			return oReturn;
		}		
	}
	
	/**
	 * Execute a system task using a dedicated docker image
	 * 
	 * @param asArgs List of arguments
	 * @param bWait True to wait the process to finish, false to not wait
	 * @param bLogCommandLine  True to log the command line false to jump
	 * @return True if the process is executed
	 */
	private static ShellExecReturn dockerShellExec(List<String> asArgs, boolean bWait, boolean bReadOutput, boolean bRedirectError, boolean bLogCommandLine) {
		ShellExecReturn oShellExecReturn = new ShellExecReturn();
		return oShellExecReturn;
	}
	
	/**
	 * Adds the run permission to a file.
	 * 
	 * @param sFile fill path of the file
	 */
	public static boolean addRunPermission(String sFile)  {
		try {
			// Make it executable
			Runtime.getRuntime().exec("chmod u+x " + sFile);

			// And wait a little bit to make the chmod done
			Thread.sleep(WasdiConfig.Current.msWaitAfterChmod);
			
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("RunTimeUtils.addRunPermission exception: " + oEx.getMessage());
			return false;
		}
	}
	
	/**
	 * Executes a local system command.
	 * The method creates a script with the content of the command, make it executable and run it locally.
	 * 
	 * @param sFolder Working folder
	 * @param sCommand Command to execute
	 * @return
	 */
	public static boolean runCommand(String sFolder, String sCommand) {
		return runCommand(sFolder, sCommand, false);
	}
	
	/**
	 * Executes a local system command.
	 * The method creates a script with the content of the command, make it executable and run it locally.
	 * 
	 * @param sFolder
	 * @param sCommand
	 * @param bRunInShell
	 * @return
	 */
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
				asArgs.add("sh");
				asArgs.add("-c");
				asArgs.add(sBuildScriptFile);
			}
			else {
				asArgs.add(sBuildScriptFile);
			}
			
			ShellExecReturn oShellExecReturn = RunTimeUtils.localShellExec(asArgs, true, true, true, true);
			
			if (bDelete) {
				FileUtils.forceDelete(oBuildScriptFile);
			}
			
			if (!Utils.isNullOrEmpty(oShellExecReturn.getOperationLogs())) {
				if (oShellExecReturn.getOperationLogs().trim().equalsIgnoreCase("false")) {
					oShellExecReturn.setOperationOk(false);
				}
			}			

			if (oShellExecReturn.isOperationOk()) {
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
