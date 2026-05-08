package wasdi.shared.parameters;

import wasdi.shared.parameters.settings.SubsetSetting;

/**
 * Parameter of the SUBSET Operation
 * 
 * Note: depecrated, use MULTISUBSET
 * 
 * @author p.campanella
 *
 */
public class SubsetParameter extends OperatorParameter {
    public SubsetParameter(){
        this.setSettings(new SubsetSetting());
    }
}
