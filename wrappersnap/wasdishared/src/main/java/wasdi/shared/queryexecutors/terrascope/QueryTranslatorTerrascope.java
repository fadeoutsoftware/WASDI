package wasdi.shared.queryexecutors.terrascope;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.queryexecutors.QueryTranslator;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryTranslatorTerrascope extends QueryTranslator {

	private static final String TERRASCOPE_CATALOGUE_URL = "https://services.terrascope.be/catalogue/products?";

	private static Map<String, String> collectionNames;

	static {
		collectionNames = new HashMap<>();

		collectionNames.put("Sentinel-1:GRD", "urn:eop:VITO:CGS_S1_GRD_L1");
		collectionNames.put("Sentinel-1:SLC", "urn:eop:VITO:CGS_S1_SLC_L1");
		collectionNames.put("Sentinel-1", "urn:eop:VITO:CGS_S1_GRD_L1,urn:eop:VITO:CGS_S1_SLC_L1");

		collectionNames.put("Sentinel-2:S2MSI1C", "urn:eop:VITO:CGS_S2_L1C");
		collectionNames.put("Sentinel-2", "urn:eop:VITO:CGS_S2_L1C");

		collectionNames.put("Proba-V:urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_333M_V001", "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_333M_V001");
		collectionNames.put("Proba-V:urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_333M_V001", "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_333M_V001");
		collectionNames.put("Proba-V:urn:ogc:def:EOP:VITO:PROBAV_S5-TOC_100M_V001", "urn:ogc:def:EOP:VITO:PROBAV_S5-TOC_100M_V001");
		collectionNames.put("Proba-V", "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_333M_V001,urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_333M_V001,urn:ogc:def:EOP:VITO:PROBAV_S5-TOC_100M_V001");

		collectionNames.put("DEM:DEM_30M", "urn:eop:VITO:COP_DEM_GLO_30M_COG");
		collectionNames.put("DEM:DEM_90M", "urn:eop:VITO:COP_DEM_GLO_90M_COG");
		collectionNames.put("DEM", "urn:eop:VITO:COP_DEM_GLO_30M_COG,urn:eop:VITO:COP_DEM_GLO_90M_COG");

		collectionNames.put("WorldCover:10m_2020_V1", "urn:eop:VITO:ESA_WorldCover_10m_2020_V1");
		collectionNames.put("WorldCover:10m_2021_V2", "urn:eop:VITO:ESA_WorldCover_10m_2021_V2");
		collectionNames.put("WorldCover:S1VVVHratio_10m_2020_V1", "urn:eop:VITO:ESA_WorldCover_S1VVVHratio_10m_2020_V1");
		collectionNames.put("WorldCover:S2RGBNIR_10m_2020_V1", "urn:eop:VITO:ESA_WorldCover_S2RGBNIR_10m_2020_V1");
		collectionNames.put("WorldCover", "urn:eop:VITO:ESA_WorldCover_10m_2020_V1,urn:eop:VITO:ESA_WorldCover_S1VVVHratio_10m_2020_V1,urn:eop:VITO:ESA_WorldCover_S2RGBNIR_10m_2020_V1");
	}

	protected List<String> translateMultiple(String sQueryFromClient) {
		// Ouptut Query
		String sTerrascopeQuery = "";

		// Convert the WASDI Query in the view model
		QueryViewModel oWasdiQuery = parseWasdiClientQuery(sQueryFromClient);

		if (Utils.isNullOrEmpty(oWasdiQuery.platformName)
				|| Utils.isNullOrEmpty(oWasdiQuery.startFromDate)
				|| Utils.isNullOrEmpty(oWasdiQuery.endToDate)) {
			return Collections.emptyList();
		}

		// Set start and end date
		String sTimeStart = oWasdiQuery.startFromDate.substring(0, 10);
		String sTimeEnd = oWasdiQuery.endToDate.substring(0, 10);
		// increment the end day by one, because the upper limit is excluded:
		// Terrascope considers the timestamp, hence the moment considered is 00:00:00
		sTimeEnd = LocalDate.parse(sTimeEnd).plusDays(1).toString();

		String sTimePeriod = "";

		if (Platforms.SENTINEL1.equals(oWasdiQuery.platformName)
				|| Platforms.SENTINEL2.equals(oWasdiQuery.platformName)
				|| Platforms.WORLD_COVER.equals(oWasdiQuery.platformName)) {
			// start=2022-01-01&end=2022-01-06
			sTimePeriod = "&start=" + sTimeStart + "&end=" + sTimeEnd;
		} else if (Platforms.DEM.equals(oWasdiQuery.platformName)) {
			// processingDate=[2022-01-01,2022-01-13]
			sTimePeriod = "&processingDate=[" + sTimeStart + "," + sTimeEnd + "]";
		}

		sTerrascopeQuery += sTimePeriod;

		if (oWasdiQuery.limit > 0) {
			String sCount = "&count=" + oWasdiQuery.limit;
			sTerrascopeQuery += sCount;
		}

		// Set the start index:
		if (oWasdiQuery.offset >= 0) {
			int iOffset = oWasdiQuery.offset + 1;
			String sOffset= "&startIndex=" + iOffset;
			sTerrascopeQuery += sOffset;
		}

		// Set the Bbox
		String sBbox = "";

		if (oWasdiQuery.north != null && oWasdiQuery.south != null && oWasdiQuery.east != null && oWasdiQuery.west != null) {
			// P.Campanella 16/02/2021: change geographic filter to avoid geoserver intersection bug.
			//sBbox = "&box=" + oWasdiQuery.west + "," + oWasdiQuery.south + "," + oWasdiQuery.east + "," + oWasdiQuery.north;
			sBbox = "&geometry=POLYGON((" + oWasdiQuery.west +" " + oWasdiQuery.south + ", " + oWasdiQuery.east +" " + oWasdiQuery.south + ", " + oWasdiQuery.east + " " + oWasdiQuery.north + ", " + oWasdiQuery.west + " " + oWasdiQuery.north+ ", " + oWasdiQuery.west  +" " + oWasdiQuery.south + "))";
		}

		sTerrascopeQuery += sBbox;

		// Set Sensor Mode
		if (!Utils.isNullOrEmpty(oWasdiQuery.sensorMode)) {
			String sSensorMode = "&SensorMode=" + oWasdiQuery.sensorMode;
			sTerrascopeQuery += sSensorMode;
		}

		// Set Cloud Coverage
		if (oWasdiQuery.cloudCoverageFrom != null && oWasdiQuery.cloudCoverageTo != null) {
			int iFrom = oWasdiQuery.cloudCoverageFrom.intValue();
			int iTo = oWasdiQuery.cloudCoverageTo.intValue();

			String sCloudCoverage = "&cloudCover=%5B" + iFrom + "," + iTo + "%5D";
			sTerrascopeQuery += sCloudCoverage;
		}

		// Set Relative Orbit
		if (oWasdiQuery.relativeOrbit >= 0) {
			String sOrbitNumber = "&orbitNumber=" + oWasdiQuery.relativeOrbit;
			sTerrascopeQuery += sOrbitNumber;
		}

		//add free text search, assuming it's the product id
		if (!Utils.isNullOrEmpty(oWasdiQuery.productName)) {
			sTerrascopeQuery += "&uid=" + oWasdiQuery.productName;
		}

		String sCollectionNames = retrieveCollectionName(oWasdiQuery.platformName, oWasdiQuery.productType);

		if (Utils.isNullOrEmpty(sCollectionNames)) {
			return Collections.emptyList();
		}

		String[] aoCollectionNames = sCollectionNames.split(",");

		List<String> aoUrls = new ArrayList<>(aoCollectionNames.length);

		for (String sCollectionName : aoCollectionNames) {
			if (!Utils.isNullOrEmpty(sCollectionName)) {
				String sBaseAddress = TERRASCOPE_CATALOGUE_URL + "collection=" + sCollectionName;

				String sUrl = sBaseAddress + sTerrascopeQuery;
				aoUrls.add(sUrl);
			}
		}

		return aoUrls;
	}

	// platformName = Sentinel-1 productType = GRD
	private static String retrieveCollectionName(String platformName, String productType) {
		String key = platformName;

		if (!Utils.isNullOrEmpty(productType)) {
			key += ":" + productType;
		}

		return collectionNames.get(key);
	}

	@Override
	protected String parseTimeFrame(String sQuery) {
		return null;
	}

	@Override
	protected String parseFootPrint(String sQuery) {
		return null;
	}

	@Override
	public String getCountUrl(String oQuery) {
		return null;
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		return null;
	}

	public String encode(String sDecoded) {
		return super.encode(sDecoded);
	}

}
