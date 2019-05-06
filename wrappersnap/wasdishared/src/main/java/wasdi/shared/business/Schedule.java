package wasdi.shared.business;

/**
 * Represent the schedule of a User Processor
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
	 * Cron min param
	 */
	private String minutes;
	
	/**
	 * Cron hour param
	 */
	private String hours;
	
	/**
	 * Cron dom param
	 */
	private String dayOfMonth;
	
	/**
	 * Cron mon param
	 */
	private String month;
	
	/**
	 * Cron dow param
	 */
	private String dayOfWeek;

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
}
