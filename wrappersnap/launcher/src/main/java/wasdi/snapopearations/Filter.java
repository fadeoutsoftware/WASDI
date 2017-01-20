package wasdi.snapopearations;

import org.esa.s1tbx.sar.gpf.filtering.SpeckleFilterOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.dataio.bigtiff.BigGeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.util.MemUtils;
import wasdi.LauncherMain;

import java.io.File;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class Filter {

    public Product getFilter(Product oProduct, String[] asBandName) throws Exception {
        OperatorSpi spiFilter = new SpeckleFilterOp.Spi();
        SpeckleFilterOp opFilter = (SpeckleFilterOp) spiFilter.createOperator();
        opFilter.setSourceProduct(oProduct);
        if (asBandName != null)
            opFilter.setParameter("sourceBands", asBandName);
        opFilter.SetFilter("Refined Lee");
        Product filterProduct = opFilter.getTargetProduct();

        return filterProduct;
    }
}
