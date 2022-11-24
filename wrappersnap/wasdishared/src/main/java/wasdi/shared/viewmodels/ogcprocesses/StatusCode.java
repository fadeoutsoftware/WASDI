package wasdi.shared.viewmodels.ogcprocesses;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StatusCode {
	ACCEPTED("accepted"),
	RUNNING("running"),
	SUCCESSFUL("successful"),
	FAILED("failed"),
	DISMISSED("dismissed");

	private String value;

	StatusCode(String sValue) {
		this.value = sValue;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@JsonCreator
	public static StatusCode fromValue(String sText) {
		for (StatusCode oCode : StatusCode.values()) {
			if (String.valueOf(oCode.value).equals(sText)) {
				return oCode;
			}
		}
		return null;
	}
}
