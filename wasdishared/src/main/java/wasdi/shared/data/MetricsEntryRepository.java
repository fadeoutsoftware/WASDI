package wasdi.shared.data;

import java.util.List;

import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IMetricsEntryRepositoryBackend;
import wasdi.shared.viewmodels.monitoring.MetricsEntry;

public class MetricsEntryRepository {

	private final IMetricsEntryRepositoryBackend m_oBackend;

	public MetricsEntryRepository() {
		m_oBackend = createBackend();
	}

	private IMetricsEntryRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createMetricsEntryRepository();
	}

	public boolean insertMetricsEntry(MetricsEntry oMetricsEntry) {
		return m_oBackend.insertMetricsEntry(oMetricsEntry);
	}

	public boolean updateMetricsEntry(MetricsEntry oMetricsEntry) {
		return m_oBackend.updateMetricsEntry(oMetricsEntry);
	}

	public List<MetricsEntry> getMetricsEntries() {
		return m_oBackend.getMetricsEntries();
	}

	public List<MetricsEntry> getMetricsEntryByNode(String sNode) {
		return m_oBackend.getMetricsEntryByNode(sNode);
	}

	public MetricsEntry getLatestMetricsEntryByNode(String sNode) {
		return m_oBackend.getLatestMetricsEntryByNode(sNode);
	}

}

