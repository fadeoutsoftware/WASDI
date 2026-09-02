package wasdi.shared.viewmodels.ogcprocesses.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;

import wasdi.shared.viewmodels.ogcprocesses.Schema;

public class DoubleSchema extends Schema {
	public DoubleSchema() {
		// "numeric" is not a valid JSON Schema type; the correct type is "number"
		type = "number";
	}
	
	public String format = "double";

	public Integer minimum;
	public Integer maximum;
	@JsonProperty("default")
	public Integer _default;
	public boolean exclusiveMinimum = false;	

}
