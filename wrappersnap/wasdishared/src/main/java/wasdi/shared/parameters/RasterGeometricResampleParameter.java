package wasdi.shared.parameters;

/**
 * Created by p.campanella on 06/03/2017.
 */
public class RasterGeometricResampleParameter extends BaseParameter{

    private String sourceProductName;

    private String destinationProductName;

    private String bandName;

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
}
