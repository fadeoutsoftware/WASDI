package wasdi.shared.viewmodels.ogcprocesses;

public class Link {
	private String href = "";
	private String rel = "";
	private String type = "";
	private String hreflang = "en";
	private String title = "title";
	  
	public String getHref() {
		return href;
	}
	public void setHref(String href) {
		this.href = href;
	}
	public String getRel() {
		return rel;
	}
	public void setRel(String rel) {
		this.rel = rel;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getHreflang() {
		return hreflang;
	}
	public void setHreflang(String hreflang) {
		this.hreflang = hreflang;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
}
