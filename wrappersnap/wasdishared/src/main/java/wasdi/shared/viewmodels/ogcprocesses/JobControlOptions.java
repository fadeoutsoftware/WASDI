package wasdi.shared.viewmodels.ogcprocesses;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum JobControlOptions {
	SYNC_EXECUTE("sync-execute"),
	ASYNC_EXECUTE("async-execute"),
	DISMISS("dismiss");
	
	private String value;

	JobControlOptions(String sValue) {
		this.value = sValue;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@JsonCreator
	public static JobControlOptions fromValue(String sText) {
		for (JobControlOptions oOption : JobControlOptions.values()) {
			if (String.valueOf(oOption.value).equals(sText)) {
				return oOption;
			}
		}
		return null;
	}
}