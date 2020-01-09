/**
 * Created by Cristiano Nattero on 2019-12-23
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

import com.google.common.base.Preconditions;

/**
 * @author c.nattero
 *
 */
public class DiasResponseTranslatorCREODIAS implements DiasResponseTranslator {

	private static final String SSIZE = "size";
	private static final String SPLATFORM = "platform";
	private static final String SSENSOR_MODE = "sensorMode";
	private static final String SINSTRUMENT = "instrument";
	private static final String SDATE = "date";
	private static final String STYPE = "type";

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasResponseTranslator#translate(java.lang.Object, java.lang.String)
	 */
	@Override
	public QueryResultViewModel translate(Object oResponseViewModel, String sProtocol) {
		// TODO translate single entry into a view model
		return null;
	}

	@Override
	public List<QueryResultViewModel> translateBatch(String sJson, boolean bFullViewModel, String sDownloadProtocol) {
		Preconditions.checkNotNull(sJson, "DiasResponseTranslatorCREODIAS.translateBatch: sJson is null" );

		List<QueryResultViewModel> aoResults = new ArrayList<>();

		//todo isolate single entry, pass it to translate and append the result
		try {
			JSONObject oJson = new JSONObject(sJson);
			JSONArray aoFeatures = oJson.optJSONArray("features");
			for (Object oItem : aoFeatures) {
				if(null!=oItem) {
					JSONObject oJsonItem = (JSONObject)(oItem);
					QueryResultViewModel oViewModel = translate(oJsonItem, sDownloadProtocol, bFullViewModel);
					if(null != oViewModel) {
						aoResults.add(oViewModel);
					}
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("DiasResponseTranslatorCREODIAS.translateBatch: " + oE);
		}
		return aoResults;
	}


	public QueryResultViewModel translate(JSONObject oInJson, String sDownloadProtocol, boolean bFullViewModel) {
		Preconditions.checkNotNull(oInJson, "DiasResponseTranslatorCREODIAS.translate: null json");

		if (null == sDownloadProtocol ) {
			//default protocol to https, trying to stay safe
			sDownloadProtocol = "https";
		}
		QueryResultViewModel oResult = new QueryResultViewModel();
		oResult.setProvider("CREODIAS");
		
		parseMainInfo(oInJson, oResult);		
		parseFootPrint(oInJson, oResult);
		parseProperties(oInJson, bFullViewModel, oResult);
		
		buildSummary(oResult);


		return oResult;
	}


	private void parseMainInfo(JSONObject oInJson, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oInJson, "DiasResponseTranslatorCREODIAS.addMainInfo: input json is null");
		Preconditions.checkNotNull(oResult,"DiasResponseTranslatorCREODIAS.addMainInfo: QueryResultViewModel is null");
				
		if(!oInJson.isNull("id")) {
			oResult.setId(oInJson.optString("id", null));
		}
		
		if(!oInJson.isNull(DiasResponseTranslatorCREODIAS.STYPE)){
			oResult.getProperties().put(DiasResponseTranslatorCREODIAS.STYPE, oInJson.optString(DiasResponseTranslatorCREODIAS.STYPE, null));
		}
	}

	/**
	 * @param oInJson
	 * @param oResult
	 */
	protected void parseFootPrint(JSONObject oInJson, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oInJson, "DiasResponseTranslatorCREODIAS.parseFootPrint: input json is null");
		Preconditions.checkNotNull(oResult, "DiasResponseTranslatorCREODIAS.parseFootPrint: QueryResultViewModel is null");
		
		String sBuffer = null;
		JSONObject oGeometry = oInJson.optJSONObject("geometry");
		if(null!=oGeometry) {
			/*
			"geometry": {
                "type": "MultiPolygon",
                "coordinates":
                //0
				[
					//1
                	[
                	 	//2
                		[
                			[9.5857, 43.772], [10.1173, 45.7206], [6.9613, 46.0018], [6.536, 44.0496], [9.5857, 43.772]
            			]
                	]
                ]
            },
			*/
			
			sBuffer = oGeometry.optString(DiasResponseTranslatorCREODIAS.STYPE, null);

			if(null != sBuffer) {
				String sFootPrint = sBuffer.toUpperCase();
				sFootPrint += " (((";
				JSONArray aoCoordinates = oGeometry.optJSONArray("coordinates"); //0
				if(null!=aoCoordinates) {
					aoCoordinates = aoCoordinates.optJSONArray(0); //1
					if(null!=aoCoordinates) {
						aoCoordinates = aoCoordinates.optJSONArray(0); //2
						if(null!=aoCoordinates) {
							for (Object oItem: aoCoordinates) {
								if(null!=oItem) {
									JSONArray aoPoint = (JSONArray) oItem;
									if( null != aoPoint ) {
										Double dx = aoPoint.optDouble(0);
										Double dy = aoPoint.optDouble(1);
										if(!Double.isNaN(dx) && !Double.isNaN(dy) ){
											sFootPrint += dx;
											sFootPrint += " ";
											sFootPrint += dy;
											sFootPrint += ", ";
										}
									}
								}
							}
						}
					}
				}
				//remove ending spaces and commas in excess 
				sFootPrint = sFootPrint.trim();
				while(sFootPrint.endsWith(",") ) {			
					sFootPrint = sFootPrint.substring(0,  sFootPrint.length() - 1 );
					sFootPrint = sFootPrint.trim();
				}
				sFootPrint += ")))";
				oResult.setFootprint(sFootPrint);
			}
			
		}
	}

