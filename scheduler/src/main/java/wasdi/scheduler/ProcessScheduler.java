package wasdi.scheduler;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.utils.Utils;

public class ProcessScheduler extends Thread {
	
	/**
	 * the folder that contains the serialised parameters used by each process
	 */
	protected File m_oParametersFilesFolder = null;
	/**
	 * sleeping time between iterations
	 */
	protected long m_lSleepingTimeMS = 2000;
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
	
	/**
	 * Parameter to slow down the scheduler special checks that will be
	 * done only every m_iSometimesCounter cycles
	 */
	protected int m_iSometimesCounter = 30;
	
	public boolean init(String sSchedulerKey) {
		
		try {
			// Save the scheduler Key
			m_sSchedulerKey = sSchedulerKey;
			// Init the scheduler log prefix
			m_sLogPrefix = m_sSchedulerKey + ": ";
			
			//Read the Serialisation Path
			File oFolder = new File(ConfigReader.getPropValue("SerializationPath", "/usr/lib/wasdi/params/"));
			
			if (!oFolder.isDirectory()) {
				WasdiScheduler.error(m_sLogPrefix + ".init: cannot access parameters folder: " + oFolder.getAbsolutePath());
			}
			
			// Save the path 
			m_oParametersFilesFolder = oFolder;
			
			// Read Max Size of Concurrent Processes of this scheduler 
			try {
				int iMaxConcurrents = Integer.parseInt(ConfigReader.getPropValue(m_sSchedulerKey.toUpperCase()+"_MAX_QUEUE"));
				if (iMaxConcurrents>0) {
					m_iNumberOfConcurrentProcess = iMaxConcurrents;
					WasdiScheduler.log(m_sLogPrefix + ".init: Max Concurrent Processes: " + m_iNumberOfConcurrentProcess);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// Read Timeout of this scheduler 
			try {
				long lTimeout = Long.parseLong(ConfigReader.getPropValue(m_sSchedulerKey.toUpperCase()+"_TIMEOUT_MS"));
				if (lTimeout>0) {
					m_lTimeOutMs = lTimeout;
					WasdiScheduler.log(m_sLogPrefix + ".init:  TimeOut Ms: " + m_lTimeOutMs);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			// Read Operation Type supported 
			try {
				// Get the string from config
				String sOperationTypes = ConfigReader.getPropValue(m_sSchedulerKey.toUpperCase()+"_OP_TYPES", "");
				
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
						String sOperationSubType = ConfigReader.getPropValue(m_sSchedulerKey.toUpperCase()+"_OP_SUB_TYPE", "");
						
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
				long iThreadSleep = Long.parseLong(ConfigReader.getPropValue("ProcessingThreadSleepingTimeMS", "2000"));
				if (iThreadSleep>0) {
					m_lSleepingTimeMS = iThreadSleep;
					WasdiScheduler.log(m_sLogPrefix + ".init: CatNap Ms: " + m_lSleepingTimeMS);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			try {
				long iStartWaitSleep = Long.parseLong(ConfigReader.getPropValue("ProcessingThreadWaitStartMS", "2000"));
				if (iStartWaitSleep>0) {
					m_lWaitProcessStartMS = iStartWaitSleep;
					WasdiScheduler.log(m_sLogPrefix + ".init: Wait Proc Start Ms: " + m_lWaitProcessStartMS);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// Read the Lancher Path
			m_sLauncherPath = ConfigReader.getPropValue("LauncherPath", "/usr/lib/wasdi/launcher/launcher.jar");
			// Read Java Exe Path
			m_sJavaExePath = ConfigReader.getPropValue("JavaExe", "java");
			// Read Wasdi Node Id
			m_sWasdiNode = ConfigReader.getPropValue("WASDI_NODE", "wasdi");
			// Read the Kill command
			m_sKillCommand = ConfigReader.getPropValue("KILL_COMMAND", "kill -9 ");
			
			// Create the Repo
			m_oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
		}
		catch (Exception oEx) {
			WasdiScheduler.error(m_sLogPrefix + ".init: init Exception: " + oEx);
			return false;
		}
		WasdiScheduler.log(m_sLogPrefix + ".init: good to go :-)\n");
		return true;
	}
		
	@Override
	public void run() {
		
		WasdiScheduler.log(m_sLogPrefix + ".run: Thread started");
		
		int iSometimes = 0;
		
		// Infinite Loop		
		while(getIsRunning()) {
			
			try {
								
				// Get the updated list of running processes
				List<ProcessWorkspace> aoRunningList = getRunningList();
				
				// Do we have any free slot?
				if (aoRunningList.size() < m_iNumberOfConcurrentProcess) {
					
					// Yes: get the list of Ready Processes
					List<ProcessWorkspace> aoReadyList = getReadyList();
					// Get the list of Created Processes
					List<ProcessWorkspace> aoCreatedList = getCreatedList();
					
					// For each ready process
					while (aoReadyList.size()> 0) {
						
						// If we fished free slots, stop the cycle
						if (aoRunningList.size()>=m_iNumberOfConcurrentProcess) break;
												
						// Get the ready process
						ProcessWorkspace oReadyProcess = aoReadyList.get(0);
						
						// Update the status to running
						oReadyProcess.setStatus(ProcessStatus.RUNNING.name());
						m_oProcessWorkspaceRepository.updateProcess(oReadyProcess);
						
						WasdiScheduler.log(m_sLogPrefix + ".run: Resumed " + oReadyProcess.getProcessObjId());
						
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
								
								WasdiScheduler.log(m_sLogPrefix + ".run: Lauched " + oCreatedProcess.getProcessObjId());
								
								// Give a little bit of time to the launcher to start
								waitForProcessToStart();
								
								// Move The process in the running list
								aoRunningList.add(oCreatedProcess);		
							}
							else {
								WasdiScheduler.log(m_sLogPrefix + ".run: ERROR Lauching " + oCreatedProcess.getProcessObjId());
							}
						}
						
						aoCreatedList.remove(0);
					}
				}
				else {
					//if (iSometimes == m_iSometimesCounter) WasdiScheduler.log(m_sLogPrefix + "Running Queue full, next cycle.");
				}
				
				// Periodic sanification tasks
				if (iSometimes == m_iSometimesCounter) {
					
					iSometimes = 0;
					
					if (m_aoLaunchedProcesses.size()!=0) {
						WasdiScheduler.log(m_sLogPrefix + ".run: Launched Processes Size: " + m_aoLaunchedProcesses.size());
					}
					
					// Check Launched Proc list
					ArrayList<String> asLaunchedToDelete = new ArrayList<String>();
					
					for (String sLaunchedProcessWorkspaceId : m_aoLaunchedProcesses.keySet()) {
						
						// If it is launched, it "needs" CREATED State. If not, has been done in some way
						ProcessWorkspace oLaunched = m_oProcessWorkspaceRepository.getProcessByProcessObjId(sLaunchedProcessWorkspaceId);
						if (oLaunched.getStatus().equals(ProcessStatus.CREATED.name()) == false) {
							asLaunchedToDelete.add(sLaunchedProcessWorkspaceId);
						}
					}
					
					// Remove launched elements
					for (String sLaunchedToRemove : asLaunchedToDelete) {
						WasdiScheduler.log(m_sLogPrefix + ".run: Remove from launched : " + sLaunchedToRemove);
						m_aoLaunchedProcesses.remove(sLaunchedToRemove);
					}
					
					// Get the updated list of running processes
					aoRunningList = getRunningList();
					
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
							if (!Utils.isProcessStillAllive(sPid)) {
								// PID does not exists: recheck and remove
								WasdiScheduler.log(m_sLogPrefix + ".run: Process " + oRunningPws.getProcessObjId() + " has PID " + sPid + ", status RUNNING but the process does not exists");
								
								// Read Again to be sure
								ProcessWorkspace oCheckProcessWorkspace = m_oProcessWorkspaceRepository.getProcessByProcessObjId(oRunningPws.getProcessObjId());
								
								// Is it still running?
								if (oCheckProcessWorkspace.getStatus().equals(ProcessStatus.RUNNING.name())) {
									
									// Force to error
									oCheckProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
									// Set the operation end date
									if (Utils.isNullOrEmpty(oCheckProcessWorkspace.getOperationEndDate())) {
										oCheckProcessWorkspace.setOperationEndDate(Utils.getFormatDate(new Date()));
									}
									// Update the process
									m_oProcessWorkspaceRepository.updateProcess(oCheckProcessWorkspace);
									WasdiScheduler.log(m_sLogPrefix + ".run: **************Process " + oRunningPws.getProcessObjId() + " status changed to ERROR");
								}							
							}
						}
						else {
							WasdiScheduler.log(m_sLogPrefix + ".run: Process " + oRunningPws.getProcessObjId() + " with status RUNNING has null PID");
						}
						
						// Is there a timeout?
						if (m_lTimeOutMs != -1) {
							// Check the last state change
							if (!Utils.isNullOrEmpty(oRunningPws.getLastStateChangeDate())) {
								
								Date oLastChange = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(oRunningPws.getLastStateChangeDate());
								Date oNow = new Date();
								long lTimeSpan = oNow.getTime() - oLastChange.getTime();
								
								// We ran over?
								if (lTimeSpan > m_lTimeOutMs) {
									// Change the status to STOPPED
									WasdiScheduler.log(m_sLogPrefix + ".run: Process " + oRunningPws.getProcessObjId() + " is in Timeout");
									
									// Stop the process
									stopProcess(oRunningPws.getPid());
									
									// Update the state
									oRunningPws.setStatus(ProcessStatus.STOPPED.name());
									
									if (Utils.isNullOrEmpty(oRunningPws.getOperationEndDate())) {
										oRunningPws.setOperationEndDate(Utils.getFormatDate(new Date()));
									}
		
									m_oProcessWorkspaceRepository.updateProcess(oRunningPws);
									WasdiScheduler.log(m_sLogPrefix + ".run: **************Process " + oRunningPws.getProcessObjId() + " Status changed to STOPPED");
								}
							}
							else {
								WasdiScheduler.log(m_sLogPrefix + ".run: Process " + oRunningPws.getProcessObjId() + " Does not have a last state change date");
							}
						}
					}
					
					// Get the list of Created Processes
					List<ProcessWorkspace> aoCreatedList = getCreatedList();
					
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
								WasdiScheduler.warn(m_sLogPrefix + ".run: **************Process " + oCreatedProcess.getProcessObjId() + " is GETTING OLD.. ");
								
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
					List<ProcessWorkspace> aoWaitingReadyList = getReadyList();
					aoWaitingReadyList.addAll(getWaitingList());
					
					// For each running process check pid and timeout
					for (ProcessWorkspace oWaitingReadyPws : aoWaitingReadyList) {
						
						// All processes in waiting o ready are anyway started so they can be removed from the launched list
						if (m_aoLaunchedProcesses.containsKey(oWaitingReadyPws.getProcessObjId())) {
							m_aoLaunchedProcesses.remove(oWaitingReadyPws.getProcessObjId());
						}
						
						// Get the PID
						String sPid = "" + oWaitingReadyPws.getPid();
						
						// Check if it is alive
						if (!Utils.isNullOrEmpty(sPid)) {
							if (!Utils.isProcessStillAllive(sPid)) {
								// PID does not exists: recheck and remove
								WasdiScheduler.warn(m_sLogPrefix + ".run: Process " + oWaitingReadyPws.getProcessObjId() + " has PID " + sPid + ", is WAITING or READY but the process does not exists");
								
								// Read Again to be sure
								ProcessWorkspace oCheckProcessWorkspace = m_oProcessWorkspaceRepository.getProcessByProcessObjId(oWaitingReadyPws.getProcessObjId());
								
								// Is it still running?
								if (oCheckProcessWorkspace.getStatus().equals(ProcessStatus.RUNNING.name()) ||
										oCheckProcessWorkspace.getStatus().equals(ProcessStatus.WAITING.name()) ||
										oCheckProcessWorkspace.getStatus().equals(ProcessStatus.READY.name())) {
									
									// It cannot be in any of these states if the PID does not exists
									
									// Force to error
									oCheckProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
									// Set the operation end date
									if (Utils.isNullOrEmpty(oCheckProcessWorkspace.getOperationEndDate())) {
										oCheckProcessWorkspace.setOperationEndDate(Utils.getFormatDate(new Date()));
									}
									
									// Update the process
									m_oProcessWorkspaceRepository.updateProcess(oCheckProcessWorkspace);
									WasdiScheduler.log(m_sLogPrefix + ".run: **************Process " + oWaitingReadyPws.getProcessObjId() + " with WAITING or READY  status changed to ERROR");
								}							
							}
						}
						else {
							WasdiScheduler.log(m_sLogPrefix + "Process " + oWaitingReadyPws.getProcessObjId() + " has null PID");
						}
					}					
				}

				iSometimes ++;

				
			} 
			catch (Exception oEx) {
				WasdiScheduler.error(m_sLogPrefix + ".run: " + oEx); 
				oEx.printStackTrace();
			} 
			finally {
				try {
					//Sleep before starting next iteration
					catnap();
				} 
				catch (Exception oEx) {
					WasdiScheduler.error(m_sLogPrefix + ".run: exception with finally: " + oEx);
					oEx.printStackTrace();
				}
			}
		}
	}
	
	
	/**
	 * Filter the list of operations with the supported types
	 * @param aoInputList
	 * @return
	 */
	protected List<ProcessWorkspace> filterOperationTypes(List<ProcessWorkspace> aoInputList) {
		try {
			for (int iProcWs = aoInputList.size()-1; iProcWs>=0; iProcWs--) {
				
				// Filter on scheduler operation types
				if (!m_asOperationTypes.contains(aoInputList.get(iProcWs).getOperationType())) {
					aoInputList.remove(iProcWs);
				}
				else {
					// Filter on operation subtype if configured
					if (m_asOperationTypes.size()==1) {
						if (!Utils.isNullOrEmpty(m_sOperationSubType)) {
							if (aoInputList.get(iProcWs).getOperationSubType()!= null) {
								if (!aoInputList.get(iProcWs).getOperationSubType().equals(m_sOperationSubType)) {
									aoInputList.remove(iProcWs);
								}							
							}
						}					
					}					
				}
			}
			
			return aoInputList;
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
			aoRunning = filterOperationTypes(aoRunning);
			Collections.reverse(aoRunning);
			return aoRunning;
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
	protected List<ProcessWorkspace> getCreatedList() {
		try {
			List<ProcessWorkspace> aoCreated = m_oProcessWorkspaceRepository.getProcessesByStateNode(ProcessStatus.CREATED.name(), m_sWasdiNode);
			aoCreated = filterOperationTypes(aoCreated);
			Collections.reverse(aoCreated);
			return aoCreated;
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
	protected List<ProcessWorkspace> getReadyList() {
		try {
			List<ProcessWorkspace> aoReady = m_oProcessWorkspaceRepository.getProcessesByStateNode(ProcessStatus.READY.name(), m_sWasdiNode, "lastStateChangeDate");
			aoReady = filterOperationTypes(aoReady);
			Collections.reverse(aoReady);
			return aoReady;
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
	protected List<ProcessWorkspace> getWaitingList() {
		try {
			List<ProcessWorkspace> aoReady = m_oProcessWorkspaceRepository.getProcessesByStateNode(ProcessStatus.WAITING.name(), m_sWasdiNode, "lastStateChangeDate");
			aoReady = filterOperationTypes(aoReady);
			Collections.reverse(aoReady);
			return aoReady;
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
	
			WasdiScheduler.log(m_sLogPrefix + "executeProcess: executing command for process " + oProcessWorkspace.getProcessObjId() + ": ");
			WasdiScheduler.log(sShellExString);

			
			Process oSystemProc = Runtime.getRuntime().exec(sShellExString);
			WasdiScheduler.log(m_sLogPrefix + "executeProcess: executed!!!");
			m_aoLaunchedProcesses.put(oProcessWorkspace.getProcessObjId(), new Date());
			
		} 
		catch (IOException oEx) {
			WasdiScheduler.error(m_sLogPrefix + "executeProcess:  Exception" + oEx.toString());
			oEx.printStackTrace();
			WasdiScheduler.error(m_sLogPrefix + "executeProcess : try to set the process in Error");
			
			try {
				oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
				m_oProcessWorkspaceRepository.updateProcess(oProcessWorkspace);				
				WasdiScheduler.error(m_sLogPrefix + "executeProcess: Error status set");
			}
			catch (Exception oInnerEx) {
				WasdiScheduler.error(m_sLogPrefix + "executeProcess:  INNER Exception" + oInnerEx);
				oInnerEx.printStackTrace();
			}
			
			return null;
		}
		catch(Throwable oThrowable) {
			WasdiScheduler.error(m_sLogPrefix + "executeProcess:  Exception" + oThrowable.toString());
			oThrowable.printStackTrace();
			return null;
		}
		
		
		return oProcessWorkspace.getProcessObjId();
	}
	
	/**
	 * Kill a process
	 * @param iPid
	 * @return
	 */
	private int stopProcess(int iPid) {
		try {
			// kill process command
			String sShellExString = m_sKillCommand + " " + iPid;
			
			WasdiScheduler.log(m_sLogPrefix + "stopProcess: shell exec " + sShellExString);
			
			Process oProc;
		
			oProc = Runtime.getRuntime().exec(sShellExString);
			return oProc.waitFor();
		} catch (Exception e) {
			WasdiScheduler.log(m_sLogPrefix + "stopProcess: exception: " + e.getMessage());
			return -1;
		}
	}
	
	/**
	 * Sleep method for the Scheduler Cycle
	 */
	protected void catnap() {
		try {
			sleep(m_lSleepingTimeMS);
		} catch (InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * Sleep method to ensure the start of a new process
	 */
	protected void waitForProcessToStart() {
		try {
			sleep(m_lWaitProcessStartMS);
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
		interrupt();
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
