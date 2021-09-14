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
import it.fadeout.services.ProcessService;
import it.fadeout.services.ProcessServiceInterface;
import it.fadeout.services.AuthProviderService;
import it.fadeout.services.ConfigProvidersCatalog;
import it.fadeout.services.ProvidersCatalog;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.NodeRepository;

/**
 * Wasdi binder,
 * @author c.nattero
 *
 */
public class WasdiBinder extends AbstractBinder {
	@Override
	protected void configure() {
		//providers catalog
		bind(ConfigProvidersCatalog.class).to(ProvidersCatalog.class).in(Singleton.class);
		//authentication provider -> keycloak
		bind(KeycloakService.class).to(AuthProviderService.class).in(Singleton.class);
		// repositories binding
		bind(NodeRepository.class).to(NodeRepository.class);
		//process workspace
		//bind(ProcessService.class).to(ProcessServiceInterface.class).in(Singleton.class);
		bind(ProcessService.class).to(ProcessService.class).in(Singleton.class);
	}
}