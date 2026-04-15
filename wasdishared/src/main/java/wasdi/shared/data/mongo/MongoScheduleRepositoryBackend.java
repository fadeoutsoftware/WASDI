package wasdi.shared.data.mongo;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;

import wasdi.shared.business.Schedule;
import wasdi.shared.data.interfaces.IScheduleRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for schedule repository.
 */
public class MongoScheduleRepositoryBackend extends MongoRepository implements IScheduleRepositoryBackend {

	public MongoScheduleRepositoryBackend() {
		m_sThisCollection = "schedule";
	}

	@Override
	public String insertSchedule(Schedule oSchedule) {

		try {

			String sJSON = s_oMapper.writeValueAsString(oSchedule);
			Document oDocument = Document.parse(sJSON);

			getCollection(m_sThisCollection).insertOne(oDocument);
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
			getCollection(m_sThisCollection).deleteOne(new Document("scheduleId", new ObjectId(sScheduleId)));

			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("ScheduleRepository.deleteScheduleById: ", oEx);
		}

		return false;
	}

	@Override
	public Schedule getSchedule(String sScheduleId) {
		try {
			Document oSessionDocument = getCollection(m_sThisCollection).find(new Document("scheduleId", sScheduleId)).first();

			if (oSessionDocument == null) {
				return null;
			}

			String sJSON = oSessionDocument.toJson();

			Schedule oSchedule = s_oMapper.readValue(sJSON, Schedule.class);

			return oSchedule;
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
			BasicDBObject oCriteria = new BasicDBObject();
			oCriteria.append("userId", sUserId);

			return deleteMany(oCriteria);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ScheduleRepository.deleteScheduleByUserId: error ", oEx);
			return -1;
		}
	}
}
