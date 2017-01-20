package wasdi.snapopearations;

import org.esa.s1tbx.sar.gpf.MultilookOp;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.engine_utilities.util.MemUtils;
import wasdi.LauncherMain;

import java.io.File;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class Multilooking {

    public Product getMultilooking(Product oProduct, String[] asBandName) throws Exception {
        OperatorSpi spiMulti = new MultilookOp.Spi();
        MultilookOp opMulti = (MultilookOp) spiMulti.createOperator();
        opMulti.setSourceProduct(oProduct);
        opMulti.setNumRangeLooks(4);
        MultilookOp.DerivedParams param = new MultilookOp.DerivedParams();
        param.nRgLooks = 4;
        MultilookOp.getDerivedParameters(oProduct, param);
        opMulti.setNumAzimuthLooks(param.nAzLooks);
        if (asBandName != null)
            opMulti.setParameter("sourceBands", asBandName);
        Product multiProduct = opMulti.getTargetProduct();

        return multiProduct;
    }

}
