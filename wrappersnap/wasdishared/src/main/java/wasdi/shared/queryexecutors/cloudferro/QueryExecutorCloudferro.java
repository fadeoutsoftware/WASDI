package wasdi.shared.queryexecutors.cloudferro;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

import wasdi.shared.business.S3Volume;
import wasdi.shared.business.ecostress.EcoStressItemForReading;
import wasdi.shared.data.S3VolumeRepository;
import wasdi.shared.data.ecostress.EcoStressRepository;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * Query Executor for Cloudferro Data Center.
 * 
 * @author PetruPetrescu
 *
 */
public class QueryExecutorCloudferro extends QueryExecutor {

	boolean m_bAuthenticated = false;

	public QueryExecutorCloudferro() {
		this.m_oQueryTranslator = new QueryTranslatorCloudferro();
		this.m_oResponseTranslator = new ResponseTranslatorCloudferro();
	}

	/**
	 * Overload of the get URI from Product Name method.
	 * For Cloudferro, we need just the original link..
	 */
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl, String sPlatform) {
		if (sProduct.toUpperCase().startsWith("EEH2TES")
				|| sProduct.toUpperCase().startsWith("EEH2STIC")
				|| sProduct.toUpperCase().startsWith("ECOV002_L2_CLOUD")
				|| sProduct.toUpperCase().startsWith("ECOV002_L1B_GEO")
				|| sProduct.toUpperCase().startsWith("ECOV002_L1B_RAD")){
			return sOriginalUrl;
		}
		return null;
	}
	
	/**
	 * Executes the count 
	 */
	@Override
	public int executeCount(String sQuery) {
		WasdiLog.debugLog("QueryExecutorCloudferro.executeCount | sQuery: " + sQuery);
		
		// Parse the query
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);

		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			return -1;
		}
		
		String sDataset = oQueryViewModel.productType;
		String sDayNightFlag = oQueryViewModel.timeliness;
		
	
		EcoStressRepository oEcoStressRepository = new EcoStressRepository();

		Double dWest = oQueryViewModel.west;
		Double dNorth = oQueryViewModel.north;
		Double dEast = oQueryViewModel.east;
		Double dSouth = oQueryViewModel.south;

		String sDateFrom = oQueryViewModel.startFromDate;
		String sDateTo = oQueryViewModel.endToDate;

		Long lDateFrom = TimeEpochUtils.fromDateStringToEpoch(sDateFrom);
		Long lDateTo = TimeEpochUtils.fromDateStringToEpoch(sDateTo);
		
		if (sDataset.equals("EEHGPP")) {
			sDataset = "EEHGPP-final";
		}
		
		long lCount = 0;
		
		if (!Utils.isNullOrEmpty(oQueryViewModel.productName)) {
			List<EcoStressItemForReading> aoItems = oEcoStressRepository.getEcoStressItemByName(oQueryViewModel.productName, dWest, dNorth, dEast, dSouth, sDataset, lDateFrom, lDateTo, sDayNightFlag);
			
			lCount = aoItems != null ? aoItems.size() : -1;
		}
		else {
			lCount = oEcoStressRepository.countItems(dWest,dNorth, dEast, dSouth, sDataset, lDateFrom, lDateTo, sDayNightFlag);
		}

		return (int) lCount;
	}

	/**
	 * Execute an Cloudferro Data Center Query and return the result in WASDI format
	 */
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		WasdiLog.debugLog("QueryExecutorCloudferro.executeAndRetrieve | sQuery: " + oQuery.getQuery());

		String sOffset = oQuery.getOffset();
		String sLimit = oQuery.getLimit();

		int iOffset = 0;
		int iLimit = 10;

		try {
			iOffset = Integer.parseInt(sOffset);
		} catch (Exception oE) {
			WasdiLog.errorLog("QueryExecutorCloudferro.executeAndRetrieve: " + oE.toString());
		}

		try {
			iLimit = Integer.parseInt(sLimit);
		} catch (Exception oE) {
			WasdiLog.errorLog("QueryExecutorCloudferro.executeAndRetrieve: " + oE.toString());
		}

		List<QueryResultViewModel> aoResults = null;

		// Parse the query
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());

		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			return aoResults;
		}
		
		String sDataset = oQueryViewModel.productType;
		String sDayNightFlag = oQueryViewModel.timeliness;

		EcoStressRepository oEcoStressRepository = new EcoStressRepository();

		Double dWest = oQueryViewModel.west;
		Double dNorth = oQueryViewModel.north;
		Double dEast = oQueryViewModel.east;
		Double dSouth = oQueryViewModel.south;


		String sDateFrom = oQueryViewModel.startFromDate;
		String sDateTo = oQueryViewModel.endToDate;

		Long lDateFrom = TimeEpochUtils.fromDateStringToEpoch(sDateFrom);
		Long lDateTo = TimeEpochUtils.fromDateStringToEpoch(sDateTo);
		
		JSONObject oParseConf = JsonUtils.loadJsonFromFile(m_sParserConfigPath);
		
		if (oParseConf!=null) {
			if(!oParseConf.has("volumeId")) {
				String sVolumeId = oParseConf.optString("volumeId");
				S3VolumeRepository oS3VolumeRepository = new S3VolumeRepository();
				S3Volume oS3Volume = oS3VolumeRepository.getVolume(sVolumeId);
				
				if (oS3Volume!=null) {
					((ResponseTranslatorCloudferro) this.m_oResponseTranslator).setVolumeName(oS3Volume.getMountingFolderName());
				}
			}
		}
		
		List<EcoStressItemForReading> aoItemList = null;

		if (!Utils.isNullOrEmpty(oQueryViewModel.productName))
			aoItemList = oEcoStressRepository.getEcoStressItemByName(oQueryViewModel.productName, dWest, dNorth, dEast, dSouth, sDataset, lDateFrom, lDateTo, sDayNightFlag);
		else
			aoItemList = oEcoStressRepository
				.getEcoStressItemList(dWest, dNorth, dEast, dSouth, sDataset, lDateFrom, lDateTo, sDayNightFlag, iOffset, iLimit);
		
		if (aoItemList != null) {
		aoResults = aoItemList.stream()
				.map((EcoStressItemForReading t) -> ((ResponseTranslatorCloudferro) this.m_oResponseTranslator).translate(t))
				.collect(Collectors.toList());
		}
		
		return aoResults;
	}

}
