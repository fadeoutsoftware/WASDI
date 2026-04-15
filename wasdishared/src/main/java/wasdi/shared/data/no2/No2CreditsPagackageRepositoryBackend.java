package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.CreditsPackage;
import wasdi.shared.data.interfaces.ICreditsPagackageRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for credits package repository.
 */
public class No2CreditsPagackageRepositoryBackend extends No2Repository implements ICreditsPagackageRepositoryBackend {

	private static final String s_sCollectionName = "creditspackages";

	@Override
	public CreditsPackage getCreditPackageById(String sCreditPackageId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("creditPackageId").eq(sCreditPackageId))) {
				return fromDocument(oDocument, CreditsPackage.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CreditsPagackageRepositoryBackend.getCreditPackageById: error", oEx);
		}

		return null;
	}

	@Override
	public CreditsPackage getCreditPackageByNameAndUserId(String sCreditPackageName, String sUserId) {
		try {
			for (CreditsPackage oCreditsPackage : listByUser(sUserId, true)) {
				if (oCreditsPackage != null && equalsSafe(oCreditsPackage.getName(), sCreditPackageName)) {
					return oCreditsPackage;
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CreditsPagackageRepositoryBackend.getCreditPackageByNameAndUserId: error", oEx);
		}

		return null;
	}

	@Override
	public Double getTotalCreditsByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			WasdiLog.warnLog("No2CreditsPagackageRepositoryBackend.getTotalCreditsByUser: user id is null or empty");
			return null;
		}

		double dCredits = 0.0;
		try {
			for (CreditsPackage oCreditsPackage : listByUser(sUserId, true)) {
				if (oCreditsPackage != null && oCreditsPackage.isBuySuccess() && oCreditsPackage.getCreditsRemaining() != null) {
					dCredits += oCreditsPackage.getCreditsRemaining();
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CreditsPagackageRepositoryBackend.getTotalCreditsByUser: error", oEx);
		}

		return dCredits;
	}

	@Override
	public List<CreditsPackage> listByUser(String sUserId, boolean bAscendingOrder) {
		if (Utils.isNullOrEmpty(sUserId)) {
			WasdiLog.warnLog("No2CreditsPagackageRepositoryBackend.listByUser: user id is null or empty");
			return null;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return new ArrayList<>();
			}

			DocumentCursor oCursor = oCollection.find(where("userId").eq(sUserId));
			List<CreditsPackage> aoReturnList = toList(oCursor, CreditsPackage.class);
			Comparator<CreditsPackage> oComparator = Comparator.comparing(
					CreditsPackage::getBuyDate,
					Comparator.nullsLast(Double::compareTo));
			aoReturnList.sort(bAscendingOrder ? oComparator : oComparator.reversed());
			return aoReturnList;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CreditsPagackageRepositoryBackend.listByUser: error", oEx);
		}

		return null;
	}

	@Override
	public boolean insertCreditPackage(CreditsPackage oCreditPackage) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oCreditPackage == null) {
				return false;
			}

			oCollection.insert(toDocument(oCreditPackage));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CreditsPagackageRepositoryBackend.insertCreditPackage: error", oEx);
		}

		return false;
	}

	@Override
	public boolean updateCreditPackage(CreditsPackage oCreditPackage) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oCreditPackage == null) {
				return false;
			}

			oCollection.update(where("creditPackageId").eq(oCreditPackage.getCreditPackageId()), toDocument(oCreditPackage));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CreditsPagackageRepositoryBackend.updateCreditPackage: error", oEx);
		}

		return false;
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}
}
