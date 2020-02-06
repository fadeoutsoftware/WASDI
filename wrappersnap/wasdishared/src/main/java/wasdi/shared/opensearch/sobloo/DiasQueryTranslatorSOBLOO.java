/**
 * Created by Cristiano Nattero on 2020-02-03
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

/**
 * @author c.nattero
 *
 */
public class DiasQueryTranslatorSOBLOO extends DiasQueryTranslator {

	//TODO one class per mission, w/ 2 maps: WASDI to SOBLOO keys, and WASDI to SOBLOO values 
	
	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#translate(java.lang.String)
	 */
	@Override
	protected String translate(String sQuery) {
		return "f=acquisition.missionName:eq:Sentinel-1A";
	}

}
