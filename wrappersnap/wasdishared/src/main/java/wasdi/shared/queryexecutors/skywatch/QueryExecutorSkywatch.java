package wasdi.shared.queryexecutors.skywatch;

import static wasdi.shared.queryexecutors.skywatch.SkywatchHttpUtils.makeAttemptsToPerformSearchOperation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.queryexecutors.skywatch.models.SearchRequest;
import wasdi.shared.queryexecutors.skywatch.models.SearchResultsResponse;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * Skywatch Query Executor.
 * This class queries the Skywatch/Earthcache platform: https://api.skywatch.co/earthcache
 * 
 * @author PetruPetrescu
 *
 */
public class QueryExecutorSkywatch extends QueryExecutor {

	public QueryExecutorSkywatch() {
		this.m_oQueryTranslator = new QueryTranslatorSkywatch();
		this.m_oResponseTranslator = new ResponseTranslatorSkywatch();
	}

	/**
	 * Overload of the get URI from Product Name method.
	 * For Skywatch, we need just the original link..
	 */
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {
		if (sProduct.startsWith("SKYWATCH_")) {
			return sOriginalUrl;
		}
		return null;
	}

	@Override
	public int executeCount(String sQuery) {
		int iCount = -1;

		try {
			SearchRequest oSearchRequest = convertQueryToSearchRequest(sQuery);

			if (oSearchRequest == null) {
				return iCount;
			}

			SearchResultsResponse oSearchResultsResponse = makeAttemptsToPerformSearchOperation(oSearchRequest, 20);

			if (oSearchResultsResponse != null) {
				SearchResultsResponse.Pagination oPagination = oSearchResultsResponse.getPagination();

				if (oPagination != null) {
					iCount = oPagination.getTotal();
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorSkywatch.executeCount: error " + oEx.toString());
		}
		
		return iCount;
	}

	private SearchRequest convertQueryToSearchRequest(String sQuery) {
		try {
			// Parse the query
			QueryViewModel oSkywatchQuery = m_oQueryTranslator.parseWasdiClientQuery(sQuery);

			if (!m_asSupportedPlatforms.contains(oSkywatchQuery.platformName)) {
				return null;
			}

			Date oStartDay = Utils.getYyyyMMddTZDate(oSkywatchQuery.startFromDate);
			Date oEndDay = Utils.getYyyyMMddTZDate(oSkywatchQuery.endToDate);

			String sStartDay = Utils.formatToYyyyDashMMDashdd(oStartDay);
			String sEndDay = Utils.formatToYyyyDashMMDashdd(oEndDay);

			String sResolution = oSkywatchQuery.productType;
			Double dCoverage = oSkywatchQuery.cloudCoverageFrom;


			SearchRequest searchRequest = new SearchRequest();

			List<List<List<Double>>> coordinates = Arrays.asList(Arrays.asList(
					Arrays.asList(oSkywatchQuery.west, oSkywatchQuery.south),
					Arrays.asList(oSkywatchQuery.east, oSkywatchQuery.south),
					Arrays.asList(oSkywatchQuery.east, oSkywatchQuery.north),
					Arrays.asList(oSkywatchQuery.west, oSkywatchQuery.north),
					Arrays.asList(oSkywatchQuery.west, oSkywatchQuery.south)
					));


			SearchRequest.Location location = new SearchRequest.Location();
			location.setType("Polygon");
			location.setCoordinates(coordinates);

			searchRequest.setLocation(location);
			searchRequest.setOrder_by(Arrays.asList("-date", "-coverage", "cost"));
			searchRequest.setStart_date(sStartDay);
			searchRequest.setEnd_date(sEndDay);
			searchRequest.setCoverage(dCoverage);
			searchRequest.setResolution(Arrays.asList(sResolution));

			return searchRequest;
		} catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorSkywatch.prepareRequestPayload: error " + oEx.toString());
		}
		return null;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		try {
			List<QueryResultViewModel> aoResults = new ArrayList<>();

			SearchRequest oSearchRequest = convertQueryToSearchRequest(oQuery.getQuery());

			if (oSearchRequest == null) {
				return aoResults;
			}

			String sOffset = oQuery.getOffset();
			String sLimit = oQuery.getLimit();

			SearchResultsResponse oSearchResultsResponse = makeAttemptsToPerformSearchOperation(oSearchRequest, sOffset, sLimit, 20);

			aoResults = convert(oSearchResultsResponse);

			return aoResults;			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorSkywatch.executeAndRetrieve: error " + oEx.toString());
		}
		
		return null;
	}

	private static List<QueryResultViewModel> convert(SearchResultsResponse oSearchResultsResponse) {
		if (oSearchResultsResponse == null) {
			return Collections.emptyList();
		}

		if (oSearchResultsResponse.getData() == null) {
			if (oSearchResultsResponse.getErrors() != null && !oSearchResultsResponse.getErrors().isEmpty()) {
				return oSearchResultsResponse.getErrors().stream()
					.map((SearchResultsResponse.Error t) -> {
						QueryResultViewModel oResult = new QueryResultViewModel();

						oResult.setId("error");
						oResult.getProperties().put("error", t.getMessage());

						return oResult;
					})
					.collect(Collectors.toList());
			} else {
				return Collections.emptyList();
			}
		}

		List<QueryResultViewModel> aoResults = oSearchResultsResponse.getData().stream()
				.map(QueryExecutorSkywatch::convert)
				.collect(Collectors.toList());

		return aoResults;
	}

	private static QueryResultViewModel convert(SearchResultsResponse.Datum oDatum) {
		QueryResultViewModel oResult = new QueryResultViewModel();

		oResult.setProvider("SKYWATCH");

		parseMainInfo(oDatum, oResult);
		parseFootPrint(oDatum.getLocation(), oResult);
		parseProperties(oDatum, oResult);

		buildLink(oResult);
		buildSummary(oResult);
		
		return oResult;
	}

	private static void parseMainInfo(SearchResultsResponse.Datum oItem, QueryResultViewModel oResult) {
		String sId = "SKYWATCH_";

		if (!Utils.isNullOrEmpty(oItem.getPreview_uri())) {
			sId += extractProductName(oItem.getPreview_uri());
		} else if (Utils.isNullOrEmpty(oItem.getThumbnail_uri())) {
			sId += extractProductName(oItem.getThumbnail_uri());
		} else {
			sId += oItem.getId();
		}

		oResult.setId(sId);
		oResult.setTitle(sId);
	}

	// https://preview.skywatch.com/esa/sentinel-2/S2B_OPER_MSI_L1C_TL_2BPS_20221122T105727_A029835_T32TMP_N04_00.jp2
	// https://s3-us-west-2.amazonaws.com/aoi-processed-images-prod/93e9b404-7191-11ed-b9d1-461602ea8867/e513ec6f-2f7f-471e-8bbd-7fcecf7fa184/preview/SKYWATCH_S2_MS_20221127T1028_ALL_Tile_0_0_602e_preview.png
	private static String extractProductName(String sUrl) {
		String sProductName = sUrl.substring(sUrl.lastIndexOf("/") + 1, sUrl.lastIndexOf("."))
				.replace("_preview", "")
				.replace("_100x100", "");

		return sProductName;
	}

	private static void parseFootPrint(SearchResultsResponse.Location sLocation, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(sLocation, "QueryExecutorSkywatch.parseFootPrint: input sLocation is null");
		Preconditions.checkNotNull(oResult, "QueryExecutorSkywatch.parseFootPrint: QueryResultViewModel is null");

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

	private static void parseProperties(SearchResultsResponse.Datum oItem, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oItem, "QueryExecutorSkywatch.addProperties: input oItem is null");
		Preconditions.checkNotNull(oResult, "QueryExecutorSkywatch.addProperties: QueryResultViewModel is null");

		oResult.getProperties().put("title", oItem.getId());

		oResult.getProperties().put("platform", "Earthcache");
		oResult.getProperties().put("platformname", "Earthcache");

		String sThumbnail = oItem.getThumbnail_uri();
		if (sThumbnail == null) {
			oResult.setPreview(null);
		} else if (sThumbnail.toLowerCase().endsWith(".jp2")) {
			oResult.setPreview(null);
		} else {
			oResult.setPreview(sThumbnail);
		}

		if (oItem.getStart_time() != null) {
			String sStartDate = TimeEpochUtils.fromEpochToDateString(oItem.getStart_time().getTime());
			oResult.getProperties().put("date", sStartDate);
			oResult.getProperties().put("startDate", sStartDate);
			oResult.getProperties().put("beginposition", sStartDate);
		}

		if (oItem.getSource() != null)
			oResult.getProperties().put(("source"), oItem.getSource());

		if (oItem.getResolution() != null)
			oResult.getProperties().put(("resolution"), Double.toString(oItem.getResolution()));

		if (oItem.getLocation_coverage_percentage() != null)
			oResult.getProperties().put(("location_coverage_percentage"), Double.toString(oItem.getLocation_coverage_percentage()));

		if (oItem.getArea_sq_km() != null)
			oResult.getProperties().put(("area_sq_km"), Double.toString(oItem.getArea_sq_km()));

		if (oItem.getCost() != null)
			oResult.getProperties().put(("cost"), Double.toString(oItem.getCost()));

		if (oItem.getResult_cloud_cover_percentage() != null)
			oResult.getProperties().put(("result_cloud_cover_percentage"), Double.toString(oItem.getResult_cloud_cover_percentage()));

		if (oItem.getAvailable_credit() != null)
			oResult.getProperties().put(("available_credit"), Double.toString(oItem.getAvailable_credit()));

		oResult.getProperties().put("source", oItem.getSource());

		parseLinks(oItem, oResult);
	}

	private static void parseLinks(SearchResultsResponse.Datum oItem, QueryResultViewModel oResult) {
		oResult.getProperties().put("href", oItem.getPreview_uri());
		oResult.getProperties().put("url", "http?product_name=" + oResult.getId() + "&search_id=" + oItem.getSearchId() + "&product_id=" + oItem.getId());
	}

	private static void buildLink(QueryResultViewModel oResult) {
		String sUrl = oResult.getProperties().get("url");

		oResult.getProperties().put("link", sUrl);
		oResult.setLink(sUrl);
	}

	protected static void buildSummary(QueryResultViewModel oResult) {
		String sDate = oResult.getProperties().get("date");
		String sSummary = "Date: " + sDate + ", ";

		String sSource = oResult.getProperties().get("source");
		sSummary = sSummary + "Source: " + sSource + ", ";

		String sResolution = oResult.getProperties().get("resolution");
		sSummary = sSummary + "Resolution: " + sResolution + ", ";

		String sSatellite = oResult.getProperties().get("platform");
		sSummary = sSummary + "Platform: " + sSatellite + ", ";

//		String sSize = oResult.getProperties().get("size");
//		sSummary = sSummary + "Size: " + sSize;

		oResult.setSummary(sSummary);
	}

}
