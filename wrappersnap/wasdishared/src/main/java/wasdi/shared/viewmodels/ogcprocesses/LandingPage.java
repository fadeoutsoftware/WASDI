
package wasdi.shared.viewmodels.ogcprocesses;

import java.util.ArrayList;
import java.util.List;

/**
 * LandingPage
 */
public class LandingPage   {
	private String title = null;
	private String description = null;
	private List<Link> links = new ArrayList<Link>();
	
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
	public List<Link> getLinks() {
		return links;
	}
	public void setLinks(List<Link> links) {
		this.links = links;
	}
}
