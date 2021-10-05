package it.fadeout.business;

/**
 * Data Provider model
 * It is used to show the list of data providers 
 */
public class DataProvider {
	
	/**
	 * Name of the Data Provider
	 */
    private String m_sName;
    
    /**
     * User Id Used by WASDI to contact the provider
     */
    private String m_sOSUser;
    
    /**
     * Password Used by WASDI to contact the provider
     */
    private String m_sOSPassword;
    
    /**
     * Data Provider Description
     */
    private String m_sDescription;
    
    /**
     * Link to the data provider
     */
    private String m_sLink;

    /**
     * Provider name
     */
    public String getName() {
        return m_sName;
    }

    public void setName(String sValue) {
        m_sName = sValue;
    }


    /**
     * Provider User
     */
    public String getOSUser() {
        return m_sOSUser;
    }

    public void setOSUser(String m_sOSUser) {
        this.m_sOSUser = m_sOSUser;
    }

    /**
     * Provider password
     */
    public String getOSPassword() {
        return m_sOSPassword;
    }

    public void setOSPassword(String m_sOSPassword) {
        this.m_sOSPassword = m_sOSPassword;
    }

    /**
     * Provider description
     */
    public String getDescription() {
        return m_sDescription;
    }

    public void setDescription(String m_sDescription) {
        this.m_sDescription = m_sDescription;
    }

    /**
     * Provider link
     */
    public String getLink() {
        return m_sLink;
    }

    public void setLink(String m_sLink) {
        this.m_sLink = m_sLink;
    }


}
