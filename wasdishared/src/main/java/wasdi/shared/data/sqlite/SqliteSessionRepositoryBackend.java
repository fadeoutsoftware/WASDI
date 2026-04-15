package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import wasdi.shared.business.users.UserSession;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.interfaces.ISessionRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for session repository.
 */
public class SqliteSessionRepositoryBackend extends SqliteRepository implements ISessionRepositoryBackend {

	public SqliteSessionRepositoryBackend() {
		m_sThisCollection = "sessions";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insertSession(UserSession oSession) {
		try {
			if (oSession == null || Utils.isNullOrEmpty(oSession.getSessionId())) {
				return false;
			}
			return insert(oSession.getSessionId(), oSession);

		} catch (Exception oEx) {
			WasdiLog.errorLog("SessionRepository.insertSession : error ", oEx);
		}

		return false;
	}

	@Override
	public UserSession createUniqueSession(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return null;
		}
		UserSession oSession = null;
		String sSessionId = "";
		do {
			sSessionId = UUID.randomUUID().toString();
			oSession = getSession(sSessionId);
		}
		while (null != oSession);

		oSession = new UserSession();
		oSession.setSessionId(sSessionId);
		oSession.setUserId(sUserId);
		oSession.setLoginDate(Utils.nowInMillis());
		oSession.setLastTouch(Utils.nowInMillis());

		return oSession;
	}

	@Override
	public UserSession insertUniqueSession(String sUserId) {
		try {
			UserSession oSession = createUniqueSession(sUserId);
			if (insertSession(oSession)) {
				return oSession;
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("SessionRepository.insertUniqueSession : error ", oE);
		}
		return null;
	}

	@Override
	public UserSession getSession(String sSessionId) {
		try {
			return findOneWhere("sessionId", sSessionId, UserSession.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("SessionRepository.getSession : error ", oEx);
		}

		return null;
	}

	@Override
	public List<UserSession> getAllActiveSessions(String sUserId) {
		final ArrayList<UserSession> aoReturnList = new ArrayList<>();
		try {
			long lNow = new Date().getTime();
			long lTimespan = WasdiConfig.Current.getSessionExpireHours()  * 60L * 60L * 1000L;
			aoReturnList.addAll(queryList(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.lastTouch') >= ?" +
					" AND json_extract(data,'$.userId') = ?",
					new Object[]{lNow - lTimespan, sUserId}, UserSession.class));
		} catch (Exception oEx) {
			WasdiLog.errorLog("SessionRepository.getAllActiveSessions : error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<UserSession> getAllExpiredSessions(String sUserId) {
		final ArrayList<UserSession> aoReturnList = new ArrayList<>();
		try {
			long lNow = new Date().getTime();
			long lTimespan = WasdiConfig.Current.getSessionExpireHours() * 60L * 60L * 1000L;
			aoReturnList.addAll(queryList(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.lastTouch') < ?" +
					" AND json_extract(data,'$.userId') = ?",
					new Object[]{lNow - lTimespan, sUserId}, UserSession.class));

		} catch (Exception oEx) {
			WasdiLog.errorLog("SessionRepository.getAllExpiredSessions : error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public boolean touchSession(UserSession oSession) {
		try {
			if (oSession == null || Utils.isNullOrEmpty(oSession.getSessionId())) {
				return false;
			}

			UserSession oStoredSession = getSession(oSession.getSessionId());
			if (oStoredSession == null) {
				return false;
			}

			oStoredSession.setLastTouch((double) new Date().getTime());
			return updateById(oStoredSession.getSessionId(), oStoredSession);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SessionRepository.touchSession : error ", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteSession(UserSession oSession) {

		if (oSession == null) {
			return false;
		}

		try {
			if (oSession == null || Utils.isNullOrEmpty(oSession.getSessionId())) {
				return true;
			}
			deleteWhere("sessionId", oSession.getSessionId());
			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("SessionRepository.deleteSession : error ", oEx);
		}

		return false;
	}

	@Override
	public int deleteSessionsByUserId(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			return deleteWhere("userId", sUserId);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SessionRepository.deleteSessionsByUserId: error ", oEx);
			return -1;
		}
	}

	@Override
	public boolean isNotExpiredSession(UserSession oSession) {

		if (oSession == null) {
			return false;
		}

		try {
			long lNow = new Date().getTime();
			long lTimespan = WasdiConfig.Current.getSessionExpireHours() * 60L * 60L * 1000L;
			long lLimit = lNow - lTimespan;

			if (oSession.getLastTouch() >= lLimit) {
				return true;
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SessionRepository.isValidSession : error ", oEx);
		}

		// Not valid, we can delete it!
		deleteSession(oSession);

		return false;
	}
}
