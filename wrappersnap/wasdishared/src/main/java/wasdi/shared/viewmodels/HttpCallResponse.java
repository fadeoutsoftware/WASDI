package wasdi.shared.viewmodels;

public class HttpCallResponse {

	private Integer responseCode = -1;
	private String responseBody = "";
	public Integer getResponseCode() {
		return responseCode;
	}
	public void setResponseCode(Integer responseCode) {
		this.responseCode = responseCode;
	}
	public String getResponseBody() {
		return responseBody;
	}
	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
	}

}
