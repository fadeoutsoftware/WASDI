package wasdi.snapopearations;

import org.esa.s1tbx.sar.gpf.geometric.RangeDopplerGeocodingOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.engine_utilities.util.MemUtils;
import org.esa.snap.dataio.bigtiff.BigGeoTiffProductReaderPlugIn;
import wasdi.LauncherMain;
import wasdi.shared.parameters.RangeDopplerGeocodingParameter;
import wasdi.shared.parameters.RangeDopplerGeocodingSetting;

import java.io.File;
/**
 * Created by s.adamo on 24/05/2016.
 */
public class TerrainCorrection {

    public Product getTerrainCorrection(Product oProduct, RangeDopplerGeocodingSetting oSetting) throws Exception {
        Product oTerrainProduct = null;
        try {
            OperatorSpi spiTerrain = new RangeDopplerGeocodingOp.Spi();
            RangeDopplerGeocodingOp opTerrain = (RangeDopplerGeocodingOp) spiTerrain.createOperator();
            opTerrain.setSourceProduct(oProduct);
            FillRangeDopplerGeocodingSettings(opTerrain, oSetting);
            oTerrainProduct = opTerrain.getTargetProduct();
        }
        catch (Exception oEx)
        {
            LauncherMain.s_oLogger.debug("TerrainCorrection.getTerrainCorrection: error generating terrain product " + oEx.getMessage());
        }
        finally {
            return oTerrainProduct;
        }

    }

    private void FillRangeDopplerGeocodingSettings(RangeDopplerGeocodingOp opTerrain, RangeDopplerGeocodingSetting oSetting) {

        //set default value
        opTerrain.setParameterDefaultValues();
        // source band (FOR THE MOMENT WE SUPPORT ONLY ONE BAND AT TIME)
        if (oSetting.getSourceBandNames() != null && oSetting.getSourceBandNames().length > 0) {
            LauncherMain.s_oLogger.debug("TerrainCorrection.FillRangeDopplerGeocodingSettings: setting source band ");
            opTerrain.setParameter("sourceBands", oSetting.getSourceBandNames());
        }
        // set dem (SRTM 1Sec HGT)
        opTerrain.setParameter("demName", oSetting.getDemName());

    }
}
