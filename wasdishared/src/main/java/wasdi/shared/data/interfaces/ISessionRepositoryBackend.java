package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.users.UserSession;

/**
 * Backend contract for session repository.
 */
public interface ISessionRepositoryBackend {

	boolean insertSession(UserSession oSession);

	UserSession createUniqueSession(String sUserId);

	UserSession insertUniqueSession(String sUserId);

	UserSession getSession(String sSessionId);

	List<UserSession> getAllActiveSessions(String sUserId);

	List<UserSession> getAllExpiredSessions(String sUserId);

	boolean touchSession(UserSession oSession);

	boolean deleteSession(UserSession oSession);

	int deleteSessionsByUserId(String sUserId);

	boolean isNotExpiredSession(UserSession oSession);
}
