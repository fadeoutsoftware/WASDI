package wasdi.shared.viewmodels.stac;

/**
 * GeoJSON geometry: coordinates shape/depth depends on "type", so it is kept generic (Object).
 */
public class StacGeometry {

	private String type;
	private Object coordinates;

	public StacGeometry() {
	}

	public StacGeometry(String type, Object coordinates) {
		this.type = type;
		this.coordinates = coordinates;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Object getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(Object coordinates) {
		this.coordinates = coordinates;
	}
}
