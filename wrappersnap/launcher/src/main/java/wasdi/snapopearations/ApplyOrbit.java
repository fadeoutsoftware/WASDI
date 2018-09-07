package wasdi.snapopearations;

import org.esa.s1tbx.sar.gpf.orbits.ApplyOrbitFileOp;
import org.esa.snap.core.gpf.Operator;
import wasdi.shared.parameters.ApplyOrbitSetting;
import wasdi.shared.parameters.ISetting;

/**
 * Apply Orbit Snap Operation Parameter Wrapper
 * Created by s.adamo on 16/03/2017.
 */
public class ApplyOrbit extends BaseOperation{

    public ApplyOrbit()
    {
        super(new ApplyOrbitFileOp.Spi());
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
