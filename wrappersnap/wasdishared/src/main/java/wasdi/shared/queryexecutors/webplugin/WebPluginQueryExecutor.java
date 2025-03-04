package wasdi.shared.queryexecutors.webplugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;

import wasdi.shared.data.MongoRepository;
import wasdi.shared.queryexecutors.ConcreteQueryTranslator;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.queryexecutors.extweb.config.ExtWebConfig;
import wasdi.shared.queryexecutors.extweb.config.ExtWebDataProviderConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class WebPluginQueryExecutor extends QueryExecutor {
	
	protected String m_sWebServiceUrl;
	protected String m_sApiKey;
	
	
	public WebPluginQueryExecutor() {
		m_oQueryTranslator = new ConcreteQueryTranslator();	
	}
	
	@Override
	public void init() {
		super.init();
				
		m_sParserConfigPath = m_oDataProviderConfig.parserConfig;
		WasdiLog.debugLog("WebPluginQueryExecutor:  parser config path is " + m_sParserConfigPath );
		
		try {
			m_sWebServiceUrl = m_oDataProviderConfig.link;
			m_sApiKey = m_oDataProviderConfig.apiKey;
			m_sUser = m_oDataProviderConfig.user;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("WebPluginQueryExecutor: exception reading config files ", oEx);
		}		
	}
	
	@Override
	public int executeCount(String sQuery) {
		
		QueryViewModel oQuery = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
		
		if (oQuery == null) {
			WasdiLog.infoLog("WebPluginQueryExecutor.executeCount: Error parsing the query return -1");
			return -1;			
		}
		
				
		try {
			String sUrl = m_sWebServiceUrl;
			if (!sUrl.endsWith("/")) sUrl += "/";
			sUrl += "query/count";
			
			Map<String, String> asHeaders = HttpUtils.getBasicAuthorizationHeaders(m_sUser, m_sApiKey);
			asHeaders.put("Content-Type", "application/json");			
			
			HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, JsonUtils.stringify(oQuery), asHeaders);
			
			if (oResponse.getResponseCode()>=200 && oResponse.getResponseCode()<=299) {
				int iCount = Integer.parseInt(oResponse.getResponseBody());
				return iCount;
			}
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("WebPluginQueryExecutor.executeCount: exception reading config files ", oEx);
		}
		
		return -1;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		QueryViewModel oQueryVM = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
		
		if (oQueryVM == null) {
			WasdiLog.infoLog("WebPluginQueryExecutor.executeAndRetrieve: Error parsing the query return -1");
			return null;			
		}
		
		List<QueryResultViewModel> aoResults = new ArrayList<>();
		
		try {
			oQueryVM.offset = Integer.parseInt(oQuery.getOffset());
		}
		catch (Exception oEx) {
			WasdiLog.infoLog("WebPluginQueryExecutor.executeAndRetrieve: impossible to parse offset");
			oQueryVM.offset = -1;
		}
		
		try {
			oQueryVM.limit = Integer.parseInt(oQuery.getLimit());
		}
		catch (Exception oEx) {
			WasdiLog.infoLog("WebPluginQueryExecutor.executeAndRetrieve: impossible to parse limit");
			oQueryVM.limit = -1;
		}		
				
		try {
			String sUrl = m_sWebServiceUrl;
			if (!sUrl.endsWith("/")) sUrl += "/";
			sUrl += "query/list";
			
			Map<String, String> asHeaders = HttpUtils.getBasicAuthorizationHeaders(m_sUser, m_sApiKey);
			asHeaders.put("Content-Type", "application/json");
			
			HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, JsonUtils.stringify(oQueryVM), asHeaders);
			
			if (oResponse.getResponseCode()>=200 && oResponse.getResponseCode()<=299) {
				
				aoResults = MongoRepository.s_oMapper.readValue(oResponse.getResponseBody(), new TypeReference<List<QueryResultViewModel>>(){});
				
				// "Sign" the results if it is my provider
				for (QueryResultViewModel oResultVM : aoResults) {
					oResultVM.setProvider(m_sDataProviderCode);
				}
				
				return aoResults;
			}
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("WebPluginQueryExecutor.executeAndRetrieve: exception reading config files ", oEx);
		}
		
		return aoResults;
	}
	
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl, String sPlatform) {
		return sOriginalUrl;
	}	

}
