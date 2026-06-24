package wasdi.shared.queryexecutors.globathywasdi;

import java.util.ArrayList;
import java.util.List;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.wasdiAPI.AuthAPIClient;
import wasdi.shared.utils.wasdiAPI.SearchAPIClient;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class QueryExecutorGlobathyWasdi extends QueryExecutor {
	
	private String m_sWasdiUrl = "";
	private String m_sWasdiUser = "";
	private String m_sWasdiPassword = "";
	private String m_sSessionId = "";
	
	private static final Object s_oShapeFileLock = new Object();


	public QueryExecutorGlobathyWasdi() {
		this.m_oQueryTranslator = new QueryTranslatoryGlobathyWasdi();
		this.m_oResponseTranslator = new ResponseTranslatorGlobathyWasdi();
		this.m_asSupportedPlatforms.add(Platforms.GLOBATHYWASDI);
	}
	
	
	@Override
	public void init() {
		super.init();
		
		try {
			
			m_sWasdiUrl = this.m_oDataProviderConfig.link;
			m_sWasdiUser = this.m_sUser;
			m_sWasdiPassword = this.m_sPassword;
			
			m_sSessionId = AuthAPIClient.login(m_sWasdiUser, m_sWasdiPassword, m_sWasdiUrl);
			
			if (Utils.isNullOrEmpty(m_sSessionId)) {
				WasdiLog.errorLog("QueryExecutorGlobathyWasdi.init: impossible to get the session id for url  " + m_sWasdiUrl + " user " + m_sWasdiUser);
			}
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorGlobathyWasdi.init. Exception reading parser config file " + m_sParserConfigPath, oEx);
		}
	}
	
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl, String sPlatform) {
		if (sProduct.toLowerCase().endsWith("_bathymetry.tif")) {
			return sOriginalUrl;
		}
		return null;
	}

	
	@Override
	public int executeCount(String sQuery) {
		
		int iCount = -1;
		
		try {
			iCount = SearchAPIClient.count(m_sSessionId, "AUTO", sQuery, m_sWasdiUrl);
		}
		catch (Exception oE) {
			WasdiLog.errorLog("QueryExecutorGlobathyWasdi.executeCount. Exception ", oE);
		}
		
		return iCount;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
			
		List<QueryResultViewModel> aoResults = null;
		
		try {
			QueryResultViewModel [] aoQueryRes = SearchAPIClient.searchList(m_sSessionId, "AUTO", oQuery.getQuery(), m_sWasdiUrl);
			
			if (aoQueryRes != null) {
				aoResults = new ArrayList<>();
				for (QueryResultViewModel oRes: aoQueryRes) aoResults.add(oRes);				
			}
		} 
		catch (Exception oE) {
			WasdiLog.errorLog("QueryExecutorGlobathyWasdi.executeAndRetrieve. Exception ", oE);
		}	
		
		return aoResults;
	}

}
