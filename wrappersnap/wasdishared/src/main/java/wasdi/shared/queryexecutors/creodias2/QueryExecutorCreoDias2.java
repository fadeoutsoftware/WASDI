package wasdi.shared.queryexecutors.creodias2;

import java.util.List;
import java.util.Map;
import java.util.LinkedList;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class QueryExecutorCreoDias2 extends QueryExecutor {
	
	
	public QueryExecutorCreoDias2() {
		m_sProvider = "CREODIAS2";
		this.m_oQueryTranslator = new QueryTranslatorCreoDias2();
		this.m_oResponseTranslator = new ResponseTranslatorCreoDias2();
	}

	@Override
	public int executeCount(String sQuery) {
		int iCount = -1;

		try {
			String sOutputQuery = m_oQueryTranslator.getCountUrl(sQuery);
			
			String sResults = standardHttpGETQuery(sOutputQuery);
			
			iCount = m_oResponseTranslator.getCountResult(sResults);
			
		} catch (Exception oException) {
			WasdiLog.debugLog("QueryExecutorCreoDias2.executeCount. Error when trying to retrieve the number of results " + oException.getMessage());
			return -1;
		}
		
		return iCount;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		// MUST
		// TODO Auto-generated method stub
		//  receive in input the WASDI query, must return the list of provider's results  as a list of QueryResultViewModel
		// (there, the important fields are title and link).
		
		
		// TODO: in the query string, most probably I will need to add the filter "&$expand=Attributes" to get the information about the product that 
		// must be shown in the list
		
		// 1- check if the platform is supported
		
		
		List<QueryResultViewModel> aoRes = new LinkedList<>();
		
		String sOutputQuery = m_oQueryTranslator.getSearchUrl(oQuery);
		
		// Call standard http get API
		String sResults = standardHttpGETQuery(sOutputQuery);
		
		if (sResults==null) return aoRes;
		
		return aoRes;
		
		
	}
	
	public static void main(String[] args) throws Exception {
		QueryExecutorCreoDias2 oQE = new QueryExecutorCreoDias2();
		int iCount = oQE.executeCount("( beginPosition:[2023-07-03T00:00:00.000Z TO 2023-07-10T23:59:59.999Z] AND endPosition:[2023-07-03T00:00:00.000Z TO 2023-07-10T23:59:59.999Z] ) AND   (platformname:Sentinel-3 AND productlevel:L1 AND Instrument:SRAL AND producttype:SR_1_SRA___ AND timeliness:Near Real Time AND relativeorbitstart:38)");
		System.out.println(iCount);
	}
}
