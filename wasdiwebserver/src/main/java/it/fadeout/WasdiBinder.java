/**
 * Created by Cristiano Nattero on 2019-03-06
 * 
 * Fadeout software
 *
 */
package it.fadeout;

import javax.inject.Singleton;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

import it.fadeout.rest.resources.wps.WpsProxyFactory;

/**
 * @author c.nattero
 *
 */
public class WasdiBinder extends AbstractBinder {
    @Override
    protected void configure() {
        bind(WpsProxyFactory.class).to(WpsProxyFactory.class).in(Singleton.class);
    }
}