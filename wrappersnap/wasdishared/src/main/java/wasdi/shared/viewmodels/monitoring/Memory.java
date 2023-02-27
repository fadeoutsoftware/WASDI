package wasdi.shared.viewmodels.monitoring;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
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

}