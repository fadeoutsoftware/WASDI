package wasdi.shared.data.sqlite;

import java.util.Arrays;
import java.util.List;

import wasdi.shared.data.interfaces.IMetricsEntryRepositoryBackend;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.monitoring.MetricsEntry;

public class SqliteMetricsEntryRepositoryBackend extends SqliteRepository implements IMetricsEntryRepositoryBackend {

	public SqliteMetricsEntryRepositoryBackend() {
		m_sThisCollection = "metricsentries";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insertMetricsEntry(MetricsEntry oMetricsEntry) {
		try {
			return insert(m_sThisCollection, oMetricsEntry.getNode(), oMetricsEntry);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MetricsEntryRepository.insertMetricsEntry: error", oEx);
		}

		return false;
	}

	@Override
	public boolean updateMetricsEntry(MetricsEntry oMetricsEntry) {
		try {
			return upsert(m_sThisCollection, oMetricsEntry.getNode(), oMetricsEntry);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MetricsEntryRepository.updateMetricsEntry: error", oEx);
		}

		return false;
	}

	@Override
	public List<MetricsEntry> getMetricsEntries() {
		try {
			return findAll(m_sThisCollection, MetricsEntry.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MetricsEntryRepository.getMetricsEntries: error", oEx);
		}

		return new java.util.ArrayList<>();
	}

	@Override
	public List<MetricsEntry> getMetricsEntryByNode(String sNode) {
		try {
			return findAllWhere(m_sThisCollection, "node", sNode, MetricsEntry.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MetricsEntryRepository.getMetricsEntryByNode: error", oEx);
		}

		return new java.util.ArrayList<>();
	}

	@Override
	public MetricsEntry getLatestMetricsEntryByNode(String sNode) {
		try {
			String sQuery = "SELECT data FROM " + m_sThisCollection
					+ " WHERE json_extract(data,'$.node') = ?"
					+ " ORDER BY json_extract(data,'$.timestamp.seconds') DESC LIMIT 1";

			return queryOne(sQuery, Arrays.asList(sNode), MetricsEntry.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("MetricsEntryRepository.getLatestMetricsEntryByNode: error", oEx);
		}

		return null;
	}
}
