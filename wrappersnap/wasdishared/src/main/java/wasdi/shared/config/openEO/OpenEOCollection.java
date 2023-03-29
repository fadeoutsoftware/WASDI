package wasdi.shared.config.openEO;

import java.util.ArrayList;

public class OpenEOCollection {
	
	public String stac_version;
	public String type;
	public String id;
	public String title;
	public String description;
	public String license;
	
	public OpenEOExtent extent; 
	
	public ArrayList<String> keywords;
	
	public ArrayList<OpenEOProvider> providers;
}
