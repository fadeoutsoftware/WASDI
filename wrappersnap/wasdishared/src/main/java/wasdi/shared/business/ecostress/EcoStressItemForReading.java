package wasdi.shared.business.ecostress;

public class EcoStressItemForReading {
	
	public EcoStressItemForReading() {
	}

	private String fileName;
	private String dayNightFlag;
	private Double beginningDate;
	private Double endingDate;
	private EcoStressLocation location;
	private String s3Path;
	private String url;
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
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
	
	public EcoStressLocation getLocation() {
		return location;
	}
	
	public void setLocation(EcoStressLocation location) {
		this.location = location;
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
