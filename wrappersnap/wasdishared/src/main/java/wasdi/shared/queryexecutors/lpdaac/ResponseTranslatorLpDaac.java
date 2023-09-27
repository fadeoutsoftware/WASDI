package wasdi.shared.queryexecutors.lpdaac;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

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
	private static final String SURL = "url";
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getCountResult(String sQueryResult) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	public QueryResultViewModel translate(ModisItemForReading oItem) {
		Preconditions.checkNotNull(oItem, "ResponseTranslatorModis.translate: null object");
		
		QueryResultViewModel oResult = new QueryResultViewModel();
		oResult.setProvider("MODIS");		
		
		// set id, title
		addMainInfo(oItem, oResult);
		addFootPrint(oItem.getBoundingBox(), oResult);
		addProperties(oItem, oResult);
		addLink(oResult);
		addSummary(oResult);
		return oResult;
	}
	
	private void addMainInfo(ModisItemForReading oItem, QueryResultViewModel oResult) {
		oResult.setId(oItem.getFileName());
		oResult.setTitle(oItem.getFileName());
	}
	
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

		oResult.getProperties().put(SLINK, oLink.toString());
		oResult.setLink(oLink.toString());
	}
	
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
