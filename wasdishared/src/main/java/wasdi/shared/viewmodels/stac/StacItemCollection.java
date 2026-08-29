package wasdi.shared.viewmodels.stac;

import java.util.List;

/**
 * Response body of GET /stac/collections/{workspaceId}/items: a GeoJSON FeatureCollection of STAC Items.
 */
public class StacItemCollection {

	private String type = "FeatureCollection";
	private String timeStamp;
	private List<StacItem> features;
	private List<StacLink> links;
	private Integer numberMatched;
	private Integer numberReturned;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<StacItem> getFeatures() {
		return features;
	}

	public void setFeatures(List<StacItem> features) {
		this.features = features;
	}

	public List<StacLink> getLinks() {
		return links;
	}

	public void setLinks(List<StacLink> links) {
		this.links = links;
	}

	public Integer getNumberMatched() {
		return numberMatched;
	}

	public void setNumberMatched(Integer numberMatched) {
		this.numberMatched = numberMatched;
	}

	public Integer getNumberReturned() {
		return numberReturned;
	}

	public void setNumberReturned(Integer numberReturned) {
		this.numberReturned = numberReturned;
	}
	
	public String getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}	
}
