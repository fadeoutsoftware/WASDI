package wasdi.jwasdilib.utils;

import java.util.ArrayList;
import java.util.List;

public class MosaicSetting {

	double pixelSizeX = -1.0;
	double pixelSizeY = -1.0;
	Integer noDataValue = null;
	Integer inputIgnoreValue = null;	
	String outputFormat = "GeoTIFF";
	
	List<String> sources = new ArrayList<>();

	public double getPixelSizeX() {
		return pixelSizeX;
	}
	public void setPixelSizeX(double pixelSizeX) {
		this.pixelSizeX = pixelSizeX;
	}
	public double getPixelSizeY() {
		return pixelSizeY;
	}
	public void setPixelSizeY(double pixelSizeY) {
		this.pixelSizeY = pixelSizeY;
	}

	public List<String> getSources() {
		return sources;
	}
	public void setSources(List<String> sources) {
		this.sources = sources;
	}
	
	public String getOutputFormat() {
		return outputFormat;
	}
	public void setOutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
	}
	public Integer getNoDataValue() {
		return noDataValue;
	}
	public void setNoDataValue(Integer noDataValue) {
		this.noDataValue = noDataValue;
	}
	public Integer getInputIgnoreValue() {
		return inputIgnoreValue;
	}
	public void setInputIgnoreValue(Integer inputIgnoreValue) {
		this.inputIgnoreValue = inputIgnoreValue;
	}	
}
