/**
 * Created by Cristiano Nattero on 2020-02-03
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.sobloo;

import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.opensearch.DiasQueryTranslator;

/**
 * @author c.nattero
 *
 */
public class DiasQueryTranslatorSOBLOO extends DiasQueryTranslator {
	
	private JSONObject m_oJSONConf;

	//TODO one class per mission, w/ 2 maps: WASDI to SOBLOO keys, and WASDI to SOBLOO values 
	
	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#translate(java.lang.String)
	 */
	@Override
	protected String translate(String sQuery) {
		return "f=acquisition.missionName:eq:Sentinel-1A";
	}

}
