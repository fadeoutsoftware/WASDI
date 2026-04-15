package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.users.UserSession;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.ISessionRepositoryBackend;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class SessionRepository {

    private final ISessionRepositoryBackend m_oBackend;
	
	public SessionRepository() {
        m_oBackend = createBackend();
    }

    private ISessionRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createSessionRepository();
	}
	
	/**
	 * Create a new session
	 * @param oSession a valid WASDI session
	 * @return true if insert was successful, false otherwise
	 */
    public boolean insertSession(UserSession oSession) {
		return m_oBackend.insertSession(oSession);
    }
    
    
    
    /**
     * Creates a new unique session for the given user
     * @param sUserId a valid wasdi user id
     * @return a UserSession if it could be created successfully, null otherwise
     */
    public UserSession createUniqueSession(String sUserId) {
        return m_oBackend.createUniqueSession(sUserId);
    }
    
    
    /**
     * Creates a new session for the given user, and inserts it in the DB
     * @param sUserId a valid WASDI user
     * @return true if the creation and insert succeded, false otherwise
     */
    public UserSession insertUniqueSession(String sUserId) {
        return m_oBackend.insertUniqueSession(sUserId);
    }
    
    
    /**
     * Get a session by Id
     * @param sSessionId
     * @return
     */
    public UserSession getSession(String sSessionId) {
		return m_oBackend.getSession(sSessionId);
    }
    
    /**
     * Get all the active sessions of a user
     * @param sUserId
     * @return
     */
    public List<UserSession> getAllActiveSessions(String sUserId) {
        return m_oBackend.getAllActiveSessions(sUserId);
    }
    
    /**
     * Get all the expired sessions of a user
     * @param sUserId
     * @return
     */
    public List<UserSession> getAllExpiredSessions(String sUserId) {
        return m_oBackend.getAllExpiredSessions(sUserId);
    }
    
    /**
     * Refresh a session
     * @param oSession
     * @return
     */
    public boolean touchSession(UserSession oSession) {
		return m_oBackend.touchSession(oSession);
    }
    
    /**
     * Delete a Session
     * @param oSession
     * @return
     */
    public boolean deleteSession(UserSession oSession) {
		return m_oBackend.deleteSession(oSession);
    }
    
    /**
     * Delete a Session
     * @param oSession
     * @return
     */
    public int deleteSessionsByUserId(String sUserId) {
        return m_oBackend.deleteSessionsByUserId(sUserId);
    }  
    
    
    /**
     * Delete a Session
     * @param oSession
     * @return
     */
    public boolean isNotExpiredSession(UserSession oSession) {
		return m_oBackend.isNotExpiredSession(oSession);
    }    
}

