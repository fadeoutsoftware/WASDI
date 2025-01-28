package wasdi.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.SchedulerQueueConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.utils.LauncherOperationsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.docker.DockerUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;

public class ProcessScheduler {
	
	/**
	 * sleeping time after starting an app to let it really start
	 */
	protected long m_lWaitProcessStartMS = 2000;
	/**
	 * Timeout associated to this queue
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
	 * Flag to know if this Process Scheduler applies the Special Wait Condition 
	 * to avoid to trigger too many processes considering also the waiting queue
	 */
	protected boolean m_bSpecialWaitCondition = false;
	
	/**
	 * Max number of waiting processes admitted before breaking the FIFO rules
	 */
	protected int m_iMaxWaitingQueue = 100;
	
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
			
			// Read the queue config
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
					WasdiLog.errorLog(m_sLogPrefix + ".init: error ", e);
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
					WasdiLog.errorLog(m_sLogPrefix + ".init: error ", e);
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
					WasdiLog.errorLog(m_sLogPrefix + ".init: error ", e);
				}	
				
				m_bSpecialWaitCondition = oSchedulerQueueConfig.specialWaitCondition;
				m_iMaxWaitingQueue = oSchedulerQueueConfig.maxWaitingQueue;
			}
			else if (sSchedulerKey.equals("DEFAULT")) {
				WasdiLog.infoLog(m_sLogPrefix + ".init: this is the default scheduler");
				
				// Read Max Size of Concurrent Processes of this scheduler 
				try {
					
					if (!Utils.isNullOrEmpty(WasdiConfig.Current.scheduler.maxQueue)) {
						int iMaxConcurrents = Integer.parseInt(WasdiConfig.Current.scheduler.maxQueue);
						if (iMaxConcurrents>0) {
							m_iNumberOfConcurrentProcess = iMaxConcurrents;
							WasdiLog.infoLog(m_sLogPrefix + ".init: Max Concurrent Processes: " + m_iNumberOfConcurrentProcess);
						}					
					}
				} catch (Exception e) {
					WasdiLog.errorLog(m_sLogPrefix + ".init: error ", e);
				}
				
				// Read Timeout of this scheduler 
				try {
					if (!Utils.isNullOrEmpty(WasdiConfig.Current.scheduler.timeoutMs)) {
						long lTimeout = Long.parseLong(WasdiConfig.Current.scheduler.timeoutMs);
						if (lTimeout>0) {
							m_lTimeOutMs = lTimeout;
							WasdiLog.infoLog(m_sLogPrefix + ".init:  TimeOut Ms: " + m_lTimeOutMs);
						}
					}
				} catch (Exception e) {
					WasdiLog.errorLog(m_sLogPrefix + ".init: error ", e);
				}				
				
				
			}
			else {
				WasdiLog.errorLog(m_sLogPrefix + ".init: " + sSchedulerKey + " NOT RECOGNIZED");
			}
			
			try {
				
				if (!Utils.isNullOrEmpty(WasdiConfig.Current.scheduler.processingThreadWaitStartMS)) {
					long iStartWaitSleep = Long.parseLong( WasdiConfig.Current.scheduler.processingThreadWaitStartMS);
					if (iStartWaitSleep>0) {
						m_lWaitProcessStartMS = iStartWaitSleep;
						WasdiLog.infoLog(m_sLogPrefix + ".init: Wait Proc Start Ms: " + m_lWaitProcessStartMS);
					}
				}
			} catch (Exception e) {
				WasdiLog.errorLog(m_sLogPrefix + ".init: error ", e);
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
	public void cycle(List<ProcessWorkspace> aoRunningList, List<ProcessWorkspace> aoReadyList, List<ProcessWorkspace> aoCreatedList, List<ProcessWorkspace> aoWaitingList) {
					
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

					boolean bFifo = true;
					
					if (m_bSpecialWaitCondition) {
						if (aoWaitingList!=null) {
							aoWaitingList = getWaitingList(aoWaitingList);
							
							if (aoWaitingList.size() > m_iMaxWaitingQueue) {
								bFifo = false;
							}
						}
					}
					
					// Get the Created process
					ProcessWorkspace oCreatedProcess = aoCreatedList.get(0);

					if (!bFifo) {
						WasdiLog.warnLog(m_sLogPrefix + ".run: Waiting queue Emergency: activate NOT FIFO mitigation");
						ProcessWorkspace oCandidate = null;
						ProcessWorkspace oToUnblock = null;
						
						for (ProcessWorkspace oWaitingProcess : aoWaitingList) {
							for (ProcessWorkspace oPotentialNewProcess : aoCreatedList) {
								if (!Utils.isNullOrEmpty(oCreatedProcess.getParentId())) {
									if (oPotentialNewProcess.getParentId().equals(oWaitingProcess.getProcessObjId())) {
										oCandidate = oPotentialNewProcess;
										oToUnblock = oWaitingProcess;
									}
								}
							}
						}
						
						if (oCandidate != null && oToUnblock != null) {
							WasdiLog.warnLog(m_sLogPrefix + ".run: Found candiate created process " + oCandidate.getProcessObjId() + " that is blocking " + oToUnblock.getProcessObjId());
							oCreatedProcess = oCandidate;
						}
						else {
							WasdiLog.warnLog(m_sLogPrefix + ".run: in the waiting queue, no one is blocked by any of the created list. We jump this cycle waiting for the queue to be smaller");
							return;
						}
					}						
					
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
					
					if (bFifo) {
						aoCreatedList.remove(0);
					}
					else {
						return;
					}
				}
			}			
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog(m_sLogPrefix + ".run: " + oEx); 
		} 
	}
	
	
	/**
	 * Periodic checks of the Scheduler
	 * @param aoRunningList
	 * @param aoCreatedList
	 * @param aoReadyList
	 * @param aoWaitingList
	 */
	public void sometimesCheck(List<ProcessWorkspace> aoRunningList, List<ProcessWorkspace> aoCreatedList, List<ProcessWorkspace> aoReadyList, List<ProcessWorkspace> aoWaitingList) {
		
		try {
			if (m_aoLaunchedProcesses.size()!=0) {
				WasdiLog.infoLog(m_sLogPrefix + ".sometimesCheck: Launched Processes Size: " + m_aoLaunchedProcesses.size());
			}
			
			
			// Log if some queue is not empty
			try {
				
				int iRunningSize = getRunningList(aoRunningList).size();
				int iCreatedSize = getCreatedList(aoCreatedList).size();
				int iReadySize = getReadyList(aoReadyList).size();
				int iWaitingSize = getWaitingList(aoWaitingList).size();
				
				if (iRunningSize>0 || iCreatedSize >0 || iReadySize >0 || iWaitingSize>0) {
					WasdiLog.infoLog(m_sLogPrefix + ".sometimesCheck: Running = " + iRunningSize + " Created = " + iCreatedSize +  " Ready = " + iReadySize + " Waiting = " + iWaitingSize);					
				}
			}
			catch (Exception oInnerEx) {
				WasdiLog.errorLog(m_sLogPrefix + ".sometimesCheck: Exception logging the queue status ", oInnerEx);
			}
			
			
			// Check Launched Procesess list to remove from launched array
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
					WasdiLog.warnLog(m_sLogPrefix + ".sometimesCheck: Invalid Proc WS ID : " + sLaunchedProcessWorkspaceId);
					asLaunchedToDelete.add(sLaunchedProcessWorkspaceId);
				}
			}
			
			// Remove launched elements
			for (String sLaunchedToRemove : asLaunchedToDelete) {
				WasdiLog.infoLog(m_sLogPrefix + ".sometimesCheck: Remove from launched : " + sLaunchedToRemove);
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
				String sPidOrContainerId = "" + oRunningPws.getPid();

				if (!WasdiConfig.Current.shellExecLocally) {
					sPidOrContainerId = oRunningPws.getContainerId();
				}
				
				// Check if it is alive
				if (!Utils.isNullOrEmpty(sPidOrContainerId)) {
					if (!RunTimeUtils.isProcessStillAllive(sPidOrContainerId)) {
						// PID does not exists: recheck and remove
						WasdiLog.infoLog(m_sLogPrefix + ".sometimesCheck: Process " + oRunningPws.getProcessObjId() + " has PID " + sPidOrContainerId + ", status RUNNING but the process does not exists");
						
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
								WasdiLog.infoLog(m_sLogPrefix + ".sometimesCheck: **************Process " + oRunningPws.getProcessObjId() + " status changed to ERROR");
							}								
						}
					}
				}
				else {
					WasdiLog.infoLog(m_sLogPrefix + ".sometimesCheck: Process " + oRunningPws.getProcessObjId() + " with status RUNNING has null PID");
				}
				
				// Is there a timeout?
				if (m_lTimeOutMs != -1) {
					
					long lTimeoutMs = m_lTimeOutMs;
					
					// Check if this is an application with an override of the timeout
					
					// We need an operation type That must run a processor
					if (LauncherOperationsUtils.doesOperationLaunchApplication(oRunningPws.getOperationType())) {
						
						// Name of the app
						String sAppName = oRunningPws.getProductName();
						
						// Do we have this processor?
						ProcessorRepository oProcessorRepository = new ProcessorRepository();
						Processor oProcessor = oProcessorRepository.getProcessorByName(sAppName);
						
						if (oProcessor != null) {
							// Yes !! We can read its timeout!!
							lTimeoutMs = oProcessor.getTimeoutMs();
							
							if (lTimeoutMs != m_lTimeOutMs) {
								WasdiLog.infoLog(m_sLogPrefix + ".sometimesCheck: Process " + oRunningPws.getProcessObjId() + " is a processor with own Timeout " + lTimeoutMs);
							}
						}
					}						
					
					if (lTimeoutMs != -1) {
						// Check the last state change
						if (!Utils.isNullOrEmpty(oRunningPws.getLastStateChangeTimestamp())) {
							
							Double oLastChange = oRunningPws.getLastStateChangeTimestamp();
							Date oNow = new Date();
							long lTimeSpan = oNow.getTime() - oLastChange.longValue();
							
							// We ran over?
							if (lTimeSpan > m_lTimeOutMs) {
								// Change the status to STOPPED
								WasdiLog.infoLog(m_sLogPrefix + ".sometimesCheck: Process " + oRunningPws.getProcessObjId() + " is in Timeout");
								
								// Stop the process
								stopProcess(sPidOrContainerId);
								
								// Update the state
								oRunningPws.setStatus(ProcessStatus.STOPPED.name());
								
								if (Utils.isNullOrEmpty(oRunningPws.getOperationEndTimestamp())) {
									oRunningPws.setOperationEndTimestamp(Utils.nowInMillis());
								}

								m_oProcessWorkspaceRepository.updateProcess(oRunningPws);
								WasdiLog.infoLog(m_sLogPrefix + ".sometimesCheck: **************Process " + oRunningPws.getProcessObjId() + " Status changed to STOPPED");
							}
						}
						else {
							WasdiLog.infoLog(m_sLogPrefix + ".sometimesCheck: Process " + oRunningPws.getProcessObjId() + " Does not have a last state change date");
						}						
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
										
					
					// Wait 300 time more than standard waiting (10 mins by default!)
					if (lTimeSpan > 300*m_lWaitProcessStartMS) {
						// No good, set as ERROR
						WasdiLog.warnLog(m_sLogPrefix + ".sometimesCheck: **************Process " + oCreatedProcess.getProcessObjId() + " is GETTING OLD.. ");
						
						// Get the PID
						String sPidOrContainerId = "" + oCreatedProcess.getPid();

						if (!WasdiConfig.Current.shellExecLocally) {
							sPidOrContainerId = oCreatedProcess.getContainerId();
						}						
						
						// Check if it is alive
						if (!Utils.isNullOrEmpty(sPidOrContainerId)) {
							if (!RunTimeUtils.isProcessStillAllive(sPidOrContainerId)) {
								// PID does not exists: recheck and remove
								WasdiLog.warnLog(m_sLogPrefix + ".sometimesCheck: Process " + oCreatedProcess.getProcessObjId() + " has PID " + sPidOrContainerId + ", status CREATED and is in the launched list, but the process does not exists. We remove it from launched");
								m_aoLaunchedProcesses.remove(oCreatedProcess.getProcessObjId());
							}
							else {
								WasdiLog.warnLog(m_sLogPrefix + ".sometimesCheck: Process " + oCreatedProcess.getProcessObjId() + " in theory is running, but the state is still CREATED");
							}
						}
						else {
							WasdiLog.warnLog(m_sLogPrefix + ".sometimesCheck: Process " + oCreatedProcess.getProcessObjId() + " with status CREATED and is in the launched list has null PID. We remove it from launched");
							m_aoLaunchedProcesses.remove(oCreatedProcess.getProcessObjId());
						}
						
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
				String sPidOrContainerId = "" + oWaitingReadyPws.getPid();
				
				if (!WasdiConfig.Current.shellExecLocally) {
					sPidOrContainerId = oWaitingReadyPws.getContainerId();
				}				
				
				// Check if it is alive
				if (!Utils.isNullOrEmpty(sPidOrContainerId)) {
					if (!RunTimeUtils.isProcessStillAllive(sPidOrContainerId)) {
						// PID does not exists: recheck and remove
						WasdiLog.warnLog(m_sLogPrefix + ".sometimesCheck: Process " + oWaitingReadyPws.getProcessObjId() + " has PID " + sPidOrContainerId + ", is WAITING or READY but the process does not exists");
						
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
								WasdiLog.infoLog(m_sLogPrefix + ".sometimesCheck: **************Process " + oWaitingReadyPws.getProcessObjId() + " with WAITING or READY  status changed to ERROR");
							}							
						}
					}
				}
				else {
					WasdiLog.infoLog(m_sLogPrefix + ".sometimesCheck: Process " + oWaitingReadyPws.getProcessObjId() + " has null PID");
				}
			}	
		}
		catch (Exception oEx) {
			WasdiLog.errorLog(m_sLogPrefix + ".sometimesCheck: ", oEx); 
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
			WasdiLog.errorLog(m_sLogPrefix + ".filterOperationTypes: error ", oE);
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
			WasdiLog.errorLog(m_sLogPrefix + ".getRunningList: error ", oE);
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
			WasdiLog.errorLog(m_sLogPrefix + ".getRunnigList: error ", oE);
			return null;
		}
	}
	
	
	protected List<ProcessWorkspace> getCreatedList() {
		try {
			List<ProcessWorkspace> aoCreated = m_oProcessWorkspaceRepository.getProcessesByStateNode(ProcessStatus.CREATED.name(), m_sWasdiNode);
			return getCreatedList(aoCreated);
		}
		catch (Exception oE) {
			WasdiLog.errorLog(m_sLogPrefix + ".getCreatedList: error ", oE);
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
			WasdiLog.errorLog(m_sLogPrefix + ".getCreatedList: error ", oE);
			return null;
		}
	}
	
	
	protected List<ProcessWorkspace> getReadyList() {
		try {
			List<ProcessWorkspace> aoReady = m_oProcessWorkspaceRepository.getProcessesByStateNode(ProcessStatus.READY.name(), m_sWasdiNode, "lastStateChangeTimestamp", "lastStateChangeDate");
			return getReadyList(aoReady);
		}
		catch (Exception oE) {
			WasdiLog.errorLog(m_sLogPrefix + ".getReadyList: error ", oE);
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
			WasdiLog.errorLog(m_sLogPrefix + ".getReadyList: error ", oE);
			return null;
		}
	}	
	
	protected List<ProcessWorkspace> getWaitingList() {
		try {
			List<ProcessWorkspace> aoWaiting = m_oProcessWorkspaceRepository.getProcessesByStateNode(ProcessStatus.WAITING.name(), m_sWasdiNode, "lastStateChangeTimestamp", "lastStateChangeDate");
			return getWaitingList(aoWaiting);
		}
		catch (Exception oE) {
			WasdiLog.errorLog(m_sLogPrefix + ".getWaitingList: error ", oE);
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
			WasdiLog.errorLog(m_sLogPrefix + ".getWaitingList: error ", oE);
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
	
			String sShellExString = m_sJavaExePath + " -jar " + m_sLauncherPath +
					" -operation " + oProcessWorkspace.getOperationType() +
					" -parameter " + oProcessWorkspace.getProcessObjId();
	
			WasdiLog.infoLog(m_sLogPrefix + "executeProcess: executing command for process " + oProcessWorkspace.getProcessObjId() + ": ");
			WasdiLog.infoLog(sShellExString);
			
			ArrayList<String> asCmd = new ArrayList<>(Arrays.asList(sShellExString.split(" ")));
			
			if (WasdiConfig.Current.shellExecLocally == false) {
				asCmd.add(0, "launcher");
			}
			
			ShellExecReturn oShellExecReturn = RunTimeUtils.shellExec(asCmd, false);
			
			if (oShellExecReturn.isOperationOk()) {
				WasdiLog.infoLog(m_sLogPrefix + "executeProcess: executed " + oProcessWorkspace.getProcessObjId() + " !!!");
				
				if (!Utils.isNullOrEmpty(oShellExecReturn.getContainerId())) {
					ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
					oProcessWorkspace.setContainerId(oShellExecReturn.getContainerId());
					oProcessWorkspaceRepository.updateProcess(oProcessWorkspace);
				}
				
				m_aoLaunchedProcesses.put(oProcessWorkspace.getProcessObjId(), new Date());				
			}
			else {
				// We use throw here just not to duplicate the recovery code in catch
				throw new Exception("Error calling shell execute on launcher");
			}
		} 
		catch (Exception oEx) {
			
			String sProcessObjId = "NA";
			if (oProcessWorkspace!=null) {
				if (!Utils.isNullOrEmpty(oProcessWorkspace.getProcessObjId())) {
					sProcessObjId = oProcessWorkspace.getProcessObjId();
				}
			}
			WasdiLog.errorLog(m_sLogPrefix + "executeProcess [" + sProcessObjId +"]:  Exception: " + oEx.toString());
			WasdiLog.errorLog(m_sLogPrefix + "executeProcess [" + sProcessObjId +"]: try to set the process in Error");
			
			try {
				oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
				m_oProcessWorkspaceRepository.updateProcess(oProcessWorkspace);				
				WasdiLog.errorLog(m_sLogPrefix + "executeProcess[" + sProcessObjId + "]: Error status set");
			}
			catch (Exception oInnerEx) {
				WasdiLog.errorLog(m_sLogPrefix + "executeProcess[" + sProcessObjId + "]:  INNER Exception ", oInnerEx);
			}
			
			return null;
		}
		catch(Throwable oThrowable) {
			WasdiLog.errorLog(m_sLogPrefix + "executeProcess:  Worst Exception" + oThrowable.toString());
			return null;
		}
		
		return oProcessWorkspace.getProcessObjId();
	}
	
	/**
	 * Kill a process
	 * @param iPid
	 * @return
	 */
	private boolean stopProcess(String sPid) {
		
		try {
			if (WasdiConfig.Current.shellExecLocally) {
				int iPid = (int) Integer.parseInt(sPid);
				
				if (iPid>1) {
					RunTimeUtils.killProcess(iPid);
				}
			}
			else { 
				String sContainerId = sPid;
				
				if (Utils.isNullOrEmpty(sContainerId)) {
					WasdiLog.warnLog(m_sLogPrefix + "stopProcess: container id is null");
					return false;
				}
				
				DockerUtils oDockerUtils = new DockerUtils();
				
				if (!oDockerUtils.stop(sContainerId)) {
					WasdiLog.warnLog(m_sLogPrefix + "stopProcess: impossible to stop the container");
					return false;
				}
			}
			
			return true;
		} 
		catch (Exception oEx) {
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
