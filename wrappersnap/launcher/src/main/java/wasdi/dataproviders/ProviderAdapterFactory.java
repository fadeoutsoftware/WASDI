/**
 * Created by Cristiano Nattero on 2018-12-19
 * 
 * Fadeout software
 *
 */
package wasdi.dataproviders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * @author c.nattero
 *
 */
public class ProviderAdapterFactory {

	private static final Map<String, ProviderAdapter> s_aoDownloaderSuppliers = new HashMap<>();

	static {
		
		ArrayList<DataProviderConfig> aoDataProviders = WasdiConfig.Current.dataProviders;
		
		for (DataProviderConfig oDPConfig : aoDataProviders) {
			try {
				String sName = oDPConfig.name;
				ProviderAdapter oQueryExecutor = (ProviderAdapter) Class.forName(oDPConfig.providerAdapterClasspath).getDeclaredConstructor().newInstance();
				s_aoDownloaderSuppliers.put(sName, oQueryExecutor);
			} catch (Exception oEx) {
				WasdiLog.errorLog("ProviderAdapterFactory.static: exception creating a Query Executor: " + oEx.toString());
			} 
		}
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

		ProviderAdapter oResult = s_aoDownloaderSuppliers.get(sProviderAdapterType);

		return oResult;
	}

}
