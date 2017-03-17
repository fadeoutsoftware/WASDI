package wasdi.snapopearations;

import org.esa.s1tbx.sar.gpf.filtering.SpeckleFilterOp;
import org.esa.snap.core.gpf.Operator;
import wasdi.shared.parameters.FilterSetting;
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

        if (oSetting == null)
            return;
        if (!(oSetting instanceof FilterSetting))
            return;
        FilterSetting oFilterSetting = (FilterSetting) oSetting;
        oOperator.setParameter("sourceBands", oFilterSetting.getSourceBandNames());
        oOperator.setParameter("filter", oFilterSetting.getFilter());
        oOperator.setParameter("filterSizeX", oFilterSetting.getFilterSizeX());
        oOperator.setParameter("filterSizeY", oFilterSetting.getFilterSizeY());
        oOperator.setParameter("dampingFactor", oFilterSetting.getDampingFactor());
        oOperator.setParameter("estimateENL", oFilterSetting.isEstimateENL());
        oOperator.setParameter("enl", oFilterSetting.getEnl());

        oOperator.setParameter("numLooksStr", oFilterSetting.getNumLooksStr());
        oOperator.setParameter("windowSize", oFilterSetting.getWindowSize());
        oOperator.setParameter("targetWindowSizeStr", oFilterSetting.getTargetWindowSizeStr());
        oOperator.setParameter("sigmaStr", oFilterSetting.getSigmaStr());
        oOperator.setParameter("anSize", oFilterSetting.getAnSize());

    }
}
