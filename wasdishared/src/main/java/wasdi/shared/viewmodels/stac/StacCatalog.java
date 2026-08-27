package wasdi.shared.viewmodels.stac;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * STAC landing page (GET /stac).
 */
public class StacCatalog {

	@JsonProperty("stac_version")
	private String stacVersion = "1.0.0";

	private String type = "Catalog";

	private String id;
	private String title;
	private String description;

	@JsonProperty("conformsTo")
	private List<String> conformsTo;

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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getConformsTo() {
		return conformsTo;
	}

	public void setConformsTo(List<String> conformsTo) {
		this.conformsTo = conformsTo;
	}

	public List<StacLink> getLinks() {
		return links;
	}

	public void setLinks(List<StacLink> links) {
		this.links = links;
	}
}
