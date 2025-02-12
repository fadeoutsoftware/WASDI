package wasdi.shared.queryexecutors.terrascope;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * Query Executor for Terrascope Data Center.
 * 
 * @author PetruPetrescu
 *
 */
public class QueryExecutorTerrascope extends QueryExecutor {

	boolean m_bAuthenticated = false;

	public QueryExecutorTerrascope() {
		this.m_oQueryTranslator = new QueryTranslatorTerrascope();
		this.m_oResponseTranslator = new ResponseTranslatorTerrascope();
	}
	
	/**
	 * Overload of the get URI from Product Name method.
	 * For Terrascope, we need just the original link..
	 */
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {
		if (sProduct.toUpperCase().startsWith("COPERNICUS_DSM_COG_")
				|| sProduct.toUpperCase().startsWith("ESA_WORLDCOVER")) {
			return sOriginalUrl;
		}
		return null;
	}

	/**
	 * Executes the count 
	 */
	@Override
	public int executeCount(String sQuery) {
		int iCount = 0;

		sQuery = adjustUserProvidedDatesTo2020(sQuery);

		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);

		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			return 0;
		}

		List<String> aoTerrascopeQuery = ((QueryTranslatorTerrascope) m_oQueryTranslator).translateMultiple(sQuery);
		for (String sTerrascopeQuery : aoTerrascopeQuery) {
			if (!Utils.isNullOrEmpty(sTerrascopeQuery)) {
				String encodedUrl = ((QueryTranslatorTerrascope) m_oQueryTranslator).encode(sTerrascopeQuery);

				// Make the call
				HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(encodedUrl); 
				String sTerrascopeResults = oHttpCallResponse.getResponseBody();

				WasdiLog.debugLog("QueryExecutorTerrascope: get Results, extract the total count");

				iCount += m_oResponseTranslator.getCountResult(sTerrascopeResults);
			}
		}

		return iCount;
	}

	/**
	 * Execute an Terrascope Data Center Query and return the result in WASDI format
	 */
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		List<QueryResultViewModel> aoReturnList = new ArrayList<>();

		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());

		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			return aoReturnList;
		}

		try {
			String sQuery = oQuery.getQuery();

			sQuery = adjustUserProvidedDatesTo2020(sQuery);

			if (!sQuery.contains("&offset")) sQuery += "&offset=" + oQuery.getOffset();
			if (!sQuery.contains("&limit")) sQuery += "&limit=" + oQuery.getLimit();

			List<String> aoTerrascopeQuery = ((QueryTranslatorTerrascope) m_oQueryTranslator).translateMultiple(sQuery);
			for (String sTerrascopeQuery : aoTerrascopeQuery) {
				if (!Utils.isNullOrEmpty(sTerrascopeQuery)) {
					String encodedUrl = ((QueryTranslatorTerrascope) m_oQueryTranslator).encode(sTerrascopeQuery);

					// Make the call
					HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(encodedUrl); 
					String sTerrascopeResults = oHttpCallResponse.getResponseBody();

					WasdiLog.debugLog("QueryExecutorTerrascope.executeAndRetrieve: got result, start conversion");

					aoReturnList.addAll(m_oResponseTranslator.translateBatch(sTerrascopeResults, bFullViewModel));
				}
			}

			String sTerrascopeQuery = m_oQueryTranslator.translateAndEncodeParams(sQuery);

			if (Utils.isNullOrEmpty(sTerrascopeQuery)) return aoReturnList;

			// Make the query
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sTerrascopeQuery); 
			String sTerrascopeResults = oHttpCallResponse.getResponseBody();

			WasdiLog.debugLog("QueryExecutorTerrascope.executeAndRetrieve: got result, start conversion");

			return m_oResponseTranslator.translateBatch(sTerrascopeResults, bFullViewModel);
		} catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorTerrascope.executeAndRetrieve: Exception = " + oEx.toString());
		}

		return aoReturnList;
	}

	/**
	 * Adjust the user provided dates (beginPosition & endPosition) to 2020-01-01 in order to make sure that the search produces valid results
	 * @param sQuery the initial query
	 * @return the query with the adjusted dates
	 */
	private static String adjustUserProvidedDatesTo2020(String sQuery) {
		if (!sQuery.contains("platformname:WorldCover") && !sQuery.contains("platformname:DEM")) {
			return sQuery;
		}

		if (!sQuery.contains("beginPosition") || !sQuery.contains("endPosition")) {
			return sQuery;
		}

		String sOriginalDateToReplace = sQuery.substring(sQuery.indexOf("[", sQuery.indexOf("beginPosition")) + 1, sQuery.indexOf("TO", sQuery.indexOf("beginPosition"))).trim();
		String sReplacedWithDate = "2020-01-01T00:00:00.000Z";

		sQuery = sQuery.replace(sOriginalDateToReplace, sReplacedWithDate);

		sOriginalDateToReplace = sQuery.substring(sQuery.indexOf("TO", sQuery.indexOf("beginPosition")) + 3, sQuery.indexOf("]", sQuery.indexOf("beginPosition"))).trim();
		sReplacedWithDate = Utils.formatToYyyyDashMMDashdd(new Date()) + "T23:59:59.999Z";

		sQuery = sQuery.replace(sOriginalDateToReplace, sReplacedWithDate);

		return sQuery;
	}

}
