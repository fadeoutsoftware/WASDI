package it.fadeout.snapopearations;

import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.dataio.bigtiff.BigGeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.util.MemUtils;

import java.io.File;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class Calibration {

    public Product getCalibration(Product oProduct, String sFilePath) throws Exception
    {
        //calibration
        OperatorSpi spi = new CalibrationOp.Spi();
        Operator op = spi.createOperator();
        op.setSourceProduct(oProduct);
        Product calibrateProduct = op.getTargetProduct();

        String bigGeoTiffFormatName = BigGeoTiffProductReaderPlugIn.FORMAT_NAME;

        File calFile = new File(sFilePath + "test_cal.tif");

        ProductIO.writeProduct(calibrateProduct, calFile.getAbsolutePath(), bigGeoTiffFormatName);
        MemUtils.freeAllMemory();
        return calibrateProduct;
    }

}
