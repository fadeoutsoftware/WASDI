package wasdi.shared.queryexecutors.lpdaac;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.config.MongoConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;


public class QueryExecutorLpDaac extends QueryExecutor {
	
	private static final String s_sEARTH_DATA_BASE_URL = "https://cmr.earthdata.nasa.gov/search/granules";
	
	private static final HashMap<String, String> as_WASDI_NASA_PRODUCT_MAPPING = new HashMap<String, String>();
	
	static {
		as_WASDI_NASA_PRODUCT_MAPPING.put("MOD11A2", "C2269056084-LPCLOUD");
		as_WASDI_NASA_PRODUCT_MAPPING.put("VNP21A1D", "C2545314555-LPCLOUD");
		as_WASDI_NASA_PRODUCT_MAPPING.put("VNP21A1N", "C2545314559-LPCLOUD");
		as_WASDI_NASA_PRODUCT_MAPPING.put("MCD43A4", "C2218719731-LPCLOUD");
		as_WASDI_NASA_PRODUCT_MAPPING.put("MCD43A3", "C2278860820-LPCLOUD");
	}
	
	public QueryExecutorLpDaac() {
		m_sProvider = "LPDAAC";
		
		this.m_oQueryTranslator = new QueryTranslatorLpDaac();
		this.m_oResponseTranslator = new ResponseTranslatorLpDaac();
		this.m_asSupportedPlatforms.addAll(Arrays.asList(
				Platforms.TERRA, 
				Platforms.VIIRS));
	}
	
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {
		if (sProduct.toUpperCase().startsWith("MOD11A2")
				|| sProduct.toUpperCase().startsWith("VNP21A1D")
				|| sProduct.toUpperCase().startsWith("VNP21A1N")
				|| sProduct.toUpperCase().startsWith("MCD43A4")
				|| sProduct.toUpperCase().startsWith("MCD43A3")) {
			return sOriginalUrl;
		}
		return null;
	}

	@Override
	public int executeCount(String sQuery) {
		WasdiLog.debugLog("QueryExecutorLpDaac.executeCount. Query: " + sQuery);
		
		int lCount = -1;
		
		try {
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
	
			if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
				WasdiLog.warnLog("QueryExecutorLpDaac.executeCount. Unsupported platform: " + oQueryViewModel.platformName);
				return lCount;
			}
			
			Double dWest = oQueryViewModel.west;
			Double dNorth = oQueryViewModel.north;
			Double dEast = oQueryViewModel.east;
			Double dSouth = oQueryViewModel.south;
	
			String sDateFrom = oQueryViewModel.startFromDate;
			String sDateTo = oQueryViewModel.endToDate;
			
			String sProductType = oQueryViewModel.productType;
			
			// TODO: check where the name of the collection is coming from
			
			if (Utils.isNullOrEmpty(sProductType)) {
				WasdiLog.warnLog("QueryExecutorLpDaac.executeCount. Product type not specified " + oQueryViewModel.platformName);
				return lCount;
			}
			
			String sEarthDataCollectionId = as_WASDI_NASA_PRODUCT_MAPPING.get(sProductType);
			
			if (Utils.isNullOrEmpty(sEarthDataCollectionId)) {
				WasdiLog.warnLog("QueryExecutorLpDaac.executeCount. Product trype " + sProductType + " not supported by WASDI" );
				return lCount;
			}
			
			// PREPARE QUERY FOR EARTHDATA (HERE WE WILL GET THE RESPONSE IN XML FORMAT SINCE WE ARE ONLY INTERESTED TO GET THE NUMBER OF HITS)
			String sUrlCount = s_sEARTH_DATA_BASE_URL;
			
			sUrlCount += getEarthDataQueryParameters(sEarthDataCollectionId, sDateFrom, sDateTo,  dWest, dNorth, dEast, dSouth);
			
			HttpCallResponse oHttpResponse = HttpUtils.httpGet(sUrlCount);
			
			int iResponseCode = oHttpResponse.getResponseCode();
			
			if (iResponseCode >= 200 && iResponseCode <= 299) {
				String sResponseBody = oHttpResponse.getResponseBody();
				Pattern oPattern = Pattern.compile("<hits>(\\d+)</hits>");
				Matcher oMatcher = oPattern.matcher(sResponseBody);
				
				if (oMatcher.find()) {
					String sHitsValue = oMatcher.group(1);
					lCount = Integer.parseInt(sHitsValue);
					WasdiLog.debugLog("QueryExecutorLpDaac.executeCount. Retrieved number of results: " + lCount);
				} else {
					WasdiLog.warnLog("QueryExecutorLpDaac.executeCount. Hits not found in the response");
				}
			} else {
				WasdiLog.warnLog("QueryExecutorLpDaac.executeCount. Error contacting the data privider " + iResponseCode);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorLpDaac.executeCount. Exception", oEx);
		}

		return lCount;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		WasdiLog.debugLog("QueryExecutorLpDaac.executeAndRetrieve. Query: " + oQuery.getQuery());

		String sOffset = oQuery.getOffset();
		String sLimit = oQuery.getLimit();

		int iOffset = 0;
		int iLimit = 10;

		try {
			iOffset = Integer.parseInt(sOffset);
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryExecutorLpDaac.executeAndRetrieve. Impossible to parse offset " + sOffset + ". " + oE.toString());
		}

		try {
			iLimit = Integer.parseInt(sLimit);
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryExecutorLpDaac.executeAndRetrieve. Impossible to parse limit " + sLimit + ". "+ oE.toString());
		}

		List<QueryResultViewModel> aoResults = new ArrayList<>();
		
		try {

			// Parse the query
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
	
			if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
				WasdiLog.debugLog("QueryExecutorLpDaac.executeAndRetrieve. Unsupported platform: " + oQueryViewModel.platformName);
				return null;
			}
	
			Double dWest = oQueryViewModel.west;
			Double dNorth = oQueryViewModel.north;
			Double dEast = oQueryViewModel.east;
			Double dSouth = oQueryViewModel.south;
	
			String sDateFrom = oQueryViewModel.startFromDate;
			String sDateTo = oQueryViewModel.endToDate;
			
			String sProductType = oQueryViewModel.productType;
			
			if (Utils.isNullOrEmpty(sProductType)) {
				WasdiLog.warnLog("QueryExecutorLpDaac.executeAndRetrieve. Product trype not specified");
				return null;
			}
			
			String sEarthDataCollectionId = as_WASDI_NASA_PRODUCT_MAPPING.get(sProductType);
			
			if (Utils.isNullOrEmpty(sEarthDataCollectionId)) {
				WasdiLog.warnLog("QueryExecutorLpDaac.executeAndRetrieve. Product trype " + sProductType + " not supported by WASDI" );
				return null;
			}
			
			// PREPARE QUERY FOR EARTHDATA (HERE WE WILL GET THE RESPONSE IN JSON FORMAT SINCE WE WANT THE TO FILL THE RESULT VIEW MODELS)
			String sSearchUrl = s_sEARTH_DATA_BASE_URL + ".json";
					
			sSearchUrl += getEarthDataQueryParameters(sEarthDataCollectionId, sDateFrom, sDateTo,  dWest, dNorth, dEast, dSouth, iLimit, iOffset);
			
			HttpCallResponse oHttpResponse = HttpUtils.httpGet(sSearchUrl);
			
			int iResponseCode = oHttpResponse.getResponseCode();
			
			if (iResponseCode >= 200 && iResponseCode <= 299) {
				String sResponseBody = oHttpResponse.getResponseBody();
				List<QueryResultViewModel> oResultList = this.m_oResponseTranslator.translateBatch(sResponseBody, bFullViewModel);
				return oResultList;
				
			} else {
				WasdiLog.warnLog("QueryExecutorLpDaac.executeAndRetrieve. Error contacting the data privider " + iResponseCode);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorLpDaac.executeAndRetrieve. Exception", oEx);
		}

		return aoResults;
	}
	
