package wasdi.shared.viewmodels.ogcprocesses.schemas;

import java.util.LinkedHashMap;
import java.util.Map;

import wasdi.shared.viewmodels.ogcprocesses.Schema;

public class BboxSchema extends Schema {
	
	// Matches the canonical OGC API Processes bbox input schema (bbox + crs properties)
	public Map<String, Object> properties = new LinkedHashMap<>();
	
	public BboxSchema() {
		type = "object";
		
		Map<String, Object> oBboxProperty = new LinkedHashMap<>();
		oBboxProperty.put("type", "array");
		
		Map<String, String> oItemsSchema = new LinkedHashMap<>();
		oItemsSchema.put("type", "number");
		oBboxProperty.put("items", oItemsSchema);
		oBboxProperty.put("minItems", 4);
		oBboxProperty.put("maxItems", 6);
		
		Map<String, Object> oCrsProperty = new LinkedHashMap<>();
		oCrsProperty.put("type", "string");
		oCrsProperty.put("format", "uri");
		
		properties.put("bbox", oBboxProperty);
		properties.put("crs", oCrsProperty);
	}
}
