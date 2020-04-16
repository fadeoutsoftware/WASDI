/**
 * Created by Cristiano Nattero on 2019-03-06
 * 
 * Fadeout software
 *
 */
package it.fadeout;

import javax.inject.Singleton;

import it.fadeout.services.ConfigProvidersCatalog;
import it.fadeout.services.ProvidersCatalog;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import it.fadeout.rest.resources.wps.WpsProxyFactory;
import wasdi.shared.data.NodeRepository;

/**
 * Wasdi binder,
 * @author c.nattero
 *
 */
public class WasdiBinder extends AbstractBinder {
    @Override
    protected void configure() {
        bind(WpsProxyFactory.class).to(WpsProxyFactory.class).in(Singleton.class);
        bind(ConfigProvidersCatalog.class).to(ProvidersCatalog.class).in(Singleton.class);

        // repositories binding
        bind(NodeRepository.class).to(NodeRepository.class);
    }
}