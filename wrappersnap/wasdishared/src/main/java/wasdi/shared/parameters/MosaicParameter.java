package wasdi.shared.parameters;

import wasdi.shared.parameters.settings.MosaicSetting;

public class MosaicParameter extends OperatorParameter {
    public MosaicParameter(){
        this.setSettings(new MosaicSetting());
    }
	
}
