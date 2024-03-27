package wasdi.shared.queryexecutors.lpdaac;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.business.modis11a2.ModisItemForReading;
import wasdi.shared.config.MongoConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.modis11a2.ModisRepository;
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
	
	private static String s_sEARTH_DATA_BASE_URL = "https://cmr.earthdata.nasa.gov/search/granules";
	
	public QueryExecutorLpDaac() {
		m_sProvider = "LPDAAC";
		
		this.m_oQueryTranslator = new QueryTranslatorLpDaac();
		this.m_oResponseTranslator = new ResponseTranslatorLpDaac();
		this.m_asSupportedPlatforms.add(Platforms.TERRA);
	}
	
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {
		if (sProduct.toUpperCase().startsWith("MOD11A2")) {
			return sOriginalUrl;
		}
		return null;
	}

	@Override
	public int executeCount(String sQuery) {
		WasdiLog.debugLog("QueryExecutorLpDaac.executeCount. Query: " + sQuery);
		
		int lCount = -1;
		
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);

		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			WasdiLog.debugLog("QueryExecutorLpDaac.executeCount. Unsupported platform: " + oQueryViewModel.platformName);
			return lCount;
		}
		
		Double dWest = oQueryViewModel.west;
		Double dNorth = oQueryViewModel.north;
		Double dEast = oQueryViewModel.east;
		Double dSouth = oQueryViewModel.south;

		String sDateFrom = oQueryViewModel.startFromDate;
		String sDateTo = oQueryViewModel.endToDate;

//		Long lDateFrom = TimeEpochUtils.fromDateStringToEpoch(sDateFrom);
//		Long lDateTo = TimeEpochUtils.fromDateStringToEpoch(sDateTo);
		
		String sFileName = oQueryViewModel.productName; // TODO
		String sProductType = oQueryViewModel.productType;


		/*
		ModisRepository oModisRepositroy = new ModisRepository();
		
		long lCount = oModisRepositroy.countItems(dWest, dNorth, dEast, dSouth, lDateFrom, lDateTo, sFileName);
		*/
		
		// TODO: check where the name of the collection is coming from
		
		String sEarthDataCollectionId = null;
		
		if (sProductType.equals("MOD11A2")) {
			sEarthDataCollectionId = "C2269056084-LPCLOUD";
		}
		
		if (Utils.isNullOrEmpty(sEarthDataCollectionId)) {
			WasdiLog.warnLog("QueryExecutorLpDaac.executeCount. Product trype " + sProductType + " not supported by WASDI" );
			return lCount;
		}
		
		// PREPARE QUERY FOR EARTHDATA (HERE WE WILL GET THE RESPONSE IN XML FORMAT SINCE WE ARE ONLY INTERESTED TO GET THE NUMBER OF HITS)
		String sUrlCount = s_sEARTH_DATA_BASE_URL;
		
		// add concept id
		sUrlCount += "?collection_concept_id=" + sEarthDataCollectionId;
		
		// add temporal filter
		DateTimeFormatter oFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
		
		ZonedDateTime oDateTimeInstant = ZonedDateTime.parse(sDateFrom);
		String sEarthDataStartTime = oFormatter.format(oDateTimeInstant);
		
		oDateTimeInstant = ZonedDateTime.parse(sDateTo);
		String sEarthDataEndTime = oFormatter.format(oDateTimeInstant);
		
		sUrlCount += "&temporal=" + sEarthDataStartTime + "," + sEarthDataEndTime;
		
		// add spatial filter
		if (dNorth != null && dSouth != null && dWest != null && dEast != null) {
			sUrlCount += "&bounding_box=" + dWest + "," + dSouth + "," + dEast + "," + dNorth;
		} else {
			WasdiLog.debugLog("QueryExecutorLpDaac.executeCount. one or more points of the WASDI bounding box are null. Spatial filter will be omitted");
		}
		
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

