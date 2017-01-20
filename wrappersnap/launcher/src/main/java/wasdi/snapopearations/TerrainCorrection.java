package wasdi.snapopearations;

import org.esa.s1tbx.sar.gpf.geometric.RangeDopplerGeocodingOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.engine_utilities.util.MemUtils;
import org.esa.snap.dataio.bigtiff.BigGeoTiffProductReaderPlugIn;
import wasdi.LauncherMain;

import java.io.File;
/**
 * Created by s.adamo on 24/05/2016.
 */
public class TerrainCorrection {

    public Product getTerrainCorrection(Product oProduct, String[] asBandName) throws Exception {
        OperatorSpi spiTerrain = new RangeDopplerGeocodingOp.Spi();
        RangeDopplerGeocodingOp opTerrain = (RangeDopplerGeocodingOp) spiTerrain.createOperator();
        opTerrain.setParameterDefaultValues();
        LauncherMain.s_oLogger.debug("LauncherMain.TerrainCorrection: Terrain setting dem elevation");
        opTerrain.setParameter("demName", "SRTM 1Sec HGT");

        if (opTerrain == null)
        {
            LauncherMain.s_oLogger.debug("LauncherMain.TerrainCorrection: Terrain operator null");
            return null;
        }
        opTerrain.setSourceProduct(oProduct);
        if (asBandName != null)
            opTerrain.setParameter("sourceBands", asBandName);

        Product terrainProduct = opTerrain.getTargetProduct();

        return terrainProduct;
    }
}
