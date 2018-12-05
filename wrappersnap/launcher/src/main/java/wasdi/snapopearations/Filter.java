package wasdi.snapopearations;

import org.esa.snap.core.gpf.Operator;
import org.esa.snap.raster.gpf.FilterOperator;

import wasdi.shared.parameters.FilterSetting;
import wasdi.shared.parameters.ISetting;

/**
 * Filter Snap Operation Parameter Wrapper
 * Created by s.adamo on 24/05/2016.
 */
public class Filter extends BaseOperation{

    public Filter()
    {
        super(new FilterOperator.Spi());
    }

    @Override
    public void FillSettings(Operator oOperator, ISetting oSetting) {
        oOperator.setParameterDefaultValues();

        if (oSetting == null)
            return;
        if (!(oSetting instanceof FilterSetting))
            return;

        //FilterSetting oFilterSetting = (FilterSetting) oSetting;
        //TODO implement

    }
}
