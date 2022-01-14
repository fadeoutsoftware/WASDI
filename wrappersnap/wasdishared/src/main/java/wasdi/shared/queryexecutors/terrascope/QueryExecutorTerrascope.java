package wasdi.shared.queryexecutors.terrascope;

import java.util.ArrayList;
import java.util.List;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
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
		m_sProvider="Terrascope";
		this.m_oQueryTranslator = new QueryTranslatorTerrascope();
		this.m_oResponseTranslator = new ResponseTranslatorTerrascope();

		m_asSupportedPlatforms.add(Platforms.SENTINEL1);
		m_asSupportedPlatforms.add(Platforms.SENTINEL2);
//		m_asSupportedPlatforms.add(Platforms.PROBAV);
		m_asSupportedPlatforms.add(Platforms.DEM);
		m_asSupportedPlatforms.add(Platforms.WORLD_COVER);
	}

	/**
	 * Executes the count 
	 */
	@Override
	public int executeCount(String sQuery) {
		int iCount = 0;

		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);

		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			return 0;
		}

		List<String> aoTerrascopeQuery = ((QueryTranslatorTerrascope) m_oQueryTranslator).translateMultiple(sQuery);
		for (String sTerrascopeQuery : aoTerrascopeQuery) {
			if (!Utils.isNullOrEmpty(sTerrascopeQuery)) {
				String encodedUrl = ((QueryTranslatorTerrascope) m_oQueryTranslator).encode(sTerrascopeQuery);

				// Make the call
				String sTerrascopeResults = HttpUtils.httpGetResults(encodedUrl);

				Utils.debugLog("QueryExecutorTerrascope: get Results, extract the total count");

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

			if (!sQuery.contains("&offset")) sQuery += "&offset=" + oQuery.getOffset();
			if (!sQuery.contains("&limit")) sQuery += "&limit=" + oQuery.getLimit();

			List<String> aoTerrascopeQuery = ((QueryTranslatorTerrascope) m_oQueryTranslator).translateMultiple(sQuery);
			for (String sTerrascopeQuery : aoTerrascopeQuery) {
				if (!Utils.isNullOrEmpty(sTerrascopeQuery)) {
					String encodedUrl = ((QueryTranslatorTerrascope) m_oQueryTranslator).encode(sTerrascopeQuery);

					// Make the call
					String sTerrascopeResults = HttpUtils.httpGetResults(encodedUrl);

					Utils.debugLog("QueryExecutorTerrascope.executeAndRetrieve: got result, start conversion");

					aoReturnList.addAll(m_oResponseTranslator.translateBatch(sTerrascopeResults, bFullViewModel));
				}
			}

			String sTerrascopeQuery = m_oQueryTranslator.translateAndEncodeParams(sQuery);

			if (Utils.isNullOrEmpty(sTerrascopeQuery)) return aoReturnList;

			// Make the query
			String sTerrascopeResults = HttpUtils.httpGetResults(sTerrascopeQuery);

			Utils.debugLog("QueryExecutorTerrascope.executeAndRetrieve: got result, start conversion");

			return m_oResponseTranslator.translateBatch(sTerrascopeResults, bFullViewModel);
		} catch (Exception oEx) {
			Utils.debugLog("QueryExecutorTerrascope.executeAndRetrieve: Exception = " + oEx.toString());
		}

		return aoReturnList;
	}

}
