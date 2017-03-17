package wasdi.shared.parameters;

/**
 * Created by s.adamo on 16/03/2017.
 */
public class MultilookingSetting implements ISetting{

    //@Parameter(description = "The list of source bands.", alias = "sourceBands",rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    //@Parameter(description = "The user defined number of range looks", interval = "[1, *)", defaultValue = "1", label = "Number of Range Looks")
    private int nRgLooks;

    //@Parameter(description = "The user defined number of azimuth looks", interval = "[1, *)", defaultValue = "1",label = "Number of Azimuth Looks")
    private int nAzLooks;

    //@Parameter(description = "For complex product output intensity or i and q", defaultValue = "false",label = "Output Intensity")
    private Boolean outputIntensity;

    //@Parameter(description = "Use ground square pixel", defaultValue = "true", label = "GR Square Pixel")
    private Boolean grSquarePixel;

    public MultilookingSetting(){
        setnRgLooks(1);
        setnAzLooks(1);
        setOutputIntensity(false);
        setGrSquarePixel(true);
    }

    public String[] getSourceBandNames() {
        return sourceBandNames;
    }

    public void setSourceBandNames(String[] sourceBandNames) {
        this.sourceBandNames = sourceBandNames;
    }

    public int getnRgLooks() {
        return nRgLooks;
    }

    public void setnRgLooks(int nRgLooks) {
        this.nRgLooks = nRgLooks;
    }

    public int getnAzLooks() {
        return nAzLooks;
    }

    public void setnAzLooks(int nAzLooks) {
        this.nAzLooks = nAzLooks;
    }

    public Boolean getOutputIntensity() {
        return outputIntensity;
    }

    public void setOutputIntensity(Boolean outputIntensity) {
        this.outputIntensity = outputIntensity;
    }

    public Boolean getGrSquarePixel() {
        return grSquarePixel;
    }

    public void setGrSquarePixel(Boolean grSquarePixel) {
        this.grSquarePixel = grSquarePixel;
    }
}
