package wasdi.shared.viewmodels.ogcprocesses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransmissionMode {
	VALUE("value"),
	REFERENCE("reference");

	private String value;

	TransmissionMode(String sValue) {
		this.value = sValue;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
	
	@JsonCreator
	public static TransmissionMode fromValue(String sText) {
		for (TransmissionMode oMode : TransmissionMode.values()) {
			if (String.valueOf(oMode.value).equals(sText)) {
				return oMode;
			}
		}
		return null;
	}
}
