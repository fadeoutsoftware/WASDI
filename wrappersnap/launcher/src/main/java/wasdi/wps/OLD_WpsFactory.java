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

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class OLD_WpsFactory {
	private static final Map<String, Supplier<OLD_WpsAdapter>> s_aoWpsSuppliers;
	
	static {
		System.out.println("WpsFactory static constructor");
		final Map<String, Supplier<OLD_WpsAdapter>> aoWpsSuppliers = new HashMap<>();
		aoWpsSuppliers.put("wasdi", OLD_WasdiWpsAdapter::new);
		aoWpsSuppliers.put("n52Demo", OLD_N52DemoWpsAdapter::new);
		//TODO add gpod
		//TODO add utep
		s_aoWpsSuppliers = Collections.unmodifiableMap(aoWpsSuppliers);
	}
	

	public OLD_WpsAdapter supply(String sWpsProvider) {
		System.out.println("WpsFactory.WpsFactory");
		OLD_WpsAdapter oWps = null;
		if(Utils.isNullOrEmpty(sWpsProvider)) {
			throw new NullPointerException("WpsFactory.WpsFactory: passed a null String");
		} else {
			oWps = s_aoWpsSuppliers.get(sWpsProvider).get();
		}
		return oWps;
		
	}

}
