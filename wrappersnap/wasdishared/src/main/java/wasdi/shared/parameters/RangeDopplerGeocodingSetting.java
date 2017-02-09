package wasdi.shared.parameters;

import wasdi.shared.utils.Constants;

import java.io.File;

/**
 * Created by s.adamo on 08/02/2017.
 */
public class RangeDopplerGeocodingSetting {

    //@Parameter(description = "The list of source bands.", alias = "sourceBands",rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames = null;

    //@Parameter(description = "The digital elevation model.",defaultValue = "SRTM 3Sec", label = "Digital Elevation Model")
    private String demName = "SRTM 3Sec";

    //@Parameter(label = "External DEM")
    private String externalDEMFile = null;

    //@Parameter(label = "External DEM No Data Value", defaultValue = "0")
    private double externalDEMNoDataValue = 0;

    //@Parameter(label = "External DEM Apply EGM", defaultValue = "true")
    private Boolean externalDEMApplyEGM = true;

    //@Parameter(defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME, label = "DEM Resampling Method")
    private String demResamplingMethod = Constants.BILINEAR_INTERPOLATION_NAME;

    //@Parameter(defaultValue = ResamplingFactory.BILINEAR_INTERPOLATION_NAME, label = "Image Resampling Method")
    private String imgResamplingMethod = Constants.BILINEAR_INTERPOLATION_NAME;

    //@Parameter(description = "The pixel spacing in meters", defaultValue = "0", label = "Pixel Spacing (m)")
    private double pixelSpacingInMeter = 0;

    //@Parameter(description = "The pixel spacing in degrees", defaultValue = "0", label = "Pixel Spacing (deg)")
    private double pixelSpacingInDegree = 0;

    //@Parameter(description = "The coordinate reference system in well known text format", defaultValue = "WGS84(DD)")
    private String mapProjection = "WGS84(DD)";

    //@Parameter(defaultValue = "true", label = "Mask out areas with no elevation", description = "Mask the sea with no data value (faster)")
    private boolean nodataValueAtSea = true;

    //@Parameter(defaultValue = "false", label = "Save DEM as band")
    private boolean saveDEM = false;

    //@Parameter(defaultValue = "false", label = "Save latitude and longitude as band")
    private boolean saveLatLon = false;

    //@Parameter(defaultValue = "false", label = "Save incidence angle from ellipsoid as band")
    private boolean saveIncidenceAngleFromEllipsoid = false;

    //@Parameter(defaultValue = "false", label = "Save local incidence angle as band")
    private boolean saveLocalIncidenceAngle = false;

    //@Parameter(defaultValue = "false", label = "Save projected local incidence angle as band")
    private boolean saveProjectedLocalIncidenceAngle = false;

    //@Parameter(defaultValue = "true", label = "Save selected source band")
    private boolean saveSelectedSourceBand = true;

    //@Parameter(defaultValue = "false", label = "Output complex data")
    private boolean outputComplex = false;

    //@Parameter(defaultValue = "false", label = "Apply radiometric normalization")
    private boolean applyRadiometricNormalization = false;

    //@Parameter(defaultValue = "false", label = "Save Sigma0 as a band")
    private boolean saveSigmaNought = false;

    //@Parameter(defaultValue = "false", label = "Save Gamma0 as a band")
    private boolean saveGammaNought = false;

    //@Parameter(defaultValue = "false", label = "Save Beta0 as a band")
    private boolean saveBetaNought = false;

    //@Parameter(valueSet = {Constants.USE_INCIDENCE_ANGLE_FROM_ELLIPSOID, Constants.USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM,
    //        Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM},
    //        defaultValue = Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM, label = "")
    private String incidenceAngleForSigma0 = Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM;

    //@Parameter(valueSet = {Constants.USE_INCIDENCE_ANGLE_FROM_ELLIPSOID, Constants.USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM,
    //        Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM},
    //        defaultValue = Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM, label = "")
    private String incidenceAngleForGamma0 = Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM;

    //@Parameter(valueSet = {CalibrationOp.LATEST_AUX, CalibrationOp.PRODUCT_AUX, CalibrationOp.EXTERNAL_AUX},
    //        description = "The auxiliary file", defaultValue = CalibrationOp.LATEST_AUX, label = "Auxiliary File")
    private String auxFile = Constants.LATEST_AUX;

    //@Parameter(description = "The antenne elevation pattern gain auxiliary data file.", label = "External Aux File")
    private String externalAuxFile = null;


    public RangeDopplerGeocodingSetting(){

        //default
        sourceBandNames = null;
        demName = "SRTM 3Sec";
        externalDEMFile = null;
        externalDEMNoDataValue = 0;
        externalDEMApplyEGM = true;
        demResamplingMethod = Constants.BILINEAR_INTERPOLATION_NAME;
        imgResamplingMethod = Constants.BILINEAR_INTERPOLATION_NAME;
        pixelSpacingInMeter = 0;
        pixelSpacingInDegree = 0;
        mapProjection = "WGS84(DD)";
        nodataValueAtSea = true;
        saveDEM = false;
        saveLatLon = false;
        saveIncidenceAngleFromEllipsoid = false;
        saveLocalIncidenceAngle = false;
        saveProjectedLocalIncidenceAngle = false;
        saveSelectedSourceBand = true;
        outputComplex = false;
        applyRadiometricNormalization = false;
        saveSigmaNought = false;
        saveGammaNought = false;
        saveBetaNought = false;
        incidenceAngleForSigma0 = Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM;
        incidenceAngleForGamma0 = Constants.USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM;
        auxFile = Constants.LATEST_AUX;
        externalAuxFile = null;
    }

