package wasdi.shared.queryexecutors.creodias2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
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

public class ResponseTranslatorCreoDias2 extends ResponseTranslator {
	
	// OData keywords
	private static final String SODATA_ATTRIBUTES = "Attributes";
	private static final String SODATA_DATE = "OriginDate";
	private static final String SODATA_FOOTPRINT = "GeoFootprint";
	private static final String SODATA_COORDINATES = "coordinates";
	private static final String SODATA_INSTRUMENT = "instrumentShortName";
	private static final String SODATA_MODE = "operationalMode";
	private static final String SODATA_NAME = "Name";
	private static final String SODATA_PLATFORM_SERIAL_ID = "platformSerialIdentifier";
	private static final String SODATA_PLATFORM_SHORT_NAME = "platformShortName";
	private static final String SODATA_POLYGON = "Polygon";
	private static final String SODATA_PRODUCT_ID = "Id";
	private static final String SODATA_RELATIVE_ORBIT = "relativeOrbitNumber";
	private static final String SODATA_S3_PATH = "S3Path";
	public static final String  SODATA_SIZE = "ContentLength";
	private static final String SODATA_TYPE = "type";
	private static final String SODATA_VALUE = "Value";
	private static final String SODATA_BEGINNING_DATE_TIME = "beginningDateTime";
	
	private static final String SODATA_BASE_DOWNLOAD_URL = "https://zipper.creodias.eu/odata/v1/Products(";
	private static final String SODATA_END_DOWNLOAD_URL = ")/$value";
	
	// WASDI  keywords
	private static final String sDATE = "date";
	private static final String SINSTRUMENT = "instrumentshortname";
	private static final String SMULTI_POLYGON = "MultiPolygon";
	private static final String SRELATIVE_ORBIT = "relativeorbitnumber";
	private static final String SPLATFORM_NAME = "platformname";
	private static final String SPOLYGON = "POLYGON";
	private static final String SSENSOR_MODE = "sensoroperationalmode";
	private static final String SSIZE = "size";
	
	public static final String SLINK_SEPARATOR_CREODIAS2 = ",";
	public static final String SLINK_PROPERTY_CREODIAS = "link";
	public static final int IPOSITIONOF_LINK = 0;
	public static final int IPOSITIONOF_FILENAME = 1;
	public static final int IPOSITIONOF_SIZEINBYTES = 2;
	public static final int IPOSITIONOF_PRODUCTIDENTIFIER = 3;
	
	//public static final String SLINK = "link";
		
	
	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		List<QueryResultViewModel> aoResults = new LinkedList<>();
		
		if (Utils.isNullOrEmpty(sResponse)) {
			WasdiLog.debugLog("ResponseTranslatorCreoDias2.translateBatch: response string is null or empty.");
			return aoResults;
		}
		
		try {
			JSONObject oJson = new JSONObject(sResponse);
			JSONArray aoFeatures = oJson.optJSONArray("value");
			for (Object oItem : aoFeatures) {
				if(oItem != null) {
					JSONObject oJsonItem = (JSONObject)(oItem);
					aoResults.add(processProduct(oJsonItem, bFullViewModel));
				}
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("ResponseTranslatorCREODIAS.translateBatch. Exception when translating the Creodias results in the Wasdi view model ", oE);
			return Collections.emptyList();
		}
		
		return aoResults;
	}
	
