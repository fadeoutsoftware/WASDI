package wasdi.shared.parameters;

/**
 * Created by p.campanella on 06/03/2017.
 */
public class RasterGeometricResampleParameter {

    private String sourceProductName;

    private String destinationProductName;

    private String workspace;

    private String bandName;

    private String userId;

    private String exchange;

    private String processObjId;

    public String getBandName() {
        return bandName;
    }

    public void setBandName(String bandName) {
        this.bandName = bandName;
    }

    public String getSourceProductName() {
        return sourceProductName;
    }

    public void setSourceProductName(String sourceProductName) {
        this.sourceProductName = sourceProductName;
    }

    public String getDestinationProductName() {
        return destinationProductName;
    }

    public void setDestinationProductName(String destinationProductName) {
        this.destinationProductName = destinationProductName;
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
}
