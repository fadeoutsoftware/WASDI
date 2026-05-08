package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.Schedule;
import wasdi.shared.data.interfaces.IScheduleRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for schedule repository.
 */
public class No2ScheduleRepositoryBackend extends No2Repository implements IScheduleRepositoryBackend {

	private static final String s_sCollectionName = "schedule";

	@Override
	public String insertSchedule(Schedule oSchedule) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oSchedule == null) {
				return "";
			}

			oCollection.insert(toDocument(oSchedule));
			return oSchedule.getScheduleId();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ScheduleRepositoryBackend.insertSchedule", oEx);
		}

		return "";
	}

	@Override
	public boolean deleteScheduleById(String sScheduleId) {
		if (Utils.isNullOrEmpty(sScheduleId)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.remove(where("scheduleId").eq(sScheduleId));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ScheduleRepositoryBackend.deleteScheduleById", oEx);
		}

		return false;
	}

	@Override
	public Schedule getSchedule(String sScheduleId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("scheduleId").eq(sScheduleId))) {
				return fromDocument(oDocument, Schedule.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ScheduleRepositoryBackend.getSchedule", oEx);
		}

		return null;
	}

	@Override
	public int deleteScheduleByUserId(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return -1;
			}

			int iCount = 0;
			for (Document oDocument : oCollection.find(where("userId").eq(sUserId))) {
				if (oDocument != null) {
					iCount++;
				}
			}

			oCollection.remove(where("userId").eq(sUserId));
			return iCount;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ScheduleRepositoryBackend.deleteScheduleByUserId", oEx);
			return -1;
		}
	}
}
