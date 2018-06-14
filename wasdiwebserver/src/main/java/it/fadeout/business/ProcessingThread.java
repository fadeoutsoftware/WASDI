package it.fadeout.business;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;

import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.ProcessWorkspaceRepository;

/**
 * thread used to manage the process queue
 * @author doy
 */
public class ProcessingThread extends Thread {

	/**
	 * the folder that contains the serialized parameters used by each process
	 */
	protected File m_oParametersFilesFolder = null;
	/**
	 * sleeping time between iteractions
	 */
	protected long m_lSleepingTimeMS = 2000;
	/**
	 * number concurrent process
	 */
	protected int m_iNumberOfConcurrentProcess = 1;
	/**
	 * running process identifiers (they are the mongo object ids of the processworkspace collection)
	 */
	protected String[] m_asRunningProcessesSlots;
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
	 * map of already launched parocess. Used to avoid multiple execution of the same process
	 */
	protected Map<String, Date> m_aoLaunchedProcesses = new HashMap<String, Date>();
	
	protected String m_sLogPrefix = "ProcessingThread: ";
	
	 private volatile boolean m_bRunning = true;
	
	/**
	 * constructor with parameters
	 * @param parametersFolder folder for the file containing the process parmeters
	 */
	public ProcessingThread(ServletConfig oServletConfig) throws Exception {
		this(oServletConfig, "ConcurrentProcess");
	}
	
