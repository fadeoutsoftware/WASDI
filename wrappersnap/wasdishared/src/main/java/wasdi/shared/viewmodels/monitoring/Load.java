package wasdi.shared.viewmodels.monitoring;

public class Load {

	private LoadData absolute;
	private LoadData percentage;
	public LoadData getPercentage() {
		return percentage;
	}
	public void setPercentage(LoadData percentage) {
		this.percentage = percentage;
	}
	public LoadData getAbsolute() {
		return absolute;
	}
	public void setAbsolute(LoadData absolute) {
		this.absolute = absolute;
	}

}