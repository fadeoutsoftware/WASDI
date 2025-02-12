/**
 * Created by Cristiano Nattero on 2020-04-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors.eodc;

import java.util.List;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * Query Executor for EODC
 * 
 * EODC uses CSW standard catalogue.
 * 
 * Files are available only on the local system and cannot be downloaded via http.
 * 
 * 
 * @author c.nattero
 *
 */
public class QueryExecutorEODC extends QueryExecutor {
	
	private static final String s_sOffsetParam = "offset=";
	private static final String s_sLimitParam = "limit=";
	
	public QueryExecutorEODC() {
		m_oQueryTranslator = new QueryTranslatorEODC();
		m_oResponseTranslator = new ResponseTranslatorEODC();
	}


	@Override
	public int executeCount(String sQuery) {
		try {
			WasdiLog.debugLog("QueryExecutorEODC.executeCount( " + sQuery + " )");
			
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
			
			if (m_asSupportedPlatforms.contains(oQueryViewModel.platformName) == false) {
				return 0;
			}			
			
			String sUrl = "https://csw.eodc.eu/";
			
			String sTranslatedQuery = m_oQueryTranslator.getCountUrl(sQuery);

			String sResponse = standardHttpPOSTQuery(sUrl, sTranslatedQuery);
			
			int iResult = m_oResponseTranslator.getCountResult(sResponse);
			
			return iResult;
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryExecutorEODC.executeCount: " + oE);
			return -1;
		}
	}


	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {

		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
		
		if (m_asSupportedPlatforms.contains(oQueryViewModel.platformName) == false) {
			return null;
		}
		
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
						WasdiLog.debugLog("QueryExecutorEODC.executeAndRetrieve: could not parse offset, defaulting");
					}
				}
				if(!sQuery.contains(s_sOffsetParam)) {
					sQuery += "&offset="+iOffset;
				} else {
					WasdiLog.debugLog("QueryExecutorEODC.executeAndRetrieve: offset already specified: " + sQuery);
				}
				
				//limit
				int iLimit = 10;
				if(!Utils.isNullOrEmpty(sLimit)) {
					try {
						iLimit = Integer.parseInt(sLimit);
					}catch (Exception oE) {
						WasdiLog.debugLog("QueryExecutorEODC.executeAndRetrieve: could not parse limit, defaulting");
					}
				}
				if(!sQuery.contains(s_sLimitParam)) {
					sQuery += "&limit="+iLimit;
				} else {
					WasdiLog.debugLog("QueryExecutorEODC.executeAndRetrieve: limit already specified: " + sQuery);
				}
				
			} else {
				if(sQuery.contains(s_sLimitParam)) {
					sQuery = sQuery.replace(s_sLimitParam, "LIMITAUTOMATICALLYREMOVED=");
				}
				
				sQuery += "&limit="+sLimit;
				
				if(sQuery.contains(s_sOffsetParam)) {
					sQuery = sQuery.replace(s_sOffsetParam, "OFFSETAUTOMATICALLYREMOVED=");
				}
				
				sQuery += "&offset="+sOffset;
			}
			
			sPayload = ((QueryTranslatorEODC)m_oQueryTranslator).translate(sQuery);
			
			WasdiLog.debugLog("EODC Payload:");
			WasdiLog.debugLog(sPayload);
			
			sResult = standardHttpPOSTQuery(sUrl, sPayload);
			
			List<QueryResultViewModel> aoResult = null;
			
			if(!Utils.isNullOrEmpty(sResult)) {
				// build result view model
				aoResult = m_oResponseTranslator.translateBatch(sResult, bFullViewModel);
				if(null==aoResult) {
					return null; 
				}
			} else {
				WasdiLog.debugLog("QueryExecutorEODC.executeAndRetrieve(2 args): could not fetch results for url: " + sUrl);
				return null;
			}
			return aoResult;
		} catch (Exception oE) {
			WasdiLog.debugLog("QueryExecutorEODC.executeAndRetrieve(2 args, with sUrl=" + sUrl + "): " + oE);
		}
		return null;
	}
}

