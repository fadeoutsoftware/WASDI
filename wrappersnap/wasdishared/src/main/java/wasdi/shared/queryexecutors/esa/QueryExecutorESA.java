package wasdi.shared.queryexecutors.esa;

import java.util.List;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class QueryExecutorESA extends QueryExecutor {

	public QueryExecutorESA() {
		this.m_sProvider = "ESA";
		this.m_oQueryTranslator = new QueryTranslatorESA();
		this.m_oResponseTranslator = new ResponseTranslatorESA();
		this.m_asSupportedPlatforms.add(Platforms.ERS);
	}

	@Override
	public int executeCount(String sQuery) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		// TODO Auto-generated method stub
		return null;
	}

}
