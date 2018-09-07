package wasdi.snapopearations;

import java.io.File;

import org.esa.s1tbx.sar.gpf.geometric.RangeDopplerGeocodingOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.dem.dataio.DEMFactory;
import org.geotools.referencing.wkt.UnformattableObjectException;

import wasdi.LauncherMain;
import wasdi.shared.parameters.ISetting;
import wasdi.shared.parameters.RangeDopplerGeocodingSetting;
import wasdi.shared.utils.Utils;

/**
 * Terrain Correction Snap Operation Parameter Wrapper
 * Created by s.adamo on 24/05/2016.
 */
public class TerrainCorrection extends BaseOperation{

    public TerrainCorrection()
    {
        super(new RangeDopplerGeocodingOp.Spi());
    }

    @Override
    public void FillSettings(Operator oOperator, ISetting oSetting) {

        //set default value
        oOperator.setParameterDefaultValues();
        if (oSetting == null) return;
        if (!(oSetting instanceof RangeDopplerGeocodingSetting))return;

        RangeDopplerGeocodingSetting oRangeDopplerGeocodingSetting = (RangeDopplerGeocodingSetting) oSetting;

        // source band (FOR THE MOMENT WE SUPPORT ONLY ONE BAND AT TIME)
        if (oRangeDopplerGeocodingSetting.getSourceBandNames() != null && oRangeDopplerGeocodingSetting.getSourceBandNames().length > 0) {
            LauncherMain.s_oLogger.debug("TerrainCorrection.FillRangeDopplerGeocodingSettings: setting source band ");
            oOperator.setParameter("sourceBands", oRangeDopplerGeocodingSetting.getSourceBandNames());

            final String properDEMName = (DEMFactory.getProperDEMName(oRangeDopplerGeocodingSetting.getDemName()));
            oOperator.setParameter("demName", properDEMName);
            oOperator.setParameter("demResamplingMethod", oRangeDopplerGeocodingSetting.getDemResamplingMethod());
            oOperator.setParameter("imgResamplingMethod", oRangeDopplerGeocodingSetting.getImgResamplingMethod());
            oOperator.setParameter("incidenceAngleForGamma0", oRangeDopplerGeocodingSetting.getIncidenceAngleForGamma0());
            oOperator.setParameter("incidenceAngleForSigma0", oRangeDopplerGeocodingSetting.getIncidenceAngleForSigma0());
            if (Utils.isNullOrEmpty(String.valueOf(oRangeDopplerGeocodingSetting.getPixelSpacingInMeter()))) {
                oOperator.setParameter("pixelSpacingInMeter", 0.0);
            } else {
                oOperator.setParameter("pixelSpacingInMeter", oRangeDopplerGeocodingSetting.getPixelSpacingInMeter());
            }

            if (Utils.isNullOrEmpty(String.valueOf(oRangeDopplerGeocodingSetting.getPixelSpacingInDegree()))) {
                oOperator.setParameter("pixelSpacingInDegree", 0.0);
            } else {
                oOperator.setParameter("pixelSpacingInDegree", oRangeDopplerGeocodingSetting.getPixelSpacingInDegree());
            }

            if(properDEMName.equals(RangeDopplerGeocodingSetting.externalDEMStr)) {
                String extFileStr = oRangeDopplerGeocodingSetting.getExternalAuxFile();
                oOperator.setParameter("externalDEMFile", new File(extFileStr));
                oOperator.setParameter("externalDEMNoDataValue", oRangeDopplerGeocodingSetting.getExternalDEMNoDataValue());
                oOperator.setParameter("externalDEMApplyEGM", oRangeDopplerGeocodingSetting.getExternalDEMApplyEGM());
            }

            /*
            // P.Campanella 09/02/2018: this code required the UI version of SNAP and it is a problem on the server
            // Replaced with a likely equivalent code to get the mapProjection 
            
            MapProjectionHandler oMapHandler = new MapProjectionHandler();
            Product[] aoProducts = {oOperator.getSourceProduct()};
            
            oMapHandler.initParameters(oRangeDopplerGeocodingSetting.getMapProjection(), aoProducts);
            if (oMapHandler.getCRS() != null) {
                final CoordinateReferenceSystem crs = oMapHandler.getCRS();
                try {
                	
                	String sWKT = crs.toWKT();
                	//System.out.println(sWKT);
                	
                    oOperator.setParameter("mapProjection", crs.toWKT());
                } catch (UnformattableObjectException e) {        // if too complex to convert using strict
                    oOperator.setParameter("mapProjection", crs.toString());
                }
            }
            */
            Product oProduct = oOperator.getSourceProduct();
            
            //String sWKT = oProduct.getSceneGeoCoding().getGeoCRS().toWKT();
            //System.out.println(sWKT);
            
            try {
                oOperator.setParameter("mapProjection", oProduct.getSceneGeoCoding().getGeoCRS().toWKT());
            } catch (UnformattableObjectException e) {        // if too complex to convert using strict
                oOperator.setParameter("mapProjection", oProduct.getSceneGeoCoding().getGeoCRS().toString());
            }

            //Nell'interfaccia di SNAP questo checkbox si chiama Mask out areas without elevation
            oOperator.setParameter("nodataValueAtSea", oRangeDopplerGeocodingSetting.isNodataValueAtSea());
            oOperator.setParameter("outputComplex", oRangeDopplerGeocodingSetting.isOutputComplex());
            oOperator.setParameter("saveDEM", oRangeDopplerGeocodingSetting.isSaveDEM());
            oOperator.setParameter("saveLatLon", oRangeDopplerGeocodingSetting.isSaveLatLon());
            oOperator.setParameter("saveIncidenceAngleFromEllipsoid", oRangeDopplerGeocodingSetting.isSaveIncidenceAngleFromEllipsoid());
            oOperator.setParameter("saveLocalIncidenceAngle", oRangeDopplerGeocodingSetting.isSaveLocalIncidenceAngle());
            oOperator.setParameter("saveProjectedLocalIncidenceAngle", oRangeDopplerGeocodingSetting.isSaveProjectedLocalIncidenceAngle());
            oOperator.setParameter("saveSelectedSourceBand", oRangeDopplerGeocodingSetting.isSaveSelectedSourceBand());
            oOperator.setParameter("applyRadiometricNormalization", oRangeDopplerGeocodingSetting.isApplyRadiometricNormalization());
            oOperator.setParameter("saveBetaNought", oRangeDopplerGeocodingSetting.isSaveBetaNought());
            oOperator.setParameter("saveGammaNought", oRangeDopplerGeocodingSetting.isSaveGammaNought());
            oOperator.setParameter("saveSigmaNought", oRangeDopplerGeocodingSetting.isSaveSigmaNought());

            oOperator.setParameter("auxFile", oRangeDopplerGeocodingSetting.getAuxFile());
            final String extAuxFileStr = oRangeDopplerGeocodingSetting.getExternalAuxFile();
            if (!Utils.isNullOrEmpty(extAuxFileStr)) {
                oOperator.setParameter("externalAuxFile", new File(extAuxFileStr));
            }

        }

    }
}
