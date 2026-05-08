package wasdi.shared.data.sqlite;

import wasdi.shared.business.Schedule;
import wasdi.shared.data.interfaces.IScheduleRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for schedule repository.
 */
public class SqliteScheduleRepositoryBackend extends SqliteRepository implements IScheduleRepositoryBackend {

	public SqliteScheduleRepositoryBackend() {
		m_sThisCollection = "schedule";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public String insertSchedule(Schedule oSchedule) {
		try {
			if (oSchedule == null || Utils.isNullOrEmpty(oSchedule.getScheduleId())) {
				return "";
			}

			insert(oSchedule.getScheduleId(), oSchedule);
			return oSchedule.getScheduleId();

		} catch (Exception oEx) {
			WasdiLog.errorLog("ScheduleRepository.insertSchedule: ", oEx);
		}

		return "";
	}

	@Override
	public boolean deleteScheduleById(String sScheduleId) {

		if (Utils.isNullOrEmpty(sScheduleId)) {
			return false;
		}

		try {
			// Keep historical behavior: method returns true when operation executes without exception.
			deleteWhere("scheduleId", sScheduleId);

			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("ScheduleRepository.deleteScheduleById: ", oEx);
		}

		return false;
	}

	@Override
	public Schedule getSchedule(String sScheduleId) {
		try {
			return findOneWhere("scheduleId", sScheduleId, Schedule.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ScheduleRepository.getSchedule: ", oEx);
		}

		return null;
	}

	@Override
	public int deleteScheduleByUserId(String sUserId) {

		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			return deleteWhere("userId", sUserId);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ScheduleRepository.deleteScheduleByUserId: error ", oEx);
			return -1;
		}
	}
}
