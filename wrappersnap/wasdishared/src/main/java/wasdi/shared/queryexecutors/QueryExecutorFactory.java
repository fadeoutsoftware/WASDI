/**
 * Created by Cristiano Nattero on 2019-02-06
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import wasdi.shared.business.AuthenticationCredentials;
import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorFactory {

	private static final Map<String, QueryExecutor> s_aoExecutors = new HashMap<>();

	static {
		WasdiLog.debugLog("QueryExecutorFactory");
		
		ArrayList<DataProviderConfig> aoDataProviders = WasdiConfig.Current.dataProviders;
		
		for (DataProviderConfig oDPConfig : aoDataProviders) {
			try {
				String sName = oDPConfig.name;
				QueryExecutor oQueryExecutor = (QueryExecutor) Class.forName(oDPConfig.classpath).getDeclaredConstructor().newInstance();
				s_aoExecutors.put(sName, oQueryExecutor);
			} catch (Exception oEx) {
				WasdiLog.errorLog("QueryExecutorFactory.static: exception creating a Query Executor: " + oEx.toString());
			} 
		}
		
		try {
			WasdiLog.debugLog("QueryExecutorFactory.static constructor, s_aoExecutors content:");
			String sExecutors = "";
			
			for (String sKey : s_aoExecutors.keySet()) {
				sExecutors += sKey + " - ";
			}
			if (!Utils.isNullOrEmpty(sExecutors)) {
				if (sExecutors.length()>3) {
					sExecutors = sExecutors.substring(0, sExecutors.length()-3);
				}
			}
			WasdiLog.debugLog("QueryExecutorFactory.s_aoExecutors key: " + sExecutors);			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorFactory.static constructor exception " + oEx.toString());
		}
	}

	private static QueryExecutor supply(String sProvider) {
		
		QueryExecutor oExecutor = null;
		if(null!=sProvider) {
			oExecutor = s_aoExecutors.get(sProvider);
		} else {
			WasdiLog.debugLog("QueryExecutorFactory.QueryExecutor: sProvider is null");
		}
		return oExecutor;	
	}

	private static QueryExecutor getExecutor(String sProvider, AuthenticationCredentials oCredentials, String sParserConfigPath, String sAppConfigPath) {
		
		QueryExecutor oExecutor = null;

		try {
			oExecutor = supply(sProvider);
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryExecutorFactory.getExecutor: " + oE );
		}
		if( null != oExecutor) {
			try {
				oExecutor.setCredentials(oCredentials);
				oExecutor.setParserConfigPath(sParserConfigPath);
				oExecutor.setAppconfigPath(sAppConfigPath);
			} catch (Exception oE1) {
				WasdiLog.debugLog("QueryExecutorFactory.getExecutor: " + oE1 );
			}
		} else {
			throw new NullPointerException("QueryExecutorFactory.getExecutor: could not get a non-null QueryExecutor" );
		}

		return oExecutor;
	}
	
	
	/**
	 * Get the Query Executor for a specific provider
	 * @param sProvider Provider code
	 * @return QueryExecutor of the specific provider
	 */
	public static  QueryExecutor getExecutor(String sProvider) {
		WasdiLog.debugLog("QueryExecutorFactory.getExecutor, provider: " + sProvider);
		QueryExecutor oExecutor = null;
		try {
			if(null!=sProvider) {
				AuthenticationCredentials oCredentials = getCredentials(sProvider);
				
				DataProviderConfig oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(sProvider);
	
				String sParserConfigPath = oDataProviderConfig.parserConfig;
				String sAppConfigPath = WasdiConfig.Current.paths.missionsConfigFilePath;
				
				oExecutor = getExecutor(
						sProvider,
						oCredentials,
						sParserConfigPath, sAppConfigPath);
				
				oExecutor.getSupportedPlatforms().clear();
				
				for (String sSupportedPlatform : oDataProviderConfig.supportedPlatforms) {
					oExecutor.getSupportedPlatforms().add(sSupportedPlatform);
				}
				
				oExecutor.init();
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryExecutorFactory.getExecutor( " + sProvider + " ): " + oE);
		}
		return oExecutor;

	}

	/**
	 * Get Auth Credentials for a specific provider
	 * @param sProvider Provider Code
	 * @return AuthenticationCredentials entity
	 */
	private static  AuthenticationCredentials getCredentials(String sProvider) {
		
		AuthenticationCredentials oCredentials = null;
		try {
			DataProviderConfig oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(sProvider);
			
			oCredentials = new AuthenticationCredentials(oDataProviderConfig.user, oDataProviderConfig.password, oDataProviderConfig.apiKey);
			
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryExecutorFactory.getCredentials( " + sProvider + " ): " + oE);
		}
		return oCredentials;
	}	

}
