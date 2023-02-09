package wasdi.shared.viewmodels.ogcprocesses.schemas;

import java.util.ArrayList;

import wasdi.shared.viewmodels.ogcprocesses.Schema;

public class OneOfSchema extends Schema {
	
	public OneOfSchema() {
		type = null;
	}
	
	public ArrayList<Schema> oneOf = new ArrayList<>();
}