    public String[] getSourceBandNames() {
        return sourceBandNames;
    }

    public void setSourceBandNames(String[] sourceBandNames) {
        this.sourceBandNames = sourceBandNames;
    }

    public String getDemName() {
        return demName;
    }

    public void setDemName(String demName) {
        this.demName = demName;
    }

    public String getExternalDEMFile() {
        return externalDEMFile;
    }

    public void setExternalDEMFile(String externalDEMFile) {
        this.externalDEMFile = externalDEMFile;
    }

    public double getExternalDEMNoDataValue() {
        return externalDEMNoDataValue;
    }

    public void setExternalDEMNoDataValue(double externalDEMNoDataValue) {
        this.externalDEMNoDataValue = externalDEMNoDataValue;
    }

    public Boolean getExternalDEMApplyEGM() {
        return externalDEMApplyEGM;
    }

    public void setExternalDEMApplyEGM(Boolean externalDEMApplyEGM) {
        this.externalDEMApplyEGM = externalDEMApplyEGM;
    }

    public String getDemResamplingMethod() {
        return demResamplingMethod;
    }

    public void setDemResamplingMethod(String demResamplingMethod) {
        this.demResamplingMethod = demResamplingMethod;
    }

    public String getImgResamplingMethod() {
        return imgResamplingMethod;
    }

    public void setImgResamplingMethod(String imgResamplingMethod) {
        this.imgResamplingMethod = imgResamplingMethod;
    }

    public double getPixelSpacingInMeter() {
        return pixelSpacingInMeter;
    }

    public void setPixelSpacingInMeter(double pixelSpacingInMeter) {
        this.pixelSpacingInMeter = pixelSpacingInMeter;
    }

    public double getPixelSpacingInDegree() {
        return pixelSpacingInDegree;
    }

    public void setPixelSpacingInDegree(double pixelSpacingInDegree) {
        this.pixelSpacingInDegree = pixelSpacingInDegree;
    }

    public String getMapProjection() {
        return mapProjection;
    }

    public void setMapProjection(String mapProjection) {
        this.mapProjection = mapProjection;
    }

    public boolean isNodataValueAtSea() {
        return nodataValueAtSea;
    }

    public void setNodataValueAtSea(boolean nodataValueAtSea) {
        this.nodataValueAtSea = nodataValueAtSea;
    }

    public boolean isSaveDEM() {
        return saveDEM;
    }

    public void setSaveDEM(boolean saveDEM) {
        this.saveDEM = saveDEM;
    }

    public boolean isSaveLatLon() {
        return saveLatLon;
    }

    public void setSaveLatLon(boolean saveLatLon) {
        this.saveLatLon = saveLatLon;
    }

    public boolean isSaveIncidenceAngleFromEllipsoid() {
        return saveIncidenceAngleFromEllipsoid;
    }

    public void setSaveIncidenceAngleFromEllipsoid(boolean saveIncidenceAngleFromEllipsoid) {
        this.saveIncidenceAngleFromEllipsoid = saveIncidenceAngleFromEllipsoid;
    }

    public boolean isSaveLocalIncidenceAngle() {
        return saveLocalIncidenceAngle;
    }

    public void setSaveLocalIncidenceAngle(boolean saveLocalIncidenceAngle) {
        this.saveLocalIncidenceAngle = saveLocalIncidenceAngle;
    }

    public boolean isSaveProjectedLocalIncidenceAngle() {
        return saveProjectedLocalIncidenceAngle;
    }

    public void setSaveProjectedLocalIncidenceAngle(boolean saveProjectedLocalIncidenceAngle) {
        this.saveProjectedLocalIncidenceAngle = saveProjectedLocalIncidenceAngle;
    }

    public boolean isSaveSelectedSourceBand() {
        return saveSelectedSourceBand;
    }

    public void setSaveSelectedSourceBand(boolean saveSelectedSourceBand) {
        this.saveSelectedSourceBand = saveSelectedSourceBand;
    }

    public boolean isOutputComplex() {
        return outputComplex;
    }

    public void setOutputComplex(boolean outputComplex) {
        this.outputComplex = outputComplex;
    }

    public boolean isApplyRadiometricNormalization() {
        return applyRadiometricNormalization;
    }

    public void setApplyRadiometricNormalization(boolean applyRadiometricNormalization) {
        this.applyRadiometricNormalization = applyRadiometricNormalization;
    }

    public boolean isSaveSigmaNought() {
        return saveSigmaNought;
    }

    public void setSaveSigmaNought(boolean saveSigmaNought) {
        this.saveSigmaNought = saveSigmaNought;
    }

    public boolean isSaveGammaNought() {
        return saveGammaNought;
    }

    public void setSaveGammaNought(boolean saveGammaNought) {
        this.saveGammaNought = saveGammaNought;
    }

    public boolean isSaveBetaNought() {
        return saveBetaNought;
    }

    public void setSaveBetaNought(boolean saveBetaNought) {
        this.saveBetaNought = saveBetaNought;
    }

    public String getIncidenceAngleForSigma0() {
        return incidenceAngleForSigma0;
    }

    public void setIncidenceAngleForSigma0(String incidenceAngleForSigma0) {
        this.incidenceAngleForSigma0 = incidenceAngleForSigma0;
    }

    public String getIncidenceAngleForGamma0() {
        return incidenceAngleForGamma0;
    }

    public void setIncidenceAngleForGamma0(String incidenceAngleForGamma0) {
        this.incidenceAngleForGamma0 = incidenceAngleForGamma0;
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
}
