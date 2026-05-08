package wasdi.shared.parameters;

import wasdi.shared.parameters.settings.RegridSetting;

/**
 * Parameter of the REGRID Operation
 * @author p.campanella
 *
 */
public class RegridParameter extends OperatorParameter {
    public RegridParameter(){
        this.setSettings(new RegridSetting());
    }
}
