package wasdi.shared.queryexecutors.creodias2;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Preconditions;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryTranslatorCreoDias2 extends QueryTranslator {
	
	protected String m_sCreoDiasApiBaseUrl = "https://datahub.creodias.eu/odata/v1/Products?";
	private static final String sOdataEQ = "eq";  // TODO: check naming convention
	private static final String sOdataAND = "and"; // TODO: check naming convention
	private static final String sOdataFilterOption = "$filter=";

 
	@Override
	protected String translate(String sQueryFromClient) {
		Preconditions.checkNotNull(sQueryFromClient, "QueryTranslatorCreoDias2.translate: query is null");
		Preconditions.checkNotNull(m_sAppConfigPath, "QueryTranslatorCreoDias2.translate: app config path is null");
		Preconditions.checkNotNull(m_sParserConfigPath, "QueryTranslatorCreoDias2.translate: parser config is null");
		
		String sQuery = this.prepareQuery(sQueryFromClient);

		QueryViewModel oQueryViewModel = parseWasdiClientQuery(sQuery);
		
		String sPlatform = oQueryViewModel.platformName;
		String sStartDate = oQueryViewModel.startFromDate;
		String sEndDate = oQueryViewModel.startToDate;
		
		// basic query: query collection of products
		String sCollectionFilter = createCollectionNameEqFilter(sPlatform);
		String sStartDateFilter = createSensingDateFilter(sStartDate, true);
		String sEndDateFilter = createSensingDateFilter(sEndDate, false); // TODO: do we generally consider the interval as included?
		String sFilterValue = String.join(" " + sOdataAND + " ", Arrays.asList(sCollectionFilter, sStartDateFilter, sEndDateFilter));

		return sOdataFilterOption + sFilterValue;
	}
	
	private String createCollectionNameEqFilter(String sCollectionName) {
		List<String> asFilterElements = Arrays.asList("Collection/Name", sOdataEQ, "'" + sCollectionName.toUpperCase() + "'");
		return String.join(" ", asFilterElements); 
	}
	

	private String createSensingDateFilter(String sDate, boolean bStartDate) {
		String sInclusion = bStartDate ? "gt" : "lt";		// TODO: so far, I consider both intervals as excluded. Maybe I need to add the inclusion
		String sContentFilterOption = "ContentDate/Start";
		List<String> asFilterElements = Arrays.asList(sContentFilterOption, sInclusion, sDate);
		return String.join(" ", asFilterElements);
	}


	@Override
	public String getCountUrl(String sQuery) {
		// TODO Auto-generated method stub
		if(Utils.isNullOrEmpty(sQuery)) {
			WasdiLog.debugLog("QueryTranslatorCreoDias2.getCountUrl: sQuery is null");
		}
		String sUrl = m_sCreoDiasApiBaseUrl;
		sUrl+=translateAndEncodeParams(sQuery);
		
		return sUrl;
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		// TODO Auto-generated method stub
		return null;
	}

}
