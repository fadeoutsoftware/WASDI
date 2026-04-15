package wasdi.shared.parameters;

import wasdi.shared.parameters.settings.MosaicSetting;

/**
 * Parameter for MOSAIC Operation
 * 
 * @author p.campanella
 *
 */
public class MosaicParameter extends OperatorParameter {
    public MosaicParameter(){
        this.setSettings(new MosaicSetting());
    }
	
}
