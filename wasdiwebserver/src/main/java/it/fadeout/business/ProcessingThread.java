package it.fadeout.business;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
	protected File parametersFilesFolder = null;
	/**
	 * sleeping time between iteractions
	 */
	protected long sleepingTimeMS = 2000;
	/**
	 * number concurrent process
	 */
	protected int numberOfConcurrentProcess = 1;
	/**
	 * running process identifiers (they are the mongo object ids of the processworkspace collection)
	 */
	protected String[] runningProcess;
	/**
	 * launcher installation path
	 */
	protected String launcherPath;
	/**
	 * java executable path
	 */
	protected String javaExe;
	/**
	 * mongo repository for processworkspace collection
	 */
	protected ProcessWorkspaceRepository repo;
	/**
	 * map of already launched parocess. Used to avoid multiple execution of the same process
	 */
	protected Map<String, Date> launched = new HashMap<String, Date>();
	
	/**
	 * constructor with parameters
	 * @param parametersFolder folder for the file containing the process parmeters
	 */
	public ProcessingThread(ServletConfig servletConfig) throws Exception {
		this(servletConfig, "ConcurrentProcess");
	}
	
	protected ProcessingThread(ServletConfig servletConfig, String concurrentProcessParameterName) throws Exception {
		super();
		
		File folder = new File(servletConfig.getInitParameter("SerializationPath"));
		if (!folder.isDirectory()) throw new Exception("ERROR: cannot access parameters folder: " + folder.getAbsolutePath());
		parametersFilesFolder = folder;
		
		try {
			int no = Integer.parseInt(servletConfig.getInitParameter(concurrentProcessParameterName));
			if (no>0) numberOfConcurrentProcess = no;
		} catch (Exception e) {
			e.printStackTrace();
		}
		runningProcess = new String[numberOfConcurrentProcess];
		
		try {
			long no = Long.parseLong(servletConfig.getInitParameter("ProcessingThreadSleepingTimeMS"));
			if (no>0) sleepingTimeMS = no;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		launcherPath = servletConfig.getInitParameter("LauncherPath");
		javaExe = servletConfig.getInitParameter("JavaExe");
	}
	

	@Override
	public void run() {
		
		System.out.println("ProcessingThread: run!");
		
		while (true) {
			
			try {
				//remove from lauched map all process older than 1 hour
				long tNow = System.currentTimeMillis();
				ArrayList<String> toClear = new ArrayList<String>();
				for (Entry<String, Date> entry : launched.entrySet()) {
					if (tNow - entry.getValue().getTime() > 3600000L) toClear.add(entry.getKey());
				}
				for (String key : toClear) {
					launched.remove(key);
					System.out.println("ProcessingThread: removing " + key + " from launched");
				}
				
				List<ProcessWorkspace> queuedProcess = null;
				int procIdx = 0;
				for (int i = 0; i < runningProcess.length; i++) {
					if (runningProcess[i]==null || isProcessDone(i)) {
						
						//if not yed loaded, load the list of process to execute
						if (queuedProcess==null) {
							checkRepo();
							queuedProcess = getQueuedProcess();
							
						}
						
						//try to execute a process
						String executedProcessId = null;
						while (procIdx<queuedProcess.size() && executedProcessId==null) {
							ProcessWorkspace process = queuedProcess.get(procIdx);
							if (!launched.containsKey(process.getProcessObjId())) {
								executedProcessId = executeProcess(process);
							} else {
								System.out.println("ProcessingThread: process lauched before: " + process.getProcessObjId());
							}
							procIdx++;
						}
						runningProcess[i] = executedProcessId;
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					//sleep before starting next iteraction
					sleep(sleepingTimeMS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}
		
	}

	protected List<ProcessWorkspace> getQueuedProcess() {
		List<ProcessWorkspace> queuedProcess = repo.GetQueuedProcess();
		
//		System.out.println("ProcessingThread: read process queue. size: " + queuedProcess.size());
//		for (ProcessWorkspace p : queuedProcess) {
//			System.out.println("ProcessingThread:      " + p.getProcessObjId());
//		}
		
		return queuedProcess;
	}


	private void checkRepo() {
		if (repo==null) repo = new ProcessWorkspaceRepository();
	}
	
	private String executeProcess(ProcessWorkspace process) {
		
		File parPath = new File(parametersFilesFolder, process.getProcessObjId());

		String shellExString = javaExe + " -jar " + launcherPath +" -operation " + process.getOperationType() + " -parameter " + parPath.getAbsolutePath();

		System.out.println("ProcessingThread: executing command for process " + process.getProcessObjId() + ": " + shellExString);

		try {
//			Process proc = Runtime.getRuntime().exec(shellExString);
			Runtime.getRuntime().exec("ls");
			System.out.println("ProcessingThread: executed!!!");
			launched.put(process.getProcessObjId(), new Date());
		} catch (IOException e) {
			e.printStackTrace();
			//if i'm unable to execute the process then return null
			return null;
		}
		
		return process.getProcessObjId();
	}


	private boolean isProcessDone(int i) {
		String procId = runningProcess[i];
		checkRepo();
		ProcessWorkspace process = repo.GetProcessByProcessObjId(procId);
		boolean ret = process==null || process.getStatus().equalsIgnoreCase(ProcessStatus.DONE.name());
		if (ret) runningProcess[i] = null;
		
		System.out.println("ProcessingThread: process " + procId + " DONE: " + ret);
		
		return ret;
	}

}
