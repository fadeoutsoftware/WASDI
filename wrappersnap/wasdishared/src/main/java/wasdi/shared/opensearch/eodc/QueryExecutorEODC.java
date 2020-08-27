/**
 * Created by Cristiano Nattero on 2020-04-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.eodc;

import java.io.IOException;
import java.util.List;

import org.apache.abdera.i18n.templates.Template;
import org.json.JSONObject;

import wasdi.shared.opensearch.PaginatedQuery;
import wasdi.shared.opensearch.QueryExecutor;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

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

		if(Utils.isNullOrEmpty(sResponse)) {
			Utils.debugLog("QueryExecutorEODC.extractNumberOfResults: response is null, aborting");
			return "";
		}

		JSONObject oJson = null;
		try {
			oJson = new JSONObject(sResponse);
		}
		catch (Exception oE) {
			Utils.debugLog("QueryExecutorEODC.extractNumberOfResults: could not create JSONObject (syntax error or duplicate key), aborting");
			return "";
		}

		if(!oJson.has("csw:GetRecordsResponse")) {
			Utils.debugLog("QueryExecutorEODC.extractNumberOfResults: \"csw:GetRecordsResponse\" not found in JSON, aborting");
			return "";
		}

		JSONObject oCswGetRecordsResponse = oJson.optJSONObject("csw:GetRecordsResponse");
		if(null==oCswGetRecordsResponse) {
			Utils.debugLog("QueryExecutorEODC.extractNumberOfResults: could not access JSONObject at \"csw:GetRecordsResponse\", aborting");
			return "";
		}

		if(!oCswGetRecordsResponse.has("csw:SearchResults")) {
			Utils.debugLog("QueryExecutorEODC.extractNumberOfResults: \"csw:SearchResults\" not found in \"csw:GetRecordsResponse\", aborting");
			return "";
		}

		JSONObject oSearchResults = oCswGetRecordsResponse.optJSONObject("csw:SearchResults");
		if(null==oSearchResults) {
			Utils.debugLog("QueryExecutorEODC.extractNumberOfResults: could not access JSONObject at \"csw:SearchResults\", aborting");
			return "";
		}

		if(!oSearchResults.has("@numberOfRecordsMatched")) {
			Utils.debugLog("QueryExecutorEODC.extractNumberOfResults: \"@numberOfRecordsMatched\" not found int \"csw:SearchResults\", aborting");
			return "";
		}

		int iResults = oSearchResults.optInt("@numberOfRecordsMatched", -1);
		if(iResults < 0) {
			Utils.debugLog("QueryExecutorEODC.extractNumberOfResults: could not access \"@numberOfRecordsMatched\", aborting");
			return "";
		}
		String sCount = "" + iResults;
		return sCount;
	}

	public int executeCount(String sQuery) throws IOException {
		try {
			Utils.debugLog("QueryExecutor.executeCount( " + sQuery + " )");
			//sQuery = encodeAsRequired(sQuery); 
			String sUrl = getCountUrl(sQuery);

			
			if(sQuery.contains("offset=")) {
				sQuery = sQuery.replace("offset=", "OFFSETAUTOMATICALLYREMOVED=");
			}
			if(sQuery.contains("limit=")) {
				sQuery = sQuery.replace("limit=", "LIMITAUTOMATICALLYREMOVED=");
			}
			//we do not need results now, just the count
			sQuery += "&limit=0";

			DiasQueryTranslatorEODC oEODC = new DiasQueryTranslatorEODC();
			String sTranslatedQuery = oEODC.translate(sQuery);
			sTranslatedQuery = sTranslatedQuery.replace("<csw:ElementSetName>full</csw:ElementSetName>", "<csw:ElementSetName>brief</csw:ElementSetName>");
			
			Utils.debugLog("EODC Payload:");
			Utils.debugLog(sTranslatedQuery);

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


	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {

		Utils.debugLog(s_sClassName + ".executeAndRetrieve(" + oQuery + ", " + bFullViewModel + ")");
		String sResult = null;
		String sUrl = null;
		try {
			sUrl = "https://csw.eodc.eu/";
			String sPayload = "";

			String sQuery = oQuery.getQuery();
			String sLimit = oQuery.getLimit();
			String sOffset = oQuery.getOffset();
			
			if(bFullViewModel) {
				//offset
				int iOffset = 1;
				if(!Utils.isNullOrEmpty(sOffset)) {
					try {
						iOffset = Integer.parseInt(sOffset);
						//in EODC offset starts at 1
						iOffset += 1;
					}catch (Exception oE) {
						Utils.debugLog(s_sClassName + ".executeAndRetrieve: could not parse offset, defaulting");
					}
				}
				if(!sQuery.contains("offset=")) {
					sQuery += "&offset="+iOffset;
				} else {
					Utils.debugLog(s_sClassName + ".executeAndRetrieve: offset already specified: " + sQuery);
				}
				
				//limit
				int iLimit = 10;
				if(!Utils.isNullOrEmpty(sLimit)) {
					try {
						iLimit = Integer.parseInt(sLimit);
					}catch (Exception oE) {
						Utils.debugLog(s_sClassName + ".executeAndRetrieve: could not parse limit, defaulting");
					}
				}
				if(!sQuery.contains("limit=")) {
					sQuery += "&limit="+iLimit;
				} else {
					Utils.debugLog(s_sClassName + ".executeAndRetrieve: limit already specified: " + sQuery);
				}
				
			} else {
				if(sQuery.contains("limit=")) {
					sQuery = sQuery.replace("limit=", "LIMITAUTOMATICALLYREMOVED=");
				}
				
				sQuery += "&limit="+sLimit;
				
				if(sQuery.contains("offset=")) {
					sQuery = sQuery.replace("offset=", "OFFSETAUTOMATICALLYREMOVED=");
				}
				
				sQuery += "&offset="+sOffset;
			}
			DiasQueryTranslatorEODC oEODC = new DiasQueryTranslatorEODC();
			sPayload = oEODC.translate(sQuery);
			
			Utils.debugLog("EODC Payload:");
			Utils.debugLog(sPayload);
			
			sResult = httpPostResults(sUrl, "search", sPayload);		
			List<QueryResultViewModel> aoResult = null;
			if(!Utils.isNullOrEmpty(sResult)) {
				//todo build result view model
				aoResult = buildResultViewModel(sResult, bFullViewModel);
				if(null==aoResult) {
					throw new NullPointerException(s_sClassName + ".executeAndRetrieve: aoResult is null"); 
				}
			} else {
				Utils.debugLog(s_sClassName + ".executeAndRetrieve(2 args): could not fetch results for url: " + sUrl);
			}
			return aoResult;
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".executeAndRetrieve(2 args, with sUrl=" + sUrl + "): " + oE);
		}
		return null;
	}


	@Override
	protected List<QueryResultViewModel> buildResultViewModel(String sJson, boolean bFullViewModel){
		DiasResponseTranslatorEODC oEODC = new DiasResponseTranslatorEODC();
		return oEODC.translateBatch(sJson, bFullViewModel, "file");
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

