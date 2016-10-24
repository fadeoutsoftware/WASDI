package wasdi.shared.parameters;

/**
 * Created by s.adamo on 10/10/2016.
 */



public class DownloadFileParameter {

    private String url;
    private String queue;
    private String userId;
    private String workspace;

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
}
