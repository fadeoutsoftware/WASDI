package wasdi.shared.queryexecutors.gpm;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * GPM Query Executor.
 * This class queries the GPM Catalogue from "https://jsimpsonhttps.pps.eosdis.nasa.gov
 * 
 * The query in this case is local: the catalogue does not have API.
 * GPM are regular (half-hourly, monthly) maps in a full world format.
 * 
 * The query is obtained with a local shape file that reproduces the GPM grid
 * 
 * @author PetruPetrescu
 *
 */
public class QueryExecutorGPM extends QueryExecutor {

	private static final String URL_TEXT_LATE = "https://jsimpsonhttps.pps.eosdis.nasa.gov/text/imerg/gis/";
	private static final String URL_TEXT_EARLY = "https://jsimpsonhttps.pps.eosdis.nasa.gov/text/imerg/gis/early/";
	private static final String URL_LATE = "https://jsimpsonhttps.pps.eosdis.nasa.gov/imerg/gis/";
	private static final String URL_EARLY = "https://jsimpsonhttps.pps.eosdis.nasa.gov/imerg/gis/early/";

	private static final String LATENCY_EARLY = "Early";
	private static final String LATENCY_LATE = "Late";

	private static final String DURATION_HHR = "HHR";
	private static final String DURATION_DAY = "DAY";
	private static final String DURATION_MO = "MO";

	private static final String ACCUMULATION_3DAY = "3day";
	private static final String ACCUMULATION_3DAY_ONLY_FOR_LATE = "3day - only for Late";
	private static final String ACCUMULATION_7DAY = "7day";
	private static final String ACCUMULATION_7DAY_ONLY_FOR_LATE = "7day - only for Late";
	private static final String ACCUMULATION_ALL = "All";

	private static final String EXTENSION_TIF = ".tif";

	public QueryExecutorGPM() {
		this.m_oQueryTranslator = new QueryTranslatorGPM();
		this.m_oResponseTranslator = new ResponseTranslatorGPM();
	}

