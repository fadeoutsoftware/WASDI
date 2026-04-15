package wasdi.shared.parameters.settings;

import java.util.ArrayList;

public class MultiSubsetSetting implements ISetting {
	
	ArrayList<String> outputNames = new ArrayList<>();
	ArrayList<Double> latNList = new ArrayList<>();
	ArrayList<Double> lonWList = new ArrayList<>();
	ArrayList<Double> latSList = new ArrayList<>();
	ArrayList<Double> lonEList = new ArrayList<>();
	ArrayList<String> bands = new ArrayList<>();
	boolean bigTiff = false;
	
	public boolean getBigTiff() {
		return bigTiff;
	}
	public void setBigTiff(boolean bigTiff) {
		this.bigTiff = bigTiff;
	}
	public ArrayList<String> getOutputNames() {
		return outputNames;
	}
	public void setOutputNames(ArrayList<String> outputNames) {
		this.outputNames = outputNames;
	}
	public ArrayList<Double> getLatNList() {
		return latNList;
	}
	public void setLatNList(ArrayList<Double> latNList) {
		this.latNList = latNList;
	}
	public ArrayList<Double> getLonWList() {
		return lonWList;
	}
	public void setLonWList(ArrayList<Double> lonWList) {
		this.lonWList = lonWList;
	}
	public ArrayList<Double> getLatSList() {
		return latSList;
	}
	public void setLatSList(ArrayList<Double> latSList) {
		this.latSList = latSList;
	}
	public ArrayList<Double> getLonEList() {
		return lonEList;
	}
	public void setLonEList(ArrayList<Double> lonEList) {
		this.lonEList = lonEList;
	}
	public ArrayList<String> getBands() {
		return bands;
	}
	public void setBands(ArrayList<String> bands) {
		this.bands = bands;
	}
}
