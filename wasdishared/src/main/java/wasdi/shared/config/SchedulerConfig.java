package wasdi.shared.config;

import java.util.ArrayList;

/**
 * Generic Scheduler Config
 * @author p.campanella
 *
 */
public class SchedulerConfig {
	
	/**
	 * Number of Ms to wait after a process is started
	 */
	public String processingThreadWaitStartMS;
	
	/**
	 * Number of Ms to sleep between scheduler cycles
	 */
	public String processingThreadSleepingTimeMS;
	
	/**
	 * Full path of Launcher jar
	 */
	public String launcherPath;
	
	/**
	 * Local Java Command Line
	 */
	public String javaExe;
	
	/**
	 * OS Kill Command
	 */
	public String killCommand;
	
	/**
	 * Default Max Queue size
	 */
	public String maxQueue;
	
	/**
	 * Default Queue timeout in ms
	 */
	public String timeoutMs;
	
	/**
	 * Direction to use to order the list of process workspaces in the queue of the scheduler 
	 */
	public int lastStateChangeDateOrderBy = -1;
	
	/**
	 * Number of cycles that must be executed before starting the periodic checks of the scheduler.
	 * This will mean that the checks are done, in seconds, maximum every: 
	 * (processingThreadSleepingTimeMS * sometimesCheckCounter)/1000 [seconds].
	 * By default is 2000 * 30 / 1000 = 60 secs, 1 min,.
	 * In reality we need to add to this time the time needed by the scheduler to do his job 
	 */
	public int sometimesCheckCounter = 30;
	
	/**
	 * Counter of the watch Dog. 
	 * We assume that if we have only waiting processes, no running, no ready for more that X times, this is 
	 * a deadlock. How can the waiting processes become ready if nothing is running? 
	 * So if we see this of X times, we stop the waiting ones.
	 * This is the X.
	 */
	public int watchDogCounter = 30;
	
	/**
	 * Flag to activate or diasctivate the watch dog
	 */
	public boolean activateWatchDog = true;
	
	/**
	 * List of configured schedulers
	 */
	public ArrayList<SchedulerQueueConfig> schedulers;
	
	/**
	 * Flag to ask the launcher to redirect the output of the logs/print. If it is true, the system will check launcherOutputPath. If it is 
	 * valorized it will redirect to that path
	 */
	public boolean redirectLauncherOutputs = false;
	
	/**
	 * If redirectLauncherOutputs is true, this represents the path where to redirect the outputs of the launcher. 
	 */
	public String launcherOutputPath = "";
	
	/**
	 * It enables the virtual node code generation. Used mainly for mini-wasdi with mongo to allow parallel execution of different containers. When true, the executeProcess 
	 * method in the server Wasdi.java, will assign the value found in the WASDI_VIRTUAL_NODE_CODE env variable to the process workpsace.
	 * The scheduler will filter in this case using the same varialbe.
	 * Note that the workspace it self will use the real node code, allowing cuncurrent execution of parall apps also in the same workspace if the volume mounted is the same  
	 */
	public boolean useVirtualNodeCode = false;
	
	/**
	 * It enables the fair Round Robin per user: it will round x processes per user in the CREATED list 
	 */
	public boolean defaultFairRoundRobin = false;
	
	/**
	 * Max number of consecutive processess per user, if the fairRoundRobin is activated and there are other users in queue
	 */
	public int defaultFairRoundRobinMaxProcessesCount = 3;

	/**
	 * If true, when defaultFairRoundRobin is active, applies a second round robin by parentId inside each user queue
	 */
	public boolean defaultFairRoundRobinParentId = false;

	/**
	 * Max number of consecutive processess per parentId group inside the selected user queue
	 */
	public int defaultFairRoundRobinParentIdMaxProcessesCount = 3;

	/**
	 * Flag to know if this Process Scheduler applies the Special Wait Condition 
	 * to avoid to trigger too many processes considering also the waiting queue
	 */	
	public boolean defaultSpecialWaitCondition = false;
	
	/**
	 * Max number of waiting processes admitted before breaking the FIFO rules
	 */
	public int defaultMaxWaitingQueue = 100;

	/**
	 * In emergency mode (waiting queue over threshold), if no unblock candidate is found,
	 * fallback to default CREATED candidate every N cycles to avoid prolonged starvation.
	 */
	public int emergencyFallbackEveryNCycles = 5;
	
	
		
	/**
	 * Get a scheduler from the queue code
	 * @param sScheduler Code of the Queue
	 * @return SchedulerQueueConfig
	 */
	public SchedulerQueueConfig getSchedulerQueueConfig(String sScheduler) {
		if (schedulers == null) return null;
		
		for (SchedulerQueueConfig oItem : schedulers) {
			if (oItem.name.equals(sScheduler)) {
				return oItem;
			}
		}
		
		return null;		
	}
}
