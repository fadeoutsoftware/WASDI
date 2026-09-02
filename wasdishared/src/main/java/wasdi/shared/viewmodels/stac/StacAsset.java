package wasdi.shared.viewmodels.stac;

import java.util.List;

/**
 * STAC Asset object: a downloadable/linkable file belonging to an Item.
 */
public class StacAsset {

	private String href;
	private String title;
	private String type;
	private List<String> roles;

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
}
