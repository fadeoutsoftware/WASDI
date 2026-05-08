package wasdi.shared.data.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.utils.log.WasdiLog;

/**
 * Base helper for SQLite repository implementations.
 *
 * Each collection maps to a single table with the schema:
 *
 *   CREATE TABLE &lt;name&gt; (
 *       id   TEXT PRIMARY KEY,
 *       data TEXT NOT NULL        -- JSON-serialized entity
 *   );
 *
 * All query predicates use SQLite's json_extract(data, '$.fieldName') so the
 * original entity fields are queryable without any schema changes.
 *
 * For complex queries that don't fit the standard helpers (LIKE, OR, aggregates,
 * pagination, multi-table joins) use queryList / queryOne / execute with raw SQL.
 *
 * Thread/process safety: SqliteConnection opens the file in WAL journal mode with
 * busy_timeout=5000 ms, so multiple JVM processes can safely share the same file.
 */
public class SqliteRepository {

	protected static final ObjectMapper s_oMapper = new ObjectMapper();
	protected String m_sThisCollection;

	static {
		s_oMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	// ------------------------------------------------------------------
	// Connection
	// ------------------------------------------------------------------

	protected Connection getConnection() {
		return SqliteConnection.getConnection();
	}

	/**
	 * No-op — SQLite uses a single file; repo-db switching is not applicable.
	 * Kept for interface compatibility with backends that implement setRepoDb.
	 */
	public void setRepoDb(String sRepoDb) {
		// intentionally empty
	}

	// ------------------------------------------------------------------
	// Schema helpers
	// ------------------------------------------------------------------

	/**
	 * Creates the table for this collection if it does not already exist.
	 * Call this from each backend's constructor.
	 *
	 * @param sTableName the collection / table name
	 */
	protected void ensureTable(String sTableName) {
		String sSql = "CREATE TABLE IF NOT EXISTS " + sTableName
				+ " (id TEXT PRIMARY KEY, data TEXT NOT NULL)";
		try (Statement oStmt = getConnection().createStatement()) {
			oStmt.execute(sSql);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.ensureTable [" + sTableName + "]: ", oEx);
		}
	}

	// ------------------------------------------------------------------
	// Write helpers
	// ------------------------------------------------------------------

	/**
	 * Inserts a new row into {@link #m_sThisCollection}. Silently ignores a duplicate-key conflict.
	 */
	protected boolean insert(String sId, Object oEntity) {
		return insert(m_sThisCollection, sId, oEntity);
	}

	/**
	 * Inserts or replaces a row in {@link #m_sThisCollection}.
	 */
	protected boolean upsert(String sId, Object oEntity) {
		return upsert(m_sThisCollection, sId, oEntity);
	}

	/**
	 * Updates a row in {@link #m_sThisCollection} by its primary-key id.
	 */
	protected boolean updateById(String sId, Object oEntity) {
		return updateById(m_sThisCollection, sId, oEntity);
	}

	/**
	 * Updates rows in {@link #m_sThisCollection} where a single JSON field equals the given value.
	 */
	protected boolean updateWhere(String sJsonField, Object oFieldValue, Object oEntity) {
		return updateWhere(m_sThisCollection, sJsonField, oFieldValue, oEntity);
	}

	/**
	 * Updates rows in {@link #m_sThisCollection} matching all conditions in the map.
	 */
	protected boolean updateWhere(Map<String, Object> aoConditions, Object oEntity) {
		return updateWhere(m_sThisCollection, aoConditions, oEntity);
	}

	/**
	 * Deletes a row from {@link #m_sThisCollection} by its primary-key id.
	 * Returns 1 if deleted, 0 if not found, -1 on error.
	 */
	protected int deleteById(String sId) {
		return deleteById(m_sThisCollection, sId) ? 1 : 0;
	}

	/**
	 * Deletes rows from {@link #m_sThisCollection} where a single JSON field equals the given value.
	 */
	protected int deleteWhere(String sJsonField, Object oFieldValue) {
		return deleteWhere(m_sThisCollection, sJsonField, oFieldValue);
	}

	/**
	 * Deletes rows from {@link #m_sThisCollection} matching all conditions in the map.
	 */
	protected int deleteWhere(Map<String, Object> aoConditions) {
		return deleteWhere(m_sThisCollection, aoConditions);
	}

	/**
	 * Finds an entity in {@link #m_sThisCollection} by its primary-key id.
	 */
	protected <T> T findById(String sId, Class<T> oClass) {
		return findById(m_sThisCollection, sId, oClass);
	}

	/**
	 * Finds the first row in {@link #m_sThisCollection} where a JSON field equals the given value.
	 */
	protected <T> T findOneWhere(String sJsonField, Object oFieldValue, Class<T> oClass) {
		return findOneWhere(m_sThisCollection, sJsonField, oFieldValue, oClass);
	}

	/**
	 * Finds the first row in {@link #m_sThisCollection} matching all conditions in the map.
	 */
	protected <T> T findOneWhere(Map<String, Object> aoConditions, Class<T> oClass) {
		return findOneWhere(m_sThisCollection, aoConditions, oClass);
	}

	/**
	 * Finds all rows in {@link #m_sThisCollection} where a JSON field equals the given value.
	 */
	protected <T> List<T> findAllWhere(String sJsonField, Object oFieldValue, Class<T> oClass) {
		return findAllWhere(m_sThisCollection, sJsonField, oFieldValue, oClass);
	}

	/**
	 * Finds all rows in {@link #m_sThisCollection} matching all conditions in the map.
	 */
	protected <T> List<T> findAllWhere(Map<String, Object> aoConditions, Class<T> oClass) {
		return findAllWhere(m_sThisCollection, aoConditions, oClass);
	}

	/**
	 * Returns all rows of {@link #m_sThisCollection}.
	 */
	protected <T> List<T> findAll(Class<T> oClass) {
		return findAll(m_sThisCollection, oClass);
	}

	/**
	 * Returns the total number of rows in {@link #m_sThisCollection}.
	 */
	protected long count() {
		return count(m_sThisCollection);
	}

	/**
	 * Returns the number of rows in {@link #m_sThisCollection} where a JSON field equals the given value.
	 */
	protected long countWhere(String sJsonField, Object oFieldValue) {
		return countWhere(m_sThisCollection, sJsonField, oFieldValue);
	}

	/**
	 * Returns the number of rows in {@link #m_sThisCollection} matching all conditions in the map.
	 */
	protected long countWhere(Map<String, Object> aoConditions) {
		return countWhere(m_sThisCollection, aoConditions);
	}

	// ------------------------------------------------------------------
	// Write helpers (full table name required)
	// ------------------------------------------------------------------

	/**
	 * Inserts a new row. Silently ignores a duplicate-key conflict (INSERT OR IGNORE).
	 *
	 * @return true if the row was inserted
	 */
	protected boolean insert(String sTableName, String sId, Object oEntity) {
		String sSql = "INSERT OR IGNORE INTO " + sTableName + " (id, data) VALUES (?, ?)";
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
			oPs.setString(1, sId);
			oPs.setString(2, toJson(oEntity));
			return oPs.executeUpdate() > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.insert [" + sTableName + "]: ", oEx);
		}
		return false;
	}

