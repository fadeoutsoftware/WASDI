package wasdi.shared.viewmodels.ogcprocesses.schemas;

import wasdi.shared.viewmodels.ogcprocesses.Schema;

public class DateSchema extends Schema {
	public DateSchema() {
		type="string";
	}
	
	public String format = "dateTime";
}
