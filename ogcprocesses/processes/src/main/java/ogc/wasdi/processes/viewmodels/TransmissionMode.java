package ogc.wasdi.processes.viewmodels;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TransmissionMode {
	VALUE("value"),
	REFERENCE("reference");

	private String value;

	TransmissionMode(String sValue) {
		this.value = sValue;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
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
