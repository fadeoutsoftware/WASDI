/**
 * Created by Cristiano Nattero on 2020-01-21
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.sobloo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.abdera.i18n.templates.Template;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.opensearch.PaginatedQuery;
import wasdi.shared.opensearch.Platforms;
import wasdi.shared.opensearch.QueryExecutor;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorSOBLOO extends QueryExecutor {

	protected String s_sBaseUrl = "https://sobloo.eu/api/v1/services/search?";
	
	static {
		s_sClassName = "QueryExecutorSOBLOO";
	}

	public QueryExecutorSOBLOO() {
		Utils.debugLog(s_sClassName);
		m_sProvider="SOBLOO";
		this.m_oQueryTranslator = new DiasQueryTranslatorSOBLOO();
		this.m_oResponseTranslator = new DiasResponseTranslatorSOBLOO();
		
		this.m_sUser = null;
		this.m_sPassword = null;
		
		m_asSupportedPlatforms.add(Platforms.SENTINEL1);
		m_asSupportedPlatforms.add(Platforms.SENTINEL2);
		m_asSupportedPlatforms.add(Platforms.SENTINEL3);
	}

	@Override
	public void setParserConfigPath(String sParserConfigPath) {
		super.setParserConfigPath(sParserConfigPath);
		this.m_oQueryTranslator.setParserConfigPath(this.m_sParserConfigPath);
	}

	@Override
	public void setAppconfigPath(String sAppconfigPath) {
		super.setAppconfigPath(sAppconfigPath);
		this.m_oQueryTranslator.setAppconfigPath(this.m_sAppConfigPath);
	}
	
	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getUrlPath()
	 */
	@Override
	protected String[] getUrlPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String extractNumberOfResults(String sResponse) {
		Preconditions.checkNotNull(sResponse, "QueryExecutorSOBLOO.extractNumberOfResults: sResponse is null");
		String sResult = null;
		try {
			JSONObject oJson = new JSONObject(sResponse);
			sResult = "" + oJson.optInt("totalnb", -1);
		} catch (Exception oE) {
			Utils.debugLog("QueryExecutorSOBLOO.extractNumberOfResults( " + sResponse + " ): " + oE);
		}
		return sResult;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#executeAndRetrieve(wasdi.shared.opensearch.PaginatedQuery, boolean)
	 */
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		Preconditions.checkNotNull(this.m_sAppConfigPath, "QueryExecutorSOBLOO.executeAndRetrieve: app config path is null");
		Preconditions.checkNotNull(this.m_sParserConfigPath, "QueryExecutorSOBLOO.executeAndRetrieve: parser config path is null");
		
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
		
		if (m_asSupportedPlatforms.contains(oQueryViewModel.platformName) == false) {
			return new ArrayList<QueryResultViewModel>();
		}		
		
//		this.m_oQueryTranslator.setParserConfigPath(this.m_sParserConfigPath);
//		this.m_oQueryTranslator.setAppconfigPath(this.m_sAppConfigPath);
		
		Utils.debugLog(s_sClassName + ".executeAndRetrieve(" + oQuery + ", " + bFullViewModel + ")");
		String sResult = null;
		String sUrl = null;
		try {
			if(bFullViewModel) {
				sUrl = getSearchUrl(oQuery);
			} else {
				sUrl = getSearchListUrl(oQuery);
			}
			this.m_sUser = null;
			this.m_sPassword = null;
			sResult = httpGetResults(sUrl, "search");		
			List<QueryResultViewModel> aoResult = null;
			if(!Utils.isNullOrEmpty(sResult)) {
				//todo build result view model
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
	protected String getSearchUrl(PaginatedQuery oQuery){
		Preconditions.checkNotNull(oQuery, s_sClassName + ".getSearchUrl: query is null");
		
		//todo add thumbnail
		return getSearchListUrl(oQuery);
		
	}

	@Override
	protected String getSearchListUrl(PaginatedQuery oQuery) {
		Utils.debugLog("QueryExecutorSOBLOO.getSearchListUrl( " + oQuery.getQuery() + " )");
		
		Preconditions.checkNotNull(oQuery, s_sClassName + ".getSearchListUrl: query is null");
//		readyQueryTranslator();
		
		return s_sBaseUrl + m_oQueryTranslator.translateAndEncode(oQuery.getQuery()) +
				"&size=" + oQuery.getLimit()+ "&from=" + oQuery.getOffset() +
				"&pretty=false&flat=false&sort=-timeStamp,uid&max-age-cache=120";
	}


	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getCountUrl(java.lang.String)
	 */
	@Override
	protected String getCountUrl(String sQuery) {
		//Utils.debugLog("QueryExecutorSOBLOO.getCountUrl( " + sQuery + " )");
		
		Preconditions.checkNotNull(m_sAppConfigPath, "QueryExecutorSOBLOO.getCountUrl: app config path is null");
		Preconditions.checkNotNull(m_sParserConfigPath, "QueryExecutorSOBLOO.getCountUrl: parser config path is null");
		
		Preconditions.checkNotNull(sQuery, "QueryExecutorSOBLOO.getCountUrl: sQuery is null");
		String sUrl = "";
		try {
//			readyQueryTranslator();
			sUrl = "https://sobloo.eu/api/v1/services/explore/explore/catalog/_count?" + m_oQueryTranslator.translateAndEncode(sQuery);
		} catch (Exception oE) {
			Utils.log("ERROR", "QueryExecutorSOBLOO.getCountUrl: " + oE);
		}
		return sUrl;
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

	/**
	 * 
	 */
	protected void readyQueryTranslator() {
		Preconditions.checkNotNull(m_sAppConfigPath, "QueryExecutorSOBLOO.readyQueryTranslator: app config path is null");
		Preconditions.checkNotNull(m_sParserConfigPath, "QueryExecutorSOBLOO.readyQueryTranslator: parser config path is null");
		
//		m_oQueryTranslator.setAppconfigPath(this.m_sAppConfigPath);
//		m_oQueryTranslator.setParserConfigPath(this.m_sParserConfigPath);
	}

	
	
	@Override
	public int executeCount(String sQuery) throws IOException {
		m_sUser = null;
		m_sPassword = null;
		
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
		
		if (m_asSupportedPlatforms.contains(oQueryViewModel.platformName) == false) {
			return 0;
		}
		
		return super.executeCount(sQuery);
	}



	@Override
	protected List<QueryResultViewModel> buildResultViewModel(String sJson, boolean bFullViewModel){
		Preconditions.checkNotNull(sJson, s_sClassName + ".buildResultLightViewModel: passed a null string");

		
		Utils.debugLog("QueryExecutorSOBLOO.buildResultViewModel( sJson, " + bFullViewModel + " )");
		
		
		
		

		List<QueryResultViewModel> aoResult = m_oResponseTranslator.translateBatch(sJson, bFullViewModel, m_sDownloadProtocol);

		if(null == aoResult || aoResult.isEmpty()) {
			Utils.debugLog(s_sClassName + ".buildResultViewModel: no results");
		}
		return aoResult;
	}


	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getTemplate()
	 */
	@Override
	protected Template getTemplate() {
		// TODO Auto-generated method stub
		return null;
	}


	public static void main(String[] args) {
		try {
			QueryExecutorSOBLOO oQE = new QueryExecutorSOBLOO();
			String sQuery = "( footprint:\"intersects(POLYGON((7.955097431033318 38.58252615935333,7.955097431033318 41.50857729743935,11.031307820285987 41.50857729743935,11.031307820285987 38.58252615935333,7.955097431033318 38.58252615935333)))\" ) AND ( beginPosition:[2020-01-14T00:00:00.000Z TO 2020-01-21T23:59:59.999Z] AND endPosition:[2020-01-14T00:00:00.000Z TO 2020-01-21T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND producttype:GRD) OR (platformname:Sentinel-2 AND producttype:S2MSI1C)";
			int iOffset = 0;
			int iLimit = 10;
			PaginatedQuery oQuery = new PaginatedQuery(sQuery, ""+iOffset, ""+iLimit, null, null);
			int iCount = oQE.executeCount(oQuery.getQuery());
			oQE.executeAndRetrieve(oQuery, true);
			Utils.debugLog(iCount);

		} catch(Exception oE) {
			oE.printStackTrace();
		}


		/*
	http://217.182.93.57//wasdiwebserver/rest/search/query?sQuery=(%20footprint:%22intersects(POLYGON((7.955097431033318%2038.58252615935333,7.955097431033318%2041.50857729743935,11.031307820285987%2041.50857729743935,11.031307820285987%2038.58252615935333,7.955097431033318%2038.58252615935333)))%22%20)%20AND%20(%20beginPosition:%5B2020-01-14T00:00:00.000Z%20TO%202020-01-21T23:59:59.999Z%5D%20AND%20endPosition:%5B2020-01-14T00:00:00.000Z%20TO%202020-01-21T23:59:59.999Z%5D%20)%20AND%20%20%20(platformname:Sentinel-1%20AND%20producttype:GRD)%20OR%20(platformname:Sentinel-2%20AND%20producttype:S2MSI1C)&offset=0&limit=10&providers=SENTINEL		

		 */
	}
}