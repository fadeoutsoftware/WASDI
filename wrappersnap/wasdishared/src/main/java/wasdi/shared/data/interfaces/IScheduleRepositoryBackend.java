package wasdi.shared.data.interfaces;

import wasdi.shared.business.Schedule;

/**
 * Backend contract for schedule repository.
 */
public interface IScheduleRepositoryBackend {

	String insertSchedule(Schedule oSchedule);

	boolean deleteScheduleById(String sScheduleId);

	Schedule getSchedule(String sScheduleId);

	int deleteScheduleByUserId(String sUserId);
}
