package wasdi.snapopearations;


import org.esa.s2tbx.radiometry.TndviOp;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.ndvi.NdviOp;

import wasdi.shared.parameters.ISetting;
import wasdi.shared.parameters.NDVISetting;

/**
 * NDVI Snap Operation Parameter Wrapper
 * Created by s.adamo on 17/03/2017.
 */
public class NDVI extends BaseOperation{

    public NDVI()
    {
        super(new NdviOp.Spi());
    }


    @Override
    public void FillSettings(Operator oOperator, ISetting oSetting) {
        //set default value
        oOperator.setParameterDefaultValues();
        if (oSetting == null)
            return;
        if (!(oSetting instanceof NDVISetting))
            return;

        NDVISetting oNDVISetting = (NDVISetting) oSetting;

        oOperator.setParameter("redFactor", oNDVISetting.getRedFactor());
        oOperator.setParameter("nirFactor", oNDVISetting.getNirFactor());
        oOperator.setParameter("redSourceBand", oNDVISetting.getRedSourceBand());
        oOperator.setParameter("nirSourceBand", oNDVISetting.getNirSourceBand());

    }
}
