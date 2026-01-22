package wasdi.shared.queryexecutors.terrascope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class ResponseTranslatorTerrascope extends ResponseTranslator {

	//String constants to be used elsewhere
	private static final String SLINK_SEPARATOR_TERRASCOPE = ",";
	private static final String SLINK = "link";

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
			
			// P.Campanella 2022-08-29
			if (!oResult.getTitle().contains(".")) {
				if (oResult.getProperties().containsKey("format"))   {
					Object oValue = oResult.getProperties().get("format");
					if (oValue != null) {
						
						if (oValue.toString().equals("tif") && oResult.getTitle().startsWith("ESA_WorldCover_")) {
							oResult.setTitle(oResult.getTitle() + "_Map." + oValue.toString());
						}
						else {
							oResult.setTitle(oResult.getTitle() + "." + oValue.toString());
						}
					}
				}
			}

			buildLink(oResult);
			buildSummary(oResult);
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorTerrascope.translate( " +
					oInJson.toString() + ", " +
					bFullViewModel + " ): " + oE );
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

			sBuffer = oProperties.optString("date", null);
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

			parseLinks(oProperties, oResult);

			parseServices(oResult, oProperties);
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorTerrascope.parseProperties(" + oInJson.toString() + ", " + bFullViewModel + ", ... ): " + oE);
		}
	}

	private static String retrieveInstrumentName(String platformName) {
		return instrumentByPlatform.get(platformName);
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
			if (!oProperties.has("links")) {
				WasdiLog.debugLog("ResponseTranslatorTerrascope.parseProperties: warning: field \"links\" was expected but not found, here's the json:\nJSON DUMP BEGIN\n" +
						oProperties + "\nJSON DUMP END");
			}
			if (oProperties.isNull("links")) {
				WasdiLog.debugLog("ResponseTranslatorTerrascope.parseProperties: warning: field \"links\" is present but has null value, here's the json:\nJSON DUMP BEGIN\n" +
						oProperties + "\nJSON DUMP END");
			}
			//try as object
			JSONObject oLinks = oProperties.optJSONObject("links");
			if (null != oLinks) {
				if(oLinks.has(ResponseTranslatorTerrascope.SHREF) && !oLinks.isNull(ResponseTranslatorTerrascope.SHREF)) {
					oResult.getProperties().put(ResponseTranslatorTerrascope.SHREF, oLinks.optString(ResponseTranslatorTerrascope.SHREF, null));
				}
			} else {
				//try as array
				JSONArray oLinksArray = oProperties.optJSONArray("links");
				if (null != oLinksArray) {
					for (Object oObject : oLinksArray) {
						try {
							JSONObject oItem = (JSONObject) oObject;
							if (!oItem.has("rel") || oItem.isNull("rel")) {
								continue;
							}
							String sRel = oItem.optString("rel", null);
							if (Utils.isNullOrEmpty(sRel)) {
								continue;
							}
							if (!sRel.equals(ResponseTranslatorTerrascope.SSELF)) {
								//if the product is missing, this link can be used to later ask again for it, after the order has been completed
								String sHref = oItem.optString(ResponseTranslatorTerrascope.SHREF, null);
								if (null != sHref) {
									oResult.getProperties().put(ResponseTranslatorTerrascope.SSELF, sHref);
								}
							}

							//just store the "self" link
							if (oItem.has(ResponseTranslatorTerrascope.SHREF) && !oItem.isNull(ResponseTranslatorTerrascope.SHREF)) {
								oResult.getProperties().put(ResponseTranslatorTerrascope.SHREF, oItem.optString(ResponseTranslatorTerrascope.SHREF, null));
							}

						} catch (Exception oE) {
							WasdiLog.debugLog("ResponseTranslatorTerrascope.parseProperties: exception while trying to cast item object to JSONObject: " + oE);
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
		String sUrl = null;

		long lSize = 0;

		String sType = (String) JsonUtils.getProperty(oProperties.toMap(), "links.data.0.type");
		if ("application/zip".equalsIgnoreCase(sType) || "image/tiff".equalsIgnoreCase(sType)) {
			Object oLength = JsonUtils.getProperty(oProperties.toMap(), "links.data.0.length");

			if (oLength instanceof Integer) {
				lSize = ((Integer) oLength).longValue();
			} else if (oLength instanceof Long) {
				lSize = ((Long) oLength).longValue();
			}

			sUrl = (String) JsonUtils.getProperty(oProperties.toMap(), "links.data.0.href");
		} else {
			List<Map<String, Object>> aoAllLinks = extractAllLinks(oProperties);

			lSize = calculateDownloadSize(aoAllLinks);

			String sPayload = prepareLinkJsonPayload(aoAllLinks);
			String sCompressedPayload = compressPayload(sPayload);
			if (sCompressedPayload != null) {
				sUrl = "https://services.terrascope.be/package/zip" + "?compressed_payload=" + sCompressedPayload + "&size=" + lSize;
			}
		}

		oResult.getProperties().put(ResponseTranslatorTerrascope.SSIZE_IN_BYTES, "" + lSize);
		String sSize = "0";
		if (lSize > 0) {
			double dTmp = (double) lSize;
			sSize = Utils.getNormalizedSize(dTmp);
			oResult.getProperties().put(ResponseTranslatorTerrascope.SSIZE, sSize);
		}

		if (!Utils.isNullOrEmpty(sUrl)) {
			oResult.getProperties().put(ResponseTranslatorTerrascope.SURL, sUrl);
			oResult.getProperties().put("format", sUrl.substring(sUrl.lastIndexOf(".") + 1));
			oResult.setLink(sUrl);
		} 
		else {
			WasdiLog.debugLog("ResponseTranslatorTerrascope.parseServices: download link not found! dumping json:\n" + "JSON DUMP BEGIN\n" + oProperties.toString() + "JSON DUMP END");
		}
	}

	private static String compressPayload(String sPayload) {
		String sCompressedPayload = null;

		try {
			sCompressedPayload = StringUtils.compressString(sPayload);
		} catch (IOException e) {
			WasdiLog.debugLog("ResponseTranslatorTerrascope.compressPayload: the payload cannot be compressed: " + e.getMessage());
		}

		return sCompressedPayload;
	}

	private static List<Map<String, Object>> extractAllLinks(JSONObject oProperties) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> aoPreviews = (List<Map<String, Object>>) JsonUtils.getProperty(oProperties.toMap(), "links.previews");

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> aoAlternates = (List<Map<String, Object>>) JsonUtils.getProperty(oProperties.toMap(), "links.alternates");

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> aoRelated = (List<Map<String, Object>>) JsonUtils.getProperty(oProperties.toMap(), "links.related");

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> aoData = (List<Map<String, Object>>) JsonUtils.getProperty(oProperties.toMap(), "links.data");

		List<Map<String, Object>> aoAllLinks = new ArrayList<Map<String, Object>>();
		aoAllLinks.addAll(aoPreviews);
		aoAllLinks.addAll(aoAlternates);
		aoAllLinks.addAll(aoRelated);
		aoAllLinks.addAll(aoData);

		return aoAllLinks;
	}

	private static long calculateDownloadSize(List<Map<String, Object>> aoLinks) {
		long lSize = 0;

		for (Map<String, Object> aoLink : aoLinks) {
			if (aoLink != null) {
				Object oLength = aoLink.get("length");

				if (oLength instanceof Integer) {
					lSize += ((Integer) oLength).longValue();
				} else if (oLength instanceof Long) {
					lSize += ((Long) oLength).longValue();
				}

			}
		}

		return lSize;
	}

	private static String prepareLinkJsonPayload(List<Map<String, Object>> aoLinks) {
		String sPayload = "[";

		for (Map<String, Object> aoLink : aoLinks) {
			if (aoLink != null) {
				String sHref = (String) aoLink.get("href");
				sPayload += "\"" + sHref + "\"" + ",";
			}
		}

		if (sPayload.length() > 1) {
			sPayload = sPayload.substring(0, sPayload.length() - 1);
		}

		sPayload += "]";

		return sPayload;
	}

	private void buildLink(QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oResult, "result view model is null");
		try {
			StringBuilder oLink = new StringBuilder("");

			String sItem = "";

			sItem = oResult.getProperties().get(ResponseTranslatorTerrascope.SURL);
			if (sItem == null || sItem.isEmpty()) {
				WasdiLog.debugLog("ResponseTranslatorTerrascope.buildLink: the download URL is null or empty. Product title: " + oResult.getTitle());
				sItem = "http://";
			}
			oLink.append(sItem).append(ResponseTranslatorTerrascope.SLINK_SEPARATOR_TERRASCOPE); //0

			sItem = oResult.getTitle();
			if (sItem == null) {
				sItem = "";
			}
			//fix file extension
			if (!sItem.toLowerCase().endsWith(".zip") && !sItem.toLowerCase().endsWith(".tif") && !sItem.toLowerCase().endsWith(".tar.gz") || !sItem.toLowerCase().endsWith(".nc")) {
				if (sItem.toUpperCase().startsWith("S1A") || sItem.toUpperCase().startsWith("S1B") ||
						sItem.toUpperCase().startsWith("S2A") || sItem.toUpperCase().startsWith("S2B") || 
						sItem.toUpperCase().startsWith("S3A") || sItem.toUpperCase().startsWith("S3B") || sItem.toUpperCase().startsWith("S3_") ||
						sItem.toUpperCase().startsWith("S5") ||
						sItem.toUpperCase().startsWith("LC08") ||
						sItem.toUpperCase().startsWith("COP-DEM") ||
						sItem.toUpperCase().startsWith("S2GLC")
						) {
					if (sItem.endsWith(".SAFE")) {
						sItem = sItem.substring(0, sItem.length() - 5);
					}
					sItem = sItem + ".zip";
				}
			}
			oLink.append(sItem).append(ResponseTranslatorTerrascope.SLINK_SEPARATOR_TERRASCOPE); //1

			sItem = oResult.getProperties().get(ResponseTranslatorTerrascope.SSIZE_IN_BYTES);
			if (sItem == null) {
				sItem = "";
			}
			oLink.append(sItem).append(ResponseTranslatorTerrascope.SLINK_SEPARATOR_TERRASCOPE); //2

			sItem = oResult.getProperties().get("status");
			if (sItem == null) {
				sItem = "";
			}
			oLink.append(sItem).append(ResponseTranslatorTerrascope.SLINK_SEPARATOR_TERRASCOPE); //3

			sItem = oResult.getProperties().get(ResponseTranslatorTerrascope.SSELF);
			if (sItem == null) {
				sItem = "";
			}
			oLink.append(sItem).append(ResponseTranslatorTerrascope.SLINK_SEPARATOR_TERRASCOPE); //4

			sItem = oResult.getProperties().get(ResponseTranslatorTerrascope.SPRODUCTIDENTIFIER);
			if (sItem == null) {
				sItem = "";
			}
			oLink.append(sItem).append(ResponseTranslatorTerrascope.SLINK_SEPARATOR_TERRASCOPE); //5

			oResult.getProperties().put(ResponseTranslatorTerrascope.SLINK, oLink.toString());
			oResult.setLink(oLink.toString());
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorTerrascope.buildLink: could not extract download link: " + oE);
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
				iResult = oJson.optInt("totalResults", -1);
			}
		}

		return iResult;
	}

}
