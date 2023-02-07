package wasdi.shared.viewmodels.ogcprocesses.schemas;

import wasdi.shared.viewmodels.ogcprocesses.Schema;

public class NumericSchema extends Schema {
	public NumericSchema() {
		type = "numeric";
	}
	
	public Integer minimum;
	public Integer maximum;
	public boolean exclusiveMinimum = false;	
}
