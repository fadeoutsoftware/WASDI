package wasdi.shared.viewmodels.stac;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * STAC Collection: one WASDI Workspace exposed as a STAC Collection.
 */
public class StacCollection {

	@JsonProperty("stac_version")
	private String stacVersion = "1.0.0";

	private String type = "Collection";

	private String id;
	private String title;
	private String description;
	private String license = "proprietary";
	private String itemType = "feature";
	private List<String> crs;
	private List<String> keywords;
	private StacExtent extent;
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

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public String getItemType() {
		return itemType;
	}

	public void setItemType(String itemType) {
		this.itemType = itemType;
	}

	public List<String> getCrs() {
		return crs;
	}

	public void setCrs(List<String> crs) {
		this.crs = crs;
	}

	public List<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}

	public StacExtent getExtent() {
		return extent;
	}

	public void setExtent(StacExtent extent) {
		this.extent = extent;
	}

	public List<StacLink> getLinks() {
		return links;
	}

	public void setLinks(List<StacLink> links) {
		this.links = links;
	}
}
