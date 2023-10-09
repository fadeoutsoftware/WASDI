package wasdi.scheduler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.SchedulerQueueConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;

public class ProcessScheduler {
	
	/**
	 * the folder that contains the serialised parameters used by each process
	 */
	protected File m_oParametersFilesFolder = null;
	/**
	 * sleeping time between iterations
	 */
	protected long m_lWaitProcessStartMS = 2000;
	/**
	 * sleeping time between iterations
	 */
	protected long m_lTimeOutMs = -1;	
	/**
	 * number concurrent process
	 */
	protected int m_iNumberOfConcurrentProcess = 1;

	/**
	 * launcher installation path
	 */
	protected String m_sLauncherPath;
	/**
	 * java executable path
	 */
	protected String m_sJavaExePath;
	/**
	 * mongo repository for processworkspace collection
	 */
	protected ProcessWorkspaceRepository m_oProcessWorkspaceRepository;
	/**
	 * map of already launched processes. Used to avoid multiple execution of the same process
	 */
	protected Map<String, Date> m_aoLaunchedProcesses = new HashMap<String, Date>();
	
	/**
	 * Process Scheduler Log Prefix
	 */
	protected String m_sLogPrefix = "ProcessScheduler.";
	
	/**
	 * Key of this scheduler instance
	 */
	protected String  m_sSchedulerKey = "ProcessScheduler";
	
	/**
	 * Kill command
	 */
	protected String m_sKillCommand = "kill -9 ";
	
	/**
	 * Wasdi Node
	 */
	protected String  m_sWasdiNode = "wasdi";	
	
	/**
	 * Bool flag to stop the thread
	 */
	private volatile boolean m_bRunning = true;
	
	/**
	 * List of operation types supported by this scheduler
	 */
	protected ArrayList<String> m_asOperationTypes = new ArrayList<String>();
	
