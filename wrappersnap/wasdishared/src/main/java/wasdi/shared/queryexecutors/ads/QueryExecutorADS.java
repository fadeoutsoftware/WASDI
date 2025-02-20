package wasdi.shared.queryexecutors.ads;

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
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * ADS Query Executor.
 * This class queries the ADS Catalogue from https://ads.atmosphere.copernicus.eu/api/v2/
 * 
 * The query in this case is local: the catalogue does not have API.
 * ADS are daily maps in a pre-defined world grid.
 * Maps are available one per day per each tile of the grid.
 * 
 * The query is obtained with a local shape file that reproduces the ADS grid
 * It intersects the users' AOI, find involved tiles and returns one result per day per tile
 * 
 * @author PetruPetrescu
 *
 */
public class QueryExecutorADS extends QueryExecutor {

	private static ObjectMapper s_oMapper = new ObjectMapper();

	public QueryExecutorADS() {
		this.m_oQueryTranslator = new QueryTranslatorADS();
		this.m_oResponseTranslator = new ResponseTranslatorADS();
	}
	
	/**
	 * Overload of the get URI from Product Name method.
	 * For ADS, we need just the original link..
	 */
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {
		if (sProduct.startsWith(Platforms.CAMS)) {
			return sOriginalUrl;
		}
		return null;
	}

	@Override
	public int executeCount(String sQuery) {
		
		try {
			// Parse the query
			QueryViewModel oADSQuery = m_oQueryTranslator.parseWasdiClientQuery(sQuery);

			if (!m_asSupportedPlatforms.contains(oADSQuery.platformName)) {
				return -1;
			}

			return 1;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorADS.executeCount: error " + oEx.toString());
		}
		
		return -1;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		try {
			List<QueryResultViewModel> aoResults = new ArrayList<>();

			// Parse the query
			QueryViewModel oADSQuery = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());

			if (!m_asSupportedPlatforms.contains(oADSQuery.platformName)) {
				return aoResults;
			}

			Date oStartDay = Utils.getYyyyMMddTZDate(oADSQuery.startFromDate);
			Date oEndDay = Utils.getYyyyMMddTZDate(oADSQuery.endToDate);

			QueryResultViewModel oResult = new QueryResultViewModel();

			String sDataset = oADSQuery.productName;
			String sType = oADSQuery.productType;
			String sVariables = oADSQuery.sensorMode;
			String sFormat = oADSQuery.timeliness;

			String sBoundingBox = oADSQuery.north + ", " + oADSQuery.west + ", " + oADSQuery.south + ", " + oADSQuery.east;

			String sStartDate = Utils.formatToYyyyDashMMDashdd(oStartDay);
			String sEndDate = Utils.formatToYyyyDashMMDashdd(oEndDay);
			String sDate = sStartDate + "/" + sEndDate;

			String sExtension = "." + sFormat;

			String sFileName = String.join("_", Platforms.CAMS, sDataset, sVariables, sDate).replaceAll("[\\W]", "_") + sExtension;

			oResult.setId(sFileName);
			oResult.setTitle(sFileName);

			String sPayload = prepareLinkJsonPayload(sDataset, sType, sVariables, /*sPresureLevels,*/ sDate, sBoundingBox, sFormat);

			String sUrl = m_oDataProviderConfig.link + "?payload=" + sPayload;
			String sUrlEncoded = StringUtils.encodeUrl(sUrl);

			oResult.setLink(sUrlEncoded);
			oResult.setSummary("Date: "  + sDate +  ", Mode: " + oADSQuery.sensorMode +  ", Instrument: " + oADSQuery.timeliness);
			oResult.setProvider(m_sDataProviderCode);
			oResult.setFootprint(extractFootprint(oQuery.getQuery()));
			oResult.getProperties().put("platformname", Platforms.CAMS);
			oResult.getProperties().put("dataset", oADSQuery.productName);
			oResult.getProperties().put("productType", oADSQuery.productType);
			oResult.getProperties().put("variables", oADSQuery.sensorMode);
			oResult.getProperties().put("format", oADSQuery.timeliness);
			oResult.getProperties().put("startDate", sDate);
			oResult.getProperties().put("beginposition", sDate);

			aoResults.add(oResult);

			return aoResults;			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorADS.executeAndRetrieve: error " + oEx.toString());
		}
		
		return null;
	}

	private static String prepareLinkJsonPayload(String sDataset, String sType, String sVariables, /*String sPresureLevels,*/ String sDate, String sBoundingBox, String sFormat) {
		Map<String, String> aoPayload = new HashMap<>();
		aoPayload.put("dataset", sDataset); // cams-global-atmospheric-composition-forecasts
		aoPayload.put("type", sType); // forecast
		aoPayload.put("variables", sVariables); // U+V
		aoPayload.put("date", sDate); // 20211201
		aoPayload.put("boundingBox", sBoundingBox); // North 10�, West 5�, South 5�, East 7
		aoPayload.put("format", sFormat); // grib | netcdf

		String sPayload = null;
		try {
			sPayload = s_oMapper.writeValueAsString(aoPayload);
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryExecutorADS.prepareLinkJsonPayload: could not serialize payload due to " + oE + ".");
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

}
