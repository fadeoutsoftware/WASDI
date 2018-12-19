/**
 * Created by Cristiano Nattero on 2018-12-11
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

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
	public QueryResultViewModel translate(Object oObject) {
		try {
			JSONObject oJson = (JSONObject)oObject;
			return translate(oJson);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public QueryResultViewModel translate(JSONObject oJsonOndaResult) {
		try {
			//TODO check every get/opt against null value
			
			QueryResultViewModel oResult = new QueryResultViewModel();
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
				String sPseudopath = oJsonOndaResult.optString("pseudopath");
				if(null!=sPseudopath) {
					asProperties.put("pseudopath", sPseudopath);
					String sPath = "file:/mnt/";
					String[] sIntermediate = sPseudopath.split(", ");
					sPath += sIntermediate[0]; 
					sPath += "/" + sName + ".value";
					oResult.setLink("file:"+sPath);
				}
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
		
			
			return oResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
