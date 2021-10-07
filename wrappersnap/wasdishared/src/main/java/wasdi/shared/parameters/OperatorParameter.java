package wasdi.shared.parameters;

import wasdi.shared.parameters.settings.ISetting;

/**
 * Created by s.adamo on 17/03/2017.
 */
public class OperatorParameter extends BaseParameter{

    private String sourceProductName;

    private String destinationProductName;

    private ISetting settings;

    public ISetting getSettings() {
        return settings;
    }

    public void setSettings(ISetting settings) {
        this.settings = settings;
    }

    public String getSourceProductName() {
        return sourceProductName;
    }

    public void setSourceProductName(String sourceProductName) {
        this.sourceProductName = sourceProductName;
    }

    public String getDestinationProductName() {
        return destinationProductName;
    }

    public void setDestinationProductName(String destinationProductName) {
        this.destinationProductName = destinationProductName;
    }
}
