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
	
	String m_sLocale;
	String m_sEnconding;
	String m_sDecoding;

	DiasQueryTranslator(){
		//MAYBE overload the constructor if more cases are needed
		m_sLocale = "UTF-8";
		m_sEnconding = m_sLocale;
		m_sDecoding = m_sLocale;
	}
	
	public abstract String translate(String sQuery);
	public abstract String encode( String sDecoded );
	public abstract String decode(String sEncoded );
	
	public String translateAndEncode(String sQuery) {
		return encode(translate(sQuery));
	}
	
	public String trimDoubles(String sQuery, char c) {
		String sSingle = new String("");
		sSingle = sSingle + c;
		String sDouble = new String(sSingle);
		sDouble = sDouble + c;
		while( sQuery.contains(sDouble) ) {
			sQuery.replaceAll(sDouble, sSingle);
		}
		
		return sQuery;
	}

}
