package wasdi.shared.queryexecutors.cloudferro;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.business.ecostress.EcoStressItemForReading;
import wasdi.shared.business.ecostress.EcoStressLocation;
import wasdi.shared.data.ecostress.EcoStressRepository;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * Query Executor for Cloudferro Data Center.
 * 
 * @author PetruPetrescu
 *
 */
public class QueryExecutorCloudferro extends QueryExecutor {

	private static final String STYPE = "type";
	private static final String SSIZE = "size";
	private static final String SPLATFORM = "platform";
	private static final String SSENSOR_MODE = "sensorMode";
	private static final String SINSTRUMENT = "instrument";
	private static final String SDATE = "date";
	private static final String STITLE = "title";
	private static final String SRELATIVEORBITNUMBER = "relativeOrbitNumber";

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
				|| sProduct.toUpperCase().startsWith("EEHSW")) {
			return sOriginalUrl;
		}
		return null;
	}

	/**
	 * Executes the count 
	 */
	@Override
	public int executeCount(String sQuery) {
		Utils.debugLog("QueryExecutorCloudferro.executeCount | sQuery: " + sQuery);

		int iCount = 0;
		
		// Parse the query
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);

		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			return -1;
		}

		String sProtocol = oQueryViewModel.productLevel;
		String sService = oQueryViewModel.productType;
		String sProduct = oQueryViewModel.productName;

		System.out.println("sProtocol: " + sProtocol);
		System.out.println("sService: " + sService);
		System.out.println("sProduct: " + sProduct);

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

		long lCount = oEcoStressRepository.countItems(dWest,dNorth, dEast, dSouth, sService, lDateFrom, lDateTo);

		return (int) lCount;
	}

	/**
	 * Execute an Cloudferro Data Center Query and return the result in WASDI format
	 */
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		Utils.debugLog("QueryExecutorCloudferro.executeAndRetrieve | sQuery: " + oQuery.getQuery());



		String sOffset = oQuery.getOffset();
		String sLimit = oQuery.getLimit();

		int iOffset = 0;
		int iLimit = 10;

		try {
			iOffset = Integer.parseInt(sOffset);
		} catch (Exception oE) {
			Utils.debugLog("QueryExecutorCloudferro.executeAndRetrieve: " + oE.toString());
		}

		try {
			iLimit = Integer.parseInt(sLimit);
		} catch (Exception oE) {
			Utils.debugLog("QueryExecutorCloudferro.executeAndRetrieve: " + oE.toString());
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

		System.out.println("sProtocol: " + sProtocol);
		System.out.println("sService: " + sService);
		System.out.println("sProduct: " + sProduct);

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
				.getEcoStressItemList(dWest, dNorth, dEast, dSouth, sService, lDateFrom, lDateTo, iOffset, iLimit);

		aoResults = aoItemList.stream()
			.map(QueryExecutorCloudferro::translate)
			.collect(Collectors.toList());

		return aoResults;
	}

	private static QueryResultViewModel translate(EcoStressItemForReading oItem) {
		QueryResultViewModel oResult = new QueryResultViewModel();
		oResult.setProvider("CLOUDFERRO");

		oResult.setId(oItem.getFileName());
		oResult.setTitle(oItem.getFileName());

		parseProperties(oItem, oResult);
		parseFootPrint(oItem.getLocation(), oResult);

		oResult.setPreview(null);
//		protected String summary;

		String sLink = oItem.getUrl() + "?fileName=" + oItem.getFileName() + "&filePath=" + oItem.getS3Path();
		oResult.setLink(sLink);

		return oResult;
	}

	private static void parseFootPrint(EcoStressLocation sLocation, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(sLocation, "QueryExecutorCloudferro.parseFootPrint: input sLocation is null");
		Preconditions.checkNotNull(oResult, "QueryExecutorCloudferro.parseFootPrint: QueryResultViewModel is null");

		// "POLYGON((7.734924852848054 42.06684525433608,7.734924852848054 44.2482410267701,11.206604540348055 44.2482410267701,11.206604540348055 42.06684525433608,7.734924852848054 42.06684525433608))"

		if (sLocation.getType().equalsIgnoreCase("Polygon")) {
			StringBuilder oFootPrint = new StringBuilder("POLYGON");
			oFootPrint.append(" ((");

			List<List<List<Double>>> aoCoordinates = sLocation.getCoordinates();

			if (aoCoordinates != null && !aoCoordinates.isEmpty()) {
				List<List<Double>> aoPolygon = aoCoordinates.get(0);

				if (aoPolygon != null && !aoPolygon.isEmpty()) {
					boolean bIsFirstPoint = true;

					for (List<Double> aoPoint : aoPolygon) {
						if (bIsFirstPoint) {
							bIsFirstPoint = false;
						} else {
							oFootPrint.append(",");
						}

						if (aoPoint != null && aoPoint.size() == 2) {
							oFootPrint.append(aoPoint.get(0)).append(" ").append(aoPoint.get(1));
						}
					}

				}
			}

			oFootPrint.append("))");

			String sFootPrint = oFootPrint.toString();
			oResult.setFootprint(sFootPrint);
		}
	}

	private static void parseFootPrint(String sLocation, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(sLocation, "QueryExecutorCloudferro.parseFootPrint: input sLocation is null");
		Preconditions.checkNotNull(oResult, "QueryExecutorCloudferro.parseFootPrint: QueryResultViewModel is null");

		try {
			String sBuffer = null;
			JSONObject oGeometry = new JSONObject(sLocation);
			if (null != oGeometry) {
				sBuffer = oGeometry.optString(STYPE, null);
				if (null != sBuffer) {
					StringBuilder oFootPrint = new StringBuilder(sBuffer.toUpperCase());

					if (sBuffer.equalsIgnoreCase("POLYGON")) {
						oFootPrint.append(" ((");
					} else if (sBuffer.equalsIgnoreCase("MULTIPOLYGON")) {
						oFootPrint.append(" (((");
					}

					parseCoordinates(oGeometry, oFootPrint);

					//remove ending spaces and commas in excess
					String sFootPrint = oFootPrint.toString();
					sFootPrint = sFootPrint.trim();
					while(sFootPrint.endsWith(",") ) {			
						sFootPrint = sFootPrint.substring(0,  sFootPrint.length() - 1 );
						sFootPrint = sFootPrint.trim();
					}

					if (sBuffer.equalsIgnoreCase("POLYGON")) {
						sFootPrint += "))";
					}
					else if (sBuffer.equalsIgnoreCase("MULTIPOLYGON")) {
						sFootPrint += ")))";
					}

					oResult.setFootprint(sFootPrint);
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("QueryExecutorCloudferro.parseFootPrint( " + sLocation + ", ... ): " + oE );
		}
	}

	/**
	 * @param oGeometry
	 * @param oFootPrint
	 */
	private static void parseCoordinates(JSONObject oGeometry, StringBuilder oFootPrint) {
		JSONArray aoCoordinates = oGeometry.optJSONArray("coordinates"); //0
		if (null != aoCoordinates) {
			aoCoordinates = aoCoordinates.optJSONArray(0); //1

			if (null != aoCoordinates) {
				String sType = oGeometry.getString("type");

				if (sType.toUpperCase().equals("MULTIPOLYGON")) {
					aoCoordinates = aoCoordinates.optJSONArray(0); //2

					if (null != aoCoordinates) {
						loadCoordinates(oFootPrint, aoCoordinates);
					}					
				} else {
					loadCoordinates(oFootPrint, aoCoordinates);// Execute with the 1-array
				}
			}
		}
	}

	/**
	 * @param oFootPrint
	 * @param aoCoordinates
	 */
	private static void loadCoordinates(StringBuilder oFootPrint, JSONArray aoCoordinates) {
		for (Object oItem: aoCoordinates) {
			//instanceof returns false for null, so no need to check it
			if (oItem instanceof JSONArray) {
				JSONArray aoPoint = (JSONArray) oItem;
				Double dx = aoPoint.optDouble(0);
				Double dy = aoPoint.optDouble(1);
				if (!Double.isNaN(dx) && !Double.isNaN(dy)) {
					oFootPrint.append(dx).append(" ").append(dy).append(", ");
				}
			}
		}
	}

	protected void buildSummary(QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oResult, "QueryExecutorCloudferro.buildSummary: QueryResultViewModel is null");

		String sDate = oResult.getProperties().get(SDATE);
		String sSummary = "Date: " + sDate + ", ";
		String sInstrument = oResult.getProperties().get(SINSTRUMENT);
		sSummary = sSummary + "Instrument: " + sInstrument + ", ";

		String sMode = oResult.getProperties().get(SSENSOR_MODE);
		sSummary = sSummary + "Mode: " + sMode + ", ";
		String sSatellite = oResult.getProperties().get(SPLATFORM);
		sSummary = sSummary + "Satellite: " + sSatellite + ", ";
		String sSize = oResult.getProperties().get(SSIZE);
		sSummary = sSummary + "Size: " + sSize;// + " " + sChosenUnit;
		oResult.setSummary(sSummary);
	}

	private static void parseProperties(EcoStressItemForReading oItem, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oItem, "QueryExecutorCloudferro.addProperties: input oItem is null");
		Preconditions.checkNotNull(oResult, "QueryExecutorCloudferro.addProperties: QueryResultViewModel is null");

		oResult.getProperties().put(STITLE, oItem.getFileName());

		if (oItem.getBeginningDate() != null) {
			String sStartDate = TimeEpochUtils.fromEpochToDateString(oItem.getBeginningDate().longValue());
			oResult.getProperties().put("startDate", sStartDate);
			oResult.getProperties().put("beginposition", sStartDate);
		}

		oResult.getProperties().put((SSENSOR_MODE), oItem.getSensor());
		oResult.getProperties().put("sensoroperationalmode", oItem.getSensor());

		oResult.getProperties().put(SPLATFORM, oItem.getPlatform());
		oResult.getProperties().put("platformname", oItem.getPlatform());

		oResult.getProperties().put(SINSTRUMENT, oItem.getInstrument());
		oResult.getProperties().put("instrumentshortname", oItem.getInstrument());

		oResult.getProperties().put(SRELATIVEORBITNUMBER, Integer.valueOf(oItem.getStartOrbitNumber()).toString());
		oResult.getProperties().put("relativeorbitnumber", Integer.valueOf(oItem.getStartOrbitNumber()).toString());

	}

}
