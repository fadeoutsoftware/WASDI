package wasdi.shared.queryexecutors;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import wasdi.shared.business.AuthenticationCredentials;
import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * Abstract Query Executor. For each Data Provider, a Query Executor is needed.
 * 
 * The goal of this class is: .Convert WASDI Query in Data Provider Query
 * .Execute the query to the data provider .Convert the results from the data
 * provider in the QueryResultViewModel
 * 
 * Public Methods/Services of this class are: .executeCount: get the total
 * number of results of a query .executeAndRetrive: execute the query and get
 * back the results
 * 
 * Each query can be done in two mode: .Paginated: returns a subset of the total
 * results with the full view model .List: returns all the results. In general,
 * can have a light View Model since the number of results can be huge
 * 
 * @author p.campanella
 *
 */
public abstract class QueryExecutor {
		
	/**
	 * Name of the data provider. Must be initialized in the constructor of derived classes
	 */
	protected String m_sDataProviderCode;
	
	/**
	 * User that must be used to authenticate to the data provider, if needed
	 */
	protected String m_sUser;
	
	/**
	 * Password of User that must be used to authenticate to the data provider, if needed
	 */
	protected String m_sPassword;
	
	/**
	 * Link to the parser config json file
	 */
	protected String m_sParserConfigPath;
	
	/**
	 * Link to the app config json file
	 */
	protected String m_sAppConfigPath;
	
	/**
	 * Query Translator to convert search and count WASDI query in the Provider equivalent 
	 */
	protected QueryTranslator m_oQueryTranslator;
	
	/**
	 * Response Translator to convert the count or the results of the Provider in WASDI number or ViewModels
	 */
	protected ResponseTranslator m_oResponseTranslator;
	
	/**
	 * List of platforms supported by the data provider. 
	 * Each derived class must initialize this list.
	 * Strings are taken from the Platoforms enum and represents all the Satellite platforms that WASDI Supports. 
	 */
	protected ArrayList<String> m_asSupportedPlatforms = new ArrayList<String>();
	
	/**
	 * Flag to decide if standard http get and post methods should use Basic Http Authentication or not
	 */
	protected boolean m_bUseBasicAuthInHttpQuery = true;
	
	/**
	 * Data Provider Config
	 */
	protected DataProviderConfig m_oDataProviderConfig = null;
	
	/**
	 * Initialization function.
	 * It is called by the Factory immediatly after the creation of the object.
	 * Can be overridden to custom Providers initializations.
	 */
	public void init() {
		
		
		
		return;
	}

	/**
	 * Abstract executeCount Method. Must be implemented for each Provider.
	 * 
	 * Takes in Input the WASDI Query and must return the total number of results for this provider.
	 * 
	 * @param sQuery WASDI query as comes from client, a string of url parameters
	 * @return Number of results for this query in this provider
	 */
	public abstract int executeCount(String sQuery);	
	
