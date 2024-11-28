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
