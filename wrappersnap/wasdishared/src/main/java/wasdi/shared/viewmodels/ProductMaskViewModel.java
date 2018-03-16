package wasdi.shared.viewmodels;

public class ProductMaskViewModel {

	String name;
	int colorRed, colorGreen, colorBlue;
	String maskType;
	String description;
	float transparency = 0.5F;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
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
	public String getMaskType() {
		return maskType;
	}
	public void setMaskType(String maskType) {
		this.maskType = maskType;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public float getTransparency() {
		return transparency;
	}
	public void setTransparency(float transparency) {
		this.transparency = transparency;
	}
	
}
