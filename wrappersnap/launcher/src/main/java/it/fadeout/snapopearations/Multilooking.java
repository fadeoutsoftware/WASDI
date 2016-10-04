package it.fadeout.snapopearations;

import org.esa.s1tbx.sar.gpf.MultilookOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class Multilooking {

    public Product getMultilooking(Product oProduct) throws Exception {
        OperatorSpi spiMulti = new MultilookOp.Spi();
        MultilookOp opMulti = (MultilookOp) spiMulti.createOperator();
        opMulti.setSourceProduct(oProduct);
        opMulti.setNumRangeLooks(4);
        MultilookOp.DerivedParams param = new MultilookOp.DerivedParams();
        param.nRgLooks = 4;
        MultilookOp.getDerivedParameters(oProduct, param);
        //opMulti.setNumAzimuthLooks(param.nAzLooks);
        Product multiProduct = opMulti.getTargetProduct();

        return multiProduct;
    }

}
