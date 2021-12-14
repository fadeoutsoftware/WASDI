package wasdi.shared.parameters.settings;

public class SubsetSetting implements ISetting {
    double latN = 90.0;
    double lonW = 180.0;
    double latS = -90.0;
    double lonE = -180.0;
    
    
    
	public double getLatN() {
		return latN;
	}
	public void setLatN(double latN) {
		this.latN = latN;
	}
	public double getLonW() {
		return lonW;
	}
	public void setLonW(double lonW) {
		this.lonW = lonW;
	}
	public double getLatS() {
		return latS;
	}
	public void setLatS(double latS) {
		this.latS = latS;
	}
	public double getLonE() {
		return lonE;
	}
	public void setLonE(double lonE) {
		this.lonE = lonE;
	}
    
    
}