	private void parseProperties(JSONObject oInJson, boolean bFullViewModel, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oInJson, "DiasResponseTranslatorCREDODIAS.addProperties: input json is null");
		Preconditions.checkNotNull(oResult, "DiasResponseTranslatorCREDODIAS.addProperties: QueryResultViewModel is null");

		JSONObject oProperties = oInJson.optJSONObject("properties");
		if(null == oProperties) {
			Utils.debugLog("DiasResponseTranslatorCREDODIAS.addProperties: input json has null properties");
			return;
		}

		//title
		if(!oProperties.isNull("title")) {
			oResult.setTitle(oProperties.optString("title", null));
			oResult.getProperties().put("title", oProperties.optString("title", null));
		}

		String sBuffer = null;
		//preview
		if(bFullViewModel) {
			sBuffer = oProperties.optString("quicklook", null);
			if(null!= sBuffer) {
				oResult.setPreview(sBuffer);
			}
		}
		
		//	todo link: create procedure to provide link
		sBuffer = oProperties.optString("startDate", null);
		if(null!=sBuffer) {
			oResult.getProperties().put(DiasResponseTranslatorCREODIAS.SDATE, sBuffer);
			oResult.getProperties().put("startDate", sBuffer);
		}
		
		if(!oProperties.isNull(DiasResponseTranslatorCREODIAS.SINSTRUMENT)) {
			oResult.getProperties().put(DiasResponseTranslatorCREODIAS.SINSTRUMENT, oProperties.optString(DiasResponseTranslatorCREODIAS.SINSTRUMENT, null));
		}

		if(!oProperties.isNull(DiasResponseTranslatorCREODIAS.SSENSOR_MODE)) {
			oResult.getProperties().put((DiasResponseTranslatorCREODIAS.SSENSOR_MODE), oProperties.optString((DiasResponseTranslatorCREODIAS.SSENSOR_MODE), null));
		}

		if(!oProperties.isNull(DiasResponseTranslatorCREODIAS.SPLATFORM)) {
			oResult.getProperties().put(DiasResponseTranslatorCREODIAS.SPLATFORM, oProperties.optString(DiasResponseTranslatorCREODIAS.SPLATFORM, null));
		}

		// todo size ->
		/*

		"services": {
         	"download": {
				"url": "https://zipper.creodias.eu/download/221165ce-4e4e-5b52-8c34-1af8f6b7154e",
                "mimeType": "application/unknown",
                "size": 1716760163
			}
		},

		*/
		JSONObject oTemp = oProperties.optJSONObject("services");
		if(null != oTemp) {
			oTemp = oTemp.optJSONObject("download");
			if(null!=oTemp) {
				long lSize = oTemp.optLong(DiasResponseTranslatorCREODIAS.SSIZE, -1);
				if(0<=lSize) {
					double dTmp = (double) lSize;
					String sSize = Utils.getNormalizedSize(dTmp);
					oResult.getProperties().put(DiasResponseTranslatorCREODIAS.SSIZE, sSize);
				}
			}
		}

	}
	
	protected void buildSummary(QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oResult, "DiasResponseTranslatorCREDODIAS.buildSummary: QueryResultViewModel is null");
		
		//summary
		//"summary": "Date: 2020-01-03T06:01:45.74Z, Instrument: SAR-C SAR, Mode: VV VH, Satellite: Sentinel-1, Size: 1.64 GB",
		
		
		String sDate = oResult.getProperties().get(DiasResponseTranslatorCREODIAS.SDATE);
		String sSummary = "Date: " + sDate + ", ";
		String sInstrument = oResult.getProperties().get(DiasResponseTranslatorCREODIAS.SINSTRUMENT);
		sSummary = sSummary + "Instrument: " + sInstrument + ", ";
		
		//todo sensorMode
		String sMode = oResult.getProperties().get(DiasResponseTranslatorCREODIAS.SSENSOR_MODE);
		sSummary = sSummary + "Mode: " + sMode + ", ";
		//TODO infer Satellite from filename
		String sSatellite = oResult.getProperties().get(DiasResponseTranslatorCREODIAS.SPLATFORM);
		sSummary = sSummary + "Satellite: " + sSatellite + ", ";
		String sSize = oResult.getProperties().get(DiasResponseTranslatorCREODIAS.SSIZE);
		sSummary = sSummary + "Size: " + sSize;// + " " + sChosenUnit;
		oResult.setSummary(sSummary);
	}

}
