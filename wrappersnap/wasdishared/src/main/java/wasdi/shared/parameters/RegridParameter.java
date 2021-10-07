package wasdi.shared.parameters;

import wasdi.shared.parameters.settings.RegridSetting;

public class RegridParameter extends OperatorParameter {
    public RegridParameter(){
        this.setSettings(new RegridSetting());
    }
}
