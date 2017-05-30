package wasdi.snapopearations;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;
import wasdi.LauncherMain;
import wasdi.shared.parameters.ISetting;


/**
 * Created by s.adamo on 17/03/2017.
 */
public abstract class BaseOperation {

    OperatorSpi m_oSpiMulti;

    public BaseOperation(OperatorSpi oOperator)
    {
        m_oSpiMulti = oOperator;
    }

    public Product getOperation(Product oSourceProduct, ISetting oSetting) throws Exception {
        Product oTargetProduct = null;

        if (m_oSpiMulti == null) {
            LauncherMain.s_oLogger.debug("BaseOperation.getOperation: Operator null ");
            return oTargetProduct;
        }
        if (oSourceProduct == null) {
            LauncherMain.s_oLogger.debug("BaseOperation.getOperation: oSourceProduct null ");
            return oTargetProduct;
        }

        try {
            Operator oOperator = m_oSpiMulti.createOperator();
            oOperator.setSourceProduct(oSourceProduct);
            FillSettings(oOperator, oSetting);
            oTargetProduct = oOperator.getTargetProduct();
        }
        catch(Exception oEx)
        {
        	oEx.printStackTrace();
            LauncherMain.s_oLogger.debug("BaseOperation.getOperation: error executing operation " + oEx.getMessage());
        }
        finally {
            return oTargetProduct;
        }

    }

    public abstract void FillSettings(Operator oOperator, ISetting oSetting);


}
