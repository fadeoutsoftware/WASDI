package wasdi.shared.queryexecutors.terrascope;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import wasdi.shared.queryexecutors.QueryTranslator;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryTranslatorTerrascope extends QueryTranslator {

	private static final String TERRASCOPE_CATALOGUE_URL = "https://stac.terrascope.be/search?";

	private static Map<String, String> s_asCollectionNames;

	static {
		s_asCollectionNames = new HashMap<>();

//		s_asCollectionNames.put("Sentinel-1:GRD", "urn:eop:VITO:CGS_S1_GRD_L1");
//		s_asCollectionNames.put("Sentinel-1:SLC", "urn:eop:VITO:CGS_S1_SLC_L1");
//		s_asCollectionNames.put("Sentinel-1", "urn:eop:VITO:CGS_S1_GRD_L1,urn:eop:VITO:CGS_S1_SLC_L1");
//
//		s_asCollectionNames.put("Sentinel-2:S2MSI1C", "urn:eop:VITO:CGS_S2_L1C");
//		s_asCollectionNames.put("Sentinel-2", "urn:eop:VITO:CGS_S2_L1C");
//
//		s_asCollectionNames.put("Proba-V:urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_333M_V001", "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_333M_V001");
//		s_asCollectionNames.put("Proba-V:urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_333M_V001", "urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_333M_V001");
//		s_asCollectionNames.put("Proba-V:urn:ogc:def:EOP:VITO:PROBAV_S5-TOC_100M_V001", "urn:ogc:def:EOP:VITO:PROBAV_S5-TOC_100M_V001");
//		s_asCollectionNames.put("Proba-V", "urn:ogc:def:EOP:VITO:PROBAV_S1-TOC_333M_V001,urn:ogc:def:EOP:VITO:PROBAV_S10-TOC_333M_V001,urn:ogc:def:EOP:VITO:PROBAV_S5-TOC_100M_V001");
//
//		s_asCollectionNames.put("DEM:DEM_30M", "urn:eop:VITO:COP_DEM_GLO_30M_COG");
//		s_asCollectionNames.put("DEM:DEM_90M", "urn:eop:VITO:COP_DEM_GLO_90M_COG");
//		s_asCollectionNames.put("DEM", "urn:eop:VITO:COP_DEM_GLO_30M_COG,urn:eop:VITO:COP_DEM_GLO_90M_COG");

		s_asCollectionNames.put("WorldCover:10m_2020_V1", "esa-worldcover-map-10m-2020-v1");
		s_asCollectionNames.put("WorldCover:10m_2021_V2", "esa-worldcover-map-10m-2021-v2");
		s_asCollectionNames.put("WorldCover:S1VVVHratio_10m_2020_V1", "esa-worldcover-s1vvvhratio-10m-2020-v1");
//		s_asCollectionNames.put("WorldCover:S2RGBNIR_10m_2020_V1", "urn:eop:VITO:ESA_WorldCover_S2RGBNIR_10m_2020_V1");
//		s_asCollectionNames.put("WorldCover", "urn:eop:VITO:ESA_WorldCover_10m_2020_V1,urn:eop:VITO:ESA_WorldCover_S1VVVHratio_10m_2020_V1,urn:eop:VITO:ESA_WorldCover_S2RGBNIR_10m_2020_V1");
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

		String sTimePeriod = "";

		if (Platforms.SENTINEL1.equals(oWasdiQuery.platformName)
				|| Platforms.SENTINEL2.equals(oWasdiQuery.platformName)
				|| Platforms.WORLD_COVER.equals(oWasdiQuery.platformName)) {
			// datetime=2020-01-05T00:00:00Z/2020-01-15T23:59:59Z
			sTimePeriod = "&datetime=" + sTimeStart + "T00:00:00Z/" + sTimeEnd + "T23:59:59Z";
		} else if (Platforms.DEM.equals(oWasdiQuery.platformName)) {
			// processingDate=[2022-01-01,2022-01-13]
			sTimePeriod = "&processingDate=[" + sTimeStart + "," + sTimeEnd + "]";
		}

		sTerrascopeQuery += sTimePeriod;

		// Set the Bbox
		String sBbox = "";

		if (oWasdiQuery.north != null && oWasdiQuery.south != null && oWasdiQuery.east != null && oWasdiQuery.west != null) {
			sBbox = "&bbox=" + oWasdiQuery.west +"," + oWasdiQuery.south + "," + oWasdiQuery.east +"," + oWasdiQuery.north;
		}

		sTerrascopeQuery += sBbox;

		if (oWasdiQuery.limit > 0) {
			String sCount = "&limit=" + oWasdiQuery.limit;
			sTerrascopeQuery += sCount;
		}

		JSONObject oStacQuery = new JSONObject();

		if (Platforms.SENTINEL1.equals(oWasdiQuery.platformName)) {
			if (!Utils.isNullOrEmpty(oWasdiQuery.sensorMode)) {
				oStacQuery.put("sar:instrument_mode", new JSONObject().put("eq", oWasdiQuery.sensorMode));
			}
		}

		if (Platforms.SENTINEL2.equals(oWasdiQuery.platformName)) {
			JSONObject oCloudCoverageFilter = new JSONObject();
			if (oWasdiQuery.cloudCoverageFrom != null) {
				oCloudCoverageFilter.put("gte", oWasdiQuery.cloudCoverageFrom);
			}
			if (oWasdiQuery.cloudCoverageTo != null) {
				oCloudCoverageFilter.put("lte", oWasdiQuery.cloudCoverageTo);
			}
			if (oCloudCoverageFilter.length() > 0) {
				oStacQuery.put("eo:cloud_cover", oCloudCoverageFilter);
			}
		}

		if (oWasdiQuery.relativeOrbit >= 0
				&& (Platforms.SENTINEL1.equals(oWasdiQuery.platformName)
						|| Platforms.SENTINEL2.equals(oWasdiQuery.platformName))) {
			oStacQuery.put("sat:relative_orbit", new JSONObject().put("eq", oWasdiQuery.relativeOrbit));
		}

		if (!Utils.isNullOrEmpty(oWasdiQuery.productName)) {
			String sProductNameProperty = "id";
			if (Platforms.SENTINEL1.equals(oWasdiQuery.platformName)) {
				sProductNameProperty = "s1:product_identifier";
			} else if (Platforms.SENTINEL2.equals(oWasdiQuery.platformName)) {
				sProductNameProperty = "s2:product_uri";
			}
			oStacQuery.put(sProductNameProperty, new JSONObject().put("eq", oWasdiQuery.productName));
		}

		if (oStacQuery.length() > 0) {
			sTerrascopeQuery += "&query=" + encodeQueryParameter(oStacQuery.toString());
		}

		String sCollectionNames = retrieveCollectionName(oWasdiQuery.platformName, oWasdiQuery.productType);

		if (Utils.isNullOrEmpty(sCollectionNames)) {
			return Collections.emptyList();
		}

		String[] aoCollectionNames = sCollectionNames.split(",");

		List<String> aoUrls = new ArrayList<>(aoCollectionNames.length);

		for (String sCollectionName : aoCollectionNames) {
			if (!Utils.isNullOrEmpty(sCollectionName)) {
				String sBaseAddress = TERRASCOPE_CATALOGUE_URL + "collections=" + sCollectionName;

				String sUrl = sBaseAddress + sTerrascopeQuery;
				aoUrls.add(sUrl);
			}
		}

		return aoUrls;
	}

	private static String encodeQueryParameter(String sQueryParameter) {
		try {
			return URLEncoder.encode(sQueryParameter, "UTF-8");
		} catch (UnsupportedEncodingException oEx) {
			throw new IllegalStateException("UTF-8 is not available", oEx);
		}
	}

	// platformName = Sentinel-1 productType = GRD
	private static String retrieveCollectionName(String platformName, String productType) {
		String key = platformName;

		if (!Utils.isNullOrEmpty(productType)) {
			key += ":" + productType;
		}

		return s_asCollectionNames.get(key);
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
