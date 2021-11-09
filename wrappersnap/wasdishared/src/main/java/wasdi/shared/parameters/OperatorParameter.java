package wasdi.shared.parameters;

import wasdi.shared.parameters.settings.ISetting;

/**
 * Generic Operator Parameter
 * 
 * adds a source and a destination name to the base parameter
 * 
 * Created by s.adamo on 17/03/2017.
 */
public class OperatorParameter extends BaseParameter{

	/**
	 * Operation Input file
	 */
    private String sourceProductName;
    
    /**
     * Operator Output file
     */
    private String destinationProductName;
    
    /**
     * Generic Settings for the parameter.
     * These settings are used for SNAP and geometric Operations like:
     * 	.subset
     * 	.multisubset
     * 	.graph
     * 	.regrid
     */
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
