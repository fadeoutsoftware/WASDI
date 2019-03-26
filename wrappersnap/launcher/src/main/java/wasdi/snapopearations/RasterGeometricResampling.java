package wasdi.snapopearations;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.common.resample.ResamplingOp;

import wasdi.LauncherMain;

/**
 * Raster Geometric Resampling Snap Operation Parameter Wrapper
 * Created by p.campanella on 06/03/2017.
 */
public class RasterGeometricResampling {

    public Product getResampledProduct(Product oProduct, String sReferenceBandName) {

        Product oResampledProduct = null;
        try {
            OperatorSpi spiResample = new ResamplingOp.Spi();
            ResamplingOp opResamplingOp = (ResamplingOp) spiResample.createOperator();
            opResamplingOp.setSourceProduct(oProduct);

            opResamplingOp.setParameter("referenceBand", sReferenceBandName);
            opResamplingOp.execute(null);
            oResampledProduct = opResamplingOp.getTargetProduct();

            //oTerrainProduct = opResamplingOp.
        }
        catch (Exception oEx)
        {
            LauncherMain.s_oLogger.debug("RasterGeometricResampling.getResampledProduct: error resampling product " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(oEx));
        }
        finally {
            
        }
        return oResampledProduct;
    }
}
