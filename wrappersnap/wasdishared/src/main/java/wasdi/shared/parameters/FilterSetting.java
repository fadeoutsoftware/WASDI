package wasdi.shared.parameters;

import wasdi.shared.utils.Constants;

/**
 * Created by s.adamo on 17/03/2017.
 */
public class FilterSetting implements ISetting {

    //@Parameter(description = "The list of source bands.", alias = "sourceBands", rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    //@Parameter(valueSet = {NONE, BOXCAR_SPECKLE_FILTER, MEDIAN_SPECKLE_FILTER, FROST_SPECKLE_FILTER,
    //        GAMMA_MAP_SPECKLE_FILTER, LEE_SPECKLE_FILTER, LEE_REFINED_FILTER, LEE_SIGMA_FILTER, IDAN_FILTER},
    //        defaultValue = LEE_SIGMA_FILTER,
    //        label = "Filter")
    private String filter = Constants.LEE_SIGMA_FILTER;

    //@Parameter(description = "The kernel x dimension", interval = "(1, 100]", defaultValue = "3", label = "Size X")
    private int filterSizeX = 3;

    //@Parameter(description = "The kernel y dimension", interval = "(1, 100]", defaultValue = "3", label = "Size Y")
    private int filterSizeY = 3;

    //@Parameter(description = "The damping factor (Frost filter only)", interval = "(0, 100]", defaultValue = "2",
    //        label = "Frost Damping Factor")
    private int dampingFactor = 2;

//    @Parameter(description = "The edge threshold (Refined Lee filter only)", interval = "(0, *)", defaultValue = "5000",
//            label = "Edge detection threshold")
//    private double edgeThreshold = 5000.0;

    //@Parameter(defaultValue = "false", label = "Estimate Eqivalent Number of Looks")
    private boolean estimateENL = true;

    //@Parameter(description = "The number of looks", interval = "(0, *)", defaultValue = "1.0",
    //        label = "Number of looks")
    private double enl = 1.0;

    //@Parameter(valueSet = {NUM_LOOKS_1, NUM_LOOKS_2, NUM_LOOKS_3, NUM_LOOKS_4},
    //        defaultValue = NUM_LOOKS_1, label = "Number of Looks")
    private String numLooksStr = Constants.NUM_LOOKS_1;

    //@Parameter(valueSet = {FilterWindow.SIZE_5x5, FilterWindow.SIZE_7x7, FilterWindow.SIZE_9x9, FilterWindow.SIZE_11x11,
    //        FilterWindow.SIZE_13x13, FilterWindow.SIZE_15x15, FilterWindow.SIZE_17x17},
    //        defaultValue = FilterWindow.SIZE_7x7, label = "Window Size")
    private String windowSize = Constants.SIZE_7x7; // window size for all filters

    //@Parameter(valueSet = {FilterWindow.SIZE_3x3, FilterWindow.SIZE_5x5}, defaultValue = FilterWindow.SIZE_3x3,
    //        label = "Point target window Size")
    private String targetWindowSizeStr = Constants.SIZE_3x3; // window size for point target determination in Lee sigma

    //@Parameter(valueSet = {SIGMA_50_PERCENT, SIGMA_60_PERCENT, SIGMA_70_PERCENT, SIGMA_80_PERCENT, SIGMA_90_PERCENT},
    //        defaultValue = SIGMA_90_PERCENT, label = "Point target window Size")
    private String sigmaStr = Constants.SIGMA_90_PERCENT; // sigma value in Lee sigma

    //@Parameter(description = "The Adaptive Neighbourhood size", interval = "(1, 200]", defaultValue = "50",
    //        label = "Adaptive Neighbourhood Size")
    private int anSize = 50;

    public FilterSetting(){

        setFilter(Constants.LEE_SIGMA_FILTER);
        setFilterSizeX(3);
        setFilterSizeY(3);
        setDampingFactor(2);
        setEstimateENL(true);
        setEnl(1.0);
        setNumLooksStr(Constants.NUM_LOOKS_1);
        setWindowSize(Constants.SIZE_7x7); // window size for all filters
        setTargetWindowSizeStr(Constants.SIZE_3x3); // window size for point target determination in Lee sigma
        setSigmaStr(Constants.SIGMA_90_PERCENT); // sigma value in Lee sigma
        setAnSize(50);
    }

    public String[] getSourceBandNames() {
        return sourceBandNames;
    }

    public void setSourceBandNames(String[] sourceBandNames) {
        this.sourceBandNames = sourceBandNames;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public int getFilterSizeX() {
        return filterSizeX;
    }

    public void setFilterSizeX(int filterSizeX) {
        this.filterSizeX = filterSizeX;
    }

    public int getFilterSizeY() {
        return filterSizeY;
    }

    public void setFilterSizeY(int filterSizeY) {
        this.filterSizeY = filterSizeY;
    }

    public int getDampingFactor() {
        return dampingFactor;
    }

    public void setDampingFactor(int dampingFactor) {
        this.dampingFactor = dampingFactor;
    }

    public boolean isEstimateENL() {
        return estimateENL;
    }

    public void setEstimateENL(boolean estimateENL) {
        this.estimateENL = estimateENL;
    }

    public double getEnl() {
        return enl;
    }

    public void setEnl(double enl) {
        this.enl = enl;
    }

    public String getNumLooksStr() {
        return numLooksStr;
    }

    public void setNumLooksStr(String numLooksStr) {
        this.numLooksStr = numLooksStr;
    }

    public String getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(String windowSize) {
        this.windowSize = windowSize;
    }

    public String getTargetWindowSizeStr() {
        return targetWindowSizeStr;
    }

    public void setTargetWindowSizeStr(String targetWindowSizeStr) {
        this.targetWindowSizeStr = targetWindowSizeStr;
    }

    public String getSigmaStr() {
        return sigmaStr;
    }

    public void setSigmaStr(String sigmaStr) {
        this.sigmaStr = sigmaStr;
    }

    public int getAnSize() {
        return anSize;
    }

    public void setAnSize(int anSize) {
        this.anSize = anSize;
    }
}
