/**
 * Created by Cristiano Nattero on 2019-03-06
 * 
 * Fadeout software
 *
 */
package it.fadeout.rest.resources.wps;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import it.fadeout.Wasdi;
import wasdi.shared.data.WpsProvidersRepository;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class WpsProxyFactory {
	
	static final Map<String,Supplier<WpsProxy>> s_aoProxies;
	
	static {
		Wasdi.DebugLog("WpsProxyFactory: static constructor");
		Map<String, Supplier<WpsProxy>> aoProxies = new HashMap<>();
		aoProxies.put("default", WpsProxy::new);
		aoProxies.put("gpod", WpsProxyGPOD::new);
		aoProxies.put("utep", WpsProxyBasicAuth::new);
		aoProxies.put("basicAuth", WpsProxyBasicAuth::new);
		aoProxies.put("wasdi", WpsProxyWASDI::new);
		
		s_aoProxies = Collections.unmodifiableMap(aoProxies);
	}
	
	public WpsProxyFactory() {
		Wasdi.DebugLog("WpsProxyFactory.WpsProxyFactory");
	}

	public WpsProxy get(String sWpsProvider) {
		Wasdi.DebugLog("WpsProxyFactory.get");
		Supplier<WpsProxy> oSupplier = null;
		if(Utils.isNullOrEmpty(sWpsProvider)) {
			Wasdi.DebugLog("WpsProxyFactory.get: null or empty provider, returning default");
			oSupplier = s_aoProxies.get("default");
		} else {
			oSupplier = s_aoProxies.get(sWpsProvider.toLowerCase());
		}
		if(null==oSupplier) {
			Wasdi.DebugLog("WpsProxyFactory.get: no suitable proxy for given provider found, trying default");
			oSupplier = s_aoProxies.get("default");
		} else {
			Wasdi.DebugLog("WpsProxyFactory.get: returning WpsProxy for "+sWpsProvider );
		}
		WpsProxy oResult = oSupplier.get();
		oResult.setProviderName(sWpsProvider);
		return oResult;
	}

}
