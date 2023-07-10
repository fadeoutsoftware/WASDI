package wasdi.shared.queryexecutors.creodias2;

import java.util.List;
import java.util.Map;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class QueryExecutorCreoDias2 extends QueryExecutor {
	
	
	public QueryExecutorCreoDias2() {
		m_sProvider = "CREODIAS2";
		this.m_oQueryTranslator = new QueryTranslatorCreoDias2();
		this.m_oResponseTranslator = new ResponseTranslatorCreoDias2();
	}

	@Override
	public int executeCount(String sQuery) {
		// MUST
		// TODO Auto-generated method stub
		// receive in input the WASDI query, must return the number of results for the provider 
		String sOutputQuery = m_oQueryTranslator.getCountUrl(sQuery);
		
		// execute standard http query
		String sResults = standardHttpGETQuery(sOutputQuery);
		
		Map<String, Object> aoProperties = JsonUtils.jsonToMapOfObjects(sResults);
		
		if (sResults == null) return -1;

		int iCount = -1;

		try {
			iCount = ((List) aoProperties.get("value")).size();
		} catch (Exception oException) {
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
		return null;
	}
	
	

}
