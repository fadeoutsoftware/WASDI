package wasdi.shared.viewmodels.processors;

public class AppPaymentViewModel {
	
	public AppPaymentViewModel(String paymentName, String processorId) {
		super();
		this.paymentName = paymentName;
		this.processorId = processorId;
	}

	private String paymentName;
	
	private String processorId;

	public String getPaymentName() {
		return paymentName;
	}

	public void setPaymentName(String paymentName) {
		this.paymentName = paymentName;
	}

	public String getProcessorId() {
		return processorId;
	}

	public void setProcessorId(String processorId) {
		this.processorId = processorId;
	}

	
}
