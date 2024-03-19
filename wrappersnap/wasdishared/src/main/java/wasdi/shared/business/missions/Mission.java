package wasdi.shared.business.missions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Represent a Mission or more in general a Data Collection.
 * Missions are stored in a json config file (usually called appconfig.json).
 * 
 * This info are sent to the client to render the search form and also to create
 * the query string.
 * Eeach element has few info (name, index name and value, selected, provider, ispublic, userid.
 * Then there are the filters => all the options available to search this kind of data.
 * 
 * 
 */
public class Mission {
	
	/**
	 * Mission Name
	 */
	private String name;
	/**
	 * This is the name of the key to use in the query
	 */
	private String indexname;
	/**
	 * This is the value of the key to use in the query
	 */
	private String indexvalue;
	/**
	 * True if it is the default mission
	 */
	private boolean selected = false;
	/**
	 * Provider (to be verified where is used!!)
	 */
	private String provider;
	/**
	 * True if it is a public Mssion/Collection, false otherwise
	 */
	private boolean ispublic = true;
	/**
	 * Optional Onwer of the Mission/Collection
	 */
	private String userid;
	/**
	 * List of filters
	 */
	private List<HashMap<String, Object>> filters = new ArrayList<HashMap<String, Object>>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIndexname() {
		return indexname;
	}

	public void setIndexname(String indexname) {
		this.indexname = indexname;
	}

	public String getIndexvalue() {
		return indexvalue;
	}

	public void setIndexvalue(String indexvalue) {
		this.indexvalue = indexvalue;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public boolean isIspublic() {
		return ispublic;
	}

	public void setIspublic(boolean ispublic) {
		this.ispublic = ispublic;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	public List<HashMap<String, Object>> getFilters() {
		return filters;
	}

	public void setFilters(List<HashMap<String, Object>> filters) {
		this.filters = filters;
	}
	

}
