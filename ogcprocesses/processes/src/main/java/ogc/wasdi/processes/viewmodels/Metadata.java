package ogc.wasdi.processes.viewmodels;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Metadata {
	@JsonProperty("title")
	private String title = null;

	@JsonProperty("role")
	private String role = null;

	@JsonProperty("href")
	private String href = null;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}
}
