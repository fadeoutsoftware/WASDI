
public class RabbitMessage {
	private String sMessageCode;
	private String sMessageResult;
	private Object oPayload; 
	private String sWorkspaceId;
	
	public RabbitMessage(){
		sMessageCode = null;
		sMessageResult = null;
		oPayload = null;
		sWorkspaceId=null;
	}
	
	public Object getoPayload() {
		return oPayload;
	}

	public void setoPayload(Object oPayload) {
		this.oPayload = oPayload;
	}

	public String getsWorkspaceId() {
		return sWorkspaceId;
	}

	public void setsWorkspaceId(String sWorkspaceId) {
		this.sWorkspaceId = sWorkspaceId;
	}

	public RabbitMessage(String sMessageCode, String sMessageResult){
		this.sMessageCode = sMessageCode;
		this.sMessageResult = sMessageResult;
	}
	public String getsMessageCode() {
		return sMessageCode;
	}
	public void setsMessageCode(String sMessageCode) {
		this.sMessageCode = sMessageCode;
	}
	public String getsMessageResult() {
		return sMessageResult;
	}
	public void setsMessageResult(String sMessageResult) {
		this.sMessageResult = sMessageResult;
	}
	
	
	
	
}
