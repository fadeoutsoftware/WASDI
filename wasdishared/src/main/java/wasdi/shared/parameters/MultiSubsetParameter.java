package wasdi.shared.parameters;

import wasdi.shared.parameters.settings.MultiSubsetSetting;

/**
 * Parameter for the MULTISUBSET Operation
 * 
 * @author p.campanella
 *
 */
public class MultiSubsetParameter extends OperatorParameter {
    public MultiSubsetParameter(){
        this.setSettings(new MultiSubsetSetting());
    }
}
