package it.fadeout.services;

import wasdi.shared.business.DataProvider;
import wasdi.shared.business.Node;
import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.NodeRepository;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import java.util.Arrays;

/**
 * Catalog of the WASDI Data Providers
 * @author p.campanella
 *
 */
public class ConfigProvidersCatalog implements ProvidersCatalog {    
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
        DataProviderConfig oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(sName); 
        

        if (oDataProviderConfig != null) {
            oProvider.setName(sName);
            oProvider.setOSUser(oDataProviderConfig.user);
            oProvider.setOSPassword(oDataProviderConfig.password);
            oProvider.setDescription(oDataProviderConfig.description);
            oProvider.setLink(oDataProviderConfig.link);
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
