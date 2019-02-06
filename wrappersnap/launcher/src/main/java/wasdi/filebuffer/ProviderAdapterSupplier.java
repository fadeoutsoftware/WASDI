/**
 * Created by Cristiano Nattero on 2018-12-19
 * 
 * Fadeout software
 *
 */
package wasdi.filebuffer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class ProviderAdapterSupplier {

	private static final Map<String, Supplier<ProviderAdapter>> s_aoDownloaderSuppliers;

	static {
		final Map<String, Supplier<ProviderAdapter>> aoDownloaders = new HashMap<>();
		aoDownloaders.put("SENTINEL", DhUSProviderAdapter::new);
		aoDownloaders.put("MATERA", DhUSProviderAdapter::new);
		aoDownloaders.put("FEDEO", DhUSProviderAdapter::new);
		aoDownloaders.put("PROBAV", PROBAVProviderAdapter::new);
		aoDownloaders.put("ONDA", ONDAProviderAdapter::new);

		s_aoDownloaderSuppliers = Collections.unmodifiableMap(aoDownloaders);
	}

	public ProviderAdapter supplyProviderAdapter(String sProviderAdapterType) {
		ProviderAdapter oResult = null;
		if(!Utils.isNullOrEmpty(sProviderAdapterType)) {
			Supplier<ProviderAdapter> oSupplier = s_aoDownloaderSuppliers.get(sProviderAdapterType);
			if(oSupplier != null ) {
				oResult = oSupplier.get();
			}
		}

		return oResult;
	}

}
