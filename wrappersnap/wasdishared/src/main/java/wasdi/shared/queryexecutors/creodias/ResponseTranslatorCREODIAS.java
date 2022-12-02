/**
 * Created by Cristiano Nattero on 2019-12-23
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors.creodias;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class ResponseTranslatorCREODIAS extends ResponseTranslator {


	//String constants to be used elsewhere
	public static final String SLINK_SEPARATOR_CREODIAS = ",";
	public static final String SLINK = "link";
	public static final int IPOSITIONOF_LINK = 0;
	public static final int IPOSITIONOF_FILENAME = 1;
	public static final int IPOSITIONOF_SIZEINBYTES = 2;
	public static final int IPOSITIONOF_STATUS = 3;
	public static final int IPOSITIONOF_REL = 3;
	public static final int IPOSITIONOF_PRODUCTIDENTIFIER = 5;

	//private string constants
	private static final String SSIZE_IN_BYTES = "sizeInBytes";
	private static final String SHREF = "href";
	private static final String SURL = "url";
	private static final String SPOLARISATION = "polarisation";
	private static final String SRELATIVEORBITNUMBER = "relativeOrbitNumber";
	private static final String SSIZE = "size";
	private static final String SPLATFORM = "platform";
	private static final String SSENSOR_MODE = "sensorMode";
	private static final String SINSTRUMENT = "instrument";
	private static final String SDATE = "date";
	private static final String STYPE = "type";
	private static final String SSELF = "self";
	private static final String STITLE = "title";
	private static final String SPRODUCTIDENTIFIER = "productIdentifier";


	@Override
	public List<QueryResultViewModel> translateBatch(String sJson, boolean bFullViewModel) {
		Preconditions.checkNotNull(sJson, "ResponseTranslatorCREODIAS.translateBatch: sJson is null" );

		List<QueryResultViewModel> aoResults = new ArrayList<>();

		//isolate single entry, pass it to translate and append the result
		try {
			JSONObject oJson = new JSONObject(sJson);
			JSONArray aoFeatures = oJson.optJSONArray("features");
			for (Object oItem : aoFeatures) {
				if(null!=oItem) {
					JSONObject oJsonItem = (JSONObject)(oItem);
					QueryResultViewModel oViewModel = translate(oJsonItem, bFullViewModel);
					if(null != oViewModel) {
						aoResults.add(oViewModel);
					}
				}
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorCREODIAS.translateBatch: " + oE);
		}
		return aoResults;
	}


	public QueryResultViewModel translate(JSONObject oInJson, boolean bFullViewModel) {
		Preconditions.checkNotNull(oInJson, "ResponseTranslatorCREODIAS.translate: null json");
		QueryResultViewModel oResult = new QueryResultViewModel();
		oResult.setProvider("CREODIAS");
		try {

			parseMainInfo(oInJson, oResult);		
			parseFootPrint(oInJson, oResult);
			parseProperties(oInJson, bFullViewModel, oResult);

			buildLink(oResult);
			buildSummary(oResult);
			
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorCREODIAS.translate( " +
					oInJson.toString() + ", " +
					bFullViewModel + " ): " + oE );
		}
		return oResult;
	}


	private void parseMainInfo(JSONObject oInJson, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oInJson, "ResponseTranslatorCREODIAS.addMainInfo: input json is null");
		Preconditions.checkNotNull(oResult,"ResponseTranslatorCREODIAS.addMainInfo: QueryResultViewModel is null");
		try {
			if(!oInJson.isNull("id")) {
				oResult.setId(oInJson.optString("id", null));
			}

			if(!oInJson.isNull(ResponseTranslatorCREODIAS.STYPE)){
				oResult.getProperties().put(ResponseTranslatorCREODIAS.STYPE, oInJson.optString(ResponseTranslatorCREODIAS.STYPE, null));
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorCREODIAS.parseMainInfo( " +
					oInJson.toString() + ", ...): " + oE );
		}
	}

	/**
	 * @param oInJson
	 * @param oResult
	 */
	private void parseFootPrint(JSONObject oInJson, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oInJson, "ResponseTranslatorCREODIAS.parseFootPrint: input json is null");
		Preconditions.checkNotNull(oResult, "ResponseTranslatorCREODIAS.parseFootPrint: QueryResultViewModel is null");

		try {
			String sBuffer = null;
			JSONObject oGeometry = oInJson.optJSONObject("geometry");
			if(null!=oGeometry) {
				sBuffer = oGeometry.optString(ResponseTranslatorCREODIAS.STYPE, null);
				if(null != sBuffer) {
					StringBuilder oFootPrint = new StringBuilder(sBuffer.toUpperCase());
					
					if (sBuffer.equalsIgnoreCase("POLYGON")) {
						oFootPrint.append(" ((");
					}
					else if (sBuffer.equalsIgnoreCase("MULTIPOLYGON")) {
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
			WasdiLog.debugLog("DiasQueryTranslatorCREODIAS.parseFootPrint( " + oInJson.toString() + ", ... ): " + oE );
		}
	}


	/**
	 * @param oGeometry
	 * @param oFootPrint
	 */
	private void parseCoordinates(JSONObject oGeometry, StringBuilder oFootPrint) {
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
		
		OR:
		
		{
			"type": "Polygon",
			"coordinates": 
				[ // 0
					[ // 1
						[ 9.73751701, 17.094709176], [ 9.747688582, 17.139499888 ], [ 9.781478322, 17.288069434 ], [ 9.815301123, 17.436606421], [ 9.73751701,17.094709176]
					]
				]
			}
		 */
		JSONArray aoCoordinates = oGeometry.optJSONArray("coordinates"); //0
		if(null!=aoCoordinates) {
			aoCoordinates = aoCoordinates.optJSONArray(0); //1
			if(null!=aoCoordinates) {
				
				String sType = oGeometry.getString("type");
				
				if (sType.toUpperCase().equals("MULTIPOLYGON")) {
					aoCoordinates = aoCoordinates.optJSONArray(0); //2
					if(null!=aoCoordinates) {
						loadCoordinates(oFootPrint, aoCoordinates);
					}					
				}
				else {
					loadCoordinates(oFootPrint, aoCoordinates);// Execute with the 1-array
				}
			}
		}
	}


	/**
	 * @param oFootPrint
	 * @param aoCoordinates
	 */
	private void loadCoordinates(StringBuilder oFootPrint, JSONArray aoCoordinates) {
		for (Object oItem: aoCoordinates) {
			//instanceof returns false for null, so no need to check it
			if(oItem instanceof JSONArray) {
				JSONArray aoPoint = (JSONArray) oItem;
				Double dx = aoPoint.optDouble(0);
				Double dy = aoPoint.optDouble(1);
				if(!Double.isNaN(dx) && !Double.isNaN(dy) ){
					oFootPrint.append(dx).append(" ").append(dy).append(", ");
				}
			}
		}
	}

	private void parseProperties(JSONObject oInJson, boolean bFullViewModel, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oInJson, "DiasResponseTranslatorCREDODIAS.addProperties: input json is null");
		Preconditions.checkNotNull(oResult, "DiasResponseTranslatorCREDODIAS.addProperties: QueryResultViewModel is null");
		try {
			JSONObject oProperties = oInJson.optJSONObject("properties");
			if(null == oProperties) {
				WasdiLog.debugLog("DiasResponseTranslatorCREDODIAS.addProperties: input json has null properties");
				return;
			}

			String sBuffer = null;

			loadMostProperties(oResult, oProperties);

			//title
			oResult.setTitle("");
			if(!oProperties.isNull(ResponseTranslatorCREODIAS.STITLE)) {
				
				String sTitle = oProperties.optString(ResponseTranslatorCREODIAS.STITLE, null);
				
				// Remove .SAFE from the name returned by EODC
				if (sTitle.endsWith(".SAFE")) {
					sTitle = sTitle.replace(".SAFE", "");
				}
				
				oResult.setTitle(sTitle);
				oResult.getProperties().put(ResponseTranslatorCREODIAS.STITLE, sTitle);
			}

			//preview
			if(bFullViewModel) {
				sBuffer = oProperties.optString("quicklook", null);
				if(null!= sBuffer) {
					oResult.setPreview(sBuffer);
				}
			}

			sBuffer = oProperties.optString("startDate", null);
			if(null!=sBuffer) {
				oResult.getProperties().put(ResponseTranslatorCREODIAS.SDATE, sBuffer);
				oResult.getProperties().put("startDate", sBuffer);
				oResult.getProperties().put("beginposition", sBuffer);
			}

			if(!oProperties.isNull(ResponseTranslatorCREODIAS.SINSTRUMENT)) {
				oResult.getProperties().put(ResponseTranslatorCREODIAS.SINSTRUMENT, oProperties.optString(ResponseTranslatorCREODIAS.SINSTRUMENT, null));
				oResult.getProperties().put("instrumentshortname", oProperties.optString(ResponseTranslatorCREODIAS.SINSTRUMENT, null));
			}

			if(!oProperties.isNull(ResponseTranslatorCREODIAS.SSENSOR_MODE)) {
				oResult.getProperties().put((ResponseTranslatorCREODIAS.SSENSOR_MODE), oProperties.optString((ResponseTranslatorCREODIAS.SSENSOR_MODE), null));
				oResult.getProperties().put("sensoroperationalmode", oProperties.optString((ResponseTranslatorCREODIAS.SSENSOR_MODE), null));
			}

			if(!oProperties.isNull(ResponseTranslatorCREODIAS.SPLATFORM)) {
				oResult.getProperties().put(ResponseTranslatorCREODIAS.SPLATFORM, oProperties.optString(ResponseTranslatorCREODIAS.SPLATFORM, null));
				oResult.getProperties().put("platformname", oProperties.optString(ResponseTranslatorCREODIAS.SPLATFORM, null));
			}

			if(!oProperties.isNull(ResponseTranslatorCREODIAS.SRELATIVEORBITNUMBER)) {
				oResult.getProperties().put(ResponseTranslatorCREODIAS.SRELATIVEORBITNUMBER, oProperties.optString(ResponseTranslatorCREODIAS.SRELATIVEORBITNUMBER, null));
				oResult.getProperties().put("relativeorbitnumber", oProperties.optString(ResponseTranslatorCREODIAS.SRELATIVEORBITNUMBER, null));
			}
			if(oProperties.has("status") && !oProperties.isNull("status")) {
				oResult.getProperties().put("status", oProperties.optString("status"));
			}

			//polarisationmode
			if(!oProperties.isNull(ResponseTranslatorCREODIAS.SPOLARISATION)){
				oResult.getProperties().put("polarisationmode", oProperties.optString(ResponseTranslatorCREODIAS.SPOLARISATION, null));
			}

			parseLinks(oProperties, oResult);

			parseServices(oResult, oProperties);
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorCREODIAS.parseProperties(" + oInJson.toString() + ", " + bFullViewModel + ", ... ): " + oE);
		}
	}


	/**
	 * @param oProperties
	 * @param oResult
	 */
	private void parseLinks(JSONObject oProperties, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oProperties, "properties JSON is null");
		Preconditions.checkNotNull(oResult, "result view model is null");
		
		try {
			//links
			if(!oProperties.has("links")) {
				WasdiLog.debugLog("ResponseTranslatorCREODIAS.parseProperties: warning: field \"links\" was expected but not found, here's the json:\nJSON DUMP BEGIN\n" +
						oProperties + "\nJSON DUMP END");
			}
			if(oProperties.isNull("links")) {
				WasdiLog.debugLog("ResponseTranslatorCREODIAS.parseProperties: warning: field \"links\" is present but has null value, here's the json:\nJSON DUMP BEGIN\n" +
						oProperties + "\nJSON DUMP END");
			}
			//try as object
			JSONObject oLinks = oProperties.optJSONObject("links");
			if(null!=oLinks) {
				if(oLinks.has(ResponseTranslatorCREODIAS.SHREF) && !oLinks.isNull(ResponseTranslatorCREODIAS.SHREF)) {
					oResult.getProperties().put(ResponseTranslatorCREODIAS.SHREF, oLinks.optString(ResponseTranslatorCREODIAS.SHREF, null));
				}
			} else {
				//try as array
				JSONArray oLinksArray = oProperties.optJSONArray("links");
				if(null!=oLinksArray) {
					for (Object oObject : oLinksArray) {
						try {
							JSONObject oItem = (JSONObject) oObject;
							if(!oItem.has("rel") || oItem.isNull("rel")) {
								continue;
							}
							String sRel = oItem.optString("rel", null);
							if(Utils.isNullOrEmpty(sRel)) {
								continue;
							}
							if(!sRel.equals(ResponseTranslatorCREODIAS.SSELF)) {
								//if the product is missing, this link can be used to later ask again for it, after the order has been completed
								String sHref = oItem.optString(ResponseTranslatorCREODIAS.SHREF, null);
								if(null!=sHref) {
									oResult.getProperties().put(ResponseTranslatorCREODIAS.SSELF, sHref);
								}
							}

							//just store the "self" link
							if(oItem.has(ResponseTranslatorCREODIAS.SHREF) && !oItem.isNull(ResponseTranslatorCREODIAS.SHREF)) {
								oResult.getProperties().put(ResponseTranslatorCREODIAS.SHREF, oItem.optString(ResponseTranslatorCREODIAS.SHREF, null));
							}								

						} catch (Exception oE) {
							WasdiLog.debugLog("ResponseTranslatorCREODIAS.parseProperties: exception while trying to cast item object to JSONObject: " + oE);
						}
					}
				}
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("DiasResponseTranslator.parseLinks( " + oProperties.toString() + ", ... ): " + oE);
		}
	}


	/**
	 * @param oResult
	 * @param oProperties
	 */
	private void loadMostProperties(QueryResultViewModel oResult, JSONObject oProperties) {
		String sBuffer;
		//load most properties
		for (String sKey : oProperties.keySet()) {
			sBuffer = oProperties.optString(sKey, null);
			if(null!=sBuffer) {
				oResult.getProperties().put(sKey, sBuffer);
			}
		}
	}


	/**
	 * @param oResult
	 * @param oProperties
	 */
	private void parseServices(QueryResultViewModel oResult, JSONObject oProperties) {
		//size
		/*
		"services": {
         	"download": {
				"url": "https://zipper.creodias.eu/download/221165ce-4e4e-5b52-8c34-1af8f6b7154e",
                "mimeType": "application/unknown",
                "size": 1716760163
			}
		},
		 */
		try {
			JSONObject oTemp = oProperties.optJSONObject("services");
			if(null != oTemp) {
				//the link for download is extracted here!
				oTemp = oTemp.optJSONObject("download");
				if(null!=oTemp) {
					long lSize = oTemp.optLong(ResponseTranslatorCREODIAS.SSIZE, -1);
					oResult.getProperties().put(ResponseTranslatorCREODIAS.SSIZE_IN_BYTES, ""+lSize);
					String sSize = "0";
					if(0<=lSize) {
						double dTmp = (double) lSize;
						sSize = Utils.getNormalizedSize(dTmp);
						oResult.getProperties().put(ResponseTranslatorCREODIAS.SSIZE, sSize);
					}
					if(oTemp.has(ResponseTranslatorCREODIAS.SURL) && !oTemp.isNull(ResponseTranslatorCREODIAS.SURL)) {
						String sUrl = oTemp.optString(ResponseTranslatorCREODIAS.SURL, null);
						oResult.getProperties().put(ResponseTranslatorCREODIAS.SURL, sUrl);						
						oResult.setLink(sUrl);
					} else {
						WasdiLog.debugLog("ResponseTranslatorCREODIAS.parseServices: download link not found! dumping json:\n" +
								"JSON DUMP BEGIN\n" +
								oProperties.toString() + 
								"JSON DUMP END" );
					}
				} else {
					WasdiLog.debugLog("ResponseTranslatorCREODIAS.parseServices: download link not found! dumping json:\n" +
							"JSON DUMP BEGIN\n" +
							oProperties.toString() + 
							"JSON DUMP END" );
				}
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorCREODIAS.parseServices( ..., " + oProperties.toString() + " ):" + oE );
		}
	}

	private void buildLink(QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oResult, "result view model is null");
		try {
			StringBuilder oLink = new StringBuilder("");
			
			String sItem = "";
			
			sItem = oResult.getProperties().get(ResponseTranslatorCREODIAS.SURL);
			if(null==sItem || sItem.isEmpty()) {
				WasdiLog.debugLog("ResponseTranslatorCREODIAS.buildLink: the download URL is null or empty. Product title: " + oResult.getTitle() );
				sItem = "http://";
			} 
			oLink.append(sItem).append(ResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS); //0
		
			sItem = oResult.getTitle();
			if(null==sItem) {
				sItem = "";
			}
			//fix file extension
			if(!sItem.toLowerCase().endsWith(".zip") && !sItem.toLowerCase().endsWith(".tif") && !sItem.toLowerCase().endsWith(".tar.gz") || !sItem.toLowerCase().endsWith(".nc")) {
				if(sItem.toUpperCase().startsWith("S1A") || sItem.toUpperCase().startsWith("S1B") ||
						sItem.toUpperCase().startsWith("S2A") || sItem.toUpperCase().startsWith("S2B") || 
						sItem.toUpperCase().startsWith("S3A") || sItem.toUpperCase().startsWith("S3B") || sItem.toUpperCase().startsWith("S3_") ||
						sItem.toUpperCase().startsWith("S5") ||
						sItem.toUpperCase().startsWith("LC08") ||
						sItem.toUpperCase().startsWith("COP-DEM") ||
						sItem.toUpperCase().startsWith("S2GLC")
						) {
					if(sItem.endsWith(".SAFE")) {
						sItem = sItem.substring(0, sItem.length()-5);
					}
					sItem = sItem + ".zip";
				}
			}
			oLink.append(sItem).append(ResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS); //1
			
			
			sItem = oResult.getProperties().get(ResponseTranslatorCREODIAS.SSIZE_IN_BYTES);
			if(null==sItem) {
				sItem = "";
			} 
			oLink.append(sItem).append(ResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS); //2

			
			sItem = oResult.getProperties().get("status");
			if(null==sItem) {
				sItem = "";
			} 
			oLink.append(sItem).append(ResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS); //3
			
			
			sItem = oResult.getProperties().get(ResponseTranslatorCREODIAS.SSELF);
			if(null==sItem) {
				sItem = "";
			} 
			oLink.append(sItem).append(ResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS); //4
			
			sItem = oResult.getProperties().get(ResponseTranslatorCREODIAS.SPRODUCTIDENTIFIER);
			if(null==sItem) {
				sItem = "";
			} 
			oLink.append(sItem).append(ResponseTranslatorCREODIAS.SLINK_SEPARATOR_CREODIAS); //5

			oResult.getProperties().put(ResponseTranslatorCREODIAS.SLINK, oLink.toString());
			oResult.setLink(oLink.toString());
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorCREODIAS.buildLink: could not extract download link: " + oE);
		}
	}

	protected void buildSummary(QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oResult, "DiasResponseTranslatorCREDODIAS.buildSummary: QueryResultViewModel is null");

		//summary
		//"summary": "Date: 2020-01-03T06:01:45.74Z, Instrument: SAR-C SAR, Mode: VV VH, Satellite: Sentinel-1, Size: 1.64 GB",


		String sDate = oResult.getProperties().get(ResponseTranslatorCREODIAS.SDATE);
		String sSummary = "Date: " + sDate + ", ";
		String sInstrument = oResult.getProperties().get(ResponseTranslatorCREODIAS.SINSTRUMENT);
		sSummary = sSummary + "Instrument: " + sInstrument + ", ";

		String sMode = oResult.getProperties().get(ResponseTranslatorCREODIAS.SSENSOR_MODE);
		sSummary = sSummary + "Mode: " + sMode + ", ";
		String sSatellite = oResult.getProperties().get(ResponseTranslatorCREODIAS.SPLATFORM);
		sSummary = sSummary + "Satellite: " + sSatellite + ", ";
		String sSize = oResult.getProperties().get(ResponseTranslatorCREODIAS.SSIZE);
		sSummary = sSummary + "Size: " + sSize;// + " " + sChosenUnit;
		oResult.setSummary(sSummary);
	}


	@Override
	public int getCountResult(String sQueryResult) {
		int iResult = -1;
		
		if(!Utils.isNullOrEmpty(sQueryResult)) {
			//parse response as json
			JSONObject oJson = new JSONObject(sQueryResult);
			if(null!=oJson) {
				JSONObject oProperties = oJson.optJSONObject("properties");
				if(null!=oProperties) {
					//properties.totalResults
					iResult = oProperties.optInt("totalResults", -1);
				}
			}
		}
		
		return iResult;
	}



}
