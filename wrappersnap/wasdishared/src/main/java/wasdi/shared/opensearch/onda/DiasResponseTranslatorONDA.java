/**
 * Created by Cristiano Nattero on 2018-12-11
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.onda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import wasdi.shared.opensearch.DiasResponseTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class DiasResponseTranslatorONDA extends DiasResponseTranslator {

	private static final String SFILENAME = "filename";
	private static final String SCREATION_DATE = "creationDate";
	private static final String SFOOTPRINT = "footprint";
	private static final String SFORMAT = "format";
	private static final Map<String,String> m_asOndaToSentinel;
	private static final String m_sPropertyPrefix;
	static {
		m_sPropertyPrefix = "properties."; 
		final Map<String, String> asTempMap = new HashMap<>();
		//productinfo
		asTempMap.put("quicklook", "preview");
		//don't put the next one into the properties, read it instead from the metadata and use the following as a last chance
		//asTempMap.put("name", m_sPropertyPrefix + "filename");
		//metadata
		asTempMap.put("beginPosition", m_sPropertyPrefix + "beginposition");
		asTempMap.put("instrumentShortName", m_sPropertyPrefix + "instrumentshortname");
		asTempMap.put("acquisitiontype", m_sPropertyPrefix + "acquisitiontype");
		asTempMap.put("orbitNumber", m_sPropertyPrefix + "orbitnumber");
		asTempMap.put("endPosition", m_sPropertyPrefix + "endposition");
		asTempMap.put("sensorOperationalMode", m_sPropertyPrefix + "sensoroperationalmode");
		asTempMap.put("platformName", m_sPropertyPrefix + "platformname");
		asTempMap.put("producttype", m_sPropertyPrefix + "producttype");
		asTempMap.put("lastOrbitNumber", m_sPropertyPrefix + "lastorbitnumber");
		asTempMap.put("relativeOrbitNumber", m_sPropertyPrefix + "relativeorbitnumber");
		asTempMap.put("lastRelativeOrbitNumber", m_sPropertyPrefix + "lastrelativeorbitnumber");
		asTempMap.put(DiasResponseTranslatorONDA.SFORMAT, m_sPropertyPrefix + DiasResponseTranslatorONDA.SFORMAT);
		asTempMap.put("instrumentName", m_sPropertyPrefix + "instrumentname");
		asTempMap.put(DiasResponseTranslatorONDA.SFILENAME, m_sPropertyPrefix + DiasResponseTranslatorONDA.SFILENAME);
		asTempMap.put(DiasResponseTranslatorONDA.SFOOTPRINT, m_sPropertyPrefix + DiasResponseTranslatorONDA.SFOOTPRINT);
		asTempMap.put("size", m_sPropertyPrefix + "size"); //note: expressed in Bytes, it must be converted
		asTempMap.put("timeliness", m_sPropertyPrefix + "timeliness");
		asTempMap.put("orbitDirection", m_sPropertyPrefix + "orbitdirection");
		asTempMap.put("status", m_sPropertyPrefix + "status");
		asTempMap.put("cycleNumber", m_sPropertyPrefix + "cycleNumber");

		//warning: the following are remapped by a seems-to-be-appropriate guess
		asTempMap.put("dataTakeIdentifier", m_sPropertyPrefix + "missiondatatakeid");
		asTempMap.put("platformNssdcid", m_sPropertyPrefix + "platformidentifier");
		asTempMap.put("polarisationChannels", m_sPropertyPrefix + "polarisationmode");



		m_asOndaToSentinel = Collections.unmodifiableMap(asTempMap);

		//the following are unmapped:
		//
		//  ONDA:
		//
		//  ONDA.productInfo:
		//    @odata.context
		//    @odata.mediaContentType
		//    creationDate
		//    offline
		//    pseudopath
		//
		//  ONDA.metadata:
		//    processingDate ?
		//    processingLevel ?
		//    sensorType ? 
		//    platformShortName ?
		//    phaseIdentifier ? ...maybe something about orbit or slice?
		//    swathIdentifier ?
		//    platformSerialIdentifier
		//    resolutionDetail
	}

//	/**
//	 * 
//	 */
//	public DiasResponseTranslatorONDA() {
//
//	}
	
	
	public List<QueryResultViewModel> translateBatch(String sJson, boolean bFullViewModel, String sDownloadProtocol){
		ArrayList<QueryResultViewModel> aoResult = null;
		if(sJson == null) {
			throw new NullPointerException("DiasResponseTranslatorONDA.translateBatch: sJson is null");
		}
		
		aoResult = new ArrayList<>();
		try {
			JSONObject oJsonOndaResponse = new JSONObject(sJson);
			JSONArray aoJsonArray = oJsonOndaResponse.optJSONArray("value");
			if(null!=aoJsonArray) {
				if(aoJsonArray.length()<=0) {
					Utils.debugLog("DiasResponseTranslatorONDA.buildResultViewModel: JSON string contains an empty array");
				} else {
					for (Object oObject : aoJsonArray) {
						if(null!=oObject) {
							JSONObject oOndaFullEntry = new JSONObject("{}");
							JSONObject oOndaEntry = (JSONObject)(oObject);
							if(!bFullViewModel) {
								String sQuicklook = oOndaEntry.optString("quicklook");
								if(!Utils.isNullOrEmpty(sQuicklook)) {
									oOndaEntry.put("quicklook", (String)null);
								}
							}
							String sEntryKey = "entry";
							oOndaFullEntry.put(sEntryKey, oOndaEntry);
							QueryResultViewModel oRes = translate(oOndaFullEntry, sDownloadProtocol);
							aoResult.add(oRes);
						}
					}
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("DiasResponseTranslatorONDA.buildResultViewModel: " + oE);
		}
		return aoResult;
	}


	public QueryResultViewModel translate(Object oObject, String sProtocol) {
		QueryResultViewModel oResult = null;
		try {
			JSONObject oJson = (JSONObject)oObject;
			oResult = translate(oJson, sProtocol);
		} catch (Exception oE) {
			Utils.debugLog("DiasResponseTranslatorONDA.translate( Object, " + sProtocol + " ): " + oE);
		}
		return oResult;
	}

	//TODO change the method signature to get rid of the protocol
	public QueryResultViewModel translate(JSONObject oInJson, String sProtocol) {
		if(null == oInJson) {
			throw new NullPointerException("DiasResponseTranslatorONDA.translate: null json");
		}
		if (null == sProtocol ) {
			//default protocol to https, trying to stay safe
			sProtocol = "https";
		}
		QueryResultViewModel oResult = null;
		try {			
			JSONObject oOndaJson = oInJson.optJSONObject("entry");
			if(null!=oOndaJson) {
				oOndaJson.toString();
				oResult = parseBaseData(sProtocol, oOndaJson);
				JSONArray aoMetadata = oOndaJson.optJSONArray("Metadata");
				if(null!=aoMetadata) {
					parseMetadataArray(oResult, aoMetadata);
				}
				finalizeViewModel(oResult);
			}
		} catch (Exception oE) {
			Utils.debugLog("DiasResponseTranslatorONDA.translate( JSONObject, " + sProtocol + " ): " + oE);
		}
		
		return oResult;
	}



	protected QueryResultViewModel parseBaseData(String sInProtocol, JSONObject oJsonOndaResult) {
		QueryResultViewModel oResult;
		oResult = new QueryResultViewModel();
		oResult.setProvider("ONDA");
		String sFootprint = oJsonOndaResult.optString(DiasResponseTranslatorONDA.SFOOTPRINT,"");
		oResult.setFootprint( sFootprint );
		String sProductId = oJsonOndaResult.optString("id","");
		String sLink = "";
		String sProtocol = sInProtocol;
		if(!Utils.isNullOrEmpty(sProductId)) {
			oResult.setId(sProductId);
			//TODO use m_sLinkPrefix and m_sLinkSuffix instead
			//tentative download
			sLink += sProtocol;
			if(!sProtocol.endsWith("//")) {
				sLink += "//";
			}
			sLink += "catalogue.onda-dias.eu/dias-catalogue/Products";
			sLink += "(";
			sLink += sProductId;
			sLink += ")";
			sLink += "/$value";
			oResult.getProperties().put("link", sLink);
		} else {
			//NOTE this should not happen. Is it possible to take countermeasures?
			Utils.debugLog("DiasResponseTranslatorONDA.translate: WARNING: sProductId is null");
		}

		String sPreview = oJsonOndaResult.optString("quicklook",(String)null);
		if(null!= sPreview ) {
			oResult.setPreview("data:image/png;base64,"+ sPreview);
		}

		String sProductFileFormat = oJsonOndaResult.optString("@odata.mediaContentType");
		if(null!=sProductFileFormat) {
			oResult.getProperties().put(DiasResponseTranslatorONDA.SFORMAT, sProductFileFormat );
		}

		String sProductFileName = oJsonOndaResult.optString("name");
		if(!Utils.isNullOrEmpty(sProductFileName)) {
			oResult.getProperties().put(DiasResponseTranslatorONDA.SFILENAME, sProductFileName);

			String sPath = "";
			String sPseudopath = oJsonOndaResult.optString("pseudopath");
			if(!Utils.isNullOrEmpty(sPseudopath) ) {
				oResult.getProperties().put("pseudopath", sPseudopath);
				sPath = "file:/mnt/";
				String[] sIntermediate = sPseudopath.split(", ");
				//MAYBE change the Launcher so that all pseudopaths can be passed (maybe iterate through them...)
				sPath += sIntermediate[0]; 
				sPath += "/" + sProductFileName + "/.value";

				//this hack is needed because ONDA serves ENVISAT images from file system only
				if(sProductFileName.startsWith("EN1")) {
					oResult.getProperties().put("link", sPath);
					sProtocol = "file";
				}
			} else {
				//NOTE this should not happen. Is it possible to take countermeasures?
				Utils.debugLog("DiasResponseTranslatorONDA.translate: WARNING: sPseudopath is null");
			}

			//TODO change how the launcher works so that alternatives can be used
			if(sProtocol.startsWith("file")){
				oResult.setLink(sPath);
			} else if(sProtocol.startsWith("http")){
				oResult.setLink(sLink);
			}
		} else {
			//NOTE this should not happen. Is it possible to take countermeasures?
			Utils.debugLog("DiasResponseTranslatorONDA.translate: WARNING: sProductFileName is null");
		}

		Long lSize = oJsonOndaResult.optLong("size");
		setSize(oResult, lSize);

		//ONDA
		String sCreationDate = oJsonOndaResult.optString(DiasResponseTranslatorONDA.SCREATION_DATE);
		if(null!= sCreationDate) {
			oResult.getProperties().put(DiasResponseTranslatorONDA.SCREATION_DATE, sCreationDate);
		}
		String sOffline = oJsonOndaResult.optString("offline");
		if(sOffline != null) {
			oResult.getProperties().put("offline", sOffline);
		}
		String sDownloadable = oJsonOndaResult.optString("downloadable");
		if(null!=sDownloadable) {
			oResult.getProperties().put("downloadable", sDownloadable);
		}
		return oResult;
	}

	//XXX move this method to some shared class
	private String pruneFileExtension(String sFileName) {
		String sResult = sFileName;
		if(sResult.contains(".")) {
			sResult = sFileName.substring(0, sFileName.lastIndexOf('.')); //eliminate the file extension .ZIP, .SAFE...
		}
		//now handle cases like: "*.tar.gz", "*.tar.bz" 
		if(sResult.endsWith(".tar")) {
			sResult = pruneFileExtension(sResult);
		}
		return sResult;
	}

	//XXX move this method to some shared class
	protected String getNormalizedSize(Long lSize) {
		Double dSize = -1.0;
		String sSize = null;
		if(null != lSize) {
			dSize = (double)lSize;
		}
		sSize = Utils.getNormalizedSize(dSize);
		return sSize;
	}


	//XXX move this method to some shared class
	protected void setSize(QueryResultViewModel oResult, Long lSize) {
		if(null!=oResult) {
			String sSize = getNormalizedSize(lSize);
			oResult.getProperties().put("size", sSize);
		}
	}


	private void finalizeViewModel(QueryResultViewModel oResult) {
		setTitle(oResult);
		buildSummary(oResult);

	}


	protected void setTitle(QueryResultViewModel oResult) {

		String sTitle = oResult.getProperties().get(DiasResponseTranslatorONDA.SFILENAME);
		if(null == sTitle) {
			sTitle = oResult.getProperties().get("name");
		}
		if(null!=sTitle) {
			sTitle = pruneFileExtension(sTitle);
			oResult.setTitle(sTitle);
		}
	}



	protected void parseMetadata(QueryResultViewModel oResult, JSONObject oEntireMetadata) {
		if(null==oEntireMetadata) {
			Utils.debugLog("DiasResponseTranslator.parseMetadata: oMetadata is null");
			return;
		}
		if(null== oResult ) {
			Utils.debugLog("DiasResponseTranslator.parseMetadata: oResult is null");
			//XXX should we throw an exception instead?
			return;
		}

		String sContextKey = "@odata.context";
		String sOdataContext = oEntireMetadata.optString(sContextKey);
		if(null!=sOdataContext) {
			oResult.getProperties().put(sContextKey, sOdataContext);
		}
		JSONArray aoMetadataArray = oEntireMetadata.optJSONArray("value");

		parseMetadataArray(oResult, aoMetadataArray);

	}


	protected void parseMetadataArray(QueryResultViewModel oResult, JSONArray aoMetadataArray) {
		for (Object oObject : aoMetadataArray) {
			if(null!=oObject) {
				JSONObject oMetadataSingleEntry = (JSONObject)oObject;
				String sMetaKey = oMetadataSingleEntry.optString("id");
				if(null!= sMetaKey) {
					String sMetaValue = null;
					sMetaValue = oMetadataSingleEntry.optString("value");
					if(null != sMetaValue) {
						String sKey = null;
						sKey = m_asOndaToSentinel.get(sMetaKey);
						if(null == sKey) {
//							String sPrevious = oResult.getProperties().get(sMetaKey);
//							if(null!=sPrevious) {
//								Utils.debugLog("DiasResponseTranslatorONDA.parseMetadataArray: sMetaKey : "+sPrevious);
//							}
							oResult.getProperties().put(sMetaKey, sMetaValue);
						} else if( sKey.startsWith(m_sPropertyPrefix) ) {
							String sTmpKey = sKey.substring( sKey.indexOf(m_sPropertyPrefix) + m_sPropertyPrefix.length() );
							String sPrevious = oResult.getProperties().get(sTmpKey);
							if(null!=sPrevious) {
								//Utils.debugLog("DiasResponseTranslatorONDA.parseMetadataArray: sKey : "+sPrevious);
								if(sTmpKey.equals("size")) {
									sMetaValue = Utils.getNormalizedSize(Double.parseDouble(sMetaValue));
								}
							}
							oResult.getProperties().put(sTmpKey, sMetaValue);
						} else {
							Utils.debugLog("DiasResponseTranslatorONDA.parseMetadata: hit a viewmodel field: " + sKey + " = " + sMetaValue);
							switch(sKey) {
							case "preview":
								oResult.setPreview(sMetaValue);
								break;
							case "title":
								oResult.setTitle(sMetaValue);
								break;
							case "summary":
								oResult.setSummary(sMetaValue);
								break;
							case "id":
								oResult.setId(sMetaValue);
								break;
							case "link":
								oResult.setLink(sMetaValue);
								break;
							case DiasResponseTranslatorONDA.SFOOTPRINT:
								oResult.setLink(sMetaValue);
								break;
							case "provider":
								oResult.setProvider(sMetaValue);
								break;
							default:
								Utils.debugLog("DiasResponseTranslatorONDA.parseMetadata: unknown viewmodel field");
							}
						}
					}
				}
			}
		}
	}

	protected void buildSummary(QueryResultViewModel oResult) {
		String sDate = oResult.getProperties().get(DiasResponseTranslatorONDA.SCREATION_DATE);
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
