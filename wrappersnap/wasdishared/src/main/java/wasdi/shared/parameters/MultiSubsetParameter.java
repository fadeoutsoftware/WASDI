package wasdi.shared.parameters;

import wasdi.shared.parameters.settings.MultiSubsetSetting;

public class MultiSubsetParameter extends OperatorParameter {
    public MultiSubsetParameter(){
        this.setSettings(new MultiSubsetSetting());
    }
}
