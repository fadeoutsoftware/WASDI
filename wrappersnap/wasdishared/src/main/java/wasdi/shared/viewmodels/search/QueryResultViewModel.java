package wasdi.shared.viewmodels.search;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import wasdi.shared.utils.Utils;

@XmlRootElement
public class QueryResultViewModel {
	
	protected String preview;
	protected String title;
	protected String summary;
	protected String id;
	protected String link;
	protected String footprint;
	protected String provider;
	
	protected Map<String, String> properties = new HashMap<String, String>();
	
	@Override
	public boolean equals(Object arg0) {
		
		if (arg0 ==null) return false;
		
		if (arg0.getClass() == QueryResultViewModel.class) {
			QueryResultViewModel oCompare = (QueryResultViewModel) arg0;
			
			return (this.provider.equals(oCompare.getProvider()) && this.link.equals(oCompare.getLink()) && this.title.equals(oCompare.getTitle()));
		}
		
		return super.equals(arg0);
	}
	
	@Override
	public int hashCode() {
		
		String sProvider = "";
		String sLink = "";
		String sTitle = "";
		
		if (!Utils.isNullOrEmpty(provider)) sProvider = provider;
		if (!Utils.isNullOrEmpty(link)) sLink = link;
		if (!Utils.isNullOrEmpty(title)) sTitle = title;
		
		String sHashCode = sProvider+sLink+sTitle;
		return sHashCode.hashCode();
	}
	
	public String getPreview() {
		return preview;
	}
	public void setPreview(String preview) {
		this.preview = preview;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public String getFootprint() {
		return footprint;
	}
	public void setFootprint(String footprint) {
		this.footprint = footprint;
	}
	public Map<String, String> getProperties() {
		return properties;
	}
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	public String getProvider() {
		return provider;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}	
}
