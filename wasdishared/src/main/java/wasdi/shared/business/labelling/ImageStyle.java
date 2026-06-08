package wasdi.shared.business.labelling;

public class ImageStyle {
	private String id;
	private String datasetId;
	private boolean singleBand;
	private String band1;
	private String band2;
	private String band3;
	private int brightness;
	private int contrast;
	private int hue;
	private int saturation;
	private int lightness;
	private boolean autoLevel;
	private boolean saturateLevel;
	private int saturationValue;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getDatasetId() {
		return datasetId;
	}
	public void setDatasetId(String datasetId) {
		this.datasetId = datasetId;
	}
	public boolean isSingleBand() {
		return singleBand;
	}
	public void setSingleBand(boolean singleBand) {
		this.singleBand = singleBand;
	}
	public String getBand1() {
		return band1;
	}
	public void setBand1(String band1) {
		this.band1 = band1;
	}
	public String getBand2() {
		return band2;
	}
	public void setBand2(String band2) {
		this.band2 = band2;
	}
	public String getBand3() {
		return band3;
	}
	public void setBand3(String band3) {
		this.band3 = band3;
	}
	public int getBrightness() {
		return brightness;
	}
	public void setBrightness(int brightness) {
		this.brightness = brightness;
	}
	public int getContrast() {
		return contrast;
	}
	public void setContrast(int contrast) {
		this.contrast = contrast;
	}
	public int getHue() {
		return hue;
	}
	public void setHue(int hue) {
		this.hue = hue;
	}
	public int getSaturation() {
		return saturation;
	}
	public void setSaturation(int saturation) {
		this.saturation = saturation;
	}
	public int getLightness() {
		return lightness;
	}
	public void setLightness(int lightness) {
		this.lightness = lightness;
	}
	public boolean isAutoLevel() {
		return autoLevel;
	}
	public void setAutoLevel(boolean autoLevel) {
		this.autoLevel = autoLevel;
	}
	public boolean isSaturateLevel() {
		return saturateLevel;
	}
	public void setSaturateLevel(boolean saturateLevel) {
		this.saturateLevel = saturateLevel;
	}
	public int getSaturationValue() {
		return saturationValue;
	}
	public void setSaturationValue(int saturationValue) {
		this.saturationValue = saturationValue;
	}
}
