package wasdi.shared.data;

import wasdi.shared.business.Schedule;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IScheduleRepositoryBackend;

public class ScheduleRepository {

    private final IScheduleRepositoryBackend m_oBackend;
	
	public ScheduleRepository() {
        m_oBackend = createBackend();
    }

    private IScheduleRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createScheduleRepository();
	}
	
	/**
	 * Insert a schedule in the db
	 * @param oSchedule Schedule Entity to insert
	 * @return Schedule Id
	 */
    public String insertSchedule(Schedule oSchedule) {
        return m_oBackend.insertSchedule(oSchedule);
    }
    
    /**
     * Delete a schedule by Id
     * @param sScheduleId
     * @return
     */
    public boolean deleteScheduleById(String sScheduleId) {
        return m_oBackend.deleteScheduleById(sScheduleId);
    }

    /**
     * Get Schedule by Id
     * @param sScheduleId Schedule Id
     * @return
     */
    public Schedule getSchedule(String sScheduleId) {
        return m_oBackend.getSchedule(sScheduleId);
    }
    
    /**
     * Delete a schedule by User Id
     * @param sUserId
     * @return
     */
    public int deleteScheduleByUserId(String sUserId) {
        return m_oBackend.deleteScheduleByUserId(sUserId);
    }    
}

