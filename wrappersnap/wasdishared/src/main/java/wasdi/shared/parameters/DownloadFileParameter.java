package wasdi.shared.parameters;

/**
 * Created by s.adamo on 10/10/2016.
 */



public class DownloadFileParameter {

    /**
     * Download url
     */
    private String url;

    /**
     * SessionId
     */
    private String queue;

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
     * Is ObjectId of MongoDb
     */
    private String processObjId;

    /**
     * Open Search provider
     */
    private String openSearchProvider;

    private String boundingBox;

    public String getUrl() { return url; }

    public void setUrl(String sUrl) {
        this.url = sUrl;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String sQueue) {
        this.queue = sQueue;
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

    public String getProcessObjId() {
        return processObjId;
    }

    public void setProcessObjId(String processObjId) {
        this.processObjId = processObjId;
    }

    public String getOpenSearchProvider() {
        return openSearchProvider;
    }

    public void setOpenSearchProvider(String openSearchProvider) {
        this.openSearchProvider = openSearchProvider;
    }

    public String getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(String boundingBox) {
        this.boundingBox = boundingBox;
    }
}
