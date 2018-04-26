package wasdi.shared.viewmodels;

import java.awt.Color;

public class ColorViewModel {

	private int colorRed;
	private int colorGreen;
	private int colorBlue;

	public ColorViewModel() {
	}

	public ColorViewModel(Color oColor) {
		setColorRed(oColor.getRed());
		setColorGreen(oColor.getGreen());
		setColorBlue(oColor.getBlue());
	}
	
	public Color asColor() {
		return new Color(colorRed, colorGreen, colorBlue);
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
	
	

}