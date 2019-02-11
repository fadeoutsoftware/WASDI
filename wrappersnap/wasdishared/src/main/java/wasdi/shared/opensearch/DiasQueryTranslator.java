/**
 * Created by Cristiano Nattero on 2018-11-28
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.HashMap;

/**
 * @author c.nattero
 *
 */
public abstract class DiasQueryTranslator {
	
	protected HashMap<String, String> keyMapping;
	protected HashMap<String, String> valueMapping;
		
	//translates from WASDI query (OpenSearch) to <derived class> format
	public abstract String translate(String sQuery);
	public abstract String encode( String sDecoded );
	
	public String translateAndEncode(String sQuery) {
		return encode(translate(sQuery));
	}
	
}
