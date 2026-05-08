package wasdi.shared.business;

/**
 * Process Status Enum
 * 
 * Represents the status of each process running in WASDI.
 * 
 * CREATED: Process inserted in the db, waiting in a queue to be scheduled.
 * RUNNING: The process is working
 * WAITING: The process is wating for another operation to finish
 * READY:  	The process has finished to wait for the other process to finish and now is waiting for the scheduler to put it again in runnig
 * ERROR: 	Process finished in error
 * DONE:	Process finished with success
 * STOPPED:	Process stopped by the user or by a timeout 	
 * 
 * @author p.campanella
 *
 */
public enum ProcessStatus {
	CREATED,
	RUNNING,
	STOPPED,
	DONE,
	ERROR,
	WAITING,
	READY
}
