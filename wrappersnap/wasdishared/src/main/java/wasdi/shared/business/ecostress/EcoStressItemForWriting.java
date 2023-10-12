package wasdi.shared.business.ecostress;

public class EcoStressItemForWriting {
	
	public EcoStressItemForWriting() {
		
	}

	private String fileName;

	private int startOrbitNumber;
	private int stopOrbitNumber;

	private String dayNightFlag;

	private Double beginningDate;
	private Double endingDate;

	private String location;

	private String platform;
	private String instrument;
	private String sensor;
	private String parameterName;

	private String s3Path;
	private String url;
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public int getStartOrbitNumber() {
		return startOrbitNumber;
	}
	public void setStartOrbitNumber(int startOrbitNumber) {
		this.startOrbitNumber = startOrbitNumber;
	}
	public int getStopOrbitNumber() {
		return stopOrbitNumber;
	}
	public void setStopOrbitNumber(int stopOrbitNumber) {
		this.stopOrbitNumber = stopOrbitNumber;
	}
	public String getDayNightFlag() {
		return dayNightFlag;
	}
	public void setDayNightFlag(String dayNightFlag) {
		this.dayNightFlag = dayNightFlag;
	}
	public Double getBeginningDate() {
		return beginningDate;
	}
	public void setBeginningDate(Double beginningDate) {
		this.beginningDate = beginningDate;
	}
	public Double getEndingDate() {
		return endingDate;
	}
	public void setEndingDate(Double endingDate) {
		this.endingDate = endingDate;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getPlatform() {
		return platform;
	}
	public void setPlatform(String platform) {
		this.platform = platform;
	}
	public String getInstrument() {
		return instrument;
	}
	public void setInstrument(String instrument) {
		this.instrument = instrument;
	}
	public String getSensor() {
		return sensor;
	}
	public void setSensor(String sensor) {
		this.sensor = sensor;
	}
	public String getParameterName() {
		return parameterName;
	}
	public void setParameterName(String parameterName) {
		this.parameterName = parameterName;
	}
	public String getS3Path() {
		return s3Path;
	}
	public void setS3Path(String s3Path) {
		this.s3Path = s3Path;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
}
