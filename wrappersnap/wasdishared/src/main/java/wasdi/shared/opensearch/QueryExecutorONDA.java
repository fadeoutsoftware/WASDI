/**
 * Created by Cristiano Nattero and Alessio Corrado on 2018-11-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import org.apache.abdera.i18n.templates.Template;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorONDA extends QueryExecutor {

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getUrlPath()
	 */
	@Override
	protected String[] getUrlPath() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getTemplate()
	 */
	@Override
	protected Template getTemplate() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getCountUrl(java.lang.String)
	 */
	@Override
	protected String getCountUrl(String sQuery) {
		// TODO Auto-generated method stub
		return null;
	}
	
	//append:
	// /Products/$count?$search="name:S2*"
	@Override
	protected String buildUrl(String sQuery){
		//expected elements
		//
		//polygon (optional):
		  //footprint:"intersects(POLYGON((
		    //9.434509277343752 36.82027895130877,
		    //9.434509277343752 44.912304304581525,
		    //24.72747802734375 44.912304304581525,
		    //24.72747802734375 36.82027895130877,
		    //9.434509277343752 36.82027895130877
		    //)))"
		//
		//time interval:
		  //beginPosition:[2018-11-20T00:00:00.000Z TO 2018-11-27T23:59:59.999Z]
		  // AND 
		  //endPosition:[2018-11-20T00:00:00.000Z TO 2018-11-27T23:59:59.999Z] 
		//
		//
		
		
		String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products/$count?$search=\"";
		String polygon = null;
		String date = null;
		String collection = null;
		String cloudCover = null;
		String snowCover = null;
		//TODO check: decode?
		String[] asQueryItems = sQuery.split("AND");
		for (String sItem : asQueryItems) {
			if(sItem.contains("footprint") || sItem.contains("beginPosition") || sItem.contains("endPosition")) {
				sUrl += sItem+" AND ";
			}
		}
		
		sUrl = sUrl +"\"";
		//TODO check: encode?
		return sUrl;
	}

}
