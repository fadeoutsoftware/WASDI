package wasdi.shared.parameters;

/**
 * Created by p.campanella on 14/10/2016.
 */
public class PublishParameters extends BaseParameter{

    private String fileName;
    private String queue;

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

}
