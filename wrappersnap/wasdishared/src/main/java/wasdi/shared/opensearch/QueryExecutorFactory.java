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

import wasdi.shared.opensearch.creodias.QueryExecutorCREODIAS;
import wasdi.shared.opensearch.eodc.QueryExecutorEODC;
import wasdi.shared.opensearch.lsa.QueryExecutorLSA;
import wasdi.shared.opensearch.onda.QueryExecutorONDA;
import wasdi.shared.opensearch.sobloo.QueryExecutorSOBLOO;
import wasdi.shared.opensearch.viirs.QueryExecutorVIIRS;
import wasdi.shared.utils.AuthenticationCredentials;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorFactory {

	private static final Map<String, Supplier<QueryExecutor>> s_aoExecutors;

	static {
		Utils.debugLog("QueryExecutorFactory");
		final Map<String, Supplier<QueryExecutor>> aoMap = new HashMap<>();

		aoMap.put("ONDA", QueryExecutorONDA::new);
		aoMap.put("SENTINEL", QueryExecutorSENTINEL::new);
		aoMap.put("PROBAV", QueryExecutorPROBAV::new);
		aoMap.put("FEDEO", QueryExecutorFEDEO::new);
		aoMap.put("SOBLOO", QueryExecutorSOBLOO::new);
		aoMap.put("EODC", QueryExecutorEODC::new);
		aoMap.put("CREODIAS", QueryExecutorCREODIAS::new);
		aoMap.put("LSA", QueryExecutorLSA::new);
		aoMap.put("VIIRS", QueryExecutorVIIRS::new);
		
		s_aoExecutors = Collections.unmodifiableMap(aoMap);
		Utils.debugLog("QueryExecutorFactory.static constructor, s_aoExecutors content:");
		for (String sKey : s_aoExecutors.keySet()) {
			Utils.debugLog("QueryExecutorFactory.s_aoExecutors key: " + sKey);
		}
	}

	private QueryExecutor supply(String sProvider) {
		
		//Utils.debugLog("QueryExecutorFactory.QueryExecutor( "+sProvider+" )");
		
		QueryExecutor oExecutor = null;
		if(null!=sProvider) {
			Supplier<QueryExecutor> oSupplier = s_aoExecutors.get(sProvider);
			if(null!=oSupplier) {
				oExecutor = oSupplier.get();
			}
		} else {
			Utils.debugLog("QueryExecutorFactory.QueryExecutor: sProvider is null");
		}
		return oExecutor;	
	}

	public QueryExecutor getExecutor(String sProvider, AuthenticationCredentials oCredentials,
			String sDownloadProtocol, String sGetMetadata, String sParserConfigPath, String sAppConfigPath) {
		
		//Utils.debugLog("QueryExecutorFactory.getExecutor( " + sProvider + ", <credentials>, " + sDownloadProtocol + ", " + sGetMetadata + " )...");
		QueryExecutor oExecutor = null;

		try {
			oExecutor = supply(sProvider);
		} catch (Exception oE) {
			Utils.debugLog("QueryExecutorFactory.getExecutor: " + oE );
		}
		if( null != oExecutor) {
			try {
				oExecutor.setCredentials(oCredentials);
				oExecutor.setMustCollectMetadata(Utils.doesThisStringMeansTrue(sGetMetadata));
				oExecutor.setDownloadProtocol(sDownloadProtocol);
				oExecutor.setParserConfigPath(sParserConfigPath);
				oExecutor.setAppconfigPath(sAppConfigPath);
			} catch (Exception oE1) {
				Utils.debugLog("QueryExecutorFactory.getExecutor: " + oE1 );
			}
		} else {
			throw new NullPointerException("QueryExecutorFactory.getExecutor: could not get a non-null QueryExecutor" );
		}

		return oExecutor;
	}

}
