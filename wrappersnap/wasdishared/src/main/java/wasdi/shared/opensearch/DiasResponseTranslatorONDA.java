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
			//TODO translate from JSON to QueryResultViewModel
			QueryResultViewModel oResult = new QueryResultViewModel();
			oResult.setProvider("ONDA");
			oResult.setFootprint( oJsonOndaResult.getString("footprint") );
			oResult.setId(oJsonOndaResult.getString("id"));
			String sPreview = oJsonOndaResult.optString("quicklook");
			//is there a preview?
			if( !Utils.isNullOrEmpty(sPreview) ) {
				oResult.setPreview("data:image/png;base64,"+ sPreview);
			} else {
				oResult.setPreview(null);
			}
						
			//XXX infer product type from file name
			//XXX infer polarisation from file name
			//XXX infer polarisation from file name
			
			Map<String, String> asProperties = new HashMap<String, String>();
			//mapped
			asProperties.put("format", oJsonOndaResult.getString("@odata.mediaContentType"));
			asProperties.put("filename", oJsonOndaResult.getString("name"));
			double dSize = oJsonOndaResult.getInt("size");
			dSize = dSize / (1024 * 1024); 
			asProperties.put("size", String.valueOf(dSize) + " GB");
			//ONDA
			asProperties.put("creationDate", oJsonOndaResult.getString("creationDate"));
			asProperties.put("offline", oJsonOndaResult.optString("offline"));
			asProperties.put("pseudopath", oJsonOndaResult.getString("pseudopath"));
			asProperties.put("downloadable", oJsonOndaResult.optString("downloadable"));
			
			oResult.setProperties(asProperties);
			
			//oResult.setLink(link);
			String sTitle = oJsonOndaResult.getString("name").split("\\.")[0];
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
			sSummary = sSummary + "Size: " +dSize + " GB";
			
			oResult.setSummary(sSummary);
		
			
			return oResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
