package wasdi.shared.parameters;

/**
 * Class that hold the parameters to start SEN2COR launcher operations
 */
public class Sen2CorParameters extends BaseParameter {


    private String productName;
    private String version; // this can be "2.5.5" or "2.9"
    private boolean deleteIntermediateFile = true;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Sen2CorParameters() {
        super();
        this.version = "2.5.5"; // default version
        deleteIntermediateFile = true; // default is to delete the intermediate files
    }

    public Sen2CorParameters(String productName, String Version, boolean deleteIntermediateFile) {
        this.productName = productName;
        this.version = Version;
        this.deleteIntermediateFile = deleteIntermediateFile;
    }

    public boolean isDeleteIntermediateFile() {
        return deleteIntermediateFile;
    }

    public void setDeleteIntermediateFile(boolean deleteIntermediateFile) {
        this.deleteIntermediateFile = deleteIntermediateFile;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }


    /**
     * Util function to check the validity of parameters
     *
     * @return true if parameters are valid
     */
    public boolean isValid() {
        return (productName != null &&
                (version.equals("2.5.5") || version.equals("2.9"))
        );
    }
}
