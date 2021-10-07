package wasdi.shared.parameters;

import wasdi.shared.parameters.settings.SubsetSetting;

public class SubsetParameter extends OperatorParameter {
    public SubsetParameter(){
        this.setSettings(new SubsetSetting());
    }
}
