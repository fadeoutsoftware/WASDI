/**
 * Created by Cristiano Nattero on 2019-12-23
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.creodias;

import wasdi.shared.opensearch.DiasQueryTranslator;

/**
 * @author c.nattero
 *
 */
public class DiasQueryTranslatorCREODIAS extends DiasQueryTranslator {

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#translate(java.lang.String)
	 */
	@Override
	public String translate(String sQuery) {
		// TODO translate and return the url string
		//already added prefix: https://finder.creodias.eu/resto/api/collections/
		return "Sentinel1/search.json?startDate=2019-12-01T00:00:00Z&completionDate=2019-12-03T23:59:59Z&geometry=POLYGON((7.397874989401342+45.00475144371268,10.373746303074263+44.94785607558927,10.389830621260842+43.612039503172866,7.703504034412235+43.809704932512176,7.397874989401342+45.00475144371268))";
	}

	@Override
	protected String parseTimeFrame(String sQuery) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected String parseFootPrint(String sQuery) {
		// TODO Auto-generated method stub
		return null;
	}

}
