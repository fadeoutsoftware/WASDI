package it.fadeout.services;

import wasdi.shared.business.Node;
import wasdi.shared.data.NodeRepository;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import it.fadeout.business.DataProvider;

import java.util.Arrays;

/**
 * Catalog of the WASDI Data Providers
 * @author p.campanella
 *
 */
public class ConfigProvidersCatalog implements ProvidersCatalog {
	
	/**
	 * Local servlet config to access web.xml file
	 */
    @Context
    ServletConfig m_oServletConfig;
    
    /**
     * Nodes Repository
     */
    @Inject
    NodeRepository m_oRepository;
    
    /**
     * Get the model of a selected provider
     */
    @Override
    public DataProvider getProvider(String sName) {

        DataProvider oProvider = new DataProvider();

        // if it's a registered provider, fill the object with the data from the configuration file
        String sProviders = m_oServletConfig.getInitParameter("SearchProviders");
        
        if (sProviders != null && sProviders.length() > 0) {
            String[] asProviders = sProviders.split(",|;");

            boolean bIsRegistered = Arrays.stream(asProviders).anyMatch(x -> x.equals(sName));
            if (bIsRegistered) {
                oProvider.setName(sName);
                oProvider.setOSUser(m_oServletConfig.getInitParameter(sName + ".OSUser"));
                oProvider.setOSPassword(m_oServletConfig.getInitParameter(sName + ".OSPwd"));
                oProvider.setDescription(m_oServletConfig.getInitParameter(sName + ".Description"));
                oProvider.setLink(m_oServletConfig.getInitParameter(sName + ".Link"));
            }
        }

        return oProvider;
    }
    
    /**
     * Get the default data provider for node
     */
    @Override
    public DataProvider getDefaultProvider(String sNode) {
        DataProvider oProvider = new DataProvider();
        Node oNode = m_oRepository.getNodeByCode(sNode);
        if (oNode != null) {
            String sDefaultProvider = oNode.getDefaultProvider();
            oProvider = getProvider(sDefaultProvider);
        }
        return oProvider;
    }
}
