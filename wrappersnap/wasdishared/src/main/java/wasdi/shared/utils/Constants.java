package wasdi.shared.utils;

/**
 * Created by s.adamo on 08/02/2017.
 */
public class Constants {
	private Constants() {
		// / private constructor to hide the public implicit one 
	}

    //Resampling Factor
    public static final String NEAREST_NEIGHBOUR_NAME = "NEAREST_NEIGHBOUR";
    public static final String BILINEAR_INTERPOLATION_NAME = "BILINEAR_INTERPOLATION";
    public static final String CUBIC_CONVOLUTION_NAME = "CUBIC_CONVOLUTION";
    public static final String BISINC_5_POINT_INTERPOLATION_NAME = "BISINC_5_POINT_INTERPOLATION";
    public static final String BISINC_11_POINT_INTERPOLATION_NAME = "BISINC_11_POINT_INTERPOLATION";
    public static final String BISINC_21_POINT_INTERPOLATION_NAME = "BISINC_21_POINT_INTERPOLATION";
    public static final String BICUBIC_INTERPOLATION_NAME = "BICUBIC_INTERPOLATION";

    public static final String USE_PROJECTED_INCIDENCE_ANGLE_FROM_DEM = "Use projected local incidence angle from DEM";
    public static final String USE_LOCAL_INCIDENCE_ANGLE_FROM_DEM = "Use local incidence angle from DEM";
    public static final String USE_INCIDENCE_ANGLE_FROM_ELLIPSOID = "Use incidence angle from Ellipsoid";

    //Calibration
    public static final String PRODUCT_AUX = "Product Auxiliary File";
    public static final String LATEST_AUX = "Latest Auxiliary File";
    public static final String EXTERNAL_AUX = "External Auxiliary File";

    //Filter
    public static final String NONE = "None";
    public static final String BOXCAR_SPECKLE_FILTER = "Boxcar";
    public static final String MEDIAN_SPECKLE_FILTER = "Median";
    public static final String FROST_SPECKLE_FILTER = "Frost";
    public static final String GAMMA_MAP_SPECKLE_FILTER = "Gamma Map";
    public static final String LEE_SPECKLE_FILTER = "Lee";
    public static final String LEE_REFINED_FILTER = "Refined Lee";
    public static final String LEE_SIGMA_FILTER = "Lee Sigma";
    public static final String IDAN_FILTER = "IDAN";
    public static final String MEAN_SPECKLE_FILTER = "Mean";
    public static final String NUM_LOOKS_1 = "1";
    public static final String NUM_LOOKS_2 = "2";
    public static final String NUM_LOOKS_3 = "3";
    public static final String NUM_LOOKS_4 = "4";
    public static final String SIZE_3x3 = "3x3";
    public static final String SIZE_5x5 = "5x5";
    public static final String SIZE_7x7 = "7x7";
    public static final String SIZE_9x9 = "9x9";
    public static final String SIZE_11x11 = "11x11";
    public static final String SIZE_13x13 = "13x13";
    public static final String SIZE_15x15 = "15x15";
    public static final String SIZE_17x17 = "17x17";
    public static final String SIGMA_50_PERCENT = "0.5";
    public static final String SIGMA_60_PERCENT = "0.6";
    public static final String SIGMA_70_PERCENT = "0.7";
    public static final String SIGMA_80_PERCENT = "0.8";
    public static final String SIGMA_90_PERCENT = "0.9";

}