//		Long lDateFrom = TimeEpochUtils.fromDateStringToEpoch(sDateFrom);
//		Long lDateTo = TimeEpochUtils.fromDateStringToEpoch(sDateTo);
		
		String sProductType = oQueryViewModel.productType;
		String sFileName = oQueryViewModel.productName; // TODO
		
		String sEarthDataCollectionId = null;
		
		if (sProductType.equals("MOD11A2")) {
			sEarthDataCollectionId = "C2269056084-LPCLOUD";
		}
		
		if (Utils.isNullOrEmpty(sEarthDataCollectionId)) {
			WasdiLog.warnLog("QueryExecutorLpDaac.executeAndRetrieve. Product trype " + sProductType + " not supported by WASDI" );
			return null;
		}
		
		// PREPARE QUERY FOR EARTHDATA (HERE WE WILL GET THE RESPONSE IN JSON FORMAT SINCE WE ARE ONLY INTERESTED TO GET THE NUMBER OF HITS)
		String sSearchUrl = s_sEARTH_DATA_BASE_URL;
		
		sSearchUrl += ".json";	
		
		// add concept id
		sSearchUrl += "?collection_concept_id=" + sEarthDataCollectionId;
		
		// add temporal filter
		DateTimeFormatter oFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
		
		ZonedDateTime oDateTimeInstant = ZonedDateTime.parse(sDateFrom);
		String sEarthDataStartTime = oFormatter.format(oDateTimeInstant);
		
		oDateTimeInstant = ZonedDateTime.parse(sDateTo);
		String sEarthDataEndTime = oFormatter.format(oDateTimeInstant);
		
		sSearchUrl += "&temporal=" + sEarthDataStartTime + "," + sEarthDataEndTime;
		
		// add spatial filter
		if (dNorth != null && dSouth != null && dWest != null && dEast != null) {
			sSearchUrl += "&bounding_box=" + dWest + "," + dSouth + "," + dEast + "," + dNorth;
		} else {
			WasdiLog.debugLog("QueryExecutorLpDaac.executeAndRetrieve. one or more points of the WASDI bounding box are null. Spatial filter will be omitted");
		}
		
		// add pagination
		sSearchUrl += "&page_size=" + iLimit + "&page_num=" + (iOffset + 1);
		
		HttpCallResponse oHttpResponse = HttpUtils.httpGet(sSearchUrl);
		
		int iResponseCode = oHttpResponse.getResponseCode();
		
		if (iResponseCode >= 200 && iResponseCode <= 299) {
			String sResponseBody = oHttpResponse.getResponseBody();
			System.out.println(sResponseBody);
			List<QueryResultViewModel> oResultList = this.m_oResponseTranslator.translateBatch(sResponseBody, bFullViewModel);
			return oResultList;
			
		} else {
			WasdiLog.warnLog("QueryExecutorLpDaac.executeAndRetrieve. Error contacting the data privider " + iResponseCode);
		}
		
		
//		ModisRepository oModisRepositroy = new ModisRepository();
//
//		List<ModisItemForReading> aoItemList = oModisRepositroy.getModisItemList(dWest, dNorth, dEast, dSouth, lDateFrom, lDateTo, iOffset, iLimit, sFileName);
//
//		aoResults = aoItemList.stream()
//				.map((ModisItemForReading t) -> ((ResponseTranslatorLpDaac) this.m_oResponseTranslator).translate(t))
//				.collect(Collectors.toList());

		return aoResults;
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
	
	public static void main(String[]args) throws Exception {
		WasdiConfig.readConfig("C:/temp/wasdi/wasdiLocalTESTConfig.json");
		QueryExecutorLpDaac oQe = new QueryExecutorLpDaac();
		String sWasdiQuery = "( footprint:\"intersects(POLYGON((-9.836481781581519 25.92643140728654,-9.836481781581519 34.56809749857311,0.8819874344851143 34.56809749857311,0.8819874344851143 25.92643140728654,-9.836481781581519 25.92643140728654)))\" ) AND ( beginPosition:[2020-02-01T00:00:00.000Z TO 2020-12-31T23:59:59.999Z] AND endPosition:[2020-02-01T00:00:00.000Z TO 2020-12-31T23:59:59.999Z] ) AND   (platformname:TERRA AND producttype:MOD11A2)";
		PaginatedQuery oQuery = new PaginatedQuery(sWasdiQuery, "0", "10", null, null);
		List<QueryResultViewModel> oList = oQe.executeAndRetrieve(oQuery);
		System.out.println(oList.get(0).getTitle());
		System.out.println(oList.get(0).getLink());
		System.out.println(oList.get(0).getFootprint());
		System.out.println(oList.get(0).getSummary());
		System.out.println(oList.get(0).getId());
		
	}

}