	/**
	 * Overload of the get URI from Product Name method.
	 * For Terrascope, we need just the original link..
	 */
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl, String sPlatform) {
		
		if (sProduct.toUpperCase().startsWith("3B-") || sProduct.toUpperCase().contains("IMERG")) {
			return sOriginalUrl;
		}
		
		return null;
	}

	@Override
	public int executeCount(String sQuery) {
		WasdiLog.debugLog("QueryExecutorGPM.executeCount | sQuery: " + sQuery);

		int iCount = 0;

		// Parse the query
		QueryViewModel oGPMQuery = m_oQueryTranslator.parseWasdiClientQuery(sQuery);

		if (!m_asSupportedPlatforms.contains(oGPMQuery.platformName)) {
			return iCount;
		}


		final String sQueryStartFromDate = oGPMQuery.startFromDate;
		final String sQueryEndToDate = oGPMQuery.endToDate;

		final Date oStartFromDate;
		final Date oEndToDate;
		
		Date oStartFromDateProvided = null;
		Date oEndToDateProvided = null;

		if (!Utils.isNullOrEmpty(sQueryStartFromDate) && !Utils.isNullOrEmpty(sQueryEndToDate)) {
			oStartFromDateProvided = Utils.getYyyyMMddTZDate(sQueryStartFromDate);
			oEndToDateProvided = Utils.getYyyyMMddTZDate(sQueryEndToDate);
		}

		if (oStartFromDateProvided == null || oEndToDateProvided == null) {
			oStartFromDate = getDefaultStartDate();
			oEndToDate = getDefaultEndDate();
		} else {
			if (oEndToDateProvided.before(oStartFromDateProvided)) {
				WasdiLog.debugLog("QueryExecutorGPM.executeCount | the end date preceedes the start date. oStartFromDate = " + oStartFromDateProvided + "; " + "oEndToDate = " + oEndToDateProvided + "; Inverting the dates.");

				oStartFromDate = oEndToDateProvided;
				oEndToDate = oStartFromDateProvided;
			} else {
				oStartFromDate = oStartFromDateProvided;
				oEndToDate = oEndToDateProvided;
			}
		}

		String sBaseUrl;

		final String sDuration;
		final String sAccumulation;
		final String sExtension;

		if (oGPMQuery.productName == null) {
			oGPMQuery.productName = LATENCY_LATE;
		}

		if (oGPMQuery.productType == null) {
			if (LATENCY_EARLY.equalsIgnoreCase(oGPMQuery.productName)) {
				oGPMQuery.productType = DURATION_HHR;
			} else {
				oGPMQuery.productType = DURATION_DAY;
			}
		}

		sDuration = oGPMQuery.productType;

		if (LATENCY_EARLY.equalsIgnoreCase(oGPMQuery.productName)) {
			sBaseUrl = URL_TEXT_EARLY;

			if (DURATION_DAY.equalsIgnoreCase(oGPMQuery.productType)
					|| DURATION_MO.equalsIgnoreCase(oGPMQuery.productType)) {
				sAccumulation = null;
				sExtension = ".zip";
			} else if (ACCUMULATION_ALL.equalsIgnoreCase(oGPMQuery.productLevel)
					|| ACCUMULATION_3DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)
					|| ACCUMULATION_7DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)) {
				sAccumulation = null;
				sExtension = EXTENSION_TIF;
			} else {
				sAccumulation = oGPMQuery.productLevel;
				sExtension = EXTENSION_TIF;
			}
		} else {
			sBaseUrl = URL_TEXT_LATE;

			if (DURATION_DAY.equalsIgnoreCase(oGPMQuery.productType)
					|| DURATION_MO.equalsIgnoreCase(oGPMQuery.productType)) {
				sAccumulation = null;
				sExtension = ".zip";
			} else if (ACCUMULATION_ALL.equalsIgnoreCase(oGPMQuery.productLevel)) {
				sAccumulation = null;
				sExtension = EXTENSION_TIF;
			} else if (ACCUMULATION_3DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)) {
				sAccumulation = ACCUMULATION_3DAY;
				sExtension = EXTENSION_TIF;
			} else if (ACCUMULATION_7DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)) {
				sAccumulation = ACCUMULATION_7DAY;
				sExtension = EXTENSION_TIF;
			} else {
				sAccumulation = oGPMQuery.productLevel;
				sExtension = EXTENSION_TIF;
			}
		}


		List<String> aoMonthsList = getMonthsBetweenDatesIncluding(oStartFromDate, oEndToDate);

		for (String sMonth : aoMonthsList) {
			if (Utils.isNullOrEmpty(sMonth)) {
				continue;
			}

			String sUrl = sBaseUrl + sMonth;
			String sHtmlPageSource = performRequest(sUrl);

			if (Utils.isNullOrEmpty(sHtmlPageSource)) {
				continue;
			}

			int indexOfText = sUrl.indexOf("/text/");
			String relativePath = sUrl.substring(indexOfText + 5);
			List<QueryCountResponseEntry> list = parseCountResponse(sHtmlPageSource, relativePath);

			List<QueryCountResponseEntry> filteredList = list.stream()
					.filter(t -> sDuration.equalsIgnoreCase(t.getDuration()))
					.filter(t -> sExtension.equalsIgnoreCase(t.getExtension()))
					.filter(t -> sAccumulation == null || sAccumulation.equalsIgnoreCase(t.getAccumulation()))
					.filter(t -> DURATION_MO.equalsIgnoreCase(t.getDuration()) || !(oStartFromDate.after(t.getDate())))
					.filter(t -> DURATION_MO.equalsIgnoreCase(t.getDuration()) || !(oEndToDate.before(t.getDate())))
					.collect(Collectors.toList());


			iCount += filteredList.size();
		}

		return iCount;
	}

	private static List<String> getMonthsBetweenDatesIncluding(Date oStartDate, Date oEndDate) {
		List<String> monthsList = new ArrayList<>();

		Calendar oStartDateCalendar = Calendar.getInstance();
		oStartDateCalendar.setTimeInMillis(oStartDate.getTime());

		Calendar oEndDateCalendar = Calendar.getInstance();
		oEndDateCalendar.setTimeInMillis(oEndDate.getTime());

		int iStartDateYear = oStartDateCalendar.get(Calendar.YEAR);
		int iStartDateMonth = oStartDateCalendar.get(Calendar.MONTH);

		int iEndDateYear = oEndDateCalendar.get(Calendar.YEAR);
		int iEndDateMonth = oEndDateCalendar.get(Calendar.MONTH);

		int iStartingMonth = iStartDateYear * 12 + iStartDateMonth;
		int iEndingMonth = iEndDateYear * 12 + iEndDateMonth;
		for (int i = iStartingMonth; i <= iEndingMonth; i++) {
			String sEntry = "" + (i / 12) + "/" + String.format("%02d", ((i % 12) + 1)) + "/";
			monthsList.add(sEntry);
		}

		return monthsList;
	}

	private static Date getDefaultStartDate() {
		Date oNow = new Date();

		Calendar oStartDateCalendar = Calendar.getInstance();
		oStartDateCalendar.setTimeInMillis(oNow.getTime());
		oStartDateCalendar.set(Calendar.HOUR, 0);
		oStartDateCalendar.set(Calendar.MINUTE, 0);
		oStartDateCalendar.set(Calendar.SECOND, 0);
		oStartDateCalendar.set(Calendar.MILLISECOND, 0);

		oStartDateCalendar.add(Calendar.DAY_OF_MONTH, -7);

		return oStartDateCalendar.getTime();
	}

	private static Date getDefaultEndDate() {
		Date oNow = new Date();

		Calendar oEndDateCalendar = Calendar.getInstance();
		oEndDateCalendar.setTimeInMillis(oNow.getTime());
		oEndDateCalendar.set(Calendar.HOUR, 23);
		oEndDateCalendar.set(Calendar.MINUTE, 59);
		oEndDateCalendar.set(Calendar.SECOND, 59);
		oEndDateCalendar.set(Calendar.MILLISECOND, 999);

		return oEndDateCalendar.getTime();
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		WasdiLog.debugLog("QueryExecutorGPM.executeAndRetrieve | sQuery: " + oQuery.getQuery());

		try {
			List<QueryResultViewModel> aoResults = new ArrayList<>();
 
			// Parse the query
			QueryViewModel oGPMQuery = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());

			if (!m_asSupportedPlatforms.contains(oGPMQuery.platformName)) {
				return aoResults;
			}

			String sOffset = oQuery.getOffset();
			String sLimit = oQuery.getLimit();

			int iOffset = 0;
			int iLimit = 10;

			try {
				iOffset = Integer.parseInt(sOffset);
			} catch (Exception oE) {
				WasdiLog.debugLog("QueryExecutorGPM.executeAndRetrieve: " + oE.toString());
			}

			try {
				iLimit = Integer.parseInt(sLimit);
			} catch (Exception oE) {
				WasdiLog.debugLog("QueryExecutorGPM.executeAndRetrieve: " + oE.toString());
			}

			
			final String sStartFromDate = oGPMQuery.startFromDate;
			final String sEndToDate = oGPMQuery.endToDate;

			final Date oStartFromDate;
			final Date oEndToDate;
			
			Date oStartFromDateProvided = null;
			Date oEndToDateProvided = null;

			if (!Utils.isNullOrEmpty(sStartFromDate) && !Utils.isNullOrEmpty(sEndToDate)) {
				oStartFromDateProvided = Utils.getYyyyMMddTZDate(sStartFromDate);
				oEndToDateProvided = Utils.getYyyyMMddTZDate(sEndToDate);
			}

			if (oStartFromDateProvided == null || oEndToDateProvided == null) {
				oStartFromDate = getDefaultStartDate();
				oEndToDate = getDefaultEndDate();
			} else {
				if (oEndToDateProvided.before(oStartFromDateProvided)) {
					WasdiLog.debugLog("QueryExecutorGPM.executeCount | the end date preceedes the start date. oStartFromDate = " + oStartFromDateProvided + "; " + "oEndToDate = " + oEndToDateProvided + "; Inverting the dates.");

					oStartFromDate = oEndToDateProvided;
					oEndToDate = oStartFromDateProvided;
				} else {
					oStartFromDate = oStartFromDateProvided;
					oEndToDate = oEndToDateProvided;
				}
			}

			String sBaseUrl;

			final String sDuration;
			final String sAccumulation;
			final String sExtension;

			if (oGPMQuery.productName == null) {
				oGPMQuery.productName = LATENCY_LATE;
			}

			if (oGPMQuery.productType == null) {
				if (LATENCY_EARLY.equalsIgnoreCase(oGPMQuery.productName)) {
					oGPMQuery.productType = DURATION_HHR;
				} else {
					oGPMQuery.productType = DURATION_DAY;
				}
			}

			sDuration = oGPMQuery.productType;

			if (LATENCY_EARLY.equalsIgnoreCase(oGPMQuery.productName)) {
				sBaseUrl = URL_EARLY;

				if (DURATION_DAY.equalsIgnoreCase(oGPMQuery.productType)
						|| DURATION_MO.equalsIgnoreCase(oGPMQuery.productType)) {
					sAccumulation = null;
					sExtension = ".zip";
				} else if (ACCUMULATION_ALL.equalsIgnoreCase(oGPMQuery.productLevel)
						|| ACCUMULATION_3DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)
						|| ACCUMULATION_7DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)) {
					sAccumulation = null;
					sExtension = EXTENSION_TIF;
				} else {
					sAccumulation = oGPMQuery.productLevel;
					sExtension = EXTENSION_TIF;
				}
			} else {
				sBaseUrl = URL_LATE;

				if (DURATION_DAY.equalsIgnoreCase(oGPMQuery.productType)
						|| DURATION_MO.equalsIgnoreCase(oGPMQuery.productType)) {
					sAccumulation = null;
					sExtension = ".zip";
				} else if (ACCUMULATION_ALL.equalsIgnoreCase(oGPMQuery.productLevel)) {
					sAccumulation = null;
					sExtension = EXTENSION_TIF;
				} else if (ACCUMULATION_3DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)) {
					sAccumulation = ACCUMULATION_3DAY;
					sExtension = EXTENSION_TIF;
				} else if (ACCUMULATION_7DAY_ONLY_FOR_LATE.equalsIgnoreCase(oGPMQuery.productLevel)) {
					sAccumulation = ACCUMULATION_7DAY;
					sExtension = EXTENSION_TIF;
				} else {
					sAccumulation = oGPMQuery.productLevel;
					sExtension = EXTENSION_TIF;
				}
			}

			List<String> aoMonthsList = getMonthsBetweenDatesIncluding(oStartFromDate, oEndToDate);


			for (String sMonth : aoMonthsList) {
				if (Utils.isNullOrEmpty(sMonth)) {
					continue;
				}

				String sUrl = sBaseUrl + sMonth;
				String sHtmlPageSource = performRequest(sUrl);

				if (Utils.isNullOrEmpty(sHtmlPageSource)) {
					continue;
				}

				List<QueryRetrieveResponseEntry> aoList = parseRetrieveResponse(sHtmlPageSource);

				List<QueryRetrieveResponseEntry> aoFilteredList = aoList.stream()
						.filter(t -> sDuration.equalsIgnoreCase(t.getDuration()))
						.filter(t -> sExtension.equalsIgnoreCase(t.getExtension()))
						.filter(t -> sAccumulation == null || sAccumulation.equalsIgnoreCase(t.getAccumulation()))
						.filter(t -> DURATION_MO.equalsIgnoreCase(t.getDuration()) || !(oStartFromDate.after(t.getDate())))
						.filter(t -> DURATION_MO.equalsIgnoreCase(t.getDuration()) || !(oEndToDate.before(t.getDate())))
						.collect(Collectors.toList());


				for (QueryRetrieveResponseEntry oQueryResponse : aoFilteredList) {
					QueryResultViewModel oViewModel = new QueryResultViewModel();

					// for the list of results, force the extension to tif
					String sTitle;
					if (oQueryResponse.getExtension().equalsIgnoreCase(EXTENSION_TIF)) {
						sTitle = oQueryResponse.getName();
					} else {
						sTitle = oQueryResponse.getName().replace(sExtension, EXTENSION_TIF);
					}

					oViewModel.setId(oQueryResponse.getName());
					oViewModel.setTitle(sTitle);
					oViewModel.setLink(sUrl + oQueryResponse.getName());
					oViewModel.setProvider(m_sDataProviderCode);
					oViewModel.setSummary("No summary, yet!");

					Map<String, String> aoProperties = oViewModel.getProperties();
					aoProperties.put("platformname", Platforms.IMERG);
					aoProperties.put("satellite", "MS");
					aoProperties.put("instrument", "MRG");
					aoProperties.put("algorithm", "3IMERG");
					aoProperties.put("title", sTitle);
					aoProperties.put("date", oQueryResponse.getLastModified());
					aoProperties.put("size", oQueryResponse.getSize());
					aoProperties.put("link", sUrl + oQueryResponse.getName());
					aoProperties.put("duration", oQueryResponse.getDuration());
					aoProperties.put("accumulation", oQueryResponse.getAccumulation());
					aoProperties.put("type", "tif");

					if (iOffset > 0) {
						iOffset--;
						continue;
					}

					aoResults.add(oViewModel);

					if (aoResults.size() >= iLimit) {
						return aoResults;
					}
				}
				

			}

			return aoResults;
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorGPM.executeAndRetrieve: error ", oEx);
		}

		return null;
	}

	private String performRequest(String sUrl) {
		String sResult = "";

		int iMaxRetry = 5;
		int iAttemp = 0;

		while (Utils.isNullOrEmpty(sResult) && iAttemp < iMaxRetry) {

			if (iAttemp > 0) {
				WasdiLog.debugLog("QueryExecutorGPM.performRequest.httpGetResults: attemp #" + iAttemp);
			}

			try {
				HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, HttpUtils.getBasicAuthorizationHeaders(m_sUser, m_sPassword)); 
				sResult = oHttpCallResponse.getResponseBody();
			} catch (Exception oEx) {
				WasdiLog.debugLog("QueryExecutorGPM.performRequest: exception in http get call: " + oEx.toString());
			}

			iAttemp ++;
		}

		return sResult;
	}

	public static List<QueryCountResponseEntry> parseCountResponse(String sSource, String sRelativePath) {
		List<QueryCountResponseEntry> aoList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sSource)) {
			return aoList;
		}

		String[] asLines = sSource.split("\n");

		for (String sLine : asLines) {
			QueryCountResponseEntry oQuerCountResponse = parseLine(sLine, sRelativePath);

			if (oQuerCountResponse == null) {
				continue;
			}

			aoList.add(oQuerCountResponse);
		}

		return aoList;
	}

	private static QueryCountResponseEntry parseLine(String sLine, String sRelativePath) {
		QueryCountResponseEntry oQueryCountResponse = null;

		if (Utils.isNullOrEmpty(sLine)) {
			return oQueryCountResponse;
		}

		if (!sLine.contains(sRelativePath)) {
			return oQueryCountResponse;
		}

		String sName = sLine.replace(sRelativePath, "");


		int iIndexOf3B = sName.indexOf("3B-");

		if (iIndexOf3B == -1) {
			return oQueryCountResponse;
		}

		int iIndexOfFirstDash = sName.indexOf("-", iIndexOf3B + 3);

		if (iIndexOfFirstDash == -1) {
			return oQueryCountResponse;
		}

		String sDuration = sName.substring(iIndexOf3B + 3, iIndexOfFirstDash);


		int iIndexOfImerg = sName.indexOf("IMERG");

		if (iIndexOfImerg == -1) {
			return oQueryCountResponse;
		}

		int iIndexOfFirstDot = sName.indexOf(".", iIndexOfImerg);

		if (iIndexOfFirstDot == -1) {
			return oQueryCountResponse;
		}

		String sDate = sName.substring(iIndexOfFirstDot + 1, iIndexOfFirstDot + 9);

		if (Utils.isNullOrEmpty(sDate)) {
			return oQueryCountResponse;
		}

		Date oDate = Utils.getYyyyMMddDate(sDate);

		int iIndexOfExtensionDot = sName.lastIndexOf(".");

		int iIndexOfAccumulationDot = sName.substring(0, iIndexOfExtensionDot).lastIndexOf(".");

		String sAccumulation = sName.substring(iIndexOfAccumulationDot + 1, iIndexOfExtensionDot);

		String sExtension = sName.substring(iIndexOfExtensionDot);

		oQueryCountResponse = new QueryCountResponseEntry();
		oQueryCountResponse.setName(sName);
		oQueryCountResponse.setDuration(sDuration);
		oQueryCountResponse.setAccumulation(sAccumulation);
		oQueryCountResponse.setExtension(sExtension);
		oQueryCountResponse.setDate(oDate);

		return oQueryCountResponse;
	}

	private static List<QueryRetrieveResponseEntry> parseRetrieveResponse(String sPageSource) {
		List<QueryRetrieveResponseEntry> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPageSource)) {
			return aoReturnList;
		}

		Document oDoc = Jsoup.parse(sPageSource);

		for (Element oTable : oDoc.select("table")) {
			for (Element oRow : oTable.select("tr")) {
				try {
					Elements aoTdElements = oRow.select("td");

					if (aoTdElements.isEmpty()) {
						continue;
					}

					if (aoTdElements.get(1).text().equalsIgnoreCase("Parent Directory")) {
						continue;
					}

					Element oTd1Element = aoTdElements.get(1);
					Elements oaAncorElements = oTd1Element.select("a");

					if (oaAncorElements == null || oaAncorElements.size() == 0) {
						continue;
					}

					Element oAncorElement = oTd1Element.select("a").first();
					String sName = oAncorElement.attr("href");

					String sLastModified = aoTdElements.get(2).text().trim();
					String sSize = aoTdElements.get(3).text();

					QueryRetrieveResponseEntry oResponseObject = new QueryRetrieveResponseEntry();

					oResponseObject.setName(sName);

					int iIndexOf3B = sName.indexOf("3B-");
					int iIndexOfFirstDash = sName.indexOf("-", iIndexOf3B + 3);
					String sDuration = sName.substring(iIndexOf3B + 3, iIndexOfFirstDash);
					oResponseObject.setDuration(sDuration);

					int iIndexOfExtensionDot = sName.lastIndexOf(".");

					int iIndexOfAccumulationDot = sName.substring(0, iIndexOfExtensionDot).lastIndexOf(".");

					String sAccumulation = sName.substring(iIndexOfAccumulationDot + 1, iIndexOfExtensionDot);
					oResponseObject.setAccumulation(sAccumulation);

					String sExtension = sName.substring(iIndexOfExtensionDot);
					oResponseObject.setExtension(sExtension);

					oResponseObject.setLastModified(sLastModified);

					int iIndexOfImerg = sName.indexOf("IMERG");

					if (iIndexOfImerg > -1) {
						int iIndexOfFirstDot = sName.indexOf(".", iIndexOfImerg);

						if (iIndexOfFirstDot > -1) {
							String sDate = sName.substring(iIndexOfFirstDot + 1, iIndexOfFirstDot + 9);

							if (!Utils.isNullOrEmpty(sDate)) {
								oResponseObject.setDate(Utils.getYyyyMMddDate(sDate));
							}
						}
					}

					oResponseObject.setSize(sSize);

					aoReturnList.add(oResponseObject);					
				}
				catch (Exception oEx) {
					WasdiLog.debugLog("QueryExecutorGPM.parseRetrieveResponse: exception handling a row of answers: " + oEx.toString());
				}

			}
		}

		return aoReturnList;
	}

}
