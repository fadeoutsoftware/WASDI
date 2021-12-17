package wasdi.shared.queryexecutors.cds;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

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

	public QueryExecutorCDS() {
		
		m_sProvider="CDS";
		
		this.m_oQueryTranslator = new QueryTranslatorCDS();
		this.m_oResponseTranslator = new ResponseTranslatorCDS();
		
		m_asSupportedPlatforms.add(Platforms.ERA5);
	}

	@Override
	public int executeCount(String sQuery) {
		int iCount = 0;

		// Parse the query
		QueryViewModel oCDSQuery = m_oQueryTranslator.parseWasdiClientQuery(sQuery);

		if (!m_asSupportedPlatforms.contains(oCDSQuery.platformName)) {
			return 0;
		}

		int iDays = TimeEpochUtils.countDaysIncluding(oCDSQuery.startFromDate, oCDSQuery.endToDate);

		iCount = iDays;

		return iCount;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
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

				System.out.println();
				System.out.println("oCDSQuery.platformName = platformName: " + oCDSQuery.platformName);
				System.out.println("oCDSQuery.productName = dataset: " + oCDSQuery.productName);
				System.out.println("oCDSQuery.productType = productType: " + oCDSQuery.productType);
				System.out.println("oCDSQuery.productLevels = presureLevels: " + oCDSQuery.productLevel);
				System.out.println("oCDSQuery.sensorMode = variables : " + oCDSQuery.sensorMode);
				System.out.println("oCDSQuery.timeliness = format : " + oCDSQuery.timeliness);

//				String sPlatformName = oCDSQuery.platformName;
				String sDataset = oCDSQuery.productName;
				String sProductType = oCDSQuery.productType;
				String sPresureLevels = oCDSQuery.productLevel;
				String sVariables = oCDSQuery.sensorMode;
				String sFormat = oCDSQuery.timeliness;

				System.out.println("oCDSQuery.boundingBox: " + oCDSQuery.north + ", " + oCDSQuery.west + ", " + oCDSQuery.south + ", " + oCDSQuery.east);
				
				String sBoundingBox = oCDSQuery.north + ", " + oCDSQuery.west + ", " + oCDSQuery.south + ", " + oCDSQuery.east;
				System.out.println("boundingBox: " + sBoundingBox);
				System.out.println();

				// filename: dataset_variabile_giorno
				// url: dataset; variabile; giorno; bb; URL JSON encoded
				// platform_name -> dataset (reanalysis-era5-pressure-levels)
				// product_type -> variabile (U, V)

				String sDate = Utils.formatToYyyyMMdd(oActualDay);
				String sExtension = "." + sFormat;

//				String sFileName = oCDSQuery.productLevel + "_" + oCDSQuery.productType.replace(" ", "") + "_" + sDate;
				String sFileName = String.join("_", sDataset, sVariables, sDate).replaceAll("[\\W]|_", "_") + sExtension;

//				String sFormat = ".netcdf";
				System.out.println("sFileName: " + sFileName);

				oResult.setId(sFileName);
				oResult.setTitle(sFileName);

//				String sPayload = prepareLinkJsonPayload(sPlatformName, oCDSQuery.productType, oCDSQuery.productLevel, sDate, sBoundingBox, sFormat);
				String sPayload = prepareLinkJsonPayload(sDataset, sProductType, sVariables, sPresureLevels, sDate, sBoundingBox, sFormat);
				System.out.println("payload: " + sPayload);

				String sUrl = "https://cds.climate.copernicus.eu/api/v2/resources" + "?payload=" + sPayload;
				String sUrlEncoded = encodeUrl(sUrl);

				oResult.setLink(sUrlEncoded);
				oResult.setSummary("Date: "  + TimeEpochUtils.fromEpochToDateString(oActualDay.getTime()) +  ", Mode: Reanalysis, Satellite: CDS");
				oResult.setProvider("CDS");
				oResult.setFootprint(extractFootprint(oQuery.getQuery()));
				oResult.getProperties().put("platformname", "ERA5");

				aoResults.add(oResult);
			}

			iActualElement ++;

			if (iActualElement > iOffset + iLimit) break;
		}

		return aoResults;
	}

//	private static String prepareLinkJsonPayload(String sPlatformName, String sProductType, String sProductLevel, String sDate, String sBoundingBox, String sFormat) {
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
			System.out.println("sFootprint: " + sFootprint);
		}

		return sFootprint;
	}

//	private String queryViewModelToString(QueryViewModel oCDSQuery) {
//		return "QueryViewModel [offset=" + oCDSQuery.offset + ", limit=" + oCDSQuery.limit + ", north=" + oCDSQuery.north + ", south=" + oCDSQuery.south
//				+ ", east=" + oCDSQuery.east + ", west=" + oCDSQuery.west + ", startFromDate=" + oCDSQuery.startFromDate + ", startToDate="
//				+ oCDSQuery.startToDate + ", endFromDate=" + oCDSQuery.endFromDate + ", endToDate=" + oCDSQuery.endToDate + ", platformName="
//				+ oCDSQuery.platformName + ", productType=" + oCDSQuery.productType + ", productLevel=" + oCDSQuery.productLevel + ", relativeOrbit="
//				+ oCDSQuery.relativeOrbit + ", absoluteOrbit=" + oCDSQuery.absoluteOrbit + ", cloudCoverageFrom=" + oCDSQuery.cloudCoverageFrom
//				+ ", cloudCoverageTo=" + oCDSQuery.cloudCoverageTo + ", sensorMode=" + oCDSQuery.sensorMode + ", productName=" + oCDSQuery.productName
//				+ ", timeliness=" + oCDSQuery.timeliness + "]";
//	}

}
