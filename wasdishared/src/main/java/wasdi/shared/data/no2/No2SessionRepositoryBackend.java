package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.users.UserSession;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.interfaces.ISessionRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for session repository.
 */
public class No2SessionRepositoryBackend extends No2Repository implements ISessionRepositoryBackend {

	private static final String s_sCollectionName = "sessions";

	private long getSessionExpireMillis() {
		try {
			
			return WasdiConfig.Current.getSessionExpireHours() * 60L * 60L * 1000L;
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SessionRepositoryBackend.getSessionExpireMillis: error", oEx);
		}

		return 24L * 60L * 60L * 1000L;
	}

	@Override
	public boolean insertSession(UserSession oSession) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oSession == null) {
				return false;
			}

			oCollection.insert(toDocument(oSession));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SessionRepositoryBackend.insertSession: error", oEx);
		}

		return false;
	}

	@Override
	public UserSession createUniqueSession(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return null;
		}

		UserSession oSession = null;
		do {
			oSession = getSession(UUID.randomUUID().toString());
		}
		while (oSession != null);

		UserSession oNewSession = new UserSession();
		oNewSession.setSessionId(UUID.randomUUID().toString());
		oNewSession.setUserId(sUserId);
		oNewSession.setLoginDate(Utils.nowInMillis());
		oNewSession.setLastTouch(Utils.nowInMillis());
		return oNewSession;
	}

	@Override
	public UserSession insertUniqueSession(String sUserId) {
		UserSession oSession = createUniqueSession(sUserId);
		if (oSession != null && insertSession(oSession)) {
			return oSession;
		}
		return null;
	}

	@Override
	public UserSession getSession(String sSessionId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("sessionId").eq(sSessionId))) {
				return fromDocument(oDocument, UserSession.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SessionRepositoryBackend.getSession: error", oEx);
		}

		return null;
	}

	@Override
	public List<UserSession> getAllActiveSessions(String sUserId) {
		List<UserSession> aoReturnList = new ArrayList<>();
		long lNow = System.currentTimeMillis();
		long lLimit = lNow - getSessionExpireMillis();

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find(where("userId").eq(sUserId)) : null;

			for (UserSession oSession : toList(oCursor, UserSession.class)) {
				if (oSession != null && oSession.getLastTouch() != null && oSession.getLastTouch() >= lLimit) {
					aoReturnList.add(oSession);
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SessionRepositoryBackend.getAllActiveSessions: error", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<UserSession> getAllExpiredSessions(String sUserId) {
		List<UserSession> aoReturnList = new ArrayList<>();
		long lNow = System.currentTimeMillis();
		long lLimit = lNow - getSessionExpireMillis();

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find(where("userId").eq(sUserId)) : null;

			for (UserSession oSession : toList(oCursor, UserSession.class)) {
				if (oSession != null && oSession.getLastTouch() != null && oSession.getLastTouch() < lLimit) {
					aoReturnList.add(oSession);
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SessionRepositoryBackend.getAllExpiredSessions: error", oEx);
		}

		return aoReturnList;
	}

	@Override
	public boolean touchSession(UserSession oSession) {
		if (oSession == null || Utils.isNullOrEmpty(oSession.getSessionId())) {
			return false;
		}

		try {
			UserSession oStored = getSession(oSession.getSessionId());
			if (oStored == null) {
				return false;
			}

			oStored.setLastTouch((double) System.currentTimeMillis());
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			oCollection.update(where("sessionId").eq(oStored.getSessionId()), toDocument(oStored));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SessionRepositoryBackend.touchSession: error", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteSession(UserSession oSession) {
		if (oSession == null || Utils.isNullOrEmpty(oSession.getSessionId())) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			oCollection.remove(where("sessionId").eq(oSession.getSessionId()));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SessionRepositoryBackend.deleteSession: error", oEx);
		}

		return false;
	}

	@Override
	public int deleteSessionsByUserId(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			int iCount = 0;
			for (UserSession oSession : getAllActiveSessions(sUserId)) {
				iCount++;
			}
			for (UserSession oSession : getAllExpiredSessions(sUserId)) {
				iCount++;
			}

			NitriteCollection oCollection = getCollection(s_sCollectionName);
			oCollection.remove(where("userId").eq(sUserId));
			return iCount;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2SessionRepositoryBackend.deleteSessionsByUserId: error", oEx);
		}

		return 0;
	}

	@Override
	public boolean isNotExpiredSession(UserSession oSession) {
		if (oSession == null || oSession.getLastTouch() == null) {
			return false;
		}

		long lNow = System.currentTimeMillis();
		long lLimit = lNow - getSessionExpireMillis();

		if (oSession.getLastTouch() >= lLimit) {
			return true;
		}

		deleteSession(oSession);
		return false;
	}
}
