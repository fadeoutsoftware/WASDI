package wasdi.snapopearations;

import org.esa.s1tbx.sar.gpf.filtering.SpeckleFilterOp;
import org.esa.snap.core.gpf.Operator;
import wasdi.shared.parameters.ISetting;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class Filter extends BaseOperation{

    public Filter()
    {
        super(new SpeckleFilterOp.Spi());
    }

    @Override
    public void FillSettings(Operator oOperator, ISetting oSetting) {
        oOperator.setParameterDefaultValues();

    }
}
