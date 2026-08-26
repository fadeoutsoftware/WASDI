package wasdi.shared.viewmodels.ogcprocesses.schemas;

import wasdi.shared.viewmodels.ogcprocesses.Schema;

public class NumericSchema extends Schema {
	public NumericSchema() {
		// "numeric" is not a valid JSON Schema type; the correct type is "integer"
		type = "integer";
	}
	
	public Integer minimum;
	public Integer maximum;
	public boolean exclusiveMinimum = false;	
}
