package wasdi.shared.queryexecutors.cloudferro;

import java.util.List;

import com.google.common.base.Preconditions;

import wasdi.shared.business.ecostress.EcoStressItemForReading;
import wasdi.shared.business.ecostress.EcoStressLocation;
import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.utils.Utils;

public class ResponseTranslatorCloudferro extends ResponseTranslator {

	private static final String SLINK_SEPARATOR = ",";
	private static final String SLINK = "link";

	private static final String SSIZE_IN_BYTES = "sizeInBytes";
	private static final String SHREF = "href";
	private static final String SURL = "url";
//	private static final String SPOLARISATION = "polarisation";
	private static final String SRELATIVEORBITNUMBER = "relativeOrbitNumber";
	private static final String SSIZE = "size";
	private static final String SPLATFORM = "platform";
	private static final String SSENSOR_MODE = "sensorMode";
	private static final String SINSTRUMENT = "instrument";
	private static final String SDATE = "date";
//	private static final String STYPE = "type";
	private static final String SSELF = "self";
	private static final String STITLE = "title";
	private static final String SPRODUCTIDENTIFIER = "productIdentifier";

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

		parseMainInfo(oItem, oResult);

		parseProperties(oItem, oResult);
		parseFootPrint(oItem.getLocation(), oResult);

		oResult.setPreview(null);
//		protected String summary;

		String sLink = oItem.getUrl() + "?fileName=" + oItem.getFileName() + "&filePath=" + oItem.getS3Path();
		oResult.setLink(sLink);

		return oResult;
	}

	private void parseMainInfo(EcoStressItemForReading oItem, QueryResultViewModel oResult) {
		oResult.setId(oItem.getFileName());
		oResult.setTitle(oItem.getFileName());
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

	private void parseProperties(EcoStressItemForReading oItem, QueryResultViewModel oResult) {
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
