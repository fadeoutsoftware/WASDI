package wasdi.shared.viewmodels;

public class SuccessResponse {
	
	public SuccessResponse() {
		
	}
	public SuccessResponse(String sMessage) {
		this.message = sMessage;
	}

	private String message;

	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

}
