package wasdi.shared.viewmodels.ogcprocesses.schemas;

import wasdi.shared.viewmodels.ogcprocesses.Schema;

public class MixedSchema extends Schema {
	
	public MixedSchema() {
		type = null;
	}
	
	public OneOfSchema schema = new OneOfSchema();
}
