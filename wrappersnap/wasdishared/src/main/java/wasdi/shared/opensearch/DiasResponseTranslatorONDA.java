/**
 * Created by Cristiano Nattero on 2018-12-11
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class DiasResponseTranslatorONDA implements DiasResponseTranslator {

	private static final Map<String,String> m_asOndaToSentinel;
	private static final String m_sPropertyPrefix;
	private static final String m_sLinkPrefix;
	private static final String m_sLinkSuffix;

	static {
		m_sPropertyPrefix = "properties."; 
		//https://catalogue.onda-dias.eu/dias-catalogue/Products(6374d3e9-8318-4bf6-aab6-1a2360b1b32f)/$value
		m_sLinkPrefix = "https://catalogue.onda-dias.eu/dias-catalogue/Products(";
		m_sLinkSuffix = ")/$value";

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
		asTempMap.put("format", m_sPropertyPrefix + "format");
		asTempMap.put("instrumentName", m_sPropertyPrefix + "instrumentname");
		asTempMap.put("filename", m_sPropertyPrefix + "filename");
		asTempMap.put("footprint", m_sPropertyPrefix + "footprint");
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

	/**
	 * 
	 */
	public DiasResponseTranslatorONDA() {

	}


	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasResponseTranslator#translate(java.lang.Object)
	 */
	@Override
	public QueryResultViewModel translate(Object oObject, String sProtocol) {
		QueryResultViewModel oResult = null;
		try {
			JSONObject oJson = (JSONObject)oObject;
			oResult = translate(oJson, sProtocol);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return oResult;
	}

	//TODO change the method signature to get rid of the protocol
	public QueryResultViewModel translate(JSONObject oInJson, String sProtocol) {
		QueryResultViewModel oResult = null;
		try {			
			String sInJson = oInJson.toString();
			System.out.println(sInJson);
			JSONObject oOndaJson = oInJson.optJSONObject("entry");
			if(null!=oOndaJson) {
				String sJson = oOndaJson.toString();
				System.out.println(sJson);
				oResult = parseBaseData(sProtocol, oOndaJson);
			}
			JSONArray aoMetadata = oOndaJson.optJSONArray("Metadata");
			if(null!=aoMetadata) {
				parseMetadataArray(oResult, aoMetadata);
			}
			
			/*
			JSONObject oMetadata = 
			Object oMetadataObject = oOndaJson.opt("Metadata");
			if(null!=oMetadataObject) {
				JSONObject oMetadata = (JSONObject)(oMetadataObject);
				parseMetadata(oResult, oMetadata);
			}
			*/
			finalizeViewModel(oResult);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return oResult;
	}


	protected QueryResultViewModel parseBaseData(String sProtocol, JSONObject oJsonOndaResult) {
		QueryResultViewModel oResult;
		oResult = new QueryResultViewModel();
		oResult.setProvider("ONDA");
		String sFootprint = oJsonOndaResult.optString("footprint","");
		oResult.setFootprint( sFootprint );
		String sProductId = oJsonOndaResult.optString("id","");
		String sLink = "";
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
			System.out.println("DiasResponseTranslatorONDA.translate: WARNING: sProductId is null");
		}

		String sPreview = oJsonOndaResult.optString("quicklook",(String)null);
		if(null!= sPreview ) {
			oResult.setPreview("data:image/png;base64,"+ sPreview);
		}

		String sProductFileFormat = oJsonOndaResult.optString("@odata.mediaContentType");
		if(null!=sProductFileFormat) {
			oResult.getProperties().put("format", sProductFileFormat );
		}

		String sProductFileName = oJsonOndaResult.optString("name");
		if(!Utils.isNullOrEmpty(sProductFileName)) {
			oResult.getProperties().put("filename", sProductFileName);

			String sPath = "";
			String sPseudopath = oJsonOndaResult.optString("pseudopath");
			if(!Utils.isNullOrEmpty(sPseudopath) ) {
				oResult.getProperties().put("pseudopath", sPseudopath);
				sPath = "file:/mnt/";
				String[] sIntermediate = sPseudopath.split(", ");
				//MAYBE change the Launcher so that all pseudopaths can be passed (maybe iterate through them...)
				sPath += sIntermediate[0]; 
				sPath += "/" + sProductFileName + "/.value";
			} else {
				//NOTE this should not happen. Is it possible to take countermeasures?
				System.out.println("DiasResponseTranslatorONDA.translate: WARNING: sPseudopath is null");
			}

			//TODO change how the launcher works so that alternatives can be used
			if(sProtocol.startsWith("file")){
				oResult.setLink(sPath);
			} else if(sProtocol.startsWith("http")){
				oResult.setLink(sLink);
			}
		} else {
			//NOTE this should not happen. Is it possible to take countermeasures?
			System.out.println("DiasResponseTranslatorONDA.translate: WARNING: sProductFileName is null");
		}

		Long lSize = oJsonOndaResult.optLong("size");
		setSize(oResult, lSize);

		//ONDA
		String sCreationDate = oJsonOndaResult.optString("creationDate");
		if(null!= sCreationDate) {
			oResult.getProperties().put("creationDate", sCreationDate);
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
			sResult = sFileName.substring(0, sFileName.lastIndexOf(".")); //eliminate the file extension .ZIP, .SAFE...
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
		String sChosenUnit = "ZZ";
		String sSize = null;
		if(null != lSize) {
			dSize = (double)lSize;
		}
		sSize = getNormalizedSize(dSize);
		return sSize;
	}

	//XXX move this method to some shared class
	protected String getNormalizedSize(Double dSize) {
		String sChosenUnit = null;
		String sSize = null;
		String[] sUnits = {"B", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB", "BB"}; //...yeah, ready for the decades to come :-O 
		int iUnitIndex = 0;
		int iLim = sUnits.length -1;
		while(iUnitIndex < iLim && dSize >= 1024) {
			dSize = dSize / 1024;
			iUnitIndex++;

			//now, round it to two decimal digits
			dSize = Math.round(dSize*100)/100.0; 
			sChosenUnit = sUnits[iUnitIndex];
			sSize = String.valueOf(dSize) + " " + sChosenUnit;
		}
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

		String sTitle = oResult.getProperties().get("filename");
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
			System.out.println("DiasResponseTranslator.parseMetadata: oMetadata is null");
			return;
		}
		if(null== oResult ) {
			System.out.println("DiasResponseTranslator.parseMetadata: oResult is null");
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
							String sPrevious = oResult.getProperties().get(sMetaKey);
							if(null!=sPrevious) {
								System.out.println("DiasResponseTranslatorONDA.parseMetadata: sMetaKey : "+sPrevious);
							}
							oResult.getProperties().put(sMetaKey, sMetaValue);
						} else if( sKey.startsWith(m_sPropertyPrefix) ) {
							String sTmpKey = sKey.substring( sKey.indexOf(m_sPropertyPrefix) + m_sPropertyPrefix.length() );
							String sPrevious = oResult.getProperties().get(sTmpKey);
							if(null!=sPrevious) {
								System.out.println("DiasResponseTranslatorONDA.parseMetadata: sKey : "+sPrevious);
								if(sTmpKey.equals("size")) {
									sMetaValue = getNormalizedSize(Double.parseDouble(sMetaValue));
								}
							}
							oResult.getProperties().put(sTmpKey, sMetaValue);
						} else {
							System.out.println("DiasResponseTranslatorONDA.parseMetadata: hit a viewmodel field: " + sKey + " = " + sMetaValue);
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
							case "footprint":
								oResult.setLink(sMetaValue);
								break;
							case "provider":
								oResult.setProvider(sMetaValue);
								break;
							default:
								System.out.println("DiasResponseTranslatorONDA.parseMetadata: unknown viewmodel field");
							}
						}
					}
				}
			}
		}
	}

	private void buildSummary(QueryResultViewModel oResult) {
		String sDate = oResult.getProperties().get("creationDate");
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
