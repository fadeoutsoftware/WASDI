package wasdi.shared.viewmodels.monitoring;

public class Memory {

	private DataBlockAbsolute absolute;
	private DataBlockPercentage percentage;

	public Double getPercentageAvailable() {
		Double oValue = null;

		if (this.percentage != null) {
			DataEntryPercentage oDataEntryPercentage = this.percentage.getAvailable();

			if (oDataEntryPercentage != null) {
				oValue = oDataEntryPercentage.getValue();
			}
		}

		return oValue;
	}

	public Double getPercentageUsed() {
		Double oValue = null;

		if (this.percentage != null) {
			DataEntryPercentage oDataEntryPercentage = this.percentage.getUsed();

			if (oDataEntryPercentage != null) {
				oValue = oDataEntryPercentage.getValue();
			}
		}

		return oValue;
	}

	public Long getAbsoluteAvailable() {
		Long oValue = null;

		if (this.absolute != null) {
			DataEntryAbsolute oDataEntryAbsolute = this.absolute.getAvailable();

			if (oDataEntryAbsolute != null) {
				oValue = oDataEntryAbsolute.getValue();
			}
		}

		return oValue;
	}

	public Long getAbsoluteUsed() {
		Long oValue = null;

		if (this.absolute != null) {
			DataEntryAbsolute oDataEntryAbsolute = this.absolute.getUsed();

			if (oDataEntryAbsolute != null) {
				oValue = oDataEntryAbsolute.getValue();
			}
		}

		return oValue;
	}

	public Long getAbsoluteTotal() {
		Long oValue = null;

		if (this.absolute != null) {
			DataEntryAbsolute oDataEntryAbsolute = this.absolute.getTotal();

			if (oDataEntryAbsolute != null) {
				oValue = oDataEntryAbsolute.getValue();
			}
		}

		return oValue;
	}

	public DataBlockAbsolute getAbsolute() {
		return absolute;
	}

	public void setAbsolute(DataBlockAbsolute absolute) {
		this.absolute = absolute;
	}

	public DataBlockPercentage getPercentage() {
		return percentage;
	}

	public void setPercentage(DataBlockPercentage percentage) {
		this.percentage = percentage;
	}

}