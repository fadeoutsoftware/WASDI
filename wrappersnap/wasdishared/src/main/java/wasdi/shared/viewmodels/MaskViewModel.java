package wasdi.shared.viewmodels;

public class MaskViewModel {

	private int colorRed;
	private int colorGreen;
	private int colorBlue;
	private float transparency = 0.5F;

	public int getColorRed() {
		return colorRed;
	}
	public void setColorRed(int colorRed) {
		this.colorRed = colorRed;
	}
	public int getColorGreen() {
		return colorGreen;
	}
	public void setColorGreen(int colorGreen) {
		this.colorGreen = colorGreen;
	}
	public int getColorBlue() {
		return colorBlue;
	}
	public void setColorBlue(int colorBlue) {
		this.colorBlue = colorBlue;
	}
	public float getTransparency() {
		return transparency;
	}
	public void setTransparency(float transparency) {
		this.transparency = transparency;
	}
}