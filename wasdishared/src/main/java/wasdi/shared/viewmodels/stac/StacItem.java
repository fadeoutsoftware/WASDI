package wasdi.shared.viewmodels.stac;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * STAC Item: one WASDI file (product) exposed as a GeoJSON Feature + STAC metadata.
 */
public class StacItem {

	@JsonProperty("stac_version")
	private String stacVersion = "1.0.0";

	private String type = "Feature";

	private String id;
	private StacGeometry geometry;
	private List<Double> bbox;
	private Map<String, Object> properties;
	private String collection;
	private Map<String, StacAsset> assets;
	private List<StacLink> links;

	public String getStacVersion() {
		return stacVersion;
	}

	public void setStacVersion(String stacVersion) {
		this.stacVersion = stacVersion;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public StacGeometry getGeometry() {
		return geometry;
	}

	public void setGeometry(StacGeometry geometry) {
		this.geometry = geometry;
	}

	public List<Double> getBbox() {
		return bbox;
	}

	public void setBbox(List<Double> bbox) {
		this.bbox = bbox;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public String getCollection() {
		return collection;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	public Map<String, StacAsset> getAssets() {
		return assets;
	}

	public void setAssets(Map<String, StacAsset> assets) {
		this.assets = assets;
	}

	public List<StacLink> getLinks() {
		return links;
	}

	public void setLinks(List<StacLink> links) {
		this.links = links;
	}
}
