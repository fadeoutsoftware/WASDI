package wasdi.shared.business;

/**
 * Represent the schedule of a User Processor
 * 
 * Schedules are read by the trigger. 
 * To schedule a processor, a new row in the server crontab must be added.
 * This cron tab task must run the trigger with the schedule ID.
 * 
 * The trigger starts, create a new valid session and triggers the execution of the 
 * requested application.
 * 
 * In this entity there are all the info needed by the trigger to start the processor
 * 
 * @author p.campanella
 *
 */
public class Schedule {
	/**
	 * Schedule Identifier
	 */
	private String scheduleId;
	
	/**
	 * User owner of the schedule
	 */
	private String userId;
	
	/**
	 * Reference Workspace
	 */
	private String workspaceId;
	
	/**
	 * Name of the processor
	 */
	private String processorName;
	
	/**
	 * Associated Params
	 */
	private String params;
	
	/**
	 * Cron min param [Not used yet]
	 */
	private String minutes;
	
	/**
	 * Cron hour param [Not used yet]
	 */
	private String hours;
	
	/**
	 * Cron dom param [Not used yet]
	 */
	private String dayOfMonth;
	
	/**
	 * Cron mon param [Not used yet]
	 */
	private String month;
	
	/**
	 * Cron dow param [Not used yet]
	 */
	private String dayOfWeek;
	
	/**
	 * Flag to define if we want to notify the user by mail or not
	 */	
	private boolean notifyOwnerByMail = false;

	public String getScheduleId() {
		return scheduleId;
	}

	public void setScheduleId(String scheduleId) {
		this.scheduleId = scheduleId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getWorkspaceId() {
		return workspaceId;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public String getProcessorName() {
		return processorName;
	}

	public void setProcessorName(String processorName) {
		this.processorName = processorName;
	}

	public String getMinutes() {
		return minutes;
	}

	public void setMinutes(String minutes) {
		this.minutes = minutes;
	}

	public String getHours() {
		return hours;
	}

	public void setHours(String hours) {
		this.hours = hours;
	}

	public String getDayOfMonth() {
		return dayOfMonth;
	}

	public void setDayOfMonth(String dayOfMonth) {
		this.dayOfMonth = dayOfMonth;
	}

	public String getMonth() {
		return month;
	}

	public void setMonth(String month) {
		this.month = month;
	}

	public String getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(String dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}
	
	public boolean isNotifyOwnerByMail() {
		return notifyOwnerByMail;
	}

	public void setNotifyOwnerByMail(boolean notifyOwnerByMail) {
		this.notifyOwnerByMail = notifyOwnerByMail;
	}	
}
