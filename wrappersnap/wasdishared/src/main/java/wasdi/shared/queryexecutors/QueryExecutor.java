package wasdi.shared.queryexecutors;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.net.io.Util;

import wasdi.shared.utils.AuthenticationCredentials;
import wasdi.shared.utils.Utils;
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
	protected String m_sProvider;
	
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
	 * @return URI to the file, usually an http/https link or a local file path
	 */
	public String getUriFromProductName(String sProduct, String sProtocol) {
		try {
			String sClientQuery = "";
			
			sClientQuery = sProduct + "beginPosition:[1893-09-07T00:00:00.000Z TO 1893-09-07T23:59:59.999Z]&endPosition:[2893-09-07T00:00:00.000Z TO 2893-09-07T23:59:59.999Z]";
			
			PaginatedQuery oQuery = new PaginatedQuery(sClientQuery, null, null, null, null);
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
			Utils.debugLog("QueryExecutor.getUriFromProductName: exception " + oEx.toString());
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
		Utils.debugLog("QueryExecutor.standardHttpGETQuery start");
		String sResult = null;
		long lStart = 0l;
		int iResponseSize = -1;
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			// optional default is GET
			oConnection.setRequestMethod("GET");
			oConnection.setRequestProperty("Accept", "*/*");

			if (m_bUseBasicAuthInHttpQuery) {
				if (m_sUser != null && m_sPassword != null) {
					String sUserCredentials = m_sUser + ":" + m_sPassword;
					String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes("UTF-8"));
					oConnection.setRequestProperty("Authorization", sBasicAuth);
				}				
			}

			Utils.debugLog("\nSending 'GET' request to URL : " + sUrl);

			lStart = System.nanoTime();
			try {
				int iResponseCode = oConnection.getResponseCode();
				Utils.debugLog("QueryExecutor.standardHttpGETQuery: Response Code : " + iResponseCode);
				String sResponseExtract = null;
				if (200 == iResponseCode) {
					InputStream oInputStream = oConnection.getInputStream();
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					if (null != oInputStream) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
					}

					if (sResult != null) {
						if (sResult.length() > 201) {
							sResponseExtract = sResult.substring(0, 200) + "...";
						} else {
							sResponseExtract = new String(sResult);
						}
						Utils.debugLog("QueryExecutor.standardHttpGETQuery: Response extract: " + sResponseExtract);
						iResponseSize = sResult.length();
					} else {
						Utils.debugLog("QueryExecutor.standardHttpGETQuery: reponse is empty");
					}
				} else {
					Utils.debugLog("QueryExecutor.standardHttpGETQuery: provider did not return 200 but "
							+ iResponseCode + " (1/2) and the following message:\n" + oConnection.getResponseMessage());
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					InputStream oErrorStream = oConnection.getErrorStream();
					Util.copyStream(oErrorStream, oBytearrayOutputStream);
					String sMessage = oBytearrayOutputStream.toString();
					if (null != sMessage) {
						sResponseExtract = sMessage.substring(0, Math.min(sMessage.length(), 200)) + "...";
						Utils.debugLog(
								"QueryExecutor.standardHttpGETQuery: provider did not return 200 but " + iResponseCode
										+ " (2/2) and this is the content of the error stream:\n" + sResponseExtract);
						if (iResponseSize <= 0) {
							iResponseSize = sMessage.length();
						}
					}
				}
			} catch (Exception oEint) {
				Utils.debugLog("QueryExecutor.standardHttpGETQuery: " + oEint);
			} finally {
				oConnection.disconnect();
			}

			long lEnd = System.nanoTime();
			long lTimeElapsed = lEnd - lStart;
			double dMillis = lTimeElapsed / (1000.0 * 1000.0);
			double dSpeed = 0;
			if (iResponseSize > 0) {
				dSpeed = ((double) iResponseSize) / dMillis;
				dSpeed *= 1000.0;
			}
			Utils.debugLog("QueryExecutor.standardHttpGETQuery( " + sUrl + " ) performance: " + dMillis + " ms, "
					+ iResponseSize + " B (" + dSpeed + " B/s)");
		} catch (Exception oE) {
			Utils.debugLog("QueryExecutor.standardHttpGETQuery: " + oE);
		}
		return sResult;
	}

	protected String standardHttpPOSTQuery(String sUrl, String sPayload) {
		Utils.debugLog("QueryExecutor.standardHttpPOSTQuery( " + sUrl + " )");
		String sResult = null;
		long lStart = 0l;
		int iResponseSize = -1;
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			// optional default is GET
			oConnection.setRequestMethod("POST");
			oConnection.setRequestProperty("Accept", "*/*");
			
			if (m_bUseBasicAuthInHttpQuery) {
				if (m_sUser != null && m_sPassword != null) {
					String sUserCredentials = m_sUser + ":" + m_sPassword;
					String sBasicAuth = "Basic " + Base64.getEncoder().encodeToString(sUserCredentials.getBytes("UTF-8"));
					oConnection.setRequestProperty("Authorization", sBasicAuth);
				}				
			}

			oConnection.setDoOutput(true);
			byte[] ayBytes = sPayload.getBytes();
			oConnection.setFixedLengthStreamingMode(ayBytes.length);
			oConnection.setRequestProperty("Content-Type", "application/xml");
			oConnection.connect();
			try (OutputStream os = oConnection.getOutputStream()) {
				os.write(ayBytes);
			}

			Utils.debugLog("QueryExecutor.standardHttpPOSTQuery: Sending 'POST' request to URL : " + sUrl);

			lStart = System.nanoTime();
			try {
				int responseCode = oConnection.getResponseCode();
				Utils.debugLog("QueryExecutor.httpGetResults: Response Code : " + responseCode);
				String sResponseExtract = null;
				if (200 == responseCode) {
					InputStream oInputStream = oConnection.getInputStream();
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					if (null != oInputStream) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
					}

					if (sResult != null) {
						if (sResult.length() > 200) {
							sResponseExtract = sResult.substring(0, 200) + "...";
						} else {
							sResponseExtract = new String(sResult);
						}
						Utils.debugLog("QueryExecutor.standardHttpPOSTQuery: Response extract: " + sResponseExtract);
						iResponseSize = sResult.length();
					} else {
						Utils.debugLog("QueryExecutor.standardHttpPOSTQuery: reponse is empty");
					}
				} else {
					Utils.debugLog("QueryExecutor.standardHttpPOSTQuery: provider did not return 200 but "
							+ responseCode + " (1/2) and the following message:\n" + oConnection.getResponseMessage());
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					InputStream oErrorStream = oConnection.getErrorStream();
					Util.copyStream(oErrorStream, oBytearrayOutputStream);
					String sMessage = oBytearrayOutputStream.toString();
					if (null != sMessage) {
						sResponseExtract = sMessage.substring(0, 200) + "...";
						Utils.debugLog(
								"QueryExecutor.standardHttpPOSTQuery: provider did not return 200 but " + responseCode
										+ " (2/2) and this is the content of the error stream:\n" + sResponseExtract);
						if (iResponseSize <= 0) {
							iResponseSize = sMessage.length();
						}
					}
				}
			} catch (Exception oEint) {
				Utils.debugLog("QueryExecutor.standardHttpPOSTQuery: " + oEint);
			} finally {
				oConnection.disconnect();
			}

			long lEnd = System.nanoTime();
			long lTimeElapsed = lEnd - lStart;
			double dMillis = lTimeElapsed / (1000.0 * 1000.0);
			double dSpeed = 0;
			if (iResponseSize > 0) {
				dSpeed = ((double) iResponseSize) / dMillis;
				dSpeed *= 1000.0;
			}
			Utils.debugLog("QueryExecutor.standardHttpPOSTQuery( " + sUrl + " ) performance: " + dMillis + " ms, "
					+ iResponseSize + " B (" + dSpeed + " B/s)");
		} catch (Exception oE) {
			Utils.debugLog("QueryExecutor.standardHttpPOSTQuery: " + oE);
		}
		return sResult;
	}
}
