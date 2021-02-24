/**
 * Created by Cristiano Nattero on 2019-03-06
 * 
 * Fadeout software
 *
 */
package it.fadeout;

import javax.inject.Singleton;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import it.fadeout.rest.resources.AuthResource;
import it.fadeout.services.KeycloakService;
import it.fadeout.services.ConfigProvidersCatalog;
import it.fadeout.services.ProvidersCatalog;
import wasdi.shared.data.NodeRepository;

/**
 * Wasdi binder,
 * @author c.nattero
 *
 */
public class WasdiBinder extends AbstractBinder {
    @Override
    protected void configure() {
        bind(ConfigProvidersCatalog.class).to(ProvidersCatalog.class).in(Singleton.class);
        
        bind(KeycloakService.class).to(AuthResource.class).in(Singleton.class);

        // repositories binding
        bind(NodeRepository.class).to(NodeRepository.class);
    }
}