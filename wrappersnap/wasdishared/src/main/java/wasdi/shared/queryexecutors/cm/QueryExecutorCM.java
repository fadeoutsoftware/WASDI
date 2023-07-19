package wasdi.shared.queryexecutors.cm;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;

import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * Copernicus Marine Query Executor.
 * This class queries the Copernicus Marine Catalogue from https://resources.marine.copernicus.eu/
 * 
 * @author PetruPetrescu
 *
 */
public class QueryExecutorCM extends QueryExecutor {

	/**
	 * Static reference to the provider config
	 */
	private static DataProviderConfig s_oDataProviderConfig;

	public QueryExecutorCM() {
		m_sProvider = "COPERNICUSMARINE";
		s_oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(m_sProvider);

		this.m_oQueryTranslator = new QueryTranslatorCM();
		this.m_oResponseTranslator = new ResponseTranslatorCM();
	}

	/**
	 * Overload of the get URI from Product Name method.
	 * For CM, we need just the original link..
	 */
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {
		return sOriginalUrl;
	}

	/**
	 * "Fake" execute Count: the service is able to return a single net-cdf file for each request.
	 * So the result is always 0 in case of problems or 1 otherwise.
	 */
	@Override
	public int executeCount(String sQuery) {
		WasdiLog.debugLog("QueryExecutorCM.executeCount | sQuery: " + sQuery);

		int iCount = 0;

		if (Utils.isNullOrEmpty(s_oDataProviderConfig.link)) {
			return iCount;
		}

		// Parse the query
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);

		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			return iCount;
		}

		return 1;
	}
	
	/**
	 * The method must create the Result View Model
	 */
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		WasdiLog.debugLog("QueryExecutorCM.executeAndRetrieve | sQuery: " + oQuery.getQuery());

		List<QueryResultViewModel> aoResults = new ArrayList<>();

		if (Utils.isNullOrEmpty(s_oDataProviderConfig.link)) {
			return aoResults;
		}

		// Parse the query
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());

		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			return aoResults;
		}

		QueryResultViewModel oResult = new QueryResultViewModel();

		String sService = oQueryViewModel.productType;
		String sProduct = oQueryViewModel.productName;

		oResult.setId(sService);
		oResult.setTitle(sProduct + "_" + Utils.nowInMillis().longValue() + ".nc");

		StringBuilder oSBQuery = new StringBuilder("");

		oSBQuery.append("&x_lo=").append(oQueryViewModel.west);
		oSBQuery.append("&x_hi=").append(oQueryViewModel.east);
		oSBQuery.append("&y_lo=").append(oQueryViewModel.south);
		oSBQuery.append("&y_hi=").append(oQueryViewModel.north);

		final String sQueryStartFromDate = oQueryViewModel.startFromDate;
		final String sQueryEndToDate = oQueryViewModel.endToDate;

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

		String sStartFromDate = Utils.formatToYyyyDashMMDashdd(oStartFromDate);
		String sEndToDate = Utils.formatToYyyyDashMMDashdd(oEndToDate);
		oSBQuery.append("&t_lo=").append(sStartFromDate);
		oSBQuery.append("&t_hi=").append(sEndToDate);


		String sVariables = oQueryViewModel.sensorMode;

		if (!Utils.isNullOrEmpty(sVariables)) {
			
			String[] asVariables = sVariables.split(" ");

			for (String sVariable : asVariables) {
				oSBQuery.append("&variable=").append(sVariable);
			}
		}

		Double dStartDepth = oQueryViewModel.cloudCoverageFrom;
		if (dStartDepth != null) {
			oSBQuery.append("&z_lo=").append(dStartDepth);
		}

		Double dEndDepth = oQueryViewModel.cloudCoverageTo;
		if (dEndDepth != null) {
			oSBQuery.append("&z_hi=").append(dEndDepth);
		}

		String sQuery = oSBQuery.toString();

		String sLinks = s_oDataProviderConfig.link;
		String[] asLinks = sLinks.split(" ");

		for (String sDomainUrl : asLinks) {
			// discard the result of an eventual previous request (made to an alternative URL)
			aoResults.clear();
			oResult.setSummary(null);
			oResult.getProperties().remove("error");

			String sProductSize = CMHttpUtils.getProductSize(sService, sProduct, sQuery, sDomainUrl, s_oDataProviderConfig.user, s_oDataProviderConfig.password);

			if (sProductSize.startsWith("Error: ")) {
				String sErrorMessage = sProductSize.replace("Error: ", "");

				if (sErrorMessage.contains("Exception: ")) {
					sErrorMessage = sErrorMessage.substring(sErrorMessage.indexOf("Exception: ") + "Exception: ".length());
				}

				oResult.getProperties().put("error", sErrorMessage);

				String sSummary = sProductSize;

				oResult.setSummary(sSummary);

				aoResults.add(oResult);

				// if the error is of type dataset-unknown, a new attempt should/could be done on an alternative URL.
				// else, use/show the current error message
				if (sProductSize.contains("005-29") || sProductSize.contains("product/dataset is unknown")) {
					continue;
				} else {
					break;
				}
			}

			String sNormalizedSize;
			if (NumberUtils.isCreatable(sProductSize)) {
				double dSizeinKB = Double.parseDouble(sProductSize);
				double dSizeinB = dSizeinKB * 1024;
				String sUnit = "B";
				sNormalizedSize = Utils.getNormalizedSize(dSizeinB, sUnit);

				oResult.getProperties().put("sizeInBytes", "" + (long) dSizeinB);
				oResult.getProperties().put("size", sNormalizedSize);
			} else {
				sNormalizedSize = sProductSize;
			}

			oResult.getProperties().put("platformname", Platforms.CM);
			oResult.getProperties().put("productType", oQueryViewModel.productType);
			oResult.getProperties().put("dataset", oQueryViewModel.productName);
			oResult.getProperties().put("variables", oQueryViewModel.sensorMode);
			oResult.getProperties().put("protocol", "SUBS");
			oResult.getProperties().put("format", "nc");
			oResult.getProperties().put("startDate", oQueryViewModel.startFromDate);
			oResult.getProperties().put("endDate", oQueryViewModel.endToDate);

			oResult.setFootprint(extractFootprint(oQuery.getQuery()));


			Pattern oPattern = Pattern.compile("(\\d\\d+_)+(\\d+)");
			Matcher oMatcher = oPattern.matcher(oQueryViewModel.productType);
			String sMode = "";

			while(oMatcher.find()) {
				sMode = oMatcher.group();
			}

			String sInstrument;

			if (oQueryViewModel.productName == null) {
				sInstrument = "";
			} else if (oQueryViewModel.productName.length() > 20) {
				sInstrument = oQueryViewModel.productName.substring(oQueryViewModel.productName.length() - 20);
			} else if (oQueryViewModel.productName.length() > 10) {
				sInstrument = oQueryViewModel.productName.substring(oQueryViewModel.productName.length() - 10);
			} else {
				sInstrument = oQueryViewModel.productName;
			}

			String sDate;
			if (sStartFromDate.equals(sEndToDate)) {
				sDate = sStartFromDate;
			} else {
				sDate = sStartFromDate + " - " + sEndToDate;
			}

			oResult.getProperties().put("beginposition", sDate);

			String sSummary = "Date: " + sDate + ", ";
			sSummary = sSummary + "Mode: " + sMode + ", ";
			sSummary = sSummary + "Instrument: " + sInstrument + ", ";
			sSummary = sSummary + "Size: " + sNormalizedSize;

			oResult.setSummary(sSummary);


			oResult.setProvider(m_sProvider);

			String sSizeInBytes = oResult.getProperties().get("sizeInBytes");

			String sLink = "http://" + "&service=" + sService + "&product=" + sProduct + "&query=" + sQuery;

			if (!Utils.isNullOrEmpty(sSizeInBytes)) {
				sLink = sLink + "&size=" + sSizeInBytes;
			}

			oResult.setLink(sLink);

			oResult.getProperties().put("link", sLink);

			aoResults.add(oResult);

			return aoResults;
		}
		
		return aoResults;
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

	private static String extractFootprint(String sQuery) {
		String sFootprint = "";

		if (sQuery.contains("footprint")) {
			String sIntro = "( footprint:\"intersects(POLYGON((";
			int iStart = sQuery.indexOf(sIntro);
			if (iStart >= 0) {
				iStart += sIntro.length();
			}
			int iEnd = sQuery.indexOf(')', iStart);
			if (0 > iEnd) {
				iEnd = sQuery.length();
			}

			sFootprint = sQuery.substring(iStart, iEnd).trim();
		}

		return sFootprint;
	}

}
