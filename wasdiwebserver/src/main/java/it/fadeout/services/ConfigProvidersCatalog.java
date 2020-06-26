package it.fadeout.services;

import it.fadeout.business.Provider;
import wasdi.shared.business.Node;
import wasdi.shared.data.NodeRepository;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import java.util.Arrays;

public class ConfigProvidersCatalog implements ProvidersCatalog {

    @Context
    ServletConfig m_oServletConfig;

    @Inject
    NodeRepository m_oRepository;

    @Override
    public Provider getProvider(String sName) {

        Provider oProvider = new Provider();

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

    @Override
    public Provider getDefaultProvider(String sNode) {
        Provider oProvider = new Provider();
        Node oNode = m_oRepository.getNodeByCode(sNode);
        if (oNode != null) {
            String sDefaultProvider = oNode.getDefaultProvider();
            oProvider = getProvider(sDefaultProvider);
        }
        return oProvider;
    }
}
