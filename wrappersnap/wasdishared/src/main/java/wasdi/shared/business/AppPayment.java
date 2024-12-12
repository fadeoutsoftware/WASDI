package wasdi.shared.business;

public class AppPayment {
	
	private String appPaymentId;
	
	private String name;
		
	private String userId;
	
	private String processorId;
	
	private String stripePaymentIntentId;
	
	private boolean buySuccess;
	
	private Double buyDate;
	
	private Double runDate;
	

	public String getAppPaymentId() {
		return appPaymentId;
	}

	public void setAppPaymentId(String appPaymentId) {
		this.appPaymentId = appPaymentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getProcessorId() {
		return processorId;
	}

	public void setProcessorId(String processorId) {
		this.processorId = processorId;
	}

	public String getStripePaymentIntentId() {
		return stripePaymentIntentId;
	}

	public void setStripePaymentIntentId(String stripePaymentIntentId) {
		this.stripePaymentIntentId = stripePaymentIntentId;
	}

	public boolean isBuySuccess() {
		return buySuccess;
	}

	public void setBuySuccess(boolean buySuccess) {
		this.buySuccess = buySuccess;
	}

	public Double getBuyDate() {
		return buyDate;
	}

	public void setBuyDate(Double buyDate) {
		this.buyDate = buyDate;
	}

	public Double getRunDate() {
		return runDate;
	}

	public void setRunDate(Double runDate) {
		this.runDate = runDate;
	}

	
}
