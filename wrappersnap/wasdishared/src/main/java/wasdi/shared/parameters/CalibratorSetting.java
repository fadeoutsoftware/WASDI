package wasdi.shared.parameters;

import wasdi.shared.utils.Constants;

/**
 * Created by s.adamo on 16/03/2017.
 */
public class CalibratorSetting implements ISetting{

    //@Parameter(description = "The list of source bands.", alias = "sourceBands", rasterDataNodeType = Band.class, label = "Source Band")
    private String[] sourceBandNames;

    //@Parameter(valueSet = {LATEST_AUX, PRODUCT_AUX, EXTERNAL_AUX}, description = "The auxiliary file", defaultValue = LATEST_AUX, label = "Auxiliary File")
    private String auxFile;

    //@Parameter(description = "The antenna elevation pattern gain auxiliary data file.", label = "External Aux File")
    private String externalAuxFile;

    //@Parameter(description = "Output image in complex", defaultValue = "false", label = "Save in complex")
    private Boolean outputImageInComplex;

    //@Parameter(description = "Output image scale", defaultValue = "false", label = "Scale in dB")
    private Boolean outputImageScaleInDb;

    //@Parameter(description = "Create gamma0 virtual band", defaultValue = "false", label = "Create gamma0 virtual band")
    private Boolean createGammaBand;

    //@Parameter(description = "Create beta0 virtual band", defaultValue = "false", label = "Create beta0 virtual band")
    private Boolean createBetaBand;

    // for Sentinel-1 mission only
    //@Parameter(description = "The list of polarisations", label = "Polarisations")
    private String[] selectedPolarisations;

    //@Parameter(description = "Output sigma0 band", defaultValue = "true", label = "Output sigma0 band")
    private Boolean outputSigmaBand;

    //@Parameter(description = "Output gamma0 band", defaultValue = "false", label = "Output gamma0 band")
    private Boolean outputGammaBand;

    //@Parameter(description = "Output beta0 band", defaultValue = "false", label = "Output beta0 band")
    private Boolean outputBetaBand;

    public CalibratorSetting(){


        setAuxFile(Constants.LATEST_AUX);
        setExternalAuxFile(null);
        setOutputImageInComplex(false);
        setOutputImageScaleInDb(false);
        setCreateGammaBand(false);
        setCreateBetaBand(false);
        setOutputSigmaBand(true);
        setOutputGammaBand(false);
        setOutputBetaBand(false);

    }

    public String[] getSourceBandNames() {
        return sourceBandNames;
    }

    public void setSourceBandNames(String[] sourceBandNames) {
        this.sourceBandNames = sourceBandNames;
    }

    public String getAuxFile() {
        return auxFile;
    }

    public void setAuxFile(String auxFile) {
        this.auxFile = auxFile;
    }

    public String getExternalAuxFile() {
        return externalAuxFile;
    }

    public void setExternalAuxFile(String externalAuxFile) {
        this.externalAuxFile = externalAuxFile;
    }

    public Boolean getOutputImageInComplex() {
        return outputImageInComplex;
    }

    public void setOutputImageInComplex(Boolean outputImageInComplex) {
        this.outputImageInComplex = outputImageInComplex;
    }

    public Boolean getOutputImageScaleInDb() {
        return outputImageScaleInDb;
    }

    public void setOutputImageScaleInDb(Boolean outputImageScaleInDb) {
        this.outputImageScaleInDb = outputImageScaleInDb;
    }

    public Boolean getCreateGammaBand() {
        return createGammaBand;
    }

    public void setCreateGammaBand(Boolean createGammaBand) {
        this.createGammaBand = createGammaBand;
    }

    public Boolean getCreateBetaBand() {
        return createBetaBand;
    }

    public void setCreateBetaBand(Boolean createBetaBand) {
        this.createBetaBand = createBetaBand;
    }

    public String[] getSelectedPolarisations() {
        return selectedPolarisations;
    }

    public void setSelectedPolarisations(String[] selectedPolarisations) {
        this.selectedPolarisations = selectedPolarisations;
    }

    public Boolean getOutputSigmaBand() {
        return outputSigmaBand;
    }

    public void setOutputSigmaBand(Boolean outputSigmaBand) {
        this.outputSigmaBand = outputSigmaBand;
    }

    public Boolean getOutputGammaBand() {
        return outputGammaBand;
    }

    public void setOutputGammaBand(Boolean outputGammaBand) {
        this.outputGammaBand = outputGammaBand;
    }

    public Boolean getOutputBetaBand() {
        return outputBetaBand;
    }

    public void setOutputBetaBand(Boolean outputBetaBand) {
        this.outputBetaBand = outputBetaBand;
    }
}