	/**
	 * Operation Subtype: it is single because, at least until now
	 * the subtype filter is supported only for mono-type schedulers
	 */
	protected String m_sOperationSubType;
	
	
	public boolean init(String sSchedulerKey) {
		
		try {
			// Save the scheduler Key
			m_sSchedulerKey = sSchedulerKey;
			// Init the scheduler log prefix
			m_sLogPrefix = m_sSchedulerKey + ": ";
			
			//Read the Serialisation Path
			File oFolder = new File(WasdiConfig.Current.paths.serializationPath);
			
			if (!oFolder.isDirectory()) {
				WasdiLog.errorLog(m_sLogPrefix + ".init: cannot access parameters folder: " + oFolder.getAbsolutePath());
			}
			
			// Save the path 
			m_oParametersFilesFolder = oFolder;
			
			SchedulerQueueConfig oSchedulerQueueConfig = WasdiConfig.Current.scheduler.getSchedulerQueueConfig(sSchedulerKey);
			
			if (oSchedulerQueueConfig!=null) {
				// Read Max Size of Concurrent Processes of this scheduler 
				try {
					
					if (!Utils.isNullOrEmpty(oSchedulerQueueConfig.maxQueue)) {
						int iMaxConcurrents = Integer.parseInt(oSchedulerQueueConfig.maxQueue);
						if (iMaxConcurrents>0) {
							m_iNumberOfConcurrentProcess = iMaxConcurrents;
							WasdiLog.infoLog(m_sLogPrefix + ".init: Max Concurrent Processes: " + m_iNumberOfConcurrentProcess);
						}					
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				// Read Timeout of this scheduler 
				try {
					if (!Utils.isNullOrEmpty(oSchedulerQueueConfig.timeoutMs)) {
						long lTimeout = Long.parseLong(oSchedulerQueueConfig.timeoutMs);
						if (lTimeout>0) {
							m_lTimeOutMs = lTimeout;
							WasdiLog.infoLog(m_sLogPrefix + ".init:  TimeOut Ms: " + m_lTimeOutMs);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				// Read Operation Type supported 
				try {
					// Get the string from config
					String sOperationTypes = ""; 
					
					if (!Utils.isNullOrEmpty(oSchedulerQueueConfig.opTypes)) {
						sOperationTypes = oSchedulerQueueConfig.opTypes;
					}
					
					
					// Split on comma
					String [] asTypes = sOperationTypes.split(",");
					
					
					// Add each element to the member list
					if (asTypes != null) {
						for (String sType : asTypes) {
							m_asOperationTypes.add(sType);
						}
						
						// If there is only one type
						if (asTypes.length == 1) {
							// Read if there is a Subtype
							String sOperationSubType = oSchedulerQueueConfig.opSubType;
							
							if (!Utils.isNullOrEmpty(sOperationSubType)) {
								// Save the subtype
								m_sOperationSubType = sOperationSubType;
							}
						}
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				try {
					
					if (!Utils.isNullOrEmpty(WasdiConfig.Current.scheduler.processingThreadSleepingTimeMS)) {
						long iStartWaitSleep = Long.parseLong( WasdiConfig.Current.scheduler.processingThreadSleepingTimeMS);
						if (iStartWaitSleep>0) {
							m_lWaitProcessStartMS = iStartWaitSleep;
							WasdiLog.infoLog(m_sLogPrefix + ".init: Wait Proc Start Ms: " + m_lWaitProcessStartMS);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}
			
			// Read the Lancher Path
			m_sLauncherPath = WasdiConfig.Current.scheduler.launcherPath;
			// Read Java Exe Path
			m_sJavaExePath = WasdiConfig.Current.scheduler.javaExe;
			// Read Wasdi Node Id
			m_sWasdiNode = WasdiConfig.Current.nodeCode;
			// Read the Kill command
			m_sKillCommand = WasdiConfig.Current.scheduler.killCommand;
			
			// Create the Repo
			m_oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog(m_sLogPrefix + ".init: init Exception: " + oEx);
			return false;
		}
		WasdiLog.infoLog(m_sLogPrefix + ".init: good to go :-)\n");
		return true;
	}
	
	/**
	 * Perform a cycle of this specific scheduler
	 * @param iSometimes
	 * @param aoRunningList
	 * @param aoReadyList
	 * @param aoCreatedList
	 */
	public void cycle(List<ProcessWorkspace> aoRunningList, List<ProcessWorkspace> aoReadyList, List<ProcessWorkspace> aoCreatedList) {
					
		try {
							
			// Get the updated list of running processes
			aoRunningList = getRunningList(aoRunningList);
			
			// Do we have any free slot?
			if (aoRunningList.size() < m_iNumberOfConcurrentProcess) {
				
				// Yes: get the list of Ready Processes
				aoReadyList = getReadyList(aoReadyList);
				// Get the list of Created Processes
				aoCreatedList = getCreatedList(aoCreatedList);
				
				// For each ready process
				while (aoReadyList.size()> 0) {
					
					// If we fished free slots, stop the cycle
					if (aoRunningList.size()>=m_iNumberOfConcurrentProcess) break;
											
					// Get the ready process
					ProcessWorkspace oReadyProcess = aoReadyList.get(0);
					
					// Update the status to running
					oReadyProcess.setStatus(ProcessStatus.RUNNING.name());
					m_oProcessWorkspaceRepository.updateProcess(oReadyProcess);
					
					WasdiLog.infoLog(m_sLogPrefix + ".run: Resumed " + oReadyProcess.getProcessObjId());
					
					// Move in the running list
					aoReadyList.remove(0);
					aoRunningList.add(oReadyProcess);
				}
									
				// For each created process
				while (aoCreatedList.size()> 0) {
																							
					// If we fished free slots, stop the cycle
					if (aoRunningList.size()>=m_iNumberOfConcurrentProcess) break;
					
					// Get the Created process
					ProcessWorkspace oCreatedProcess = aoCreatedList.get(0);

					
					// Check if we did not launch this before
					if (!m_aoLaunchedProcesses.containsKey(oCreatedProcess.getProcessObjId())) {
													
						// Execute the process
						if (executeProcess(oCreatedProcess) != null) {
							
							WasdiLog.infoLog(m_sLogPrefix + ".run: Lauched " + oCreatedProcess.getProcessObjId());
							
							// Give a little bit of time to the launcher to start
							waitForProcessToStart();
							
							// Move The process in the running list
							aoRunningList.add(oCreatedProcess);		
						}
						else {
							WasdiLog.infoLog(m_sLogPrefix + ".run: ERROR Lauching " + oCreatedProcess.getProcessObjId());
						}
					}
					
					aoCreatedList.remove(0);
				}
			}
			else {
				//if (iSometimes == m_iSometimesCounter) WasdiLog.infoLog(m_sLogPrefix + "Running Queue full, next cycle.");
			}
			
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog(m_sLogPrefix + ".run: " + oEx); 
			oEx.printStackTrace();
		} 
	}
	
	
	
	public void sometimesCheck(List<ProcessWorkspace> aoRunningList, List<ProcessWorkspace> aoCreatedList, List<ProcessWorkspace> aoReadyList, List<ProcessWorkspace> aoWaitingList) {
		
		try {
			if (m_aoLaunchedProcesses.size()!=0) {
				WasdiLog.infoLog(m_sLogPrefix + ".run: Launched Processes Size: " + m_aoLaunchedProcesses.size());
			}
			
			// Check Launched Proc list
			ArrayList<String> asLaunchedToDelete = new ArrayList<String>();
			
			for (String sLaunchedProcessWorkspaceId : m_aoLaunchedProcesses.keySet()) {
				
				// If it is launched, it "needs" CREATED State. If not, has been done in some way
				ProcessWorkspace oLaunched = m_oProcessWorkspaceRepository.getProcessByProcessObjId(sLaunchedProcessWorkspaceId);
				
				if (oLaunched != null) {
					if (oLaunched.getStatus().equals(ProcessStatus.CREATED.name()) == false) {
						asLaunchedToDelete.add(sLaunchedProcessWorkspaceId);
					}					
				}
				else {
					WasdiLog.infoLog(m_sLogPrefix + ".run: Invalid Proc WS ID : " + sLaunchedProcessWorkspaceId);
					asLaunchedToDelete.add(sLaunchedProcessWorkspaceId);
				}
			}
			
			// Remove launched elements
			for (String sLaunchedToRemove : asLaunchedToDelete) {
				WasdiLog.infoLog(m_sLogPrefix + ".run: Remove from launched : " + sLaunchedToRemove);
				m_aoLaunchedProcesses.remove(sLaunchedToRemove);
			}
			
			// Get the updated list of running processes
			aoRunningList = getRunningList(aoRunningList);
			
			// For each running process check pid and timeout
			for (ProcessWorkspace oRunningPws : aoRunningList) {
				
				// All processes in running can be removed from the launched list
				if (m_aoLaunchedProcesses.containsKey(oRunningPws.getProcessObjId())) {
					m_aoLaunchedProcesses.remove(oRunningPws.getProcessObjId());
				}
				
				// Get the PID
				String sPid = "" + oRunningPws.getPid();
				
				// Check if it is alive
				if (!Utils.isNullOrEmpty(sPid)) {
					if (!RunTimeUtils.isProcessStillAllive(sPid)) {
						// PID does not exists: recheck and remove
						WasdiLog.infoLog(m_sLogPrefix + ".run: Process " + oRunningPws.getProcessObjId() + " has PID " + sPid + ", status RUNNING but the process does not exists");
						
						// Read Again to be sure
						ProcessWorkspace oCheckProcessWorkspace = m_oProcessWorkspaceRepository.getProcessByProcessObjId(oRunningPws.getProcessObjId());
						
						if (oCheckProcessWorkspace != null) {
							// Is it still running?
							if (oCheckProcessWorkspace.getStatus().equals(ProcessStatus.RUNNING.name())) {
								
								// Force to error
								oCheckProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
								// Set the operation end date
								if (Utils.isNullOrEmpty(oCheckProcessWorkspace.getOperationEndTimestamp())) {
									oCheckProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
								}
								// Update the process
								m_oProcessWorkspaceRepository.updateProcess(oCheckProcessWorkspace);
								WasdiLog.infoLog(m_sLogPrefix + ".run: **************Process " + oRunningPws.getProcessObjId() + " status changed to ERROR");
							}								
						}
					}
				}
				else {
					WasdiLog.infoLog(m_sLogPrefix + ".run: Process " + oRunningPws.getProcessObjId() + " with status RUNNING has null PID");
				}
				
				// Is there a timeout?
				if (m_lTimeOutMs != -1) {
					// Check the last state change
					if (!Utils.isNullOrEmpty(oRunningPws.getLastStateChangeTimestamp())) {
						
						Double oLastChange = oRunningPws.getLastStateChangeTimestamp();
						Date oNow = new Date();
						long lTimeSpan = oNow.getTime() - oLastChange.longValue();
						
						// We ran over?
						if (lTimeSpan > m_lTimeOutMs) {
							// Change the status to STOPPED
							WasdiLog.infoLog(m_sLogPrefix + ".run: Process " + oRunningPws.getProcessObjId() + " is in Timeout");
							
							// Stop the process
							stopProcess(oRunningPws.getPid());
							
							// Update the state
							oRunningPws.setStatus(ProcessStatus.STOPPED.name());
							
							if (Utils.isNullOrEmpty(oRunningPws.getOperationEndTimestamp())) {
								oRunningPws.setOperationEndTimestamp(Utils.nowInMillis());
							}

							m_oProcessWorkspaceRepository.updateProcess(oRunningPws);
							WasdiLog.infoLog(m_sLogPrefix + ".run: **************Process " + oRunningPws.getProcessObjId() + " Status changed to STOPPED");
						}
					}
					else {
						WasdiLog.infoLog(m_sLogPrefix + ".run: Process " + oRunningPws.getProcessObjId() + " Does not have a last state change date");
					}
				}
			}
			
			// Get the list of Created Processes
			aoCreatedList = getCreatedList(aoCreatedList);
			
			for (ProcessWorkspace oCreatedProcess : aoCreatedList) {
										
				// Check if it has been launched before
				if (m_aoLaunchedProcesses.containsKey(oCreatedProcess.getProcessObjId())) {
					
					// We already triggered the execution: check if it is not starting...
					
					// Get Now and Starting date
					Date oNow = new Date();
					Date oStartDate = m_aoLaunchedProcesses.get(oCreatedProcess.getProcessObjId());
					long lTimeSpan = oNow.getTime()-oStartDate.getTime();
					
					// Wait 10 time more than standard waiting
					if (lTimeSpan > 2000*m_lWaitProcessStartMS) {
						// No good, set as ERROR
						WasdiLog.warnLog(m_sLogPrefix + ".run: **************Process " + oCreatedProcess.getProcessObjId() + " is GETTING OLD.. ");
						
						/*
						oCreatedProcess.setStatus(ProcessStatus.ERROR.name());
						
						if (Utils.isNullOrEmpty(oCreatedProcess.getOperationEndDate())) {
							oCreatedProcess.setOperationEndDate(Utils.GetFormatDate(new Date()));
						}
						
						m_oProcessWorkspaceRepository.updateProcess(oCreatedProcess);
						// Remove from created
						aoCreatedList.remove(0);
						// And from Launched
						m_aoLaunchedProcesses.remove(oCreatedProcess.getProcessObjId());
						*/
					}
				}
			}
			
			// Get the updated list of waiting and ready processes
			List<ProcessWorkspace> aoWaitingReadyList = getReadyList(aoReadyList);
			aoWaitingReadyList.addAll(getWaitingList(aoWaitingList));
			
			// For each ready and waiting process check pid
			for (ProcessWorkspace oWaitingReadyPws : aoWaitingReadyList) {
				
				// All processes in waiting o ready are anyway started so they can be removed from the launched list
				if (m_aoLaunchedProcesses.containsKey(oWaitingReadyPws.getProcessObjId())) {
					m_aoLaunchedProcesses.remove(oWaitingReadyPws.getProcessObjId());
				}
				
				// Get the PID
				String sPid = "" + oWaitingReadyPws.getPid();
				
				// Check if it is alive
				if (!Utils.isNullOrEmpty(sPid)) {
					if (!RunTimeUtils.isProcessStillAllive(sPid)) {
						// PID does not exists: recheck and remove
						WasdiLog.warnLog(m_sLogPrefix + ".run: Process " + oWaitingReadyPws.getProcessObjId() + " has PID " + sPid + ", is WAITING or READY but the process does not exists");
						
						// Read Again to be sure
						ProcessWorkspace oCheckProcessWorkspace = m_oProcessWorkspaceRepository.getProcessByProcessObjId(oWaitingReadyPws.getProcessObjId());
						
						if (oCheckProcessWorkspace != null) {
							// Is it still running?
							if (oCheckProcessWorkspace.getStatus().equals(ProcessStatus.RUNNING.name()) ||
									oCheckProcessWorkspace.getStatus().equals(ProcessStatus.WAITING.name()) ||
									oCheckProcessWorkspace.getStatus().equals(ProcessStatus.READY.name())) {
								
								// It cannot be in any of these states if the PID does not exists
								
								// Force to error
								oCheckProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
								// Set the operation end date
								if (Utils.isNullOrEmpty(oCheckProcessWorkspace.getOperationEndTimestamp())) {
									oCheckProcessWorkspace.setOperationEndTimestamp(Utils.nowInMillis());
								}
								
								// Update the process
								m_oProcessWorkspaceRepository.updateProcess(oCheckProcessWorkspace);
								WasdiLog.infoLog(m_sLogPrefix + ".run: **************Process " + oWaitingReadyPws.getProcessObjId() + " with WAITING or READY  status changed to ERROR");
							}							
						}
					}
				}
				else {
					WasdiLog.infoLog(m_sLogPrefix + "Process " + oWaitingReadyPws.getProcessObjId() + " has null PID");
				}
			}	
		}
		catch (Exception oEx) {
			WasdiLog.errorLog(m_sLogPrefix + ".run: " + oEx); 
			oEx.printStackTrace();
		}
		
	}
	
	
	/**
	 * Filter the list of operations with the supported types
	 * @param aoInputList
	 * @return
	 */
	protected List<ProcessWorkspace> filterOperationTypes(List<ProcessWorkspace> aoInputList) {
		
		ArrayList<ProcessWorkspace> aoRetList = new ArrayList<ProcessWorkspace>();
		
		try {
			
			for (int iProcWs = 0; iProcWs<aoInputList.size(); iProcWs++) {
				
				boolean bJump = false;
				
				// Filter on scheduler operation types
				if (!m_asOperationTypes.contains(aoInputList.get(iProcWs).getOperationType())) {
					bJump = true;
				}
				else {
					// Filter on operation subtype if configured
					if (m_asOperationTypes.size()==1) {
						if (!Utils.isNullOrEmpty(m_sOperationSubType)) {
							if (aoInputList.get(iProcWs).getOperationSubType()!= null) {
								if (!aoInputList.get(iProcWs).getOperationSubType().equals(m_sOperationSubType)) {
									bJump = true;
								}							
							}
						}					
					}					
				}
				
				if (!bJump) {
					aoRetList.add(aoInputList.get(iProcWs));
				}
			}
			
			return aoRetList;
		}
		catch (Exception oE) {
			oE.printStackTrace();
			return new ArrayList<ProcessWorkspace>();
		}
	}
	
	/**
	 * Get the list of running processes
	 * @return list of running processes
	 */
	protected List<ProcessWorkspace> getRunningList() {
		try {
			List<ProcessWorkspace> aoRunning = m_oProcessWorkspaceRepository.getProcessesByStateNode(ProcessStatus.RUNNING.name(), m_sWasdiNode);
			return getRunningList(aoRunning);
		}
		catch (Exception oE) {
			oE.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Get the list of running processes
	 * @return list of running processes
	 */
	protected List<ProcessWorkspace> getRunningList(List<ProcessWorkspace> aoRunning) {
		try {
			aoRunning = filterOperationTypes(aoRunning);
			Collections.reverse(aoRunning);
			return aoRunning;
		}
		catch (Exception oE) {
			oE.printStackTrace();
			return null;
		}
	}
	
	
	protected List<ProcessWorkspace> getCreatedList() {
		try {
			List<ProcessWorkspace> aoCreated = m_oProcessWorkspaceRepository.getProcessesByStateNode(ProcessStatus.CREATED.name(), m_sWasdiNode);
			return getCreatedList(aoCreated);
		}
		catch (Exception oE) {
			oE.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Get the list of created processes
	 * @return list of created processes
	 */
	protected List<ProcessWorkspace> getCreatedList(List<ProcessWorkspace> aoCreated) {
		try {
			aoCreated = filterOperationTypes(aoCreated);
			Collections.reverse(aoCreated);
			return aoCreated;
		}
		catch (Exception oE) {
			oE.printStackTrace();
			return null;
		}
	}
	
	
	protected List<ProcessWorkspace> getReadyList() {
		try {
			List<ProcessWorkspace> aoReady = m_oProcessWorkspaceRepository.getProcessesByStateNode(ProcessStatus.READY.name(), m_sWasdiNode, "lastStateChangeTimestamp", "lastStateChangeDate");
			return getReadyList(aoReady);
		}
		catch (Exception oE) {
			oE.printStackTrace();
			return null;
		}
	}	
	
	/**
	 * Get the list of ready processes
	 * @return list of ready processes
	 */
	protected List<ProcessWorkspace> getReadyList(List<ProcessWorkspace> aoReady) {
		try {
			aoReady = filterOperationTypes(aoReady);
			Collections.reverse(aoReady);
			return aoReady;
		}
		catch (Exception oE) {
			oE.printStackTrace();
			return null;
		}
	}	
	
	protected List<ProcessWorkspace> getWaitingList() {
		try {
			List<ProcessWorkspace> aoWaiting = m_oProcessWorkspaceRepository.getProcessesByStateNode(ProcessStatus.WAITING.name(), m_sWasdiNode, "lastStateChangeTimestamp", "lastStateChangeDate");
			return getWaitingList(aoWaiting);
		}
		catch (Exception oE) {
			oE.printStackTrace();
			return null;
		}
	}	
	
	/**
	 * Get the list of Waiting processes
	 * @return list of WAITING processes
	 */
	protected List<ProcessWorkspace> getWaitingList(List<ProcessWorkspace> aoWaiting) {
		try {
			aoWaiting = filterOperationTypes(aoWaiting);
			Collections.reverse(aoWaiting);
			return aoWaiting;
		}
		catch (Exception oE) {
			oE.printStackTrace();
			return null;
		}
	}	
	
	/**
	 * Trigger the real execution of a process
	 * @param oProcessWorkspace
	 * @return Process Object Identifier
	 */
	private String executeProcess(ProcessWorkspace oProcessWorkspace) {
		try {
			File oParameterFilePath = new File(m_oParametersFilesFolder, oProcessWorkspace.getProcessObjId());
	
			String sShellExString = m_sJavaExePath + " -jar " + m_sLauncherPath +
					" -operation " + oProcessWorkspace.getOperationType() +
					" -parameter " + oParameterFilePath.getAbsolutePath();
	
			WasdiLog.infoLog(m_sLogPrefix + "executeProcess: executing command for process " + oProcessWorkspace.getProcessObjId() + ": ");
			WasdiLog.infoLog(sShellExString);
			
			ArrayList<String> asCmd = new ArrayList<>(Arrays.asList(sShellExString.split(" ")));
			
			if (WasdiConfig.Current.shellExecLocally == false) {
				asCmd.add(0, "launcher");
			}
			
			ShellExecReturn oShellExecReturn = RunTimeUtils.shellExec(asCmd, false);
			
			if (oShellExecReturn.isOperationOk()) {
				WasdiLog.infoLog(m_sLogPrefix + "executeProcess: executed!!!");
				m_aoLaunchedProcesses.put(oProcessWorkspace.getProcessObjId(), new Date());				
			}
			else {
				// We use throw here just not to duplicate the recovery code in catch
				throw new Exception("Error calling shell execute on launcher");
			}
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog(m_sLogPrefix + "executeProcess:  Exception" + oEx.toString());
			WasdiLog.errorLog(m_sLogPrefix + "executeProcess : try to set the process in Error");
			
			try {
				oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
				m_oProcessWorkspaceRepository.updateProcess(oProcessWorkspace);				
				WasdiLog.errorLog(m_sLogPrefix + "executeProcess: Error status set");
			}
			catch (Exception oInnerEx) {
				WasdiLog.errorLog(m_sLogPrefix + "executeProcess:  INNER Exception" + oInnerEx);
				oInnerEx.printStackTrace();
			}
			
			return null;
		}
		catch(Throwable oThrowable) {
			WasdiLog.errorLog(m_sLogPrefix + "executeProcess:  Exception" + oThrowable.toString());
			return null;
		}
		
		return oProcessWorkspace.getProcessObjId();
	}
	
	/**
	 * Kill a process
	 * @param iPid
	 * @return
	 */
	private boolean stopProcess(int iPid) {
		try {
			return RunTimeUtils.killProcess(iPid);
		} catch (Exception oEx) {
			WasdiLog.infoLog(m_sLogPrefix + "stopProcess: exception: " + oEx.getMessage());
			return false;
		}
	}
	
	
	/**
	 * Sleep method to ensure the start of a new process
	 */
	protected void waitForProcessToStart() {
		try {
			Thread.sleep(m_lWaitProcessStartMS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * Safe method to get the thread status
	 * @return
	 */
	public synchronized boolean getIsRunning() {
		return m_bRunning;
	}
	
	/**
	 * Stops the thread
	 */
	public synchronized void stopThread() {
		m_bRunning = false;
	}

	/**
	 * Get the list of operation types supported by this process scheduler
	 * @return
	 */
	public List<String> getSupportedTypes() {
		return m_asOperationTypes;
	}
	
	/**
	 * remove an operation type supported by this process scheduler
	 * @param sSupportedType
	 */
	public void removeSupportedType(String sSupportedType) {
		m_asOperationTypes.remove(sSupportedType);
	}
	
	/**
	 * add an operation type supported by this process scheduler
	 * @param sSupportedType
	 */
	public void addSupportedType(String sSupportedType) {
		m_asOperationTypes.add(sSupportedType);
	}
	
	/**
	 * Get Operation SubType
	 * @return
	 */
	public String getOperationSubType() {
		return m_sOperationSubType;
	}
	
	/**
	 * Set Operation SubType
	 * @param sOperationSubType
	 */
	public void setOperationSubType(String sOperationSubType) {
		this.m_sOperationSubType = sOperationSubType;
	}


}
