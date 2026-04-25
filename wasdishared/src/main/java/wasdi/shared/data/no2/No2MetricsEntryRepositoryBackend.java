package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.data.interfaces.IMetricsEntryRepositoryBackend;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.monitoring.MetricsEntry;

/**
 * NO2 backend implementation for metrics entry repository.
 */
public class No2MetricsEntryRepositoryBackend extends No2Repository implements IMetricsEntryRepositoryBackend {

	private static final String s_sCollectionName = "metricsentries";

	@Override
	public boolean insertMetricsEntry(MetricsEntry oMetricsEntry) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oMetricsEntry == null) {
				return false;
			}

			oCollection.insert(toDocument(oMetricsEntry));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2MetricsEntryRepositoryBackend.insertMetricsEntry: error", oEx);
		}

		return false;
	}

	@Override
	public boolean updateMetricsEntry(MetricsEntry oMetricsEntry) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oMetricsEntry == null) {
				return false;
			}

			oCollection.update(where("node").eq(oMetricsEntry.getNode()), toDocument(oMetricsEntry));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2MetricsEntryRepositoryBackend.updateMetricsEntry: error", oEx);
		}

		return insertMetricsEntry(oMetricsEntry);
	}

	@Override
	public List<MetricsEntry> getMetricsEntries() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find() : null;
			return toList(oCursor, MetricsEntry.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2MetricsEntryRepositoryBackend.getMetricsEntries: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<MetricsEntry> getMetricsEntryByNode(String sNode) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return new ArrayList<>();
			}
			DocumentCursor oCursor = oCollection.find(where("node").eq(sNode));
			return toList(oCursor, MetricsEntry.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2MetricsEntryRepositoryBackend.getMetricsEntryByNode: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public MetricsEntry getLatestMetricsEntryByNode(String sNode) {
		try {
			List<MetricsEntry> aoEntries = getMetricsEntryByNode(sNode);
			aoEntries.sort(Comparator.comparing(
					(MetricsEntry oEntry) -> getTimestampSortKey(oEntry),
					Comparator.nullsLast(Double::compareTo)).reversed());

			for (MetricsEntry oEntry : aoEntries) {
				if (oEntry != null) {
					return oEntry;
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2MetricsEntryRepositoryBackend.getLatestMetricsEntryByNode: error", oEx);
		}

		return null;
	}

	private Double getTimestampSortKey(MetricsEntry oEntry) {
		if (oEntry == null || oEntry.getTimestamp() == null) {
			return null;
		}

		Double dSeconds = oEntry.getTimestamp().getSeconds();
		Double dMillis = oEntry.getTimestamp().getMillis();

		if (dSeconds == null && dMillis == null) {
			return null;
		}

		double dSafeSeconds = dSeconds != null ? dSeconds.doubleValue() : 0d;
		double dSafeMillis = dMillis != null ? dMillis.doubleValue() : 0d;
		return dSafeSeconds + (dSafeMillis / 1000d);
	}
}