	/**
	 * Method for extracting the information related to a single product returned by Creodias in Json format and saving it in a view model.
	 * @param oJsonItem the Json representation of a product, as returned by the Creodias API.
	 * @param bFullViewModel determines the amount of information to be stored in the view model. When true, the view model is populated with complete information about the product. When false only the essential information is added.
	 * @return the WASDI result view model for a product
	 */
	private QueryResultViewModel processProduct(JSONObject oJsonItem, boolean bFullViewModel) {
		// BASIC INFO: title, summary, product id, link, footprint, provider id
		String sProductTitle =  removeExtensionFromProductTitle(oJsonItem.optString(SODATA_NAME));
		String sProductId = oJsonItem.optString(SODATA_PRODUCT_ID);
		String sLink = SODATA_BASE_DOWNLOAD_URL + sProductId + SODATA_END_DOWNLOAD_URL;
		String sFootprint = parseFootPrint(oJsonItem);
		double dByteSize = (double) oJsonItem.optLong(SODATA_SIZE, -1L);
		String sSize = dByteSize > 0 ? Utils.getNormalizedSize(dByteSize) : "";
		String sS3Path = oJsonItem.optString(SODATA_S3_PATH);
		
		// some properties to add to the summary. TODO: decide which are the one we need
		JSONArray aoAttributes = oJsonItem.optJSONArray(SODATA_ATTRIBUTES);
		String sInstrument = getAttribute(aoAttributes, SODATA_INSTRUMENT);
		String sMode = getAttribute(aoAttributes, SODATA_MODE);
		String sPlatform = getAttribute(aoAttributes, SODATA_PLATFORM_SHORT_NAME);
		String sPlatformSerialId = getAttribute(aoAttributes, SODATA_PLATFORM_SERIAL_ID);
		String sRelativeOrbit = getAttribute(aoAttributes, SODATA_RELATIVE_ORBIT);
		String sBeginningDateTime = getAttribute(aoAttributes, SODATA_BEGINNING_DATE_TIME);
		
		// retrieve product date
		String sDate = !Utils.isNullOrEmpty(sBeginningDateTime) 
				? sBeginningDateTime 
				: oJsonItem.optString(SODATA_DATE);
				
		String sSummary = getSummary(sDate, sSize, sInstrument, sMode, sPlatform, sPlatformSerialId);
		
		QueryResultViewModel oResult = new QueryResultViewModel();
		setBasicInfo(oResult, sProductId, sProductTitle, sLink, sSummary, sFootprint);
		setBasicProperties(oResult, sDate, sPlatform, sPlatformSerialId, sInstrument, sMode, sSize, sRelativeOrbit);
		setAllProviderProperties(oResult, aoAttributes);
		setLink(oResult, dByteSize, sS3Path);
		
		return oResult;
	}
	
	
	/**
	 * Remove the file extension from the title of the product
	 * @param sProductTitle the title of the product
	 * @return the sama title, without the file extension at the end
	 */
	public static String removeExtensionFromProductTitle(String sProductTitle) {
		return sProductTitle.replace(".SAFE", "")	// extension for Sentinel-1 and Sentinel-2
				.replace(".SEN3", "") 				// extension for Sentinel-3
				.replace(".nc", "")					// extension for Sentine-5
				.replace(".SEN6", ""); 				// extension for Sentine-6
	}
	
	
	/**
	 * Set the minimum set of information of a product in the view model
	 * @param oViewModel the view model to be sent to the client
	 * @param sProductId the id of the product on Creodias
	 * @param sProductTitle the title of the product
	 * @param sSummary the string representing the summary of the the main properties of the product
	 * @param sFootprint the string representing the footprint of the product
	 */
	private void setBasicInfo(QueryResultViewModel oViewModel, String sProductId, String sProductTitle, String sLink, String sSummary, String sFootprint) {
		oViewModel.setProvider("CREODIAS2");
		
		if (!Utils.isNullOrEmpty(sProductId))
		oViewModel.setId(sProductId);
		
		if (!Utils.isNullOrEmpty(sProductTitle))
			oViewModel.setTitle(sProductTitle);
		
		if (!Utils.isNullOrEmpty(sLink))
			oViewModel.setLink(sLink);
		
		if (!Utils.isNullOrEmpty(sSummary))
			oViewModel.setSummary(sSummary);
		
		if (!Utils.isNullOrEmpty(sFootprint))
			oViewModel.setFootprint(sFootprint);
	}
	