	/**
	 * Inserts or replaces a row (upsert semantics — INSERT OR REPLACE).
	 *
	 * @return true if the row was written
	 */
	protected boolean upsert(String sTableName, String sId, Object oEntity) {
		String sSql = "INSERT OR REPLACE INTO " + sTableName + " (id, data) VALUES (?, ?)";
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
			oPs.setString(1, sId);
			oPs.setString(2, toJson(oEntity));
			return oPs.executeUpdate() > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.upsert [" + sTableName + "]: ", oEx);
		}
		return false;
	}

	/**
	 * Updates the JSON data of an existing row by its primary-key id.
	 *
	 * @return true if a row was found and updated
	 */
	protected boolean updateById(String sTableName, String sId, Object oEntity) {
		String sSql = "UPDATE " + sTableName + " SET data = ? WHERE id = ?";
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
			oPs.setString(1, toJson(oEntity));
			oPs.setString(2, sId);
			return oPs.executeUpdate() > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.updateById [" + sTableName + "]: ", oEx);
		}
		return false;
	}

	/**
	 * Updates all rows where a single JSON field equals the given value.
	 *
	 * @return true if at least one row was updated
	 */
	protected boolean updateWhere(String sTableName, String sJsonField, Object oFieldValue, Object oEntity) {
		String sSql = "UPDATE " + sTableName + " SET data = ? WHERE json_extract(data, ?) = ?";
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
			oPs.setString(1, toJson(oEntity));
			oPs.setString(2, jp(sJsonField));
			setParam(oPs, 3, oFieldValue);
			return oPs.executeUpdate() > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.updateWhere [" + sTableName + "]: ", oEx);
		}
		return false;
	}

	/**
	 * Updates all rows matching all conditions in the map (AND).
	 *
	 * @return true if at least one row was updated
	 */
	protected boolean updateWhere(String sTableName, Map<String, Object> aoConditions, Object oEntity) {
		if (aoConditions == null || aoConditions.isEmpty()) {
			return false;
		}
		StringBuilder oSb = new StringBuilder("UPDATE ").append(sTableName).append(" SET data = ? WHERE ");
		buildWhereClause(oSb, aoConditions);
		try (PreparedStatement oPs = getConnection().prepareStatement(oSb.toString())) {
			oPs.setString(1, toJson(oEntity));
			bindWhereParams(oPs, aoConditions, 2);
			return oPs.executeUpdate() > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.updateWhere(map) [" + sTableName + "]: ", oEx);
		}
		return false;
	}

	/**
	 * Deletes a row by its primary-key id.
	 *
	 * @return true if the row existed and was deleted
	 */
	protected boolean deleteById(String sTableName, String sId) {
		String sSql = "DELETE FROM " + sTableName + " WHERE id = ?";
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
			oPs.setString(1, sId);
			return oPs.executeUpdate() > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.deleteById [" + sTableName + "]: ", oEx);
		}
		return false;
	}

	/**
	 * Deletes all rows where a single JSON field equals the given value.
	 *
	 * @return number of rows deleted, or -1 on error
	 */
	protected int deleteWhere(String sTableName, String sJsonField, Object oFieldValue) {
		String sSql = "DELETE FROM " + sTableName + " WHERE json_extract(data, ?) = ?";
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
			oPs.setString(1, jp(sJsonField));
			setParam(oPs, 2, oFieldValue);
			return oPs.executeUpdate();
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.deleteWhere [" + sTableName + "]: ", oEx);
		}
		return -1;
	}

	/**
	 * Deletes all rows matching all conditions in the map (AND).
	 *
	 * @return number of rows deleted, or -1 on error
	 */
	protected int deleteWhere(String sTableName, Map<String, Object> aoConditions) {
		if (aoConditions == null || aoConditions.isEmpty()) {
			return 0;
		}
		StringBuilder oSb = new StringBuilder("DELETE FROM ").append(sTableName).append(" WHERE ");
		buildWhereClause(oSb, aoConditions);
		try (PreparedStatement oPs = getConnection().prepareStatement(oSb.toString())) {
			bindWhereParams(oPs, aoConditions, 1);
			return oPs.executeUpdate();
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.deleteWhere(map) [" + sTableName + "]: ", oEx);
		}
		return -1;
	}

	// ------------------------------------------------------------------
	// Read helpers — single field equality
	// ------------------------------------------------------------------

	/**
	 * Finds an entity by its primary-key id.
	 */
	protected <T> T findById(String sTableName, String sId, Class<T> oClass) {
		String sSql = "SELECT data FROM " + sTableName + " WHERE id = ?";
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
			oPs.setString(1, sId);
			try (ResultSet oRs = oPs.executeQuery()) {
				if (oRs.next()) {
					return fromJson(oRs.getString(1), oClass);
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.findById [" + sTableName + "]: ", oEx);
		}
		return null;
	}

	/**
	 * Finds the first row where a JSON field equals the given value.
	 */
	protected <T> T findOneWhere(String sTableName, String sJsonField, Object oFieldValue, Class<T> oClass) {
		String sSql = "SELECT data FROM " + sTableName + " WHERE json_extract(data, ?) = ? LIMIT 1";
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
			oPs.setString(1, jp(sJsonField));
			setParam(oPs, 2, oFieldValue);
			try (ResultSet oRs = oPs.executeQuery()) {
				if (oRs.next()) {
					return fromJson(oRs.getString(1), oClass);
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.findOneWhere [" + sTableName + "]: ", oEx);
		}
		return null;
	}

	/**
	 * Finds all rows where a JSON field equals the given value.
	 */
	protected <T> List<T> findAllWhere(String sTableName, String sJsonField, Object oFieldValue, Class<T> oClass) {
		String sSql = "SELECT data FROM " + sTableName + " WHERE json_extract(data, ?) = ?";
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
			oPs.setString(1, jp(sJsonField));
			setParam(oPs, 2, oFieldValue);
			return collectResults(oPs, oClass);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.findAllWhere [" + sTableName + "]: ", oEx);
		}
		return new ArrayList<>();
	}

	// ------------------------------------------------------------------
	// Read helpers — multi-field (AND) conditions
	// ------------------------------------------------------------------

	/**
	 * Finds all rows matching all conditions in the map (AND).
	 * Map key = JSON field name (without "$." prefix), value = expected value.
	 */
	protected <T> List<T> findAllWhere(String sTableName, Map<String, Object> aoConditions, Class<T> oClass) {
		if (aoConditions == null || aoConditions.isEmpty()) {
			return findAll(sTableName, oClass);
		}
		StringBuilder oSb = new StringBuilder("SELECT data FROM ").append(sTableName).append(" WHERE ");
		buildWhereClause(oSb, aoConditions);
		try (PreparedStatement oPs = getConnection().prepareStatement(oSb.toString())) {
			bindWhereParams(oPs, aoConditions, 1);
			return collectResults(oPs, oClass);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.findAllWhere(map) [" + sTableName + "]: ", oEx);
		}
		return new ArrayList<>();
	}

	/**
	 * Finds the first row matching all conditions in the map (AND).
	 */
	protected <T> T findOneWhere(String sTableName, Map<String, Object> aoConditions, Class<T> oClass) {
		if (aoConditions == null || aoConditions.isEmpty()) {
			return null;
		}
		StringBuilder oSb = new StringBuilder("SELECT data FROM ").append(sTableName).append(" WHERE ");
		buildWhereClause(oSb, aoConditions);
		oSb.append(" LIMIT 1");
		try (PreparedStatement oPs = getConnection().prepareStatement(oSb.toString())) {
			bindWhereParams(oPs, aoConditions, 1);
			try (ResultSet oRs = oPs.executeQuery()) {
				if (oRs.next()) {
					return fromJson(oRs.getString(1), oClass);
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.findOneWhere(map) [" + sTableName + "]: ", oEx);
		}
		return null;
	}

	// ------------------------------------------------------------------
	// Read helpers — full table scan
	// ------------------------------------------------------------------

	/**
	 * Returns all rows in the table.
	 */
	protected <T> List<T> findAll(String sTableName, Class<T> oClass) {
		String sSql = "SELECT data FROM " + sTableName;
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
			return collectResults(oPs, oClass);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.findAll [" + sTableName + "]: ", oEx);
		}
		return new ArrayList<>();
	}

	// ------------------------------------------------------------------
	// Count helpers
	// ------------------------------------------------------------------

	/** Returns the total number of rows in the table. */
	protected long count(String sTableName) {
		String sSql = "SELECT COUNT(*) FROM " + sTableName;
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql);
				ResultSet oRs = oPs.executeQuery()) {
			if (oRs.next()) {
				return oRs.getLong(1);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.count [" + sTableName + "]: ", oEx);
		}
		return 0L;
	}

	/** Returns the number of rows where a JSON field equals the given value. */
	protected long countWhere(String sTableName, String sJsonField, Object oFieldValue) {
		String sSql = "SELECT COUNT(*) FROM " + sTableName + " WHERE json_extract(data, ?) = ?";
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
			oPs.setString(1, jp(sJsonField));
			setParam(oPs, 2, oFieldValue);
			try (ResultSet oRs = oPs.executeQuery()) {
				if (oRs.next()) {
					return oRs.getLong(1);
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.countWhere [" + sTableName + "]: ", oEx);
		}
		return 0L;
	}

	/** Returns the number of rows matching all conditions in the map (AND). */
	protected long countWhere(String sTableName, Map<String, Object> aoConditions) {
		if (aoConditions == null || aoConditions.isEmpty()) {
			return count(sTableName);
		}
		StringBuilder oSb = new StringBuilder("SELECT COUNT(*) FROM ").append(sTableName).append(" WHERE ");
		buildWhereClause(oSb, aoConditions);
		try (PreparedStatement oPs = getConnection().prepareStatement(oSb.toString())) {
			bindWhereParams(oPs, aoConditions, 1);
			try (ResultSet oRs = oPs.executeQuery()) {
				if (oRs.next()) {
					return oRs.getLong(1);
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.countWhere(map) [" + sTableName + "]: ", oEx);
		}
		return 0L;
	}

	// ------------------------------------------------------------------
	// Raw SQL helpers (complex queries: LIKE, OR, aggregates, pagination)
	// ------------------------------------------------------------------

	/**
	 * Executes a raw SELECT and maps each result row's first column (a JSON string)
	 * to an entity of the given class.
	 * Convenience overload accepting an {@code Object[]} parameter array.
	 */
	protected <T> List<T> queryList(String sSql, Object[] aoParams, Class<T> oClass) {
		return queryList(sSql, aoParams == null ? null : java.util.Arrays.asList(aoParams), oClass);
	}

	/**
	 * Executes a raw SELECT and maps each result row's first column (a JSON string)
	 * to an entity of the given class.
	 *
	 * @param sSql    complete SQL string with {@code ?} placeholders
	 * @param aoParams ordered parameter values (may be null or empty)
	 */
	protected <T> List<T> queryList(String sSql, List<Object> aoParams, Class<T> oClass) {
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
			bindList(oPs, aoParams);
			return collectResults(oPs, oClass);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.queryList: ", oEx);
		}
		return new ArrayList<>();
	}

	/**
	 * Executes a raw SELECT and returns the first row mapped to an entity.
	 * Convenience overload accepting an {@code Object[]} parameter array.
	 */
	protected <T> T queryOne(String sSql, Object[] aoParams, Class<T> oClass) {
		return queryOne(sSql, aoParams == null ? null : java.util.Arrays.asList(aoParams), oClass);
	}

	/**
	 * Executes a raw SELECT and returns the first row mapped to an entity.
	 *
	 * @param sSql    complete SQL string with {@code ?} placeholders
	 * @param aoParams ordered parameter values (may be null or empty)
	 */
	protected <T> T queryOne(String sSql, List<Object> aoParams, Class<T> oClass) {
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
			bindList(oPs, aoParams);
			try (ResultSet oRs = oPs.executeQuery()) {
				if (oRs.next()) {
					return fromJson(oRs.getString(1), oClass);
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.queryOne: ", oEx);
		}
		return null;
	}

	/**
	 * Executes a raw INSERT / UPDATE / DELETE and returns the affected row count.
	 * Convenience overload accepting an {@code Object[]} parameter array.
	 */
	protected int execute(String sSql, Object[] aoParams) {
		return execute(sSql, aoParams == null ? null : java.util.Arrays.asList(aoParams));
	}

	/**
	 * Executes a raw INSERT / UPDATE / DELETE and returns the affected row count.
	 *
	 * @param sSql    complete SQL string with {@code ?} placeholders
	 * @param aoParams ordered parameter values (may be null or empty)
	 * @return affected row count, or -1 on error
	 */
	protected int execute(String sSql, List<Object> aoParams) {
		try (PreparedStatement oPs = getConnection().prepareStatement(sSql)) {
			bindList(oPs, aoParams);
			return oPs.executeUpdate();
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.execute: ", oEx);
		}
		return -1;
	}

	// ------------------------------------------------------------------
	// JSON / serialization helpers
	// ------------------------------------------------------------------

	/**
	 * Serializes an entity to a JSON string.
	 *
	 * @throws Exception if Jackson serialization fails
	 */
	protected String toJson(Object oEntity) throws Exception {
		return s_oMapper.writeValueAsString(oEntity);
	}

	/**
	 * Deserializes a JSON string to an entity of the given class.
	 * Returns null on parse errors (logged internally).
	 */
	protected <T> T fromJson(String sJson, Class<T> oClass) {
		if (sJson == null) {
			return null;
		}
		try {
			return s_oMapper.readValue(sJson, oClass);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteRepository.fromJson: ", oEx);
			return null;
		}
	}

	// ------------------------------------------------------------------
	// Utility helpers
	// ------------------------------------------------------------------

	/**
	 * Returns the SQLite json_extract path for a field name.
	 * Example: {@code jp("userId")} → {@code "$.userId"}
	 */
	protected static String jp(String sFieldName) {
		return "$." + sFieldName;
	}

	// ------------------------------------------------------------------
	// Private / internal helpers
	// ------------------------------------------------------------------

	/**
	 * Appends {@code json_extract(data, ?) = ?} fragments joined by AND for each
	 * map entry. Parameter binding is done separately via bindWhereParams.
	 */
	private void buildWhereClause(StringBuilder oSb, Map<String, Object> aoConditions) {
		boolean bFirst = true;
		for (int i = 0; i < aoConditions.size(); i++) {
			if (!bFirst) {
				oSb.append(" AND ");
			}
			oSb.append("json_extract(data, ?) = ?");
			bFirst = false;
		}
	}

	/**
	 * Binds path and value parameters for each map entry starting at iStartIndex.
	 *
	 * @return the next unused parameter index
	 */
	private int bindWhereParams(PreparedStatement oPs, Map<String, Object> aoConditions, int iStartIndex)
			throws Exception {
		int iIdx = iStartIndex;
		for (Map.Entry<String, Object> oEntry : aoConditions.entrySet()) {
			oPs.setString(iIdx++, jp(oEntry.getKey()));
			setParam(oPs, iIdx++, oEntry.getValue());
		}
		return iIdx;
	}

	private void bindList(PreparedStatement oPs, List<Object> aoParams) throws Exception {
		if (aoParams != null) {
			for (int i = 0; i < aoParams.size(); i++) {
				setParam(oPs, i + 1, aoParams.get(i));
			}
		}
	}

	/**
	 * Sets a single typed parameter on a PreparedStatement.
	 * Handles Integer, Long, Double, Boolean, and String (default toString).
	 */
	private void setParam(PreparedStatement oPs, int iIndex, Object oValue) throws Exception {
		if (oValue == null) {
			oPs.setNull(iIndex, java.sql.Types.NULL);
		} else if (oValue instanceof Integer) {
			oPs.setInt(iIndex, (Integer) oValue);
		} else if (oValue instanceof Long) {
			oPs.setLong(iIndex, (Long) oValue);
		} else if (oValue instanceof Double) {
			oPs.setDouble(iIndex, (Double) oValue);
		} else if (oValue instanceof Boolean) {
			// SQLite stores booleans as 1/0; json_extract returns them as integers
			oPs.setInt(iIndex, ((Boolean) oValue) ? 1 : 0);
		} else {
			oPs.setString(iIndex, oValue.toString());
		}
	}

	/**
	 * Iterates the ResultSet of a prepared statement and deserializes each row's
	 * first column as a JSON entity.
	 */
	private <T> List<T> collectResults(PreparedStatement oPs, Class<T> oClass) throws Exception {
		List<T> aoResults = new ArrayList<>();
		try (ResultSet oRs = oPs.executeQuery()) {
			while (oRs.next()) {
				T oEntity = fromJson(oRs.getString(1), oClass);
				if (oEntity != null) {
					aoResults.add(oEntity);
				}
			}
		}
		return aoResults;
	}
}
