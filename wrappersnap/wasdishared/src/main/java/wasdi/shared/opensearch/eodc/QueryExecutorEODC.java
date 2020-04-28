/**
 * Created by Cristiano Nattero on 2020-04-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.eodc;

import java.io.IOException;

import org.apache.abdera.i18n.templates.Template;
import org.json.JSONObject;

import wasdi.shared.opensearch.QueryExecutor;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorEODC extends QueryExecutor {

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
		String sUrl = "https://csw.eodc.eu/";
		return sUrl;
	}
	
	
	@Override
	protected String extractNumberOfResults(String sResponse) {
		//todo parse response json and extract number
		String sCount = "";
		if(!Utils.isNullOrEmpty(sResponse)) {
			JSONObject oJson = new JSONObject(sResponse);
			if(null!=oJson) {
				if(oJson.has("csw:GetRecordsResponse")) {
					JSONObject oCswGetRecordsResponse = oJson.optJSONObject("csw:GetRecordsResponse");
					if(null!=oCswGetRecordsResponse) {
						if(oCswGetRecordsResponse.has("csw:SearchResults")) {
							JSONObject oSearchResults = oCswGetRecordsResponse.optJSONObject("csw:SearchResults");
							if(null!=oSearchResults) {
								if(oSearchResults.has("@numberOfRecordsMatched")) {
									int iResults = oSearchResults.optInt("@numberOfRecordsMatched", -1);
									sCount = "" + iResults;
								}
							}
						}
					}
				}
			}
		}
		
		return sCount;
	}

	public int executeCount(String sQuery) throws IOException {
		try {
			Utils.debugLog("QueryExecutor.executeCount( " + sQuery + " )");
			//sQuery = encodeAsRequired(sQuery); 
			String sUrl = getCountUrl(sQuery);
			
			
			// ( footprint:"intersects(POLYGON((92.36417183697604 12.654592055231863,92.36417183697604 26.282214356266774,99.48157676962991 26.282214356266774,99.48157676962991 12.654592055231863,92.36417183697604 12.654592055231863)))" ) AND ( beginPosition:[2019-05-01T00:00:00.000Z TO 2020-04-27T23:59:59.999Z] AND endPosition:[2019-05-01T00:00:00.000Z TO 2020-04-27T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND producttype:GRD AND relativeorbitnumber:33)&providers=ONDA
			DiasQueryTranslatorEODC oEODC = new DiasQueryTranslatorEODC();
			//String sTranslatedQuery = oEODC.translateAndEncode(sQuery);
			String sTranslatedQuery = oEODC.translate(sQuery);
			sTranslatedQuery.replace("<csw:ElementSetName>full</csw:ElementSetName>", "<csw:ElementSetName>brief</csw:ElementSetName>");
			
			String sResponse = httpPostResults(sUrl, "count", sTranslatedQuery);
			int iResult = 0;
			try {
				iResult = Integer.parseInt(extractNumberOfResults(sResponse));
			} catch (NumberFormatException oNfe) {
				Utils.debugLog("QueryExecutor.executeCount: the response ( " + sResponse + " ) was not an int: " + oNfe);
				return -1;
			}
			return iResult;
		} catch (Exception oE) {
			Utils.debugLog("QueryExecutor.executeCount: " + oE);
			return -1;
		}
	}
	
	
	public static void main(String[] args) {
		QueryExecutorEODC oEODC = new QueryExecutorEODC();
		int iResults = -2;
		try {
			iResults = oEODC.executeCount("( footprint:\"intersects(POLYGON((92.36417183697604 12.654592055231863,92.36417183697604 26.282214356266774,99.48157676962991 26.282214356266774,99.48157676962991 12.654592055231863,92.36417183697604 12.654592055231863)))\" ) AND ( beginPosition:[2019-05-01T00:00:00.000Z TO 2020-04-27T23:59:59.999Z] AND endPosition:[2019-05-01T00:00:00.000Z TO 2020-04-27T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND producttype:GRD AND relativeorbitnumber:33)&providers=ONDA");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(iResults);
		
	}
}