	/**
	 * Set the minimum set of properties in the map of the view model
	 * @param oViewModel the view model to be sent to the client
	 * @param sDate the reference date for the product
	 * @param sPlatform the product's platform
	 * @param sPlatformSerialId the platform's serial id (e.g. A or Be, for Sentinel-1)
	 * @param sInstrument the instrument
	 * @param sMode the sensor mode
	 * @param sSize the size of the file
	 * @param sRelativeOrbit the relative orbit 
	 */
	private void setBasicProperties(QueryResultViewModel oViewModel, String sDate, String sPlatform, String sPlatformSerialId, String sInstrument, String sMode, String sSize, String sRelativeOrbit) {
		Map<String, String> oMapProperties = oViewModel.getProperties();
		
		if (!Utils.isNullOrEmpty(sDate))
			oMapProperties.put(sDATE, sDate);
		
		if (!Utils.isNullOrEmpty(sPlatform))
			oMapProperties.put(SPLATFORM_NAME, sPlatform + (!Utils.isNullOrEmpty(sPlatformSerialId) ? sPlatformSerialId : "") );
		
		if (!Utils.isNullOrEmpty(sInstrument))
			oMapProperties.put(SINSTRUMENT, sInstrument);
		
		if (!Utils.isNullOrEmpty(sMode))
			oMapProperties.put(SSENSOR_MODE, sMode);
		
		if (!Utils.isNullOrEmpty(sSize))
			oMapProperties.put(SSIZE, sSize);
		
		if (!Utils.isNullOrEmpty(sRelativeOrbit))
			oMapProperties.put(SRELATIVE_ORBIT, sRelativeOrbit);	
		
		
	}
	
	/**
	 * Creates and stores in the view model the link that the provider adapter will use to download a product from Creodias.
	 * The link is actually a string made by a concatenation of four values, separated by comma: the URL to download the file from Creodias,
	 * the name that will be used to save the downloaded copy of the file (with the zip extension), the size of the file and its S3Path on creodias. 
	 * @param oResult the view model where the link string will be stored
	 * @param sDownloadLink the URL to be used for downloading a product from Creodias
	 * @param sProductTitle the name of the product to download (without any extension)
	 * @param dSizeInBytes the size of the product 
	 * @param sS3Path the S3 path on Creodias
	 */
	private void setLink(QueryResultViewModel oResult, double dSizeInBytes, String sS3Path) {
		Preconditions.checkNotNull(oResult, "result view model is null");
		try {
			StringBuilder oLink = new StringBuilder("");
			
			
			String sLink = oResult.getLink(); 
			if(Utils.isNullOrEmpty(sLink)) {
				WasdiLog.debugLog("ResponseTranslatorCREODIAS.buildLink: the download URL is null or empty. ");
				sLink = "http://";
			} 
			oLink.append(sLink).append(SLINK_SEPARATOR_CREODIAS2); //0: on-line download link
		
			String sTitle = oResult.getTitle();  
			if(Utils.isNullOrEmpty(sTitle)) {
				WasdiLog.debugLog("ResponseTranslatorCREODIAS.buildLink: the product title is empty.");
				sTitle = "";
			}
			sTitle = sTitle + ".zip";
			oLink.append(sTitle).append(SLINK_SEPARATOR_CREODIAS2); //1: file title (zip format)
			
			
			String sSizeInBytes = ""; 
			if(dSizeInBytes > -1) {
				sSizeInBytes = Double.toString(dSizeInBytes);
			} 
			oLink.append(sSizeInBytes).append(SLINK_SEPARATOR_CREODIAS2); //2: size in bytes

			
			String sPathIdentifier = sS3Path;
			if(Utils.isNullOrEmpty(sPathIdentifier)) {
				sPathIdentifier = "";
			} 
			oLink.append(sPathIdentifier).append(SLINK_SEPARATOR_CREODIAS2); //3: path identifier

			oResult.getProperties().put(SLINK_PROPERTY_CREODIAS, oLink.toString());
			oResult.setLink(oLink.toString());
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorCREODIAS.buildLink: could not extract download link: " + oE);
		}
	}
	
	
	/**
	 * Takes all the product's attributes sent by Creodias and stores them in the view model's attribute 
	 * @param oViewModel the view model 
	 * @param aoAttributes the json array of attributes, as sent by Creodias
	 */
	private void setAllProviderProperties(QueryResultViewModel oViewModel, JSONArray aoAttributes) {
		Map<String, String> oMapProperties = oViewModel.getProperties();
		for (Object oAtt : aoAttributes) {
			JSONObject oJsonAtt = (JSONObject) oAtt;
			oMapProperties.putIfAbsent(oJsonAtt.optString(SODATA_NAME), oJsonAtt.optString(SODATA_VALUE));
		}
	}
	
	
	/**
	 * Returns the value of a given attribute from the array of attributes sent by Creodias
	 * @param aoAttributes the array of attributes
	 * @param sAttributeName the name of the attribute to look for
	 * @return the value of the attribute
	 */
	private String getAttribute(JSONArray aoAttributes, String sAttributeName) {
		for (Object oAtt : aoAttributes) {
			JSONObject oJsonAtt = (JSONObject) oAtt;
			if (oJsonAtt.get(SODATA_NAME).equals(sAttributeName))
				return oJsonAtt.optString(SODATA_VALUE);
		}
		return "";
	}
	
