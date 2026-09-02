package wasdi.shared.viewmodels.stac;

public class StacExtent {

	private StacSpatialExtent spatial;
	private StacTemporalExtent temporal;

	public StacSpatialExtent getSpatial() {
		return spatial;
	}

	public void setSpatial(StacSpatialExtent spatial) {
		this.spatial = spatial;
	}

	public StacTemporalExtent getTemporal() {
		return temporal;
	}

	public void setTemporal(StacTemporalExtent temporal) {
		this.temporal = temporal;
	}
}
