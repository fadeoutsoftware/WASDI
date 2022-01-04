package wasdi.shared.queryexecutors.cds;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
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
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
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
		
		m_sProvider="CDS";
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

			int iDays = TimeEpochUtils.countDaysIncluding(oCDSQuery.startFromDate, oCDSQuery.endToDate);

			iCount = iDays;

			return iCount;			
		}
		catch (Exception oEx) {
			Utils.debugLog("QueryExecutorCDS.executeCount: error " + oEx.toString());
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
				Utils.debugLog("QueryExecutorCDS.executeAndRetrieve: " + oE.toString());
			}

			try {
				iLimit = Integer.parseInt(sLimit);
			} catch (Exception oE) {
				Utils.debugLog("QueryExecutorCDS.executeAndRetrieve: " + oE.toString());
			}

			int iDays = TimeEpochUtils.countDaysIncluding(oCDSQuery.startFromDate, oCDSQuery.endToDate);
			for (int i = 0; i < iDays; i++) {
				Date oActualDay = TimeEpochUtils.getLaterDate(lStart, i);

				if (iActualElement >= iOffset && iActualElement < iOffset + iLimit) {
					QueryResultViewModel oResult = new QueryResultViewModel();

					String sDataset = oCDSQuery.productName;
					String sProductType = oCDSQuery.productType;
					String sPresureLevels = oCDSQuery.productLevel;
					String sVariables = oCDSQuery.sensorMode;
					String sFormat = oCDSQuery.timeliness;

					String sBoundingBox = oCDSQuery.north + ", " + oCDSQuery.west + ", " + oCDSQuery.south + ", " + oCDSQuery.east;

					String sDate = Utils.formatToYyyyMMdd(oActualDay);
					String sExtension = "." + sFormat;

					String sFileName = String.join("_", Platforms.ERA5, sDataset, sVariables, sDate).replaceAll("[\\W]", "_") + sExtension;

					oResult.setId(sFileName);
					oResult.setTitle(sFileName);

					String sPayload = prepareLinkJsonPayload(sDataset, sProductType, sVariables, sPresureLevels, sDate, sBoundingBox, sFormat);

					String sUrl = s_oDataProviderConfig.link + "?payload=" + sPayload;
					String sUrlEncoded = encodeUrl(sUrl);

					oResult.setLink(sUrlEncoded);
					String sDateTime = TimeEpochUtils.fromEpochToDateString(oActualDay.getTime());
					oResult.setSummary("Date: "  + sDateTime +  ", Mode: " + oCDSQuery.sensorMode +  ", Instrument: " + oCDSQuery.timeliness);
					oResult.setProvider(s_oDataProviderConfig.name);
					oResult.setFootprint(extractFootprint(oQuery.getQuery()));
					oResult.getProperties().put("platformname", Platforms.ERA5);
					oResult.getProperties().put("dataset", oCDSQuery.productName);
					oResult.getProperties().put("productType", oCDSQuery.productType);
					oResult.getProperties().put("presureLevels", oCDSQuery.productLevel);
					oResult.getProperties().put("variables", oCDSQuery.sensorMode);
					oResult.getProperties().put("format", oCDSQuery.timeliness);
					oResult.getProperties().put("startDate", sDateTime);
					oResult.getProperties().put("beginposition", sDateTime);

					aoResults.add(oResult);
				}

				iActualElement ++;

				if (iActualElement > iOffset + iLimit) break;
			}

			return aoResults;			
		}
		catch (Exception oEx) {
			Utils.debugLog("QueryExecutorCDS.executeAndRetrieve: error " + oEx.toString());
		}
		
		return null;
	}

	private static String prepareLinkJsonPayload(String sDataset, String sProductType, String sVariables, String sPresureLevels, String sDate, String sBoundingBox, String sFormat) {
		Map<String, String> aoPayload = new HashMap<>();
		aoPayload.put("dataset", sDataset); // reanalysis-era5-pressure-levels
		aoPayload.put("productType", sProductType); // Reanalysis
		aoPayload.put("variables", sVariables); // U+V
		aoPayload.put("presureLevels", sPresureLevels); // 1 hPa
		aoPayload.put("date", sDate); // 20211201
		aoPayload.put("boundingBox", sBoundingBox); // North 10°, West 5°, South 5°, East 7
		aoPayload.put("format", sFormat); // grib | netcdf

		String sPayload = null;
		try {
			sPayload = s_oMapper.writeValueAsString(aoPayload);
		} catch (Exception oE) {
			Utils.debugLog("QueryExecutorCDS.prepareLinkJsonPayload: could not serialize payload due to " + oE + ".");
		}

		return sPayload;
	}

	private static String encodeUrl(String sUrl) {
		try {
			return URLEncoder.encode(sUrl, java.nio.charset.StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException oE) {
			Utils.debugLog("QueryExecutorCDS.encodeUrl: could not encode URL due to " + oE + ".");
		}

		return sUrl;
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

}
