package wasdi.shared.data.sqlite;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.CreditsPackage;
import wasdi.shared.data.interfaces.ICreditsPagackageRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for credits package repository.
 */
public class SqliteCreditsPagackageRepositoryBackend extends SqliteRepository implements ICreditsPagackageRepositoryBackend {

	public SqliteCreditsPagackageRepositoryBackend() {
		m_sThisCollection = "creditspackages";
		ensureTable(m_sThisCollection);
	}

	@Override
	public CreditsPackage getCreditPackageById(String sCreditPackageId) {
		try {
			return findOneWhere(m_sThisCollection, "creditPackageId", sCreditPackageId, CreditsPackage.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCreditsPagackageRepositoryBackend.getCreditPackageById: error", oEx);
		}

		return null;
	}

	@Override
	public CreditsPackage getCreditPackageByNameAndUserId(String sCreditPackageName, String sUserId) {
		try {
			Map<String, Object> aoFilter = new LinkedHashMap<>();
			aoFilter.put("userId", sUserId);
			aoFilter.put("name", sCreditPackageName);
			return findOneWhere(m_sThisCollection, aoFilter, CreditsPackage.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCreditsPagackageRepositoryBackend.getCreditPackageByNameAndUserId: error", oEx);
		}

		return null;
	}

	@Override
	public Double getTotalCreditsByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			WasdiLog.warnLog("SqliteCreditsPagackageRepositoryBackend.getTotalCreditsByUser: user id is null or empty");
			return null;
		}

		try {
			String sSql = "SELECT COALESCE(SUM(json_extract(data, '$.creditsRemaining')), 0.0)"
					+ " FROM " + m_sThisCollection
					+ " WHERE json_extract(data, '$.userId') = ?"
					+ " AND json_extract(data, '$.buySuccess') = 1";

			try (java.sql.PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
				oPs.setString(1, sUserId);
				try (java.sql.ResultSet oRs = oPs.executeQuery()) {
					if (oRs.next()) {
						return oRs.getDouble(1);
					}
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCreditsPagackageRepositoryBackend.getTotalCreditsByUser: error", oEx);
		}

		return 0.0;
	}

	@Override
	public List<CreditsPackage> listByUser(String sUserId, boolean bAscendingOrder) {
		if (Utils.isNullOrEmpty(sUserId)) {
			WasdiLog.warnLog("SqliteCreditsPagackageRepositoryBackend.listByUser: user id is null or empty");
			return null;
		}

		try {
			String sOrder = bAscendingOrder ? "ASC" : "DESC";
			String sSql = "SELECT data FROM " + m_sThisCollection
					+ " WHERE json_extract(data, '$.userId') = ?"
					+ " ORDER BY json_extract(data, '$.buyDate') " + sOrder;
			return queryList(sSql, Arrays.asList(sUserId), CreditsPackage.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCreditsPagackageRepositoryBackend.listByUser: error", oEx);
		}

		return null;
	}

	@Override
	public boolean insertCreditPackage(CreditsPackage oCreditPackage) {
		if (oCreditPackage == null) {
			return false;
		}

		try {
			return insert(m_sThisCollection, oCreditPackage.getCreditPackageId(), oCreditPackage);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCreditsPagackageRepositoryBackend.insertCreditPackage: error", oEx);
		}

		return false;
	}

	@Override
	public boolean updateCreditPackage(CreditsPackage oCreditPackage) {
		if (oCreditPackage == null) {
			return false;
		}

		try {
			return updateWhere(m_sThisCollection, "creditPackageId", oCreditPackage.getCreditPackageId(), oCreditPackage);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCreditsPagackageRepositoryBackend.updateCreditPackage: error", oEx);
		}

		return false;
	}
}
