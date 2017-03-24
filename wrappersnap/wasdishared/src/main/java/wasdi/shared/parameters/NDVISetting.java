package wasdi.shared.parameters;

/**
 * Created by s.adamo on 17/03/2017.
 */
public class NDVISetting implements ISetting{

    //@Parameter(label = "Red factor", defaultValue = "1.0F", description = "The value of the red source band is multiplied by this value.")
    private float redFactor;

    //@Parameter(label = "NIR factor", defaultValue = "1.0F", description = "The value of the NIR source band is multiplied by this value.")
    private float nirFactor;

    //@Parameter(label = "Red source band",
    //        description = "The red band for the TNDVI computation. If not provided, the " +
    //                "operator will try to find the best fitting band.",
    //        rasterDataNodeType = Band.class)
    //@BandParameter(minWavelength = 600, maxWavelength = 650)
    private String redSourceBand;

    //@Parameter(label = "NIR source band",
    //        description = "The near-infrared band for the TNDVI computation. If not provided," +
    //                " the operator will try to find the best fitting band.",
    //        rasterDataNodeType = Band.class)
    //@BandParameter(minWavelength = 800, maxWavelength = 900)
    private String nirSourceBand;

    public NDVISetting()
    {
        setRedFactor(1.0F);
        setNirFactor(1.0F);
        setRedSourceBand(null);
        setNirSourceBand(null);
    }

    public float getRedFactor() {
        return redFactor;
    }

    public void setRedFactor(float redFactor) {
        this.redFactor = redFactor;
    }

    public float getNirFactor() {
        return nirFactor;
    }

    public void setNirFactor(float nirFactor) {
        this.nirFactor = nirFactor;
    }

    public String getRedSourceBand() {
        return redSourceBand;
    }

    public void setRedSourceBand(String redSourceBand) {
        this.redSourceBand = redSourceBand;
    }

    public String getNirSourceBand() {
        return nirSourceBand;
    }

    public void setNirSourceBand(String nirSourceBand) {
        this.nirSourceBand = nirSourceBand;
    }
}
