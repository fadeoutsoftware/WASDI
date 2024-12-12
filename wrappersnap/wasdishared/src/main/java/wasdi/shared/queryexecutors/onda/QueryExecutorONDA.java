/**
 * Created by Cristiano Nattero and Alessio Corrado on 2018-11-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors.onda;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.opensearch.QueryExecutorOpenSearch;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * Query Executor for ONDA.
 * 
 * ONDA uses OpenSearch, and this class is derived by the appropriate QueryExecutorOpenSearch
 * 
 * ONDA should be able to serve file both in http and file system
 * 
 * @author c.nattero
 *
 */
public class QueryExecutorONDA extends QueryExecutorOpenSearch {
	
	private static final List<String> s_asUnsupportedProductTypes = Arrays.asList("SL_2_AOD___", "SL_2_FRP___");
	
	public QueryExecutorONDA() {
		m_sProvider="ONDA";
		this.m_oQueryTranslator = new QueryTranslatorONDA();
		this.m_oResponseTranslator = new ResponseTranslatorONDA();
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getUrlPath()
	 */
	@Override
	protected String[] getUrlPath() {
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getCountUrl(java.lang.String)
	 */
	@Override
	protected String getCountUrl(String sQuery) {
		if(Utils.isNullOrEmpty(sQuery)) {
			WasdiLog.debugLog("QueryExecutorONDA.getCountUrl: sQuery is null");
		}
		String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products/$count?$search=%22";
		sUrl+=m_oQueryTranslator.translateAndEncodeParams(sQuery);
		sUrl+="%22";
		return sUrl;
	}

	@Override
	protected String getSearchUrl(PaginatedQuery oQuery){
		if(null==oQuery) {
			WasdiLog.debugLog("QueryExecutorONDA.getSearchUrl: oQuery is null");
		}
		String sUrl = buildUrlPrefix(oQuery);		

		String sMetadata = "$expand=Metadata";
		if(!Utils.isNullOrEmpty(sMetadata)) {
			sUrl += "&" + sMetadata;
		}
		sUrl = buildUrlSuffix(oQuery, sUrl);
		return sUrl;
	}

	@Override
	protected String getSearchListUrl(PaginatedQuery oQuery) {
		if(null==oQuery) {
			WasdiLog.debugLog("QueryExecutorONDA.getSearchListUrl: oQuery is null");
		}
		String sUrl = buildUrlPrefix(oQuery);

		sUrl += "&$expand=Metadata&$select=id,name,creationDate,beginPosition,offline,size,pseudopath,footprint";

		sUrl = buildUrlSuffix(oQuery, sUrl);
		return sUrl;
	}

	private String buildUrlPrefix(PaginatedQuery oQuery) {
		if(null==oQuery) {
			throw new NullPointerException("QueryExecutorONDA.buildUrlPrefix: oQuery is null");
		}
		String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products?$search=%22";
		sUrl+=m_oQueryTranslator.translateAndEncodeParams(oQuery.getQuery()) + "%22";
		return sUrl;
	}

	protected String buildUrlSuffix(PaginatedQuery oQuery, String sInUrl) {
		String sUrl = sInUrl;
		sUrl+="&$top=" + oQuery.getLimit() + "&$skip="+ oQuery.getOffset();

		String sFormat = "$format=json";
		if(!Utils.isNullOrEmpty(sFormat)) {
			sUrl += "&" + sFormat;
		}

		String sOrderBy = oQuery.getSortedBy();
		//XXX do not use hardcoded values
		sOrderBy = "$orderby=creationDate";
		if(!Utils.isNullOrEmpty(sOrderBy)) {
			sUrl += "&" + sOrderBy;
		}
		return sUrl;
	}


	@Override
	public int executeCount(String sQuery) {
		
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
		
		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			return -1;
		}
		
		// two product types are not supported by ONDA. In this case, we return -1
		if (!Utils.isNullOrEmpty(oQueryViewModel.productType) 
				&& (s_asUnsupportedProductTypes.contains(oQueryViewModel.productType))) {
			WasdiLog.debugLog("QueryExecutorONDA.executeAndRetrieve. The following product type is not supported by the provider: " + oQueryViewModel.productType);
			return -1;
		}
		
		return super.executeCount(sQuery);
	}


	/**
	 * @param sUrl
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@Override
	protected String encodeAsRequired(String sUrl) throws UnsupportedEncodingException {
		return sUrl;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
		
		if (m_asSupportedPlatforms.contains(oQueryViewModel.platformName) == false) {
			return new ArrayList<QueryResultViewModel>();
		}	
		
		// two product types are not supported by ONDA. In this case, we return null
		if (!Utils.isNullOrEmpty(oQueryViewModel.productType) 
				&& (s_asUnsupportedProductTypes.contains(oQueryViewModel.productType))) {
			WasdiLog.debugLog("QueryExecutorONDA.executeAndRetrieve. The following product type is not supported by the provider: " + oQueryViewModel.productType);
			return null;
		}
		
		String sResult = null;
		String sUrl = null;
		try {
			if(bFullViewModel) {
				sUrl = getSearchUrl(oQuery);
			} else {
				sUrl = getSearchListUrl(oQuery);
			}
			sResult = standardHttpGETQuery(sUrl);		
			List<QueryResultViewModel> aoResult = null;
			if(!Utils.isNullOrEmpty(sResult)) {
				aoResult = buildResultViewModel(sResult, bFullViewModel);
				if(null==aoResult) {
					throw new NullPointerException("QueryExecutorONDA.executeAndRetrieve: aoResult is null"); 
				}
			} else {
				WasdiLog.debugLog("QueryExecutorONDA.executeAndRetrieve(2 args): could not fetch results for url: " + sUrl);
			}
			return aoResult;
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryExecutorONDA.executeAndRetrieve(2 args, with sUrl=" + sUrl + "): " + oE);
		}
		return null;
	}
	
	@Override
	protected List<QueryResultViewModel> buildResultViewModel(String sJson, boolean bFullViewModel){
		WasdiLog.debugLog("QueryExecutorONDA.buildResultViewModel( sJson, " + bFullViewModel + " )");
		if(null==sJson ) {
			WasdiLog.debugLog("QueryExecutorONDA.buildResultLightViewModel: passed a null string");
			throw new NullPointerException("QueryExecutorONDA.buildResultLightViewModel: passed a null string");
		}
		
		List<QueryResultViewModel> aoResult = m_oResponseTranslator.translateBatch(sJson, bFullViewModel);
		
		if(null == aoResult || aoResult.isEmpty()) {
			WasdiLog.debugLog("QueryExecutorONDA.buildResultViewModel: no results");
		}
		return aoResult;
	}


}

