/**
 * Created by Cristiano Nattero and Alessio Corrado on 2018-11-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.onda;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.abdera.i18n.templates.Template;

import wasdi.shared.opensearch.PaginatedQuery;
import wasdi.shared.opensearch.Platforms;
import wasdi.shared.opensearch.QueryExecutor;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;
import wasdi.shared.viewmodels.QueryViewModel;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorONDA extends QueryExecutor {

	static {
		s_sClassName = "QueryExecutorONDA";
	}
	
	public QueryExecutorONDA() {
		Utils.debugLog(s_sClassName);
		m_sProvider="ONDA";
		this.m_oQueryTranslator = new DiasQueryTranslatorONDA();
		this.m_oResponseTranslator = new DiasResponseTranslatorONDA();
		
		m_asSupportedPlatforms.add(Platforms.SENTINEL1);
		m_asSupportedPlatforms.add(Platforms.SENTINEL2);
		m_asSupportedPlatforms.add(Platforms.SENTINEL3);
		
		m_asSupportedPlatforms.add(Platforms.ENVISAT);
		m_asSupportedPlatforms.add(Platforms.LANDSAT8);
		m_asSupportedPlatforms.add(Platforms.COPERNICUS_MARINE);
		
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getUrlPath()
	 */
	@Override
	protected String[] getUrlPath() {
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getTemplate()
	 */
	@Override
	protected Template getTemplate() {
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getCountUrl(java.lang.String)
	 */
	@Override
	protected String getCountUrl(String sQuery) {
		//Utils.debugLog(s_sClassName + "getCountUrl");
		if(Utils.isNullOrEmpty(sQuery)) {
			Utils.debugLog(s_sClassName + ".getCountUrl: sQuery is null");
		}
		String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products/$count?$search=%22";
		sUrl+=m_oQueryTranslator.translateAndEncode(sQuery);
		sUrl+="%22";
		return sUrl;
	}

	@Override
	protected String getSearchUrl(PaginatedQuery oQuery){
		//Utils.debugLog(s_sClassName + ".BuildUrl( " + oQuery + " )");
		if(null==oQuery) {
			Utils.debugLog(s_sClassName + ".buildUrl: oQuery is null");
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
		//Utils.debugLog(s_sClassName + ".buildUrlForList( " + oQuery + " )");
		if(null==oQuery) {
			Utils.debugLog(s_sClassName + ".buildUrlForList: oQuery is null");
		}
		String sUrl = buildUrlPrefix(oQuery);

		sUrl += "&$expand=Metadata&$select=id,name,creationDate,beginPosition,offline,size,pseudopath,footprint";

		sUrl = buildUrlSuffix(oQuery, sUrl);
		return sUrl;
	}

	private String buildUrlPrefix(PaginatedQuery oQuery) {
		//Utils.debugLog(s_sClassName + ".BuildBaseUrl( " + oQuery + " )");
		if(null==oQuery) {
			throw new NullPointerException(s_sClassName + ".buildBaseUrl: oQuery is null");
		}
		String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products?$search=%22";
		sUrl+=m_oQueryTranslator.translateAndEncode(oQuery.getQuery()) + "%22";
		return sUrl;
	}

	protected String buildUrlSuffix(PaginatedQuery oQuery, String sInUrl) {
		//Utils.debugLog(s_sClassName + ".BuildUrlSuffix( " + oQuery + ", " + sInUrl + " )");
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
	public int executeCount(String sQuery) throws IOException {
		
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
		
		if (m_asSupportedPlatforms.contains(oQueryViewModel.platformName) == false) {
			return 0;
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
		
		String sResult = null;
		String sUrl = null;
		try {
			if(bFullViewModel) {
				sUrl = getSearchUrl(oQuery);
			} else {
				sUrl = getSearchListUrl(oQuery);
			}
			sResult = httpGetResults(sUrl, "search");		
			List<QueryResultViewModel> aoResult = null;
			if(!Utils.isNullOrEmpty(sResult)) {
				aoResult = buildResultViewModel(sResult, bFullViewModel);
				if(null==aoResult) {
					throw new NullPointerException(s_sClassName + ".executeAndRetrieve: aoResult is null"); 
				}
			} else {
				Utils.debugLog(s_sClassName + ".executeAndRetrieve(2 args): could not fetch results for url: " + sUrl);
			}
			return aoResult;
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".executeAndRetrieve(2 args, with sUrl=" + sUrl + "): " + oE);
		}
		return null;
	}


	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery_AssumeFullviewModel) throws IOException {
		Utils.debugLog(s_sClassName + ".executeAndRetrieve("+ oQuery_AssumeFullviewModel + ")");
		return executeAndRetrieve(oQuery_AssumeFullviewModel, true);
	}

	
	@Override
	protected List<QueryResultViewModel> buildResultViewModel(String sJson, boolean bFullViewModel){
		Utils.debugLog(s_sClassName + ".buildResultViewModel( sJson, " + bFullViewModel + " )");
		if(null==sJson ) {
			Utils.debugLog(s_sClassName + ".buildResultLightViewModel: passed a null string");
			throw new NullPointerException(s_sClassName + ".buildResultLightViewModel: passed a null string");
		}
		
		List<QueryResultViewModel> aoResult = m_oResponseTranslator.translateBatch(sJson, bFullViewModel, m_sDownloadProtocol);
		
		if(null == aoResult || aoResult.isEmpty()) {
			Utils.debugLog(s_sClassName + ".buildResultViewModel: no results");
		}
		return aoResult;
	}

}

