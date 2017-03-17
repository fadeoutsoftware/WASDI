package wasdi.snapopearations;

import org.esa.s1tbx.sar.gpf.MultilookOp;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import wasdi.shared.parameters.ISetting;
import wasdi.shared.parameters.MultilookingSetting;

/**
 * Created by s.adamo on 24/05/2016.
 */
public class Multilooking extends BaseOperation{

    public Multilooking()
    {
        super(new MultilookOp.Spi());
    }

    @Override
    public void FillSettings(Operator oOperator, ISetting oSetting) {
        oOperator.setParameterDefaultValues();
        if (oSetting == null)
            return;
        if (!(oSetting instanceof MultilookingSetting))
            return;
        MultilookingSetting oMultilookingSetting = (MultilookingSetting) oSetting;
        oOperator.setParameter("sourceBands", oMultilookingSetting.getSourceBandNames());
        oOperator.setParameter("nRgLooks", oMultilookingSetting.getnRgLooks());
        oOperator.setParameter("nAzLooks", oMultilookingSetting.getnAzLooks());
        if(!isComplexSrcProduct(oOperator.getSourceProduct())) {
            oOperator.setParameter("outputIntensity", true);
        }

        oOperator.setParameter("grSquarePixel", oMultilookingSetting.getGrSquarePixel());
    }

    private boolean isComplexSrcProduct(Product oProduct) {
        if (oProduct != null) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(oProduct);
            if (absRoot != null) {
                final String sampleType = absRoot.getAttributeString(
                        AbstractMetadata.SAMPLE_TYPE, AbstractMetadata.NO_METADATA_STRING).trim();
                if (sampleType.equalsIgnoreCase("complex"))
                    return true;
            }
            return false;
        }
        return false;
    }
}
