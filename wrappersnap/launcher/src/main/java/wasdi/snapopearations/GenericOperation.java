package wasdi.snapopearations;

import org.esa.snap.core.gpf.Operator;

import wasdi.shared.SnapOperatorFactory;
import wasdi.shared.parameters.ApplyOrbitSetting;
import wasdi.shared.parameters.ISetting;

/**
 * Generic SPI Snap Operation Parameter Wrapper
 * Created by s.adamo on 16/03/2017.
 */
public class GenericOperation extends BaseOperation{

    public GenericOperation(String sOperation)
    {
        super(SnapOperatorFactory.getOperatorSpi(sOperation));
    }

    @Override
    public void FillSettings(Operator oOperator, ISetting oSetting) {

        oOperator.setParameterDefaultValues();
        if (oSetting == null)
            return;
        if (!(oSetting instanceof ApplyOrbitSetting))
            return;
        ApplyOrbitSetting oApplyOrbitSetting = (ApplyOrbitSetting) oSetting;
        oOperator.setParameter("sourceBands", oApplyOrbitSetting.getSourceBandNames());
        oOperator.setParameter("polyDegree", oApplyOrbitSetting.getPolyDegree());
        oOperator.setParameter("orbitType", oApplyOrbitSetting.getOrbitType());
        oOperator.setParameter("continueOnFail", oApplyOrbitSetting.getContinueOnFail());

    }
}
