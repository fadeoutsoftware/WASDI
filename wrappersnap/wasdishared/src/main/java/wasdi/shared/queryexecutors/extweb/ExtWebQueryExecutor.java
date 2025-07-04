package wasdi.shared.queryexecutors.extweb;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

public class ExtWebQueryExecutor extends QueryExecutor {
	
	private ExtWebConfig m_oExtWebConfig;
	
	public ExtWebQueryExecutor() {
		m_oQueryTranslator = new ConcreteQueryTranslator();	
	}
	
	@Override
	public void init() {
		super.init();
				
		m_sParserConfigPath = m_oDataProviderConfig.parserConfig;
		WasdiLog.debugLog("ExtWebQueryExecutor:  parser config path is " + m_sParserConfigPath );
		
		try {
			Stream<String> oLinesStream = null;
			
	        try {
	        	
	        	oLinesStream = Files.lines(Paths.get(m_sParserConfigPath), StandardCharsets.UTF_8);
				String sJson = oLinesStream.collect(Collectors.joining(System.lineSeparator()));
				m_oExtWebConfig = MongoRepository.s_oMapper.readValue(sJson,ExtWebConfig.class);
				
				for (ExtWebDataProviderConfig oDataProviderConfig : m_oExtWebConfig.providers) {
					if (!m_asSupportedPlatforms.contains(oDataProviderConfig.mission)) {
						m_asSupportedPlatforms.add(oDataProviderConfig.mission);
						WasdiLog.infoLog("ExtWebQueryExecutor.ExtWebQueryExecutor: adding support to " + oDataProviderConfig.mission);						
					}
				}
				
			} 
	        catch (Exception oEx) {
				WasdiLog.errorLog("ExtWebQueryExecutor.ExtWebQueryExecutor: exception ", oEx);
			} 
	        finally {
				if (oLinesStream != null)  {
					oLinesStream.close();
				}
			}			
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ExtWebQueryExecutor: exception reading config files ", oEx);
		}		
	}
	
	protected ExtWebDataProviderConfig getConfigByMissionName(String sMissionName) {
		for (ExtWebDataProviderConfig oDataProviderConfig : m_oExtWebConfig.providers) {
			if (oDataProviderConfig.mission.equals(sMissionName)) return oDataProviderConfig;
		}
		return null;
	}

	@Override
	public int executeCount(String sQuery) {
		
		QueryViewModel oQuery = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
		
		if (oQuery == null) {
			WasdiLog.infoLog("ExtWebQueryExecutor.executeCount: Error parsing the query return -1");
			return -1;			
		}
		
		ExtWebDataProviderConfig oConfig =  getConfigByMissionName(oQuery.platformName);
		
		if (oConfig == null) {
			WasdiLog.infoLog("ExtWebQueryExecutor.executeCount: cannot find config for mission " + oQuery.platformName);
			return -1;
		}
		
		try {
			String sUrl = oConfig.baseUrl;
			if (!sUrl.endsWith("/")) sUrl += "/";
			sUrl += "query/count";
			
			Map<String, String> asHeaders = new HashMap<>();
			asHeaders.put("Content-Type", "application/json");			
			
			HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, JsonUtils.stringify(oQuery), asHeaders);
			
			if (oResponse.getResponseCode()>=200 && oResponse.getResponseCode()<=299) {
				int iCount = Integer.parseInt(oResponse.getResponseBody());
				return iCount;
			}
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ExtWebQueryExecutor.executeCount: exception reading config files ", oEx);
		}
		
		return -1;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		QueryViewModel oQueryVM = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
		
		if (oQueryVM == null) {
			WasdiLog.infoLog("ExtWebQueryExecutor.executeAndRetrieve: Error parsing the query return -1");
			return null;			
		}
		
		List<QueryResultViewModel> aoResults = new ArrayList<>();
		
		try {
			oQueryVM.offset = Integer.parseInt(oQuery.getOffset());
		}
		catch (Exception oEx) {
			WasdiLog.infoLog("ExtWebQueryExecutor.executeAndRetrieve: impossible to parse offset");
			oQueryVM.offset = -1;
		}
		
		try {
			oQueryVM.limit = Integer.parseInt(oQuery.getLimit());
		}
		catch (Exception oEx) {
			WasdiLog.infoLog("ExtWebQueryExecutor.executeAndRetrieve: impossible to parse limit");
			oQueryVM.limit = -1;
		}		
				
		ExtWebDataProviderConfig oConfig =  getConfigByMissionName(oQueryVM.platformName);
		
		if (oConfig == null) {
			WasdiLog.infoLog("ExtWebQueryExecutor.executeAndRetrieve: cannot find config for mission " + oQueryVM.platformName);
			return null;
		}
		
		try {
			String sUrl = oConfig.baseUrl;
			if (!sUrl.endsWith("/")) sUrl += "/";
			sUrl += "query/list";
			
			Map<String, String> asHeaders = new HashMap<>();
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
			WasdiLog.errorLog("ExtWebQueryExecutor.executeAndRetrieve: exception reading config files ", oEx);
		}
		
		return aoResults;
	}
	
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl, String sPlatform) {
		
		return sOriginalUrl;
	}	

}
