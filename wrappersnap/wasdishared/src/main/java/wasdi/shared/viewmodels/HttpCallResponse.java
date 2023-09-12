package wasdi.shared.viewmodels;

public class HttpCallResponse {

	private Integer m_iResponseCode = -1;
	private String m_sResponseBody = "";
	
	public Integer getResponseCode() {
		return m_iResponseCode;
	}
	public void setResponseCode(Integer responseCode) {
		this.m_iResponseCode = responseCode;
	}
	public String getResponseBody() {
		return m_sResponseBody;
	}
	public void setResponseBody(String responseBody) {
		this.m_sResponseBody = responseBody;
	}

}
