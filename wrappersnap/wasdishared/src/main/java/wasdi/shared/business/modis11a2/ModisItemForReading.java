package wasdi.shared.business.modis11a2;

public class ModisItemForReading {
	
	public ModisItemForReading() {
		
	}
	
	private String fileName;
	private long fileSize;
	private String dayNightFlag;
	
	private Double startDate;
	private Double endDate;

	private ModisLocation boundingBox;

	private String instrument; 
	private String sensor;
	private String platform;
	private String url;
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String sFileName) {
		this.fileName = sFileName;
	}
	public long getFileSize() {
		return fileSize;
	}
	public void setFileSize(long lFileSize) {
		this.fileSize = lFileSize;
	}
	public String getDayNightFlag() {
		return dayNightFlag;
	}
	public void setDayNightFlag(String sDayNightFlag) {
		this.dayNightFlag = sDayNightFlag;
	}
	public Double getStartDate() {
		return startDate;
	}
	public void setStartDate(Double dStartDate) {
		this.startDate = dStartDate;
	}
	public Double getEndDate() {
		return endDate;
	}
	public void setDEndDate(Double dEndDate) {
		this.endDate = dEndDate;
	}
	public ModisLocation getBoundingBox() {
		return boundingBox;
	}
	public void setBoundingBox(ModisLocation oBoundingBox) {
		this.boundingBox = oBoundingBox;
	}
	public String getInstrument() {
		return instrument;
	}
	public void setSInstrument(String sInstrument) {
		this.instrument = sInstrument;
	}
	public String getSensor() {
		return sensor;
	}
	public void setSensor(String sSensor) {
		this.sensor = sSensor;
	}

	public String getPlatform() {
		return platform;
	}
	public void setSPlatform(String sPlatform) {
		this.platform = sPlatform;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String sUrl) {
		this.url = sUrl;
	}

}
