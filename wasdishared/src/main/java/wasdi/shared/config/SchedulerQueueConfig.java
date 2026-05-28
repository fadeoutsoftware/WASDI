package wasdi.shared.config;

/**
 * Single Scheduler Queue configuration
 * 
 * @author p.campanella
 *
 */
public class SchedulerQueueConfig {
	
	/**
	 * Name of the Queue
	 */
	public String name;
	
	/**
	 * Max number of elements in the queue
	 */
	public String maxQueue;
	/**
	 * Default Queue timeout in ms
	 */
	public String timeoutMs;
	
	/**
	 * Comma separated Operation Types supported by this queue
	 */
	public String opTypes;
	
	/**
	 * Operation subtype: to be valorized, OpTypes must contain only one operation
	 * and subtype must be a valid sub type of OpType
	 */
	public String opSubType;
	
	/**
	 * Flag to enable or disable this queue
	 */
	public String enabled;
	
	/**
	 * Flag to know if this Process Scheduler applies the Special Wait Condition 
	 * to avoid to trigger too many processes considering also the waiting queue
	 */	
	public boolean specialWaitCondition = false;
	
	/**
	 * Max number of waiting processes admitted before breaking the FIFO rules
	 */
	public int maxWaitingQueue = 100;
	
	/**
	 * It enables the fair Round Robin per user: it will round x processes per user in the CREATED list 
	 */
	public boolean fairRoundRobin = false;
	
	/**
	 * Max number of consecutive processess per user, if the fairRoundRobin is activated and there are other users in queue
	 */
	public int fairRoundRobinMaxProcessesCount = 3;

	/**
	 * If true, when fairRoundRobin is active, applies a second round robin by parentId inside each user queue
	 */
	public boolean fairRoundRobinParentId = false;

	/**
	 * Max number of consecutive processess per parentId group inside the selected user queue
	 */
	public int fairRoundRobinParentIdMaxProcessesCount = 3;

}
