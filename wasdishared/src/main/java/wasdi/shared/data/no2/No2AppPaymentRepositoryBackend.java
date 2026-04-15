package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.AppPayment;
import wasdi.shared.data.interfaces.IAppPaymentRepositoryBackend;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for app payment repository.
 */
public class No2AppPaymentRepositoryBackend extends No2Repository implements IAppPaymentRepositoryBackend {

	private static final String s_sCollectionName = "appspayments";

	@Override
	public boolean insertAppPayment(AppPayment oAppPayment) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oAppPayment == null) {
				return false;
			}

			oCollection.insert(toDocument(oAppPayment));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2AppPaymentRepositoryBackend.insertAppPayment: error", oEx);
		}

		return false;
	}

	@Override
	public AppPayment getAppPaymentById(String sAppPaymentId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("appPaymentId").eq(sAppPaymentId))) {
				return fromDocument(oDocument, AppPayment.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2AppPaymentRepositoryBackend.getAppPaymentById: error", oEx);
		}

		return null;
	}

	@Override
	public List<AppPayment> getAppPaymentByProcessorAndUser(String sProcessorId, String sUserId) {
		List<AppPayment> aoReturnList = new ArrayList<>();

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return aoReturnList;
			}

			DocumentCursor oCursor = oCollection.find(where("userId").eq(sUserId));
			for (AppPayment oAppPayment : toList(oCursor, AppPayment.class)) {
				if (oAppPayment != null && equalsSafe(oAppPayment.getProcessorId(), sProcessorId)) {
					aoReturnList.add(oAppPayment);
				}
			}

			return aoReturnList;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2AppPaymentRepositoryBackend.getAppPaymentByProcessorAndUser: error", oEx);
		}

		return null;
	}

	@Override
	public List<AppPayment> getAppPaymentByNameAndUser(String sName, String sUserId) {
		List<AppPayment> aoReturnList = new ArrayList<>();

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return aoReturnList;
			}

			DocumentCursor oCursor = oCollection.find(where("userId").eq(sUserId));
			for (AppPayment oAppPayment : toList(oCursor, AppPayment.class)) {
				if (oAppPayment != null && equalsSafe(oAppPayment.getName(), sName)) {
					aoReturnList.add(oAppPayment);
				}
			}

			return aoReturnList;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2AppPaymentRepositoryBackend.getAppPaymentByNameAndUser: error", oEx);
		}

		return null;
	}

	@Override
	public boolean updateAppPayment(AppPayment oAppPayment) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oAppPayment == null) {
				return false;
			}

			oCollection.update(where("appPaymentId").eq(oAppPayment.getAppPaymentId()), toDocument(oAppPayment));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2AppPaymentRepositoryBackend.updateAppPayment: error", oEx);
		}

		return false;
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}
}
