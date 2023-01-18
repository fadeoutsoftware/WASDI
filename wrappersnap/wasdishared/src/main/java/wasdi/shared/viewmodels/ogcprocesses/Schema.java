package wasdi.shared.viewmodels.ogcprocesses;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.ogcprocesses.schemas.ArraySchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.BooleanSchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.DateSchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.DoubleSchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.NumericSchema;
import wasdi.shared.viewmodels.ogcprocesses.schemas.StringSchema;

public class Schema {
	
	public String type;
	
	public static Schema getSchemaFromType(String sType) {
		if (Utils.isNullOrEmpty(sType)) {
			return null;
		}
		
		if (sType.equals("string")) {
			return new StringSchema();
		}
		else if (sType.equals("numeric")) {
			return new NumericSchema();
		}
		else if (sType.equals("double")) {
			return new DoubleSchema();
		}
		else if (sType.equals("array")) {
			return new ArraySchema();
		}
		else if (sType.equals("boolean")) {
			return new BooleanSchema();
		}
		else if (sType.equals("date")) {
			return new DateSchema();
		}		
		
		return null;
	}
}

