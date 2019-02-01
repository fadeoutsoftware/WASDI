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

import com.google.gson.JsonObject;

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
		asTempMap.put("instrumentShortName", m_sPropertyPrefix + "instrumentshortName");
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
		asTempMap.put("creationDate", m_sPropertyPrefix + "ingestiondate");
		 
		 

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
		try {
			JSONObject oJson = (JSONObject)oObject;
			return translate(oJson, sProtocol);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	//TODO change the method signature to get rid of the protocol
	public QueryResultViewModel translate(JSONObject oInJsonOndaResult, String sProtocol) {
		QueryResultViewModel oResult = null;
		try {			
			JSONObject oJsonOndaResult = oInJsonOndaResult.optJSONObject("entry");
			if(null!=oJsonOndaResult) {
				oResult = new QueryResultViewModel();
				oResult.setProvider("ONDA");
				String sFootprint = oJsonOndaResult.optString("footprint","");
				oResult.setFootprint( sFootprint );
				String sId = oJsonOndaResult.optString("id","");
				oResult.setId(sId);

				String sPreview = oJsonOndaResult.optString("quicklook","");
				oResult.setPreview("data:image/png;base64,"+ sPreview);

				//mapped
				String sFormat = oJsonOndaResult.optString("@odata.mediaContentType");
				if(null!=sFormat) {
					oResult.getProperties().put("format", sFormat );
				}
				String sName = oJsonOndaResult.optString("name");
				if(!Utils.isNullOrEmpty(sName)) {
					oResult.getProperties().put("filename", sName);
					String sPath = "";
					String sPseudopath = oJsonOndaResult.optString("pseudopath");

					//do this anyway, an appropriate (or empty) pseudopath can always be reconstructed
					if(!Utils.isNullOrEmpty(sPseudopath) && !Utils.isNullOrEmpty(sName)) {
						oResult.getProperties().put("pseudopath", sPseudopath);
						//TODO change the Launcher so that all pseudopaths can be passed (maybe iterate through them...)
						sPath = "file:/mnt/";
						String[] sIntermediate = sPseudopath.split(", ");
						sPath += sIntermediate[0]; 
						sPath += "/" + sName + "/.value";
					} else {
						//FIXME this should not happen. Is it possible to take countermeasures?
						return null;
					}
					//do this anyway, an appropriate link can always be reconstructed
					String sLink = "";
					if(!Utils.isNullOrEmpty(sId)) {
						//TODO use m_sLinkPrefix and m_sLinkSuffix instead
						//tentative download
						sLink += sProtocol;
						if(!sProtocol.endsWith("//")) {
							sLink += "//";
						}
						sLink += "catalogue.onda-dias.eu/dias-catalogue/Products";
						sLink += "(";
						sLink += sId;
						sLink += ")";
						sLink += "/$value";
						oResult.getProperties().put("link", sLink);
						} else {
							//FIXME this should not happen. Is it possible to take countermeasures?
							return null;
						}
					//TODO change this choice so that both alternatives are passed to the launcher
					if(sProtocol.startsWith("file")){
						oResult.setLink(sPath);
					} else if(sProtocol.startsWith("http")){
						oResult.setLink(sLink);
					}
				}

				Long lSize = oJsonOndaResult.optLong("size");
				setsize(oResult, lSize);

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
			}

			Object oMetadataObject = oInJsonOndaResult.opt("metadata");
			if(null!=oMetadataObject) {
				JSONObject oMetadata = (JSONObject)(oMetadataObject);
				parseMetadata(oResult, oMetadata);
			}
			finalizeViewModel(oResult);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return oResult;
	}


	protected void setsize(QueryResultViewModel oResult, Long lSize) {
		Double dSize = -1.0;
		String sChosenUnit = "ZZ";
		if(null != lSize) {
			dSize = (double)lSize;
			//check against null size
			String[] sUnits = {"B", "kB", "MB", "GB"};
			int iUnitIndex = 0;
			int iLim = sUnits.length -1;
			while(iUnitIndex < iLim && dSize >= 1024) {
				dSize = dSize / 1024;
				iUnitIndex++;
			}
			//now, round it to two decimal digits
			dSize = Math.round(dSize*100)/100.0; 
			sChosenUnit = sUnits[iUnitIndex];
			String sSize = String.valueOf(dSize) + " " + sChosenUnit;
			oResult.getProperties().put("size", sSize);
		}
	}


	private void finalizeViewModel(QueryResultViewModel oResult) {
		//TODO review view model fields: is it all correct? Is there anything missing that can be reconstructed from properties?
		//TODO review m_asOndaToSentinel entries: is it all correct? Is there anything missing that can be reconstructed from properties?
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
			
			if( !Utils.isNullOrEmpty(sTitle) ) {
				if(sTitle.contains(".")) {
					sTitle = sTitle.split("\\.")[0];
				}
			}
			oResult.setTitle(sTitle);
		}
	}


	//XXX move this method to some shared class
	private String pruneFileExtension(String sFileName) {
		String sResult = sFileName.substring(0, sFileName.lastIndexOf(".")); //eliminate the file extension .ZIP, .SAFE...
		//now handle cases like: 2.tar.gz", ".tar.bz" 
		if(sResult.endsWith(".tar")) {
			sResult = pruneFileExtension(sResult);
		}
		return sResult;
	}


	protected void parseMetadata(QueryResultViewModel oResult, JSONObject oMetadata) {
		if(null==oMetadata) {
			System.out.println("DiasResponseTranslator.parseMetadata: oMetadata is null");
			return;
		}
		if(null== oResult ) {
			System.out.println("DiasResponseTranslator.parseMetadata: oResult is null");
			//XXX should we throw an exception instead?
			return;
		}
		if(null!=oMetadata) {
			String sContextKey = "@odata.context";
			String sOdataContext = oMetadata.optString(sContextKey);
			if(null!=sOdataContext) {
				oResult.getProperties().put(sContextKey, sOdataContext);
			}
			JSONArray aoMetadata = oMetadata.optJSONArray("value");

			for (Object oObject : aoMetadata) {
				if(null!=oObject) {
					JSONObject oMetadataEntry = (JSONObject)oObject;
					String sMetaKey = oMetadataEntry.optString("id");
					if(null!= sMetaKey) {
						String sMetaValue = null;
						sMetaValue = oMetadata.optString("value");
						if(null != sMetaValue) {
							String sKey = null;
							sKey = m_asOndaToSentinel.get(sMetaKey);
							if(null == sKey) {
								oResult.getProperties().put(sMetaKey, sMetaValue);
							} else if( sKey.startsWith(m_sPropertyPrefix) ) {
								sKey = sKey.substring(m_sPropertyPrefix.length());
								oResult.getProperties().put(sKey, sMetaValue);
							} else {
								//then it must be a field:
								
							}
						}
					}
				}
			}
		}
	}

	private void buildSummary(QueryResultViewModel oResult) {
		//TODO parse oResult to get the required values instead
		HashMap<String, String> asSummary = new HashMap<>();
		//MAYBE Class FileNameParser + one derived class for each mission, passed by outside (depending on the query) 
		String sSummary = "Date: " + asSummary.get("Date") + ", ";
		sSummary = sSummary + "Instrument: " + asSummary.get("Instrument") + ", ";
		//TODO infer Mode from filename
		sSummary = sSummary + "Mode: " + asSummary.get("Mode") + ", ";
		//TODO infer Satellite from filename
		sSummary = sSummary + "Satellite: " + asSummary.get("Satellite") + ", ";
		sSummary = sSummary + "Size: " + asSummary.get("Size");// + " " + sChosenUnit;
		oResult.setSummary(sSummary);
	}

}
