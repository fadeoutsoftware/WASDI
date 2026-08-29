package wasdi.shared.viewmodels.stac;

import java.util.List;

/**
 * STAC Collection "extent.temporal": one or more [start, end] ISO-8601 intervals (either end may be null).
 */
public class StacTemporalExtent {

	private List<List<String>> interval;

	public List<List<String>> getInterval() {
		return interval;
	}

	public void setInterval(List<List<String>> interval) {
		this.interval = interval;
	}
}
