package wasdi.shared.config;

import java.util.ArrayList;

public class SchedulerConfig {
	public String ProcessingThreadWaitStartMS;
	public String ProcessingThreadSleepingTimeMS;
	public String LauncherPath;
	public String JavaExe;
	public String KillCommand;
	
	public String MaxQueue;
	public String TimeoutMs;	
	
	public ArrayList<SchedulerQueueConfig> schedulers;
}
