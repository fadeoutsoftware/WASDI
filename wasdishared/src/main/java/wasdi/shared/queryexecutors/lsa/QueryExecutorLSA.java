package wasdi.shared.queryexecutors.lsa;

import java.io.StringReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
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
		this.m_oQueryTranslator = new QueryTranslatorLSA();
		this.m_oResponseTranslator = new ResponseTranslatorLSA();
		
	}
	
	@Override
	public void init() {
		super.init();
		
		try {
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(m_sParserConfigPath);
			boolean bEnableFast24 = oAppConf.getBoolean("enableFast24");
			
			((QueryTranslatorLSA)m_oQueryTranslator).setEnableFast24(bEnableFast24);
			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorLSA.init(): exception reading parser config file " + m_sParserConfigPath);
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
			return -1;
		}
		
		String sLSAQuery = m_oQueryTranslator.translateAndEncodeParams(sQuery);
		
		if (Utils.isNullOrEmpty(sLSAQuery)) return -1;
		
		if (!m_bAuthenticated) {
			LSAHttpUtils.authenticate(m_sUser, m_sPassword);
			m_bAuthenticated = true;
		}
		
		// Make the query
		String sRLSAResults = LSAHttpUtils.httpGetResults(sLSAQuery, (CookieManager) CookieHandler.getDefault());
		
		WasdiLog.debugLog("QueryExecutorLSA.executeCount: get Results, extract the total count");
		DocumentBuilderFactory oDocBuildFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder oDocBuilder;		
		
		try {
			
			oDocBuilder = oDocBuildFactory.newDocumentBuilder();
			
			Document oResultsXml = oDocBuilder.parse(new InputSource(new StringReader(sRLSAResults)));
			
			oResultsXml.getDocumentElement().normalize();
			
			WasdiLog.debugLog("QueryExecutorLSA.executeCount root element: " + oResultsXml.getDocumentElement().getNodeName());
			
			// loop through each Entry item
			NodeList aoItems = oResultsXml.getElementsByTagName("os:totalResults");
			
			for (int iItem = 0; iItem < aoItems.getLength(); iItem++)
			{
				// We need an Element Node
				org.w3c.dom.Node oNode = aoItems.item(iItem);
				if (oNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)  continue;
				
				Element oEntry = (Element) oNode;
				
				String sCount = oEntry.getTextContent();
				iCount = Integer.parseInt(sCount);
			}
			
			WasdiLog.debugLog("QueryExecutorLSA.executeCount: Search Done: found " + iCount + " results");		
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorLSA.executeCount: Exception = " + oEx.toString());
			iCount = -1;
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
			return null;
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
			
			if (sRLSAResults ==null) {
				return null;
			}
			
			WasdiLog.debugLog("QueryExecutorLSA.executeAndRetrieve: got result, start conversion");
			
			return m_oResponseTranslator.translateBatch(sRLSAResults, bFullViewModel);
		
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorLSA.executeAndRetrieve: Exception = " + oEx.toString());
		}		

		
		return aoReturnList;
	}
	
	@Override
	public void closeConnections() {
		if (m_bAuthenticated) {
			WasdiLog.debugLog("QueryExecutorLSA.closeConnections: calling logut");
			LSAHttpUtils.logout();
			m_bAuthenticated = false;
		}
		else {
			WasdiLog.debugLog("QueryExecutorLSA.closeConnections: not authenticated");
		}
	}
}
