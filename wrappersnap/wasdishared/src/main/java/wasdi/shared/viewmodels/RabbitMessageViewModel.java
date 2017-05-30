package wasdi.shared.viewmodels;

/**
 * Created by p.campanella on 04/11/2016.
 */
public class RabbitMessageViewModel {
    private String messageCode;
    private String messageResult;
    private Object payload;
    private String workspaceId;

    public String getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(String messageCode) {
        this.messageCode = messageCode;
    }

    public String getMessageResult() {
        return messageResult;
    }

    public void setMessageResult(String messageResult) {
        this.messageResult = messageResult;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

}
