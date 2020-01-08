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
					QueryResultViewModel oViewModel = translate(oJsonItem, sDownloadProtocol);
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
		
		String sBuffer = null;
		
		if(!oInJson.isNull("id")) {
			oResult.setId(oInJson.optString("id", null));
		}
		
		sBuffer = oInJson.optString(DiasResponseTranslatorCREODIAS.STYPE);
		if(null!=sBuffer) {
			oResult.getProperties().put(DiasResponseTranslatorCREODIAS.STYPE, sBuffer);
		}
	}

	/**
	 * @param oInJson
	 * @param oResult
	 */
	protected void parseFootPrint(JSONObject oInJson, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oInJson, "DiasResponseTranslatorCREODIAS.parseFootPrint: input json is null");
		Preconditions.checkNotNull(oResult, "DiasResponseTranslatorCREODIAS.parseFootPrint: QueryResultViewModel is null");
		
		String sBuffer;
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
						aoCoordinates.optJSONArray(0); //2
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
											sFootPrint += ",";
										}
									}
								}
							}
						}
					}
				}
				if(sFootPrint.endsWith(",") ) {			
					sFootPrint = sFootPrint.substring(0,  sFootPrint.length() - 1 );
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

		String sBuffer = null;

		//title
		sBuffer = oProperties.optString("title", null);
		if(null != sBuffer) {
			oResult.setTitle(sBuffer);
			oResult.getProperties().put("title", sBuffer);
		}

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
			oResult.getProperties().put("date", sBuffer);
			oResult.getProperties().put("startDate", sBuffer);
		}
		
		//instrument
		sBuffer = oProperties.optString("instrument", null);
		oProperties.has("");
		// mode
		// satellite
		// size

	}
	
	protected void buildSummary(QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oResult, "DiasResponseTranslatorCREDODIAS.buildSummary: QueryResultViewModel is null");
		
		//summary
		//"summary": "Date: 2020-01-03T06:01:45.74Z, Instrument: SAR-C SAR, Mode: VV VH, Satellite: Sentinel-1, Size: 1.64 GB",
		
		
		String sDate = ""; //oResult.getProperties().get(DiasResponseTranslatorONDA.SCREATION_DATE);
		String sSummary = "Date: " + sDate + ", ";
		String sInstrument = oResult.getProperties().get("instrumentshortname");
		sSummary = sSummary + "Instrument: " + sInstrument + ", ";
		String sMode = oResult.getProperties().get("sensoroperationalmode");
		sSummary = sSummary + "Mode: " + sMode + ", ";
		//TODO infer Satellite from filename
		String sSatellite = oResult.getProperties().get("platformname");
		sSummary = sSummary + "Satellite: " + sSatellite + ", ";
		String sSize = oResult.getProperties().get("size");
		sSummary = sSummary + "Size: " + sSize;// + " " + sChosenUnit;
		oResult.setSummary(sSummary);
	}

}
