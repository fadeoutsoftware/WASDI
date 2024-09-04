/**
 * Created by Cristiano Nattero on 2018-12-19
 * 
 * Fadeout software
 *
 */
package wasdi.dataproviders;

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
		aoDownloaders.put("PROBAV", PROBAVProviderAdapter::new);
		aoDownloaders.put("CREODIAS", CREODIASProviderAdapter::new);
		aoDownloaders.put("CREODIAS2", CreoDias2ProviderAdapter::new);
		aoDownloaders.put("SOBLOO", SOBLOOProviderAdapter::new);
		aoDownloaders.put("EODC", EODCProviderAdapter::new);
		aoDownloaders.put("LSA", LSAProviderAdapter::new);
		aoDownloaders.put("VIIRS", VIIRSProviderAdapter::new);
		aoDownloaders.put("ADS", ADSProviderAdapter::new);
		aoDownloaders.put("CDS", CDSProviderAdapter::new);
		aoDownloaders.put("PLANET", PLANETProviderAdapter::new);
		aoDownloaders.put("TERRASCOPE", TerrascopeProviderAdapter::new);
		aoDownloaders.put("STATICS", STATICSProviderAdapter::new);
		aoDownloaders.put("GPM", GPMProviderAdapter::new);
		aoDownloaders.put("COPERNICUSMARINE", CM2ProviderAdapter::new);
		aoDownloaders.put("CLOUDFERRO", CloudferroProviderAdapter::new);
		aoDownloaders.put("SKYWATCH", SkywatchProviderAdapter::new);
		aoDownloaders.put("LPDAAC", LpDaacProviderAdapter::new);
		aoDownloaders.put("JRC", JRCProviderAdapter::new);
		aoDownloaders.put("DLR", DLRProviderAdapter::new);
		aoDownloaders.put("SINA", SinaProviderAdapter::new);		
		aoDownloaders.put("EXT_WEB", ExtWebProviderAdapter::new);
		s_aoDownloaderSuppliers = Collections.unmodifiableMap(aoDownloaders);
	}

	/**
	 * Check that the provided type is not null and that is a valid one. Throws an exception if it is not.
	 * @param sProviderAdapterType the type of the provider adapter
	 */
	public void validateProviderAdapterType(String sProviderAdapterType) {
		if (Utils.isNullOrEmpty(sProviderAdapterType)) {
			throw new NullPointerException("ProviderAdapterSupplier.validateProviderAdapterType: a null String has been passed");
		}

		if (!s_aoDownloaderSuppliers.containsKey(sProviderAdapterType)) {
			throw new IllegalArgumentException("ProviderAdapterSupplier.validateProviderAdapterType: Provider Adapter with name " + sProviderAdapterType + " is null. Check the provider name");
		}
	}

	/**
	 * Supply a provider adapter based on the requested type.
	 * @param sProviderAdapterType the type of the provider adapter
	 * @return a provider adapter that matches the requested type
	 */
	public ProviderAdapter supplyProviderAdapter(String sProviderAdapterType) {
		validateProviderAdapterType(sProviderAdapterType);

		Supplier<ProviderAdapter> oSupplier = s_aoDownloaderSuppliers.get(sProviderAdapterType);
		ProviderAdapter oResult = oSupplier.get();

		return oResult;
	}

}
