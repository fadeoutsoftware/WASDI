package wasdi.shared.config.openEO;

import java.util.ArrayList;

public class OpenEO {
	public String baseAddress;
	public String api_version = "1.1.0";
	public String backend_version = "";
	public String stac_version = "";
	public String type = "";
	public String id = "";
	public String title = "";
	public String description = "";
	public boolean production = false;
	public String openEOWasdiAppName = "wasdiopeneo";
	public String processes_config = "/etc/wasdi/openeo_processes.json";
	public ArrayList<OpenEOCollection> collections = new ArrayList<>();
	
	/**
	 * Return the configuration of a collection with
	 * @param sId
	 * @return
	 */
	public OpenEOCollection getCollection(String sId) {
		
		for (OpenEOCollection oCollection : collections) {
			if (oCollection.id.equals(sId)) return oCollection;
		}
		
		return null;
	}
	
}
