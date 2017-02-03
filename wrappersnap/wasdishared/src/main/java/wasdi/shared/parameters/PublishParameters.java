package wasdi.shared.parameters;

/**
 * Created by p.campanella on 14/10/2016.
 */
public class PublishParameters {

    private String fileName;
    private String queue;
    private String userId;
    private String workspace;
    private String exchange;
    private String processObjId;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String sFileName) {
        this.fileName = sFileName;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getProcessObjId() {
        return processObjId;
    }

    public void setProcessObjId(String processObjId) {
        this.processObjId = processObjId;
    }
}
