package wasdi.shared.parameters;

/**
 * Created by p.campanella on 31/10/2016.
 */
public class PublishBandParameter extends BaseParameter{

    private String fileName;
    private String queue;
    private String bandName;
    private String processObjId;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getBandName() {
        return bandName;
    }

    public void setBandName(String bandName) {
        this.bandName = bandName;
    }

    public String getProcessObjId() {
        return processObjId;
    }

    public void setProcessObjId(String processObjId) {
        this.processObjId = processObjId;
    }
}
