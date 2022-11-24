package wasdi.shared.viewmodels.ogcprocesses;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Reference {
	@JsonProperty("$ref")
	private String $ref = null;

	public String get$ref() {
		return $ref;
	}

	public void set$ref(String $ref) {
		this.$ref = $ref;
	}
}
