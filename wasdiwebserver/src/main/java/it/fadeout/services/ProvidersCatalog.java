package it.fadeout.services;

import it.fadeout.business.DataProvider;

/**
 * Holds information about the registered providers
 */
public interface ProvidersCatalog {

    /**
     * Retrieve a provider from the catalog
     * @param sName Name of the provider
     * @return requested provider if any
     */
    DataProvider getProvider(String sName);

    /**
     * Retrieve the default provider for a node
     * @param sNode name of the node
     * @return requested provider if any
     */
    DataProvider getDefaultProvider(String sNode);


}
