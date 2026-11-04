package wasdi.shared.queryexecutors.terrascope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class ResponseTranslatorTerrascope extends ResponseTranslator {

	private static final String SPOLARISATION = "polarisation";
	private static final String SRELATIVEORBITNUMBER = "relativeOrbitNumber";
	private static final String SSIZE = "size";
	private static final String SPLATFORM = "platform";
	private static final String SSENSOR_MODE = "sensorMode";
	private static final String SINSTRUMENT = "instrument";
	private static final String SDATE = "date";
	private static final String STYPE = "type";
	private static final String STITLE = "title";

	private static Map<String, String> instrumentByPlatform;

	static {
		instrumentByPlatform = new HashMap<>();

		instrumentByPlatform.put("Sentinel-1", "C-SAR");
		instrumentByPlatform.put("SENTINEL-1", "C-SAR");
		instrumentByPlatform.put("Sentinel-2", "MSI");
		instrumentByPlatform.put("SENTINEL-2", "MSI");
		instrumentByPlatform.put("TSX-1", "SAR");
		instrumentByPlatform.put("TDX-1", "SAR");
		instrumentByPlatform.put("Sentinel-5P", "TROPOMI");
		instrumentByPlatform.put("SENTINEL-5P", "TROPOMI");
		instrumentByPlatform.put("S5P", "TROPOMI");
		instrumentByPlatform.put("PROBA-V", "VGT");
		instrumentByPlatform.put("SPOT-VGT", "VGT");
		instrumentByPlatform.put("SPOT-4", "VEGETATION-1");
		instrumentByPlatform.put("SPOT-5", "VEGETATION-2");
		instrumentByPlatform.put("Copernicus Land Monitoring Service", "PROBA-V Sentinel-3 OLCI");
		instrumentByPlatform.put("CLMS", "PROBA-V Sentinel-3 OLCI");
		instrumentByPlatform.put("Copernicus Digital Elevation Model (DEM)", "TanDEM-X");
		instrumentByPlatform.put("DEM", "TanDEM-X");

		instrumentByPlatform.put(null, "");
	}


	@Override
	public List<QueryResultViewModel> translateBatch(String sJson, boolean bFullViewModel) {
		Preconditions.checkNotNull(sJson, "ResponseTranslatorTerrascope.translateBatch: sJson is null" );

		List<QueryResultViewModel> aoResults = new ArrayList<>();

		//isolate single entry, pass it to translate and append the result
		try {
			JSONObject oJson = new JSONObject(sJson);
			JSONArray aoFeatures = oJson.optJSONArray("features");
			for (Object oItem : aoFeatures) {
				if (null != oItem) {
					JSONObject oJsonItem = (JSONObject) oItem;
					QueryResultViewModel oViewModel = translate(oJsonItem, bFullViewModel);
					if (null != oViewModel) {
						aoResults.add(oViewModel);
					}
				}
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorTerrascope.translateBatch: " + oE);
		}
		return aoResults;
	}

	public QueryResultViewModel translate(JSONObject oInJson, boolean bFullViewModel) {
		Preconditions.checkNotNull(oInJson, "ResponseTranslatorTerrascope.translate: null json");

		QueryResultViewModel oResult = new QueryResultViewModel();
		oResult.setProvider("TERRASCOPE");

		try {
			parseMainInfo(oInJson, oResult);		
			parseFootPrint(oInJson, oResult);
			parseProperties(oInJson, bFullViewModel, oResult);
						
			JSONObject oAssets = oInJson.optJSONObject("assets");
			if (oAssets == null ) {
				WasdiLog.debugLog("ResponseTranslatorTerrascope.addProperties: input json has null assets");
				return oResult;
			}
			
			JSONObject oMap = oAssets.optJSONObject("Map");
			if (oMap == null) {
				WasdiLog.debugLog("ResponseTranslatorTerrascope.addProperties: input asset has not map. Trying with 'DEM'");
				oMap = oAssets.optJSONObject("DEM");
				if (oMap == null) {
					WasdiLog.debugLog("ResponseTranslatorTerrascope.addProperties: input asset has not dem");
					return oResult;
				}
			}
			
			
			String sLink = oMap.optString("href");
			oResult.setLink(sLink);
			
			// P.Campanella 2026-11-04
			if (!oResult.getTitle().contains(".")) {
				
				String sType = oMap.optString("type");
				
				if (!Utils.isNullOrEmpty(sType)) {
					if (sType.contains("tiff")) {
						oResult.setTitle(oResult.getTitle()+ ".tif"); 
					}
					else if (sType.contains("zip")) {
						oResult.setTitle(oResult.getTitle()+ ".zip");
					}
				}
				
				// Back up option: we try a tif...
				if (!oResult.getTitle().contains(".")) oResult.setTitle(oResult.getTitle()+ ".tif");				
			}
			
			buildSummary(oResult);
		} 
		catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorTerrascope.translate( " + oInJson.toString() + ", " +bFullViewModel + " ): " + oE );
		}

		return oResult;
	}

	private void parseMainInfo(JSONObject oInJson, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oInJson, "ResponseTranslatorTerrascope.addMainInfo: input json is null");
		Preconditions.checkNotNull(oResult,"ResponseTranslatorTerrascope.addMainInfo: QueryResultViewModel is null");

		try {
			if(!oInJson.isNull("id")) {
				oResult.setId(oInJson.optString("id", null));
			}

			if(!oInJson.isNull(ResponseTranslatorTerrascope.STYPE)){
				oResult.getProperties().put(ResponseTranslatorTerrascope.STYPE, oInJson.optString(ResponseTranslatorTerrascope.STYPE, null));
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorTerrascope.parseMainInfo( " +
					oInJson.toString() + ", ...): " + oE );
		}
	}

	/**
	 * @param oInJson
	 * @param oResult
	 */
	private void parseFootPrint(JSONObject oInJson, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oInJson, "ResponseTranslatorTerrascope.parseFootPrint: input json is null");
		Preconditions.checkNotNull(oResult, "ResponseTranslatorTerrascope.parseFootPrint: QueryResultViewModel is null");

		try {
			String sBuffer = null;
			JSONObject oGeometry = oInJson.optJSONObject("geometry");
			if (null != oGeometry) {
				sBuffer = oGeometry.optString(ResponseTranslatorTerrascope.STYPE, null);
				if (null != sBuffer) {
					StringBuilder oFootPrint = new StringBuilder(sBuffer.toUpperCase());

					if (sBuffer.equalsIgnoreCase("POLYGON")) {
						oFootPrint.append(" ((");
					} else if (sBuffer.equalsIgnoreCase("MULTIPOLYGON")) {
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
			WasdiLog.debugLog("ResponseTranslatorTerrascope.parseFootPrint( " + oInJson.toString() + ", ... ): " + oE );
		}
	}

	/**
	 * @param oGeometry
	 * @param oFootPrint
	 */
	private void parseCoordinates(JSONObject oGeometry, StringBuilder oFootPrint) {
		JSONArray aoCoordinates = oGeometry.optJSONArray("coordinates"); //0
		if (null != aoCoordinates) {
			aoCoordinates = aoCoordinates.optJSONArray(0); //1

			if (null != aoCoordinates) {
				String sType = oGeometry.getString("type");

				if (sType.toUpperCase().equals("MULTIPOLYGON")) {
					aoCoordinates = aoCoordinates.optJSONArray(0); //2

					if (null != aoCoordinates) {
						loadCoordinates(oFootPrint, aoCoordinates);
					}					
				} else {
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
			if (oItem instanceof JSONArray) {
				JSONArray aoPoint = (JSONArray) oItem;
				Double dx = aoPoint.optDouble(0);
				Double dy = aoPoint.optDouble(1);
				if (!Double.isNaN(dx) && !Double.isNaN(dy)) {
					oFootPrint.append(dx).append(" ").append(dy).append(", ");
				}
			}
		}
	}

	private void parseProperties(JSONObject oInJson, boolean bFullViewModel, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oInJson, "ResponseTranslatorTerrascope.addProperties: input json is null");
		Preconditions.checkNotNull(oResult, "ResponseTranslatorTerrascope.addProperties: QueryResultViewModel is null");

		try {
			JSONObject oProperties = oInJson.optJSONObject("properties");
			if (null == oProperties) {
				WasdiLog.debugLog("ResponseTranslatorTerrascope.addProperties: input json has null properties");
				return;
			}
			
			String sBuffer = null;

			loadMostProperties(oResult, oProperties);
			
			//title
			oResult.setTitle("");
			if (!oProperties.isNull(ResponseTranslatorTerrascope.STITLE)) {
				String sTitle = oProperties.optString(ResponseTranslatorTerrascope.STITLE, null);

				// Remove .SAFE from the name returned by EODC
				if (sTitle.endsWith(".SAFE")) {
					sTitle = sTitle.replace(".SAFE", "");
				}

				oResult.setTitle(sTitle);
				oResult.getProperties().put(ResponseTranslatorTerrascope.STITLE, sTitle);
			}
			

			//preview
			if (bFullViewModel) {

				String sPreviewCategory = (String) JsonUtils.getProperty(oProperties.toMap(), "links.previews.0.category");
				if ("QUICKLOOK".equalsIgnoreCase(sPreviewCategory)) {
					String sPreviewHref = (String) JsonUtils.getProperty(oProperties.toMap(), "links.previews.0.href");

					if (sPreviewHref == null) {
						oResult.setPreview(null);
					} else if (sPreviewHref.toLowerCase().endsWith(".jp2")) {
						oResult.setPreview(null);
					} else {
						oResult.setPreview(sPreviewHref);
					}
				}

			}

			sBuffer = oProperties.optString("datetime", null);
			if (null != sBuffer) {
				oResult.getProperties().put(ResponseTranslatorTerrascope.SDATE, sBuffer);
				oResult.getProperties().put("startDate", sBuffer);
				oResult.getProperties().put("beginposition", sBuffer);
			}

			String sSensoroperationalmode = (String) JsonUtils.getProperty(oProperties.toMap(), "acquisitionInformation.1.acquisitionParameters.operationalMode");
			oResult.getProperties().put((ResponseTranslatorTerrascope.SSENSOR_MODE), sSensoroperationalmode);
			oResult.getProperties().put("sensoroperationalmode", sSensoroperationalmode);

			String sPlatformname = (String) JsonUtils.getProperty(oProperties.toMap(), "acquisitionInformation.0.platform.platformSerialIdentifier");
			oResult.getProperties().put(ResponseTranslatorTerrascope.SPLATFORM, sPlatformname);
			oResult.getProperties().put("platformname", sPlatformname);

			String sPlatformShortName = (String) JsonUtils.getProperty(oProperties.toMap(), "acquisitionInformation.0.platform.platformShortName");
			String sInstrumentName = retrieveInstrumentName(sPlatformShortName);
			oResult.getProperties().put(ResponseTranslatorTerrascope.SINSTRUMENT, sInstrumentName);
			oResult.getProperties().put("instrumentshortname", sInstrumentName);

			Integer iRelativeorbitnumber = (Integer) JsonUtils.getProperty(oProperties.toMap(), "acquisitionInformation.1.acquisitionParameters.relativeOrbitNumber");
			if (iRelativeorbitnumber != null) {
				oResult.getProperties().put(ResponseTranslatorTerrascope.SRELATIVEORBITNUMBER, iRelativeorbitnumber.toString());
				oResult.getProperties().put("relativeorbitnumber", iRelativeorbitnumber.toString());
			}

			if(oProperties.has("status") && !oProperties.isNull("status")) {
				oResult.getProperties().put("status", oProperties.optString("status"));
			}

			//polarisationmode
			if(!oProperties.isNull(ResponseTranslatorTerrascope.SPOLARISATION)){
				oResult.getProperties().put("polarisationmode", oProperties.optString(ResponseTranslatorTerrascope.SPOLARISATION, null));
			}

			//parseLinks(oProperties, oResult);
			//parseServices(oResult, oProperties);
			
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorTerrascope.parseProperties(" + oInJson.toString() + ", " + bFullViewModel + ", ... ): " + oE);
		}
	}

	private static String retrieveInstrumentName(String platformName) {
		return instrumentByPlatform.get(platformName);
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

	protected void buildSummary(QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oResult, "ResponseTranslatorTerrascope.buildSummary: QueryResultViewModel is null");

		String sDate = oResult.getProperties().get(ResponseTranslatorTerrascope.SDATE);
		String sSummary = "Date: " + sDate + ", ";
		String sInstrument = oResult.getProperties().get(ResponseTranslatorTerrascope.SINSTRUMENT);
		sSummary = sSummary + "Instrument: " + sInstrument + ", ";

		String sMode = oResult.getProperties().get(ResponseTranslatorTerrascope.SSENSOR_MODE);
		sSummary = sSummary + "Mode: " + sMode + ", ";
		String sSatellite = oResult.getProperties().get(ResponseTranslatorTerrascope.SPLATFORM);
		sSummary = sSummary + "Satellite: " + sSatellite + ", ";
		String sSize = oResult.getProperties().get(ResponseTranslatorTerrascope.SSIZE);
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
				iResult = oJson.optInt("numberMatched", -1);
			}
		}

		return iResult;
	}

}
