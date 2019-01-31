/**
 * Created by Cristiano Nattero on 2018-12-11
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

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

	/**
	 * 
	 */
	public DiasResponseTranslatorONDA() {
		// TODO Auto-generated constructor stub
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

	public QueryResultViewModel translate(JSONObject oInJsonOndaResult, String sProtocol) {
		QueryResultViewModel oResult = null;
		try {
			//TODO check every get/opt against null value

			//TODO key entry

			//			Object oObject = oInJsonOndaResult.get("entry");
			//			JSONObject oJsonOndaResult = (JSONObject)oObject;
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


				//XXX infer product type from file name
				//XXX infer polarisation from file name
				//XXX infer polarisation from file name

				Map<String, String> asProperties = new HashMap<String, String>();
				//mapped
				String sFormat = oJsonOndaResult.optString("@odata.mediaContentType");
				if(null!=sFormat) {
					asProperties.put("format", sFormat );
				}
				String sName = oJsonOndaResult.optString("name");
				if(!Utils.isNullOrEmpty(sName)) {
					asProperties.put("filename", sName);
					String sPath = "";
					String sPseudopath = oJsonOndaResult.optString("pseudopath");

					if(sProtocol.equals("file:")) {
						if(!Utils.isNullOrEmpty(sPseudopath) && !Utils.isNullOrEmpty(sName)) {
							asProperties.put("pseudopath", sPseudopath);
							sPath = "file:/mnt/";
							String[] sIntermediate = sPseudopath.split(", ");
							sPath += sIntermediate[0]; 
							sPath += "/" + sName + "/.value";
						} else {
							return null;
						}
					} else if(sProtocol.startsWith("https:")) {
						if(!Utils.isNullOrEmpty(sId)) {
							//tentative download
							sPath += sProtocol;
							if(!sProtocol.endsWith("//")) {
								sPath += "//";
							}
							sPath += "catalogue.onda-dias.eu/dias-catalogue/Products";
							sPath += "(";
							sPath += sId;
							sPath += ")";
							sPath += "/$value";
						} else {
							return null;
						}
					}
					oResult.setLink(sPath);
				}
				Long lSize = oJsonOndaResult.optLong("size");
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
					asProperties.put("size", String.valueOf(dSize) + " " + sChosenUnit);
				}

				//ONDA
				String sCreationDate = oJsonOndaResult.optString("creationDate");
				if(null!= sCreationDate) {
					asProperties.put("creationDate", sCreationDate);
				}
				String sOffline = oJsonOndaResult.optString("offline");
				if(sOffline != null) {
					asProperties.put("offline", sOffline);
				}
				String sDownloadable = oJsonOndaResult.optString("downloadable");
				if(null!=sDownloadable) {
					asProperties.put("downloadable", sDownloadable);
				}
				oResult.setProperties(asProperties);

				String sTitle = sName;
				if( !Utils.isNullOrEmpty(sTitle) ) {
					if(sTitle.contains(".")) {
						sTitle = sTitle.split("\\.")[0];
					}
				}
				oResult.setTitle(sTitle);

				//build summary
				//MAYBE Class FileNameParser + one derived class for each mission, passed by outside (depending on the query) 
				//TODO infer date from filename instead
				String sSummary = "Date: " + asProperties.get("creationDate" + ", ");
				//TODO infer instrument from filename
				sSummary = sSummary + "Instrument: " + ", ";
				//TODO infer Mode from filename
				sSummary = sSummary + "Mode: " + ", ";
				//TODO infer Satellite from filename
				sSummary = sSummary + "Satellite: " + ", ";
				sSummary = sSummary + "Size: " +dSize + " " + sChosenUnit;

				oResult.setSummary(sSummary);
			}
			//not necessary, since these are info already acquired
//			Object oProductInfoObject = oInJsonOndaResult.opt("productInfo");
//			if(null!=oProductInfoObject) {
//				JSONObject oProductInfo = (JSONObject)(oProductInfoObject); 
//				if(null!= oProductInfo) {
//					//TODO check consistency
//					String sProdId = oProductInfo.optString("id");
//					if(null!= sProdId) {
//						if(oResult.getId() != null && !sProdId.equals(oResult.getId())) {
//							throw new Exception("queried id is: " + oResult.getId() + " but productInfo id is: " + sProdId);
//						} else if(null == oResult.getId() ) {
//							//last rescue option, but can this ever really happen?
//							oResult.setId(sProdId);
//						}
//					}//else it can be null, no problem
//					String sProdFootprint = oProductInfo.optString("footprint");
//					if(null!= sProdFootprint) {
//						if(null!= oResult.getFootprint() && !sProdFootprint.equals(oResult.getFootprint())) {
//							throw new Exception("queried footprint is: " + oResult.getFootprint() + " but productInfo footprint is: " + sProdFootprint);
//						} else if (null==oResult.getFootprint()) {
//							oResult.setFootprint(sProdFootprint);
//						}
//					}
//					String sProdQuicklook = oProductInfo.optString("quicklook");
//					if(null!=sProdQuicklook) {
//						if(null!=oResult.getPreview() && !sProdQuicklook.equals(oResult.getPreview())) {
//							throw new Exception("queried preview is: " + oResult.getPreview() + " but productInfo preview (quicklook) is: " + sProdQuicklook);
//						}
//					}
//					//TODO integrate with productInfo
//				}
//			}

			Object oMetadataObject = oInJsonOndaResult.opt("metadata");
			JSONObject oMetadata = (JSONObject)(oMetadataObject);
			if(null!=oMetadata) {
				String sKey = "@odata.context";
				String sOdataContext = oMetadata.optString(sKey);
				if(null!=sOdataContext) {
					oResult.getProperties().put(sKey, sOdataContext);
				}
				JSONArray aoMetadata = oMetadata.optJSONArray("value");
				Map<String,String> asOndaToSentinel = new HashMap<>();
				asOndaToSentinel.put("beginPosition", "properties.beginPosition");
				//TODO populate the map
				if(null!=aoMetadata) {
					for (Object oObject : aoMetadata) {
						if(null!=oObject) {
							JSONObject oMetadataEntry = (JSONObject)oObject;
							//TODO get id and use it as a key
							//if the corresponding value is not null then use it as a key
							//to save the metadata "value" field into oResult
							//else if it is null use the metadata "id" value as a key and save it
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return oResult;
	}

}
