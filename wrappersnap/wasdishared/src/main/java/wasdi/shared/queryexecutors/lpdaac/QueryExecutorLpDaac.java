package wasdi.shared.queryexecutors.lpdaac;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.business.modis11a2.ModisItemForReading;
import wasdi.shared.config.MongoConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.modis11a2.ModisRepository;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;


public class QueryExecutorLpDaac extends QueryExecutor {
	
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
		WasdiLog.debugLog("QueryExecutorModis.executeCount. Query: " + sQuery);
		
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);

		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			return -1;
		}
		
		Double dWest = oQueryViewModel.west;
		Double dNorth = oQueryViewModel.north;
		Double dEast = oQueryViewModel.east;
		Double dSouth = oQueryViewModel.south;

		String sDateFrom = oQueryViewModel.startFromDate;
		String sDateTo = oQueryViewModel.endToDate;

		Long lDateFrom = TimeEpochUtils.fromDateStringToEpoch(sDateFrom);
		Long lDateTo = TimeEpochUtils.fromDateStringToEpoch(sDateTo);
		
		String sFileName = oQueryViewModel.productName;

		ModisRepository oModisRepositroy = new ModisRepository();
		
		long lCount = oModisRepositroy.countItems(dWest, dNorth, dEast, dSouth, lDateFrom, lDateTo, sFileName);
		
		WasdiLog.debugLog("QueryExecutorModis.executeCount. Retrieved number of results: " + lCount);

		return (int) lCount;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		WasdiLog.debugLog("QueryExecutorModis.executeAndRetrieve. Query: " + oQuery.getQuery());

		String sOffset = oQuery.getOffset();
		String sLimit = oQuery.getLimit();

		int iOffset = 0;
		int iLimit = 10;

		try {
			iOffset = Integer.parseInt(sOffset);
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryExecutorModis.executeAndRetrieve. Impossible to parse offset " + sOffset + ". "+ oE.toString());
		}

		try {
			iLimit = Integer.parseInt(sLimit);
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryExecutorModis.executeAndRetrieve. Impossible to parse limit " + sLimit + ". "+ oE.toString());
		}

		List<QueryResultViewModel> aoResults = new ArrayList<>();

		// Parse the query
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());

		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			return aoResults;
		}

		Double dWest = oQueryViewModel.west;
		Double dNorth = oQueryViewModel.north;
		Double dEast = oQueryViewModel.east;
		Double dSouth = oQueryViewModel.south;

		String sDateFrom = oQueryViewModel.startFromDate;
		String sDateTo = oQueryViewModel.endToDate;

		Long lDateFrom = TimeEpochUtils.fromDateStringToEpoch(sDateFrom);
		Long lDateTo = TimeEpochUtils.fromDateStringToEpoch(sDateTo);
		
		String sFileName = oQueryViewModel.productName;
		
		ModisRepository oModisRepositroy = new ModisRepository();

		List<ModisItemForReading> aoItemList = oModisRepositroy.getModisItemList(dWest, dNorth, dEast, dSouth, lDateFrom, lDateTo, iOffset, iLimit, sFileName);

		aoResults = aoItemList.stream()
				.map((ModisItemForReading t) -> ((ResponseTranslatorLpDaac) this.m_oResponseTranslator).translate(t))
				.collect(Collectors.toList());

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

}
