package wasdi.snapopearations;

import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.dataio.bigtiff.BigGeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.util.MemUtils;
import wasdi.LauncherMain;

import java.io.File;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class Calibration {

    public Product getCalibration(Product oProduct, String[] asBandName) throws Exception
    {
        //calibration
        OperatorSpi spi = new CalibrationOp.Spi();
        CalibrationOp op = (CalibrationOp) spi.createOperator();
        op.setSourceProduct(oProduct);
        if (asBandName != null)
            op.setParameter("sourceBands", asBandName);
        Product calibrateProduct = op.getTargetProduct();

        return calibrateProduct;
    }

}
