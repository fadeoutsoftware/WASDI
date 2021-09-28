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
public class ProviderAdapterFactory {

	private static final Map<String, Supplier<ProviderAdapter>> s_aoDownloaderSuppliers;

	static {
		final Map<String, Supplier<ProviderAdapter>> aoDownloaders = new HashMap<>();
		aoDownloaders.put("ONDA", ONDAProviderAdapter::new);
		aoDownloaders.put("SENTINEL", DhUSProviderAdapter::new);
		aoDownloaders.put("MATERA", DhUSProviderAdapter::new);
		aoDownloaders.put("PROBAV", PROBAVProviderAdapter::new);
		aoDownloaders.put("FEDEO", DhUSProviderAdapter::new);
		aoDownloaders.put("CREODIAS", CREODIASProviderAdapter::new);
		aoDownloaders.put("SOBLOO", SOBLOOProviderAdapter::new);
		aoDownloaders.put("EODC", EODCProviderAdapter::new);
		aoDownloaders.put("LSA", LSAProviderAdapter::new);
		aoDownloaders.put("VIIRS", VIIRSProviderAdapter::new);
		s_aoDownloaderSuppliers = Collections.unmodifiableMap(aoDownloaders);
	}

	public ProviderAdapter supplyProviderAdapter(String sProviderAdapterType) {
		ProviderAdapter oResult = null;
		if(Utils.isNullOrEmpty(sProviderAdapterType)) {
			throw new NullPointerException("ProviderAdapterSupplier.supplyProviderAdapter: a null String has been passed");
		} else {
			Supplier<ProviderAdapter> oSupplier = s_aoDownloaderSuppliers.get(sProviderAdapterType);
			if(oSupplier != null ) {
				oResult = oSupplier.get();
				oResult.readConfig();
			}
		}
		
		return oResult;
	}
}
