/**
 * Created by Cristiano Nattero on 2018-11-28
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

/**
 * @author c.nattero
 *
 */
public abstract class DiasQueryTranslator {
	
	//MAYBE specify locale, encoding and decoding formats
	
	//translates from WASDI query (OpenSearch) to <derived class> format
	public abstract String translate(String sQuery);
	public abstract String encode( String sDecoded );
	public abstract String decode(String sEncoded );
	
	public String translateAndEncode(String sQuery) {
		return encode(translate(sQuery));
	}
	
}
