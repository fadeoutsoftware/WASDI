package wasdi.shared.viewmodels;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.json.JSONObject;

@XmlRootElement
public class QueryResultViewModel {
	
	String preview;
	String title;
	String summary;
	String id;
	String link;
	String footprint;
	String provider;
	Map<String, String> properties = new HashMap<String, String>();
	
	//this field must be populated ad hoc in each subclass
	Map<String, String> asProviderToWasdiKeyMap;
	
	public QueryResultViewModel() {
		asProviderToWasdiKeyMap = new HashMap<String, String>();
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
	
	public void populate(JSONObject oJson) {
		String[] asKeys = JSONObject.getNames(oJson);
		for (String sKey: asKeys) {
			String sValue = oJson.optString(sKey);
			addField(sKey, sValue);
			//FIXME manage boolean and other types
		}
	}
	
	public void addField(String sProviderKey, String sValue){
		String sMappedKey = asProviderToWasdiKeyMap.get(sProviderKey);
		if(null == sMappedKey) {
			return;
		}
		try {
			//try with a field first
			Field aoField = this.getClass().getDeclaredField(sMappedKey);
			aoField.set(this,sValue);
			
		} catch (NoSuchFieldException e) {
			//if not add it as a property
			addProperty(sMappedKey, sValue);
		} //catch (NullPointerException e ) {}
		catch (IllegalArgumentException e) {
			// should not happen as it's checked against
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			//Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void addProperty(String sKey, String sValue) {
		properties.put(sKey, sValue);
	}
	
	public void buildSummary() {
		//override in derived classes
	}
}
