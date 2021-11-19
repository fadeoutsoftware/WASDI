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
