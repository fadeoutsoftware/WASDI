package wasdi.jwasdilib.utils;

import java.util.ArrayList;
import java.util.List;

public class MosaicSetting {

	String crs = "GEOGCS[\"WGS84(DD)\", \r\n" + 
    		"\t\t  DATUM[\"WGS84\", \r\n" + 
    		"\t\t\tSPHEROID[\"WGS84\", 6378137.0, 298.257223563]], \r\n" + 
    		"\t\t  PRIMEM[\"Greenwich\", 0.0], \r\n" + 
    		"\t\t  UNIT[\"degree\", 0.017453292519943295], \r\n" + 
    		"\t\t  AXIS[\"Geodetic longitude\", EAST], \r\n" + 
    		"\t\t  AXIS[\"Geodetic latitude\", NORTH]]";
	double southBound = -1.0;
	double eastBound = -1.0;
	double westBound = -1.0;
	double northBound = -1.0;
	double pixelSizeX = -1.0;
	double pixelSizeY = -1.0;
	Integer noDataValue = null;
	Integer inputIgnoreValue = null;
	String overlappingMethod = "MOSAIC_TYPE_OVERLAY";
	Boolean showSourceProducts = false;
	String elevationModelName = "ASTER 1sec GDEM";
	String resamplingName = "Nearest";
	Boolean updateMode = false;
	Boolean nativeResolution = true;
	String combine = "OR";
	
	String outputFormat = "GeoTIFF";
	
	List<String> sources = new ArrayList<>();
	List<String> variableNames = new ArrayList<>();
	List<String> variableExpressions = new ArrayList<>();
	
	public String getCrs() {
		return crs;
	}
	public void setCrs(String crs) {
		this.crs = crs;
	}
	public double getSouthBound() {
		return southBound;
	}
	public void setSouthBound(double southBound) {
		this.southBound = southBound;
	}
	public double getEastBound() {
		return eastBound;
	}
	public void setEastBound(double eastBound) {
		this.eastBound = eastBound;
	}
	public double getWestBound() {
		return westBound;
	}
	public void setWestBound(double westBound) {
		this.westBound = westBound;
	}
	public double getNorthBound() {
		return northBound;
	}
	public void setNorthBound(double northBound) {
		this.northBound = northBound;
	}
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
	public String getOverlappingMethod() {
		return overlappingMethod;
	}
	public void setOverlappingMethod(String overlappingMethod) {
		this.overlappingMethod = overlappingMethod;
	}
	public Boolean getShowSourceProducts() {
		return showSourceProducts;
	}
	public void setShowSourceProducts(Boolean showSourceProducts) {
		this.showSourceProducts = showSourceProducts;
	}
	public String getElevationModelName() {
		return elevationModelName;
	}
	public void setElevationModelName(String elevationModelName) {
		this.elevationModelName = elevationModelName;
	}
	public String getResamplingName() {
		return resamplingName;
	}
	public void setResamplingName(String resamplingName) {
		this.resamplingName = resamplingName;
	}
	public Boolean getUpdateMode() {
		return updateMode;
	}
	public void setUpdateMode(Boolean updateMode) {
		this.updateMode = updateMode;
	}
	public Boolean getNativeResolution() {
		return nativeResolution;
	}
	public void setNativeResolution(Boolean nativeResolution) {
		this.nativeResolution = nativeResolution;
	}
	public String getCombine() {
		return combine;
	}
	public void setCombine(String combine) {
		this.combine = combine;
	}
	public List<String> getSources() {
		return sources;
	}
	public void setSources(List<String> sources) {
		this.sources = sources;
	}
	public List<String> getVariableNames() {
		return variableNames;
	}
	public void setVariableNames(List<String> variableNames) {
		this.variableNames = variableNames;
	}
	public List<String> getVariableExpressions() {
		return variableExpressions;
	}
	public void setVariableExpressions(List<String> variableExpressions) {
		this.variableExpressions = variableExpressions;
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
