/**
 * Created by Cristiano Nattero on 2019-02-20
 * 
 * Fadeout software
 *
 */
package wasdi.wps;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import wasdi.filebuffer.DhUSProviderAdapter;
import wasdi.filebuffer.ONDAProviderAdapter;
import wasdi.filebuffer.PROBAVProviderAdapter;
import wasdi.filebuffer.ProviderAdapter;

/**
 * @author c.nattero
 *
 */
public class WpsFactory {
	private static final Map<String, Supplier<WpsExecutionClient>> s_aoWpsSuppliers;
	
	static {
		final Map<String, Supplier<WpsExecutionClient>> aoWpsSuppliers = new HashMap<>();
		//TODO populate
		s_aoWpsSuppliers = Collections.unmodifiableMap(aoWpsSuppliers);
	}
	
	public WpsFactory(String getsWpsProvider) {
		// TODO Auto-generated constructor stub
		return;
	}

	public void supply(String wpsProvider) {
		// TODO Auto-generated method stub
		
	}

}
