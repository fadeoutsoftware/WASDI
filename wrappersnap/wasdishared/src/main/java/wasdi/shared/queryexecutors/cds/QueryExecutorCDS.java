package wasdi.shared.queryexecutors.cds;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
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
	private static DataProviderConfig s_oDataProviderConfig;

	public QueryExecutorCDS() {
		
		m_sProvider = "CDS";
		s_oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(m_sProvider);
		
		this.m_oQueryTranslator = new QueryTranslatorCDS();
		this.m_oResponseTranslator = new ResponseTranslatorCDS();
		
	}
	
	/**
	 * Overload of the get URI from Product Name method.
	 * For CDS, we need just the original link..
	 */
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {
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
			boolean bIsMonthlyAggregation = oCDSQuery.absoluteOrbit == 1;
			
			if (bIsMonthlyAggregation) {
				iCount = TimeEpochUtils.countMonthsIncluding(oCDSQuery.startFromDate, oCDSQuery.endToDate);
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
			String sVariables = oCDSQuery.sensorMode;
			String sFormat = oCDSQuery.timeliness;
			String sBoundingBox = oCDSQuery.north + ", " + oCDSQuery.west + ", " + oCDSQuery.south + ", " + oCDSQuery.east;
			String sFootPrint = extractFootprint(oQuery.getQuery());

			// we use the absolute orbit parameter to decide if the data should be aggregated monthly
			boolean bIsMonthlyAggregation = oCDSQuery.absoluteOrbit == 1;

			if (bIsMonthlyAggregation) {
				Date oStartDate = TimeEpochUtils.fromEpochToDateObject(lStart);
				Date oEndDate = TimeEpochUtils.fromEpochToDateObject(TimeEpochUtils.fromDateStringToEpoch(oCDSQuery.endToDate));
				
				Calendar oStartCalendar = Calendar.getInstance();
				oStartCalendar.setTime(oStartDate);
				Calendar oEndCalendar = Calendar.getInstance();
				oEndCalendar.setTime(oEndDate);
				
				while (oStartCalendar.before(oEndCalendar)) {
					int iStartMonth = oStartCalendar.get(Calendar.MONTH);
					int iEndMonth = oEndCalendar.get(Calendar.MONTH);
					int iStartYear = oStartCalendar.get(Calendar.YEAR);
					int iEndYear = oEndCalendar.get(Calendar.YEAR);
					
					if (iStartMonth == iEndMonth && iStartYear == iEndYear) {
						Date oStartIntervalDate = oStartCalendar.getTime();
						Date oEndIntervalDate = oEndCalendar.getTime();
						String sStartDate = Utils.formatToYyyyMMdd(oStartIntervalDate);
						String sEndDate = Utils.formatToYyyyMMdd(oEndCalendar.getTime());
						
						String sPayload = prepareLinkJsonPayload(sDataset, sProductType, sVariables, sPresureLevels, sStartDate, sEndDate, sBoundingBox, sFormat);
						QueryResultViewModel oResult = getQueryResultViewModel(oCDSQuery, sPayload, sFootPrint, oStartIntervalDate, oEndIntervalDate);
						aoResults.add(oResult);
						break;
					} else {
						int iLastDayOfMonth = oStartCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
						Date oStartIntervalDate = oStartCalendar.getTime();
						oStartCalendar.set(Calendar.DAY_OF_MONTH, iLastDayOfMonth);
						Date oEndIntervalDate = oStartCalendar.getTime();
						String sStartDate = Utils.formatToYyyyMMdd(oStartIntervalDate);
						String sEndDate = Utils.formatToYyyyMMdd(oEndIntervalDate);
						
						String sPayload = prepareLinkJsonPayload(sDataset, sProductType, sVariables, sPresureLevels, sStartDate, sEndDate, sBoundingBox, sFormat);
						QueryResultViewModel oResult = getQueryResultViewModel(oCDSQuery, sPayload, sFootPrint, oStartIntervalDate, oEndIntervalDate);
						aoResults.add(oResult);
						
						oStartCalendar.add(Calendar.DAY_OF_MONTH, 1);
					}
					
				}
			} else {
				int iDays = TimeEpochUtils.countDaysIncluding(oCDSQuery.startFromDate, oCDSQuery.endToDate);
				for (int i = 0; i < iDays; i++) {
					Date oActualDay = TimeEpochUtils.getLaterDate(lStart, i);

					if (iActualElement >= iOffset && iActualElement < iOffset + iLimit) {
						String sDate = Utils.formatToYyyyMMdd(oActualDay);
						String sPayload = prepareLinkJsonPayload(sDataset, sProductType, sVariables, sPresureLevels, sDate, null, sBoundingBox, sFormat);

						QueryResultViewModel oResult = getQueryResultViewModel(oCDSQuery, sPayload, sFootPrint,
								oActualDay, null);
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
			String sPresureLevels, String sStartDate, String sEndDate, String sBoundingBox, String sFormat) {
		String sMonthlyAggregation = String.valueOf(!Utils.isNullOrEmpty(sEndDate)); // if sEndDate is not null, then we
																						// do the monthly aggregation
		Map<String, String> aoPayload = new HashMap<>();
		aoPayload.put("dataset", sDataset); // reanalysis-era5-pressure-levels
		aoPayload.put("productType", sProductType); // Reanalysis
		aoPayload.put("variables", sVariables); // U+V
		aoPayload.put("presureLevels", sPresureLevels); // 1 hPa
		aoPayload.put("boundingBox", sBoundingBox); // North 10�, West 5�, South 5�, East 7
		aoPayload.put("format", sFormat); // grib | netcdf
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
			Date oStartDate, Date oEndDate) {
		String sStartDate = Utils.formatToYyyyMMdd(oStartDate);
		String sStartDateTime = TimeEpochUtils.fromEpochToDateString(oStartDate.getTime());
		String sExtension = "." + oQuery.timeliness;
		String sFileName = String.join("_", Platforms.ERA5, oQuery.productName, oQuery.sensorMode, sStartDate).replaceAll("[\\W]", "_") + sExtension; // TODO: does the fileName change for the aggregated results?
		String sUrl = s_oDataProviderConfig.link + "?payload=" + sPayload;
		String sEncodedUrl = StringUtils.encodeUrl(sUrl);

		QueryResultViewModel oResult = new QueryResultViewModel();
		oResult.setId(sFileName);
		oResult.setTitle(sFileName);
		oResult.setLink(sEncodedUrl);
		oResult.setSummary("Date: " + sStartDateTime + ", Mode: " + oQuery.sensorMode + ", Instrument: " + oQuery.timeliness); // TODO: does the summary change for the aggregated results?
		oResult.setProvider(m_sProvider);
		oResult.setFootprint(sFootPrint);
		oResult.getProperties().put("platformname", Platforms.ERA5);
		oResult.getProperties().put("dataset", oQuery.productName);
		oResult.getProperties().put("productType", oQuery.productType);
		oResult.getProperties().put("presureLevels", oQuery.productLevel);
		oResult.getProperties().put("variables", oQuery.sensorMode);
		oResult.getProperties().put("format", oQuery.timeliness);
		oResult.getProperties().put("startDate", sStartDateTime);
		oResult.getProperties().put("beginposition", sStartDateTime);

		if (oEndDate != null) { // TODO: is it ok to add this properties when we have aggregated results?
			String sEndDateTime = TimeEpochUtils.fromEpochToDateString(oEndDate.getTime());
			oResult.getProperties().put("endDate", sEndDateTime);
			oResult.getProperties().put("endposition", sEndDateTime);
		}
		return oResult;
	}
	
}