	/**
	 * Abstract executeAndRetrieve method. 
	 * 
	 * Takes in input a Paginated Query object (query + info about pagination) and a flag to know, where is possible
	 * if the query must return a full or a ligth view model.
	 * 
	 * Must translate and foreward the query to the Provider, collect the answer and translate the results. 
	 * 
	 * @param oQuery Paginated Query object 
	 * @param bFullViewModel true to obtain the full view model, false if not
	 * @return List of QueryResultViewModel, one for each  found image 
	 */
	public abstract List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel);
	
	/**
	 * executeAndRetrieve method with full View Model.
	 * 
	 * Calls the abstract one with bFullViewModel = true 
	 * 
	 * @param oQuery Paginated Query object 
	 * @return List of QueryResultViewModel, one for each  found image 
	 */
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery) {
		return executeAndRetrieve(oQuery, true);
	}
	
	/**
	 * Get the URI to a product from the product name and the required protocol.
	 * Protocol can be "https" or "file" (more may happen with new Data Provideres).
	 * 
	 * The method must query the data provider with the extact name of the product
	 * get the result and return the appropriate link to the file.
	 * 
	 * At the moment is expected or an https link that can be used to download or
	 * a local file path to make a copy
	 * 
	 * @param sProduct Exact Name of the product that must be found
	 * @param sProtocol Protocol of interest that determinate the return string (ie http link or local path)
	 * @param sOriginalUrl Original Url available in the DownloadParameter
	 * @param sPlatform declared Platform. If it is null, will try to autodetect it
	 * @return URI to the file, usually an http/https link or a local file path
	 */
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl, String sPlatform) {
		try {
			String sClientQuery = "";
			
			sClientQuery = sProduct + " AND ( beginPosition:[1960-09-07T00:00:00.000Z TO 2193-09-07T23:59:59.999Z] AND endPosition:[1960-09-07T00:00:00.000Z TO 2193-09-07T23:59:59.999Z] ) ";
			
			if (Utils.isNullOrEmpty(sPlatform)) {
				sPlatform = MissionUtils.getPlatformFromSatelliteImageFileName(sProduct);	
			}
			
			if (!Utils.isNullOrEmpty(sPlatform)) {
				sClientQuery = sClientQuery + "AND (platformname:" + sPlatform + " )";
			}
			
			PaginatedQuery oQuery = new PaginatedQuery(sClientQuery, "0", "10", null, null);
			List<QueryResultViewModel> aoResults = executeAndRetrieve(oQuery);
			
			if (aoResults != null) {
				if (aoResults.size()>0) {
					QueryResultViewModel oResult = aoResults.get(0);
					
					if (oResult!=null) {
						return oResult.getLink();
					}
				}
			}
			
			return "";					
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutor.getUriFromProductName: exception " + oEx.toString());
		}
		
		return "";
	}

	/**
	 * Get the list of supported platforms
	 * @return
	 */
	public ArrayList<String> getSupportedPlatforms() {
		return m_asSupportedPlatforms;
	}
	
	/**
	 * Set Provider Credentialts 
	 * @param oCredentials AuthenticationCredentials object (user and password strings)
	 */
	public void setCredentials(AuthenticationCredentials oCredentials) {
		if (null != oCredentials) {
			this.m_sUser = oCredentials.getUser();
			this.m_sPassword = oCredentials.getPassword();
		}

	}
	
	/**
	 * Set Parser config json file path
	 * @param sParserConfigPath path of the Parser config json file  
	 */
	public void setParserConfigPath(String sParserConfigPath) {
		m_sParserConfigPath = sParserConfigPath;
		
		if (m_oQueryTranslator != null) {
			m_oQueryTranslator.setParserConfigPath(sParserConfigPath);
		}
	}
	
	/**
	 * Set App config json file path
	 * @param sAppConfigPath path of the Parser config json file  
	 */
	public void setAppconfigPath(String sAppConfigPath) {
		this.m_sAppConfigPath = sAppConfigPath;
		if (m_oQueryTranslator != null) {
			m_oQueryTranslator.setAppconfigPath(sAppConfigPath);
		}
	}

	protected String standardHttpGETQuery(String sUrl) {
		WasdiLog.debugLog("QueryExecutor.standardHttpGETQuery start");

		HashMap<String, String> asHeaders = new HashMap<String, String>();

		if (m_bUseBasicAuthInHttpQuery) {
			if (m_sUser != null && m_sPassword != null) {
				String sUserCredentials = m_sUser + ":" + m_sPassword;
				try {
					String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes("UTF-8"));
					asHeaders.put("Authorization", sBasicAuth);
				} catch (UnsupportedEncodingException oE) {
					WasdiLog.debugLog("QueryExecutor.standardHttpGETQuery: " + oE);
				}
			}
		}

		long lStart = System.nanoTime();
		HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders); 
		String sResult = oHttpCallResponse.getResponseBody();
		long lEnd = System.nanoTime();

		HttpUtils.logOperationSpeed(sUrl, "standardHttpGETQuery", lStart, lEnd, sResult);
		
		if (oHttpCallResponse.getResponseCode()>=200 && oHttpCallResponse.getResponseCode()<=299) {
			return sResult;
		}
		else {
			return null;
		}
	}
	
	protected String standardHttpPOSTQuery(String sUrl, String sPayload) {
		WasdiLog.debugLog("QueryExecutor.standardHttpPOSTQuery( " + sUrl + " )");

		HashMap<String, String> asHeaders = new HashMap<String, String>();
		asHeaders.put("Content-Type", "application/xml");

		if (m_bUseBasicAuthInHttpQuery) {
			if (m_sUser != null && m_sPassword != null) {
				String sUserCredentials = m_sUser + ":" + m_sPassword;
				try {
					String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes("UTF-8"));
					asHeaders.put("Authorization", sBasicAuth);
				} catch (UnsupportedEncodingException oE) {
					WasdiLog.debugLog("QueryExecutor.standardHttpGETQuery: " + oE);
				}
			}
		}

		long lStart = System.nanoTime();
		HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(sUrl, sPayload, asHeaders); 
		String sResult = oHttpCallResponse.getResponseBody();
		long lEnd = System.nanoTime();

		HttpUtils.logOperationSpeed(sUrl, "standardHttpPOSTQuery", lStart, lEnd, sResult);

		return sResult;
	}

	public String getCode() {
		return m_sDataProviderCode;
	}

	public void setCode(String sCode) {
		
		this.m_sDataProviderCode = sCode;
		m_oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(m_sDataProviderCode);		
	}

	public void closeConnections() {
		
	}
}
