package wasdi.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
	 * In emergency mode, when no unblock candidate is found and running queue is not empty,
	 * fallback to default CREATED candidate every N cycles to avoid prolonged starvation.
	 */
	protected int m_iEmergencyFallbackEveryNCycles = 5;

	/**
	 * Counter of consecutive emergency cycles without unblock candidate.
	 */
	protected int m_iEmergencyNoUnblockStreak = 0;
	
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
	 * Enable fair round robin over CREATED processes by user
	 */
	protected boolean m_bFairRoundRobin = false;

	/**
	 * Max number of CREATED processes dispatched consecutively for one user turn
	 */
	protected int m_iFairRoundRobinMaxProcessesCount = 3;

	/**
	 * Enable second-level fair round robin over parentId groups inside one user queue
	 */
	protected boolean m_bFairRoundRobinParentId = false;

	/**
	 * Max number of CREATED processes dispatched consecutively for one parentId group turn
	 */
	protected int m_iFairRoundRobinParentIdMaxProcessesCount = 3;

	/**
	 * Persistent round robin state: user order and pointer
	 */
	protected ArrayList<String> m_asFairRoundRobinUsers = new ArrayList<String>();
	
	/**
	 * Pointer to the list of users
	 */
	protected int m_iFairRoundRobinCurrentUserIndex = 0;

	/**
	 * Persistent burst state: dispatched count for the current user turn
	 */
	protected int m_iFairRoundRobinCurrentUserBurstCount = 0;

	/**
	 * Persistent parentId round robin state by user
	 */
	protected Map<String, ArrayList<String>> m_aaoFairRoundRobinParentGroupsByUser = new HashMap<String, ArrayList<String>>();

	/**
	 * Pointer to current parentId group by user
	 */
	protected Map<String, Integer> m_aoFairRoundRobinCurrentParentIndexByUser = new HashMap<String, Integer>();

	/**
	 * Current burst count for selected parentId group by user
	 */
	protected Map<String, Integer> m_aoFairRoundRobinCurrentParentBurstCountByUser = new HashMap<String, Integer>();
	
	
	public boolean init(String sSchedulerKey) {
		
		try {
			m_iEmergencyFallbackEveryNCycles = WasdiConfig.Current.scheduler.emergencyFallbackEveryNCycles;
			if (m_iEmergencyFallbackEveryNCycles <= 0) {
				m_iEmergencyFallbackEveryNCycles = 1;
			}

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
							WasdiLog.debugLog(m_sLogPrefix + ".init: Max Concurrent Processes: " + m_iNumberOfConcurrentProcess);
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
							WasdiLog.debugLog(m_sLogPrefix + ".init:  TimeOut Ms: " + m_lTimeOutMs);
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
				m_bFairRoundRobin = oSchedulerQueueConfig.fairRoundRobin;
				m_iFairRoundRobinMaxProcessesCount = oSchedulerQueueConfig.fairRoundRobinMaxProcessesCount;
				m_bFairRoundRobinParentId = oSchedulerQueueConfig.fairRoundRobinParentId;
				m_iFairRoundRobinParentIdMaxProcessesCount = oSchedulerQueueConfig.fairRoundRobinParentIdMaxProcessesCount;

				if (m_iFairRoundRobinMaxProcessesCount <= 0) {
					m_iFairRoundRobinMaxProcessesCount = 1;
				}

				if (m_iFairRoundRobinParentIdMaxProcessesCount <= 0) {
					m_iFairRoundRobinParentIdMaxProcessesCount = 1;
				}

				if (m_bFairRoundRobin) {
					WasdiLog.infoLog(m_sLogPrefix + ".init: FairRoundRobin enabled. Max per user per round = " + m_iFairRoundRobinMaxProcessesCount);
				}
			}
			else if (sSchedulerKey.equals("DEFAULT")) {
				WasdiLog.debugLog(m_sLogPrefix + ".init: this is the default scheduler");
				
				// Read Max Size of Concurrent Processes of this scheduler 
				try {
					
					if (!Utils.isNullOrEmpty(WasdiConfig.Current.scheduler.maxQueue)) {
						int iMaxConcurrents = Integer.parseInt(WasdiConfig.Current.scheduler.maxQueue);
						if (iMaxConcurrents>0) {
							m_iNumberOfConcurrentProcess = iMaxConcurrents;
							WasdiLog.debugLog(m_sLogPrefix + ".init: Max Concurrent Processes: " + m_iNumberOfConcurrentProcess);
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
							WasdiLog.debugLog(m_sLogPrefix + ".init:  TimeOut Ms: " + m_lTimeOutMs);
						}
					}
				} catch (Exception e) {
					WasdiLog.errorLog(m_sLogPrefix + ".init: error ", e);
				}				
				
				m_bSpecialWaitCondition = WasdiConfig.Current.scheduler.defaultSpecialWaitCondition;
				m_iMaxWaitingQueue = WasdiConfig.Current.scheduler.defaultMaxWaitingQueue;
				m_bFairRoundRobin = WasdiConfig.Current.scheduler.defaultFairRoundRobin;
				m_iFairRoundRobinMaxProcessesCount = WasdiConfig.Current.scheduler.defaultFairRoundRobinMaxProcessesCount;
				m_bFairRoundRobinParentId = WasdiConfig.Current.scheduler.defaultFairRoundRobinParentId;
				m_iFairRoundRobinParentIdMaxProcessesCount = WasdiConfig.Current.scheduler.defaultFairRoundRobinParentIdMaxProcessesCount;

				if (m_iFairRoundRobinMaxProcessesCount <= 0) {
					m_iFairRoundRobinMaxProcessesCount = 1;
				}

				if (m_iFairRoundRobinParentIdMaxProcessesCount <= 0) {
					m_iFairRoundRobinParentIdMaxProcessesCount = 1;
				}

				if (m_bFairRoundRobin) {
					WasdiLog.infoLog(m_sLogPrefix + ".init: FairRoundRobin enabled. Max per user per round = " + m_iFairRoundRobinMaxProcessesCount);
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
						WasdiLog.debugLog(m_sLogPrefix + ".init: Wait Proc Start Ms: " + m_lWaitProcessStartMS);
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
		
		WasdiLog.debugLog(m_sLogPrefix + ".init: good to go :-)\n");
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
			boolean bEmergencyModeActive = false;
							
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
								bEmergencyModeActive = true;
							}
						}
					}
					
					// Get the Created process
					ProcessWorkspace oCreatedProcess = getNextCreatedProcessToDispatch(aoCreatedList);
					
					if (oCreatedProcess==null) continue;

					if (!bFifo) {
						WasdiLog.warnLog(m_sLogPrefix + ".run: Waiting queue Emergency: activate NOT FIFO mitigation");
						ProcessWorkspace oCandidate = null;
						ProcessWorkspace oToUnblock = null;
						
						for (ProcessWorkspace oWaitingProcess : aoWaitingList) {
							for (ProcessWorkspace oPotentialNewProcess : aoCreatedList) {
								if (!Utils.isNullOrEmpty(oPotentialNewProcess.getParentId())) {
									if (oPotentialNewProcess.getParentId().equals(oWaitingProcess.getProcessObjId())) {
										oCandidate = oPotentialNewProcess;
										oToUnblock = oWaitingProcess;
										break;
									}
								}
							}

							if (oCandidate != null && oToUnblock != null) {
								break;
							}
						}
						
						if (oCandidate != null && oToUnblock != null) {
							WasdiLog.warnLog(m_sLogPrefix + ".run: Found candiate created process " + oCandidate.getProcessObjId() + " that is blocking " + oToUnblock.getProcessObjId());
							oCreatedProcess = oCandidate;
							m_iEmergencyNoUnblockStreak = 0;
						}
						else {
							m_iEmergencyNoUnblockStreak++;
							if (aoRunningList.size() == 0) {
								WasdiLog.warnLog(m_sLogPrefix + ".run: emergency mode with empty running queue and no unblock candidate. Fallback to default candidate " + oCreatedProcess.getProcessObjId());
								m_iEmergencyNoUnblockStreak = 0;
							}
							else if (m_iEmergencyNoUnblockStreak >= m_iEmergencyFallbackEveryNCycles) {
								WasdiLog.warnLog(m_sLogPrefix + ".run: emergency mode no unblock candidate for " + m_iEmergencyNoUnblockStreak + " cycles. Fallback to default candidate " + oCreatedProcess.getProcessObjId());
								m_iEmergencyNoUnblockStreak = 0;
							}
							else {
								WasdiLog.warnLog(m_sLogPrefix + ".run: in the waiting queue, no one is blocked by any created process. Skip cycle " + m_iEmergencyNoUnblockStreak + "/" + m_iEmergencyFallbackEveryNCycles + " to reduce queue pressure");
								return;
							}
						}
					}						
					
					// Check if we did not launch this before
					if (!m_aoLaunchedProcesses.containsKey(oCreatedProcess.getProcessObjId())) {
													
						// Execute the process
						if (executeProcess(oCreatedProcess) != null) {
							
							// Give a little bit of time to the launcher to start
							waitForProcessToStart();
							
							// Move The process in the running list
							aoRunningList.add(oCreatedProcess);		
							
							WasdiLog.infoLog(m_sLogPrefix + ".run: Lauched " + oCreatedProcess.getProcessObjId() + " Running Queue Size = " + aoRunningList.size());
						}
						else {
							WasdiLog.infoLog(m_sLogPrefix + ".run: ERROR Lauching " + oCreatedProcess.getProcessObjId());
						}
					}
					
					if (bFifo) {
						removeCreatedProcessFromList(aoCreatedList, oCreatedProcess);
					}
					else {
						if (aoRunningList.size() > 0 && m_iEmergencyNoUnblockStreak != 0) {
							WasdiLog.debugLog(m_sLogPrefix + ".run: preserving emergency no-unblock streak value " + m_iEmergencyNoUnblockStreak);
						}
						return;
					}
				}

			if (!bEmergencyModeActive && m_iEmergencyNoUnblockStreak != 0) {
				m_iEmergencyNoUnblockStreak = 0;
			}
			}			
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog(m_sLogPrefix + ".run: " + oEx); 
		} 
	}

	/**
	 * Pick the next CREATED process to dispatch.
	 * When fair round robin is disabled, this is equivalent to FIFO head.
	 */
	protected ProcessWorkspace getNextCreatedProcessToDispatch(List<ProcessWorkspace> aoCreatedList) {
		if (aoCreatedList == null || aoCreatedList.size() == 0) {
			return null;
		}

		if (!m_bFairRoundRobin) {
			return aoCreatedList.get(0);
		}

		try {
			LinkedHashMap<String, ArrayList<ProcessWorkspace>> aoCreatedByUser = new LinkedHashMap<String, ArrayList<ProcessWorkspace>>();

			for (ProcessWorkspace oCreatedProcess : aoCreatedList) {
				String sUserKey = getFairRoundRobinUserKey(oCreatedProcess);
				if (!aoCreatedByUser.containsKey(sUserKey)) {
					aoCreatedByUser.put(sUserKey, new ArrayList<ProcessWorkspace>());
				}
				aoCreatedByUser.get(sUserKey).add(oCreatedProcess);
			}

			if (aoCreatedByUser.size() == 0) {
				return aoCreatedList.get(0);
			}

			ArrayList<String> asActiveUsers = new ArrayList<String>(aoCreatedByUser.keySet());

			for (int i = m_asFairRoundRobinUsers.size() - 1; i >= 0; i--) {
				if (!aoCreatedByUser.containsKey(m_asFairRoundRobinUsers.get(i))) {
					m_asFairRoundRobinUsers.remove(i);
					if (i < m_iFairRoundRobinCurrentUserIndex) {
						m_iFairRoundRobinCurrentUserIndex--;
					}
				}
			}

			// Add newly active users at the tail to preserve round continuity
			for (String sActiveUser : asActiveUsers) {
				if (!m_asFairRoundRobinUsers.contains(sActiveUser)) {
					m_asFairRoundRobinUsers.add(sActiveUser);
				}
			}

			if (m_asFairRoundRobinUsers.size() == 0) {
				return aoCreatedList.get(0);
			}

			int iUsers = m_asFairRoundRobinUsers.size();
			if (m_iFairRoundRobinCurrentUserIndex >= iUsers || m_iFairRoundRobinCurrentUserIndex < 0) {
				m_iFairRoundRobinCurrentUserIndex = 0;
				m_iFairRoundRobinCurrentUserBurstCount = 0;
			}

			for (int iStep = 0; iStep < iUsers; iStep++) {
				String sUserKey = m_asFairRoundRobinUsers.get(m_iFairRoundRobinCurrentUserIndex);
				ArrayList<ProcessWorkspace> aoUserCreated = aoCreatedByUser.get(sUserKey);

				if (aoUserCreated != null && aoUserCreated.size() > 0 && m_iFairRoundRobinCurrentUserBurstCount < m_iFairRoundRobinMaxProcessesCount) {
					ProcessWorkspace oSelected = selectNextCreatedProcessForUser(sUserKey, aoUserCreated);

					if (oSelected == null) {
						oSelected = aoUserCreated.get(0);
					}

					m_iFairRoundRobinCurrentUserBurstCount++;

					// Move to next user after max burst or if this was the last item of this user queue
					if (m_iFairRoundRobinCurrentUserBurstCount >= m_iFairRoundRobinMaxProcessesCount || aoUserCreated.size() == 1) {
						m_iFairRoundRobinCurrentUserIndex = (m_iFairRoundRobinCurrentUserIndex + 1) % iUsers;
						m_iFairRoundRobinCurrentUserBurstCount = 0;
					}

					return oSelected;
				}

				// Current user cannot be dispatched now: move to next user and reset burst counter
				m_iFairRoundRobinCurrentUserIndex = (m_iFairRoundRobinCurrentUserIndex + 1) % iUsers;
				m_iFairRoundRobinCurrentUserBurstCount = 0;
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog(m_sLogPrefix + ".getNextCreatedProcessToDispatch: exception ", oEx);
		}

		return aoCreatedList.get(0);
	}

	/**
	 * Select next CREATED process for one user queue, optionally applying parentId-level fairness.
	 */
	protected ProcessWorkspace selectNextCreatedProcessForUser(String sUserKey, List<ProcessWorkspace> aoUserCreated) {
		if (aoUserCreated == null || aoUserCreated.size() == 0) {
			return null;
		}

		if (!m_bFairRoundRobinParentId) {
			return aoUserCreated.get(0);
		}

		try {
			LinkedHashMap<String, ArrayList<ProcessWorkspace>> aoCreatedByParentGroup = new LinkedHashMap<String, ArrayList<ProcessWorkspace>>();

			for (ProcessWorkspace oCreatedProcess : aoUserCreated) {
				String sParentGroupKey = getFairRoundRobinParentGroupKey(oCreatedProcess);
				if (!aoCreatedByParentGroup.containsKey(sParentGroupKey)) {
					aoCreatedByParentGroup.put(sParentGroupKey, new ArrayList<ProcessWorkspace>());
				}
				aoCreatedByParentGroup.get(sParentGroupKey).add(oCreatedProcess);
			}

			if (aoCreatedByParentGroup.size() == 0) {
				return aoUserCreated.get(0);
			}

			ArrayList<String> asActiveParentGroups = new ArrayList<String>(aoCreatedByParentGroup.keySet());
			ArrayList<String> asParentGroupsOrder = m_aaoFairRoundRobinParentGroupsByUser.get(sUserKey);

			if (asParentGroupsOrder == null) {
				asParentGroupsOrder = new ArrayList<String>();
				m_aaoFairRoundRobinParentGroupsByUser.put(sUserKey, asParentGroupsOrder);
			}

			int iCurrentParentIndex = 0;
			if (m_aoFairRoundRobinCurrentParentIndexByUser.containsKey(sUserKey)) {
				iCurrentParentIndex = m_aoFairRoundRobinCurrentParentIndexByUser.get(sUserKey);
			}

			for (int i = asParentGroupsOrder.size() - 1; i >= 0; i--) {
				if (!aoCreatedByParentGroup.containsKey(asParentGroupsOrder.get(i))) {
					asParentGroupsOrder.remove(i);
					if (i < iCurrentParentIndex) {
						iCurrentParentIndex--;
					}
				}
			}

			for (String sActiveParentGroup : asActiveParentGroups) {
				if (!asParentGroupsOrder.contains(sActiveParentGroup)) {
					asParentGroupsOrder.add(sActiveParentGroup);
				}
			}

			if (asParentGroupsOrder.size() == 0) {
				return aoUserCreated.get(0);
			}

			int iCurrentParentBurstCount = 0;
			if (m_aoFairRoundRobinCurrentParentBurstCountByUser.containsKey(sUserKey)) {
				iCurrentParentBurstCount = m_aoFairRoundRobinCurrentParentBurstCountByUser.get(sUserKey);
			}

			int iParentGroupsCount = asParentGroupsOrder.size();
			if (iCurrentParentIndex >= iParentGroupsCount || iCurrentParentIndex < 0) {
				iCurrentParentIndex = 0;
				iCurrentParentBurstCount = 0;
			}

			for (int iStep = 0; iStep < iParentGroupsCount; iStep++) {
				String sCurrentParentGroup = asParentGroupsOrder.get(iCurrentParentIndex);
				ArrayList<ProcessWorkspace> aoParentGroupCreated = aoCreatedByParentGroup.get(sCurrentParentGroup);

				if (aoParentGroupCreated != null && aoParentGroupCreated.size() > 0 && iCurrentParentBurstCount < m_iFairRoundRobinParentIdMaxProcessesCount) {
					ProcessWorkspace oSelected = aoParentGroupCreated.get(0);
					iCurrentParentBurstCount++;

					if (iCurrentParentBurstCount >= m_iFairRoundRobinParentIdMaxProcessesCount || aoParentGroupCreated.size() == 1) {
						iCurrentParentIndex = (iCurrentParentIndex + 1) % iParentGroupsCount;
						iCurrentParentBurstCount = 0;
					}

					m_aoFairRoundRobinCurrentParentIndexByUser.put(sUserKey, iCurrentParentIndex);
					m_aoFairRoundRobinCurrentParentBurstCountByUser.put(sUserKey, iCurrentParentBurstCount);
					return oSelected;
				}

				iCurrentParentIndex = (iCurrentParentIndex + 1) % iParentGroupsCount;
				iCurrentParentBurstCount = 0;
			}

			m_aoFairRoundRobinCurrentParentIndexByUser.put(sUserKey, iCurrentParentIndex);
			m_aoFairRoundRobinCurrentParentBurstCountByUser.put(sUserKey, iCurrentParentBurstCount);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog(m_sLogPrefix + ".selectNextCreatedProcessForUser: exception ", oEx);
		}

		return aoUserCreated.get(0);
	}

	/**
	 * Remove from created list by process id. Fallback to index 0 to preserve progress.
	 */
	protected void removeCreatedProcessFromList(List<ProcessWorkspace> aoCreatedList, ProcessWorkspace oCreatedProcess) {
		if (aoCreatedList == null || aoCreatedList.size() == 0) {
			return;
		}

		if (oCreatedProcess == null || Utils.isNullOrEmpty(oCreatedProcess.getProcessObjId())) {
			aoCreatedList.remove(0);
			return;
		}

		String sProcessObjId = oCreatedProcess.getProcessObjId();

		for (int i = 0; i < aoCreatedList.size(); i++) {
			ProcessWorkspace oItem = aoCreatedList.get(i);
			if (oItem != null && sProcessObjId.equals(oItem.getProcessObjId())) {
				aoCreatedList.remove(i);
				return;
			}
		}

		aoCreatedList.remove(0);
	}

	/**
	 * User key used for fair scheduling.
	 */
	protected String getFairRoundRobinUserKey(ProcessWorkspace oCreatedProcess) {
		if (oCreatedProcess == null || Utils.isNullOrEmpty(oCreatedProcess.getUserId())) {
			return "UNKNOWN";
		}

		return oCreatedProcess.getUserId();
	}

	/**
	 * ParentId-group key used for second-level fair scheduling inside one user queue.
	 */
	protected String getFairRoundRobinParentGroupKey(ProcessWorkspace oCreatedProcess) {
		if (oCreatedProcess == null) {
			return "UNKNOWN_PARENT";
		}

		if (!Utils.isNullOrEmpty(oCreatedProcess.getParentId())) {
			return "PARENT:" + oCreatedProcess.getParentId();
		}

		if (!Utils.isNullOrEmpty(oCreatedProcess.getProcessObjId())) {
			return "SELF:" + oCreatedProcess.getProcessObjId();
		}

		return "NO_PARENT";
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
					WasdiLog.infoLog(m_sLogPrefix + ".sometimesCheck: Running = " + iRunningSize + " Created = " + iCreatedSize +  " Ready = " + iReadySize + " Waiting = " + iWaitingSize + " Max Running Queue = " + m_iNumberOfConcurrentProcess + " Max Waiting Queue = " + m_iMaxWaitingQueue);					
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
							if (lTimeSpan > lTimeoutMs) {
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
	
			
			ArrayList<String> asCmd = new ArrayList<>(Arrays.asList(sShellExString.split(" ")));
			
			if (WasdiConfig.Current.shellExecLocally == false) {
				asCmd.add(0, "launcher");
			}
			
			if (WasdiConfig.Current.scheduler.redirectLauncherOutputs && !Utils.isNullOrEmpty(WasdiConfig.Current.scheduler.launcherOutputPath)) {
				String sPath = WasdiConfig.Current.scheduler.launcherOutputPath;
				
				asCmd.add(">>");
				asCmd.add(sPath);
				asCmd.add("2>&1");
			}
			
			sShellExString = "";
			if (asCmd != null) {
				for (String sArg : asCmd) {
					sShellExString += sArg + " ";
				}			
			}			
			
			WasdiLog.infoLog(m_sLogPrefix + "executeProcess: executing command for process " + oProcessWorkspace.getProcessObjId() + ": ");
			WasdiLog.infoLog(sShellExString);
			
			ShellExecReturn oShellExecReturn = RunTimeUtils.shellExec(asCmd, false);
			
			if (oShellExecReturn.isOperationOk()) {
				WasdiLog.debugLog(m_sLogPrefix + "executeProcess: executed " + oProcessWorkspace.getProcessObjId() + " !!!");
				
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
				if (oProcessWorkspace!=null) {
					oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
					m_oProcessWorkspaceRepository.updateProcess(oProcessWorkspace);				
					WasdiLog.errorLog(m_sLogPrefix + "executeProcess[" + sProcessObjId + "]: Error status set");					
				}
				else {
					WasdiLog.errorLog(m_sLogPrefix + "executeProcess[" + sProcessObjId + "]: oProcessWorkspace is null!!!");
				}
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
