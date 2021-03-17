package wasdi.shared.opensearch.lsa;

import java.io.IOException;
import java.io.StringReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.i18n.templates.Template;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.Parser;
import org.apache.abdera.parser.ParserOptions;
import org.json.JSONObject;

import wasdi.shared.opensearch.PaginatedQuery;
import wasdi.shared.opensearch.QueryExecutor;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.viewmodels.QueryResultViewModel;

public class QueryExecutorLSA extends QueryExecutor {
	
	boolean m_bAuthenticated = false;
	
	static {
		s_sClassName = "LSA";
	}

	public QueryExecutorLSA() {
		Utils.debugLog(s_sClassName);
		m_sProvider=s_sClassName;
		this.m_oQueryTranslator = new DiasQueryTranslatorLSA();
		this.m_oResponseTranslator = new DiasResponseTranslatorLSA();
	}
	
	@Override
	public void init() {
		super.init();
		
		try {
			JSONObject oAppConf = WasdiFileUtils.loadJsonFromFile(m_sParserConfigPath);
			boolean bEnableFast24 = oAppConf.getBoolean("enableFast24");
			
			((DiasQueryTranslatorLSA)m_oQueryTranslator).setEnableFast24(bEnableFast24);
			
		}
		catch (Exception oEx) {
			Utils.debugLog("QueryExecutorLSA.init(): exception reading parser config file " + m_sParserConfigPath);
		}
	}
	

	@Override
	protected String[] getUrlPath() {
		return null;
	}

	@Override
	protected Template getTemplate() {
		return null;
	}

	@Override
	protected String getCountUrl(String sQuery) {
		return null;
	}
	
	/**
	 * Executes the count 
	 */
	@Override
	public int executeCount(String sQuery) throws IOException {
		
		int iCount = 0;
		
		String sLSAQuery = m_oQueryTranslator.translateAndEncode(sQuery);
		
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
		
		try {
			
			String sQuery = oQuery.getQuery();
			
			if (!sQuery.contains("&offset")) sQuery += "&offset=" + oQuery.getOffset();
			if (!sQuery.contains("&limit")) sQuery += "&limit=" + oQuery.getLimit();
			
			String sLSAQuery = m_oQueryTranslator.translateAndEncode(sQuery);
			
			if (!m_bAuthenticated) {
				LSAHttpUtils.authenticate(m_sUser, m_sPassword);
				m_bAuthenticated = true;
			}			
			
			// Make the query
			String sRLSAResults = LSAHttpUtils.httpGetResults(sLSAQuery, (CookieManager) CookieHandler.getDefault());
			
			Utils.debugLog("QueryExecutorLSA.executeAndRetrieve: got result, start conversion");
			
			return m_oResponseTranslator.translateBatch(sRLSAResults, bFullViewModel, "");
		
		}
		catch (Exception oEx) {
			Utils.debugLog("QueryExecutorLSA.executeAndRetrieve: Exception = " + oEx.toString());
		}		

		
		return aoReturnList;
	}

}
