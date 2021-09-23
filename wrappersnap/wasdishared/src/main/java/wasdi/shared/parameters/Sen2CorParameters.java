package wasdi.shared.parameters;

/**
 * Class that hold the parameters to start SEN2COR launcher operations
 */
public class Sen2CorParameters extends BaseParameter{



    private String m_sWorkspaceId;
    private String m_sProductName;
    private String m_sVersion; // this can be "2.5.5" or "2.9"
    private boolean m_bDeleteIntermediateFile;


    public Sen2CorParameters() {
        this.m_sVersion = "2.5.5"; // default version
        m_bDeleteIntermediateFile = true; // default is to delete the intermediate files
    }

    public Sen2CorParameters(String m_sWorkspaceId, String m_sProductName, String m_sVersion, boolean m_bDeleteIntermediateFile) {
        this.m_sWorkspaceId = m_sWorkspaceId;
        this.m_sProductName = m_sProductName;
        this.m_sVersion = m_sVersion;
        this.m_bDeleteIntermediateFile = m_bDeleteIntermediateFile;
    }

    public boolean isM_bDeleteIntermediateFile() {
        return m_bDeleteIntermediateFile;
    }

    public void setM_bDeleteIntermediateFile(boolean m_bDeleteIntermediateFile) {
        this.m_bDeleteIntermediateFile = m_bDeleteIntermediateFile;
    }

    public String getM_sProductName() {
        return m_sProductName;
    }

    public void setM_sProductName(String m_sProductName) {
        this.m_sProductName = m_sProductName;
    }

    public String getM_sVersion() {
        return m_sVersion;
    }

    public void setM_sVersion(String m_sVersion) {
        this.m_sVersion = m_sVersion;
    }


    /**
     * Util function to check the validity of parameters
     * @return true if parameters are valid
     */
    boolean isValid(){
        return (m_sProductName != null && m_sWorkspaceId != null &&
                (m_sVersion.equals("2.5.5") || m_sVersion.equals(2.9))
        );
    }
}
