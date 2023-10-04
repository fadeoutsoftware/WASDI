package wasdi.shared.utils.runtime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;

import wasdi.shared.config.ShellExecItemConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.docker.DockerUtils;
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
		oShellExecReturn.setOperationOk(false);
		
		if (asArgs == null) {
			WasdiLog.warnLog("RunTimeUtils.dockerShellExec: arg are null");
			return oShellExecReturn;
		}

		if (asArgs.size()<=0) {
			WasdiLog.warnLog("RunTimeUtils.dockerShellExec: arg are empty");
			return oShellExecReturn;
		}

		// Get the shell exec item
		ShellExecItemConfig oShellExecItem = WasdiConfig.Current.dockers.getShellExecItem(asArgs.get(0));
		
		if (oShellExecItem == null) {
			WasdiLog.warnLog("RunTimeUtils.dockerShellExec: impossible to find the command, try locally");
			return localShellExec(asArgs, bWait, bReadOutput, bRedirectError, bLogCommandLine);
		}
		
		DockerUtils oDockerUtils = new DockerUtils(null, null, null);
		
		if (!oShellExecItem.includeFistCommand) asArgs.remove(0);
		
		String sContainerName = oDockerUtils.run(oShellExecItem.dockerImage, oShellExecItem.containerVersion, asArgs);
		
		if (Utils.isNullOrEmpty(sContainerName)) {
			WasdiLog.warnLog("RunTimeUtils.dockerShellExec: impossible to get the container name from run");
			return oShellExecReturn;
		}
		else {
			if (bWait) {
				
				WasdiLog.debugLog("RunTimeUtils.dockerShellExec: wait for the container to make its job");
				
				boolean bFinished = oDockerUtils.waitForContainerToFinish(sContainerName);
				
				if (!bFinished) {
					WasdiLog.warnLog("RunTimeUtils.dockerShellExec: it looks we had some problems waiting the docker to finish :(");
					return oShellExecReturn;
				}
				
				oShellExecReturn.setAsynchOperation(false);
				oShellExecReturn.setOperationOk(true);
				
				if (bReadOutput || bRedirectError) {
					WasdiLog.debugLog("RunTimeUtils.dockerShellExec: collect also the logs");
					String sLogs = oDockerUtils.getContainerLogsByContainerName(sContainerName);
					oShellExecReturn.setOperationLogs(sLogs);
				}
			}
			else {
				WasdiLog.debugLog("RunTimeUtils.dockerShellExec: no need to wait, we return");
				oShellExecReturn.setAsynchOperation(true);
				oShellExecReturn.setOperationOk(true);
			}
		}
		
		
		return oShellExecReturn;
	}
	
	/**
	 * Adds the run permission to a file.
	 * 
	 * @param sFile fill path of the file
	 */
	public static boolean addRunPermission(String sFile)  {
		try {
			
			ArrayList<String> asCmd = new ArrayList<>();
			
			// Make it executable
			asCmd.add("chmod");
			asCmd.add("u+x");
			asCmd.add(sFile);
			
			shellExec(asCmd, false);

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
	 * Kill a running process
	 * @param iPid PID of the process to kill
	 * @return true or false
	 */
	public static boolean killProcess(int iPid) {
		try {
			if (iPid > 0) {
				// Pid exists, kill the process
				String sShellExString = WasdiConfig.Current.scheduler.killCommand;
				if (Utils.isNullOrEmpty(sShellExString)) sShellExString = "kill -9";
				sShellExString += " " + iPid;
				
				ArrayList<String> asCmd = new ArrayList<>(Arrays.asList(sShellExString.split(" ")));
				
				shellExec(asCmd, true);
				
				return true;

			} else {
				WasdiLog.errorLog("RunTimeUtils.killProcess: Process pid not <= 0");
				return false;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("RunTimeUtils.killProcess: ", oE);
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
	 * Check if a process is alive starting from PID
	 * The function tryes to support both Windows and Linux
	 * 
	 * @param sPidStr String representing the PID of the process
	 * @return
	 */
	public static boolean isProcessStillAllive(String sPidStr) {
		
	    String sOS = System.getProperty("os.name").toLowerCase();
	    String sCommand = null;
	    
	    if (sOS.indexOf("win") >= 0) {
	    	//Check alive Windows mode
	        sCommand = "cmd /c tasklist /FI \"PID eq " + sPidStr + "\"";            
	    } 
	    else if (sOS.indexOf("nix") >= 0 || sOS.indexOf("nux") >= 0) {
	    	//Check alive Linux/Unix mode
	        sCommand = "ps -p " + sPidStr;            
	    } 
	    else {
	    	//("Unsuported OS: go on Linux")
	    	sCommand = "ps -p " + sPidStr;
	    }
	    return isProcessIdRunning(sPidStr, sCommand); // call generic implementation
	}
	
	/**
	 * Checks if a specific process identified by a PID is still running.
	 * 
	 * @param sPid
	 * @param sCommand
	 * @return
	 */
	private static boolean isProcessIdRunning(String sPid, String sCommand) {
	    try {
	    	
	    	ArrayList<String> asCmd = new ArrayList<>(Arrays.asList(sCommand.split(" ")));
	    	
	    	ShellExecReturn oReturn = shellExec(asCmd, true, true, true, true);
	    	
	    	if (oReturn.getOperationLogs().contains(sPid)) return true;
	    	
	        return false;
	        
	    } catch (Exception oEx) {
	    	WasdiLog.debugLog("RunTimeUtils.isProcessIdRunning: Got exception using system command [{}] " + sCommand);
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
