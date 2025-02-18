package wasdi.shared.business;

public class CreditsPackage {
	
	private String creditPackageId;
	private String name;
	private String description;
	private String type;
	private Double buyDate;
	private String userId;
	private boolean buySuccess;
	private Double creditsRemaining;
	private Double lastUpdate;
	
	
	
	
	public CreditsPackage(String creditPackageId, String name, String description, String type, 
			Double buyDate, String userId,
			boolean buySuccess, Double creditsRemaining, double lastUpdate) {
		super();
		this.creditPackageId = creditPackageId;
		this.name = name;
		this.description = description;
		this.type = type;
		this.buyDate = buyDate;
		this.userId = userId;
		this.buySuccess = buySuccess;
		this.creditsRemaining = creditsRemaining;
		this.lastUpdate = lastUpdate;
	}

	public String getCreditPackageId() {
		return creditPackageId;
	}
	
	public void setCreditPackageId(String creditPackageId) {
		this.creditPackageId = creditPackageId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public Double getBuyDate() {
		return buyDate;
	}
	
	public void setBuyDate(Double buyDate) {
		this.buyDate = buyDate;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public boolean isBuySuccess() {
		return buySuccess;
	}
	
	public void setBuySuccess(boolean buySuccess) {
		this.buySuccess = buySuccess;
	}
	
	public Double getCreditsRemaining() {
		return creditsRemaining;
	}
	
	public void setCreditsRemaining(Double creditsRemaining) {
		this.creditsRemaining = creditsRemaining;
	}
	
	public Double getLastUpdate() {
		return lastUpdate;
	}
	
	public void setLastUpdate(Double lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	
}
