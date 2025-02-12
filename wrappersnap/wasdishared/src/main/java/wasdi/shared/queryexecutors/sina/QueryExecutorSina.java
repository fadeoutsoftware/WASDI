package wasdi.shared.queryexecutors.sina;

import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

import java.util.ArrayList;
import java.time.YearMonth;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorSina extends QueryExecutor {

	private String m_sFileListUrl = null;
	
	public QueryExecutorSina() {
		this.m_oQueryTranslator = new QueryTranslatorSina();
		this.m_oResponseTranslator = new ResponseTranslatorSina();
	}
	
	@Override
	public void init() {
		super.init();
		
		try {
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(m_sParserConfigPath);
			m_sFileListUrl = oAppConf.getString("fileListUrl");
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorSina.init: exception reading parser config file " + m_sParserConfigPath);
		}		
	}
	
	
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {
		return sOriginalUrl;
	}

	@Override
	public int executeCount(String sQuery) {
		WasdiLog.debugLog("QueryExecutorSina.executeCount. Query: " + sQuery);
		
		int lCount = -1;
		
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
		
		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			WasdiLog.debugLog("QueryExecutorSina.executeCount. Unsupported platform: " + oQueryViewModel.platformName);
			return lCount;
		}
		
				
		List<String> asRelevantFileNames = searchResults(oQueryViewModel);
		
		if (asRelevantFileNames == null) {
			WasdiLog.warnLog("QueryExecutorSina.executeCount. List of results is null");
			return lCount;
		}
		
		lCount = asRelevantFileNames.size();
		
		return lCount;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		WasdiLog.debugLog("QueryExecutorSina.executeAndRetrieve. Query: " + oQuery.getQuery());

		String sOffset = oQuery.getOffset();
		String sLimit = oQuery.getLimit();

		int iOffset = 0;
		int iLimit = 10;

		try {
			iOffset = Integer.parseInt(sOffset);
		} catch (Exception oE) {
			WasdiLog.errorLog("QueryExecutorSina.executeAndRetrieve. Impossible to parse offset " + sOffset, oE);
			return null;
		}

		try {
			iLimit = Integer.parseInt(sLimit);
		} catch (Exception oE) {
			WasdiLog.errorLog("QueryExecutorSina.executeAndRetrieve. Impossible to parse limit " + sLimit, oE);
			return null;
		}
		
		// Parse the query
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());

		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			WasdiLog.debugLog("QueryExecutorSina.executeAndRetrieve. Unsupported platform: " + oQueryViewModel.platformName);
			return null;
		}
		
		List<String> asRelevantFileNames = searchResults(oQueryViewModel);
		
		if (asRelevantFileNames == null) {
			WasdiLog.warnLog("QueryExecutorSina.executeCount. List of results is null");
			return null;
		}
		
		List<QueryResultViewModel> aoViewModels = asRelevantFileNames.stream()
				.map(sFileName -> ((ResponseTranslatorSina) m_oResponseTranslator).translate(sFileName))
				.collect(Collectors.toList());
		
		int iEnd = iOffset + iLimit > aoViewModels.size() ? aoViewModels.size() : iOffset + iLimit;
		return aoViewModels.subList(iOffset, iEnd);
	}
	
	
	
	
	private List<String> searchResults(QueryViewModel oQueryViewModel) {
		String sDateFrom = oQueryViewModel.startFromDate;
		String sDateTo = oQueryViewModel.endToDate;
		
		String sDataset = oQueryViewModel.sensorMode;
		
		
		if (Utils.isNullOrEmpty(sDateFrom) || Utils.isNullOrEmpty(sDateTo) || Utils.isNullOrEmpty(sDataset)) {
			WasdiLog.debugLog("QueryExecutorSina.searchResults. Some of the necessary filters are empty");
			return null;
		}
		
		String sStartYear = sDateFrom.substring(0, 4);
		String sStartMonth = sDateFrom.substring(5, 7);
		
		String sEndYear = sDateTo.substring(0, 4);
		String sEndMonth = sDateTo.substring(5, 7);
		
		// we generate the list of all files that we need
		int iStartYear = -1;
		int iStartMonth = -1;
		int iEndYear = -1;
		int iEndMonth = -1;
		
		try {
			iStartYear = Integer.parseInt(sStartYear);
			iStartMonth = Integer.parseInt(sStartMonth);
			iEndYear = Integer.parseInt(sEndYear);
			iEndMonth = Integer.parseInt(sEndMonth);
		} catch (NumberFormatException oEx) {
			WasdiLog.errorLog("QueryExecutorSina.searchResults. Impossible to parse the date filters", oEx);
			return null;
		}
				
		List<YearMonth> aoYearMonth = generateYearMonthRange(iStartYear, iStartMonth, iEndYear, iEndMonth);
		return getRelevantFiles(aoYearMonth, sDataset);
	}
	
	
	private List<String> getRelevantFiles(List<YearMonth> aoTimeRange, String sDatasetPrefix) {
		
		if (aoTimeRange == null || aoTimeRange.size() == 0) {
			WasdiLog.warnLog("QueryExecutorSina.getRelevantFiles: time range is null or empty");
			return null;
		}
		
		List<String> asSearchFileNames = aoTimeRange.stream()
				.map(oRange -> sDatasetPrefix + "_" + oRange.getYear() + "_" + String.format("%02d", oRange.getMonthValue()))
				.collect(Collectors.toList());
		
		List<String> aoZipFiles = getZipContent(sDatasetPrefix);
		
		if (aoZipFiles == null) {
			WasdiLog.warnLog("QueryExecutorSina.getRelevantFiles: list of files from data provider is null");
			return null;
		}
		
		List<String> aoResults = new ArrayList<>();
		
		for (String sSearchFileName : asSearchFileNames) {
			if (aoZipFiles.contains(sSearchFileName + ".asc")) {
				aoResults.add(sSearchFileName + ".asc");
			}
		}
		
		return aoResults;
		
	}
	

	private List<YearMonth> generateYearMonthRange(int iStartYear, int iStartMonth, int iEndYear, int iEndMonth) {
		List<YearMonth> aoYearMonth = new ArrayList<>();
		
		YearMonth oStart = YearMonth.of(iStartYear, iStartMonth);
		YearMonth oEnd = YearMonth.of(iEndYear, iEndMonth);
		
		while (!oStart.isAfter(oEnd)) {
			aoYearMonth.add(oStart);
			oStart = oStart.plusMonths(1);
		}
		return aoYearMonth;
	}
	
	
	private List<String> getZipContent(String sDatasetPrefix) {
		List<String> aoFileList = null;
		
		if (Utils.isNullOrEmpty(m_sFileListUrl)) {
			WasdiLog.warnLog("QueryExecutorSina.getZipContent. Path to parser config is null");
			return aoFileList;
		}
		
		String sUrl = m_sFileListUrl;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		
		if (sDatasetPrefix.equals("SPEI01")) {
			sUrl += "spei01_1952-2022_ascii-1/download/en/1/SPEI01_1952-2022_ASCII.zip?action=view";
		} else {
			WasdiLog.warnLog("QueryExecutorSina.getZipContent. Unknown url for dataset " + sDatasetPrefix);
			return aoFileList;
		}
		
		HttpCallResponse oResponse = HttpUtils.httpGet(sUrl);
		
		if (oResponse.getResponseCode() >= 200 || oResponse.getResponseCode() <= 299) {
			
			if (Utils.isNullOrEmpty(oResponse.getResponseBody())) 
				WasdiLog.warnLog("QueryExecutorSina.getZipContent: Response body is empty");
			else
				aoFileList = sanitizeHTMLContent(oResponse.getResponseBody());
		}
		else {
			WasdiLog.warnLog("QueryExecutorSina.getZipContent: got error code from http request" + oResponse.getResponseCode());
		}
		
		return aoFileList;
	}
	
	
	private List<String> sanitizeHTMLContent(String sHttpBody) {
		String sSanitizedContent = sHttpBody.replaceAll("\\<[^>]*>", "");
		String[] asLines = sSanitizedContent.split("\n");
		List<String> asSanitizedLines = new ArrayList<>();
		
		for(int i = 0; i < asLines.length; i++) {
			String sTrimmed = asLines[i].trim();
			if (!Utils.isNullOrEmpty(sTrimmed) && sTrimmed.startsWith("SPEI")) {
				asSanitizedLines.add(sTrimmed);
			}
		}
		return asSanitizedLines;
	}


}
