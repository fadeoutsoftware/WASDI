package wasdi.shared.queryexecutors.lsa;

import java.io.StringReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.ParserOptions;
import org.json.JSONObject;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * Query Executor for LSA Data Center.
 * 
 * LSA Data Center use OData. 
 * Standard http get cannot be used due to a dedicated authentication method.
 * This method is implemetned in the LSAHttpUtils that allows to login and add 
 * the correct headers to the API calls.
 * 
 * LSA data center can provide images via http or in a local disk
 * 
 * @author p.campanella
 *
 */
public class QueryExecutorLSA extends QueryExecutor {
	
	boolean m_bAuthenticated = false;
	
	public QueryExecutorLSA() {
		m_sProvider="LSA";
		this.m_oQueryTranslator = new QueryTranslatorLSA();
		this.m_oResponseTranslator = new ResponseTranslatorLSA();
		
		m_asSupportedPlatforms.add(Platforms.SENTINEL1);
		m_asSupportedPlatforms.add(Platforms.SENTINEL2);
	}
	
	@Override
	public void init() {
		super.init();
		
		try {
			JSONObject oAppConf = WasdiFileUtils.loadJsonFromFile(m_sParserConfigPath);
			boolean bEnableFast24 = oAppConf.getBoolean("enableFast24");
			
			((QueryTranslatorLSA)m_oQueryTranslator).setEnableFast24(bEnableFast24);
			
		}
		catch (Exception oEx) {
			Utils.debugLog("QueryExecutorLSA.init(): exception reading parser config file " + m_sParserConfigPath);
		}
	}
		
	/**
	 * Executes the count 
	 */
	@Override
	public int executeCount(String sQuery) {
		
		int iCount = 0;
		
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
		
		if (m_asSupportedPlatforms.contains(oQueryViewModel.platformName) == false) {
			return 0;
		}
		
		String sLSAQuery = m_oQueryTranslator.translateAndEncodeParams(sQuery);
		
		if (Utils.isNullOrEmpty(sLSAQuery)) return 0;
		
		if (!m_bAuthenticated) {
			LSAHttpUtils.authenticate(m_sUser, m_sPassword);
			m_bAuthenticated = true;
		}
		
		// Make the query
		String sRLSAResults = LSAHttpUtils.httpGetResults(sLSAQuery, (CookieManager) CookieHandler.getDefault());
		
		Utils.debugLog("QueryExecutorLSA: get Results, extract the total count");
		
		try {
			
			// Parse results with abdera
			Abdera oAbdera = new Abdera();
			
			Parser oParser = oAbdera.getParser();
			ParserOptions oParserOptions = oParser.getDefaultParserOptions();
			
			oParserOptions.setCharset("UTF-8");
			oParserOptions.setFilterRestrictedCharacterReplacement('_');
			oParserOptions.setFilterRestrictedCharacters(true);
			oParserOptions.setMustPreserveWhitespace(false);
			oParserOptions.setParseFilter(null);

			org.apache.abdera.model.Document<Feed> oDocument = null;
			
			oDocument = oParser.parse(new StringReader(sRLSAResults), oParserOptions);
			
			if (oDocument == null) {
				Utils.debugLog("QueryExecutorLSA.executeCount: Document response null, aborting");
				return 0;
			}
			
			// Extract the count
			Feed oFeed = (Feed) oDocument.getRoot();
			String sText = null;
			for (Element oElement : oFeed.getElements()) {
				if (oElement.getQName().getLocalPart().equals("totalResults")) {
					sText = oElement.getText();
					break;
				}
			} 
			
			Utils.debugLog("QueryExecutorLSA.executeCount: " + sText);
			
			try {
				// Cast the result
				iCount = Integer.parseInt(sText);
			}
			catch (Exception oEx) {
				Utils.debugLog("QueryExecutorLSA.executeCount: Exception = " + oEx.toString());
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("QueryExecutorLSA.executeCount: Exception = " + oEx.toString());
		}
		
		return iCount;
	}
	

	/**
	 * Execute an LSA Data Center Query and return the result in WASDI format
	 */
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		ArrayList<QueryResultViewModel> aoReturnList = new ArrayList<QueryResultViewModel>();
		
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
		
		if (m_asSupportedPlatforms.contains(oQueryViewModel.platformName) == false) {
			return aoReturnList;
		}		
		
		try {
			
			String sQuery = oQuery.getQuery();
			
			if (!sQuery.contains("&offset")) sQuery += "&offset=" + oQuery.getOffset();
			if (!sQuery.contains("&limit")) sQuery += "&limit=" + oQuery.getLimit();
			
			String sLSAQuery = m_oQueryTranslator.translateAndEncodeParams(sQuery);
			
			if (Utils.isNullOrEmpty(sLSAQuery)) return aoReturnList;
			
			if (!m_bAuthenticated) {
				LSAHttpUtils.authenticate(m_sUser, m_sPassword);
				m_bAuthenticated = true;
			}			
			
			// Make the query
			String sRLSAResults = LSAHttpUtils.httpGetResults(sLSAQuery, (CookieManager) CookieHandler.getDefault());
			
			Utils.debugLog("QueryExecutorLSA.executeAndRetrieve: got result, start conversion");
			
			return m_oResponseTranslator.translateBatch(sRLSAResults, bFullViewModel);
		
		}
		catch (Exception oEx) {
			Utils.debugLog("QueryExecutorLSA.executeAndRetrieve: Exception = " + oEx.toString());
		}		

		
		return aoReturnList;
	}
}
