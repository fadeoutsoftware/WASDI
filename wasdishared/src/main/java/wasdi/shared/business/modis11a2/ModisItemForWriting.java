package wasdi.shared.business.modis11a2;

public class ModisItemForWriting {
	
	public ModisItemForWriting() {
		
	}
	
	private String sFileName;
	private long lFileSize;
	private String sDayNightFlag;
	private Double dStartDate;
	private Double dEndDate;
	private String sBoundingBox;
	private String sInstrument; 
	private String sSensor;
	private String sLatitude;
	private String sPlatform;
	private String sUrl;
	
	public String getSFileName() {
		return sFileName;
	}
	public void setSFileName(String sFileName) {
		this.sFileName = sFileName;
	}
	public long getLFileSize() {
		return lFileSize;
	}
	public void setLFileSize(long lFileSize) {
		this.lFileSize = lFileSize;
	}
	public String getSDayNightFlag() {
		return sDayNightFlag;
	}
	public void setSDayNightFlag(String sDayNightFlag) {
		this.sDayNightFlag = sDayNightFlag;
	}
	public Double getDStartDate() {
		return dStartDate;
	}
	public void setDStartDate(Double dStartDate) {
		this.dStartDate = dStartDate;
	}
	public Double getDEndDate() {
		return dEndDate;
	}
	public void setDEndDate(Double dEndDate) {
		this.dEndDate = dEndDate;
	}
	public String getSBoundingBox() {
		return sBoundingBox;
	}
	public void setSBoundingBox(String sBoundingBox) {
		this.sBoundingBox = sBoundingBox;
	}
	public String getSInstrument() {
		return sInstrument;
	}
	public void setSInstrument(String sInstrument) {
		this.sInstrument = sInstrument;
	}
	public String getSSensor() {
		return sSensor;
	}
	public void setSSensor(String sSensor) {
		this.sSensor = sSensor;
	}
	public String getSLatitude() {
		return sLatitude;
	}
	public void setSLatitude(String sLatitude) {
		this.sLatitude = sLatitude;
	}
	public String getSPlatform() {
		return sPlatform;
	}
	public void setSPlatform(String sPlatform) {
		this.sPlatform = sPlatform;
	}
	public String getSUrl() {
		return sUrl;
	}
	public void setSUrl(String sUrl) {
		this.sUrl = sUrl;
	}
}
