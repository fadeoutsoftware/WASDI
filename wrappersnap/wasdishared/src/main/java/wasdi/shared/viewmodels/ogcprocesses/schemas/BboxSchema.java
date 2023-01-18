package wasdi.shared.viewmodels.ogcprocesses.schemas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import wasdi.shared.viewmodels.ogcprocesses.Schema;

public class BboxSchema extends Schema {
	public BboxSchema() {
		type = "";
		
		HashMap<String, String> aoSchema = new HashMap<>();
		aoSchema.put("format", "ogc-bbox");
		aoSchema.put("$ref", "../../openapi/schemas/bbox.yaml");
	}
	
	public ArrayList<Map<String, String>> allOf = new ArrayList<>();
}
