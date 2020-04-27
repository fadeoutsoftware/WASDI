/**
 * Created by Cristiano Nattero on 2020-04-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.eodc;

import wasdi.shared.opensearch.DiasQueryTranslator;

/**
 * @author c.nattero
 *
 */
public class DiasQueryTranslatorEODC extends DiasQueryTranslator {

	static String s_sQueryPrefix ="<csw:GetRecords xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" service=\"CSW\" version=\"2.0.2\" resultType=\"results\" startPosition=\"1\" maxRecords=\"10\" outputFormat=\"application/json\" outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/cat/csw/2.0.2 http://schemas.opengis.net/csw/2.0.2/CSW-discovery.xsd\"><csw:Query typeNames=\"csw:Record\"><csw:ElementSetName>full</csw:ElementSetName><csw:Constraint version=\"1.1.0\"><ogc:Filter><ogc:And>";
	static String s_sQuerySuffix = "</ogc:And></ogc:Filter></csw:Constraint></csw:Query></csw:GetRecords>";
	
	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#translate(java.lang.String)
	 */
	@Override
	protected String translate(String sQueryFromClient) {
		String sQuery = prepareQuery(sQueryFromClient);
		String sResult = "";
		//todo translate
		
		if(sQuery.contains(s))
		
		return sResult;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#parseTimeFrame(java.lang.String)
	 */
	@Override
	protected String parseTimeFrame(String sQuery) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#parseFootPrint(java.lang.String)
	 */
	@Override
	protected String parseFootPrint(String sQuery) {
		// TODO Auto-generated method stub
		return null;
	}

}
