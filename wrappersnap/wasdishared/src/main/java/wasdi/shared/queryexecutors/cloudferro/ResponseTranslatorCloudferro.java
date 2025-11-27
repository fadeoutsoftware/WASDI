package wasdi.shared.queryexecutors.cloudferro;

import java.util.List;

import com.google.common.base.Preconditions;

import wasdi.shared.business.ecostress.EcoStressItemForReading;
import wasdi.shared.business.ecostress.EcoStressLocation;
import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class ResponseTranslatorCloudferro extends ResponseTranslator {

	private static final String SLINK_SEPARATOR = ",";
	private static final String SLINK = "link";
	private static final String SSIZE_IN_BYTES = "sizeInBytes";
	private static final String SHREF = "href";
	private static final String SURL = "url";
	private static final String SSIZE = "size";
	private static final String SPLATFORM = "platform";
	private static final String SDATE = "date";
	private static final String SSELF = "self";
	private static final String STITLE = "title";
	private static final String SPRODUCTIDENTIFIER = "productIdentifier";
	
	protected String m_sVolumeName="ecostress_fast";

	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		return null;
	}

	@Override
	public int getCountResult(String sQueryResult) {
		return 0;
	}

	public QueryResultViewModel translate(EcoStressItemForReading oItem) {
		Preconditions.checkNotNull(oItem, "ResponseTranslatorCloudferro.translate: null json");

		QueryResultViewModel oResult = new QueryResultViewModel();
		oResult.setProvider("CLOUDFERRO");

		oResult.setId(oItem.getFileName());
		oResult.setTitle(oItem.getFileName());
		parseFootPrint(oItem.getLocation(), oResult);
		parseProperties(oItem, oResult);

		
		buildLink(oResult);
		buildSummary(oResult);
		
		String sVolumePath = m_sVolumeName + "/"+oItem.getS3Path();
		sVolumePath = sVolumePath.replace(oItem.getFileName(), "");
		oResult.setVolumeName(m_sVolumeName);
		oResult.setVolumePath(sVolumePath);

		return oResult;
	}

	private void parseFootPrint(EcoStressLocation sLocation, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(sLocation, "QueryExecutorCloudferro.parseFootPrint: input sLocation is null");
		Preconditions.checkNotNull(oResult, "QueryExecutorCloudferro.parseFootPrint: QueryResultViewModel is null");

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

	protected void buildSummary(QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oResult, "QueryExecutorCloudferro.buildSummary: QueryResultViewModel is null");

		String sDate = oResult.getProperties().get(SDATE);
		String sSummary = "Date: " + sDate + ", ";
		
		String sSatellite = oResult.getProperties().get(SPLATFORM);
		sSummary = sSummary + "Satellite: " + sSatellite + ", ";
		
		String sSize = oResult.getProperties().get(SSIZE);
		sSummary = sSummary + "Size: " + sSize;// + " " + sChosenUnit;
		oResult.setSummary(sSummary);
	}

	private void parseProperties(EcoStressItemForReading oItem, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oItem, "QueryExecutorCloudferro.addProperties: input oItem is null");
		Preconditions.checkNotNull(oResult, "QueryExecutorCloudferro.addProperties: QueryResultViewModel is null");

		oResult.setTitle(oItem.getFileName());
		oResult.getProperties().put(STITLE, oItem.getFileName());

		oResult.setPreview(null);

		if (oItem.getBeginningDate() != null) {
			String sStartDate = TimeEpochUtils.fromEpochToDateString(oItem.getBeginningDate().longValue());
			oResult.getProperties().put(SDATE, sStartDate);
			oResult.getProperties().put("startDate", sStartDate);
			oResult.getProperties().put("beginposition", sStartDate);
		}

		oResult.getProperties().put(SPLATFORM, "ECOSTRESS");
		oResult.getProperties().put("platformname", "ECOSTRESS");

		oResult.getProperties().put("polarisationmode", oItem.getDayNightFlag());

		parseLinks(oItem, oResult);

		parseServices(oResult, oItem);
	}

	/**
	 * @param oItem
	 * @param oResult
	 */
	private void parseLinks(EcoStressItemForReading oItem, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oItem, "oItem is null");
		Preconditions.checkNotNull(oResult, "result view model is null");

		oResult.getProperties().put(SHREF, oItem.getUrl());
	}

	/**
	 * @param oResult
	 * @param oProperties
	 */
	private void parseServices(QueryResultViewModel oResult, EcoStressItemForReading oItem) {
		String sUrl = null;

		long lSize = 0;

		sUrl = oItem.getUrl();
		oResult.getProperties().put(SSIZE_IN_BYTES, "" + lSize);
		String sSize = "";
		oResult.getProperties().put(SSIZE, sSize);

		if (!Utils.isNullOrEmpty(sUrl)) {
			oResult.getProperties().put(SURL, sUrl);
			oResult.setLink(sUrl);
		} else {
			WasdiLog.debugLog("ResponseTranslatorCloudferro.parseServices: download link not found! dumping object:\n"
					+ "Object DUMP BEGIN\n" + oItem.toString() + "Object DUMP END");
		}
	}

	private void buildLink(QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oResult, "result view model is null");

		StringBuilder oLink = new StringBuilder("");

		String sItem = "";

		sItem = oResult.getProperties().get(SURL);
		if (sItem == null || sItem.isEmpty()) {
			WasdiLog.debugLog("ResponseTranslatorCloudferro.buildLink: the download URL is null or empty. Product title: " + oResult.getTitle());
			sItem = "http://";
		}
		oLink.append(sItem).append(SLINK_SEPARATOR); //0

		sItem = oResult.getTitle();
		if (sItem == null) {
			sItem = "";
		}

		oLink.append(sItem).append(SLINK_SEPARATOR); //1

		sItem = oResult.getProperties().get(SSIZE_IN_BYTES);
		if (sItem == null) {
			sItem = "";
		}
		oLink.append(sItem).append(SLINK_SEPARATOR); //2

		sItem = oResult.getProperties().get("status");
		if (sItem == null) {
			sItem = "";
		}
		oLink.append(sItem).append(SLINK_SEPARATOR); //3

		sItem = oResult.getProperties().get(SSELF);
		if (sItem == null) {
			sItem = "";
		}
		oLink.append(sItem).append(SLINK_SEPARATOR); //4

		sItem = oResult.getProperties().get(SPRODUCTIDENTIFIER);
		if (sItem == null) {
			sItem = "";
		}
		oLink.append(sItem).append(SLINK_SEPARATOR); //5

		oResult.getProperties().put(SLINK, oLink.toString());
		oResult.setLink(oLink.toString());
	}

	public String geVolumeName() {
		return m_sVolumeName;
	}

	public void setVolumeName(String sVolumeName) {
		this.m_sVolumeName = sVolumeName;
	}

}
