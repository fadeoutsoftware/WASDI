package wasdi.shared.viewmodels.stac;

import java.util.List;

/**
 * STAC Collection "extent.spatial": one or more [west, south, east, north] bboxes, first is the overall one.
 */
public class StacSpatialExtent {

	private List<List<Double>> bbox;

	public List<List<Double>> getBbox() {
		return bbox;
	}

	public void setBbox(List<List<Double>> bbox) {
		this.bbox = bbox;
	}
}
