package wasdi.shared.viewmodels;

/**
 * View Model used to exchange messages with rabbit with all the WASDI Components
 * Mainly used by the launcher to talk with the client.
 * 
 * Each Rabbit Message has:
 * 		.messageCode: type of message, has values of LauncherOperations enum
 * 		.messageResult: OK or KO
 * 		.payload: info associated to the message
 * 		.workspaceId: workspace interested to this message
 * 
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
