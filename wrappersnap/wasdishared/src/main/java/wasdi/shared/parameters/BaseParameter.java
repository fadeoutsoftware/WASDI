package wasdi.shared.parameters;

/**
 * Created by s.adamo on 16/03/2017.
 */
public class BaseParameter {

    /**
     * UserId
     */
    private String userId;

    /**
     * Workspace
     */
    private String workspace;

    /**
     * Is workspace
     */
    private String exchange;
    
    /**
     * Is ObjectId of the process
     */
    private String processObjId;

    public String getProcessObjId() {
        return processObjId;
    }

    public void setProcessObjId(String processObjId) {
        this.processObjId = processObjId;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }
}
