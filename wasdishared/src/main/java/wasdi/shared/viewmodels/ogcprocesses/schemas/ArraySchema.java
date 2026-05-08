package wasdi.shared.viewmodels.ogcprocesses.schemas;

import wasdi.shared.viewmodels.ogcprocesses.Schema;

public class ArraySchema extends Schema {
	public ArraySchema() {
		type = "array";
	}
	public Integer minItems=null;
	public Integer maxItems=null;
	public Schema items=null;
}