	public String getEarthDataQueryParameters(String sCollectionConceptId, String sDateFrom, String sDateTo, Double dWest, Double dNorth, Double dEast, Double dSouth) {
		return getEarthDataQueryParameters(sCollectionConceptId, sDateFrom, sDateTo, dWest, dNorth, dEast, dSouth, -1, -1);
	}
	
	public String getEarthDataQueryParameters(String sCollectionConceptId, String sDateFrom, String sDateTo, Double dWest, Double dNorth, Double dEast, Double dSouth, int iLimit, int iOffset) {
		List<String> asParameters = new ArrayList<>();
		
		// add collection concept id
		if (!Utils.isNullOrEmpty(sCollectionConceptId)) {
			asParameters.add("collection_concept_id=" + sCollectionConceptId);
		}
		
		// add temporal filter
		if (!Utils.isNullOrEmpty(sDateFrom) && !Utils.isNullOrEmpty(sDateTo)) {
			DateTimeFormatter oFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
				
			ZonedDateTime oDateTimeInstant = ZonedDateTime.parse(sDateFrom);
			String sEarthDataStartTime = oFormatter.format(oDateTimeInstant);
				
			oDateTimeInstant = ZonedDateTime.parse(sDateTo);
			String sEarthDataEndTime = oFormatter.format(oDateTimeInstant);
				
			 asParameters.add("temporal=" + sEarthDataStartTime + "," + sEarthDataEndTime);
		}
		
		// add spatial filter
		if (dNorth != null && dSouth != null && dWest != null && dEast != null) {
			asParameters.add("bounding_box=" + dWest + "," + dSouth + "," + dEast + "," + dNorth);
		} 
		
		// add limit
		if (iLimit >= 0) {
			asParameters.add("page_size=" + iLimit);
		}
		
		// add offset
		if (iOffset >= 0) {
			asParameters.add("offset=" + iOffset);
		}
		
		if (asParameters.size() > 0) {
			return "?" + String.join("&", asParameters);
		}
		
		return "";	

	}
	
	@Override
	public void init() {
		
		super.init();
		
		if (Utils.isNullOrEmpty(this.m_sParserConfigPath)) {
			WasdiLog.errorLog("QueryExecutorLpDaac.init. Path to parser config is empty. It won't be possible to establish a connection to the db");
			return;
		}
		
		Stream<String> oLinesStream = null;
    	try {
    		
    		oLinesStream = Files.lines(Paths.get(this.m_sParserConfigPath), StandardCharsets.UTF_8);
    		String sModisConfigJson = oLinesStream.collect(Collectors.joining(System.lineSeparator()));
    		ObjectMapper oMapper = new ObjectMapper(); 
            MongoConfig oModisConfig = oMapper.readValue(sModisConfigJson, MongoConfig.class);
            MongoRepository.addMongoConnection("modis", oModisConfig.user, oModisConfig.password, oModisConfig.address, oModisConfig.replicaName, oModisConfig.dbName);
            
        } 
    	catch (Exception oEx) {
        	WasdiLog.errorLog("QueryExecutorLpDaac.init. Error while trying to connect to MODIS db. ", oEx);
        } 
    	finally {	
        	if (oLinesStream != null)
        		oLinesStream.close();
        }
    	
	}

}
