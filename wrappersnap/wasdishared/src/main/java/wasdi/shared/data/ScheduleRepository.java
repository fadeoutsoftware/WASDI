package wasdi.shared.data;

import org.bson.Document;
import org.bson.types.ObjectId;

import wasdi.shared.business.Schedule;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class ScheduleRepository extends MongoRepository {
	
	public ScheduleRepository() {
		m_sThisCollection = "schedule";
	}
	
	/**
	 * Insert a schedule in the db
	 * @param oSchedule Schedule Entity to insert
	 * @return Schedule Id
	 */
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
    
    /**
     * Delete a schedule by Id
     * @param sScheduleId
     * @return
     */
    public boolean deleteScheduleById(String sScheduleId) {
    	
    	if (Utils.isNullOrEmpty(sScheduleId)) return false;

        try {
            getCollection(m_sThisCollection).deleteOne(new Document("scheduleId", new ObjectId(sScheduleId)));

            return true;

        } catch (Exception oEx) {
        	WasdiLog.errorLog("ScheduleRepository.deleteScheduleById: ", oEx);
        }

        return false;
    }

    /**
     * Get Schedule by Id
     * @param sScheduleId Schedule Id
     * @return
     */
    public Schedule getSchedule(String sScheduleId) {
        try {
            Document oSessionDocument = getCollection(m_sThisCollection).find(new Document("scheduleId", sScheduleId)).first();

            if (oSessionDocument==null) return  null;

            String sJSON = oSessionDocument.toJson();

            Schedule oSchedule = s_oMapper.readValue(sJSON,Schedule.class);

            return oSchedule;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ScheduleRepository.getSchedule: ", oEx);
        }

        return  null;
    }
}
