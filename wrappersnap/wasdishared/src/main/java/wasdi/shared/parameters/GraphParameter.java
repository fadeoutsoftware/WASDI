package wasdi.shared.parameters;

import wasdi.shared.parameters.settings.GraphSetting;

/**
 * Parameter of a GRAPH Operation
 * Created by s.adamo on 16/03/2017.
 */
public class GraphParameter extends OperatorParameter{

    public GraphParameter(){
        this.setSettings(new GraphSetting());
    }
}
