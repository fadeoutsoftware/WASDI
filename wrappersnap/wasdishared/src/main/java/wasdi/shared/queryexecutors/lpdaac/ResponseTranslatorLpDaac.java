package wasdi.shared.queryexecutors.lpdaac;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.business.modis11a2.ModisItemForReading;
import wasdi.shared.business.modis11a2.ModisLocation;
import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class ResponseTranslatorLpDaac extends ResponseTranslator {
	
	public static final String SLINK_SEPARATOR = ",";
	private static final String SLINK = "link";

	private static final String SSIZE_IN_BYTES = "sizeInBytes";
	private static final String SHREF = "href";
	private static final String SSIZE = "size";
	private static final String SPLATFORM = "platform";
	private static final String SSENSOR_MODE = "sensorMode";
	private static final String SINSTRUMENT = "instrument";
	private static final String SDATE = "date";
	private static final String STITLE = "title";
	
	public static final int S_IURL_INDEX = 0;
	public static final int S_IFILE_NAME_INDEX = 1;
	public static final int S_IFILE_SIZE_INDEX = 2;

	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		List<QueryResultViewModel> oResultsList = new ArrayList<>();
		JSONObject oJsonHttpResponse = new JSONObject(sResponse);
		
		JSONObject oJsonFeed = oJsonHttpResponse.optJSONObject("feed");
		
		if (oJsonFeed == null) {
			WasdiLog.warnLog("ResponseTranslatorLpDaac.translateBatch: no 'feed' element found in the response");
			return null;
		}
		
		JSONArray oJsonEntries = oJsonFeed.optJSONArray("entry");
		
		if (oJsonEntries == null) {
			WasdiLog.warnLog("ResponseTranslatorLpDaac.translateBatch: no 'entry' element found in the response");
			return null;
		}
		
		JSONObject oJsonModisEntry = null;
		for (int i = 0; i < oJsonEntries.length(); i++) {
			try {
				oJsonModisEntry = oJsonEntries.getJSONObject(i);
				QueryResultViewModel oResViewModel = translate(oJsonModisEntry);
				if (oResViewModel != null) 
					oResultsList.add(oResViewModel);
				else 
					WasdiLog.warnLog("ResponseTranslatorLpDaac.translateBatch: failed to translate one of the entries");
			} catch(Exception oEx) {
				WasdiLog.errorLog("ResponseTranslatorLpDaac.translateBatch: error reading the json entry ", oEx);
			}
		}
		return oResultsList;
	}

	@Override
	public int getCountResult(String sQueryResult) {
		return 0;
	}
	
	
	public QueryResultViewModel translate(JSONObject oJsonEntry) {
		
		if (oJsonEntry == null) {
			WasdiLog.debugLog("ResponseTranslatorLpDaac.translate: json item is null");
			return null;
		}
		
		QueryResultViewModel oResult = new QueryResultViewModel();
		oResult.setProvider("LPDAAC");		
		
		//id
		String sId = oJsonEntry.optString("id");
		if (Utils.isNullOrEmpty(sId)) {
			WasdiLog.errorLog("ResponseTranslatorLpDaac.translate: no id found for the product. It will be impossible to download it");
			return null;
		} else {
			oResult.setId(sId);
		}
		
		// title
		String sTitle = oJsonEntry.optString("title");
		if (Utils.isNullOrEmpty(sTitle)) {
			WasdiLog.errorLog("ResponseTranslatorLpDaac.translate: no title found for the product. It will be impossible to download it");
			return null;
		} else {
			oResult.setTitle(sTitle);
		}
		
		// footprint
		JSONArray oPolygons = oJsonEntry.optJSONArray("polygons");
		JSONArray oFirstPolygon =null;
		
		if (oPolygons.length() > 0) {
			oFirstPolygon = oPolygons.optJSONArray(0);
		}
		
		if (oFirstPolygon == null) {
			WasdiLog.warnLog("ResponseTranslatorLpDaac.translate: impossible to read the bounding box");
			return null;
		}
		
		List<String> asPolygons = new ArrayList<>();
		for (int i = 0; i < oFirstPolygon.length(); i++) {
			String sPolygon = oFirstPolygon.getString(i);
			// the polygon returned by earth data has the following form: ["lat1 long1 lat2 long2 lat3 long3 lat4 long4 lat1 long1"]
			String[] asCoordinates = sPolygon.split(" ");
			int iIndex = 0;
			String sWKTCoordinates = "";
			while (iIndex < asCoordinates.length-1) {
				try {
					String sLatitude = asCoordinates[iIndex];
					String sLongitude = asCoordinates[iIndex + 1];
					if (!Utils.isNullOrEmpty(sWKTCoordinates))
						sWKTCoordinates += ", ";
					sWKTCoordinates += sLongitude + " " + sLatitude;
				} catch (IndexOutOfBoundsException oEx) {
					WasdiLog.errorLog("ResponseTranslatorLpDaac.translate: index not valid when reading the footprint", oEx);
					return null;
				}
				iIndex += 2;
			}
			asPolygons.add("(" + sWKTCoordinates + ")");
		}
		String sFootprint = "POLYGON(" + String.join(", ", asPolygons) + ")";
		oResult.setFootprint(sFootprint);
		
		// properties
		addProperties(oJsonEntry, sTitle, oResult);
		
		// link
		addLink(oResult);
		
		// summary
		addSummary(oResult);
		
		return oResult;
	}
	
	public QueryResultViewModel translate(ModisItemForReading oItem) {
		Preconditions.checkNotNull(oItem, "ResponseTranslatorModis.translate: null object");
		
		QueryResultViewModel oResult = new QueryResultViewModel();
		oResult.setProvider("LPDAAC");		
		
		// set id, title
		addMainInfo(oItem, oResult);
		addFootPrint(oItem.getBoundingBox(), oResult);
		addProperties(oItem, oResult);
		addLink(oResult);
		addSummary(oResult);
		return oResult;
	}
	
	/**
	 * add the product id and the title to the query result view model
	 * @param oItem the object representing a MODIS product, read from the DB
	 * @param oResult the result view model
	 */
	private void addMainInfo(ModisItemForReading oItem, QueryResultViewModel oResult) {
		oResult.setId(oItem.getFileName());
		oResult.setTitle(oItem.getFileName());
	}
	
	/**
	 * add the product id and the title to the query result view model
	 * @param sLocation the object representing the bounding box, as stored in the DB
	 * @param oResult the result view model
	 */
	private void addFootPrint(ModisLocation sLocation, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(sLocation, "ResponseTranslatorModis.addFootPrint: input sLocation is null");
		Preconditions.checkNotNull(oResult, "ResponseTranslatorModis.addFootPrint: QueryResultViewModel is null");

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
	
	private void addProperties(JSONObject oJsonItem, String sTitle, QueryResultViewModel oResult) {
		
		if (oJsonItem == null || oResult == null) {
			WasdiLog.warnLog("ResponseTranslatorLpDaac.addProperties. Entry or result view model are null. Properties won't be added");
			return;
		}
		
		oResult.getProperties().put(STITLE, sTitle);
		
		
		long lSizeByte = -1;
		try {
			String sSize = oJsonItem.optString("granule_size", null);
			
			if (!Utils.isNullOrEmpty(sSize)) {
				lSizeByte = (long) Double.parseDouble(sSize) * 1000;
				String sNormalizedSize = lSizeByte > 0 ? Utils.getNormalizedSize((double) lSizeByte) : "";
				oResult.getProperties().put(SSIZE_IN_BYTES, "" + lSizeByte);
				oResult.getProperties().put(SSIZE, sNormalizedSize);
			} else {
				WasdiLog.warnLog("ResponseTranslatorLpDaac.addProperties. No information about product size");
			}
		} catch (JSONException oEx) {
			WasdiLog.warnLog("ResponseTranslatorLpDaac.addProperties. Could not retrieve granule size");
		}


		oResult.setPreview(null);

		String sStartDate = oJsonItem.optString("time_start", null);
		if (!Utils.isNullOrEmpty(sStartDate)) {
			oResult.getProperties().put(SDATE, sStartDate);
			oResult.getProperties().put("startDate", sStartDate);
			oResult.getProperties().put("beginposition", sStartDate);
		}
		
		JSONArray oLinks = oJsonItem.optJSONArray("links");
		if (oLinks == null) {
			WasdiLog.warnLog("ResponseTranslatorLpDaac.addProperties. Arrays of links not found. It won't be possible to download the prouct");
			return;
		}
		
		String sLink = null;
		for (int i = 0; i < oLinks.length(); i++) {
			JSONObject oLinkItem = oLinks.getJSONObject(i);
			String sHref = oLinkItem.optString("href", null);
			String sRel = oLinkItem.optString("rel", null);
			if (!Utils.isNullOrEmpty(sHref) 
					&& !Utils.isNullOrEmpty(sRel) 
					&& sHref.startsWith("https://e4ftl01.cr.usgs.gov") 
					&& sRel.equals("http://esipfed.org/ns/fedsearch/1.1/data#")) {
				if (sTitle.startsWith("MOD11A2") && (sHref.endsWith("MOD11A2.061/") || sHref.endsWith("MOD11A2.061"))) {
					// let's build back the url
					// we first add the start date
					if (!sHref.endsWith("/"))
						sHref += "/";
					
					String sDateFolder = sStartDate.substring(0, 10).replace("-", ".");
					sHref += sDateFolder + "/" + sTitle + ".hdf";
					sLink = sHref;
					break;
				}
			}
		} 
		
		if (sLink == null) {
			WasdiLog.warnLog("ResponseTranslatorLpDaac.addProperties. Link is null. It won't be possible to download the prouct");
			return;
		}
		
		oResult.getProperties().put(SHREF, sLink);
		
		
		if (sTitle.startsWith("MOD11A2")) {
			oResult.getProperties().put(SPLATFORM, "Terra");
			oResult.getProperties().put("platformname", "Terra");

			oResult.getProperties().put((SSENSOR_MODE), "MODIS");
			oResult.getProperties().put("sensoroperationalmode", "MODIS");

			oResult.getProperties().put(SINSTRUMENT, "MODIS");
			oResult.getProperties().put("instrumentshortname", "MODIS");

		}

		String sDayNightFlag = oJsonItem.optString("day_night_flag", null);
		if (!Utils.isNullOrEmpty(sDayNightFlag)) {
			oResult.getProperties().put("polarisationmode", sDayNightFlag);
		}

	}
	
	/**
	 * Reads the properties of a MODIS product and add them to the result view model
	 * @param oItem  oItem the object representing a MODIS product, read from the DB
	 * @param oResult the result view model
	 */
	private void addProperties(ModisItemForReading oItem, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oItem, "ResponseTranslatorModis.addProperties: input oItem is null");
		Preconditions.checkNotNull(oResult, "ResponseTranslatorModis.addProperties: QueryResultViewModel is null");

		oResult.getProperties().put(STITLE, oItem.getFileName());
		oResult.getProperties().put(SHREF, oItem.getUrl());
		
		long lSizeBye = oItem.getFileSize();
		String sNormalizedSize = lSizeBye > 0 ? Utils.getNormalizedSize((double) lSizeBye) : "";
		oResult.getProperties().put(SSIZE_IN_BYTES, "" + lSizeBye);
		oResult.getProperties().put(SSIZE, sNormalizedSize);

		oResult.setPreview(null);

		if (oItem.getStartDate() != null) {
			String sStartDate = TimeEpochUtils.fromEpochToDateString(oItem.getStartDate().longValue());
			oResult.getProperties().put(SDATE, sStartDate);
			oResult.getProperties().put("startDate", sStartDate);
			oResult.getProperties().put("beginposition", sStartDate);
		}
		
		oResult.getProperties().put(SPLATFORM, oItem.getPlatform());
		oResult.getProperties().put("platformname", oItem.getPlatform());

		oResult.getProperties().put((SSENSOR_MODE), oItem.getSensor());
		oResult.getProperties().put("sensoroperationalmode", oItem.getSensor());

		oResult.getProperties().put(SINSTRUMENT, oItem.getInstrument());
		oResult.getProperties().put("instrumentshortname", oItem.getInstrument());


		oResult.getProperties().put("polarisationmode", oItem.getDayNightFlag());

	}
	
	/**
	 * Builds the link that will be passed to the provider adapter and adds it to the result view model
	 * @param oResult the result view model
	 */
	private void addLink(QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oResult, "ResponseTranslatorModis.addProperties: result view model is null");

		StringBuilder oLink = new StringBuilder("");

		String sItem = "";

		sItem = oResult.getProperties().get(SHREF); //0 - url
		if (sItem == null || sItem.isEmpty()) {
			WasdiLog.debugLog("ResponseTranslatorModis.addLink: the download URL is null or empty. Product title: " + oResult.getTitle());
			sItem = "http://";
		}
		oLink.append(sItem).append(SLINK_SEPARATOR); 

		sItem = oResult.getTitle(); //1 - title
		if (sItem == null) {
			sItem = "";
		}
		oLink.append(sItem).append(SLINK_SEPARATOR); 

		sItem = oResult.getProperties().get(SSIZE_IN_BYTES); //2 - size
		if (sItem == null) {
			sItem = "";
		}
		oLink.append(sItem);

		oResult.getProperties().put(SLINK, oLink.toString());
		oResult.setLink(oLink.toString());
	}
	
	/**
	 * Builds the summary and adds it to the result view model
	 * @param oResult the result view model
	 */
	private void addSummary(QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oResult, "ResponseTranslatorModis.addSummary: QueryResultViewModel is null");

		Map<String, String> aoProperties = oResult.getProperties();
		
		List<String> asSummaryElements = new ArrayList<>();
		
		String sProperty = aoProperties.get(SDATE);
		if (!Utils.isNullOrEmpty(sProperty))
			asSummaryElements.add("Date: " + sProperty);
		
		sProperty = aoProperties.get(SINSTRUMENT);
		if (!Utils.isNullOrEmpty(sProperty))
			asSummaryElements.add("Instrument: " + sProperty);

		sProperty = aoProperties.get(SSENSOR_MODE);
		if (!Utils.isNullOrEmpty(sProperty))
			asSummaryElements.add("Mode: " + sProperty);
		
		sProperty = aoProperties.get(SPLATFORM);
		if (!Utils.isNullOrEmpty(sProperty))
			asSummaryElements.add("Satellite: " + sProperty);
		
		sProperty = aoProperties.get(SSIZE);
		if (!Utils.isNullOrEmpty(sProperty))
			asSummaryElements.add("Size: " +  sProperty);
		
		String sSummary = String.join(", ", asSummaryElements);
		oResult.setSummary(sSummary);
	}

}
