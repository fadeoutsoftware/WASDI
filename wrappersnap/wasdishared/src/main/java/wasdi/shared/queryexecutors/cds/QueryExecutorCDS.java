package wasdi.shared.queryexecutors.cds;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * CDS Query Executor.
 * This class queries the CDS Catalogue from https://cds.climate.copernicus.eu/api/v2/
 * 
 * The query in this case is local: the catalogue does not have API.
 * CDS are daily maps in a pre-defined world grid.
 * Maps are available one per day per each tile of the grid.
 * 
 * The query is obtained with a local shape file that reproduces the CDS grid
 * It intersects the users' AOI, find involved tiles and returns one result per day per tile
 * 
 * @author PetruPetrescu
 *
 */
public class QueryExecutorCDS extends QueryExecutor {

	private static ObjectMapper s_oMapper = new ObjectMapper();
	public static final String s_sSEA_SURFACE_TEMPERATURE_DATASET = "satellite-sea-surface-temperature";

	public QueryExecutorCDS() {
		this.m_oQueryTranslator = new QueryTranslatorCDS();
		this.m_oResponseTranslator = new ResponseTranslatorCDS();
	}
	
	/**
	 * Overload of the get URI from Product Name method.
	 * For CDS, we need just the original link..
	 */
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl, String sPlatform) {
		if (sProduct.startsWith(Platforms.ERA5)) {
			return sOriginalUrl;
		}
		return null;
	}

	@Override
	public int executeCount(String sQuery) {
		
		try {
			int iCount = 0;

			// Parse the query
			QueryViewModel oCDSQuery = m_oQueryTranslator.parseWasdiClientQuery(sQuery);

			if (!m_asSupportedPlatforms.contains(oCDSQuery.platformName)) {
				return -1;
			}
			
			// we use the absolute orbit parameter to decide if the data should be aggregated monthly
			boolean bIsMonthlyAggregation = oCDSQuery.startToDate.equals("monthly");
			
			if (bIsMonthlyAggregation) {
				long lStart = TimeEpochUtils.fromDateStringToEpoch(oCDSQuery.startFromDate);
				long lEnd = TimeEpochUtils.fromDateStringToEpoch(oCDSQuery.endToDate);
				Date oStartDate = TimeEpochUtils.fromEpochToDateObject(lStart);
				Date oEndDate = TimeEpochUtils.fromEpochToDateObject(lEnd);
				iCount = Utils.splitTimeRangeInMonthlyIntervals(oStartDate, oEndDate, 0, Integer.MAX_VALUE).size();
			} else {
				int iDays = TimeEpochUtils.countDaysIncluding(oCDSQuery.startFromDate, oCDSQuery.endToDate);
				iCount = iDays;
			}
			return iCount;
		} catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorCDS.executeCount: error " + oEx.toString());
		}

		return -1;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {

		try {
			List<QueryResultViewModel> aoResults = new ArrayList<>();

			int iActualElement = 0;

			// Parse the query
			QueryViewModel oCDSQuery = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());

			if (!m_asSupportedPlatforms.contains(oCDSQuery.platformName)) {
				return aoResults;
			}

			long lStart = TimeEpochUtils.fromDateStringToEpoch(oCDSQuery.startFromDate);

			String sOffset = oQuery.getOffset();
			String sLimit = oQuery.getLimit();

			int iOffset = 0;
			int iLimit = 10;

			try {
				iOffset = Integer.parseInt(sOffset);
			} catch (Exception oE) {
				WasdiLog.debugLog("QueryExecutorCDS.executeAndRetrieve: " + oE.toString());
			}

			try {
				iLimit = Integer.parseInt(sLimit);
			} catch (Exception oE) {
				WasdiLog.debugLog("QueryExecutorCDS.executeAndRetrieve: " + oE.toString());
			}

			String sDataset = oCDSQuery.productName;
			String sProductType = oCDSQuery.productType;
			String sPresureLevels = oCDSQuery.productLevel;
			String sVariables = sDataset.contentEquals(s_sSEA_SURFACE_TEMPERATURE_DATASET) 
					? "all" 
					: oCDSQuery.sensorMode;
			String sFormat = sDataset.contentEquals(s_sSEA_SURFACE_TEMPERATURE_DATASET) 
					? "zip" 
					: oCDSQuery.timeliness;
			String sVersion = oCDSQuery.instrument;
			String sBoundingBox = oCDSQuery.north + ", " + oCDSQuery.west + ", " + oCDSQuery.south + ", " + oCDSQuery.east;
			String sFootPrint = extractFootprint(oQuery.getQuery());

			// we use the "startToDate" parameter to decide if the data should be aggregated monthly
			boolean bIsMonthlyAggregation = oCDSQuery.startToDate.equals("monthly");

			if (bIsMonthlyAggregation) {
				Date oStartDate = TimeEpochUtils.fromEpochToDateObject(lStart);
				Date oEndDate = TimeEpochUtils.fromEpochToDateObject(TimeEpochUtils.fromDateStringToEpoch(oCDSQuery.endToDate));
				
				List<Date[]> aaoIntervals = Utils.splitTimeRangeInMonthlyIntervals(oStartDate, oEndDate, iOffset, iLimit);
				
				for (Date[] aoInterval : aaoIntervals) {
					Date oStartIntervalDate = aoInterval[0];
					Date oEndIntervalDate = aoInterval[1];
					String sStartDate = Utils.formatToYyyyMMdd(oStartIntervalDate);
					String sEndDate = Utils.formatToYyyyMMdd(oEndIntervalDate);
					
					String sPayload = prepareLinkJsonPayload(sDataset, sProductType, sVariables, sPresureLevels, sStartDate, sEndDate, sBoundingBox, sFormat, sVersion);
					QueryResultViewModel oResult = getQueryResultViewModel(oCDSQuery, sPayload, sFootPrint, oStartIntervalDate, oEndIntervalDate, sVariables);
					aoResults.add(oResult);
				}
				
			} else {
				int iDays = TimeEpochUtils.countDaysIncluding(oCDSQuery.startFromDate, oCDSQuery.endToDate);
				for (int i = 0; i < iDays; i++) {
					Date oActualDay = TimeEpochUtils.getLaterDate(lStart, i);

					if (iActualElement >= iOffset && iActualElement < iOffset + iLimit) {
						String sDate = Utils.formatToYyyyMMdd(oActualDay);
						String sPayload = prepareLinkJsonPayload(sDataset, sProductType, sVariables, sPresureLevels, sDate, null, sBoundingBox, sFormat, sVersion);

						QueryResultViewModel oResult = getQueryResultViewModel(oCDSQuery, sPayload, sFootPrint, oActualDay, null, sVariables);
						aoResults.add(oResult);
					}

					iActualElement++;

					if (iActualElement > iOffset + iLimit)
						break;
				}
			}

			return aoResults;
		} catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorCDS.executeAndRetrieve: error " + oEx.toString());
		}

		return null;
	}

	private static String prepareLinkJsonPayload(String sDataset, String sProductType, String sVariables,
			String sPressureLevels, String sStartDate, String sEndDate, String sBoundingBox, String sFormat, String sVersion) {
		String sMonthlyAggregation = String.valueOf(!Utils.isNullOrEmpty(sEndDate)); // if sEndDate is not null, then we do the monthly aggregation
		
		Map<String, String> aoPayload = new HashMap<>();
		
		aoPayload.put("dataset", sDataset); // reanalysis-era5-pressure-levels
		
		// insert product type (for reanalysis) or sensor (for sea-surface-temperature)
		String sKey = "";
		if (!Utils.isNullOrEmpty(sProductType)) {
			sKey = "productType";;
			if (sDataset.equalsIgnoreCase(s_sSEA_SURFACE_TEMPERATURE_DATASET)) { 
				sKey = "sensor";
				sProductType = sProductType.toLowerCase().replace(" ", "_");
			}
			aoPayload.put(sKey, sProductType); 
		}
		
		// insert variables  
		if (!Utils.isNullOrEmpty(sVariables))
			aoPayload.put("variables", sVariables);
		
		// insert pressure level (for reanalysis) or processing level (for sea-surface-temperature)
		if (!Utils.isNullOrEmpty(sPressureLevels)) {
			sKey = "presureLevels";
			if (sDataset.equalsIgnoreCase(s_sSEA_SURFACE_TEMPERATURE_DATASET)) { 
				sKey = "processingLevel";
				sPressureLevels = sPressureLevels.toLowerCase().replace(" ", "_");
			}
			aoPayload.put(sKey, sPressureLevels);
		}
		
		// insert bounding box  (for reanalysis)
		if (!Utils.isNullOrEmpty(sBoundingBox))
			aoPayload.put("boundingBox", sBoundingBox); // North 10�, West 5�, South 5�, East 7
		
		// insert format 
		if (!Utils.isNullOrEmpty(sFormat))
			aoPayload.put("format", sFormat); // grib | netcdf
		
		// insert version (for sea-surface-temperature)
		if (!Utils.isNullOrEmpty(sVersion))
			aoPayload.put("version", sVersion.replace(".", "_"));
		
		// insert monthly aggregation
		aoPayload.put("monthlyAggregation", sMonthlyAggregation); // true | false
		if (sMonthlyAggregation.equalsIgnoreCase("true")) {
			aoPayload.put("startDate", sStartDate);
			aoPayload.put("endDate", sEndDate);
		} else {
			aoPayload.put("date", sStartDate);
		}

		String sPayload = null;
		try {
			sPayload = s_oMapper.writeValueAsString(aoPayload);
		} catch (Exception oE) {
			WasdiLog.debugLog(
					"QueryExecutorCDS.prepareLinkJsonPayload: could not serialize payload due to " + oE + ".");
		}

		return sPayload;
	}

	private static String extractFootprint(String sQuery) {
		String sFootprint = "";

		if (sQuery.contains("footprint")) {
			String sIntro = "( footprint:\"intersects(POLYGON((";
			int iStart = sQuery.indexOf(sIntro);
			if (iStart >= 0) {
				iStart += sIntro.length();
			}
			int iEnd = sQuery.indexOf(')', iStart);
			if (0 > iEnd) {
				iEnd = sQuery.length();
			}

			sFootprint = sQuery.substring(iStart, iEnd).trim();
		}

		return sFootprint;
	}

	private QueryResultViewModel getQueryResultViewModel(QueryViewModel oQuery, String sPayload, String sFootPrint,
			Date oStartDate, Date oEndDate, String sVariables) {
		String sStartDate = Utils.formatToYyyyMMdd(oStartDate);
		String sEndDate = oEndDate != null 
				? Utils.formatToYyyyMMdd(oEndDate) 
				: "";
		String sStartDateTime = TimeEpochUtils.fromEpochToDateString(oStartDate.getTime());
		String sEndDateTime = null;
		if (oEndDate != null) {
			sEndDateTime = TimeEpochUtils.fromEpochToDateString(oEndDate.getTime());
		}

		String sDataset = oQuery.productName;
		String sExtension = sDataset.equalsIgnoreCase(s_sSEA_SURFACE_TEMPERATURE_DATASET) ? "zip" : oQuery.timeliness;
		String sFileNameFootprint = CDSUtils.getFootprintForFileName(oQuery.north, oQuery.west, oQuery.south, oQuery.east, sDataset);
		String sExtensionWithComma = "." +  sExtension;
		String sFileName = CDSUtils.getFileName(oQuery.productName, sVariables, sStartDate, sStartDate, sEndDate, sExtensionWithComma, sFileNameFootprint);
		String sUrl = m_oDataProviderConfig.link + "?payload=" + sPayload;
		String sEncodedUrl = StringUtils.encodeUrl(sUrl);

		QueryResultViewModel oResult = new QueryResultViewModel();
		oResult.setId(sFileName);
		oResult.setTitle(sFileName);
		oResult.setLink(sEncodedUrl);
		oResult.setSummary(getSummary(sStartDateTime, sEndDateTime, sVariables, sExtension)); 
		oResult.setProvider(m_sDataProviderCode);
		oResult.setFootprint(sFootPrint);
		oResult.getProperties().put("platformname", Platforms.ERA5);
		oResult.getProperties().put("dataset", oQuery.productName);
		oResult.getProperties().put("variables", sVariables);
		oResult.getProperties().put("format", sExtension);
		oResult.getProperties().put("startDate", sStartDateTime);
		oResult.getProperties().put("beginposition", sStartDateTime);

		if (oEndDate != null) {
			oResult.getProperties().put("endDate", sEndDateTime);
			oResult.getProperties().put("endposition", sEndDateTime);
		}
		
		if (!oQuery.productName.equalsIgnoreCase(s_sSEA_SURFACE_TEMPERATURE_DATASET)) { 
			oResult.getProperties().put("productType", oQuery.productType);
			oResult.getProperties().put("presureLevels", oQuery.productLevel);
		}
		return oResult;
	}

	private String getSummary(String sStartDateTime, String sEndDateTime, String sMode, String sTimeliness) {
		List<String> sSummaryElements = new ArrayList<>();
		
		if (Utils.isNullOrEmpty(sEndDateTime)) {
			sSummaryElements.add("Date: " + sStartDateTime);
		} else {
			sSummaryElements.add("Start date: " + sStartDateTime);
			sSummaryElements.add("End date: " + sEndDateTime);
		}
			
		if (!Utils.isNullOrEmpty(sMode)) 
			sSummaryElements.add("Mode: " + sMode);
		
		if (!Utils.isNullOrEmpty(sTimeliness))
			sSummaryElements.add("Instrument: " + sTimeliness);
		
		return String.join(", ", sSummaryElements);
	}
	
	

}