	/**
	 * @param sDate date of reference
	 * @param sSize size of the file
	 * @param sInstrument instrument
	 * @param sMode sensor mode
	 * @param sPlatform platform name (e.g. Sentinel-1)
	 * @param sPlatformSerialId the id of the platform (e.g. A or B for Sentinel-1)
	 * @return the string representing the summary of the main info about the product
	 */
	private String getSummary(String sDate, String sSize, String sInstrument, String sMode, String sPlatform, String sPlatformSerialId) {
		List<String> asRes = new ArrayList<>();
		if (!Utils.isNullOrEmpty(sDate))
			asRes.add("Date: " + sDate);
		if (!Utils.isNullOrEmpty(sInstrument))
			asRes.add("Instrument: " + sInstrument);
		if (!Utils.isNullOrEmpty(sMode))
			asRes.add("Mode: " + sMode);
		if (!Utils.isNullOrEmpty(sPlatform))
			asRes.add("Satellite: " + sPlatform + (!Utils.isNullOrEmpty(sPlatformSerialId) ? sPlatformSerialId : ""));
		if (!Utils.isNullOrEmpty(sSize))
			asRes.add("Size: " + sSize);
		return String.join(", ", asRes);
		
	}
	
	/**
	 * @param oJsonObject the whole Json object describing the product, as sent by Creosias
	 * @return the string representing the footprint in the WASDI format
	 */
	private String parseFootPrint(JSONObject oJsonObject) {
		try {
			JSONObject oJsonFootprint = new JSONObject(oJsonObject.optString(SODATA_FOOTPRINT));
			
			String sType = oJsonFootprint.optString(SODATA_TYPE);
			
			if (sType.equals(SODATA_POLYGON)) {
				JSONArray aoCoordinates = (JSONArray) oJsonFootprint.optJSONArray(SODATA_COORDINATES);
				String sCoordinates = parseCoordinates(aoCoordinates);
				return SPOLYGON + " ((" + sCoordinates + "))";
			}
			else if (sType.equals(SMULTI_POLYGON)) {
				// NOTE: like this we are taking only the first polygon of the multi-polygon. May be to improved in future
				JSONArray aoCoordinates = (JSONArray) oJsonFootprint.optJSONArray(SODATA_COORDINATES);
				String sCoordinates = parseCoordinates(aoCoordinates.getJSONArray(0));
				return SPOLYGON + " ((" + sCoordinates + "))";			
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ResponseTranslatorCreoDias2.parseFootPrint: error " + oEx.toString());
		}
		
		return "";	
	}
	
	/**
	 * @param aoCoordinates the json array of coordinates sent by Creodias
	 * @return the string representing the concatenation of coordinates, in the format: "X1 Y1, X2 Y2, X3 Y3"
	 */
	private String parseCoordinates(JSONArray aoCoordinates) {
		List<String> asCoordinates = new ArrayList<>();
		for (Object oItem: aoCoordinates) {
			if(oItem instanceof JSONArray) {
				for (int i = 0; i < ((JSONArray) oItem).length(); i++) {
					JSONArray oValues = ((JSONArray) oItem).getJSONArray(i);
					Double dX = oValues.getDouble(0);
					Double dY = oValues.getDouble(1);
					if(!Double.isNaN(dX) && !Double.isNaN(dY) )
						asCoordinates.add(dX + " " + dY);
				}
			}
		}
		return String.join(", ", asCoordinates);
	}
	
	

	@Override
	public int getCountResult(String sQueryResult) {
		int iCount = -1; 
		
		if (Utils.isNullOrEmpty(sQueryResult)) 
			return iCount;
		
		Map<String, Object> aoProperties = JsonUtils.jsonToMapOfObjects(sQueryResult);
		
		iCount = (Integer) aoProperties.get("@odata.count");
		
		return iCount;
	}

}