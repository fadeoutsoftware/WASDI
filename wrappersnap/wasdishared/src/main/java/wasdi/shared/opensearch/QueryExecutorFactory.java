/**
 * Created by Cristiano Nattero on 2019-02-06
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorFactory {

	private static final Map<String, Supplier<QueryExecutor>> s_aoExecutors;

	static {
		final Map<String, Supplier<QueryExecutor>> aoMap = new HashMap<>();
		
		aoMap.put("ONDA", QueryExecutorONDA::new);
		aoMap.put("SENTINEL", QueryExecutorSENTINEL::new);
		aoMap.put("MATERA", QueryExecutorMATERA::new);
		aoMap.put("PROBAV", QueryExecutorPROBAV::new);
		aoMap.put("FEDEO", QueryExecutorFEDEO::new);
		
		s_aoExecutors = Collections.unmodifiableMap(aoMap);
	}
	
	private QueryExecutor supply(String sProvider) {
		QueryExecutor oExecutor = null;
		if(null!=sProvider) {
			Supplier<QueryExecutor> oSupplier = s_aoExecutors.get(sProvider);
			if(null!=oSupplier) {
				oExecutor = oSupplier.get();
			}
		}
		return oExecutor;	
	}
	
	public QueryExecutor getExecutor(
			String sProvider,
			AuthenticationCredentials oCredentials,
			String sDownloadProtocol, String sGetMetadata) {
		
		QueryExecutor oExecutor = null;
		
		try {
			
			oExecutor = supply(sProvider);
			oExecutor.setCredentials(oCredentials);
			
			oExecutor.setMustCollectMetadata(Utils.doesThisStringMeansTrue(sGetMetadata));
			oExecutor.setDownloadProtocol(sDownloadProtocol);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return oExecutor;
	}

}
