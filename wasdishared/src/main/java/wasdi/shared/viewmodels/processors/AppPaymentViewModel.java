package wasdi.shared.viewmodels.processors;

public class AppPaymentViewModel {

	private String appPaymentId;
	
	private String paymentName;
				
	private String userId;
	
	private String processorId;
	
	private boolean buySuccess;
	
	private String buyDate;
	
	private String runDate;
	
	
	public AppPaymentViewModel() {
		super();
		this.appPaymentId = "";
		this.paymentName = "";
		this.userId = "";
		this.processorId = "";
		this.buySuccess = false;
		this.buyDate = "";
		this.runDate = "";
	}
	
	public AppPaymentViewModel(String appPaymentId, String paymentName, String userId, String processorId,
			boolean buySuccess, String buyDate, String runDate) {
		super();
		this.appPaymentId = appPaymentId;
		this.paymentName = paymentName;
		this.userId = userId;
		this.processorId = processorId;
		this.buySuccess = buySuccess;
		this.buyDate = buyDate;
		this.runDate = runDate;
	}

	public String getAppPaymentId() {
		return appPaymentId;
	}

	public void setAppPaymentId(String appPaymentId) {
		this.appPaymentId = appPaymentId;
	}

	public String getPaymentName() {
		return paymentName;
	}

	public void setPaymentName(String paymentName) {
		this.paymentName = paymentName;
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

	public boolean isBuySuccess() {
		return buySuccess;
	}

	public void setBuySuccess(boolean buySuccess) {
		this.buySuccess = buySuccess;
	}

	public String getBuyDate() {
		return buyDate;
	}

	public void setBuyDate(String buyDate) {
		this.buyDate = buyDate;
	}

	public String getRunDate() {
		return runDate;
	}

	public void setRunDate(String runDate) {
		this.runDate = runDate;
	}
	
	

	
}
