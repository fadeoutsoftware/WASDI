package wasdi.shared.data;

import org.bson.Document;
import org.bson.types.ObjectId;

import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProcessorLog;
import wasdi.shared.business.Schedule;

public class ScheduleRepository extends MongoRepository {
	
	/**
	 * Insert a schedule in the db
	 * @param oSchedule Schedule Entity to insert
	 * @return Schedule Id
	 */
    public String InsertSchedule(Schedule oSchedule) {

        try {
        	
            String sJSON = s_oMapper.writeValueAsString(oSchedule);
            Document oDocument = Document.parse(sJSON);
            
            getCollection("schedule").insertOne(oDocument);
            return oSchedule.getScheduleId();

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return "";
    }
    
    /**
     * Delete a schedule by Id
     * @param sScheduleId
     * @return
     */
    public boolean DeleteScheduleById(String sScheduleId) {

        try {
            getCollection("schedule").deleteOne(new Document("scheduleId", new ObjectId(sScheduleId)));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    /**
     * Get Schedule by Id
     * @param sScheduleId Schedule Id
     * @return
     */
    public Schedule GetSchedule(String sScheduleId) {
        try {
            Document oSessionDocument = getCollection("schedule").find(new Document("scheduleId", sScheduleId)).first();

            if (oSessionDocument==null) return  null;

            String sJSON = oSessionDocument.toJson();

            Schedule oSchedule = s_oMapper.readValue(sJSON,Schedule.class);

            return oSchedule;
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }
}
