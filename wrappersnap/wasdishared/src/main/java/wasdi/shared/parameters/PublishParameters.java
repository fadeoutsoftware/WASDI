package wasdi.shared.parameters;

/**
 * Created by p.campanella on 14/10/2016.
 */
public class PublishParameters {

    private String fileName;
    private String geoserverStore;
    private String queue;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String sFileName) {
        this.fileName = sFileName;
    }

    public String getStore() {
        return geoserverStore;
    }

    public void setStore(String sStore) {
        this.geoserverStore = sStore;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }
}