	/**
	 * Constructor with the name of the parameters with max concurrent processes
	 * @param oServletConfig
	 * @param sConcurrentProcessParameterName
	 * @throws Exception
	 */
	protected ProcessingThread(ServletConfig oServletConfig, String sConcurrentProcessParameterName) throws Exception {
		super();
		
		File oFolder = new File(oServletConfig.getInitParameter("SerializationPath"));
		if (!oFolder.isDirectory()) throw new Exception("ERROR: cannot access parameters folder: " + oFolder.getAbsolutePath());
		m_oParametersFilesFolder = oFolder;
		
		try {
			int iMaxConcurrents = Integer.parseInt(oServletConfig.getInitParameter(sConcurrentProcessParameterName));
			if (iMaxConcurrents>0) m_iNumberOfConcurrentProcess = iMaxConcurrents;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		m_asRunningProcessesSlots = new String[m_iNumberOfConcurrentProcess];
		
		try {
			long iThreadSleep = Long.parseLong(oServletConfig.getInitParameter("ProcessingThreadSleepingTimeMS"));
			if (iThreadSleep>0) m_lSleepingTimeMS = iThreadSleep;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		m_sLauncherPath = oServletConfig.getInitParameter("LauncherPath");
		m_sJavaExePath = oServletConfig.getInitParameter("JavaExe");
	}
	

	@Override
	public void run() {
		
		System.out.println(m_sLogPrefix + "run!");
		
		while (getIsRunning()) {
			
			try {
				//remove from lauched map all process older than 1 hour
				long lNow = System.currentTimeMillis();
				
				// List of array to clear
				ArrayList<String> aoToClear = new ArrayList<String>();
				
				// For each running entry
				for (Entry<String, Date> entry : m_aoLaunchedProcesses.entrySet()) {
					
					// If if is so old, kill it
					if (lNow - entry.getValue().getTime() > 3600000L) aoToClear.add(entry.getKey());
				}
				
				// Clear every killed process
				for (String key : aoToClear) {
					m_aoLaunchedProcesses.remove(key);
					System.out.println(m_sLogPrefix + "removing " + key + " from launched");
				}
				
				List<ProcessWorkspace> queuedProcess = null;
				
				int iProcIndex = 0;
				
				// For all the running available slots
				for (int iRunningSlots = 0; iRunningSlots < m_asRunningProcessesSlots.length; iRunningSlots++) {
					
					// If a slot is free now
					if (m_asRunningProcessesSlots[iRunningSlots]==null || isProcessDone(iRunningSlots)) {
						
						//if not yet loaded, load the list of process to execute
						if (queuedProcess==null) {
							checkRepo();
							queuedProcess = getQueuedProcess();
						}
						
						//try to execute a process
						String executedProcessId = null;
						
						while (iProcIndex<queuedProcess.size() && executedProcessId==null) {
							ProcessWorkspace process = queuedProcess.get(iProcIndex);
							if (!m_aoLaunchedProcesses.containsKey(process.getProcessObjId())) {
								executedProcessId = executeProcess(process);
								// Let the process start...
								try {
									//sleep before starting next iteraction
									sleep(2000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							} else {
								System.out.println(m_sLogPrefix + "process lauched before: " + process.getProcessObjId());
							}
							iProcIndex++;
						}
						m_asRunningProcessesSlots[iRunningSlots] = executedProcessId;
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					//sleep before starting next iteraction
					sleep(m_lSleepingTimeMS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}	
		}
		
		System.out.println(m_sLogPrefix + " STOPPED");
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
	 * Get the list of process in queue
	 * @return
	 */
	protected List<ProcessWorkspace> getQueuedProcess() {
		checkRepo();
		List<ProcessWorkspace> queuedProcess = m_oProcessWorkspaceRepository.GetQueuedProcess();
		
//		System.out.println(m_sLogPrefix + "read process queue. size: " + queuedProcess.size());
//		for (ProcessWorkspace p : queuedProcess) {
//			System.out.println(logPrefix + "     " + p.getProcessObjId());
//		}
		
		// Reverse the collection, otherwise the olders will dead of starvation
		Collections.reverse(queuedProcess);
		
		return queuedProcess;
	}


	private void checkRepo() {
		if (m_oProcessWorkspaceRepository==null) m_oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
	}
	
	private String executeProcess(ProcessWorkspace oProcessWorkspace) {
		
		File oParameterFilePath = new File(m_oParametersFilesFolder, oProcessWorkspace.getProcessObjId());

		String sShellExString = m_sJavaExePath + " -jar " + m_sLauncherPath +" -operation " + oProcessWorkspace.getOperationType() + " -parameter " + oParameterFilePath.getAbsolutePath();

		System.out.println(m_sLogPrefix + "executing command for process " + oProcessWorkspace.getProcessObjId() + ": " + sShellExString);

		try {
			
			Process oSystemProc = Runtime.getRuntime().exec(sShellExString);
			System.out.println(m_sLogPrefix + "executed!!!");
			
			m_aoLaunchedProcesses.put(oProcessWorkspace.getProcessObjId(), new Date());
			
		} catch (IOException e) {
			
			System.out.println(m_sLogPrefix + " executeProcess : Exception" + e.toString());
			e.printStackTrace();
			System.out.println(m_sLogPrefix + " executeProcess : try to set the process in Error");
			
			try {
				checkRepo();
				oProcessWorkspace.setStatus(ProcessStatus.ERROR.name());
				m_oProcessWorkspaceRepository.UpdateProcess(oProcessWorkspace);				
				System.out.println(m_sLogPrefix + " executeProcess : Error status set");
			}
			catch (Exception oInnerEx) {
				System.out.println(m_sLogPrefix + " executeProcess : INNER Exception" + oInnerEx.toString());
				oInnerEx.printStackTrace();
			}
			
			
			
			
			
			return null;
		}
		
		return oProcessWorkspace.getProcessObjId();
	}

	/**
	 * Check if a Process is Done (or finished some way, also Error or Stopped)
	 * @param i
	 * @return
	 */
	private boolean isProcessDone(int i) {
		String procId = m_asRunningProcessesSlots[i];
		ProcessWorkspace process = m_oProcessWorkspaceRepository.GetProcessByProcessObjId(procId);
		boolean ret = process==null || process.getStatus().equalsIgnoreCase(ProcessStatus.DONE.name()) || process.getStatus().equalsIgnoreCase(ProcessStatus.ERROR.name()) || process.getStatus().equalsIgnoreCase(ProcessStatus.STOPPED.name());
		if (ret) m_asRunningProcessesSlots[i] = null;
		
		//System.out.println(logPrefix + "process " + procId + " DONE: " + ret);
		
		return ret;
	}

}
