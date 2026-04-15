package wasdi.shared.viewmodels.ogcprocesses.schemas;

import wasdi.shared.viewmodels.ogcprocesses.Schema;

public class StringArraySchema extends Schema {
	public StringArraySchema() {
		type = "array";
	}
	
	public Schema items = new StringSchema();
	
}
