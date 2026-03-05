package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.viewmodels.monitoring.MetricsEntry;

/**
 * Backend contract for metrics entry repository.
 */
public interface IMetricsEntryRepositoryBackend {

	boolean insertMetricsEntry(MetricsEntry oMetricsEntry);

	boolean updateMetricsEntry(MetricsEntry oMetricsEntry);

	List<MetricsEntry> getMetricsEntries();

	List<MetricsEntry> getMetricsEntryByNode(String sNode);

	MetricsEntry getLatestMetricsEntryByNode(String sNode);
}
