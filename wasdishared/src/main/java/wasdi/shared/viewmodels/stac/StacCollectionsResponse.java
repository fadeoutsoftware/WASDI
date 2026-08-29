package wasdi.shared.viewmodels.stac;

import java.util.List;

/**
 * Response body of GET /stac/collections.
 */
public class StacCollectionsResponse {

	private List<StacCollection> collections;
	private List<StacLink> links;

	public List<StacCollection> getCollections() {
		return collections;
	}

	public void setCollections(List<StacCollection> collections) {
		this.collections = collections;
	}

	public List<StacLink> getLinks() {
		return links;
	}

	public void setLinks(List<StacLink> links) {
		this.links = links;
	}
}
