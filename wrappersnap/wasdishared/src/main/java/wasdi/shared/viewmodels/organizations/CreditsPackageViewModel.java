package wasdi.shared.viewmodels.organizations;

public class CreditsPackageViewModel {
	
	private String creditPackageId;
	private String name;
	private String description;
	private String type;
	private Double buyDate;
	private String userId;
	private boolean buySuccess;
	private double creditsRemaining;
	private double lastUpdate;
	
	
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
	
	public double getCreditsRemaining() {
		return creditsRemaining;
	}
	
	public void setCreditsRemaining(double creditsRemaining) {
		this.creditsRemaining = creditsRemaining;
	}
	
	public double getLastUpdate() {
		return lastUpdate;
	}
	
	public void setLastUpdate(double lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

}
