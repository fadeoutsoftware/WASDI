package wasdi.shared.queryexecutors.creodias2;

import java.util.List;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class QueryExecutorCreoDias2 extends QueryExecutor {
	
	
	public QueryExecutorCreoDias2() {
		m_sProvider = "CREODIAS2";
		this.m_oQueryTranslator = new QueryTranslatorCreoDias2();
		this.m_oResponseTranslator = new ResponseTranslatorCreoDias2();
	}

	@Override
	public int executeCount(String sQuery) {
		// TODO Auto-generated method stub
		// receive in input the WASDI query, must return the number of results for the provider 
		return 0;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		// TODO Auto-generated method stub
		//  receive in input the WASDI query, must return the list of provider's results  as a list of QueryResultViewModel
		// (there, the important fields are title and link).
		return null;
	}
	
	

}
