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
	public ArrayList<OpenEOCollection> collections;
	
}
