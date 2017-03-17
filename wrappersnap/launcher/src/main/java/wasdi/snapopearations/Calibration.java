package wasdi.snapopearations;

import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.snap.core.gpf.Operator;
import wasdi.shared.parameters.CalibratorSetting;
import wasdi.shared.parameters.ISetting;

import java.io.File;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class Calibration extends BaseOperation{

    public Calibration()
    {
        super(new CalibrationOp.Spi());
    }

    @Override
    public void FillSettings(Operator oOperator, ISetting oSetting) {
        oOperator.setParameterDefaultValues();
        if (oSetting == null)
            return;
        if (!(oSetting instanceof CalibratorSetting))
            return;
        CalibratorSetting oCalibratorSetting = (CalibratorSetting) oSetting;
        oOperator.setParameter("sourceBands", oCalibratorSetting.getSourceBandNames());
        oOperator.setParameter("auxFile", oCalibratorSetting.getAuxFile());

        final String extFileStr = oCalibratorSetting.getExternalAuxFile();
        if (!extFileStr.isEmpty()) {
            oOperator.setParameter("externalAuxFile", new File(extFileStr));
        }

        oOperator.setParameter("outputImageInComplex", oCalibratorSetting.getOutputImageInComplex());
        oOperator.setParameter("outputImageScaleInDb", oCalibratorSetting.getOutputImageScaleInDb());
        oOperator.setParameter("createGammaBand", oCalibratorSetting.getCreateGammaBand());
        oOperator.setParameter("createBetaBand", oCalibratorSetting.getCreateBetaBand());

        oOperator.setParameter("selectedPolarisations", oCalibratorSetting.getSelectedPolarisations());
        oOperator.setParameter("outputSigmaBand", oCalibratorSetting.getOutputSigmaBand());
        oOperator.setParameter("outputGammaBand", oCalibratorSetting.getOutputGammaBand());
        oOperator.setParameter("outputBetaBand", oCalibratorSetting.getOutputBetaBand());
    }
}
