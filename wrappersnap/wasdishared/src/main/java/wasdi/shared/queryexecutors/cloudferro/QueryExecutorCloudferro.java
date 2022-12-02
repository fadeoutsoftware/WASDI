package wasdi.shared.queryexecutors.cloudferro;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import wasdi.shared.business.ecostress.EcoStressItemForReading;
import wasdi.shared.data.ecostress.EcoStressRepository;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.TimeEpochUtils;
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
		m_sProvider = "CLOUDFERRO";

		this.m_oQueryTranslator = new QueryTranslatorCloudferro();
		this.m_oResponseTranslator = new ResponseTranslatorCloudferro();

		m_asSupportedPlatforms.add(Platforms.ECOSTRESS);
	}

	/**
	 * Overload of the get URI from Product Name method.
	 * For Cloudferro, we need just the original link..
	 */
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {
		if (sProduct.toUpperCase().startsWith("EEHCM")
				|| sProduct.toUpperCase().startsWith("EEHSEBS")
				|| sProduct.toUpperCase().startsWith("EEHSTIC")
				|| sProduct.toUpperCase().startsWith("EEHSW")
				|| sProduct.toUpperCase().startsWith("EEHTES")
				|| sProduct.toUpperCase().startsWith("EEHTSEB")) {
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

		String sProtocol = oQueryViewModel.productLevel;
		String sService = oQueryViewModel.productType;
		String sProduct = oQueryViewModel.productName;
		int iRelativeOrbit = oQueryViewModel.relativeOrbit;
		String sDayNightFlag = oQueryViewModel.timeliness;

		System.out.println("sProtocol: " + sProtocol);
		System.out.println("sService: " + sService);
		System.out.println("sProduct: " + sProduct);
		System.out.println("iRelativeOrbit: " + iRelativeOrbit);
		System.out.println("sDayNightFlag: " + sDayNightFlag);

		EcoStressRepository oEcoStressRepository = new EcoStressRepository();

		Double dWest = oQueryViewModel.west;
		Double dNorth = oQueryViewModel.north;
		Double dEast = oQueryViewModel.east;
		Double dSouth = oQueryViewModel.south;


		String sDateFrom = oQueryViewModel.startFromDate;
		String sDateTo = oQueryViewModel.endToDate;

		System.out.println("sDateFrom: " + sDateFrom);
		System.out.println("sDateTo: " + sDateTo);

		Long lDateFrom = TimeEpochUtils.fromDateStringToEpoch(sDateFrom);
		Long lDateTo = TimeEpochUtils.fromDateStringToEpoch(sDateTo);

		System.out.println("lDateFrom: " + lDateFrom.toString());
		System.out.println("lDateTo: " + lDateTo.toString());

		long lCount = oEcoStressRepository.countItems(dWest,dNorth, dEast, dSouth, sService, lDateFrom, lDateTo, iRelativeOrbit, sDayNightFlag);

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
			WasdiLog.debugLog("QueryExecutorCloudferro.executeAndRetrieve: " + oE.toString());
		}

		try {
			iLimit = Integer.parseInt(sLimit);
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryExecutorCloudferro.executeAndRetrieve: " + oE.toString());
		}

		List<QueryResultViewModel> aoResults = new ArrayList<>();

		// Parse the query
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());

		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			return aoResults;
		}

		String sProtocol = oQueryViewModel.productLevel;
		String sService = oQueryViewModel.productType;
		String sProduct = oQueryViewModel.productName;
		int iRelativeOrbit = oQueryViewModel.relativeOrbit;
		String sDayNightFlag = oQueryViewModel.timeliness;

		System.out.println("sProtocol: " + sProtocol);
		System.out.println("sService: " + sService);
		System.out.println("sProduct: " + sProduct);
		System.out.println("iRelativeOrbit: " + iRelativeOrbit);
		System.out.println("sDayNightFlag: " + sDayNightFlag);

		EcoStressRepository oEcoStressRepository = new EcoStressRepository();

		Double dWest = oQueryViewModel.west;
		Double dNorth = oQueryViewModel.north;
		Double dEast = oQueryViewModel.east;
		Double dSouth = oQueryViewModel.south;


		String sDateFrom = oQueryViewModel.startFromDate;
		String sDateTo = oQueryViewModel.endToDate;

		System.out.println("sDateFrom: " + sDateFrom);
		System.out.println("sDateTo: " + sDateTo);

		Long lDateFrom = TimeEpochUtils.fromDateStringToEpoch(sDateFrom);
		Long lDateTo = TimeEpochUtils.fromDateStringToEpoch(sDateTo);

		System.out.println("lDateFrom: " + lDateFrom.toString());
		System.out.println("lDateTo: " + lDateTo.toString());

		List<EcoStressItemForReading> aoItemList = oEcoStressRepository
				.getEcoStressItemList(dWest, dNorth, dEast, dSouth, sService, lDateFrom, lDateTo, iRelativeOrbit, sDayNightFlag, iOffset, iLimit);

		aoResults = aoItemList.stream()
				.map((EcoStressItemForReading t) -> ((ResponseTranslatorCloudferro) this.m_oResponseTranslator).translate(t))
				.collect(Collectors.toList());

		return aoResults;
	}

}
